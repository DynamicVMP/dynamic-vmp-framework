package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import java.util.List;

/**
 * @author Leonardo Benitez.
 */
public interface Selection {

    List<Individual> select(Population population, int arity);
    Individual select(Population population);

}
