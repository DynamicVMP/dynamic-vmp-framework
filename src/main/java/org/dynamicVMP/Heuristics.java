/*
 * Virtual Machine Placement: Overbooking and Elasticity
 * Author: Rodrigo Ferreira (rodrigofepy@gmail.com)
 * Author: Saúl Zalimben (szalimben93@gmail.com)
 * Author: Leonardo Benitez  (benitez.leonardo.py@gmail.com)
 * Author: Augusto Amarilla (agu.amarilla@gmail.com)
 * 21/8/2016.
 */

package org.dynamicVMP;

import org.domain.*;
import org.dynamicVMP.comparators.BestComparator;
import org.dynamicVMP.comparators.WorstComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Saul Zalimben.
 * @since 8/15/16.
 */
public class Heuristics {

    /**
     * Algorithm Interface
     */
    @FunctionalInterface
    interface Algorithm {
        Boolean useAlgorithm(Scenario s, List<PhysicalMachine> physicalMachines,
                List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs);
    }

    /**
     * List of Heuristics Algorithms
     */
    private static Algorithm[] algorithms = new Algorithm[] {
            Heuristics::firstFit,
            Heuristics::bestFit,
            Heuristics::worstFit,
            Heuristics::firstFit,  // First Fit Decreasing
            Heuristics::bestFit,   // Worst Fit Decreasing
    };

    /**
     *
     * @return Array of Pointers to Function
     */
    public static Algorithm[] getAlgorithms() {
        return algorithms;
    }



    /**
     * Update VM
     * @param s                    Scenario
     * @param virtualMachines      Virtual Machines
     * @param derivedVMs           List of Derived Virtual Machine
     * @param physicalMachines     List of Physical Machines
     * @param faultTolerance       Flag that indicates if it is applying tolerance to failures
     * @param protectionFactor     Flag that indicates the degree of Overbooking
     * @return <b>True</b>, if PM can host new Resources
     */
    public static Boolean updateVM(Scenario s, List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs,
            List<PhysicalMachine> physicalMachines, Boolean
            isMigrationActive) {

        Boolean success = false;
        PhysicalMachine physicalMachine;
        Resources utilization;

        // If Migration is active we add an MIGRATION_FACTOR_LOAD
        if(isMigrationActive) {
            utilization = new Resources(
                    s.getUtilization().getCpu() + DynamicVMP.MIGRATION_FACTOR_LOAD,
                    s.getUtilization().getRam(),
                    s.getUtilization().getNet());
        } else {
            utilization = s.getUtilization();
        }

        VirtualMachine updatedVM = new VirtualMachine(s.getVirtualMachineID(), s.getResources(), s.getRevenue(),
                s.getTinit(), s.getTend(), utilization, s.getDatacenterID(), s.getCloudServiceID(), null);

        //todo refactorizar getVMbyID
        for (VirtualMachine vm : virtualMachines) {
            physicalMachine = PhysicalMachine.getById(vm.getPhysicalMachine(), physicalMachines);
            if (updatedVM.equals(vm) && physicalMachine != null) {

                if (Constraints.checkResources(physicalMachine, vm, updatedVM, virtualMachines,true)) {

                    for (int k = 0; k < physicalMachine.getResources().size(); k++) {
                        physicalMachine.updateResource(k, vm.getResources().get(k) * vm.getUtilization().get(k)/100,
                                Utils.SUB);
                    }

                    updateVmResources(virtualMachines, updatedVM);
                    allocateVMToPM(updatedVM, physicalMachine);
                    return true;
                } else {
                    getViolation(s.getTime(), vm, updatedVM, physicalMachine);
                    return false;
                }
            } else {
                // This means that the VM was allocated in other Datacenter that belongs to the same Federation Cloud
                success = true;
            }
        }

        for(VirtualMachine derivedVM : derivedVMs){
            if(derivedVM.getId().equals(updatedVM.getId())){
                success=true;
                break;
            }
        }
        return success;
    }

    /**
     * Allocate a VM to a PM
     * @param vm Virtual Machine
     * @param pm Physical Machine
     */
    public static void allocateVMToPM(VirtualMachine vm, PhysicalMachine pm) {

        pm.updateResource(0, vm.getResources().get(0) * vm.getUtilization().get(0) / 100, Utils.SUM);
        pm.updateResource(1, vm.getResources().get(1) * vm.getUtilization().get(1) / 100, Utils.SUM);
        pm.updateResource(2, vm.getResources().get(2) * vm.getUtilization().get(2) / 100, Utils.SUM);

        pm.updateUtilization();
    }

    /**
     * Return the penalty for SLA Violation
     * @param timeViolation Time Violation
     * @param oldVm         Virtual Machine (previous version of Virtual Machine)
     * @param vm            Virtual Machine (new version of Virtual Machine)
     * @param pm            Physical Machine
     */
    public static void getViolation(Integer timeViolation, VirtualMachine oldVm, VirtualMachine vm,
            PhysicalMachine pm) {

        Float cpuViolation = 0F;
        Float ramViolation = 0F;
        Float netViolation = 0F;

        Float cpu = pm.getResourcesRequested().get(0)
                - (oldVm.getResources().get(0) * oldVm.getUtilization().get(0)/100 )
                + (vm.getResources().get(0) * vm.getUtilization().get(0)/100);

        Float ram = pm.getResourcesRequested().get(1)
                - (oldVm.getResources().get(1) * oldVm.getUtilization().get(1)/100 )
                + (vm.getResources().get(1) * vm.getUtilization().get(1)/100);


        Float net = pm.getResourcesRequested().get(2)
                - (oldVm.getResources().get(2) * oldVm.getUtilization().get(2)/100 )
                + (vm.getResources().get(2) * vm.getUtilization().get(2)/100);

        if(pm.getResources().get(0) <= cpu) {
            cpuViolation = cpu - pm.getResources().get(0);
        }

        if(pm.getResources().get(1) <= ram) {
            ramViolation = ram - pm.getResources().get(1);
        }

        if(pm.getResources().get(2) <= net) {
            netViolation = net - pm.getResources().get(2);
        }

        Resources res = new Resources(cpuViolation, ramViolation, netViolation);
        Violation violation = new Violation(timeViolation, res);

        DynamicVMP.updateEconomicalPenalties(vm,res);
        DynamicVMP.getUnsatisfiedResources().put(vm.getId(), violation);
    }

    /**
     * Update the Resources and Utilization of a VM.
     *
     * @param virtualMachines Virtual Machines
     * @param updatedVM       Updated VM
     */
    public static void updateVmResources(List<VirtualMachine> virtualMachines, VirtualMachine updatedVM) {

        virtualMachines.forEach(vm -> {

            if (vm.equals(updatedVM)) {
                vm.setUtilization(updatedVM.getUtilization());
                vm.setResources(updatedVM.getResources());
            }
        });
    }

    /**
     * Remove VM when its lifetime expired
     * @param virtualMachines  List of VMs
     * @param timeUnit         Current time
     * @param physicalMachines List of Physical Machines
     */
    public static void removeVMByTime(List<VirtualMachine> virtualMachines, Integer timeUnit,
            List<PhysicalMachine> physicalMachines) {

        List<VirtualMachine> toRemoveVM = new ArrayList<>();

        virtualMachines.forEach(vm -> {
            if (vm.getTend() <= timeUnit) {
                Integer pmId = vm.getPhysicalMachine();
                PhysicalMachine pm = PhysicalMachine.getById(pmId, physicalMachines);
                if(pm != null ) {
                    for (int i = 0; i < vm.getResources().size(); i++) {
                        Float freeResource = vm.getResources().get(i) * vm.getUtilization().get(i)/100;
                        pm.updateResource(i, freeResource, Utils.SUB);
                    }
                    pm.updateUtilization();
                    toRemoveVM.add(vm);
                }
            }
        });
        virtualMachines.removeAll(toRemoveVM);
    }

    /**
     * Remove VM when its lifetime expired
     * @param derivatedVMs  List of VMs
     * @param timeUnit         Current time
     */
    public static void removeDerivatedVMByTime(List<VirtualMachine> derivatedVMs, Integer timeUnit) {

        List<VirtualMachine> toRemoveVM = new ArrayList<>();

        derivatedVMs.forEach(vm -> {
            if (vm.getTend() <= timeUnit) {
                    toRemoveVM.add(vm);
            }
        });
        derivatedVMs.removeAll(toRemoveVM);
    }

    /**
     * First Fit
     * @param s                  Scenario
     * @param physicalMachines   Physical Machines
     * @param virtualMachines    Virtual Machines
     * @param derivedVMs         Derived Virtual Machines
     * @return <b>True</b>, if VM was successfully allocated
     */
    public static Boolean firstFit(Scenario s, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs) {

        if (allocateVMToDC(s, physicalMachines, virtualMachines)) {
            return true;
        }
        // Set PM for derived VM to null
        VirtualMachine vm = new VirtualMachine(s.getVirtualMachineID(), s.getResources(), s.getRevenue(),
                                               s.getTinit(), s.getTend(), s.getUtilization(), s.getDatacenterID(),
                                               s.getCloudServiceID(), null);
        derivedVMs.add(vm);
        return false;
    }

    /**
     * @param s                Scenario
     * @param physicalMachines List of PM
     * @param virtualMachines  List of VM
     * @return <b>True</b>, if DC can host the VM <br> <b>False</b>, otherwise
     */
    private static boolean allocateVMToDC(final Scenario s, final List<PhysicalMachine> physicalMachines,
            final List<VirtualMachine> virtualMachines) {

        Resources uti = new Resources(100F, 100F, 100F);

        VirtualMachine vm = new VirtualMachine(s.getVirtualMachineID(), s.getResources(), s.getRevenue(),
                s.getTinit(), s.getTend(), uti,
                s.getDatacenterID(), s.getCloudServiceID(), null);

        for (PhysicalMachine pm : physicalMachines) {
            if (Constraints.checkResources(pm, null, vm, virtualMachines, false)) {
                // Allocate la VM to VM
                allocateVMToPM(vm, pm);
                vm.setPhysicalMachine(pm.getId());
                virtualMachines.add(vm);
                return true;
            }
        }
        return false;
    }

    /**
     * Best Fit
     * @param s                  Scenario
     * @param physicalMachines   Physical Machines
     * @param virtualMachines    Virtual Machines
     * @param derivedVMs         Derived Virtual Machines
     * @return <b>True</b>, if VM was successfully allocated
     */
    public static Boolean bestFit(Scenario s, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs) {

        return bestOrWorstFit(true, s, physicalMachines, virtualMachines, derivedVMs);

    }

    /**
     * Best/Worst Fit
     * @param s                  Scenario
     * @param physicalMachines   Physical Machines
     * @param virtualMachines    Virtual Machines
     * @param derivedVMs         Derived Virtual Machines
     * @return <b>True</b>, if VM was successfully allocated
     */
    public static Boolean  bestOrWorstFit(Boolean isBest, Scenario s, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs) {

        if (isBest) {
            Collections.sort(physicalMachines, new BestComparator());
        } else {
            Collections.sort(physicalMachines, new WorstComparator());
        }

        if (allocateVMToDC(s, physicalMachines, virtualMachines)) {
            return true;
        }

        VirtualMachine vm = new VirtualMachine(s.getVirtualMachineID(), s.getResources(), s.getRevenue(),
                                               s.getTinit(), s.getTend(), s.getUtilization(), s.getDatacenterID(),
                                               s.getCloudServiceID(), 0);
        derivedVMs.add(vm);

        return false;
    }

    /**
     * Worst Fit
     * @param s                  Scenario
     * @param physicalMachines   Physical Machines
     * @param virtualMachines    Virtual Machines
     * @param derivedVMs         Derived Virtual Machines
     * @return <b>True</b>, if VM was successfully allocated
     */
    public static Boolean worstFit(Scenario s, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs) {

        return bestOrWorstFit(false, s, physicalMachines, virtualMachines, derivedVMs);

    }


}
