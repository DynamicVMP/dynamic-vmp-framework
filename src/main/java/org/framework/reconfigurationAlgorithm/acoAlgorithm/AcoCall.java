package org.framework.reconfigurationAlgorithm.acoAlgorithm;

import org.domain.APrioriValue;
import org.domain.Placement;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by nabil on 2/28/17.
 */
public class AcoCall implements Callable<Placement>{
    private List<APrioriValue> aPrioriValueList;
    private Placement actualPlacement;
    private AcoSettings acoSettings;

    /**
     * Default constructor to call the ACO algorithm.
     * @param actualPlacement Placement to be improved
     * @param aPrioriValueList Apriori value list for O.F.
     * @param acoSettings ACO settings
     */
    public AcoCall(Placement actualPlacement, List<APrioriValue> aPrioriValueList, AcoSettings acoSettings) {
        this.aPrioriValueList = aPrioriValueList;
        this.actualPlacement = actualPlacement;
        this.acoSettings = acoSettings;
    }


    /**
     * Executes the ACO algorithm, and returns a possibly better new placement.
     * @return Placement
     */
    @Override
    public Placement call() throws Exception {
        Aco aco = new Aco();
        return aco.reconfiguration(actualPlacement, aPrioriValueList, acoSettings);
    }
}
