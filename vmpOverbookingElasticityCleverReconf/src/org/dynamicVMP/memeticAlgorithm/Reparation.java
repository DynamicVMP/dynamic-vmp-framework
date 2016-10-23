package org.dynamicVMP.memeticAlgorithm;

import org.domain.PhysicalMachine;
import org.domain.VirtualMachine;
import org.dynamicVMP.Constraints;
import org.dynamicVMP.DynamicVMP;
import org.dynamicVMP.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leonardo Benitez.
 */
public class Reparation {

    /**
     *
     * @param population
     * @param virtualMachineList
     * @param physicalMachineList
     * @param numberOfResources
     * @return
     */
    public static Population repairPopulation(Population population,List<VirtualMachine> virtualMachineList,List<PhysicalMachine> physicalMachineList,
                                             int numberOfResources){

        List<VirtualMachine> individualVmList = VirtualMachine.cloneVMsList(virtualMachineList);
        List<PhysicalMachine> individualPmList = PhysicalMachine.clonePMsList(physicalMachineList);

        for(Individual individual : population.getIndividuals()) {
           individualVmList= individual.convertToVMList(individualVmList);
           individualPmList= individual.convertToPMList(individualPmList,numberOfResources);
           checkAndRepair(individual,individualVmList,individualPmList,numberOfResources);
        }
        return population;
    }

    /**
     *
     * @param individual
     * @param individualVmList
     * @param individualPmList
     * @param numberOfResources
     */
    public static void checkAndRepair(Individual individual,List<VirtualMachine> individualVmList, List<PhysicalMachine> individualPmList,
                                     int numberOfResources){

        int iteratorSolution;
        int physicalMachineId;
        PhysicalMachine pm;
        VirtualMachine vm;
        List<VirtualMachine> vmsInPM;
        for(iteratorSolution=0;iteratorSolution<individual.getSize(); iteratorSolution++){
           physicalMachineId = individual.getSolution()[iteratorSolution];
           if(physicalMachineId != 0){
               pm = PhysicalMachine.getById(physicalMachineId,individualPmList);
               vmsInPM = Utils.filterVMsByPM(individualVmList,physicalMachineId);
               vm = individualVmList.get(iteratorSolution);
               if(Constraints.checkPMOverloaded(pm,vmsInPM, DynamicVMP.PROTECTION_FACTOR)){
                  moveVM(individual,iteratorSolution,vm,individualVmList,individualPmList,numberOfResources);
               }
           }
        }
    }

    /**
     *
     * @param individual Individuo of population
     * @param iteratorSolution Index Solution
     * @param vm                Virtual Machine
     * @param virtualMachineList List of Virtual Machines
     * @param physicalMachineList List of Physical Machines
     * @param numberOfResources Number of Resources
     * @return
     */
    private static Boolean moveVM(Individual individual,int iteratorSolution, VirtualMachine vm,List<VirtualMachine> virtualMachineList, List<PhysicalMachine> physicalMachineList,
                                  int numberOfResources){

        int pmIdCandidate;
        int iteratorPhysical;
        int iteratorResources;
        Float vmResource;
        Float resourceRequested;
        Float newResourceRequested;
        int numberOfPMs  = physicalMachineList.size();
        int actualPMId = vm.getPhysicalMachine();
        PhysicalMachine pmCandidate;

        pmIdCandidate = Utils.getRandomInt(1,numberOfPMs);
        for(iteratorPhysical=0;iteratorPhysical<numberOfPMs;iteratorPhysical++){
            pmCandidate = PhysicalMachine.getById(pmIdCandidate,physicalMachineList);
            if(Constraints.checkResources(pmCandidate,null,vm,virtualMachineList,false)){

                for(iteratorResources=0;iteratorResources<numberOfResources;iteratorResources++){

                    vmResource = vm.getResources().get(iteratorResources)*(vm.getUtilization().get(iteratorResources)/100);

                    resourceRequested = individual.getUtilization()[actualPMId-1][iteratorResources];
                    newResourceRequested = resourceRequested - vmResource;
                    individual.getUtilization()[actualPMId-1][iteratorResources] = newResourceRequested;
                    physicalMachineList.get(actualPMId-1).getResourcesRequested().set(iteratorResources,newResourceRequested);

                    resourceRequested = individual.getUtilization()[pmIdCandidate-1][iteratorResources];
                    newResourceRequested = resourceRequested + vmResource;
                    individual.getUtilization()[pmIdCandidate-1][iteratorResources] = newResourceRequested;
                    physicalMachineList.get(pmIdCandidate-1).getResourcesRequested().set(iteratorResources,newResourceRequested);

                }

                individual.getSolution()[iteratorSolution] = pmIdCandidate;
                virtualMachineList.get(iteratorSolution).setPhysicalMachine(pmIdCandidate);

                return true;
            }

            if(pmIdCandidate<numberOfPMs){
                pmIdCandidate+=1;
            }else{
                pmIdCandidate=1;
            }
        }

        return false;
    }
}
