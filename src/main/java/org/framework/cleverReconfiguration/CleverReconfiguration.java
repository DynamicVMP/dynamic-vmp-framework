package org.framework.cleverReconfiguration;

import org.domain.*;
import org.framework.*;
import org.framework.concurrent.StaticReconfMemeCall;
import org.framework.memeticAlgorithm.MASettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Saul Zalimben.
 * @since 12/26/16.
 */
public class CleverReconfiguration {

    public static final String DYNAMIC_VMP_CLEVER_RECONFIGURATION = "DynamicVMP: Clever Reconfiguration";

    /**
     * VMPManager
     * @param workload Workload Trace
     * @param physicalMachines List of Physical Machines
     * @param virtualMachines List of Virtual Machines
     * @param derivedVMs List of Derived Virtual Machines
     * @param revenueByTime Revenue by time
     * @param wastedResources WastedResources by time
     * @param wastedResourcesRatioByTime WastedResourcesRatio per time
     * @param powerByTime Power Consumption by time
     * @param placements List of Placement by time
     * @param code Heuristics Algorithm Code
     * @param timeUnit Time init
     * @param requestsProcess Type of Process
     * @param maxPower Maximum Power Consumption
     * @param realRevenue Revenue
     *
     * <b>RequestsProcess</b>:
     *  <ul>
     *      <li>Requests[0]: requestServed Number of requests served</li>
     *      <li>Requests[1]: requestRejected Number of requests rejected</li>
     *      <li>Requests[2]: requestUpdated Number of requests updated</li>
     *      <li>Requests[3]: violation Number of violation</li>
     *  </ul>
     *
     * @throws IOException
     */
    public static void cleverReconfigurationgManager(List<Scenario> workload, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine>
            virtualMachines, List<VirtualMachine> derivedVMs,
            Map<Integer, Float> revenueByTime, List<Resources> wastedResources,  Map<Integer, Float> wastedResourcesRatioByTime,
            Map<Integer, Float> powerByTime, Map<Integer, Placement> placements, Integer code, Integer timeUnit,
            Integer[] requestsProcess, Float maxPower, Float[] realRevenue, String scenarioFile)
            throws IOException, InterruptedException, ExecutionException {

        System.out.println(DYNAMIC_VMP_CLEVER_RECONFIGURATION);

        List<APrioriValue> aPrioriValuesList = new ArrayList<>();
        List<VirtualMachine> vmsToMigrate = new ArrayList<>();
        List<Integer> vmsMigrationEndTimes = new ArrayList<>();
        List<Float> valuesSelectedForecast = new ArrayList<>();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        MASettings memeConfig = Utils.getMemeConfig(true);
        Callable<Placement> staticReconfgTask;
        Future<Placement> reconfgResult = null;
        Placement reconfgPlacementResult = null;

        Boolean isMigrationActive = false;
        Boolean isUpdateVmUtilization = false;
        Boolean isReconfigurationActive = false;
        Integer actualTimeUnit;
        Integer nextTimeUnit;

        Integer memeticTimeInit = -1;
        Integer memeticTimeEnd=-1;

        Integer migrationTimeInit=-1;
        Integer migrationTimeEnd=-1;

        Integer vmEndTimeMigration = 0;

        for (int iterator = 0; iterator < workload.size(); ++iterator) {
            Scenario request = workload.get(iterator);
            actualTimeUnit = request.getTime();
            //check if is the last request, assign -1 to nextTimeUnit if so.
            nextTimeUnit = iterator + 1 == workload.size() ? -1 : workload.get(iterator + 1).getTime();

            if (nextTimeUnit!= -1 && isMigrationActive && DynamicVMP.isVmBeingMigrated(request.getVirtualMachineID(),
                    vmsToMigrate)){

                // TODO: Check why this is null in some scenarios
                VirtualMachine vmMigrating = VirtualMachine.getById(request.getVirtualMachineID(),virtualMachines);
                if(vmMigrating != null) {
                    vmEndTimeMigration = Utils.getEndTimeMigrationByVm(vmMigrating.getId(),vmsToMigrate,vmsMigrationEndTimes);
                }

                isUpdateVmUtilization = actualTimeUnit <= vmEndTimeMigration;
            }

            DynamicVMP.runHeuristics(request, code, physicalMachines, virtualMachines, derivedVMs, requestsProcess, isUpdateVmUtilization);

            // check if its the last request or a variation of time unit will occurs.
            if (nextTimeUnit == -1 || !actualTimeUnit.equals(nextTimeUnit)) {
                ObjectivesFunctions.getObjectiveFunctionsByTime(physicalMachines,
                        virtualMachines, derivedVMs, wastedResources,
                        wastedResourcesRatioByTime, powerByTime, revenueByTime, timeUnit, actualTimeUnit);

                Float placementScore = ObjectivesFunctions.getDistanceOrigenByTime(request.getTime(),
                        maxPower, powerByTime, revenueByTime, wastedResourcesRatioByTime);

                DynamicVMP.updateLeasingCosts(derivedVMs);

                //        TODO: Only for debug
                Utils.printToFile(scenarioFile, placementScore);

                timeUnit = actualTimeUnit;

                Placement heuristicPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
                        VirtualMachine.cloneVMsList(virtualMachines),
                        VirtualMachine.cloneVMsList(derivedVMs), placementScore);
                placements.put(actualTimeUnit, heuristicPlacement);

                //check the historical information
                if(nextTimeUnit!=-1 && placements.size() > Parameter.HISTORICAL_DATA_SIZE &&
                        !isReconfigurationActive && !isMigrationActive ){
                    //collect O.F. historical values
                    valuesSelectedForecast.clear();
                    for(int timeIterator = nextTimeUnit - Parameter.HISTORICAL_DATA_SIZE; timeIterator<=actualTimeUnit;
                            timeIterator++){
                        if(placements.get(timeIterator)!=null){
                            valuesSelectedForecast.add(placements.get(timeIterator).getPlacementScore());
                        }else{
                            valuesSelectedForecast.add(0F);
                        }
                    }

                    //check if a  call for reconfiguration is needed and set the init time
                    if(Utils.callToReconfiguration(valuesSelectedForecast, Parameter.FORECAST_SIZE)){
                        Utils.printToFile(Constant.RECONFIGURATION_CALL_TIMES_FILE,nextTimeUnit);
                        memeticTimeInit = nextTimeUnit;
                        isReconfigurationActive=true;
                    }else{
                        memeticTimeInit=-1;
                    }
                }

                if(nextTimeUnit!=-1 && nextTimeUnit.equals(memeticTimeInit)){

                    memeticTimeInit = nextTimeUnit;
                    if(virtualMachines.size()!=0){
                        // Clone the current placement
                        Placement memeticPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
                                VirtualMachine.cloneVMsList(virtualMachines),
                                VirtualMachine.cloneVMsList(derivedVMs));

                        //get the list of a priori values
                        aPrioriValuesList = Utils.getAprioriValuesList(actualTimeUnit);
                        //config the call for the memetic algorithm
                        staticReconfgTask = new StaticReconfMemeCall(memeticPlacement,aPrioriValuesList,
                                memeConfig);
                        //call the memetic algorithm in a separate thread
                        reconfgResult = executorService.submit(staticReconfgTask);
                        //update the time end of the memetic algorithm execution
                        memeticTimeEnd = memeticTimeInit + memeConfig.getExecutionDuration();
                        //update the migration init time
                        migrationTimeInit = memeticTimeEnd+1;

                    }else{
                        migrationTimeInit += 1;
                    }


                }else if(nextTimeUnit != -1 && nextTimeUnit.equals(migrationTimeInit)) {
                    isMigrationActive = true;
                    try {

                        if(reconfgResult != null) {
                            //get the placement from the memetic algorithm execution
                            reconfgPlacementResult = reconfgResult.get();
                            //get vms to migrate
                            vmsToMigrate  = Utils.getVMsToMigrate(reconfgPlacementResult.getVirtualMachineList(),
                                    placements.get(memeticTimeInit - 1).getVirtualMachineList());
                            //update de virtual machine list of the placement for the migration operation
                            Utils.removeDeadVMsFromPlacement(reconfgPlacementResult,actualTimeUnit,memeConfig.getNumberOfResources());
                            //update de virtual machines migrated
                            Utils.removeDeadVMsMigrated(vmsToMigrate,actualTimeUnit);
                            //update the placement score after filtering dead  virtual machines.
                            reconfgPlacementResult.updatePlacementScore(aPrioriValuesList);
                            //get end time of vms migrations
                            vmsMigrationEndTimes = Utils.getTimeEndMigrationByVM(vmsToMigrate, actualTimeUnit);
                            //update migration end
                            migrationTimeEnd = Utils.getMigrationEndTime(vmsMigrationEndTimes);

                        }
                    } catch (ExecutionException e) {
                        Logger.getLogger(DYNAMIC_VMP_CLEVER_RECONFIGURATION).log(Level.SEVERE, "Migration Failed!");
                        throw e;
                    }

                } else if(nextTimeUnit != -1 && actualTimeUnit.equals(migrationTimeEnd)) {
					/* get here the new virtual machines to insert in the placement generated by the
		            memetic algorithm using Best Fit Decreasing */
                    isMigrationActive = false;
                    isReconfigurationActive = false;

                    //update de virtual machine list of the placement after the migration operation
                    Utils.removeDeadVMsFromPlacement(reconfgPlacementResult,actualTimeUnit,memeConfig.getNumberOfResources());
                    //update the placement score after filtering dead  virtual machines.
                    reconfgPlacementResult.updatePlacementScore(aPrioriValuesList);

                    Placement memeticPlacement = DynamicVMP.updatePlacementAfterReconf(workload, Constant.BFD,
                            reconfgPlacementResult,
                            memeticTimeInit,
                            migrationTimeEnd);

                    if(DynamicVMP.isMememeticPlacementBetter(placements.get(actualTimeUnit), memeticPlacement)) {
                        physicalMachines = new ArrayList<>(memeticPlacement.getPhysicalMachines());
                        virtualMachines = new ArrayList<>(memeticPlacement.getVirtualMachineList());
                        derivedVMs = new ArrayList<>(memeticPlacement.getDerivedVMs());
                    }
                }
            }
        }
        Float scenarioScored = ObjectivesFunctions.getScenarioScore(revenueByTime, placements, realRevenue);

        //        TODO: Only for debug
        //        System.out.println("************************RESULTS*************************");
        //        System.out.println("Simulated time\t\t\t\t: \t" + timeSimulated);
        //        System.out.println("Unique Virtual Machine\t\t: \t" + DynamicVMP.vmUnique );
        //        System.out.println("Scenario Score\t\t\t\t: \t" + scenarioScored );
        //        System.out.println("Max revenue lost (possible)\t: \t" + maxRevenueLost);
        //        System.out.println("Real revenue lost\t\t\t: \t" + realRevenue[0]);
        //        System.out.println("Request Updated\t\t\t\t: \t" + requestsProcess[2]);
        //        System.out.println("Request Violation\t\t\t: \t" + requestsProcess[3]);
        //        System.out.println("Request Rejected(VM Derived)\t: \t" + requestsProcess[1]);
        //        System.out.println("Request Serviced(VM Allocated)\t: \t" + requestsProcess[0]);
        //        System.out.println("********************************************************\n");

        Utils.printToFile(Constant.POWER_CONSUMPTION_FILE, Utils.getAvgPwConsumptionNormalized(powerByTime));
        Utils.printToFile(Constant.WASTED_RESOURCES_FILE, Utils.getAvgResourcesWNormalized(wastedResourcesRatioByTime));
        Utils.printToFile(Constant.ECONOMICAL_REVENUE_FILE, Utils.getAvgRevenueNormalized(revenueByTime));
        Utils.printToFile(Constant.WASTED_RESOURCES_RATIO_FILE, wastedResources);
        Utils.printToFile(Constant.SCENARIOS_SCORES, scenarioScored);
        Utils.printToFile(Constant.RECONFIGURATION_CALL_TIMES_FILE,"\n");
        Utils.printToFile(Constant.ECONOMICAL_PENALTIES_FILE, DynamicVMP.economicalPenalties);
        Utils.printToFile(Constant.LEASING_COSTS_FILE, DynamicVMP.leasingCosts);

        Utils.executorServiceTermination(executorService);
    }
}
