/*
 * Virtual Machine Placement: Overbooking and Elasticity
 * Author: Rodrigo Ferreira (rodrigofepy@gmail.com)
 * Author: Saúl Zalimben (szalimben93@gmail.com)
 * Author: Leonardo Benitez  (benitez.leonardo.py@gmail.com)
 * Author: Augusto Amarilla (agu.amarilla@gmail.com)
 * 22/8/2016.
 */

package org.framework;

import org.domain.*;
import org.framework.algorithm.cleverReconfiguration.CleverReconfiguration;
import org.framework.algorithm.periodicMigration.PeriodicMigration;
import org.framework.algorithm.stateOfArt.StateOfArt;
import org.framework.algorithm.thresholdBasedApproach.ThresholdBasedApproach;
import org.framework.iterativeAlgorithm.Heuristics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DynamicVMP {

    /**
     * Algorithm Interface
     */
    @FunctionalInterface
    interface Algorithm {
        void useAlgorithm(List<Scenario> workload, List<PhysicalMachine> physicalMachines,
                List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs,
                Map<Integer, Float> revenueByTime, List<Resources> wastedResources,  Map<Integer, Float> wastedResourcesRatioByTime,
                Map<Integer, Float> powerByTime, Map<Integer, Placement> placements, Integer code, Integer timeUnit,
                Integer[] requestsProcess, Float maxPower, String scenarioFile)
                throws IOException, InterruptedException, ExecutionException;
    }

    /**
     * List of Heuristics Algorithms
     */
    private static final Algorithm[] algorithms;
    static {
        algorithms = new Algorithm[]{
                PeriodicMigration::periodicMigrationManager,            // Alg0
                StateOfArt::stateOfArtManager,                          // Alg1
                ThresholdBasedApproach::thresholdBasedApproachManager,  // Alg2
                CleverReconfiguration::cleverReconfigurationgManager,   // Alg3
        };
    }

    public static final String DYNAMIC_VMP = "DynamicVMP";
    private static Logger logger = Logger.getLogger(DYNAMIC_VMP);

    public static Integer timeSimulated;
    public static Integer initialTimeUnit;
    public static Integer vmUnique = 0;

    /**
     * A priori Values
     */
    public static Float maxPower = 0F;
    public static Float maxRevenueLost = 0F;
    public static Float economicalPenalties = 0F;
    public static Float leasingCosts = 0F;

    /**
     * Apriori values lists by time
     */
    static Map<Integer, Float> revenueAprioriTime = new HashMap<>();
    static Map<Integer, Float> migratedMemoryAprioriTime = new HashMap<>();
    private static Map<Integer, Float> economicalPenaltiesAprioriTime = new HashMap<>();
    private static List<Float> leasingCostsApriori = new ArrayList<>();

    /**
     *  Map {VM_ID, Violation} = Violation per VM, per Time, per Resources
     *  Track resources violation per VM, per Time and per resources <br>
     *  <pre>
     *  violation {
     *      { virtualMachineId = 1,
     *        { time = 1,
     *          { cpuViolation = X,
     *            ramViolation = X,
     *            netViolation = x }
     *          }
     *        }
     *      }
     * </pre>
     */
    public static Map<String, Violation> unsatisfiedResources = new HashMap<>();

    private DynamicVMP () {
        // Default Constructor
    }

    /**
     * If
     * Add new VMs request to the Datacenter <br>
     *
     *
     *
     * @param s                One request of Workload Scenario
     * @param code             Heuristics Code
     * @param physicalMachines List of Physical Machine
     * @param virtualMachines  List of Virtual Machine
     * @param derivedVMs       List of Derived Virtual Machine
     * @param requests         Requests:
     *        <ul>
     *          <li>Requests[0]: requestServed Number of requests served</li>
     *          <li>Requests[1]: requestRejected Number of requests rejected</li>
     *          <li>Requests[2]: requestUpdated Number of requests updated</li>
     *          <li>Requests[3]: violation Number of violation</li>
     *        </ul>
     * @param isMigrationActive Is migration active
     */
    public static void runHeuristics (Scenario s, Integer code, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs, Integer[] requests,
            Boolean isMigrationActive) {

        // By default Physical Machine is null
        VirtualMachine vm = new VirtualMachine(s.getVirtualMachineID(), s.getResources(), s.getRevenue(),
                s.getTinit(), s.getTend(), s.getUtilization(),
                s.getDatacenterID(), s.getCloudServiceID(), null);

        // If current_time is equals to VM tinit, allocated VM
        if (s.getTime() <= s.getTinit()) {
            if (Heuristics.getHeuristics()[code]
                    .useHeuristic(vm, physicalMachines, virtualMachines, derivedVMs)) {
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
                logger.log(Level.SEVERE, "WorkloadException!");
            }
        }
    }

    /**
     * @param code             Heuristic Code
     * @param physicalMachines List of Physical Machines
     * @param virtualMachines  List of allocated Virtual Machines
     * @param derivedVMs       List of derived Virtual Machines
     * @param vmToMigrate      List of VM to migrate
     */
    public static void runHeuristics (Integer code, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs,
            List<VirtualMachine> vmToMigrate) {

        Collections.sort(vmToMigrate);
        PhysicalMachine hostPm;

        for (VirtualMachine vm : vmToMigrate) {
            hostPm = PhysicalMachine.getById(vm.getPhysicalMachine(), physicalMachines);

            for (PhysicalMachine pm : physicalMachines) {

                if(hostPm != null && !pm.getId().equals(hostPm.getId())
                    && Constraints.checkResources(pm, null, vm, virtualMachines, true)
                    && Heuristics.getHeuristics()[code]
                        .useHeuristic(vm, physicalMachines, virtualMachines, derivedVMs)) {

                    virtualMachines.remove(vm);
                    hostPm.updatePMResources(vm, Utils.SUB);
                    break;

                }
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

        Integer code = Constant.HEURISTIC_MAP.get(heuristicCode);
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
     *      ALGORITHM = Algorithm Code
     *  </li>
     *      <li>HEURISTIC_CODE = Heuristic Code
     *      <ul>
     *          <li> FF = First Fit</li>
     *          <li> BF = Best Fit </li>
     *          <li> WF = Worst Fit </li>
     *          <li> FFD = First Fit Decreasing</li>
     *          <li> BFD = Best Fit Decreasing</li>
     *      </ul>
     *  </li>
     *  <li>
     *      PM_CONFIG = Load CPU Configuration
     *      <ul>
     *          <li> LOW </li>
     *          <li> MED </li>
     *          <li> HIGH </li>
     *          <li> FULL </li>
     *          <li> SATURATED </li>
     *      </ul>
     *  </li>
     *  <li>
     *      DERIVE_COST = Cost per each derived VM
     *  </li>
     *  <li>
     *      FAULT_TOLERANCE = Fault tolerance
     *  </li>
     *  <li>
     *      PROTECTION_FACTOR_1 = Resource1 protection factor [0;1]
     *  </li>
     *  <li>
     *      PROTECTION_FACTOR_2 = Resource2 protection factor [0;1]
     *  </li>
     *  <li>
     *      PROTECTION_FACTOR_3 = Resource3 protection factor [0;1]
     *  </li>
     *  <li>
     *      PENALTY_FACTOR_01 = Resource1 penalty factor (greater than 1)
     *  </li>
     *  <li>
     *      PENALTY_FACTOR_02 = Resource2 penalty factor (greater than 1)
     *  </li>
     *  <li>
     *      PENALTY_FACTOR_03 = Resource3 penalty factor (greater than 1)
     *  </li>
     *  <li>
     *      INTERVAL_EXECUTION_MEMETIC = Periodic Time of MA Execution
     *  </li>
     *  <li>
     *      POPULATION_SIZE = Population size for MA
     *  </li>
     *  <li>
     *      NUMBER_GENERATIONS = Generations size for MA
     *  </li>
     *  <li>
     *      EXECUTION_DURATION = Time of Duration
     *  </li>
     *  <li>
     *      LINK_CAPACITY = Link Capacity for Migration
     *  </li>
     *  <li>
     *      MIGRATION_FACTOR_LOAD = Factor Load per Migration
     *  </li>
     *  <li>
     *      HISTORICAL_DATA_SIZE = Historical Data Sieze
     *  </li>
     *  <li>
     *      FORECAST_SIZE = Forecast Size
     *  </li>
     *  <li>
     *      SCALARIZATION_METHOD = Scalarization Method
     *      <ul>
     *          <li>- ED = Euclidean Distance </li>
     *          <li>- MD = Manhattan Distance </li>
     *          <li>- CD = Chevyshev Distance </li>
     *          <li>- WS = Weighted Sum </li>
     *      </ul>
     *  </li>
     *       *  <li>
     *      MAX_PHEROMONE = Max pheromone allowed in ACO.
     *  </li>
     *
     *  <li>
     *      PHEROMONE_CONSTANT = Pheromone constant for ACO, range [0,1],
     * determines how fast pheromone evaporates.
     * Pheromones evaporates quicker as pheromone
     * constant grows.
     *  </li>
     *
     *  <li>
     *      N_ANTS = Number of ants used for ACO.
     *  </li>
     *
     *  <li>
     *      ACO_ITERATIONS = Number of iterations to be performed in ACO
     * to return a solution.
     *  </li>
     *
     *  <li>
     *      VMPR_ALGORITHM = VMPr algorithm
     *      <ul>
     *          <li> MEMETIC </li>
     *          <li> ACO </li>
     *      </ul>
     *  </li>
     *  <li>
     *      SCENARIOS = List of Request
     *  </li>

     * </ul>
     * If you want to add more parameters, changes the {@link DynamicVMP#loadParameters(ArrayList, String)}
     *
     * @param args List of arguments (as array)
     *
     * @throws IOException          Error managing files
     * @throws InterruptedException Multi-thread error
     * @throws ExecutionException   Multi-thread error
     */
    public static void main (String[] args) throws IOException, InterruptedException, ExecutionException {

        ArrayList<String> scenariosFiles  = new ArrayList<>();
        if(0 == args.length) {
            logger.log(Level.INFO, "Some arguments are missing!");
        }
        String parameterFile = args[0];
        loadParameters(scenariosFiles, parameterFile);

        logger.log(Level.INFO, "EXECUTING EXPERIMENTS");

        for (String scenarioFile : scenariosFiles) {
            // For debug only
            // logger.log(Level.INFO, scenarioFile);
            launchExperiments(Parameter.HEURISTIC_CODE, Parameter.PM_CONFIG, scenarioFile);
        }
        logger.log(Level.INFO, "ENDING EXPERIMENTS");

    }

    /**
     * @param scenariosFiles Scenarios Files
     * @param parameterFile  Parameter File
     * @throws IOException
     */
    private static void loadParameters(ArrayList<String> scenariosFiles, String parameterFile) throws IOException {

        try (Stream<String> stream = Files.lines(Paths.get(parameterFile))) {
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
    private static void launchExperiments(String heuristicCode, String pmConfig, String scenarioFile)
            throws IOException, InterruptedException, ExecutionException {
        // VARIABLES
        List<PhysicalMachine> physicalMachines = new ArrayList<>();
        List<Scenario> scenarios = new ArrayList<>();
        List<VirtualMachine> virtualMachines = new ArrayList<>();
        List<VirtualMachine> derivedVMs = new ArrayList<>();

        Integer[] requestsProcess = initRequestProcess();
        Float[] realRevenue = new Float[]{0F};

        Files.write(Paths.get(Utils.OUTPUT + Utils.PLACEMENT_SCORE_BY_TIME + scenarioFile + Constant.EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME),
                (scenarioFile + "\n" ).getBytes(), StandardOpenOption.CREATE);

        // LIST
        List<Resources> wastedResources = new ArrayList<>();
        Map<Integer, Float> wastedResourcesRatioByTime = new HashMap<>();
        Map<Integer, Float> powerByTime = new HashMap<>();
        Map<Integer, Float> revenueByTime = new HashMap<>();
        Map<Integer, Placement> placements = new HashMap<>();

        maxPower = Utils.loadDatacenter(pmConfig, scenarioFile, physicalMachines, scenarios);
        timeSimulated = scenarios.get(scenarios.size() - 1).getTime();
        Integer code = Constant.HEURISTIC_MAP.get(heuristicCode);

        // Check if the algorithm is valid!
        if(code == null) {
            logger.log(Level.SEVERE, "Is not a valid algorithm!");
            return;
        }

        Integer timeUnit = scenarios.get(0).getTime();
        initialTimeUnit = timeUnit;
        timeAdjustment(wastedResources, wastedResourcesRatioByTime, powerByTime, revenueByTime, scenarioFile);

        loadAprioriValuesByTime(scenarios);

        // Prepare scenario for Decreasing Algorithms (Sort by Total Revenue)
        if (Constant.BFD.equals(heuristicCode) || Constant.FFD.equals(heuristicCode)) {
            Collections.sort(scenarios);
        }

        try{
            getAlgorithms()[Parameter.ALGORITHM]
                    .useAlgorithm(scenarios, physicalMachines, virtualMachines, derivedVMs,
                            revenueByTime, wastedResources, wastedResourcesRatioByTime, powerByTime,
                            placements, code, timeUnit, requestsProcess, maxPower, scenarioFile);
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.log(Level.SEVERE, "Is not a valid algorithm!");
            throw e;
        }

        Float scenarioScored = ObjectivesFunctions.getScenarioScore(revenueByTime, placements, realRevenue);

        Utils.printToFile(Constant.POWER_CONSUMPTION_FILE + Constant.EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME, Utils.getAvgPwConsumptionNormalized(powerByTime));
        Utils.printToFile(Constant.WASTED_RESOURCES_FILE + Constant.EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME, Utils.getAvgResourcesWNormalized(wastedResourcesRatioByTime));
        Utils.printToFile(Constant.ECONOMICAL_REVENUE_FILE + Constant.EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME, Utils.getAvgRevenueNormalized(revenueByTime));
        Utils.printToFile(Constant.WASTED_RESOURCES_RATIO_FILE + Constant.EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME, wastedResources);
        Utils.printToFile(Constant.SCENARIOS_SCORES + Constant.EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME, scenarioScored);
        Utils.printToFile(Constant.RECONFIGURATION_CALL_TIMES_FILE + Constant.EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME,"\n");
        Utils.printToFile(Constant.ECONOMICAL_PENALTIES_FILE + Constant.EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME, Utils.getAvgPenaltyNormalized(DynamicVMP.economicalPenaltiesAprioriTime));
        Utils.printToFile(Constant.LEASING_COSTS_FILE + Constant.EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME, Utils.getAvgNormalized(DynamicVMP.leasingCostsApriori));
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
                Utils.printToFile(Utils.OUTPUT + Utils.PLACEMENT_SCORE_BY_TIME + scenarioFile + Constant.EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME, 0);
                timeAdjust++;
                timeSimulated += 1;
            }
        }
    }

    /**
     * Load the objective function's  a priori values from the scenario
     * @param workload List of Scenarios
     */
    public static void loadAprioriValuesByTime(List<Scenario> workload) {

        Map<Integer, Float> revenueAPrioriByTime = new HashMap<>();
        Map<Integer, Float> migratedMemoryAPrioriByTime = new HashMap<>();
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
            revenueAPriori += request.getRevenue().getCpu() * request.getResources().getCpu() *  Parameter.DERIVE_COST;
            revenueAPriori += request.getRevenue().getRam() * request.getResources().getRam() *  Parameter.DERIVE_COST;
            revenueAPriori += request.getRevenue().getNet() * request.getResources().getNet() *  Parameter.DERIVE_COST;

	        migratedMemoryAPriori+= request.getResources().getRam();

            if(request.getTime() <= request.getTinit()) {
                numberUniqueVm++;
            }

            if((iteratroScenario + 1) == workload.size() || !request.getTime().equals(workload.get(iteratroScenario + 1).getTime())){
                revenueAPrioriByTime.put(request.getTime(), revenueAPriori);
                maxRevenueLost += revenueAPriori;
                migratedMemoryAPrioriByTime.put(request.getTime(), migratedMemoryAPriori);
                revenueAPriori = 0F;
                migratedMemoryAPriori = 0F;
            }
        }

        revenueAprioriTime = revenueAPrioriByTime;
        migratedMemoryAprioriTime = migratedMemoryAPrioriByTime;
        vmUnique = numberUniqueVm;

    }

    /**
     *
     * @param heuristicPlacement Heuristic Placement
     * @param memeticPlacement Memetic Placement
     * @return <b>True</b>, is Memetic Placement is better than Heuristic Placement <br> <b>False</b>, otherwise
     */
    public static Boolean isMememeticPlacementBetter(Placement heuristicPlacement, Placement memeticPlacement) {

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

    public static Boolean isVmBeingMigrated(Integer virtualMachineId, List<VirtualMachine> vmsToMigrate) {
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

    public static void updateEconomicalPenalties(VirtualMachine vm, Resources resourcesViolated, Integer timeViolation) {

        Float violationRevenue = 0F;
        violationRevenue += resourcesViolated.getCpu() * vm.getRevenue().getCpu() * Parameter.PENALTY_FACTOR.get(0);
        violationRevenue += resourcesViolated.getRam() * vm.getRevenue().getRam() * Parameter.PENALTY_FACTOR.get(1);
        violationRevenue += resourcesViolated.getNet() * vm.getRevenue().getNet() * Parameter.PENALTY_FACTOR.get(2);

        Float currentRevenue = DynamicVMP.revenueAprioriTime.get(timeViolation);
        Float newAPrioriRevenue = currentRevenue + violationRevenue;
        DynamicVMP.revenueAprioriTime.put(timeViolation, newAPrioriRevenue);

        if(economicalPenaltiesAprioriTime.get(timeViolation)==null){
            economicalPenaltiesAprioriTime.put(timeViolation, violationRevenue);
        }else{
            Float old = economicalPenaltiesAprioriTime.get(timeViolation);
            economicalPenaltiesAprioriTime.put(timeViolation, old+violationRevenue);
        }
        economicalPenalties += violationRevenue;
    }

    public static void updateLeasingCosts(List<VirtualMachine> derivedVMs) {

        Float leasingCostRevenue = 0F;
        for (VirtualMachine dvm : derivedVMs) {
            leasingCostRevenue += dvm.getResources().get(0) * dvm.getRevenue().getCpu() * Parameter.DERIVE_COST;
            leasingCostRevenue += dvm.getResources().get(1) * dvm.getRevenue().getRam() * Parameter.DERIVE_COST;
            leasingCostRevenue += dvm.getResources().get(2) * dvm.getRevenue().getNet() * Parameter.DERIVE_COST;
        }

        leasingCostsApriori.add(leasingCostRevenue);

        leasingCosts += leasingCostRevenue;
    }

    public static Algorithm[] getAlgorithms() {

        return algorithms;
    }

    public static Logger getLogger() {

        return logger;
    }
}

