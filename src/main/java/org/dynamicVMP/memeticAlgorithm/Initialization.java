package org.dynamicVMP.memeticAlgorithm;

import org.dynamicVMP.Utils;

/**
 * @author Leonardo Benitez.
 */
public class Initialization {

    /**
     *
     * @param numberOfVMs
     * @param numberOfPMs
     * @param maSettings
     * @return
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
     *
     * @param maxPossible
     * @param includeZero
     * @return
     */
    public int generateSolutionPosition(int maxPossible, boolean includeZero){

        if(includeZero){
            return Utils.getRandomInt(0,maxPossible);
        }else{
            return Utils.getRandomInt(1,maxPossible);
        }
    }

}
