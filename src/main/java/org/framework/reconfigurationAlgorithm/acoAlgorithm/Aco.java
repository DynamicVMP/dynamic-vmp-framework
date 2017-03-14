package org.framework.reconfigurationAlgorithm.acoAlgorithm;

import org.domain.APrioriValue;
import org.domain.PhysicalMachine;
import org.domain.Placement;
import org.domain.VirtualMachine;
import org.framework.ObjectivesFunctions;
import org.framework.Utils;

import java.util.List;

/**
 * Created by nabil on 2/24/17.
 */
public class Aco {

    private Float pheromone[][];
    private Float heuristic[][];
    private Float probability[][];
    private Float normalizedProb[][];
    private Integer nVMs, nPMs;
    private Integer randomVM, randomPM;

    private List<PhysicalMachine> tempAntPMs;
    private List<VirtualMachine> tempAntVMs;

    private Float[] objectiveFunctions;

    /**
     * This method is called to executed de ACO VMPr algorithm, to find and
     * return a new posible best placement of VMs in the provided PMs
     *
     * @param actualPlacement The actual placement, the one ACO will try to improve it
     * @param aPrioriValueList A priori value list for the objective functions
     * @param settings ACO settings
     * @return Placement
     */
    public Placement reconfiguration(Placement actualPlacement, List<APrioriValue> aPrioriValueList,
                                     AcoSettings settings){

        //Initialize variables
        nVMs = actualPlacement.getVirtualMachineList().size();
        nPMs = actualPlacement.getPhysicalMachines().size();
        pheromone = new Float[nVMs][nPMs];
        heuristic = new Float[nVMs][nPMs];
        probability = new Float[nVMs][nPMs];
        normalizedProb = new Float[nVMs][nPMs];
        objectiveFunctions = new Float[4];
        Integer nAnts = settings.getnAnts();
        Integer acoIterations = settings.getAcoIterations();

        // initialize pheromone[v][p] to maxPheromone
        setMaxPheromone(settings);

        // local variables
        Float tempAntScore, antScore, bestAntScore, globalScore, initialScore;
//        Float nAntMigrations;


        List<PhysicalMachine> antPMs;
        List<PhysicalMachine> bestAntPMs;
        List<PhysicalMachine> mapPMs;


        List<VirtualMachine> antVMs;
        List<VirtualMachine> bestAntVMs;
        List<VirtualMachine> mapVMs;

        // initialize map
        mapPMs = PhysicalMachine.clonePMsList(actualPlacement.getPhysicalMachines());
        mapVMs = VirtualMachine.cloneVMsList(actualPlacement.getVirtualMachineList());

        // first time loading O.F. to initialize globalScore
        loadObjectiveFunctions(actualPlacement.getVirtualMachineList(), actualPlacement.getVirtualMachineList(),
                actualPlacement.getDerivedVMs(), actualPlacement.getPhysicalMachines());
        initialScore = Utils.calcPlacemenScore(objectiveFunctions, aPrioriValueList);
        globalScore = initialScore;
        // printing
//        System.out.println("\nfirst score: " + globalScore);
        while(acoIterations>0){
            acoIterations--;
            bestAntScore = initialScore;

            bestAntPMs = PhysicalMachine.clonePMsList(actualPlacement.getPhysicalMachines());
            bestAntVMs = VirtualMachine.cloneVMsList(VirtualMachine.cloneVMsList(actualPlacement.getVirtualMachineList()));

            for(int a=0; a<nAnts; a++){
                antScore = initialScore;
//                nAntMigrations = 0F;

                antPMs = PhysicalMachine.clonePMsList(actualPlacement.getPhysicalMachines());
                antVMs = VirtualMachine.cloneVMsList(VirtualMachine.cloneVMsList(actualPlacement.getVirtualMachineList()));

                tempAntPMs = PhysicalMachine.clonePMsList(actualPlacement.getPhysicalMachines());
                tempAntVMs = VirtualMachine.cloneVMsList(VirtualMachine.cloneVMsList(actualPlacement.getVirtualMachineList()));

                for(int v=0; v<nVMs; v++){
                    computeHeuristic();
                    computeProbability();
                    chooseRandomVMandPM();

                    // now we have a
                    // randomVM and random PM
                    // based on normalizedProb[v][p]

//                  objective functions here
                    updateTempVMsandPMs();
//                    nAntMigrations++;

                    // evaluate tempAntScore
                    loadObjectiveFunctions(tempAntVMs, antVMs, actualPlacement.getDerivedVMs(), tempAntPMs);

                    tempAntScore = Utils.calcPlacemenScore(objectiveFunctions, aPrioriValueList);

//                    debug only
//                    Integer v11 = randomVM;
//                    Integer p11 = randomPM;

                    if(tempAntScore<antScore){
                        antScore = tempAntScore;
                        antPMs = PhysicalMachine.clonePMsList(tempAntPMs);
                        antVMs = VirtualMachine.cloneVMsList(tempAntVMs);
                    }
                    else {
                        // rollback the migration
//                        nAntMigrations--;
                        tempAntPMs = PhysicalMachine.clonePMsList(antPMs);
                        tempAntVMs = VirtualMachine.cloneVMsList(antVMs);
                    }
                }

                if(antScore<bestAntScore){
                    bestAntScore = antScore;
                    bestAntPMs = PhysicalMachine.clonePMsList(antPMs);
                    bestAntVMs = VirtualMachine.cloneVMsList(antVMs);

//                  printing
//                    if(a!=0){
//                        System.out.println("bestAntScore: " + bestAntScore + "\t\ta: "+a);
//                    }
                }
            } // end of ants

            // update globalScore, mapPMs, mapVMs according to the best ant if appropiate
            if(bestAntScore<globalScore){
                globalScore = bestAntScore;

//                debug only
//                Boolean equalList = compareVMLists(mapVMs, bestAntVMs);

                mapPMs = PhysicalMachine.clonePMsList(bestAntPMs);
                mapVMs = VirtualMachine.cloneVMsList(bestAntVMs);

                // printing
//                System.out.println("globalScore: " + globalScore + "\t\tacoIterations: "+acoIterations);
            }

            // update pheromone
            for(int v=0; v<nVMs;v++){
                for(int p=0; p<nPMs;p++){
                    Integer pmIdIterator = mapPMs.get(p).getId();
                    Integer pmId = mapVMs.get(v).getPhysicalMachine();
                    if(pmId.equals(pmIdIterator)) {
//                        pheromone[v][p] = (1 - settings.getPheromoneConstant()) * pheromone[v][p] + ((1/globalScore)*20);
                        pheromone[v][p] = (1 - settings.getPheromoneConstant()) * pheromone[v][p] + (1-globalScore);
                    }else{
                        pheromone[v][p] = (1 - settings.getPheromoneConstant()) * pheromone[v][p];
                    }

                    // check max pheromone boundary
                    if(pheromone[v][p]>1F){
                        pheromone[v][p] = 1.0F;
                    }else if(pheromone[v][p]<0.2F){
                        pheromone[v][p] = 0.2F;
                    }
                }
            }

        } // end while

        Placement newPlacement = new Placement(mapPMs,mapVMs,actualPlacement.getDerivedVMs());
        newPlacement.setPlacementScore(globalScore);

//        debug only
//        Boolean equalList = compareVMLists(actualPlacement.getVirtualMachineList(), newPlacement.getVirtualMachineList());
//        if(equalList) {
//            System.out.println("Returned score: " + globalScore+"\t\t equalList");
//        }else {
//            System.out.println("Returned score: "+ globalScore+"\t\t NOT equalList");
//        }

        return newPlacement;
    }

    /**
     * Mostly used for debugging and to check if after running the algorithm
     * any VM have migrated
     * @param oldVMList oldVMList before running a possible migration
     * @param newVMList newVMList after running a possible migration
     * @return Boolean
     */
    private Boolean compareVMLists(List<VirtualMachine> oldVMList, List<VirtualMachine> newVMList) {
        VirtualMachine oldVM;
        VirtualMachine newVM;
        Integer oldPM;
        Integer newPM;

        for(int i=0; i<oldVMList.size(); i++){
            oldVM = oldVMList.get(i);
            newVM = newVMList.get(i);
            oldPM = oldVM.getPhysicalMachine();
            newPM = newVM.getPhysicalMachine();

            if(!oldPM.equals(newPM)){
                return false;
            }
        }
        return true;
    }

    /**
     * This method will calculate and then load the values of each O.F. in the global
     * variable objectiveFuntions[number_of_objective_functions]
     * @param tempAntVMs newVM list, after possible migration
     * @param antVMs oldVM list, before possible migration
     * @param derivedVMs deviredVMs
     * @param tempAntPMs newPM list, after possible migration
     */
    private void loadObjectiveFunctions(List<VirtualMachine> tempAntVMs, List<VirtualMachine> antVMs,
                                        List<VirtualMachine> derivedVMs, List<PhysicalMachine> tempAntPMs) {
        Float economicalRevenue;
        Float powerConsumption;
        Float wastedResources;
        Float memoryMigrated;

        memoryMigrated = ObjectivesFunctions.migratedMemoryBtwPM(antVMs, tempAntVMs, tempAntPMs.size());
        powerConsumption = ObjectivesFunctions.powerConsumption(tempAntPMs);
        wastedResources = ObjectivesFunctions.wastedResources(tempAntPMs, null);
//        Utils.updateDerivedVMs(tempAntVMs, derivedVMs);
        economicalRevenue = ObjectivesFunctions.economicalRevenue(tempAntVMs, derivedVMs, null);

        objectiveFunctions[0] = powerConsumption;
        objectiveFunctions[1] = economicalRevenue;
        objectiveFunctions[2] = wastedResources;
        objectiveFunctions[3] = memoryMigrated;
    }


    /**
     * Update the resources of the new randomPM selected, as well as the oldPM
     * of the randomVM chosen. It also sets the PM of randomVM to randomPM.
     */
    private void updateTempVMsandPMs() {
        PhysicalMachine pm = tempAntPMs.get(randomPM);
        VirtualMachine vm = tempAntVMs.get(randomVM);
        PhysicalMachine oldPM = PhysicalMachine.getById(vm.getPhysicalMachine(), tempAntPMs);

        if(!pm.getId().equals(oldPM.getId())) {
            Float vmResource0;
            Float vmResource1;
            Float vmResource2;

            vmResource0 = vm.getResources().get(0)*(vm.getUtilization().get(0)/100);
            vmResource1 = vm.getResources().get(1)*(vm.getUtilization().get(1)/100);
            vmResource2 = vm.getResources().get(2)*(vm.getUtilization().get(2)/100);

            Float freeSpace0 = pm.getResources().get(0) - (pm.getResourcesRequested().get(0) + vmResource0);
            Float freeSpace1 = pm.getResources().get(1) - (pm.getResourcesRequested().get(1) + vmResource1);
            Float freeSpace2 = pm.getResources().get(2) - (pm.getResourcesRequested().get(2) + vmResource2);
            if (freeSpace0>=0.00000001F && freeSpace1>=0.00000001F && freeSpace2>=0.00000001F) {
                // update PM of VM
                vm.setPhysicalMachine(pm.getId());

                // update PM capacity
                oldPM.updateResource(0, vmResource0, "SUB");
                oldPM.updateResource(1, vmResource1, "SUB");
                oldPM.updateResource(2, vmResource2, "SUB");

                pm.updateResource(0, vmResource0, "SUM");
                pm.updateResource(1, vmResource1, "SUM");
                pm.updateResource(2, vmResource2, "SUM");
            }

////            debug only
//            else{
//                System.out.println("ERROR freeSpace. Line 243.");
//                System.out.println("freeSpace0: "+freeSpace0);
//                System.out.println("freeSpace1: "+freeSpace1);
//                System.out.println("freeSpace2: "+freeSpace2);
//                System.out.println("normalizedProb: "+normalizedProb[randomVM][randomPM]);
//                System.out.println("heuristic: "+heuristic[randomVM][randomPM]);
//                System.out.println("pheromone: "+pheromone[randomVM][randomPM]);
//                if(oldPM.getId()==null){
//                    System.out.println("oldPM = null.");
//                }
//            }
        }

//        debug only
//        else {
//            System.out.println("same PM. randomVM(index)= "+randomVM+" randomPM(index)= "+randomPM);
//        }
    }


    /**
     *  Generate a random number between [0,1] and then based on the cumulative
     *  probability a randomVM and randomPM are chosen.
     */
    private void chooseRandomVMandPM() {
        Double randomProb = Math.random();

        randomVM = 0;
        randomPM = 0;

//        this code wasnt right I guess
//        if(normalizedProb[0][0]>randomProb) {
//            randomVM=0;
//            randomPM=0;
//        }else {
//            Boolean finish;
//            for (int v = 0; v < nVMs; v++) {
//                finish = false;
//                for (int p = 0; p < nPMs; p++) {
//                    if (normalizedProb[v][p] > randomProb) {
//                        if (p == 0) {
//                            randomVM = v-1;
//                            randomPM = nPMs-1;
//                        } else {
//                            randomVM = v;
//                            randomPM = p-1;
//                        }
//                        finish = true;
//                        break;
//                    }
//                }
//                if (finish) {
//                    break;
//                }
//            }
//        }


        Boolean finish=false;
        for (int v = 0; v < nVMs; v++) {
            for (int p = 0; p < nPMs; p++) {
                if (normalizedProb[v][p] >= randomProb) {
                    randomVM = v;
                    randomPM = p;
                    finish = true;
                    break;
                }
            }
            if (finish) {
                break;
            }
        }

//        debug only
//        if(!finish){
//            System.out.println("FINISH false. Line 299.");
//        }
    }


    /**
     * Calculates the probability of migration (based on heuristic and pheromone)
     * for each [vm,pm] pair. Then the probability is normalized to find the cumulative
     * one.
     */
    private void computeProbability() {
        Float sum=0F;
        Float sumProbabilities=0F;
        Integer pmId;
        VirtualMachine vm;

//      old approach
//        for(int v=0; v<nVMs; v++){
//            sum=0F;
//            vm = tempAntVMs.get(v);
//            for(int p=0; p<nPMs; p++){
//                for(int eachVM=0; eachVM<nVMs; eachVM++){
//                    pmId = vm.getPhysicalMachine();
//
////                    pay attention to this
//                    if(pmId.equals(tempAntPMs.get(p).getId())){
//                        sum += (pheromone[eachVM][p]*heuristic[eachVM][p]);
//                    }
//                }
//
//                if(sum>0.00000001F){
//                    probability[v][p] = (pheromone[v][p]*heuristic[v][p])/sum;
//                    sumProbabilities += probability[v][p];
//                }else {
//                    probability[v][p] = 0F;
//                }
//            }
//        }

        // first approach, every "v" in "p"
        // take care with "sum", when "p" of same place is zero
//        for(int p=0; p<nPMs; p++){
//            sum=0F;
//            pmId = tempAntPMs.get(p).getId();
//            for(int eachVM=0; eachVM<nVMs; eachVM++){
//                if(tempAntVMs.get(eachVM).getPhysicalMachine().equals(pmId)){
//                    sum += (pheromone[eachVM][p]*heuristic[eachVM][p]);
//                }
//            }
//
//            if(sum>0.00000001F){
//                for(int v=0; v<nVMs; v++){
//                    probability[v][p] = (pheromone[v][p]*heuristic[v][p])/sum;
//                    sumProbabilities += probability[v][p];
//                }
//            }else{
//                for(int v=0; v<nVMs; v++){
//                    probability[v][p] = 0F;
//                }
//            }
//        }

        // second approach, normalized already
        for(int p=0; p<nPMs; p++){
            for(int v=0; v<nVMs; v++){
                probability[v][p] = (pheromone[v][p]*heuristic[v][p]);
                sumProbabilities += probability[v][p];
            }
        }


        // normalizing probability
        if(sumProbabilities!=0F){
            for(int v=0; v<nVMs; v++){
                for(int p=0; p<nPMs; p++){
                    normalizedProb[v][p] = probability[v][p]/sumProbabilities;
                }
            }

            // cumulative
            Float last=0F;
            for(int v=0; v<nVMs; v++){
                for(int p=0; p<nPMs; p++){
                    if(normalizedProb[v][p]!=0){
                        normalizedProb[v][p] += last;
                        last = normalizedProb[v][p];
                    }
                }
            }
        }

//        debug only
//        else {
//            System.out.println("ERROR sumP robabilities = 0, dividing.");
//        }

    }

    /**
     * Compute the heuristic for each [vm,pm] pair.
     */
    private void computeHeuristic() {

        PhysicalMachine pm;
        VirtualMachine vm;
        Float freeSpace0;
        Float freeSpace1;
        Float freeSpace2;
        Float heuristic0;
        Float heuristic1;
        Float heuristic2;
        Float vmResource0;
        Float vmResource1;
        Float vmResource2;
        Integer pmId, pmOfVm;

//        debug only
//        if(tempAntVMs.size()==14){
//            System.out.println("debug 14.");
//        }

        for(int v=0; v<nVMs; v++){
            for(int p=0; p<nPMs; p++){
                pm = tempAntPMs.get(p);
                vm = tempAntVMs.get(v);

                pmId = pm.getId();
                pmOfVm = vm.getPhysicalMachine();

                if(!pmId.equals(pmOfVm)) {
                    vmResource0 = vm.getResources().get(0) * (vm.getUtilization().get(0) / 100);
                    vmResource1 = vm.getResources().get(1) * (vm.getUtilization().get(1) / 100);
                    vmResource2 = vm.getResources().get(2) * (vm.getUtilization().get(2) / 100);

                    freeSpace0 = pm.getResources().get(0) - (pm.getResourcesRequested().get(0) +
                            vmResource0);
                    freeSpace1 = pm.getResources().get(1) - (pm.getResourcesRequested().get(1) +
                            vmResource1);
                    freeSpace2 = pm.getResources().get(2) - (pm.getResourcesRequested().get(2) +
                            vmResource2);

                    if (freeSpace0 >= 0.00000001F && freeSpace1 >= 0.00000001F && freeSpace2 >= 0.00000001F) {
                        heuristic0 = 1 / freeSpace0;
                        heuristic1 = 1 / freeSpace1;
                        heuristic2 = 1 / freeSpace2;
                        heuristic[v][p] = (heuristic0 + heuristic1 + heuristic2) / 3;
                    } else {
                        heuristic[v][p] = 0F;
                    }
                }else {
                    heuristic[v][p] = 0F;
                }
            }
        }
    }

    /**
     * Initialize the pheromone bidimensional array to the MAX_PHEROMONE from settings.
     * @param settings ACO settings
     */
    public void setMaxPheromone(AcoSettings settings) {
        for(int v=0; v<nVMs; v++){
            for(int p=0; p<nPMs; p++){
                pheromone[v][p] = settings.getMaxPheromone();
            }
        }
    }
}
