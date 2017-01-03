package org.domain;

import java.util.List;

/**
 * Class that represents a Cloud Service.
 *  * <p>
 *     A Cloud Service has a set of {@link VirtualMachine}
 * </p>
 * @author Saul Zalimben.
 * @since 8/14/16.
 */
public class CloudService {

    private Integer id;

    private List<VirtualMachine> virtualMachines;

    /* Getters and Setters */

    public CloudService(final Integer id, final List<VirtualMachine> virtualMachines) {

        this.id = id;
        this.virtualMachines = virtualMachines;
    }

    public Integer getId() {

        return id;
    }

    public void setId(final Integer id) {

        this.id = id;
    }

    public List<VirtualMachine> getVirtualMachines() {

        return virtualMachines;
    }

    public void setVirtualMachines(final List<VirtualMachine> virtualMachines) {

        this.virtualMachines = virtualMachines;
    }

}
