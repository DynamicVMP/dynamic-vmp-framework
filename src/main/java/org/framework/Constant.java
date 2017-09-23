package org.framework;

import java.util.HashMap;
import java.util.Map;

/**
 * DynamicVMP Framework: Constants
 * @author Saul Zalimben.
 * @since 12/26/16.
 */
public class Constant {

    /**
     * Files pointers
     */
    public static final String POWER_CONSUMPTION_FILE = Utils.OUTPUT + "power_consumption";
    public static final String ECONOMICAL_REVENUE_FILE = Utils.OUTPUT + "economical_revenue";
    public static final String WASTED_RESOURCES_FILE = Utils.OUTPUT + "wasted_resources";
    public static final String WASTED_RESOURCES_RATIO_FILE = Utils.OUTPUT + "wasted_resources_ratio";
    public static final String SCENARIOS_SCORES = Utils.OUTPUT + "scenarios_scores";
    public static final String PLACEMENT_SCORE = Utils.OUTPUT + "resources_per_scenario";
    public static final String RECONFIGURATION_CALL_TIMES_FILE = Utils.OUTPUT + "reconfiguration_call_times";
    public static final String ECONOMICAL_PENALTIES_FILE = Utils.OUTPUT + "economical_penalties";
    public static final String LEASING_COSTS_FILE = Utils.OUTPUT + "leasing_costs";
    public static final String PLACEMENT_SCORE_BY_TIME_FILE = Utils.OUTPUT + "placement_score_by_time/";

    /**
     * Not a constant but once is setted, is used as
     */
    public static String EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME;

    /**
     * Not a constant but once is setted, is used as
     */
    public static String EXPERIMENTS_PARAMETERS_TO_OUTPUT_NAME;

    /**
     * Heuristics Algorithms
     */
    public static final String BFD = "BFD";
    public static final String FFD = "FFD";
    public static final String BF = "BF";
    public static final String FF = "FF";
    public static final String WF = "WF";

    /**
     * Weight for Weighted Sum (Online)
     */
    public static final Float WEIGHT_ONLINE = 0.33F;

    /**
     * Weight for Weighted Sum (Offline)
     */
    public static final Float WEIGHT_OFFLINE = 0.25F;

    /**
     * Time unit duration in seconds
     */
    public static final Float TIMEUNIT_DURATION = 1F;

    /**
     * Number of Objective Functions
     */
    public static final Integer NUM_OBJ_FUNCT_COMP = 3;

    /**
     * Map of the Heuristics Algorithm
     * HEURISTIC_MAP.put("FF", 0);
     * HEURISTIC_MAP.put("BF", 1);
     * HEURISTIC_MAP.put("WF", 2);
     * HEURISTIC_MAP.put("FFD", 3);
     * HEURISTIC_MAP.put("BFD", 4);
     */
    public static final Map<String, Integer> HEURISTIC_MAP;
    static {
        HEURISTIC_MAP = new HashMap<>();
        HEURISTIC_MAP.put("FF", 0);
        HEURISTIC_MAP.put("BF", 1);
        HEURISTIC_MAP.put("WF", 2);
        HEURISTIC_MAP.put("FFD", 3);
        HEURISTIC_MAP.put("BFD", 4);
    }

    private Constant() {
        // Default Constructor
    }
}

