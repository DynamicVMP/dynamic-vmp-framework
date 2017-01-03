package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.framework.Utils;

import java.util.List;

/**
 * @author Leonardo Benitez.
 */
public class OnePointCrossover implements Crossover {


    private final Double probability;

    public OnePointCrossover(Double probability){
        this.probability = probability;
    }


    @Override
    public Population crossover(List<Individual> parents, int arity) {

         Population population  = new Population();
         Individual parent1, parent2;

        for(int iteratorIndividual=0;iteratorIndividual<parents.size(); iteratorIndividual++) {
            parent1 = parents.get(iteratorIndividual);

            parent2 = iteratorIndividual % 2 == 0 ? parents.get(iteratorIndividual + 1) : parents.get(iteratorIndividual - 1);

            Individual[] result = crossover(parent1, parent2);

            population.getIndividuals().add(result[0]);
            population.getIndividuals().add(result[1]);

            if (population.size() >= arity) break;
        }
        population.truncate(arity);
        return population;
    }

    @Override
    public Individual[] crossover(Individual individual1, Individual individual2) {

        int crossoverPoint,temp;
        int individualSize = individual1.getSize();
        Individual result1 = individual1.copy();
        Individual result2 = individual2.copy();

        if(Utils.getRandomDouble() <= probability){

            if(individual1.getSize() % 2 == 0){
                crossoverPoint  = individualSize/2;
            }else{
                crossoverPoint = individualSize/2 + 1;
            }

            for(int iterator = 0 ; iterator< crossoverPoint; iterator++){
                temp = result1.getSolution()[iterator];
                result1.getSolution()[iterator] = result2.getSolution()[iterator];
                result2.getSolution()[iterator] = temp;
            }

        }
        return new Individual[]{result1,result2};
    }
}
