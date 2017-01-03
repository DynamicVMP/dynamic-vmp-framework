package org.domain;

import java.util.List;

/**
 * Class that represents a Datacenter.
 * <p>
 *     A datacenter has a set of {@link PhysicalMachine}
 * </p>
 * @author Saul Zalimben.
 * @since 8/14/16.
 */
public class Datacenter {

    private Integer id;

    private List<PhysicalMachine> physicalMachines;

    /* Getters and Setters **/

    public Integer getId() {

        return id;
    }

    public void setId(final Integer id) {

        this.id = id;
    }

    public List<PhysicalMachine> getPhysicalMachines() {

        return physicalMachines;
    }

    public void setPhysicalMachines(final List<PhysicalMachine> physicalMachines) {

        this.physicalMachines = physicalMachines;
    }
}
