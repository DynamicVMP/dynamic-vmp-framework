package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.framework.Utils;

/**
 * @author Leonardo Benitez.
 */
public class UniformMutation implements Mutation {


    @Override
    public Population mutate(Population population) {

        for(int iteratorIndividual =0; iteratorIndividual<population.size(); iteratorIndividual++){

            Individual individualMutated = mutate(population.getIndividual(iteratorIndividual));
            population.setIndividual(individualMutated,iteratorIndividual);

        }

        return population;

    }

    @Override
    public Individual mutate(Individual individual) {

        int numberOfVMs = individual.getSolution().length;
        int numberOfPMs = individual.getUtilization().length;
        int oldPhysicalPosition, newPhysicalPosition;


        for(int iteratorSolution=0; iteratorSolution<numberOfVMs; iteratorSolution++){
            oldPhysicalPosition = individual.getSolution()[iteratorSolution];
            if(Utils.getRandomDouble() < 1F/numberOfVMs) {
                do{
                    newPhysicalPosition = Utils.getRandomInt(1, numberOfPMs);
                    if(newPhysicalPosition != oldPhysicalPosition){
                        individual.getSolution()[iteratorSolution] = newPhysicalPosition;
                    }
                }while (newPhysicalPosition==oldPhysicalPosition && numberOfPMs>1);
            }
        }

        return individual;
    }
}
