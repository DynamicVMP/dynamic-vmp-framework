package org.domain;

import org.framework.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that represent a Virtual Machine
 * @author Saul Zalimben.
 * @since 8/14/16.
 */
public class VirtualMachine implements Comparable<VirtualMachine> {

    private Integer id;

    private List<Float> resources;

    private Revenue revenue;

    private Integer tinit;

    private Integer tend;

    private List<Float> utilization;

    private Integer datacenter;

    private Integer cloudService;

    private Integer physicalMachine;

    /* Constructors */

    /**
     * Constructor with all resources and utilization init to ZERO
     * @param resources Resources
     * @param utilization Utilization
     */
    public VirtualMachine( List<Float> resources,  List<Float> utilization) {

        for (int i =0; i < 3; i++) {
            Float newResource = 0F;
            resources.add(newResource);
            Float newUtilization = 0F;
            utilization.add(newUtilization);
        }

        this.resources = resources;
        this.utilization = utilization;
    }

    /**
     * Constructor
     * @param id              Virtual Machine ID
     * @param resources       Resources
     * @param revenue         Revenue
     * @param tinit           Time init
     * @param tend            Time end
     * @param utilization     Utilization
     * @param datacenter      Datacenter
     * @param cloudService    Cloud Service
     * @param physicalMachine Physical Machine
     */
    public VirtualMachine(Integer id,  List<Float> resources,  Revenue revenue,
             Integer tinit,  Integer tend,  List<Float> utilization,  Integer datacenter,
             Integer cloudService,  Integer physicalMachine) {

        this.id = id;
        this.resources = Utils.getListClone(resources);
        this.revenue = revenue;
        this.utilization = Utils.getListClone(utilization);
        this.datacenter = datacenter != null ? new Integer(datacenter) : null;
        this.cloudService = new Integer(cloudService);
        this.physicalMachine =  physicalMachine != null ? new Integer(physicalMachine) : null;
        this.tinit = tinit;
        this.tend = tend;
    }

    /**
     * Constructor
     * @param id              Virtual Machine ID
     * @param resources       Resources
     * @param revenue         Revenue
     * @param tinit           Time init
     * @param tend            Time end
     * @param utilization     Utilization
     * @param datacenter      Datacenter
     * @param cloudService    Cloud Service
     * @param physicalMachine Physical Machine
     */
    public VirtualMachine(Integer id,  Resources resources,  Revenue revenue,
                          Integer tinit,  Integer tend,  Resources utilization,  Integer datacenter,
                          Integer cloudService,  Integer physicalMachine) {

        this.id = id;
        this.resources = Arrays.asList(resources.getCpu(),resources.getRam(),resources.getNet());
        this.revenue = revenue;
        this.utilization = Arrays.asList(utilization.getCpu(),utilization.getRam(),utilization.getNet());
        this.datacenter = datacenter != null ? new Integer(datacenter) : null;
        this.cloudService = new Integer(cloudService);
        this.physicalMachine =  physicalMachine != null ? new Integer(physicalMachine) : null;
        this.tinit = tinit;
        this.tend = tend;
    }

    /* Getters and Setters */

    public List<Float> getResources() {

        return resources;
    }

    public void setResources( List<Float> resources) {

        this.resources = resources;
    }

    public Integer getTinit() {

        return tinit;
    }

    public void setTinit( Integer tinit) {

        this.tinit = tinit;
    }

    public Integer getTend() {

        return tend;
    }

    public void setTend( Integer tend) {

        this.tend = tend;
    }

    public List<Float> getUtilization() {

        return utilization;
    }

    public void setUtilization( List<Float> utilization) {

        this.utilization = utilization;
    }

    public Integer getPhysicalMachine() {

        return physicalMachine;
    }

    public void setPhysicalMachine( Integer physicalMachine) {

        this.physicalMachine = physicalMachine;
    }

    public Integer getId() {

        return id;
    }

    public void setId( Integer id) {

        this.id = id;
    }

    public Integer getDatacenter() {

        return datacenter;
    }

    public void setDatacenter( Integer datacenter) {

        this.datacenter = datacenter;
    }

    public Integer getCloudService() {

        return cloudService;
    }

    public void setCloudService( Integer cloudService) {

        this.cloudService = cloudService;
    }

    public Revenue getRevenue() {

        return revenue;
    }

    public void setRevenue(final Revenue revenue) {

        this.revenue = revenue;
    }

    /* Methods */

    /**
     * Compare two Virtual Machine Objects
     *
     * @param obj Virtual Machine
     *
     * @return <b>True</b>, if and only if VM ID, Service ID and DC ID match <br>
     * <b>False</b>, otherwise
     */
    @Override
    public boolean equals( Object obj) {

        VirtualMachine vm = (VirtualMachine) obj;

        return vm != null
                && this.getCloudService().equals(vm.getCloudService())
                && this.getDatacenter().equals(vm.getDatacenter())
                && this.getId().equals(vm.getId());

    }

    /**
     * Create a copy of a Virtual Machine
     * @return Cloned VM
     */
    public VirtualMachine cloneVM() {

        return new VirtualMachine(this.getId(), this.getResources(), this.getRevenue(), this.getTinit(),
                this.getTend(), this.getUtilization(),this.getDatacenter(), this.getCloudService(),
                this.getPhysicalMachine());

    }

    /**
     * Create a copy of each VM in a list
     * @param virtualMachines List of Virtual Machines
     * @return Copy of virtualMachines
     */
    public static List<VirtualMachine> cloneVMsList(final List<VirtualMachine> virtualMachines) {

        List<VirtualMachine> cloneVM = new ArrayList<>();

        virtualMachines.forEach(vm ->
            cloneVM.add((VirtualMachine) vm.cloneVM()));

        return cloneVM;
    }

    /**
     * Get VM by Id
     * @param vmId            Virtual Machine Id
     * @param cloudServiceId  Cloud Service Id
     * @param virtualMachines List of VMs
     * @return Virtual Machine
     */
    public static VirtualMachine getById(Integer vmId, Integer cloudServiceId, List<VirtualMachine> virtualMachines) {

        for (VirtualMachine vm : virtualMachines) {
            if (vm.getId().equals(vmId) && vm.getCloudService().equals(cloudServiceId)) {
                return vm;
            }
        }
        return null;
    }

    @Override
    public int compareTo(VirtualMachine o) {

        int compare = 0;

        if(this.getTotalRevenue() > o.getTotalRevenue()) {
            return 1;
        } else  if(this.getTotalRevenue() < o.getTotalRevenue()) {
            return -1;
        }

        return compare;

    }

    /**
     * Get the total Revenue
     * @return Total Revenue
     */
    public Float getTotalRevenue() {

        return this.getRevenue().totalRevenue();

    }
}
