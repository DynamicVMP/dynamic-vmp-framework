package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.domain.APrioriValue;
import org.domain.PhysicalMachine;
import org.domain.VirtualMachine;
import org.framework.Constant;
import org.framework.ObjectivesFunctions;
import org.framework.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leonardo Benitez.
 */
public class EvaluationByScalarizationMethod implements FitnessEvaluation{

    public EvaluationByScalarizationMethod() {
        super();
    }

    @Override
    public void evaluate(Population population, List<VirtualMachine> virtualMachineList,List<VirtualMachine> derivedVMs, List<PhysicalMachine> physicalMachineList, List<APrioriValue> aPrioriValuesList, int numberOfResources,int numberOfObjFunctions) {

        /*auxiliary lists used to obtain information of an individual and calculates the objective functions*/
        List<VirtualMachine> individualVMsList = VirtualMachine.cloneVMsList(virtualMachineList);
        List<PhysicalMachine> individualPMsList = PhysicalMachine.clonePMsList(physicalMachineList);
        List<VirtualMachine> individualDerivedVMs =  VirtualMachine.cloneVMsList(derivedVMs);

        for(Individual individual : population.getIndividuals()){
            individualVMsList = individual.convertToVMList(individualVMsList);
            individualPMsList = individual.convertToPMList(individualPMsList,numberOfResources);
            loadObjectiveFunctions(individual,individualVMsList,virtualMachineList,individualDerivedVMs,individualPMsList,numberOfResources);
            loadFitness(individual,aPrioriValuesList,numberOfObjFunctions);
        }

    }

    @Override
    public void loadObjectiveFunctions(Individual individual, List<VirtualMachine> virtualMachineList,List<VirtualMachine> previousVirtualMachineList,List<VirtualMachine> derivedVMs, List<PhysicalMachine> physicalMachineList, int numberOfResources) {

        Float economicalRevenue;
        Float powerConsumption;
        Float wastedResources;
        Float memoryMigrated;

        memoryMigrated = ObjectivesFunctions.migratedMemoryBtwPM(previousVirtualMachineList,virtualMachineList,physicalMachineList.size());
        powerConsumption = ObjectivesFunctions.powerConsumption(physicalMachineList);
        wastedResources = ObjectivesFunctions.wastedResources(physicalMachineList,null);
        Utils.updateDerivedVMs(virtualMachineList,derivedVMs);
        economicalRevenue = ObjectivesFunctions.economicalRevenue(virtualMachineList,derivedVMs,null);

        individual.getObjectiveFunctions()[0] = powerConsumption;
        individual.getObjectiveFunctions()[1] = economicalRevenue;
        individual.getObjectiveFunctions()[2] = wastedResources;
        individual.getObjectiveFunctions()[3] = memoryMigrated;

    }

    @Override
    public void loadFitness(Individual individual, List<APrioriValue> aPrioriValuesList, int numberOfObjFunctions) {

        int iteratorObjFunctions;
        APrioriValue aPrioriValue;
        List<Float> normalizedOjbFunctions = new ArrayList<>();
        Float normalizedValue ;
        for(iteratorObjFunctions= 0; iteratorObjFunctions < numberOfObjFunctions; iteratorObjFunctions++){
            aPrioriValue = aPrioriValuesList.get(iteratorObjFunctions);
            normalizedValue = Utils.normalizeValue(individual.getObjectiveFunctions()[iteratorObjFunctions],aPrioriValue.getMinValue(),aPrioriValue.getMaxValue());
            normalizedOjbFunctions.add(iteratorObjFunctions,normalizedValue);
        }

        Float distance = ObjectivesFunctions.getScalarizationMethod(normalizedOjbFunctions, Constant.WEIGHT_OFFLINE);
        individual.setFitness(distance);

    }



    @Override
    public void loadUtilization(Population population, List<VirtualMachine> virtualMachineList, int numberOfResources) {
        int iteratorSolution;
        int iteratorResource;
        int physicalMachineId;
        VirtualMachine vm;
        for (Individual individual : population.getIndividuals()){
            for(iteratorSolution=0;iteratorSolution<individual.getSize(); iteratorSolution++) {
                physicalMachineId = individual.getSolution()[iteratorSolution];
                if (physicalMachineId != 0) {
                    vm = virtualMachineList.get(iteratorSolution);
                    for (iteratorResource = 0; iteratorResource < numberOfResources; iteratorResource++) {
                        individual.getUtilization()[physicalMachineId - 1][iteratorResource] += vm.getResources().get(iteratorResource) * (vm.getUtilization().get(iteratorResource)/100);
                    }
                }
            }
        }
    }

}

