package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Leonardo Benitez.
 */

public class Population {

    private List<Individual> individuals;


    public Population(){

        this.individuals  = new ArrayList<>();
    }


    public List<Individual> getIndividuals() {

        return individuals;
    }

    public void setIndividuals(List<Individual> individuals) {

        this.individuals = individuals;
    }


    public void sort(Comparator<? super Individual> comparator) {

        Collections.sort(individuals, comparator);
    }

    public Individual getIndividual(int index){

        return individuals.get(index);
    }

    public void setIndividual(Individual individual, int index){

        this.getIndividuals().set(index,individual);
    }

    public void truncate(int size){

        while(individuals.size() > size){
            individuals.remove(individuals.size()-1);
        }

    }

    public int size() {

        return individuals.size();
    }


}
