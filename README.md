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

1. ALGORITHM = Algorithm Code
2. HEURISTIC_CODE = Heuristic Code
 * FF -> First Fit
 * BF -> Best Fit
 * WF -> Worst Fit
 * FFD -> First Fit Decreasing
 * BFD -> Best Fit Decreasing
3. PM_CONFIG = Load CPU Configuration
 * LOW
 * MED
 * HIGH
 * FULL
 * SATURATED
4. DERIVE_COST = Cost per each derived VM
5. PROTECTION_FACTOR = Protection Factor
6. INTERVAL_EXECUTION_MEMETIC = Periodic Time of MA Execution
7. POPULATION_SIZE = Population size for MA
8. NUMBER_GENERATIONS = Generations size for MA
9. EXECUTION_DURATION = Time of Duration
10. LINK_CAPACITY = Link Capacity for Migration
11. MIGRATION_FACTOR_LOAD = Factor Load per Migration
12. HISTORICAL_DATA_SIZE = Historical Data Sieze
13. FORECAST_SIZE = Forecast Size
14. SCALARIZATION_METHOD = Scalarization Method
 * ED -> Euclidean Distance
 * MD -> Manhattan Distance
 * CD -> Chevyshev Distance
 * WS -> Weighted Sum
15. SCENARIOS = List of Request
