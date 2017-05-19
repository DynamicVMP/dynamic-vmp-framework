# DynamicVMP Framework

To develop and execute the following items are required:

## To develop:

1. Maven 3 or greater
2. Java 8 (JDK 1.8)
3. Java IDE (i.e. eclipse, intellij, ...)

## To run:
The framework could be compile with maven.

1. Go to the project root and execute:
``` bash
$ mvn clean package
```

2. In the target directory is the compiled framework, to execute use the following command:
``` bash
$ java -jar target/DynamicVMPFramework.jar parameter
```
## Parameter File Structure



1. APPROACH = Algorithm approach
 * CENTRALIZED
 * DISTRIBUTED -> This approach will automatically launch the distributed approach and you don't
                  need to specify the following inputs: VMPr, VMPr_TRIGGERING, VMPr_RECOVERING.
2. iVMP = iVMP algorithm
 * FF -> First Fit
 * BF -> Best Fit
 * WF -> Worst Fit
 * FFD -> First Fit Decreasing
 * BFD -> Best Fit Decreasing
3. VMPr = VMPr algorithm
 * MEMETIC
 * ACO
4. VMPr_TRIGGERING = VMPr triggering
 * PERIODICALLY
 * PREDICTION-BASED
5. VMPr_RECOVERING = VMPr recovering
 * CANCELLATION
 * UPDATE-BASED
6. PM_CONFIG = Load CPU Configuration
 * LOW
 * MED
 * HIGH
 * FULL
 * SATURATED
7. DERIVE_COST = Cost per each derived VM
8. PROTECTION_FACTOR_01 = Resource1 protection factor [0;1]
9. PROTECTION_FACTOR_02 = Resource2 protection factor [0;1]
10. PROTECTION_FACTOR_03 = Resource3 protection factor [0;1]
11. PENALTY_FACTOR_01 = Resource1 penalty factor (greater than 1)
12. PENALTY_FACTOR_02 = Resource1 penalty factor (greater than 1)
13. PENALTY_FACTOR_03 = Resource1 penalty factor (greater than 1)
14. INTERVAL_EXECUTION_MEMETIC = Periodic Time of MA Execution
15. POPULATION_SIZE = Population size for MA
16. NUMBER_GENERATIONS = Generations size for MA
17. EXECUTION_DURATION = Time of Duration
18. LINK_CAPACITY = Link Capacity for Migration
19. MIGRATION_FACTOR_LOAD = Factor Load per Migration
20. HISTORICAL_DATA_SIZE = Historical Data Sieze
21. FORECAST_SIZE = Forecast Size
22. SCALARIZATION_METHOD = Scalarization Method
 * ED -> Euclidean Distance
 * MD -> Manhattan Distance
 * CD -> Chevyshev Distance
 * WS -> Weighted Sum
23. MAX_PHEROMONE = Max pheromone allowed in ACO
24. PHEROMONE_CONSTANT = Pheromone constant for ACO, range [0,1],
determines how fast pheromone evaporates. Pheromones evaporates
quicker as pheromone constant grows
25. N_ANTS = Number of ants used for ACO
26. ACO_ITERATIONS = Number of iterations to be performed in ACO
to return a solution
27. SCENARIOS = List of Request
