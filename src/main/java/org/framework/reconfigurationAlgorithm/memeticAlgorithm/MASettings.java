/*
 * Virtual Machine Placement: Overbooking and Elasticity
 * Author: Rodrigo Ferreira (rodrigofepy@gmail.com)
 * Author: Saúl Zalimben (szalimben93@gmail.com)
 * Author: Leonardo Benitez  (benitez.leonardo.py@gmail.com)
 * Author: Augusto Amarilla (agu.amarilla@gmail.com)
 * 26/9/2016.
 */

package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

/**
 * @author Leonardo Benitez.
 */
public class MASettings {

    private	Integer populationSize;
    private	Integer numberOfGenerations;
    private	Integer numberOfResources;
    private	Integer numberOfObjFunctions;
    private	Double crossoverProb;
	private	Integer executionInterval;
	private	Integer executionDuration;
    private Integer executionFirstTime;
    private	Boolean faultTolerance;

    public MASettings(){
    }


    public Integer getPopulationSize() {

		return populationSize;
    }

    public void setPopulationSize(Integer populationSize) {

		this.populationSize = populationSize;
    }

    public Integer getNumberOfGenerations() {
        return numberOfGenerations;
    }

    public void setNumberOfGenerations(Integer numberOfGenerations) {
        this.numberOfGenerations = numberOfGenerations;
    }

    public Integer getNumberOfResources() {
        return numberOfResources;
    }

    public void setNumberOfResources(Integer numberOfResources) {
        this.numberOfResources = numberOfResources;
    }

    public Integer getNumberOfObjFunctions() {
        return numberOfObjFunctions;
    }

    public void setNumberOfObjFunctions(Integer numberOfObjFunctions) {
        this.numberOfObjFunctions = numberOfObjFunctions;
    }

    public Double getCrossoverProb() {
        return crossoverProb;
    }

    public void setCrossoverProb(Double crossoverProb) {
       this.crossoverProb = crossoverProb;
    }

    public Boolean getFaultTolerance() {
        return faultTolerance;
    }

    public void setFaultTolerance(Boolean faultTolerance) {
        this.faultTolerance = faultTolerance;
    }

	public Integer getExecutionInterval() {
		return executionInterval;
	}

	public void setExecutionInterval(Integer executionInterval) {
		this.executionInterval = executionInterval;
	}

	public Integer getExecutionDuration() {
		return executionDuration;
	}

	public void setExecutionDuration(Integer executionDuration) {
		this.executionDuration = executionDuration;
	}

    public Integer getExecutionFirstTime() {

        return executionFirstTime;
    }

    public void setExecutionFirstTime(final Integer executionFirstTime) {

        this.executionFirstTime = executionFirstTime;
    }
}
