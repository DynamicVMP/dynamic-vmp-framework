package org.framework.algorithm.stateOfArt;

import org.domain.Scenario;

import java.util.List;

/**
 * @author Saul Zalimben.
 * @since 12/29/16.
 */
public class StateOfArtUtils {

    /**
     * @param workload        Workload Scenario
     * @param memeticTimeInit Memetic Time init
     * @param memeticTimeEnd  Memetic Time end
     * @return <b>True</b>, if a VM is requested during migration <br> <b>False</b>, otherwise
     */
    public static boolean newVmDuringMemeticExecution(List<Scenario> workload, Integer memeticTimeInit,
            Integer memeticTimeEnd) {

        List<Scenario> cloneScenario = Scenario.cloneScenario(workload, memeticTimeInit, memeticTimeEnd);

        for (Scenario request : cloneScenario) {
            if (request.getTime() <= request.getTinit()) {
                return true;
            }
        }

        return false;
    }

}
