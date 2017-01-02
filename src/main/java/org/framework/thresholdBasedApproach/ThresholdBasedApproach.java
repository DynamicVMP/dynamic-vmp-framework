package org.framework.thresholdBasedApproach;

import org.domain.*;
import org.framework.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Saul Zalimben.
 * @since 12/31/16.
 */
public class ThresholdBasedApproach {

    public static final String DYNAMIC_VMP_THRESHOLD_BASED = "DynamicVMP: Threshold Based";

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
    public static void thresholdBasedApproachManager(List<Scenario> workload, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine>
            virtualMachines, List<VirtualMachine> derivedVMs,
            Map<Integer, Float> revenueByTime, List<Resources> wastedResources,  Map<Integer, Float> wastedResourcesRatioByTime,
            Map<Integer, Float> powerByTime, Map<Integer, Placement> placements, Integer code, Integer timeUnit,
            Integer[] requestsProcess, Float maxPower, Float[] realRevenue, String scenarioFile)
            throws IOException, InterruptedException, ExecutionException {

        System.out.println(DYNAMIC_VMP_THRESHOLD_BASED);

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
                    vmsToMigrate)){

                VirtualMachine vmMigrating = VirtualMachine.getById(request.getVirtualMachineID(),virtualMachines);
                if(vmMigrating != null) {
                    vmEndTimeMigration = Utils.getEndTimeMigrationByVm(vmMigrating.getId(),vmsToMigrate,vmsMigrationEndTimes);
                }
                //check the migration time of the vm considered. If the vm is being migrated, a utilization overhead is added.
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

                //        TODO: Only for debug
                Utils.printToFile(scenarioFile, placementScore);

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
                        if (Constraints.isPMOverloaded(pm)) {
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
