package org.framework;

/**
 * DynamicVMP Framework: Parameters
 * @author Saul Zalimben.
 * @since 12/26/16.
 */
public class Parameter {

    // EXPERIMENTS PARAMETERS
    /**
     * Heuristic Algorithm Code
     */
    static String HEURISTIC_CODE;

    /**
     * Physical Machine Configuration
     */
    static String PM_CONFIG;

    /**
     * Penalty for derive VM
     */
    static Float DERIVE_COST;

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
    static Boolean FAULT_TOLERANCE;

    /**
     * Protection Factor:
     * It can take values between 0 and 1,where
     * <ul>
     *     <li>
     *         0 -> No overbooking
     *     </li>
     *     <li>
     *         1 -> Full Overbooking (high risk of violation of SLA)
     *     </li>
     * </ul>
     */
    public static Float PROTECTION_FACTOR;

    /**
     * Interval Execution Memetic Algorithm
     */
    static Integer INTERVAL_EXECUTION_MEMETIC;

    /**
     * Population Size
     */
    static Integer POPULATION_SIZE;

    /**
     * Number of Generations
     */
    static Integer NUMBER_GENERATIONS;

    /**
     * Execution Duration
     */
    static Integer EXECUTION_DURATION;

    /**
     * Link Capacity in Gbps
     */
    static Float LINK_CAPACITY;

    /**
     * Percentage of overload when migration process is active
     */
    static Float MIGRATION_FACTOR_LOAD;

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
     * ED -> Euclidean Distance
     * CD -> Chevyshev Distance
     * MD -> Manhattan Distance
     * WS -> Weighted Sum
     */
    static String SCALARIZATION_METHOD;

    /**
     * Type of Algorithm
     * 0 -> State of Art
     * 1 -> Beloglazob Approach
     * 2 -> Periodic Reconfiguration
     * 3 -> Clever Reconfiguration
     */
    public static Integer ALGORITHM;
}
