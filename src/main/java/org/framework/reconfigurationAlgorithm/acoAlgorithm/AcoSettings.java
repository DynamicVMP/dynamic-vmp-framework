package org.framework.reconfigurationAlgorithm.acoAlgorithm;


/**
 * Created by nabil on 2/24/17.
 */

public class AcoSettings {
    private Float maxPheromone;
    private Float pheromoneConstant;
    private Integer nAnts; // numbers of ants to be used
    private Integer acoIterations;

    // Constructor
    public AcoSettings() {
    }

    // Getters and setters


    public Integer getAcoIterations() {
        return acoIterations;
    }

    public void setAcoIterations(Integer acoIterations) {
        this.acoIterations = acoIterations;
    }

    public Float getMaxPheromone() {
        return maxPheromone;
    }

    public void setMaxPheromone(Float maxPheromone) {
        this.maxPheromone = maxPheromone;
    }

    public Float getPheromoneConstant() {
        return pheromoneConstant;
    }

    public void setPheromoneConstant(Float pheromoneConstant) {
        this.pheromoneConstant = pheromoneConstant;
    }

    public Integer getnAnts() {
        return nAnts;
    }

    public void setnAnts(Integer nAnts) {
        this.nAnts = nAnts;
    }
}
