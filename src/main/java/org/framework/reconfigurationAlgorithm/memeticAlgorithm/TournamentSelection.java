package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.framework.Utils;
import org.framework.comparator.DistanceComparator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leonardo Benitez.
 */
public class TournamentSelection implements Selection {

    private int size;

    private final FitnessComparator comparator;


    public TournamentSelection(){

        this(2,new DistanceComparator(false));
    }

    public TournamentSelection(int size, FitnessComparator comparator){
        this.size =size;
        this.comparator = comparator;
    }


    @Override
    public List<Individual> select(Population population, int arity) {

        List<Individual> parents = new ArrayList<>();
        while(parents.size()<= arity){
            Individual individual  = select(population);
            parents.add(individual);
        }

        return parents;
    }

    @Override
    public Individual select(Population population) {

        Individual winner = population.getIndividual(Utils.getRandomtInt(population.size()));
        for(int iterator=1;iterator<size;iterator++) {
            Individual candidate = population.getIndividual(Utils.getRandomtInt(population.size()));

            int result = comparator.compare(winner, candidate);

            if (result > 0) {
                winner = candidate;
            }

        }
        return winner;
    }
}