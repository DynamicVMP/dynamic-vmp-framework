package org.framework.algorithm.cleverReconfiguration;

import org.domain.*;
import org.framework.*;
import org.framework.algorithm.stateOfArt.StateOfArtUtils;
import org.framework.reconfigurationAlgorithm.acoAlgorithm.AcoCall;
import org.framework.reconfigurationAlgorithm.concurrent.StaticReconfMemeCall;
import org.framework.reconfigurationAlgorithm.memeticAlgorithm.MASettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.domain.VirtualMachine.getById;

/**
 * <b>Algorithm 3: Recalculation-based VMPr Recovering</b>
 * <p>
 *     Recovering method based on partially recalculating the placement reconfiguration calculated by the VMPr,
 *     according to the changes that happened during the placement calculation,
 *     applying basic operations to update the potentially obsolete placement.
 * </p>
 *
 * @author Saul Zalimben
 * @since 12/26/16
 */
public class CleverReconfiguration {

    private static Logger logger = DynamicVMP.getLogger();

    private CleverReconfiguration() {
        // Default Constructor
    }

    /**
     * Clever Reconfiguration VMP
     * @param workload                   Workload Trace
     * @param physicalMachines           List of Physical Machines
     * @param virtualMachines            List of Virtual Machines
     * @param derivedVMs                 List of Derived Virtual Machines
     * @param revenueByTime              Revenue by time
     * @param wastedResources            WastedResources by time
     * @param wastedResourcesRatioByTime WastedResourcesRatio per time
     * @param powerByTime                Power Consumption by time
     * @param placements                 List of Placement by time
     * @param code                       Heuristics Algorithm Code
     * @param timeUnit                   Time init
     * @param requestsProcess            Type of Process
     * @param maxPower                   Maximum Power Consumption
     * @param scenarioFile               Name of Scenario
     *
     * <b>RequestsProcess</b>:
     *  <ul>
     *      <li>Requests[0]: requestServed Number of requests served</li>
     *      <li>Requests[1]: requestRejected Number of requests rejected</li>
     *      <li>Requests[2]: requestUpdated Number of requests updated</li>
     *      <li>Requests[3]: violation Number of violation</li>
     *  </ul>
     *
     * @throws IOException          Error managing files
     * @throws InterruptedException Multi-thread error
     * @throws ExecutionException   Multi-thread error
     */
    public static void cleverReconfigurationgManager(List<Scenario> workload, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine>
            virtualMachines, List<VirtualMachine> derivedVMs,
            Map<Integer, Float> revenueByTime, List<Resources> wastedResources,  Map<Integer, Float> wastedResourcesRatioByTime,
            Map<Integer, Float> powerByTime, Map<Integer, Placement> placements, Integer code, Integer timeUnit,
            Integer[] requestsProcess, Float maxPower, String scenarioFile)
            throws IOException, InterruptedException, ExecutionException {

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
        Integer memeticTimeEnd;

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

                VirtualMachine vmMigrating = getById(request.getVirtualMachineID(),virtualMachines);
                vmEndTimeMigration = Utils.updateVmEndTimeMigration(vmsToMigrate, vmsMigrationEndTimes,
                        vmEndTimeMigration,
                        vmMigrating);

                isUpdateVmUtilization = actualTimeUnit <= vmEndTimeMigration;
            }

            DynamicVMP.runHeuristics(request, code, physicalMachines, virtualMachines, derivedVMs, requestsProcess, isUpdateVmUtilization);

            // Check if its the last request or a variation of time unit will occurs.
            if (nextTimeUnit == -1 || !actualTimeUnit.equals(nextTimeUnit)) {

                ObjectivesFunctions.getObjectiveFunctionsByTime(physicalMachines,
                        virtualMachines, derivedVMs, wastedResources,
                        wastedResourcesRatioByTime, powerByTime, revenueByTime, timeUnit, actualTimeUnit);

                Float placementScore = ObjectivesFunctions.getDistanceOrigenByTime(request.getTime(),
                        maxPower, powerByTime, revenueByTime, wastedResourcesRatioByTime);

                DynamicVMP.updateLeasingCosts(derivedVMs);

                // Print the Placement Score by Time t
                Utils.printToFile( Utils.OUTPUT + Utils.PLACEMENT_SCORE_BY_TIME + scenarioFile + Constant.EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME, placementScore);

                timeUnit = actualTimeUnit;

                Placement heuristicPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
                        VirtualMachine.cloneVMsList(virtualMachines),
                        VirtualMachine.cloneVMsList(derivedVMs), placementScore);
                placements.put(actualTimeUnit, heuristicPlacement);

                // Check the historical information
                if(nextTimeUnit!=-1 && placements.size() > Parameter.HISTORICAL_DATA_SIZE &&
                        !isReconfigurationActive && !isMigrationActive ){

                    // Collect O.F. historical values
                    valuesSelectedForecast.clear();
                    for(int timeIterator = nextTimeUnit - Parameter.HISTORICAL_DATA_SIZE; timeIterator<=actualTimeUnit;
                            timeIterator++){
                        if(placements.get(timeIterator)!=null){
                            valuesSelectedForecast.add(placements.get(timeIterator).getPlacementScore());
                        }else{
                            valuesSelectedForecast.add(0F);
                        }
                    }

                    // Check if a  call for reconfiguration is needed and set the init time
                    if(Utils.callToReconfiguration(valuesSelectedForecast, Parameter.FORECAST_SIZE)){
                        Utils.printToFile(Constant.RECONFIGURATION_CALL_TIMES_FILE + Constant.EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME,nextTimeUnit);
                        memeticTimeInit = nextTimeUnit;
                        isReconfigurationActive=true;
                    }else{
                        memeticTimeInit=-1;
                    }
                }

                // Take a snapshot of the current placement to launch reconfiguration
                if(nextTimeUnit!=-1 && nextTimeUnit.equals(memeticTimeInit)){

                    // If a new VM request cames while memetic execution, memetic algorithm is cancel.
                    if (Parameter.RECOVERING_METHOD.equals(Utils.CANCELLATION) &&
                            StateOfArtUtils.newVmDuringMemeticExecution(workload, memeticTimeInit, memeticTimeInit +
                                    memeConfig.getExecutionDuration())) {
                        memeticTimeInit = memeticTimeInit + memeConfig.getExecutionInterval();
                    }else {

                        memeticTimeInit = nextTimeUnit;
                        if (!virtualMachines.isEmpty()) {

                            // Get the list of a priori values
                            aPrioriValuesList = Utils.getAprioriValuesList(actualTimeUnit);

                            // Clone the current placement
                            Placement memeticPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
                                    VirtualMachine.cloneVMsList(virtualMachines),
                                    VirtualMachine.cloneVMsList(derivedVMs));


                            // Get the VMPr algorithm task
                            if (Parameter.VMPR_ALGORITHM.equals("MEMETIC")) {
                                // Config the call for the memetic algorithm
                                staticReconfgTask = new StaticReconfMemeCall(memeticPlacement, aPrioriValuesList,
                                        memeConfig);
                            } else {
                                staticReconfgTask = new AcoCall(memeticPlacement, aPrioriValuesList, Utils.getAcoSettings());
                            }

                            // Call the memetic algorithm in a separate thread
                            reconfgResult = executorService.submit(staticReconfgTask);

                            // Update the time end of the memetic algorithm execution

                            memeticTimeEnd = memeticTimeInit + memeConfig.getExecutionDuration();

                            // Update the migration init time
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

                        }
                    } catch (ExecutionException e) {
                        logger.log(Level.SEVERE, "Migration Failed!");
                        throw e;
                    }

                } else if(nextTimeUnit != -1 && actualTimeUnit.equals(migrationTimeEnd)) {
					/* Get here the new virtual machines to insert in the placement generated by the
		             * memetic algorithm using Best Fit Decreasing
		             */
                    // Set Migration and Reconfiguration Active
                    isMigrationActive = false;
                    isReconfigurationActive = false;

                    // Update de virtual machine list of the placement after the migration operation (remove VMs)
                    Utils.removeDeadVMsFromPlacement(reconfgPlacementResult,actualTimeUnit,memeConfig.getNumberOfResources());

                    /* Update de virtual machine list of the placement after the migration operation, update VMs
                     * resources and add new VMs
                     */
                    Placement memeticPlacement = DynamicVMP.updatePlacementAfterReconf(workload, Constant.BFD,
                            reconfgPlacementResult,
                            memeticTimeInit,
                            migrationTimeEnd);

                    // Update the placement score after filtering dead  virtual machines.
                    reconfgPlacementResult.updatePlacementScore(aPrioriValuesList);

                    if(DynamicVMP.isMememeticPlacementBetter(placements.get(actualTimeUnit), memeticPlacement)) {
                        physicalMachines = new ArrayList<>(memeticPlacement.getPhysicalMachines());
                        virtualMachines = new ArrayList<>(memeticPlacement.getVirtualMachineList());
                        derivedVMs = new ArrayList<>(memeticPlacement.getDerivedVMs());
                    }
                }
            }
        }
        Utils.executorServiceTermination(executorService);
    }
}
