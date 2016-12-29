package org.dynamicVMP.stateOfArt;

import org.domain.Scenario;

import java.util.List;

/**
 * @author Saul Zalimben.
 * @since 12/29/16.
 */
public class StateOfArtUtils {

    /**
     *
     * @param workload
     * @param memeticTimeInit
     * @param memeticTimeEnd
     * @return
     */
    public static boolean newVmDuringMemeticExecution(List<Scenario> workload, Integer memeticTimeInit,
            Integer memeticTimeEnd) {

        List<Scenario> cloneScenario = Scenario.cloneScneario(workload, memeticTimeInit, memeticTimeEnd);

        for (Scenario request : cloneScenario) {
            if (request.getTime() <= request.getTinit()) {
                return true;
            }
        }

        return false;
    }

}
