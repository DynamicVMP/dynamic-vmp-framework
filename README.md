# Dynamic Virutal Machine Placement Framework

This project contains the source code and inputs of the simulations for the article entitled:

## *"Virtual Machine Placement for Elastic Infrastructures in Overbooked Cloud Computing Datacenters Under Uncertainty"*
                                              
 *Future Generation Computer Systems, 2017* \
 **Authors**: F. Lopez-Pires and B. Baran and A. Amarilla and L. Benitez and S. Zalimben

###### Abstract

Infrastructure as a Service (IaaS) providers must support requests for virtual resources in highly dynamic cloud computing envi-ronments.  Due to the randomness of customer requests, Virtual Machine Placement (VMP) problems should be formulated underuncertainty.  This work presents a novel two-phase optimization scheme for the resolution of VMP problems for cloud computingunder uncertainty of several relevant parameters, combining advantages of online and offline formulations in dynamic environmentsconsidering service elasticity and overbooking of physical resources. In this context, a formulation of a VMP problem is presented,considering the optimization of the following four objective functions: (1) power consumption, (2) economical revenue, (3) resourceutilization and (4) placement reconfiguration time. The proposed two-phase optimization scheme includes novel methods to decidewhen to trigger a placement reconfiguration through migration of virtual machines (VMs) between physical machines (PMs) andwhat to do with VMs requested during the reconfiguration period.  An experimental evaluation against state-of-the-art alternativeapproaches for VMP problems was performed considering 400 scenarios.  Experimental results indicate that the proposed schemeoutperforms other evaluated alternatives, improving the quality of solutions in a scenario-based uncertainty model considering thefollowing evaluation criteria among solutions: (1) average, (2) minimum and (3) maximum objective function cost.

### Development and execution  

To develop and execute the following items are required:

#### To develop:

1. Maven 3 or greater
2. Java 8 (JDK 1.8)
3. Java IDE (i.e. eclipse, intellij, ...)

#### To run:
The framework could be compile with maven.

1. Go to the project root and execute:
``` bash
$ mvn clean package
```

2. In the target directory is the compiled framework, to execute use the following command:
``` bash
$ java -jar target/DynamicVMPFramework.jar parameter
```

### Input & Output Data
#### Input File
- *parameter:* Configuration file and scenarios

##### Parameter File Structure

1. ALGORITHM = Algorithm Code
2. HEURISTIC_CODE = Heuristic Code (*Algorithm for the incremental phase (iVMP)*)
 * FF -> First Fit
 * BF -> Best Fit
 * WF -> Worst Fit
 * FFD -> First Fit Decreasing
 * BFD -> Best Fit Decreasing
3. PM_CONFIG = Load CPU Configuration (*Utilization percentage*)
 * LOW -> (=<30%)
 * MED  -> (=<60%)
 * HIGH  -> (=<90%)
 * FULL  -> (=<98%)
 * SATURATED  -> (=<120%)
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

#### Output Data
The framework generates the following files:
- *economical_penalties*: Average economical penalties per each SLA violation. 
- *economical_revenue*: Average ecomical revenue per each VM hosted in the main provider.
- *leasing_costs*: Average economical revenue lost per each VM hosted in an alternative provider from federation.
- *power_consumption*: Average power energy consumed 
- *reconfiguration_call_times*: Number of reconfiguration calls.
- *wasted_resources*: Average of wasted resources (one column per resource)
- *wasted_resources_ratio*: Average of wasted resources (considering all resources)
- *scenarios_scores*: Score per each executed scenario.