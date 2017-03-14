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
import org.framework.iterativeAlgorithm.Heuristics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Saul Zalimben.
 * @since 8/15/16.
 */
public class ObjectivesFunctions {

    /**
     * Minimum Power Consumption Percentage <br>
     * According to:
     * <p>
     *  A. Beloglazov, J. Abawajy, and R. Buyya. <br>
     *  Energy-aware resource allocation heuristics for
     *  efficient management of data centers for cloud
     *  computing. <br> Future Generation Computer Systems
     * </p>
     */
    public static final Float MIN_POWER_PERCENTAGE = 0.6F;

    /**
     * Minimum Power Consumption is 0 (All PMs are off)
     */
    static final Float MIN_POWER = 0F;

    /**
     * Minimum Revenue is 0 (All VMs are served)
     */
    static final Float MIN_REVENUE = 0F;

    private ObjectivesFunctions() {
        // Default Constructor
    }

    /**
     * OF: Power Consumption
     * @param physicalMachines List of Physical Machines
     * @return Power Consumption at time t
     */
    public static Float powerConsumption(List<PhysicalMachine> physicalMachines) {

        Float utilidad;
        Float powerConsumption = 0F;

        for (PhysicalMachine pm : physicalMachines) {
            if (pm.getResourcesRequested().get(0) > 0.0001) {
                utilidad = pm.getResourcesRequested().get(0) / pm.getResources().get(0);
                powerConsumption += (pm.getPowerMax() - pm.getPowerMax() * MIN_POWER_PERCENTAGE)
                        * utilidad + pm.getPowerMax() * MIN_POWER_PERCENTAGE;
            }
        }

        return powerConsumption;
    }

    /**
     * OF: Economical Revenue
     * @param virtualMachines    List of Virtual Machines
     * @param derivedVMs         List of Derived Virtual Machines
     * @param timeUnit           Time Unit
     * @return Lost Revenue at time t
     */
    public static Float economicalRevenue(List<VirtualMachine> virtualMachines, List<VirtualMachine>
            derivedVMs, Integer timeUnit) {

        Float totalRevenue = 0F;
        Float violationRevenue = 0F;

        Violation violation;
        Resources resources;

        for (VirtualMachine vm : virtualMachines) {

            // Get violation per VM per time (if exists)
            violation = DynamicVMP.unsatisfiedResources.get(vm.getCloudService()+"_"+vm.getId());
            if (violation != null && timeUnit != null) {
                resources = violation.getResourcesViolated().get(timeUnit);
                // Get Violated Resources
                if(resources != null) {
                    violationRevenue += resources.getCpu() * vm.getRevenue().getCpu() * Parameter.PENALTY_FACTOR.get(0);
                    violationRevenue += resources.getRam() * vm.getRevenue().getRam() * Parameter.PENALTY_FACTOR.get(1);
                    violationRevenue += resources.getNet() * vm.getRevenue().getNet() * Parameter.PENALTY_FACTOR.get(2);
                }
                totalRevenue += violationRevenue;
                violationRevenue = 0F;
            }
        }

        for (VirtualMachine dvm : derivedVMs) {
            totalRevenue += dvm.getResources().get(0) * dvm.getRevenue().getCpu() * Parameter.DERIVE_COST;
            totalRevenue += dvm.getResources().get(1) * dvm.getRevenue().getRam() * Parameter.DERIVE_COST;
            totalRevenue += dvm.getResources().get(2) * dvm.getRevenue().getNet() * Parameter.DERIVE_COST;
        }

        return totalRevenue;
    }

    /**
     * OF: Wasted Resources
     * @param physicalMachines List of Physical Machines
     * @param wastedResources  List of Wasted Resources
     * @return wastedResourcesRatio
     */
    public static Float wastedResources(List<PhysicalMachine> physicalMachines,
            List<Resources> wastedResources) {

        float wastedCPU = 0F;
        float wastedRAM = 0F;
        float wastedNET = 0F;

        float wastedCpuResourcesRatio;
        float wastedRamResourcesRatio;
        float wastedNetResourcesRatio;

        float alpha = 1F;
        float beta = 1F;
        float gamma = 1F;
        float wastedResourcesRatio;

        int workingPms = 0;

        for (PhysicalMachine pm : physicalMachines) {

            if (pm.getResourcesRequested().get(0) > 0.0001
                    || pm.getResourcesRequested().get(1) > 0.0001
                    || pm.getResourcesRequested().get(2) > 0.0001) {

                workingPms++;
                float wcpu = 1 - pm.getResourcesRequested().get(0) / pm.getResources().get(0);
                float wram = 1 - pm.getResourcesRequested().get(1) / pm.getResources().get(1);
                float wnet = 1 - pm.getResourcesRequested().get(2) / pm.getResources().get(2);

                if(wcpu > 0) {
                    wastedCPU += wcpu;
                } else {
                    wastedCPU += 0;
                }

                if(wram > 0) {
                    wastedRAM += wram;
                } else {
                    wastedRAM += 0;
                }

                if(wnet > 0) {
                    wastedNET += wnet;
                } else {
                    wastedNET += 0;
                }

            }
        }

        // If no pms working return 0
        if (workingPms == 0) {
            return 0F;
        }

        // total wasted resources of all PMs / num of working PMs
        wastedCpuResourcesRatio = wastedCPU / workingPms;
        wastedRamResourcesRatio = wastedRAM / workingPms;
        wastedNetResourcesRatio = wastedNET / workingPms;

        Resources wasted = new Resources(wastedCpuResourcesRatio, wastedRamResourcesRatio, wastedNetResourcesRatio);

        if (wastedResources != null) {
            wastedResources.add(wasted);
        }

        // sum the wasted resources ratio and divide with the number of resources cosidered (3 in this case)
        wastedResourcesRatio = (
                wastedCpuResourcesRatio * alpha
                + wastedRamResourcesRatio * beta
                + wastedNetResourcesRatio * gamma ) / 3;

        return wastedResourcesRatio;
    }

    /**
     * OF: Migration Count
     * @param oldVirtualMachineList List of Virtual Machines (before Migration)
     * @param newVirtualMachineList List of Virtual Machines (after Migration)
     * @return number of vm migrated
     */
    public static Float migrationCount(List<VirtualMachine> oldVirtualMachineList, List<VirtualMachine> newVirtualMachineList){
        int iterator;
        int oldPosition;
        int newPosition;
        float migrationCounter = 0;
        for(iterator=0;iterator<oldVirtualMachineList.size();iterator++){
            oldPosition = oldVirtualMachineList.get(iterator).getPhysicalMachine();
            newPosition = newVirtualMachineList.get(iterator).getPhysicalMachine();
            if(oldPosition!=newPosition && newPosition!=0){
                migrationCounter+=1;
            }
        }

        return migrationCounter;
    }


    /**
     * OF: Migration Overhead
     * @param oldVirtualMachineList List of VMs (before migration)
     * @param newVirtualMachineList List of VMs (after migration)
     * @return total memory migrated
     */
    public static Float memoryMigrated(List<VirtualMachine> oldVirtualMachineList, List<VirtualMachine> newVirtualMachineList){
        int iterator;
        int oldPosition;
        int newPosition;
        float memoryMigrated = 0;
        for(iterator=0;iterator<oldVirtualMachineList.size();iterator++){
            oldPosition = oldVirtualMachineList.get(iterator).getPhysicalMachine();
            newPosition = newVirtualMachineList.get(iterator).getPhysicalMachine();
            if(oldPosition!=newPosition && newPosition!=0){
                memoryMigrated+=newVirtualMachineList.get(iterator).getResources().get(1);
            }
        }

        return memoryMigrated;
    }

    /**
     * OF: Max Migrated memory between Physical Machines
     * @param oldVirtualMachineList List of VMs (before migration)
     * @param newVirtualMachineList List of VMs (after migration)
     * @param numberOfPMs           Number of PMs
     * @return max migrated memory between two physical machines
     */
    public static Float migratedMemoryBtwPM(List<VirtualMachine> oldVirtualMachineList, List<VirtualMachine> newVirtualMachineList, Integer numberOfPMs){

        //get the memory migrated between physical machines
        Float[][] migrationMatrix  = Utils.getMigratedMemoryByPM(oldVirtualMachineList,newVirtualMachineList,numberOfPMs);
        //set the max memory migrated
        Float maxMigratedMemory = 0F;
        for(int iteratorRow=0;iteratorRow<numberOfPMs;iteratorRow++){
            for(int iteratorColumn=0;iteratorColumn<numberOfPMs;iteratorColumn++){
                //check  migrated memory between two physical machines
                if(migrationMatrix[iteratorRow][iteratorColumn] > maxMigratedMemory){
                    maxMigratedMemory = migrationMatrix[iteratorRow][iteratorColumn];
                }
            }
        }
        return maxMigratedMemory;
    }

    /**
     * Combine objective function values using the approach distance from the origin
     * @param objFunctValues List of objective function value normalized.
     * @param weight         PM's weight
     * @return distance from origin
     */
    public static Float getScalarizationMethod(List<Float> objFunctValues, Float weight){

        if("ED".equals(Parameter.SCALARIZATION_METHOD)) {
            return getEuclideanDistance(objFunctValues);
        } else if("CD".equals(Parameter.SCALARIZATION_METHOD)) {
            return getChebyshevDistance(objFunctValues);
        } else if("MD".equals(Parameter.SCALARIZATION_METHOD)) {
            return getManhattanDistance(objFunctValues);
        } else {
            return getWeightedSum(objFunctValues, weight);
        }

    }

    private static Float getEuclideanDistance(final List<Float> objFunctValues) {

        float tempSum = 0;
        for (Float objFunctValue : objFunctValues) {
            //sum the square of each objective function
            tempSum += Math.pow(objFunctValue,2);
        }
        Double distance = Math.sqrt(tempSum);
        return distance.floatValue();
    }

    private static Float getChebyshevDistance(final List<Float> objFunctValues) {

        return objFunctValues.stream().max(Float::compareTo).get();

    }

    private static Float getManhattanDistance(final List<Float> objFunctValues) {

        Double d = objFunctValues.stream().mapToDouble(Float::doubleValue).sum();

        return d.floatValue();

    }

    private static Float getWeightedSum(final List<Float> objFunctValues, Float weight) {

        float tempSum = 0F;

        for (Float objFunctValue : objFunctValues) {
            //sum the square of each objective function
            tempSum += weight * objFunctValue;
        }

        return tempSum;

    }

    /**
     * Calculates Objective Functions
     * @param physicalMachines           List of Physical Machibes
     * @param virtualMachines            List of Virtual Machines
     * @param derivedVMs                 List of Derived Virtual Machines
     * @param wastedResources            WastedResources per time t
     * @param wastedResourcesRatioByTime WastedResourcesRatio per time t
     * @param powerByTime                Power Consumption per time t
     * @param revenueByTime              Revenue per time t
     * @param timeUnit                   Time Unit
     * @param currentTimeUnit            Initial Time Unit
     */
    public static void getObjectiveFunctionsByTime(List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs,
            List<Resources> wastedResources,  Map<Integer, Float> wastedResourcesRatioByTime,
            Map<Integer, Float> powerByTime,  Map<Integer, Float> revenueByTime,
            Integer timeUnit, Integer currentTimeUnit ) {

        // Remove VM from previous t
        Heuristics.removeVMByTime(virtualMachines, timeUnit, physicalMachines);
        Heuristics.removeDerivatedVMByTime(derivedVMs, timeUnit);

        revenueByTime.put(currentTimeUnit, ObjectivesFunctions
                .economicalRevenue(virtualMachines, derivedVMs, currentTimeUnit));

        powerByTime.put(currentTimeUnit, ObjectivesFunctions.powerConsumption(physicalMachines));

        wastedResourcesRatioByTime
                .put(currentTimeUnit, ObjectivesFunctions.wastedResources(physicalMachines, wastedResources));

    }

    /**
     * Get the Current Score of the Placement
     * <p>
     *     Normalize objective functions values and use {@link Parameter#SCALARIZATION_METHOD} to combined them into
     *     one value. (the placement score)
     * </p>
     * @param timeUnit                   TimeUnit
     * @param maxPower                   MaxPower (possible) consumed by a PM
     * @param wastedResourcesRatioByTime WastedResourcesRatio per time t
     * @param powerByTime                Power Consumption per time t
     * @param revenueByTime              Revenue per time t
     * @return Distance to origen in time t
     */
    public static Float getDistanceOrigenByTime (Integer timeUnit, Float maxPower,  Map<Integer, Float> powerByTime,
            Map<Integer, Float> revenueByTime,  Map<Integer, Float> wastedResourcesRatioByTime) {

        // Sum of all results at each time t. (Normalized)
        Float powerConsumptionResult = 0F;
        Float revenueResult = 0F;
        Float wastedResourcesResult = 0F;
        Float normalizedPowerConsumption;
        Float normalizedRevenue;

        if (powerByTime.get(timeUnit) == null ) {
            powerByTime.put(timeUnit, 0F);
            revenueByTime.put(timeUnit, 0F);
            wastedResourcesRatioByTime.put(timeUnit, 0F);
        }
        // Power Consumption
        normalizedPowerConsumption = Utils.normalizeValue(powerByTime.get(timeUnit), MIN_POWER, maxPower);

        // Revenue
        if(revenueByTime.get(timeUnit) != null && revenueByTime.get(timeUnit) > 0) {
            normalizedRevenue = Utils.normalizeValue(revenueByTime.get(timeUnit), MIN_REVENUE,
                    DynamicVMP.revenueAprioriTime.get(timeUnit));

        }else{
            normalizedRevenue = 0F;
        }

        powerConsumptionResult += normalizedPowerConsumption;
        revenueResult += normalizedRevenue;
        wastedResourcesResult += wastedResourcesRatioByTime.get(timeUnit);

        List<Float> objectiveFunctionsResult = new ArrayList<>();
        objectiveFunctionsResult.add(powerConsumptionResult);
        objectiveFunctionsResult.add(revenueResult);
        objectiveFunctionsResult.add(wastedResourcesResult);

        return ObjectivesFunctions.getScalarizationMethod(objectiveFunctionsResult, Constant.WEIGHT_ONLINE);
    }

    /**
     * @param virtualMachineList  List of Virtual Machine
     * @param derivedVMs          List of Derived Virtual Machine
     * @param physicalMachineList List of Physical Machine
     * @return Objective Functions
     */
    public static Float[] loadObjectiveFunctions(List<VirtualMachine> virtualMachineList, List<VirtualMachine> derivedVMs, List<PhysicalMachine> physicalMachineList){

		Float[] objectiveFunctions = new Float[Constant.NUM_OBJ_FUNCT_COMP];

	    objectiveFunctions[0]=ObjectivesFunctions.powerConsumption(physicalMachineList);
	    objectiveFunctions[1]=ObjectivesFunctions.economicalRevenue(virtualMachineList,derivedVMs,null);
	    objectiveFunctions[2]= ObjectivesFunctions.wastedResources(physicalMachineList,null);

	    return objectiveFunctions;

    }


    /**
     * Get Scenario Score
     * @param revenueByTime Revenue By Time
     * @param placements List of Placement per time T
     * @param realRevenue Real revenue lost
     * @return Scenario score
     */
    public static Float getScenarioScore( Map<Integer, Float> revenueByTime, Map<Integer, Placement> placements,
            final Float[] realRevenue) {

        // Calculates total revenue lost
        for (Map.Entry<Integer, Float> entry : revenueByTime.entrySet()) {
            realRevenue[0] += entry.getValue();
        }

        // Calculates scenario score
        Float scenarioScored = 0F;
        for (Map.Entry<Integer, Placement> entry : placements.entrySet()) {
            scenarioScored += entry.getValue().getPlacementScore();
        }
        return scenarioScored;
    }
}

