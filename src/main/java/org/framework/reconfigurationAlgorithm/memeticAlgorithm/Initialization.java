package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.framework.Utils;

/**
 * @author Leonardo Benitez.
 */
public class Initialization {

    /**
     * @param numberOfVMs Number of Virtual Machines
     * @param numberOfPMs Number of Physical Machines
     * @param maSettings  MA settings
     * @return First Population
     */
    public Population initialize( int numberOfVMs, int numberOfPMs, MASettings maSettings){

        Population population  = new Population();

        for(int iteratorIndividual = 0; iteratorIndividual<maSettings.getPopulationSize();iteratorIndividual++){

            Individual individual  = new Individual(maSettings.getNumberOfObjFunctions(),numberOfVMs,numberOfPMs,maSettings.getNumberOfResources());

            for(int iteratorSolution = 0; iteratorSolution<numberOfVMs; iteratorSolution++){
                individual.getSolution()[iteratorSolution] = generateSolutionPosition(numberOfPMs,false);
            }
            population.getIndividuals().add(individual);
        }
        return population;
    }

    /**
     * @param maxPossible Max Value possible
     * @param includeZero <b>True</b>, if ZERO is included <br> <b>False</b>, otherwise
     * @return Solution Position
     */
    public int generateSolutionPosition(int maxPossible, boolean includeZero){

        if(includeZero){
            return Utils.getRandomInt(0,maxPossible);
        }else{
            return Utils.getRandomInt(1,maxPossible);
        }
    }

}
