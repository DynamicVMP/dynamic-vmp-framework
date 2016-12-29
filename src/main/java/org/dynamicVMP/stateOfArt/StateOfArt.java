package org.dynamicVMP.stateOfArt;

import org.domain.*;
import org.dynamicVMP.*;
import org.dynamicVMP.concurrent.StaticReconfMemeCall;
import org.dynamicVMP.memeticAlgorithm.MASettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Saul Zalimben.
 * @since 12/29/16.
 */
public class StateOfArt {

    public static final String DYNAMIC_VMP_STATE_OF_ART = "DynamicVMP: State of Art";

    public static void stateOfArtManager(List<Scenario> workload, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine>
            virtualMachines, List<VirtualMachine> derivedVMs,
            Map<Integer, Float> revenueByTime, List<Resources> wastedResources,  Map<Integer, Float> wastedResourcesRatioByTime,
            Map<Integer, Float> powerByTime, Map<Integer, Placement> placements, Integer code, Integer timeUnit,
            Integer[] requestsProcess, Float maxPower, Float[] realRevenue, String scenarioFile)
            throws IOException, InterruptedException, ExecutionException {

        List<APrioriValue> aPrioriValuesList = new ArrayList<>();
        List<VirtualMachine> vmsToMigrate = new ArrayList<>();
        List<Integer> vmsMigrationEndTimes = new ArrayList<>();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        MASettings memeConfig = Utils.getMemeConfig(true);
        Callable<Placement> staticReconfgTask;
        Future<Placement> reconfgResult = null;
        Placement reconfgPlacementResult = null;

        Boolean isMigrationActive = false;
        Boolean isUpdateVmUtilization = false;
        Integer actualTimeUnit;
        Integer nextTimeUnit;

        Integer memeticTimeInit = timeUnit + memeConfig.getExecutionFirstTime();
        Integer memeticTimeEnd=-1;

        Integer migrationTimeInit =- 1;
        Integer migrationTimeEnd =- 1;

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

            DynamicVMP.runHeuristics(request, code, physicalMachines, virtualMachines, derivedVMs, requestsProcess,
                    isUpdateVmUtilization);

            // check if its the last request or a variation of time unit will occurs.
            if (nextTimeUnit == -1 || !actualTimeUnit.equals(nextTimeUnit)) {
                ObjectivesFunctions.getObjectiveFunctionsByTime(physicalMachines,
                        virtualMachines, derivedVMs, wastedResources,
                        wastedResourcesRatioByTime, powerByTime, revenueByTime, timeUnit, actualTimeUnit);

                Float placementScore = ObjectivesFunctions.getDistanceOrigenByTime(request.getTime(),
                        maxPower, powerByTime, revenueByTime, wastedResourcesRatioByTime);

                //        TODO: Only for debug
                Utils.printToFile(scenarioFile, placementScore);

                timeUnit = actualTimeUnit;

                Placement heuristicPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
                        VirtualMachine.cloneVMsList(virtualMachines),
                        VirtualMachine.cloneVMsList(derivedVMs), placementScore);
                placements.put(actualTimeUnit, heuristicPlacement);

                if(nextTimeUnit!=-1 && nextTimeUnit.equals(memeticTimeInit)){

                    // If a new VM request cames while memetic execution, memetic algorithm is cancel.
                    if (StateOfArtUtils.newVmDuringMemeticExecution(workload, memeticTimeInit, memeticTimeInit +
                            memeConfig
                            .getExecutionDuration())) {
                        memeticTimeInit = memeticTimeInit + memeConfig.getExecutionInterval();
                    } else {

                        if (virtualMachines.size() != 0) {
                            // Clone the current placement
                            Placement memeticPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
                                    VirtualMachine.cloneVMsList(virtualMachines),
                                    VirtualMachine.cloneVMsList(derivedVMs));

                            //get the list of a priori values
                            aPrioriValuesList = Utils.getAprioriValuesList(actualTimeUnit);
                            //config the call for the memetic algorithm
                            staticReconfgTask = new StaticReconfMemeCall(memeticPlacement, aPrioriValuesList,
                                    memeConfig);
                            //call the memetic algorithm in a separate thread
                            reconfgResult = executorService.submit(staticReconfgTask);
                            //update the time end of the memetic algorithm execution
                            memeticTimeEnd = memeticTimeInit + memeConfig.getExecutionDuration();
                            //update the migration init time
                            migrationTimeInit = memeticTimeEnd + 1;

                        } else {
                            migrationTimeInit += 1;
                        }
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
                            //update the memetic algorithm init time
                            memeticTimeInit = memeticTimeEnd + memeConfig.getExecutionInterval();
                            //update the migration init
                            memeticTimeInit  += memeConfig.getExecutionInterval();

                        }
                    } catch (ExecutionException e) {
                        Logger.getLogger(DYNAMIC_VMP_STATE_OF_ART).log(Level.SEVERE, "Migration Failed!");
                        throw e;
                    }

                } else if(nextTimeUnit != -1 && actualTimeUnit.equals(migrationTimeEnd)) {
					/* get here the new virtual machines to insert in the placement generated by the
		            memetic algorithm using Best Fit Decreasing */
                    isMigrationActive = false;

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

        Utils.printToFile(Constant.POWER_CONSUMPTION_FILE, Utils.getAvgPwConsumptionNormalized(powerByTime));
        Utils.printToFile(Constant.WASTED_RESOURCES_FILE, Utils.getAvgResourcesWNormalized(wastedResourcesRatioByTime));
        Utils.printToFile(Constant.ECONOMICAL_REVENUE_FILE, Utils.getAvgRevenueNormalized(revenueByTime));
        Utils.printToFile(Constant.WASTED_RESOURCES_RATIO_FILE, wastedResources);
        Utils.printToFile(Constant.SCENARIOS_SCORES, scenarioScored);
        Utils.printToFile(Constant.RECONFIGURATION_CALL_TIMES_FILE,"\n");
        Utils.printToFile(Constant.ECONOMICAL_PENALTIES_FILE, DynamicVMP.ECONOMICAL_PENALTIES);
        Utils.printToFile(Constant.LEASING_COSTS_FILE, DynamicVMP.LEASING_COSTS);

        Utils.executorServiceTermination(executorService);
    }



}
