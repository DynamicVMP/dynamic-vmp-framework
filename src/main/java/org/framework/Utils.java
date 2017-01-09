/*
 * Virtual Machine Placement: Overbooking and Elasticity
 * Author: Rodrigo Ferreira (rodrigofepy@gmail.com)
 * Author: Saúl Zalimben (szalimben93@gmail.com)
 * Author: Leonardo Benitez  (benitez.leonardo.py@gmail.com)
 * Author: Augusto Amarilla (agu.amarilla@gmail.com)
 * 22/8/2016.
 */

package org.framework;

import org.domain.*;
import org.framework.comparator.MemoryComparator;
import org.framework.reconfigurationAlgorithm.enums.ResourcesEnum;
import org.framework.reconfigurationAlgorithm.memeticAlgorithm.MASettings;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Files.*;

/**
 * @author Saul Zalimben.
 * @since 8/15/16.
 */
public class Utils {

    public static final String OUTPUT = "outputs/";
    public static final String PLACEMENT_SCORE_BY_TIME = "placement_score_by_time/";

    public static final String INPUT = "inputs/";

    public static final String SUM = "SUM";
    public static final String SUB = "SUB";
    public static final String SCENARIOS = "SCENARIOS";
    private static Random  random;

    private static Logger logger = Logger.getLogger("Utils");

    private Utils() {
        // Default Constructor
    }

    static{
        random = new Random();
    }

    /**
     * Load the PM configuration
     * @param physicalMachines List of PMs
     * @param stream           InputStream
     * @return MaxPower Possible
     */
    private static Float loadPhysicalMachines(List<PhysicalMachine> physicalMachines,
            Stream<String> stream) {

        Float[] maxPower = new Float[1];

        maxPower[0] = 0F;
        stream.forEach(line -> {

            List<Float> resources = new ArrayList<>();

            String[] splitLine = line.split("\t");

            Float r1 = Float.parseFloat(splitLine[0]);
            Float r2 = Float.parseFloat(splitLine[1]);
            Float r3 = Float.parseFloat(splitLine[2]);
            Integer pmax = Integer.parseInt(splitLine[3]);

            resources.add(r1);
            resources.add(r2);
            resources.add(r3);

            PhysicalMachine pm = new PhysicalMachine(physicalMachines.size(), pmax, resources);
            physicalMachines.add(pm);
            maxPower[0] += pm.getPowerMax();
        });
        return maxPower[0];

    }

    /**
     * Load the workloadtrace
     * @param scenarios Workload trace
     * @param stream    InputStream
     */
    private static void loadScenario(List<Scenario> scenarios,
            Stream<String> stream) {

        stream.forEach(line -> {

            Resources resources = new Resources();
            Resources utilization = new Resources();
            Revenue revenue = new Revenue();

            String[] splitLine = line.split("\t");
            Integer time = Integer.parseInt(splitLine[0]);
            Integer service = Integer.parseInt(splitLine[1]);
            Integer datacenter = Integer.parseInt(splitLine[2]);
            Integer virtualMachine = Integer.parseInt(splitLine[3]);
            loadResources(resources, splitLine);
            loadUtilization(utilization, splitLine);
            loadRevenue(revenue,splitLine);
            Integer tinit = Integer.parseInt(splitLine[13]);
            Integer tend = Integer.parseInt(splitLine[14]);
            Scenario scenario = new Scenario(time, service, datacenter, virtualMachine, resources,
                    utilization, revenue, tinit, tend );

            scenarios.add(scenario);
        });
    }

    /**
     * @param utilization Utilization
     * @param splitLine   String file
     */
    private static void loadUtilization(Resources utilization, String[] splitLine) {

        Float u1 = Float.parseFloat(splitLine[7]);
        Float u2 = Float.parseFloat(splitLine[8]);
        Float u3 = Float.parseFloat(splitLine[9]);

        utilization.setCpu(u1);
        utilization.setRam(u2);
        utilization.setNet(u3);
    }

    /**
     * @param resources Resources
     * @param splitLine String file
     */
    private static void loadResources(Resources resources, String[] splitLine) {

        Float r1 = Float.parseFloat(splitLine[4]);
        Float r2 = Float.parseFloat(splitLine[5]);
        Float r3 = Float.parseFloat(splitLine[6]);

        resources.setCpu(r1);
        resources.setRam(r2);
        resources.setNet(r3);
    }

    private  static void loadRevenue(Revenue revenue, String[] splitLine){

        Float r1 = Float.parseFloat(splitLine[10]);
        Float r2 = Float.parseFloat(splitLine[11]);
        Float r3 = Float.parseFloat(splitLine[12]);

        revenue.setCpu(r1);
        revenue.setRam(r2);
        revenue.setNet(r3);
    }

    public static int getRandomInt(int min, int max){

        return min + random.nextInt(max - min + 1);
    }

    public static int getRandomtInt(int n) {

        return random.nextInt(n);
    }

    public static double getRandomDouble(){

        return random.nextDouble();
    }

    /**
     * Update the list of Virtual Machine derived and the list of Virtual Machines allocated en the DC.
     * @param virtualMachineList Virtual Machines
     * @param derivedVMs Derived Virtual Machines
     */
    public static void updateDerivedVMs(List<VirtualMachine> virtualMachineList, List<VirtualMachine> derivedVMs){

        List<VirtualMachine> vmsToRemove = new ArrayList<>();
        virtualMachineList.forEach(vm ->{
            if(vm.getPhysicalMachine()==0){
                derivedVMs.add(vm);
                vmsToRemove.add(vm);
            }
        });

        vmsToRemove.forEach(virtualMachineList::remove);
    }

    /**
     * List of Virtual per PM
     * @param virtualMachineList List of Virtual Machines
     * @param physicalMachineId Physical Machine ID
     * @return List of Virtual per PM
     */
    public static List<VirtualMachine> filterVMsByPM(List<VirtualMachine> virtualMachineList, Integer physicalMachineId){
        Predicate<VirtualMachine> vmFilter =  vm -> vm.getPhysicalMachine().equals(physicalMachineId);
        return virtualMachineList.stream().filter(vmFilter).collect(Collectors.toList());
    }

    /**
     * Obtains a matrix of migrated memory between physical machines
     * @param oldVirtualMachineList List of Virtual Machine (Old Placement)
     * @param newVirtualMachineList List of Virutal Machine (New Placement)
     * @param numberOfPM            Number of Physical Machine
     * @return Migrated Memory  by PM
     */
    public static Float[][] getMigratedMemoryByPM(List<VirtualMachine> oldVirtualMachineList, List<VirtualMachine> newVirtualMachineList, int numberOfPM){
        Float[][] memoryMigrationByPM = new Float[numberOfPM][numberOfPM];
	    Utils.initializeMatrix(memoryMigrationByPM,numberOfPM,numberOfPM);
        int iteratorVM;
        int oldVMPosition;
        int newVMPosition;
	    VirtualMachine vm;
	    Integer ramIndex = ResourcesEnum.RAM.getIndex();

        for(iteratorVM=0;iteratorVM<oldVirtualMachineList.size();iteratorVM++){
            oldVMPosition = oldVirtualMachineList.get(iteratorVM).getPhysicalMachine();
            newVMPosition = newVirtualMachineList.get(iteratorVM).getPhysicalMachine();
            if(oldVMPosition!=newVMPosition && newVMPosition!=0){
	            vm = oldVirtualMachineList.get(iteratorVM);
                memoryMigrationByPM[oldVMPosition-1][newVMPosition-1] += vm.getResources().get(ramIndex) * (vm.getUtilization().get(ramIndex)/100);
            }
        }
        return memoryMigrationByPM;

    }

    /**
     * Load DataCenter
     * <ul>
     *  <li>
     *      Load Physical Machine Configuration
     *  </li>
     * </ul>
     * @param scenarioFile    List of Config Files
     * @param physicalMachines List of Physical Machines
     * @param scenarios        Workload Trace
     * @return MaxPower DC
     * @throws IOException
     */
    public static Float loadDatacenter(String pmConfig, String scenarioFile, List<PhysicalMachine>
            physicalMachines,
            List<Scenario> scenarios) throws IOException {

        Float maxPower;
        try (Stream<String> stream = lines(Paths.get(INPUT + pmConfig))) {
            maxPower = Utils.loadPhysicalMachines(physicalMachines, stream);
        } catch (IOException e) {
            Logger.getLogger(DynamicVMP.DYNAMIC_VMP).log(Level.SEVERE, "Error trying to load PM Configuration!");
            throw e;
        }

        try (Stream<String> stream = lines(Paths.get(INPUT + scenarioFile))) {
            Utils.loadScenario(scenarios, stream);
        } catch (IOException e) {
            Logger.getLogger(DynamicVMP.DYNAMIC_VMP).log(Level.SEVERE, "Error trying to load Scenario: " +
                    scenarioFile);
            throw e;
        }

        return  maxPower;
    }


    /**
     * Print to File
     *
     * @param file    File name
     * @param toPrint Objective Function
     */
    public static void printToFile(String file, Object toPrint) throws IOException {

	    if(!(Paths.get(Utils.OUTPUT).toFile().exists())){
		    createDirectory(Paths.get(Utils.OUTPUT));
	    }

	    if(toPrint instanceof Collection<?>){
		    List<?> toPrintList = (ArrayList<String>)toPrint;
		    toPrintList.forEach(consumer -> {
			    try {
				    write(Paths.get(file), consumer.toString().getBytes(),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				    write(Paths.get(file), "\n".getBytes(), StandardOpenOption.APPEND);
			    } catch (IOException e) {
				    Logger.getAnonymousLogger().log(Level.SEVERE, e.getMessage(), e );
			    }
		    });

	    }else if(toPrint instanceof  Map<?,?>){
		    Map<Integer,Float> toPrintMap  = (Map<Integer,Float>)toPrint;
		    for (Map.Entry<Integer, Float> entry : toPrintMap.entrySet()) {
			    try {
				    write(Paths.get(file), entry.getValue().toString().getBytes(),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				    write(Paths.get(file), "\n".getBytes(), StandardOpenOption.APPEND);
			    } catch (IOException e) {
				    Logger.getAnonymousLogger().log(Level.SEVERE, e.getMessage(), e );
			    }
		    }
	    }else{
		    try {
			    write(Paths.get(file), toPrint.toString().getBytes(),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			    write(Paths.get(file), "\n".getBytes(), StandardOpenOption.APPEND);
		    } catch (IOException e) {
			    Logger.getAnonymousLogger().log(Level.SEVERE, e.getMessage(), e );
		    }
	    }


    }


    /**
     * Clones a List of Float elements
     * @param floatList List<Float></Float>
     * @return Copy of floatList
     */
    public static List<Float> getListClone(List<Float> floatList) {

        List<Float> cloneList = new ArrayList<>();
        floatList.forEach(x -> cloneList.add(new Float(x)));

        return cloneList;
    }

    /**
     *
     * List of Apriori values:
     * <ul>
     *     <li> Power Consumption </li>
     *     <li> Economical Revenue </li>
     *     <li> Quality of Service </li>
     *     <li> Wasted Resources </li>
     *     <li> Migration Count </li>
     *     <li> Memory Migrated </li>
     * </ul>
     *
     * @return List of Apriori Values
     */
    public static List<APrioriValue> getAprioriValuesList(Integer timeUnit){
        List<APrioriValue> aPrioriValuesList = new ArrayList<>();

        aPrioriValuesList.add(new APrioriValue(ObjectivesFunctions.MIN_POWER,DynamicVMP.maxPower));
        aPrioriValuesList.add(new APrioriValue(ObjectivesFunctions.MIN_REVENUE,DynamicVMP.revenueAprioriTime.get(timeUnit)));
        aPrioriValuesList.add(new APrioriValue(0F,1F));
        aPrioriValuesList.add(new APrioriValue(0F,DynamicVMP.migratedMemoryAprioriTime.get(timeUnit)));

        return aPrioriValuesList;
    }

    /**
     * Steps to Terminate a thread pool of a Executor Service
     * @param pool ExecutorService
     */
    public static void executorServiceTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    logger.log(Level.INFO,"Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Obtain the configuration for the Memetic Algorithm
     *
     * @param isFullMeme Flag indicate to launch Full Memetic Algorithm
     * @return A settings instance for the Memetic Algorithm
     */
    public static MASettings getMemeConfig(Boolean isFullMeme){
        MASettings settings = new MASettings();

        settings.setCrossoverProb(1.0);                                        // set the crossover operator probability
        settings.setNumberOfResources(3);                                      // set the number of resources
        settings.setNumberOfObjFunctions(4);                                   // set the number of objective functions
        settings.setExecutionDuration(Parameter.EXECUTION_DURATION);          // set the estimated duration of execution of the algorithm
        settings.setExecutionInterval(Parameter.INTERVAL_EXECUTION_MEMETIC);  // set the the interval of time to execute the algorithm
        settings.setFaultTolerance(Parameter.FAULT_TOLERANCE);                // set the flag to consider or not the fault tolerance constraint
        settings.setExecutionFirstTime(3);

        if (isFullMeme){
            settings.setPopulationSize(Parameter.POPULATION_SIZE);
            settings.setNumberOfGenerations(Parameter.NUMBER_GENERATIONS);
        }else{
            settings.setPopulationSize(10);
            settings.setNumberOfGenerations(10);
        }

        return settings;
    }

    /**
     * Math function to normalize values.
     * @param value Value to normalize
     * @param minValue Min Value
     * @param maxValue Max Value
     * @return Normalized value
     */
    public static  Float normalizeValue(Float value, Float minValue, Float maxValue) {
        if(value==0 || maxValue.equals(minValue)){
	        return 0F;
        }
	    return (value - minValue)/(maxValue - minValue);
    }

    /**
     * Remove from Virtual machine list of a placement the Virtual machines that have a time end less or equal
     * to the current time unit.
     * @param placement         Placement
     * @param currentTimeUnit   Current Time
     * @param numberOfResources Number of Resources
     */
    public static void removeDeadVMsFromPlacement(Placement placement, Integer currentTimeUnit, Integer numberOfResources) {

		Integer iteratorResource;
		List<VirtualMachine> toRemoveVMs = new ArrayList<>();
		List<PhysicalMachine> physicalMachineList = placement.getPhysicalMachines();
		PhysicalMachine pm;
		Float resourceUpdate;

		//collect the vm to remove from the placement.VirtualMachine
		for (VirtualMachine vm : placement.getVirtualMachineList()){
			//if the vm is dead
			if (vm.getTend() <= currentTimeUnit) {
				toRemoveVMs.add(vm);
				pm = PhysicalMachine.getById(vm.getPhysicalMachine(),physicalMachineList);
				for(iteratorResource=0;iteratorResource<numberOfResources;iteratorResource++){
					resourceUpdate = vm.getResources().get(iteratorResource)*(vm.getUtilization().get(iteratorResource)/100);
					updatePMResRequested(pm,iteratorResource,resourceUpdate,false);
				}

			}
		}
		placement.getVirtualMachineList().removeAll(toRemoveVMs);
		toRemoveVMs.clear();
        //collect the vm to remove from the placement.DerivedVirtualMachine
        placement.getDerivedVMs().forEach(dvm -> {
            if (dvm.getTend() <= currentTimeUnit) {
                toRemoveVMs.add(dvm);
            }
        });
	}

    /**
     * Remove from a list of Virtual Machine Migrated  the Virtual machines that have a time end less or equal
     * to the current time unit.
     * @param vmsToMigrate    List of Virtual Machine to Migrate
     * @param currentTimeUnit Current time
     */
    public static void removeDeadVMsMigrated(List<VirtualMachine> vmsToMigrate,Integer currentTimeUnit){
		List<VirtualMachine> toRemoveVMs = new ArrayList<>();
		vmsToMigrate.forEach(mvm -> {
            //if the vm is dead
            if (mvm.getTend() <= currentTimeUnit) {
                toRemoveVMs.add(mvm);
            }
        });
		vmsToMigrate.removeAll(toRemoveVMs);
	}

    /**
     * Calculate the end time migration of each virtual machine migrated
     * @param migratedVirtualMachines virtual machines migrated.
     * @param currentTimeUnit the current time unit.
     * @return a list of end times of each virtual machine migrated.
     */
	public static List<Integer> getTimeEndMigrationByVM(final List<VirtualMachine> migratedVirtualMachines , final Integer currentTimeUnit){
		final Integer byteToBitsFactor=8;
		List<Integer>timeEndMigrationList = new ArrayList<>();
        Integer timeEndMigrationSec;
		Integer vmEndTimeMigration;
        for(VirtualMachine vm : migratedVirtualMachines){
            timeEndMigrationSec = Math.round((vm.getResources().get(ResourcesEnum.RAM.getIndex())*byteToBitsFactor)/ Parameter.LINK_CAPACITY);
			vmEndTimeMigration = currentTimeUnit + secondsToTimeUnit(timeEndMigrationSec, Constant.TIMEUNIT_DURATION);
            timeEndMigrationList.add(vmEndTimeMigration);
        }
		return timeEndMigrationList;
	}

    /**
     * Obtains the list of virtual machines to migrate
     * @param newVirtualMachineList List of Virtual Machines
     * @param oldVirtualMachineList List of Virtual Machines (after migration)
     * @return List of Virtual Machine
     */
	public static List<VirtualMachine> getVMsToMigrate(List<VirtualMachine> newVirtualMachineList, List<VirtualMachine> oldVirtualMachineList){
        int iterator;
        int oldPosition;
        int newPosition;
        List<VirtualMachine> vmsToMigrate = new ArrayList<>();
        for(iterator=0;iterator<oldVirtualMachineList.size();iterator++){
            oldPosition = oldVirtualMachineList.get(iterator).getPhysicalMachine();
            newPosition = newVirtualMachineList.get(iterator).getPhysicalMachine();
            if(oldPosition!=newPosition && newPosition!=0){
                vmsToMigrate.add(newVirtualMachineList.get(iterator));
            }
        }

        return vmsToMigrate;
    }

	/***
	 *
	 * @param objectiveFuntions Objective Functions []
	 * @param aPrioriValuesList Apriori Values
	 * @return Placement Score
	 */
	public static Float calcPlacemenScore(Float[] objectiveFuntions, List<APrioriValue> aPrioriValuesList){
		Float normalizedValue;
		APrioriValue aPrioriValue;
		List<Float> normalizedValues = new ArrayList<>();
		int iteratorObjFuncts;
		for(iteratorObjFuncts = 0; iteratorObjFuncts < Constant.NUM_OBJ_FUNCT_COMP; iteratorObjFuncts++){
			aPrioriValue = aPrioriValuesList.get(iteratorObjFuncts);
			normalizedValue = Utils.normalizeValue(objectiveFuntions[iteratorObjFuncts],aPrioriValue.getMinValue()
					,aPrioriValue.getMaxValue());
			normalizedValues.add(normalizedValue);
		}
		return ObjectivesFunctions.getScalarizationMethod(normalizedValues, Constant.WEIGHT_OFFLINE);
	}


	/**
	 * Update the resources requested of a Physical Machine
	 * @param physicalMachine Physical Machine
	 * @param resourceIndex Id of Resource to update
	 * @param resource quantity of resource to update
	 * @param add a flag that indicates if the update is for add resource requested or remove.
	 */
	public static void updatePMResRequested(PhysicalMachine physicalMachine,Integer resourceIndex, Float resource, Boolean add){
		Float actualResourceRequested;
		Float newResourceRequested;
		actualResourceRequested = physicalMachine.getResourcesRequested().get(resourceIndex);
		newResourceRequested = add.equals(true) ? actualResourceRequested + resource : actualResourceRequested - resource;
		physicalMachine.getResourcesRequested().set(resourceIndex,newResourceRequested);
	}


	/**
	 * Get the end time of the migration operation
	 * @param endTimesMigration List of end time for vm migrations
	 * @return the end time of the migration operation
	 */
	public static Integer getMigrationEndTime(final List<Integer> endTimesMigration){
		Integer largestEndTime=0;
		for(Integer endTime : endTimesMigration){
			if(endTime>largestEndTime){
				largestEndTime = endTime;
			}
		}
		return largestEndTime;
	}

    /**
     * @param virtualMachineId  Virtual Machine Id
     * @param vmsToMigrate      Virtual Machine to Migrate
     * @param endTimesMigration End time of migration
     * @return End time of migration
     */
    public static Integer getEndTimeMigrationByVm(Integer virtualMachineId, final List<VirtualMachine> vmsToMigrate,final List<Integer> endTimesMigration){

		VirtualMachine mvm;
		for(int iteratorVM=0; iteratorVM<vmsToMigrate.size();iteratorVM++){
			mvm = vmsToMigrate.get(iteratorVM);
			if(mvm.getId().equals(virtualMachineId)){
				return endTimesMigration.get(iteratorVM);
			}
		}
		return 0;
	}

    /**
     * @param seconds          Seconds
     * @param timeUnitDuration Time Unit Duration
     * @return Factor of Second to TimeUnit
     */
    public static Integer secondsToTimeUnit(Integer seconds,Float timeUnitDuration){

		return Math.round((float)seconds/timeUnitDuration);

	}

    /**
     * Initialize a matrix
     * @param matrix  Matrix
     * @param rows    Rows
     * @param columns colums
     */
    public static void initializeMatrix(Float[][] matrix, Integer rows, Integer columns){

		for(int iteratorRow=0;iteratorRow<rows;iteratorRow++){
			for(int iteratorColumns=0;iteratorColumns<columns;iteratorColumns++){
				matrix[iteratorRow][iteratorColumns]= 0F;
			}
		}

	}

    /**
     * Load Experiments Parameters
     *
     * @param scenariosFiles Scenarios Files
     * @param stream         Stream
     */
    public static void loadParameter(List<String> scenariosFiles, Stream<String> stream) {

        List<String> parameter = stream.filter(s -> s.length() > 0).collect(Collectors.toList());
        Map<String, Object> parameterMap = new HashMap<>();
        parameter.stream()
                 .filter(line -> line.split("=").length > 1)
                 .forEach(line ->
            ((HashMap) parameterMap).put(line.split("=")[0], line.split("=")[1])
        );

        Parameter.ALGORITHM = Integer.parseInt( (String) parameterMap.get("ALGORITHM"));
        Parameter.HEURISTIC_CODE = (String) parameterMap.get("HEURISTIC_CODE");
        Parameter.PM_CONFIG = (String) parameterMap.get("PM_CONFIG");
        Parameter.DERIVE_COST = new Float ((String) parameterMap.get("DERIVE_COST"));
        Parameter.FAULT_TOLERANCE = Boolean.getBoolean( (String) parameterMap.get("FAULT_TOLERANCE"));
        Parameter.PROTECTION_FACTOR =   new Float ((String) parameterMap.get("PROTECTION_FACTOR"));
        Parameter.INTERVAL_EXECUTION_MEMETIC = Integer.parseInt( (String) parameterMap.get
                ("INTERVAL_EXECUTION_MEMETIC"));
        Parameter.POPULATION_SIZE = Integer.parseInt( (String) parameterMap.get("POPULATION_SIZE"));
        Parameter.NUMBER_GENERATIONS = Integer.parseInt( (String) parameterMap.get("NUMBER_GENERATIONS"));
        Parameter.EXECUTION_DURATION = Integer.parseInt( (String) parameterMap.get("EXECUTION_DURATION"));
        Parameter.LINK_CAPACITY =  new Float ((String) parameterMap.get("LINK_CAPACITY"));
        Parameter.MIGRATION_FACTOR_LOAD =  new Float ((String) parameterMap.get("MIGRATION_FACTOR_LOAD"));
        Parameter.HISTORICAL_DATA_SIZE = Integer.parseInt( (String) parameterMap.get("HISTORICAL_DATA_SIZE"));
        Parameter.FORECAST_SIZE =Integer.parseInt( (String)  parameterMap.get("FORECAST_SIZE"));
        Parameter.SCALARIZATION_METHOD = (String) parameterMap.get("SCALARIZATION_METHOD");

        parameter.stream()
                 .filter(line -> line.split("=").length == 1 && !line.equals(SCENARIOS))
                 .forEach(scenariosFiles::add);
    }


	/**
	 * Forecast n values for n periods ahead using double exponential smoothing
	 * @param series : know values
	 * @param alpha : smoothing factor
	 * @param beta : trend factor
	 * @param nForecast: number of values to forecast
	 * @return values forecasted
	 */
	public static List<Float>  doubleExponentialSmooth(List<Float> series, Float alpha, Float beta, Integer nForecast){
		List<Float> result = new ArrayList<>();
		Float level = 0F;
		Float trend = 0F;
		Float lastLevel;
        Float lastTrend;
        Float value;

		result.add(series.get(0));
		for(int iterator=1;iterator<series.size(); iterator++){
			if(iterator==1){
				level = series.get(0);
				trend = series.get(1)-series.get(0);
			}

			value = series.get(iterator);
			lastLevel = level;
			level = alpha*value + (1-alpha)*(level + trend);
			lastTrend = trend;
			trend = beta*(level - lastLevel) + (1-beta)*trend;
			result.add(lastLevel+lastTrend);
		}
		//forecasting
		lastLevel = level;
		lastTrend = trend;
		for(int n=1;n<=nForecast;n++){
			result.add(lastLevel + n*lastTrend);
		}

		return result;
	}

	/**
	 *
	 * @param series    List of Float
	 * @param nForecast nForecast
	 * @return List of Forecast
	 */
	public static List<Float> calculateNForecast(List<Float> series, Integer nForecast){
		Float alpha = 0.5F;
		Float beta = 0.5F;
		List<Float> result = doubleExponentialSmooth(series,alpha,beta,nForecast);
		return result.subList(series.size(),result.size());
	}

	/**
	 * Makes a forecast of n possible values ​​of f(x) to decide to call or
	 * not to reconfiguration
	 * @return true: do reconfiguration or false: don't do reconfiguration
	 *
	 */
	public static Boolean callToReconfiguration(List<Float> series, Integer forecastSize){
		List<Float> resultForecasting = calculateNForecast(series,forecastSize);

		// check if the condition to call reconfiguration "f1(x)<f2(x)<...<fn(x)" is met.
		for (int i = 1; i < resultForecasting.size(); i++) {
			if (resultForecasting.get(i-1).compareTo(resultForecasting.get(i)) > 0){
				return false;
			}
		}
		return true;
	}

	/**
	 * Obtains the average of a list of float numbers
	 * @param listNumbers list of float numbers
	 * @return the average of the list of numberss
	 */
	public static Float average(List<Float> listNumbers){
		Float sum = 0F;
		for(Float n : listNumbers ){
			sum+=n;
		}
		return listNumbers.isEmpty() ? 0 : sum/listNumbers.size();
	}

	/**
	 *
	 * @param pwConsumptionByTime Power Consumption By Time
	 * @return Average Power Consumption By Time
	 */
	public static Float getAvgPwConsumptionNormalized(Map<Integer,Float>  pwConsumptionByTime){
        List<Float> pwConsumptionNormalizedList = pwConsumptionByTime.entrySet().stream().map(pw->{
	        Float revenue = pw.getValue();
	        return normalizeValue(revenue,ObjectivesFunctions.MIN_POWER,DynamicVMP.maxPower);
        }).collect(Collectors.toList());

		return average(pwConsumptionNormalizedList);
	}

	/**
	 *
	 * @param revenueByTime Map of Revenue By Time
	 * @return Average Revenue By Time
	 */
	public static Float getAvgRevenueNormalized(Map<Integer,Float> revenueByTime){
		List<Float> revenueNormalizedList = revenueByTime.entrySet().stream().map(r->{
			Integer timeUnit = r.getKey();
			Float revenue = r.getValue();
			Float maxRevenueValue = DynamicVMP.revenueAprioriTime.get(timeUnit);
			return normalizeValue(revenue,ObjectivesFunctions.MIN_REVENUE,maxRevenueValue);
		}).collect(Collectors.toList());
		return average(revenueNormalizedList);

	}

	/**
	 *
	 * @param wastedResourcesByTime Map of Wasted Resources By Time
	 * @return Average Wasted Resources By Time
	 */
	public static Float getAvgResourcesWNormalized(Map<Integer,Float> wastedResourcesByTime){
		List<Float> wastedResourcesList = wastedResourcesByTime.entrySet().stream()
                                                               .map(Map.Entry::getValue)
                                                               .collect(Collectors.toList());
		return average(wastedResourcesList);
	}

    /***
     *
     * @param pm PhysicalMachine
     * @param vmsInPM List of VMs
     */
    public static List<VirtualMachine> getVMsToMigrate(PhysicalMachine pm, List<VirtualMachine> vmsInPM){

        PhysicalMachine pmCopy = new PhysicalMachine(pm.getId(),pm.getPowerMax(),pm.getResources(),pm.getResourcesRequested(),
                pm.getUtilization());
        List<VirtualMachine> vmsToMigrate = new ArrayList<>();
        MemoryComparator comparator =  new MemoryComparator();
        VirtualMachine vm;
        Integer vmIterator=0;

        Collections.sort(vmsInPM, comparator);

        while (!Constraints.isPMOverloaded(pmCopy) || vmIterator.equals(vmsInPM.size())){
            vm = vmsInPM.get(vmIterator);
            vmsToMigrate.add(vm);
            pmCopy.updatePMResources(vm,Utils.SUB);
            vmIterator++;
        }

        return vmsToMigrate;
    }



}