package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import java.util.List;

/**
 * @author Leonardo Benitez.
 */
public interface Crossover {

    Population crossover(List<Individual> parents, int arity);
    Individual[] crossover(Individual individual1, Individual individual2);
}
