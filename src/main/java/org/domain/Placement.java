package org.domain;

import org.framework.ObjectivesFunctions;
import org.framework.Utils;

import java.util.List;

/**
 * Created by Leonardo Benitez.
 */
public class Placement  {

    private List<PhysicalMachine> physicalMachines;
    private List<VirtualMachine> virtualMachineList;
    private Float   totalMigratedMemory;
    private List<VirtualMachine> derivedVMs;
    private Float placementScore;

    public Placement(final List<PhysicalMachine> physicalMachines, final List<VirtualMachine> virtualMachineList,
            final List<VirtualMachine> derivedVMs) {

        this.physicalMachines = physicalMachines;
        this.virtualMachineList = virtualMachineList;
        this.derivedVMs = derivedVMs;
    }

    public Placement(final List<PhysicalMachine> physicalMachines, final List<VirtualMachine> virtualMachineList,
            final List<VirtualMachine> derivedVMs, Float placementScore) {

        this.physicalMachines = physicalMachines;
        this.virtualMachineList = virtualMachineList;
        this.derivedVMs = derivedVMs;
        this.placementScore = placementScore;
    }

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


    public void updatePlacementScore(List<APrioriValue> aPrioriValueList){

        Float[] objectiveFunctions = ObjectivesFunctions.loadObjectiveFunctions(this.getVirtualMachineList(),
                this.getDerivedVMs(),this.getPhysicalMachines());

        Float score = Utils.calcPlacemenScore(objectiveFunctions,aPrioriValueList);
        this.setPlacementScore(score);
    }

}
