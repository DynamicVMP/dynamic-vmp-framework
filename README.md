# Dynamic Virutal Machine Placement Framework

This project contains the source code and results of the following research:

#### *"Two-Phase Virtual Machine Placement Algorithms for Cloud Computing. An experimental Evaluation Under Uncertainty"*.

*Conferencia Latinoamericana de Informática (CLEI), 2017* \
**Authors:** N. Chamas and F. López-Pires and B. Barán \
**Release:** [CLEI-2017](https://github.com/DynamicVMP/dynamic-vmp-framework/releases/tag/CLEI-2017)

##

#### *"Virtual Machine Placement for Elastic Infrastructures in Overbooked Cloud Computing Datacenters Under Uncertainty"*
                                              
 *Future Generation Computer Systems, 2017* \
 **Authors**: F. López-Pires and B. Barán and A. Amarilla and L. Benítez and S. Zalimben \
**Release:**  [FGS-2017](https://github.com/DynamicVMP/dynamic-vmp-framework/releases/tag/FGCS-2017)

###### Abstract

Infrastructure as a Service (IaaS) providers must support requests for virtual resources in highly dynamic cloud computing envi-ronments.  Due to the randomness of customer requests, Virtual Machine Placement (VMP) problems should be formulated underuncertainty.  This work presents a novel two-phase optimization scheme for the resolution of VMP problems for cloud computingunder uncertainty of several relevant parameters, combining advantages of online and offline formulations in dynamic environmentsconsidering service elasticity and overbooking of physical resources. In this context, a formulation of a VMP problem is presented,considering the optimization of the following four objective functions: (1) power consumption, (2) economical revenue, (3) resourceutilization and (4) placement reconfiguration time. The proposed two-phase optimization scheme includes novel methods to decidewhen to trigger a placement reconfiguration through migration of virtual machines (VMs) between physical machines (PMs) andwhat to do with VMs requested during the reconfiguration period.  An experimental evaluation against state-of-the-art alternativeapproaches for VMP problems was performed considering 400 scenarios.  Experimental results indicate that the proposed schemeoutperforms other evaluated alternatives, improving the quality of solutions in a scenario-based uncertainty model considering the following evaluation criteria among solutions: (1) average, (2) minimum and (3) maximum objective function cost.

##

#### *"Evaluating a Two-Phase Virtual Machine PlacementOptimization Scheme for Cloud Computing Datacenters"*

*Metaheuristics International Conference, 2017* \
 **Authors**: F. López-Pires and B. Barán and A. Amarilla and L. Benítez and S. Zalimben \
 **Release:** [MIC-2017](https://github.com/DynamicVMP/dynamic-vmp-framework/releases/tag/1.0)

###### Abstract 

Infrastructure as a Service (IaaS) providers must support requests for virtual resources in complexdynamic cloud computing environments, taking into account service elasticity and overbooking ofphysical resources. Due to the randomness of customer requests, Virtual Machine Placement (VMP)problems should be formulated under uncertainty.  This work proposes an experimental evaluationof a two-phase optimization scheme for VMP problems, studying different (i) online heuristics, (ii)overbooking protection factors and (iii) objective function scalarization methods.  The proposed ex-perimental evaluation considers an uncertain VMP formulation for the optimization of the followingthree objective functions: (i) power consumption, (ii) economical revenue, and (iii) resource utiliza-tion.  Experiments were performed considering 96 different scenarios, representing complex cloudcomputing environments.  Experimental results shows that Best-Fit and Best-Fit Decreasing heuris-tics are recommended in the incremental VMP (iVMP) phase working with the considered MemeticAlgorithm in the VMP reconfiguration (VMPr) phase, adjusting protection factors to 0.00 and 0.75in  low  and  high  CPU  load  scenarios  respectively,  while  scalarazing  the  proposed  three  objectivefunctions considering an Euclidean distance to the origin.

##  

To develop and execute the following items are required:

### To develop:

1. Maven 3 or greater
2. Java 8 (JDK 1.8)
3. Java IDE (i.e. eclipse, intellij, ...)

### To run:
The framework could be compile with maven.

1. Go to the project root and execute:
``` bash
$ mvn clean package
```

2. In the target directory is the compiled framework, to execute use the following command:
``` bash
$ java -jar target/DynamicVMPFramework.jar parameter
```

##
#### Input File
- *parameter:* Configuration file and scenarios

##### Parameter File Structure

1. APPROACH = Algorithm approach
 * CENTRALIZED
 * DISTRIBUTED -> This approach will automatically launch the distributed approach and you don't need to specify the following inputs: VMPr, VMPr_TRIGGERING, VMPr_RECOVERING.
 
2. iVMP = Algorithm for the incremental phase (iVMP).
 * FF -> First Fit
 * BF -> Best Fit
 * WF -> Worst Fit
 * FFD -> First Fit Decreasing
 * BFD -> Best Fit Decreasing
3. VMPr = Algorithm for the reconfiguration phase (VMPr).
 * MEMETIC -> Memetic Algorithm
 * ACO -> Ant Colony Optimization
4. VMPr_TRIGGERING = VMPr triggering strategy
 * PERIODICALLY
 * PREDICTION-BASED
5. VMPr_RECOVERING = VMPr recovering strategy
 * CANCELLATION
 * UPDATE-BASED
6. PM_CONFIG = Load CPU Configuration
 * LOW -> (<10%)
 * MED  -> (<30%)
 * HIGH  -> (<80%)
 * FULL  -> (<95%)
 * SATURATED  -> (<120%)
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

#### Output Files
The framework generates the following files:
- *economical_penalties*: Average economical penalties per each SLA violation. 
- *economical_revenue*: Average ecomical revenue per each VM hosted in the main provider.
- *leasing_costs*: Average economical revenue lost per each VM hosted in an alternative provider from federation.
- *power_consumption*: Average power energy consumed 
- *reconfiguration_call_times*: Number of reconfiguration calls.
- *wasted_resources*: Average of wasted resources (one column per resource)
- *wasted_resources_ratio*: Average of wasted resources (considering all resources)
- *scenarios_scores*: Score per each executed scenario.