/*
 * Virtual Machine Placement: Overbooking and Elasticity
 * Author: Rodrigo Ferreira (rodrigofepy@gmail.com)
 * Author: Saúl Zalimben (szalimben93@gmail.com)
 * Author: Leonardo Benitez  (benitez.leonardo.py@gmail.com)
 * Author: Augusto Amarilla (agu.amarilla@gmail.com)
 * 22/8/2016.
 */

package org.dynamicVMP;

import org.domain.*;
import org.dynamicVMP.concurrent.StaticReconfMemeCall;
import org.dynamicVMP.memeticAlgorithm.MASettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DynamicVMP {

    // FILES
    static final String POWER_CONSUMPTION_FILE = Utils.OUTPUT + "power_consumption";
    static final String ECONOMICAL_REVENUE_FILE = Utils.OUTPUT + "economical_revenue";
    static final String WASTED_RESOURCES_FILE = Utils.OUTPUT + "wasted_resources";
    static final String WASTED_RESOURCES_RATIO_FILE = Utils.OUTPUT + "wasted_resources_ratio";
    static final String SCENARIOS_SCORES = Utils.OUTPUT + "scenarios_scores";
    static final String PLACEMENT_SCORE = Utils.OUTPUT + "resources_per_scenario";
    static final String RECONFIGURATION_CALL_TIMES = Utils.OUTPUT + "reconfiguration_call_times";

    // EXPERIMENTS PARAMETERS

    /**
     * Heuristic Algorithm Code
     */
    static String HEURISTIC_CODE;

    /**
     * Physical Machine Configuration
     */
    static String PM_CONFIG;

    /**
     * Penalty for derive VM
     */
    public static Float DERIVE_COST;

    /**
     * FAULT_TOLERANCE: Indicates if it is applying tolerance to failures.
     * <ul>
     *     <li>
     *         <b>True</b>, VMs from same Service cannot be hosted in the same PM
     *     </li>
     *     <li>
     *         <b>False</b>, VMs from same Service can be hosted in the same PM
     *     </li>
     * </ul>
     *
     */
    public static Boolean FAULT_TOLERANCE;

    /**
     * Protection Factor:
     * It can take values between 0 and 1,where
     * <ul>
     *     <li>
     *         0 -> No overbooking
     *     </li>
     *     <li>
     *         1 -> Full Overbooking (high risk of violation of SLA)
     *     </li>
     * </ul>
     */
    public static Float PROTECTION_FACTOR;

    /**
     * Interval Execution Memetic Algorithm
     */
    static Integer INTERVAL_EXECUTION_MEMETIC;

    /**
     * Population Size
     */
    static Integer POPULATION_SIZE;

    /**
     * Number of Generations
     */
    static Integer NUMBER_GENERATIONS;

    /**
     * Execution Duration
     */
    static Integer EXECUTION_DURATION;

    /**
     * Link Capacity in Mbps
     */
    static Float LINK_CAPACITY;

    /**
     * Percentage of overload when migration process is active
     */
    static Float MIGRATION_FACTOR_LOAD;

    /**
     * Time unit duration in seconds
     */
     static final Float TIMEUNIT_DURATION = 1F;

    /**
     * Number of Objective Functions
     */
    static final Integer NUM_OBJ_FUNCT_COMP = 3;

    /**
     * Historical objective functions values size
     */
    static final Integer HISTORIAL_DATA_SIZE = 5;

    /**
     * Map of the Heuristics Algorithm
     * heuristicsMap.put("FF", 0);
     * heuristicsMap.put("BF", 1);
     * heuristicsMap.put("WF", 2);
     * heuristicsMap.put("FFD", 3);
     * heuristicsMap.put("BFD", 4);
     */
    static final Map<String, Integer> heuristicsMap = new HashMap<>();

    public static Integer timeSimulated;
    public static Integer initialTimeUnit;

    public static final String BFD = "BFD";
    public static final String FFD = "FFD";
    public static final String BF = "BF";
    public static final String FF = "FF";
    public static final String WF = "WF";

    /**
     *  Map <VM_ID, Violation> -> Violation per VM, per Time, per Resources
     *  Track resources violation per VM, per Time and per resources <br>
     *  <pre>
     *  violation ->
     *      < virtualMachineId = 1,
     *        < time = 1,
     *          < cpuViolation = X,
     *            ramViolation = X,
     *            netViolation = x >
     *          >
     *        >
     *      >
     * </pre>
     */
    private static Map<Integer, Violation> unsatisfiedResources = new HashMap<>();

    // Apriori values lists by time
    static Map<Integer, Float> REVENUE_APRIORI_TIME = new HashMap<>();
    static Float MAX_REVENUE_LOST = 0F;
    static Map<Integer, Float> MIGRATED_MEMORY_APRIORI_TIME = new HashMap<>();
    static Float MAX_POWER = 0F;
    static Integer VM_UNIQUE = 0;

    public static final String DYNAMIC_VMP = "DynamicVMP";

    private DynamicVMP () {
        // Default Constructor
    }

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
    public static void VMPManager(List<Scenario> workload, List<PhysicalMachine> physicalMachines, List<VirtualMachine>
            virtualMachines, List<VirtualMachine> derivedVMs,
            Map<Integer, Float> revenueByTime, List<Resources> wastedResources,  Map<Integer, Float> wastedResourcesRatioByTime,
            Map<Integer, Float> powerByTime, Map<Integer, Placement> placements, Integer code, Integer timeUnit,
            Integer[] requestsProcess, Float maxPower, Float[] realRevenue, String scenarioFile)
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
        Integer memeticTimeEnd=-1;

        Integer migrationTimeInit=-1;
        Integer migrationTimeEnd=-1;

        Integer vmEndTimeMigration = 0;

        for (int iterator = 0; iterator < workload.size(); ++iterator) {
            Scenario request = workload.get(iterator);
            actualTimeUnit = request.getTime();
	        //check if is the last request, assign -1 to nextTimeUnit if so.
            nextTimeUnit = iterator + 1 == workload.size() ? -1 : workload.get(iterator + 1).getTime();

	        if (nextTimeUnit!= -1 && isMigrationActive && isVmBeingMigrated(request.getVirtualMachineID(),vmsToMigrate)){

                // TODO: Check why this is null in some scenarios
				VirtualMachine vmMigrating = VirtualMachine.getById(request.getVirtualMachineID(),virtualMachines);
                if(vmMigrating != null) {
                    vmEndTimeMigration = Utils.getEndTimeMigrationByVm(vmMigrating.getId(),vmsToMigrate,vmsMigrationEndTimes);
                }

                isUpdateVmUtilization = actualTimeUnit <= vmEndTimeMigration;
            }

            runHeuristics(request, code, physicalMachines, virtualMachines, derivedVMs, requestsProcess, isUpdateVmUtilization);

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

                //check the historical information
                if(nextTimeUnit!=-1 && placements.size()>HISTORIAL_DATA_SIZE &&
                        !isReconfigurationActive && !isMigrationActive ){
                    //collect O.F. historical values
                    valuesSelectedForecast.clear();
                    for(int timeIterator=nextTimeUnit-HISTORIAL_DATA_SIZE; timeIterator<=actualTimeUnit;timeIterator++){
                        valuesSelectedForecast.add(placements.get(timeIterator).getPlacementScore());
                    }

                    //check if a  call for reconfiguration is needed and set the init time
                    if(Utils.callToReconfiguration(valuesSelectedForecast)){
                        Utils.printToFile(RECONFIGURATION_CALL_TIMES,nextTimeUnit);
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
			            Logger.getLogger(DYNAMIC_VMP).log(Level.SEVERE, "Migration Failed!");
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

                    Placement memeticPlacement = updatePlacementAfterReconf(workload, BFD, reconfgPlacementResult, memeticTimeInit,
                            migrationTimeEnd);

                    if(isMememeticPlacementBetter(placements.get(actualTimeUnit), memeticPlacement)) {
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
//        System.out.println("Unique Virtual Machine\t\t: \t" + DynamicVMP.VM_UNIQUE );
//        System.out.println("Scenario Score\t\t\t\t: \t" + scenarioScored );
//        System.out.println("Max revenue lost (possible)\t: \t" + MAX_REVENUE_LOST);
//        System.out.println("Real revenue lost\t\t\t: \t" + realRevenue[0]);
//        System.out.println("Request Updated\t\t\t\t: \t" + requestsProcess[2]);
//        System.out.println("Request Violation\t\t\t: \t" + requestsProcess[3]);
//        System.out.println("Request Rejected(VM Derived)\t: \t" + requestsProcess[1]);
//        System.out.println("Request Serviced(VM Allocated)\t: \t" + requestsProcess[0]);
//        System.out.println("********************************************************\n");

        Utils.printToFileMap(POWER_CONSUMPTION_FILE, powerByTime);
        Utils.printToFileMap(WASTED_RESOURCES_FILE, wastedResourcesRatioByTime);
        Utils.printToFileMap(ECONOMICAL_REVENUE_FILE, revenueByTime);
        Utils.printToFile(WASTED_RESOURCES_RATIO_FILE, wastedResources);
        Utils.printToFile(SCENARIOS_SCORES, scenarioScored);

        Utils.executorServiceTermination(executorService);
    }

    /**
     *
     * @param s One request of Workload Scenario
     * @param code Heuristics Code
     * @param physicalMachines List of Physical Machine
     * @param virtualMachines List of Virtual Machine
     * @param derivedVMs List of Derived Virtual Machine
     * @param requests Requests:
     *        <ul>
     *          <li>Requests[0]: requestServed Number of requests served</li>
     *          <li>Requests[1]: requestRejected Number of requests rejected</li>
     *          <li>Requests[2]: requestUpdated Number of requests updated</li>
     *          <li>Requests[3]: violation Number of violation</li>
     *        </ul>
     */
    public static void runHeuristics (Scenario s, Integer code, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs, Integer[] requests,
            Boolean isMigrationActive) {

        // If current_time is equals to VM tinit, allocated VM
        if (s.getTime() <= s.getTinit()) {
            if (Heuristics.getAlgorithms()[code]
                    .useAlgorithm(s, physicalMachines, virtualMachines, derivedVMs)) {
                requests[0]++;
            } else {
                // Derive Virtual Machine
                requests[1]++;
            }
        } else {
            // Update VM resources
            if (s.getTime() <= s.getTend()) {
                if (Heuristics.updateVM(s, virtualMachines,derivedVMs, physicalMachines, isMigrationActive)) {
                    // If the VM is in another DC, the update will be successful
                    requests[2]++;
                } else {
                    requests[3]++;
                }
            } else {
                Logger.getLogger(DYNAMIC_VMP).log(Level.SEVERE, "WorkloadException!");
            }
        }
    }

    /**
     * Updates Placement After Reconfiguration
     * @param workload            Workload Trace
     * @param heuristicCode       Heuristic Algorithm
     * @param placement           Placement from the memetic algorithm execution
     * @param startTimeMemeticAlg Start time of Memetic Algorithm
     * @param endTimeMemeticAlg   End time of Memetic Algorithm
     * @return Placement after Reconfiguration and with all missed request
     */
    public static Placement updatePlacementAfterReconf (List<Scenario> workload, String heuristicCode, Placement placement,
            Integer startTimeMemeticAlg, Integer endTimeMemeticAlg) {

        Integer code = heuristicsMap.get(heuristicCode);
        Integer[] requestsProcessAfterReconf = initRequestProcess();

        // List of missed requests by Memetic Algorithm order by Revenue (excepts removed VM)
        List<Scenario> cloneScenario = Scenario.cloneScneario(workload, startTimeMemeticAlg, endTimeMemeticAlg);

        cloneScenario.forEach(request ->
            runHeuristics(request, code, placement.getPhysicalMachines(), placement.getVirtualMachineList(),
                    placement.getDerivedVMs(), requestsProcessAfterReconf, false)
        );

        return placement;
    }

    /**
     * <b>Parameter file structure:</b>
     * <ul>
     *  <li>
     *      args[0]: Heuristics Code
     *      <ul>
     *          <li>- FF -> First Fit</li>
     *          <li>- BF -> Best Fit </li>
     *          <li>- WF -> Worst Fit </li>
     *          <li>- FFD -> First Fit Decreasing</li>
     *          <li>- BFD -> Best Fit Decreasing</li>
     *      </ul>
     *  </li>
     *  <li>
     *      args[1]: PMConfig file
     *  </li>
     *  <li>
     *      args[2 ... n]: Scenario files
     *  </li>
     * </ul>
     * If you want to add more parameters, changes the {@link DynamicVMP#loadParameters(ArrayList, String)}
     */
    public static void main (String[] args) throws IOException, InterruptedException, ExecutionException {

        ArrayList<String> scenariosFiles  = new ArrayList<>();
        loadParameters(scenariosFiles, args[0]);

        initHeuristicMap();
        Logger.getLogger(DYNAMIC_VMP).log(Level.INFO, "STARTING EXPERIMENTS");

        for (String scenarioFile : scenariosFiles) {
            // TODO: For debug only
            System.out.println(scenarioFile);
            launchExperiments(HEURISTIC_CODE, PM_CONFIG, scenarioFile);
        }
        Logger.getLogger(DYNAMIC_VMP).log(Level.INFO, "ENDING EXPERIMENTS");

    }

    /**
     * @param scenariosFiles Scenarios Files
     * @throws IOException
     */
    private static void loadParameters(ArrayList<String> scenariosFiles, String file) throws IOException {

        try (Stream<String> stream = Files.lines(Paths.get(file))) {
            Utils.loadParameter(scenariosFiles, stream);
        } catch (IOException e) {
            Logger.getLogger(DynamicVMP.DYNAMIC_VMP).log(Level.SEVERE, "Error trying to load experiments parameters.");
            throw e;
        }
    }

    /**
     * @param heuristicCode Heuristic Code
     * @param pmConfig      Physical Machine Configuration
     * @param scenarioFile  Scenario File
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private static void launchExperiments(final String heuristicCode, final String pmConfig, String scenarioFile)
            throws IOException, InterruptedException, ExecutionException {
        // VARIABLES
        List<PhysicalMachine> physicalMachines = new ArrayList<>();
        List<Scenario> scenarios = new ArrayList<>();
        List<VirtualMachine> virtualMachines = new ArrayList<>();
        List<VirtualMachine> derivedVMs = new ArrayList<>();

        Integer[] requestsProcess = initRequestProcess();
        Float[] realRevenue = new Float[]{0F};

        Files.write(Paths.get(scenarioFile), scenarioFile.toString().getBytes(), StandardOpenOption
                .CREATE);

        // LIST
        List<Resources> wastedResources = new ArrayList<>();
        Map<Integer, Float> wastedResourcesRatioByTime = new HashMap<>();
        Map<Integer, Float> powerByTime = new HashMap<>();
        Map<Integer, Float> revenueByTime = new HashMap<>();
        Map<Integer, Placement> placements = new HashMap<>();

        MAX_POWER = Utils.loadDatacenter(pmConfig, scenarioFile, physicalMachines, scenarios);
        timeSimulated = scenarios.get(scenarios.size() - 1).getTime();
        Integer code = heuristicsMap.get(heuristicCode);

        Integer timeUnit = scenarios.get(0).getTime();
        initialTimeUnit = timeUnit;
        timeAdjustment(wastedResources, wastedResourcesRatioByTime, powerByTime, revenueByTime, scenarioFile);

        loadAprioriValuesByTime(scenarios);

        // Prepare scenario for Decreasing Algorithms (Sort by Total Revenue)
        if (BFD.equals(heuristicCode) || FFD.equals(heuristicCode)) {
            Collections.sort(scenarios);
        }

        VMPManager(scenarios, physicalMachines, virtualMachines, derivedVMs,
                revenueByTime, wastedResources, wastedResourcesRatioByTime, powerByTime,
                placements, code, timeUnit, requestsProcess, MAX_POWER, realRevenue, scenarioFile
        );
    }

    /**
     * @param wastedResources            Wasted Resources List
     * @param wastedResourcesRatioByTime Wasted Resources List per time t
     * @param powerByTime                Power Consumption per time t
     * @param revenueByTime              Economical Revenue per time yt
     */
    private static void timeAdjustment(List<Resources> wastedResources,
            Map<Integer, Float> wastedResourcesRatioByTime, Map<Integer, Float> powerByTime,
            Map<Integer, Float> revenueByTime, String scenarioFile) throws IOException {

        Integer timeAdjust = 0;
        if(initialTimeUnit != 0 ) {
            while (timeAdjust < initialTimeUnit) {
                powerByTime.put(timeAdjust, 0F);
                wastedResources.add(new Resources());
                wastedResourcesRatioByTime.put(timeAdjust, 0F);
                revenueByTime.put(timeAdjust, 0F);
                Utils.printToFile(scenarioFile, 0);
                timeAdjust++;
                timeSimulated += 1;
            }
        }
    }

    /**
     * Load the objective function's  a priori values from the scenario
     * @param workload List of Scenarios
     */
    public static void loadAprioriValuesByTime(List<Scenario> workload){

        Map<Integer, Float> revenueAPrioriByTime = new HashMap<>();
        Map<Integer, Float> migratedMemoryAPrioriByTime = new HashMap<>();
        MAX_REVENUE_LOST = 0F;
        Integer numberUniqueVm = 0;
        Float revenueAPriori=0F;
        Float migratedMemoryAPriori = 0F;
        Integer timeAdjust = 0;

        if(initialTimeUnit != 0 ) {
            while (timeAdjust < initialTimeUnit) {
                revenueAPrioriByTime.put(timeAdjust, 0F);
                migratedMemoryAPrioriByTime.put(timeAdjust, 0F);
                timeAdjust++;
            }
        }

        for( int iteratroScenario=0; iteratroScenario<workload.size(); iteratroScenario++){

            Scenario request = workload.get(iteratroScenario);
            revenueAPriori += request.getRevenue().getCpu() * request.getResources().getCpu();
            revenueAPriori += request.getRevenue().getRam() * request.getResources().getRam();
            revenueAPriori += request.getRevenue().getNet() * request.getResources().getNet();

	        migratedMemoryAPriori+= request.getResources().getRam();

            if(request.getTime() <= request.getTinit()) {
                numberUniqueVm++;
            }

            if((iteratroScenario + 1) == workload.size() || !request.getTime().equals(workload.get(iteratroScenario + 1).getTime())){
                revenueAPrioriByTime.put(request.getTime(), revenueAPriori);
                MAX_REVENUE_LOST += revenueAPriori;
                migratedMemoryAPrioriByTime.put(request.getTime(), migratedMemoryAPriori);
                revenueAPriori = 0F;
                migratedMemoryAPriori = 0F;
            }
        }

        REVENUE_APRIORI_TIME = revenueAPrioriByTime;
        MIGRATED_MEMORY_APRIORI_TIME = migratedMemoryAPrioriByTime;
        VM_UNIQUE = numberUniqueVm;

    }

    /**
     *
     * @param heuristicPlacement Heuristic Placement
     * @param memeticPlacement Memetic Placement
     * @return <b>True</b>, is Memetic Placement is better than Heuristic Placement <br> <b>False</b>, otherwise
     */
    private static Boolean isMememeticPlacementBetter(Placement heuristicPlacement, Placement memeticPlacement) {

        Boolean isBetter = false;
        if(memeticPlacement == null) {
            return false;
        }

        int compare = heuristicPlacement.getPlacementScore().compareTo(memeticPlacement.getPlacementScore());

        // If placements are equals.
        if (compare == 0) {
            isBetter = false;
        }

        // If Heuristics Placement have better score
        if (compare < 0) {
            isBetter = false;
        }

        // If Memetic Placement have better score
        if (compare > 0) {
            isBetter = true;
        }

        return isBetter;
    }

    private static Integer[] initRequestProcess() {

        Integer[] requestsProcess = new Integer[4];
        requestsProcess[0] = 0;    // Requests Served
        requestsProcess[1] = 0;    // Requests Rejected
        requestsProcess[2] = 0;    // Requests Updated
        requestsProcess[3] = 0;    // Requests Violated
        return requestsProcess;
    }

    private static void initHeuristicMap() {
        heuristicsMap.put("FF", 0);
        heuristicsMap.put("BF", 1);
        heuristicsMap.put("WF", 2);
        heuristicsMap.put("FFD", 3);
        heuristicsMap.put("BFD", 4);
    }

    /**
     *
     * @return Unsatisfied Resources
     */
    public static Map<Integer, Violation> getUnsatisfiedResources() {

        return unsatisfiedResources;
    }

    public static Boolean isVmBeingMigrated(Integer virtualMachineId, List<VirtualMachine> vmsToMigrate){
		Integer iteratorVM;
	    VirtualMachine vmMigrated;
	    for(iteratorVM=0;iteratorVM<vmsToMigrate.size();iteratorVM++){
			vmMigrated = vmsToMigrate.get(iteratorVM);
			if(vmMigrated.getId().equals(virtualMachineId)) {
				return true;
			}
		}
		return false;
    }

}

