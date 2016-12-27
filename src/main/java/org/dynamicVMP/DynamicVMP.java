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
import org.dynamicVMP.cleverReconfiguration.CleverReconfiguration;

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

    public static final String DYNAMIC_VMP = "DynamicVMP";

    static Integer TIME_SIMULATED;
    static Integer INITIAL_TIME_UNIT;
    static Integer VM_UNIQUE = 0;

    /**
     * A priori Values
     */
    public static Float MAX_POWER = 0F;
    public static Float MAX_REVENUE_LOST = 0F;
    public static Float ECONOMICAL_PENALTIES = 0F;
    public static Float LEASING_COSTS = 0F;

    /**
     * Apriori values lists by time
     */
    static Map<Integer, Float> REVENUE_APRIORI_TIME = new HashMap<>();
    static Map<Integer, Float> MIGRATED_MEMORY_APRIORI_TIME = new HashMap<>();

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
    static Map<Integer, Violation> UNSATISFIED_RESOURCES = new HashMap<>();

    private DynamicVMP () {
        // Default Constructor
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

        Logger.getLogger(DYNAMIC_VMP).log(Level.INFO, "STARTING EXPERIMENTS");

        for (String scenarioFile : scenariosFiles) {
            // TODO: For debug only
            System.out.println(scenarioFile);
            launchExperiments(Parameter.HEURISTIC_CODE, Parameter.PM_CONFIG, scenarioFile);
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
        TIME_SIMULATED = scenarios.get(scenarios.size() - 1).getTime();
        Integer code = Constant.HEURISTIC_MAP.get(heuristicCode);

        Integer timeUnit = scenarios.get(0).getTime();
        INITIAL_TIME_UNIT = timeUnit;
        timeAdjustment(wastedResources, wastedResourcesRatioByTime, powerByTime, revenueByTime, scenarioFile);

        loadAprioriValuesByTime(scenarios);

        // Prepare scenario for Decreasing Algorithms (Sort by Total Revenue)
        if (Constant.BFD.equals(heuristicCode) || Constant.FFD.equals(heuristicCode)) {
            Collections.sort(scenarios);
        }

        CleverReconfiguration.CleverReconfigurationgManager(scenarios, physicalMachines, virtualMachines, derivedVMs,
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
        if(INITIAL_TIME_UNIT  != 0 ) {
            while (timeAdjust < INITIAL_TIME_UNIT) {
                powerByTime.put(timeAdjust, 0F);
                wastedResources.add(new Resources());
                wastedResourcesRatioByTime.put(timeAdjust, 0F);
                revenueByTime.put(timeAdjust, 0F);
                Utils.printToFile(scenarioFile, 0);
                timeAdjust++;
                TIME_SIMULATED  += 1;
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

        if(INITIAL_TIME_UNIT  != 0 ) {
            while (timeAdjust < INITIAL_TIME_UNIT ) {
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

    public static void updateEconomicalPenalties(VirtualMachine vm, Resources resourcesViolated) {

        Float violationRevenue = 0F;
        violationRevenue += resourcesViolated.getCpu() * vm.getRevenue().getCpu();
        violationRevenue += resourcesViolated.getRam() * vm.getRevenue().getRam();
        violationRevenue += resourcesViolated.getNet() * vm.getRevenue().getNet();

        ECONOMICAL_PENALTIES += violationRevenue;
    }

    public static void updateLeasingCosts(List<VirtualMachine> derivedVMs) {

        Float leasingCostRevenue = 0F;
        for (VirtualMachine dvm : derivedVMs) {
            leasingCostRevenue += dvm.getResources().get(0) * dvm.getRevenue().getCpu() * Parameter.DERIVE_COST;
            leasingCostRevenue += dvm.getResources().get(1) * dvm.getRevenue().getRam() * Parameter.DERIVE_COST;
            leasingCostRevenue += dvm.getResources().get(2) * dvm.getRevenue().getNet() * Parameter.DERIVE_COST;
        }

        LEASING_COSTS+= leasingCostRevenue;
    }

}

