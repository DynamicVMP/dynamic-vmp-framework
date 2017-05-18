package org.framework;

import java.util.List;

/**
 * DynamicVMP Framework: Parameters
 * @author Saul Zalimben.
 * @since 12/26/16.
 */
public class Parameter {

    private Parameter() {
        // Default Constructor
    }

    // EXPERIMENTS PARAMETERS

    /**
     * Type of Algorithm
     * 0 = Periodic Reconfiguration
     * 1 = State of Art
     * 2 = Beloglazob Approach
     * 3 = Clever Reconfiguration
     */
    public static Integer ALGORITHM;

    /**
     * Heuristic Algorithm Code
     */
    public static String HEURISTIC_CODE;

    /**
     * Physical Machine Configuration
     */
    public static String PM_CONFIG;

    /**
     * Penalty for derive VM
     */
    public static Float DERIVE_COST;

    /**
     * FAULT_TOLERANCE: Indicates if it is applying tolerance to failures.
     * <ul>
     *     <li>
     *         <b>True</b>, VMs from same Service cannot be hosted in the same PM
     *     </li>
     *     <li>
     *         <b>False</b>, VMs from same Service can be hosted in the same PM
     *     </li>
     * </ul>
     *
     */
    public static Boolean FAULT_TOLERANCE;

    /**
     * List of protection factor per resource:
     * It can take values between 0 and 1,where
     * <ul>
     *     <li>
     *         0 = No overbooking
     *     </li>
     *     <li>
     *         1 = Full Overbooking (high risk of violation of SLA)
     *     </li>
     * </ul>
     */
    public static List<Float> PROTECTION_FACTOR;

    /**
     * List of penalty factor per resource, it can
     * take values greater than 1.
     */
    public static List<Float> PENALTY_FACTOR;

    /**
     * Interval Execution Memetic Algorithm
     */
    public static Integer INTERVAL_EXECUTION_MEMETIC;

    /**
     * Population Size
     */
    public static Integer POPULATION_SIZE;

    /**
     * Number of Generations
     */
    public static Integer NUMBER_GENERATIONS;

    /**
     * Execution Duration
     */
    public static Integer EXECUTION_DURATION;

    /**
     * Link Capacity in Gbps
     */
    public static Float LINK_CAPACITY;

    /**
     * Percentage of overload when migration process is active
     */
    public static Float MIGRATION_FACTOR_LOAD;

    /**
     * Historical objective functions values size
     */
    public static Integer HISTORICAL_DATA_SIZE;

    /**
     * number of values ​​to predict
     */
    public static Integer FORECAST_SIZE;

    /**
     * Map of the Scalarization Method
     * ED = Euclidean Distance
     * CD = Chevyshev Distance
     * MD = Manhattan Distance
     * WS = Weighted Sum
     */
    public static String SCALARIZATION_METHOD;


    /**
     * Max pheromone allowed in ACO.
     */
    public static Float MAX_PHEROMONE;

    /**
     * Pheromone constant for ACO, range [0,1],
     * determines how fast pheromone evaporates.
     * Pheromones evaporates quicker as pheromone
     * constant grows.
     */
    public static Float PHEROMONE_CONSTANT;

    /**
     * Number of ants used for ACO.
     */
    public static Integer N_ANTS;

    /**
     * Number of iterations to be performed in ACO
     * to return a solution.
     */
    public static Integer ACO_ITERATIONS;

    /**
     * VMPr algorithm:
     * - MEMETIC
     * - ACO
     */
    public static String VMPR_ALGORITHM;

    /**
     * Recovering method:
     * - CANCELLATION
     * - UPDATE-BASED
     */
    public static String RECOVERING_METHOD;

    /**
     * Approach:
     * - CENTRALIZED
     * - DISTRIBUTED
     */
    public static String APPROACH;
}
