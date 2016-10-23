/*
 * Virtual Machine Placement: Overbooking and Elasticity
 * Author: Rodrigo Ferreira (rodrigofepy@gmail.com)
 * Author: Saúl Zalimben (szalimben93@gmail.com)
 * Author: Leonardo Benitez  (benitez.leonardo.py@gmail.com)
 * Author: Augusto Amarilla (agu.amarilla@gmail.com)
 * 24/9/2016.
 */

package org.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Saul Zalimben.
 * @since 9/24/16.
 */
public class Violation {

    private Map<Integer, Resources> resourcesViolated;

    public Violation(Integer time, Resources resourcesViolation) {

        getResourcesViolated().put(time, resourcesViolation);
    }

    public Map<Integer, Resources> getResourcesViolated() {

        if(resourcesViolated == null) {
            resourcesViolated = new HashMap<>();
        }

        return resourcesViolated;
    }

}
