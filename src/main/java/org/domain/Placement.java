package org.domain;

import org.framework.ObjectivesFunctions;
import org.framework.Utils;

import java.util.List;

/**
 * Class that represents a Placement.
 * <p>
 *     A placement has a set of physical machines,
 *     a set of allocated virtual machines,
 *     a set of derived virtual machines,
 *     the total memory migrated,
 *     and his score
 * </p>
 * @author Leonardo Benitez.
 */
public class Placement  {

    private List<PhysicalMachine> physicalMachines;
    private List<VirtualMachine> virtualMachineList;
    private Float   totalMigratedMemory;
    private List<VirtualMachine> derivedVMs;
    private Float placementScore;

    /**
     * Constructor
     * @param physicalMachines   List of Physical Machine
     * @param virtualMachineList List of allocated Virtual Machine
     * @param derivedVMs         List of derived Virtual Machine
     */
    public Placement(final List<PhysicalMachine> physicalMachines, final List<VirtualMachine> virtualMachineList,
            final List<VirtualMachine> derivedVMs) {

        this.physicalMachines = physicalMachines;
        this.virtualMachineList = virtualMachineList;
        this.derivedVMs = derivedVMs;
    }

    /* Constructors */

    /**
     * Constructor
     * @param physicalMachines   List of Physical Machine
     * @param virtualMachineList List of allocated Virtual Machine
     * @param derivedVMs         List of derived Virtual Machine
     * @param placementScore     Placement Score
     */
    public Placement(final List<PhysicalMachine> physicalMachines, final List<VirtualMachine> virtualMachineList,
            final List<VirtualMachine> derivedVMs, Float placementScore) {

        this.physicalMachines = physicalMachines;
        this.virtualMachineList = virtualMachineList;
        this.derivedVMs = derivedVMs;
        this.placementScore = placementScore;
    }

    /* Getters and Setters */

    public List<VirtualMachine> getVirtualMachineList() {
        return virtualMachineList;
    }

    public void setVirtualMachineList(List<VirtualMachine> virtualMachineList) {
        this.virtualMachineList = virtualMachineList;
    }

    public List<VirtualMachine> getDerivedVMs() {
        return derivedVMs;
    }

    public void setDerivedVMs(List<VirtualMachine> derivedVMs) {
        this.derivedVMs = derivedVMs;
    }

    public List<PhysicalMachine> getPhysicalMachines() {

        return physicalMachines;
    }

    public void setPhysicalMachines(final List<PhysicalMachine> physicalMachines) {

        this.physicalMachines = physicalMachines;
    }

    public Float getPlacementScore() {

        return placementScore;
    }

    public void setPlacementScore(final Float placementScore) {

        this.placementScore = placementScore;
    }

    public Float getTotalMigratedMemory() {
        return totalMigratedMemory;
    }

    public void setTotalMigratedMemory(Float totalMigratedMemory) {
        this.totalMigratedMemory = totalMigratedMemory;
    }

    /* Methods */

    /**
     * Update the Placement Score
     * @param aPrioriValueList List of AprioriValues
     */
    public void updatePlacementScore(List<APrioriValue> aPrioriValueList){

        Float[] objectiveFunctions = ObjectivesFunctions.loadObjectiveFunctions(this.getVirtualMachineList(),
                this.getDerivedVMs(),this.getPhysicalMachines());

        Float score = Utils.calcPlacemenScore(objectiveFunctions,aPrioriValueList);
        this.setPlacementScore(score);
    }

}
