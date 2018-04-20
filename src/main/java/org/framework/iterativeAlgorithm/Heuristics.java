/*
 * Virtual Machine Placement: Overbooking and Elasticity
 * Author: Rodrigo Ferreira (rodrigofepy@gmail.com)
 * Author: Saúl Zalimben (szalimben93@gmail.com)
 * Author: Leonardo Benitez  (benitez.leonardo.py@gmail.com)
 * Author: Augusto Amarilla (agu.amarilla@gmail.com)
 * 21/8/2016.
 */

package org.framework.iterativeAlgorithm;

import org.domain.*;
import org.framework.Constraints;
import org.framework.DynamicVMP;
import org.framework.Parameter;
import org.framework.Utils;
import org.framework.comparator.BestComparator;
import org.framework.comparator.WorstComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.domain.VirtualMachine.getById;

/**
 * Heuristics Code.
 * <p>
 *     All heuristic's logic is here
 * </p>
 * @author Saul Zalimben.
 * @since 8/15/16.
 */
public class Heuristics {

    /**
     * Algorithm Interface
     */
    @FunctionalInterface
    public interface Algorithm {
        Boolean useHeuristic(VirtualMachine vm,
            List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines,
            List<VirtualMachine> derivedVMs,
            Boolean isMigration);
    }

    /**
     * List of Heuristics Algorithms
     */
    private static Algorithm[] heuristics = new Algorithm[] {
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
    public static Algorithm[] getHeuristics() {
        return heuristics;
    }



    /**
     * Update VM (allocated and derived).
     * <p>
     *     Update the resources and utilization of the VM and the it's PM host
     * </p>
     * @param s                    Scenario
     * @param virtualMachines      Virtual Machines
     * @param derivedVMs           List of Derived Virtual Machine
     * @param physicalMachines     List of Physical Machines
     * @param isMigrationActive    Is Migration active
     * @return <b>True</b>, if PM can host new Resources
     */
    public static Boolean updateVM(Scenario s, List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs,
            List<PhysicalMachine> physicalMachines, Boolean isMigrationActive) {

        Boolean success = false;
        PhysicalMachine physicalMachine;
        Resources utilization;

        // If Migration is active we add an MIGRATION_FACTOR_LOAD
        if(isMigrationActive) {
            utilization = new Resources(
                    s.getUtilization().getCpu() + Parameter.MIGRATION_FACTOR_LOAD,
                    s.getUtilization().getRam(),
                    s.getUtilization().getNet());
        } else {
            utilization = s.getUtilization();
        }

        // Instance a VM (updateVM)
        VirtualMachine updatedVM = new VirtualMachine(s.getVirtualMachineID(), s.getResources(), s.getRevenue(),
                s.getTinit(), s.getTend(), utilization, s.getDatacenterID(), s.getCloudServiceID(), null);

        // Search allocated VM
        VirtualMachine vm = getById(updatedVM.getId(), updatedVM.getCloudService(), virtualMachines);

        // Check if VM was allocated
        if(vm != null) {
            // Get PM host
            physicalMachine = PhysicalMachine.getById(vm.getPhysicalMachine(), physicalMachines);
            // Check resources
            if (Constraints.checkResources(physicalMachine, vm, updatedVM, virtualMachines,
                true)) {
                // Update allocated VM
                physicalMachine.updatePMResources(vm, Utils.SUB);
                updateVmResources(virtualMachines, updatedVM);
                allocateVMToPM(updatedVM, physicalMachine);
                return true;
            } else {
                getViolation(s.getTime(), vm, updatedVM);
                return false;
            }
        }

        // If VM is not allocated
        for(VirtualMachine derivedVM : derivedVMs){
            if(updatedVM.equals(derivedVM)) {
                updateVmResources(derivedVMs, updatedVM);
                success=true;
            }
        }
        return success;
    }

    /**
     * Allocate VM to PM
     * <p>
     *     Update the resources and utilization of the PM
     * </p>
     * @param vm Virtual Machine
     * @param pm Physical Machine
     */
    private static void allocateVMToPM(VirtualMachine vm, PhysicalMachine pm) {
        pm.updatePMResources(vm, Utils.SUM);
    }

    /**
     * Calculate the penalty for SLA Violation
     * <p>
     *     Register the VM, time and what resource was violated
     * </p>
     *
     * @param timeViolation Time Violation
     * @param oldVm         Virtual Machine (previous version of Virtual Machine)
     * @param vm            Virtual Machine (new version of Virtual Machine)
     */
    private static Boolean getViolation(Integer timeViolation, VirtualMachine oldVm, VirtualMachine vm) {

        // use the protection factor to mitigate the violation
        Float cpu = vm.getResources().get(0) * vm.getUtilization().get(0)/100
                    - (oldVm.getResources().get(0) * oldVm.getUtilization().get(0)/100
                        + oldVm.getResources().get(0) * (1 - oldVm.getUtilization().get(0)/100) * Parameter.PROTECTION_FACTOR.get(0));

        Float ram = vm.getResources().get(1) * vm.getUtilization().get(1)/100
                    - (oldVm.getResources().get(1) * oldVm.getUtilization().get(1)/100
                        + oldVm.getResources().get(1) * (1 - oldVm.getUtilization().get(1)/100) * Parameter.PROTECTION_FACTOR.get(1));

        Float net = vm.getResources().get(2) * vm.getUtilization().get(2)/100
                    - (oldVm.getResources().get(2) * oldVm.getUtilization().get(2)/100
                        + oldVm.getResources().get(2) * (1 - oldVm.getUtilization().get(2)/100) * Parameter.PROTECTION_FACTOR.get(2));

        Float cpuViolation = cpu > 0 ? cpu : 0F;
        Float ramViolation = ram > 0 ? ram : 0F;
        Float netViolation = net > 0 ? net : 0F;

        // if violation was not mitigated by the resources reserved by the protection factor
        if(cpuViolation > 0 || ramViolation > 0 || netViolation > 0) {
            Resources res = new Resources(cpuViolation, ramViolation, netViolation);
            Violation violation = new Violation(timeViolation, res);

            DynamicVMP.updateEconomicalPenalties(vm,res, timeViolation);
            DynamicVMP.unsatisfiedResources.put(vm.getCloudService()+"_"+vm.getId(), violation);
            return false;
        }

        // violation has been successful mitigated by the resources reserved by the protection factor
        return true;
    }

    /**
     * Update the Resources and Utilization of a VM.
     *
     * @param virtualMachines Virtual Machines
     * @param updatedVM       Updated VM
     */
    private static void updateVmResources(List<VirtualMachine> virtualMachines, VirtualMachine updatedVM) {

        virtualMachines.forEach(vm -> {

            if (vm.equals(updatedVM)) {
                vm.setUtilization(updatedVM.getUtilization());
                vm.setResources(updatedVM.getResources());
                vm.setRevenue(updatedVM.getRevenue());
            }
        });
    }

    /**
     * Remove VM when its lifetime expired
     *
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
                    pm.updatePMResources(vm, Utils.SUB);
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
     * @param vm                 VirtualMachine
     * @param physicalMachines   Physical Machines
     * @param virtualMachines    Virtual Machines
     * @param derivedVMs         Derived Virtual Machines
     * @param isMigration      Indicates if the VM is being migrated
     * @return <b>True</b>, if VM was successfully allocated
     */
    private static Boolean firstFit(VirtualMachine vm, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs, Boolean isMigration) {

        if (allocateVMToDC(vm, physicalMachines, virtualMachines, isMigration)) {
            return true;
        }
        // Set PM for derived VM to null
        derivedVMs.add(vm);
        return false;
    }

    /**
     * Allocate VM To DC
     * @param vm               VirtualMachine
     * @param physicalMachines List of PM
     * @param virtualMachines  List of VM
     * @param isMigration      Indicates if the VM is being migrated
     * @return <b>True</b>, if DC can host the VM <br> <b>False</b>, otherwise
     */
    private static boolean allocateVMToDC(final VirtualMachine vm, final List<PhysicalMachine> physicalMachines,
            final List<VirtualMachine> virtualMachines, Boolean isMigration) {

        // If is migration we don't update the utilization
        if(!isMigration) {
            // If the VM is new, we set the utilization to 100%, we don't know this information a priori.
            Resources uti = new Resources(100F, 100F, 100F);
            vm.setUtilization(Arrays.asList(uti.getCpu(), uti.getRam(), uti.getNet()));
        }

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
     * @param vm                 VirtualMachine
     * @param physicalMachines   Physical Machines
     * @param virtualMachines    Virtual Machines
     * @param derivedVMs         Derived Virtual Machines
     * @param isMigration      Indicates if the VM is being migrated
     * @return <b>True</b>, if VM was successfully allocated
     */
    private static Boolean bestFit(VirtualMachine vm, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs, Boolean isMigration) {

        return bestOrWorstFit(true, vm, physicalMachines, virtualMachines, derivedVMs, isMigration);

    }

    /**
     * Best/Worst Fit
     * @param isBest           Boolean
     * @param vm               VirtualMachine
     * @param physicalMachines Physical Machines
     * @param virtualMachines  Virtual Machines
     * @param derivedVMs       Derived Virtual Machines
     * @param isMigration      Indicates if the VM is being migrated
     * @return <b>True</b>, if VM was successfully allocated
     */
    private static Boolean  bestOrWorstFit(Boolean isBest, VirtualMachine vm, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs, Boolean isMigration) {

        if (isBest) {
            Collections.sort(physicalMachines, new BestComparator());
        } else {
            Collections.sort(physicalMachines, new WorstComparator());
        }

        if (allocateVMToDC(vm, physicalMachines, virtualMachines, isMigration)) {
            return true;
        }

        derivedVMs.add(vm);

        return false;
    }

    /**
     * Worst Fit
     * @param vm               VirtualMachine
     * @param physicalMachines Physical Machines
     * @param virtualMachines  Virtual Machines
     * @param derivedVMs       Derived Virtual Machines
     * @param isMigration      Indicates if the VM is being migrated
     * @return <b>True</b>, if VM was successfully allocated
     */
    private static Boolean worstFit(VirtualMachine vm, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs, Boolean isMigration) {

        return bestOrWorstFit(false, vm, physicalMachines, virtualMachines, derivedVMs, isMigration);

    }


}
