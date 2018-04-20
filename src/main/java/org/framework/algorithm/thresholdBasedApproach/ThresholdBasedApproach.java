package org.framework.algorithm.thresholdBasedApproach;

import org.domain.*;
import org.framework.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.domain.VirtualMachine.getById;

/**
 *
 * <b>Algorithm 2: Threshold Based Approach</b>
 * <p>
 *     Migration is launching when datacenter is underloaded or overloaded .
 * </p>
 *
 * @author Saul Zalimben.
 * @since 12/31/16.
 */
public class ThresholdBasedApproach {

    private ThresholdBasedApproach() {
        // Default Constructor
    }

    /**
     * VMPManager
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
    public static void thresholdBasedApproachManager(List<Scenario> workload, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine>
            virtualMachines, List<VirtualMachine> derivedVMs,
            Map<Integer, Float> revenueByTime, List<Resources> wastedResources,  Map<Integer, Float> wastedResourcesRatioByTime,
            Map<Integer, Float> powerByTime, Map<Integer, Placement> placements, Integer code, Integer timeUnit,
            Integer[] requestsProcess, Float maxPower, String scenarioFile)
            throws IOException, InterruptedException, ExecutionException {

        List<VirtualMachine> vmsToMigrateFromPM = new ArrayList<>();
        List<VirtualMachine> vmsToMigrate = new ArrayList<>();
        List<Integer> vmsMigrationEndTimes = new ArrayList<>();
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Boolean isMigrationActive = false;
        Boolean isUpdateVmUtilization = false;
        Integer actualTimeUnit;
        Integer nextTimeUnit;
        Integer migrationTimeEnd=-1;
        Integer vmEndTimeMigration = 0;

        Integer heuristicCode = Constant.HEURISTIC_MAP.get(Constant.FFD);

        for (int iterator = 0; iterator < workload.size(); ++iterator) {
            Scenario request = workload.get(iterator);
            actualTimeUnit = request.getTime();
            //check if is the last request, assign -1 to nextTimeUnit if so.
            nextTimeUnit = iterator + 1 == workload.size() ? -1 : workload.get(iterator + 1).getTime();

            //check if the request corresponds to a vm that is being migrated
            if (nextTimeUnit!= -1 && isMigrationActive && DynamicVMP.isVmBeingMigrated(request.getVirtualMachineID(),
                request.getCloudServiceID(), vmsToMigrate)) {

                VirtualMachine vmMigrating = getById(request.getVirtualMachineID(), request.getCloudServiceID(),
                    virtualMachines);
                vmEndTimeMigration = Utils.updateVmEndTimeMigration(vmsToMigrate, vmsMigrationEndTimes,
                        vmEndTimeMigration,
                        vmMigrating);

                /* Check the migration time of the vm considered. If the vm is being migrated, a utilization overhead
                 * is added.
                 */
                isUpdateVmUtilization = actualTimeUnit <= vmEndTimeMigration;
            }

            DynamicVMP.runHeuristics(request, code, physicalMachines, virtualMachines, derivedVMs, requestsProcess,
                     isUpdateVmUtilization);

            // check if it's the last request or will occur a variation of time unit.
            if (nextTimeUnit == -1 || !actualTimeUnit.equals(nextTimeUnit)) {
                ObjectivesFunctions.getObjectiveFunctionsByTime(physicalMachines,
                        virtualMachines, derivedVMs, wastedResources,
                        wastedResourcesRatioByTime, powerByTime, revenueByTime, timeUnit, actualTimeUnit);

                Float placementScore = ObjectivesFunctions.getDistanceOrigenByTime(request.getTime(),
                        maxPower, powerByTime, revenueByTime, wastedResourcesRatioByTime);

                DynamicVMP.updateLeasingCosts(derivedVMs);
                // Print the Placement Score by Time t
//                Utils.checkPathFolders(Constant.PLACEMENT_SCORE_BY_TIME_FILE);
                // Print the Placement Score by Time t
//                Utils.printToFile( Constant.PLACEMENT_SCORE_BY_TIME_FILE + scenarioFile + Constant.EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME, placementScore);

                timeUnit = actualTimeUnit;

                Placement heuristicPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
                        VirtualMachine.cloneVMsList(virtualMachines),
                        VirtualMachine.cloneVMsList(derivedVMs), placementScore);
                placements.put(actualTimeUnit, heuristicPlacement);

                //if an operation of virtual machines migration is not active, then
                // check the state of the physical machines
                if(nextTimeUnit!=-1 && !isMigrationActive) {

                    //contains the virtual machines allocated to a particular physical machine.
                    List<VirtualMachine> vmsInPM;

                    for (PhysicalMachine pm : physicalMachines) {
                        vmsInPM = Utils.filterVMsByPM(virtualMachines, pm.getId());
                        if (Constraints.isPMOverloaded(pm) && !vmsInPM.isEmpty()) {
                            vmsToMigrateFromPM.clear();
                            //the physical machine is overloaded, select the vms to migrate from this pm
                            vmsToMigrateFromPM = Utils.getVMsToMigrate(pm,vmsInPM);
                            //move virtual machines selected
                            DynamicVMP.runHeuristics(heuristicCode,physicalMachines,virtualMachines,derivedVMs,
                                     vmsToMigrateFromPM);
                            //add virtual machines selected to a list of migration
                            vmsToMigrate.addAll(vmsToMigrateFromPM);
                        } else if (Constraints.isPMUnderloaded(pm) && !vmsInPM.isEmpty()) {
                            vmsToMigrateFromPM.clear();
                            //the physical machine is underloaded, move all the virtual machine from this pm
                            vmsToMigrateFromPM.addAll(vmsInPM);
                            //move virtual machines
                            DynamicVMP.runHeuristics(heuristicCode,physicalMachines,virtualMachines,
                                    derivedVMs,vmsToMigrateFromPM);
                            //add virtual machines to a list of migration
                            vmsToMigrate.addAll(vmsToMigrateFromPM);
                        }
                    }

                    if(!vmsToMigrate.isEmpty()){
                        //if the are virtual machines to migrate, then the migrations times for each vm to migrate
                        //is obtained
                        vmsMigrationEndTimes  = Utils.getTimeEndMigrationByVM(vmsToMigrate,actualTimeUnit);
                        //the final time for migration
                        migrationTimeEnd = Utils.getMigrationEndTime(vmsMigrationEndTimes);
                        isMigrationActive = true;
                    }else{
                        //clean migration variables
                        vmsToMigrate.clear();
                        vmsMigrationEndTimes.clear();
                        isMigrationActive = false;
                    }

                }else if(nextTimeUnit!=-1 && actualTimeUnit.equals(migrationTimeEnd)){
                    //clean migration variables
                    vmsToMigrate.clear();
                    vmsMigrationEndTimes.clear();
                    isMigrationActive = false;
                }
            }
        }
        Utils.executorServiceTermination(executorService);
    }

}
