package org.domain;

/**
 *
 */
public class Revenue {

    private Float cpu;
    private Float ram;
    private Float net;

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

    public Float totalRevenue(){
        return this.getCpu() + this.getRam() + this.getNet();
    }
}
