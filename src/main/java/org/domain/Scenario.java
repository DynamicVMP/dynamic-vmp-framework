package org.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class that represents a Scenario
 * <p>
 *     A set of scenarios is the workload trace to be process.
 * </p>
 * @author Saul Zalimben.
 * @since 8/14/16.
 */
public class Scenario implements Comparable<Scenario> {

    private Integer time;

    private Integer cloudServiceID;

    private Integer datacenterID;

    private Integer virtualMachineID;

    private Resources resources;

    private Resources utilization;

    private Revenue revenue;

    private Integer tinit;

    private Integer tend;


    /* Constructors */

    /**
     * Constructor
     * @param time             Current Time
     * @param cloudServiceID   Cloud Service ID
     * @param datacenterID     Datacenter ID
     * @param virtualMachineID Virtual Machine ID
     * @param resources        Resources
     * @param utilization      Utilization
     * @param revenue          Revenue
     * @param tinit            Time init
     * @param tend             Time end
     */
    public Scenario(final Integer time, final Integer cloudServiceID, final Integer datacenterID,
            final Integer virtualMachineID, final Resources resources, final Resources utilization,
            final Revenue revenue, final Integer tinit, final Integer tend) {

        this.time = time;
        this.cloudServiceID = cloudServiceID;
        this.datacenterID = datacenterID;
        this.virtualMachineID = virtualMachineID;
        this.resources = resources;
        this.utilization = utilization;
        this.revenue = revenue;
        this.tinit = tinit;
        this.tend = tend;
    }

    /* Getters and Setters */

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public Integer getCloudServiceID() {
        return cloudServiceID;
    }

    public void setCloudServiceID(Integer cloudServiceID) {
        this.cloudServiceID = cloudServiceID;
    }

    public Integer getDatacenterID() {
        return datacenterID;
    }

    public void setDatacenterID(Integer datacenterID) {
        this.datacenterID = datacenterID;
    }

    public Integer getVirtualMachineID() {
        return virtualMachineID;
    }

    public void setVirtualMachineID(Integer virtualMachineID) {
        this.virtualMachineID = virtualMachineID;
    }

    public Resources getResources() {
        return resources;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    public Resources getUtilization() {
        return utilization;
    }

    public void setUtilization(Resources utilization) {
        this.utilization = utilization;
    }

    public Revenue getRevenue() {
        return revenue;
    }

    public void setRevenue(Revenue revenue) {
        this.revenue = revenue;
    }

    public Integer getTinit() {
        return tinit;
    }

    public void setTinit(Integer tinit) {
        this.tinit = tinit;
    }

    public Integer getTend() {
        return tend;
    }

    public void setTend(Integer tend) {
        this.tend = tend;
    }

    /* Methods */

    /**
     * Compares two scenarios, using the revenue as comparator.
     * @param requestB Scenario to compare
     * @return If scenarioA is greater than scenarioB return 1 <br>
     *     If scenario A is equals to scenarioB return 0 <br>
     *     If scenarioB is greater than scenarioB return -1
     */
    @Override
    public int compareTo(final Scenario requestB) {

        // The average value of the requests A and B
        Float representativeWeightA;
        Float representativeWeightB;

        // If the request A occurs before B, A is better than B
        if (this.getTime() > requestB.getTime()) {
            return 1;
        }
        if (this.getTime() < requestB.getTime()) {
            return -1;
        }

        // Compares the requested Revenue for now
        representativeWeightA = this.getRevenue().totalRevenue();
        representativeWeightB = requestB.getRevenue().totalRevenue();

        if (representativeWeightA > representativeWeightB) {
            return -1;
        } else if (representativeWeightA.equals(representativeWeightB)) {
            return 0;
        }

        // B is better than A
        return 1;
    }

    /**
     * Clones requests inside the [timeMemeticInit, timeMemeticEnd] range
     * @param workload         Workload
     * @param timeMemeticStart Start time Memetic Algorithm.
     * @param timeMemeticEnd   End time Memetic Algorithm
     * @return List of missed requests by Memetic Algorithm order by Revenue
     */
    public static List<Scenario> cloneScenario(List<Scenario> workload, Integer timeMemeticStart,
                                               Integer timeMemeticEnd) {

        List<Scenario> cloneScenario = new ArrayList<>();

        workload.forEach(request -> {
            if(request.getTime() >= timeMemeticStart && request.getTime() <= timeMemeticEnd
                    && request.getTend() >= timeMemeticEnd) {
                cloneScenario.add(request);
            }
        });

        Collections.sort(cloneScenario);
        return cloneScenario;
    }

    /**
     * Print the scenario using {@link System#out}
     */
    public void printS() {

        System.out.print(this.getTime() + "\t");
        System.out.print(this.getCloudServiceID() + "\t");
        System.out.print(this.getDatacenterID() + "\t");
        System.out.print(this.getVirtualMachineID() + "\t");
        System.out.print(this.getResources().getCpu() + "\t");
        System.out.print(this.getResources().getRam() + "\t");
        System.out.print(this.getResources().getNet() + "\t");
        System.out.print(this.getUtilization().getCpu() + "\t");
        System.out.print(this.getUtilization().getRam() + "\t");
        System.out.print(this.getUtilization().getNet() + "\t");
        System.out.print(this.getRevenue().totalRevenue() + "\t");
        System.out.print(this.getTinit() + "\t");
        System.out.print(this.getTend());
        System.out.println();
    }
}
