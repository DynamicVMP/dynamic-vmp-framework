package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.domain.PhysicalMachine;
import org.domain.VirtualMachine;
import org.framework.Utils;

import java.util.Arrays;
import java.util.List;

/**
 * @author Leonardo Benitez.
 */
public class Individual {

    private Integer[] solution;
    private Float[][] utilization;
    private Float[] objectiveFunctions;
    private Float fitness;


    public Individual(){

    }


    public Individual(Integer numberOfObjFuncts, Integer numberOfVMs, Integer numberOfPMs, Integer numberOfRes){
        this.solution = new Integer[numberOfVMs];
        this.utilization = new Float[numberOfPMs][numberOfRes];
        this.objectiveFunctions = new Float[numberOfObjFuncts];
        this.utilization = new Float[numberOfPMs][numberOfRes];
	    Utils.initializeMatrix(utilization,numberOfPMs,numberOfRes);
	    this.fitness= 0F;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(Float fitness) {

        this.fitness = fitness;
    }

    public Integer[] getSolution() {

        return solution;
    }

    public void setSolution(Integer[] solution) {

        this.solution = solution;
    }

    public Float[][] getUtilization() {

        return utilization;
    }

    public void setUtilization(Float[][] utilization) {

        this.utilization = utilization;
    }

    public Float[] getObjectiveFunctions() {

        return objectiveFunctions;
    }

    public void setObjectiveFunctions(Float[] objectiveFunctions) {

        this.objectiveFunctions = objectiveFunctions;
    }

    public Individual copy(){

        return new Individual(this);
    }

    protected Individual(Individual individual){
        Integer numberOfVMs = individual.getSolution().length;
        Integer numberOfObjFuncts = individual.getObjectiveFunctions().length;
        Integer numberOfPMs = individual.getUtilization().length;
        Integer numberOfRes = individual.getUtilization()[0].length;

        this.solution = new Integer[numberOfVMs];
        this.objectiveFunctions = new Float[numberOfObjFuncts];
        this.utilization = new Float[numberOfPMs][numberOfRes];
	    Utils.initializeMatrix(utilization,numberOfPMs,numberOfRes);
        this.solution = Arrays.copyOf(individual.getSolution(),numberOfVMs);

    }

    public Integer getSize(){

        return this.getSolution().length;
    }

    /**
     * Transform Individual to List of Virtual Machine
     * @param virtualMachineList List of Virtual Machine
     * @return List of Virtual Machine (List of Virtual Machine Objects)
     */
    public List<VirtualMachine> convertToVMList(List<VirtualMachine> virtualMachineList){

        Integer iteratorSolution,physicalMachine;

        for(iteratorSolution=0;iteratorSolution<this.getSize();iteratorSolution++){
            physicalMachine = this.getSolution()[iteratorSolution];
            virtualMachineList.get(iteratorSolution).setPhysicalMachine(physicalMachine);
        }

        return virtualMachineList;
    }

    /**
     * Transform Individual to List of Physical Machine
     * @param physicalMachineList List of Physical Machine
     * @param numberOfResources   Number of Resources
     * @return List of Physical Machines
     */
    public List<PhysicalMachine> convertToPMList(List<PhysicalMachine> physicalMachineList, Integer numberOfResources){

        Integer iteratorPhysical, iteratorResources;
        Float utilizationOfResource, utilizationPercentage, resource;

        for (iteratorPhysical=1;iteratorPhysical<=physicalMachineList.size();iteratorPhysical++){
            for(iteratorResources=0;iteratorResources<numberOfResources;iteratorResources++){
                resource = PhysicalMachine.getById(iteratorPhysical,physicalMachineList).getResources().get(iteratorResources);
                utilizationOfResource = this.getUtilization()[iteratorPhysical-1][iteratorResources];

                utilizationPercentage = (utilizationOfResource/resource)*100;

                PhysicalMachine.getById(iteratorPhysical,physicalMachineList).getResourcesRequested().set(iteratorResources,utilizationOfResource);
                PhysicalMachine.getById(iteratorPhysical,physicalMachineList).getUtilization().set(iteratorResources,utilizationPercentage);
            }
        }

        return physicalMachineList;
    }

}