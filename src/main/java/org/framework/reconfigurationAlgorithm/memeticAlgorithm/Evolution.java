package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.framework.comparator.DistanceComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Leonardo Benitez.
 */
public class Evolution {

    private Evolution() {
        // Default Constructor
    }

    /**
     *
     * @param populationP Population P
     * @param populationQ Population Q
     * @return New Generation of Population
     */
    public static Population getNextGeneration(Population populationP, Population populationQ) {

        List<Individual> individualsPQ = new ArrayList<>();
        individualsPQ.addAll(populationP.getIndividuals());
        individualsPQ.addAll(populationQ.getIndividuals());
        Comparator<Individual> distanceComparator = new DistanceComparator(Boolean.FALSE);

        Collections.sort(individualsPQ,distanceComparator);

        Population population = new Population();
        population.setIndividuals(individualsPQ);
        population.truncate(populationP.size());
        return population;
    }
}
