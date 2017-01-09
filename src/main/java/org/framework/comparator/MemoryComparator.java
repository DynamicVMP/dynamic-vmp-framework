package org.framework.comparator;

import org.domain.VirtualMachine;

import java.util.Comparator;

/**
 * Compares {@link VirtualMachine} bt Memory
 *
 * @author Saul Zalimben.
 * @since 12/31/16.
 */
public class MemoryComparator implements Comparator<VirtualMachine> {

    private final Boolean largerValuesPreferred;

    // Constructor
    public MemoryComparator(){

        this.largerValuesPreferred = true;
    }

    // Default Constructor
    public MemoryComparator(Boolean largerValuesPreferred){

        this.largerValuesPreferred = largerValuesPreferred;
    }

    @Override
    public int compare(VirtualMachine vm1, VirtualMachine vm2) {

        if(largerValuesPreferred){
            return Double.compare(vm1.getResources().get(1),vm2.getResources().get(1)) * -1;
        }else{
            return Double.compare(vm1.getResources().get(1),vm2.getResources().get(1));
        }

    }
}
