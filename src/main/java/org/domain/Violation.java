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
 * Class that represents a Resource Violation
 * <p>
 *  Track resources violation per VM, per Time and per resources <br>
 *  <pre>
 *  violation {
 *      { virtualMachineId = 1,
 *        { time = 1,
 *          { cpuViolation = X,
 *            ramViolation = X,
 *            netViolation = x }
 *          }
 *        }
 *      }
 * </pre>
 *
 * @author Saul Zalimben.
 * @since 9/24/16.
 */
public class Violation {

    private Map<Integer, Resources> resourcesViolated;

    /* Constructors */

    public Violation(Integer time, Resources resourcesViolation) {

        getResourcesViolated().put(time, resourcesViolation);
    }

    /* Getters and Setters */

    public Map<Integer, Resources> getResourcesViolated() {

        if(resourcesViolated == null) {
            resourcesViolated = new HashMap<>();
        }

        return resourcesViolated;
    }

}
