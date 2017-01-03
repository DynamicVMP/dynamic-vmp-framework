package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.domain.APrioriValue;
import org.domain.PhysicalMachine;
import org.domain.Placement;
import org.domain.VirtualMachine;
import org.framework.Utils;

import java.util.List;

/**
 * @author Leonardo Benitez.
 */
public class MoMaVMP {


    /**
     * @param actualPlacement  Placement
     * @param aPrioriValueList Apriori Values
     * @param settings         MA Settings
     * @return Placement
     */
    public Placement reconfiguration(Placement actualPlacement, List<APrioriValue> aPrioriValueList, MASettings settings){

        Individual individualSelected  = this.search(actualPlacement.getVirtualMachineList(),actualPlacement.getDerivedVMs(),
                actualPlacement.getPhysicalMachines(),aPrioriValueList,settings);

        List<VirtualMachine> newVirtualMachineList = VirtualMachine.cloneVMsList(actualPlacement.getVirtualMachineList());
        newVirtualMachineList = individualSelected.convertToVMList(newVirtualMachineList);
        List<VirtualMachine> newDerivedVMs = VirtualMachine.cloneVMsList(actualPlacement.getDerivedVMs());
        Utils.updateDerivedVMs(newVirtualMachineList,newDerivedVMs);
        List<PhysicalMachine> newPhysicalMachineList = PhysicalMachine.clonePMsList(actualPlacement.getPhysicalMachines());
        newPhysicalMachineList = individualSelected.convertToPMList(newPhysicalMachineList,settings.getNumberOfResources());

        Placement newPlacement =  new Placement(newPhysicalMachineList,newVirtualMachineList,newDerivedVMs);
        Float placementScore = Utils.calcPlacemenScore(individualSelected.getObjectiveFunctions(), aPrioriValueList);
        newPlacement.setPlacementScore(placementScore);
        return newPlacement;
    }

    /**
     * @param virtualMachineList  List of Virtual Machine
     * @param derivedVMs          List of derive Virtual Machine
     * @param physicalMachineList List of Physical Machine
     * @param aPrioriValuesList   Apriori Values
     * @param settings            MA Settings
     * @return Individual
     */
    public Individual search(List<VirtualMachine> virtualMachineList, List<VirtualMachine> derivedVMs,
                             List<PhysicalMachine> physicalMachineList, List<APrioriValue> aPrioriValuesList, MASettings settings){

        Selection selectionOperator = new TournamentSelection();
        Crossover crossoverOperator = new OnePointCrossover(settings.getCrossoverProb());
        Mutation mutationOperator = new UniformMutation();
        Initialization initialization = new Initialization();
        FitnessEvaluation fitnessEvaluator = new EvaluationByScalarizationMethod();
        Population populationQ,populationP;

        int generation = 0;
        populationP = initialization.initialize(virtualMachineList.size(),physicalMachineList.size(), settings);
        fitnessEvaluator.loadUtilization(populationP,virtualMachineList,settings.getNumberOfResources());
        populationP = Reparation.repairPopulation(populationP,virtualMachineList,physicalMachineList,
                settings.getNumberOfResources());
        fitnessEvaluator.evaluate(populationP,virtualMachineList,derivedVMs, physicalMachineList,aPrioriValuesList,
                settings.getNumberOfResources(),settings.getNumberOfObjFunctions());
        while(generation < settings.getNumberOfGenerations()){

            List<Individual> parents = selectionOperator.select(populationP,populationP.size());
            populationQ = crossoverOperator.crossover(parents,populationP.size());
            populationQ = mutationOperator.mutate(populationQ);
            fitnessEvaluator.loadUtilization(populationQ,virtualMachineList,settings.getNumberOfResources());
            populationQ = Reparation.repairPopulation(populationQ,virtualMachineList,physicalMachineList,
                    settings.getNumberOfResources());
            fitnessEvaluator.evaluate(populationQ,virtualMachineList,derivedVMs, physicalMachineList,aPrioriValuesList,
                    settings.getNumberOfResources(),settings.getNumberOfObjFunctions());
            populationP = Evolution.getNextGeneration(populationP,populationQ);
            generation+=1;
        }

         return populationP.getIndividual(0);

    }




}
