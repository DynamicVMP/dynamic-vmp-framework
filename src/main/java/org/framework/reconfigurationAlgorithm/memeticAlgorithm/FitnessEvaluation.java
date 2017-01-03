package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.domain.APrioriValue;
import org.domain.PhysicalMachine;
import org.domain.VirtualMachine;

import java.util.List;

/**
 * @author Leonardo Benitez.
 */
public interface FitnessEvaluation {

    void evaluate(Population population, List<VirtualMachine> virtualMachineList,List<VirtualMachine> derivedVMs,
            List<PhysicalMachine> physicalMachineList, List<APrioriValue> aPrioriValuesList, int numberOfResources,
            int numberOfObjFunctions);

    void loadUtilization(Population population, List<VirtualMachine> virtualMachineList, int numberOfResources);

    void loadObjectiveFunctions(Individual individual, List<VirtualMachine> virtualMachineList,
            List<VirtualMachine> previousVirtualMachineList,List<VirtualMachine> derivedMVs,
            List<PhysicalMachine> physicalMachineList, int numberOfResources);

    void loadFitness(Individual individual, List<APrioriValue> aPrioriValuesList, int numberOfObjFunctions);
}
