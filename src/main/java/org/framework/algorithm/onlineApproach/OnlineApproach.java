package org.framework.algorithm.onlineApproach;

import org.domain.*;
import org.framework.Constant;
import org.framework.DynamicVMP;
import org.framework.ObjectivesFunctions;
import org.framework.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class OnlineApproach {


	public static final String DYNAMIC_VMP_ONLINE = "DynamicVMP: Only Online Approach";

	private static Logger logger = DynamicVMP.getLogger();

	private OnlineApproach() {
		// Default Constructor
	}

	/**
	 * Periodic Migration Manager
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
	 */
	public static void onlineApproachManager(List<Scenario> workload, List<PhysicalMachine> physicalMachines,
	                                            List<VirtualMachine>
			                                            virtualMachines, List<VirtualMachine> derivedVMs,
	                                            Map<Integer, Float> revenueByTime, List<Resources> wastedResources, Map<Integer, Float> wastedResourcesRatioByTime,
	                                            Map<Integer, Float> powerByTime, Map<Integer, Placement> placements, Integer code, Integer timeUnit,
	                                            Integer[] requestsProcess, Float maxPower, String scenarioFile)
			throws IOException {

		Integer actualTimeUnit;
		Integer nextTimeUnit;

		for (int iterator = 0; iterator < workload.size(); ++iterator) {
			Scenario request = workload.get(iterator);
			actualTimeUnit = request.getTime();
			//check if is the last request, assign -1 to nextTimeUnit if so.
			nextTimeUnit = iterator + 1 == workload.size() ? -1 : workload.get(iterator + 1).getTime();

			DynamicVMP.runHeuristics(request, code, physicalMachines, virtualMachines, derivedVMs, requestsProcess, false);

			// check if its the last request or a variation of time unit will occurs.
			if (nextTimeUnit == -1 || !actualTimeUnit.equals(nextTimeUnit)) {

				ObjectivesFunctions.getObjectiveFunctionsByTime(physicalMachines,
						virtualMachines, derivedVMs, wastedResources,
						wastedResourcesRatioByTime, powerByTime, revenueByTime, timeUnit, actualTimeUnit);

				Float placementScore = ObjectivesFunctions.getDistanceOrigenByTime(request.getTime(),
						maxPower, powerByTime, revenueByTime, wastedResourcesRatioByTime);

				Utils.checkPathFolders(Constant.PLACEMENT_SCORE_BY_TIME_FILE);
				// Print the Placement Score by Time t
				Utils.printToFile( Constant.PLACEMENT_SCORE_BY_TIME_FILE + scenarioFile, placementScore);

				timeUnit = actualTimeUnit;

				Placement heuristicPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
						VirtualMachine.cloneVMsList(virtualMachines),
						VirtualMachine.cloneVMsList(derivedVMs), placementScore);
				placements.put(actualTimeUnit, heuristicPlacement);

				}
			}

	}
}
