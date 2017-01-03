package org.domain;

/**
 * Class that represents the Revenue.
 * <p>
 *     This class works as a wrapper of the Virtual Machine revenue,
 *     is a set of the revenue per each resource (CPU, RAM and NET).
 * </p>
 * @author Saul Zalimben.
 * @since 8/20/16.
 */
public class Revenue {

    private Float cpu;
    private Float ram;
    private Float net;

    /* Constructors */

    /**
     * Constructor
     * @param cpu CPU
     * @param ram RAM
     * @param net NET
     */
    public Revenue(Float cpu, Float ram, Float net) {
        this.cpu = cpu;
        this.ram = ram;
        this.net = net;
    }

    /**
     * Default Constructor <br>
     * All resources init to ZERO
     */
    public Revenue() {
        this.cpu = 0F;
        this.ram = 0F;
        this.net = 0F;
    }

    /* Getters and Setters */

    public Float getCpu() {
        return cpu;
    }

    public void setCpu(Float cpu) {
        this.cpu = cpu;
    }

    public Float getRam() {
        return ram;
    }

    public void setRam(Float ram) {
        this.ram = ram;
    }

    public Float getNet() {
        return net;
    }

    public void setNet(Float net) {
        this.net = net;
    }

    /* Methods */

    /**
     * Get the sum of the revenue per each resource
     * @return Total Revenue
     */
    public Float totalRevenue(){
        return this.getCpu() + this.getRam() + this.getNet();
    }
}
