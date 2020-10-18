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

        while(acoIterations>0){
            acoIterations--;
            bestAntScore = initialScore;

            bestAntPMs = PhysicalMachine.clonePMsList(actualPlacement.getPhysicalMachines());
            bestAntVMs = VirtualMachine.cloneVMsList(VirtualMachine.cloneVMsList(actualPlacement.getVirtualMachineList()));

            for(int a=0; a<nAnts; a++){
                antScore = initialScore;

                antPMs = PhysicalMachine.clonePMsList(actualPlacement.getPhysicalMachines());
                antVMs = VirtualMachine.cloneVMsList(VirtualMachine.cloneVMsList(actualPlacement.getVirtualMachineList()));

                tempAntPMs = PhysicalMachine.clonePMsList(actualPlacement.getPhysicalMachines());
                tempAntVMs = VirtualMachine.cloneVMsList(VirtualMachine.cloneVMsList(actualPlacement.getVirtualMachineList()));

                for(int v=0; v<nVMs; v++){
                    computeHeuristic();
                    computeProbability();
                    choosePMforVM(v);
                    updateTempVMsandPMs();

                    // evaluate tempAntScore
                    loadObjectiveFunctions(tempAntVMs, antVMs, actualPlacement.getDerivedVMs(), tempAntPMs);
                    tempAntScore = Utils.calcPlacemenScore(objectiveFunctions, aPrioriValueList);

                    // replaces the best solution if Ant found a better one
                    if(tempAntScore<antScore){
                        antScore = tempAntScore;
                        antPMs = PhysicalMachine.clonePMsList(tempAntPMs);
                        antVMs = VirtualMachine.cloneVMsList(tempAntVMs);
                    }
                }

                if(antScore<bestAntScore){
                    bestAntScore = antScore;
                    bestAntPMs = PhysicalMachine.clonePMsList(antPMs);
                    bestAntVMs = VirtualMachine.cloneVMsList(antVMs);
                }
            } // end Ants

            // update globalScore, mapPMs, mapVMs according to the best ant if appropiate
            if(bestAntScore<globalScore){
                globalScore = bestAntScore;
                mapPMs = PhysicalMachine.clonePMsList(bestAntPMs);
                mapVMs = VirtualMachine.cloneVMsList(bestAntVMs);
            }

            // update pheromone
            for(int v=0; v<nVMs;v++){
                for(int p=0; p<nPMs;p++){
                    Integer pmIdIterator = mapPMs.get(p).getId();
                    Integer pmId = mapVMs.get(v).getPhysicalMachine();
                    if(pmId.equals(pmIdIterator)) {
                        pheromone[v][p] = (1 - settings.getPheromoneConstant()) * pheromone[v][p] + (1/globalScore);
                    }else{
                        pheromone[v][p] = (1 - settings.getPheromoneConstant()) * pheromone[v][p];
                    }

                    // check max and min pheromone
                    if(pheromone[v][p]>1F){
                        pheromone[v][p] = 1.0F;
                    }else if(pheromone[v][p]<0.2F){
                        pheromone[v][p] = 0.2F;
                    }
                }
            }

        } // end ACO iterations

        Placement newPlacement = new Placement(mapPMs,mapVMs,actualPlacement.getDerivedVMs());
        newPlacement.setPlacementScore(globalScore);

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
     * Update the resources of the pmSelected, as well as the oldPM
     * of the vmSelected chosen. It also sets the PM of vmSelected to pmSelected.
     */
    private void updateTempVMsandPMs() {
        PhysicalMachine pm = tempAntPMs.get(pmSelected);
        VirtualMachine vm = tempAntVMs.get(vmSelected);
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
        }
    }


    /**
     *  Generate a random number between [0,1] and then based on the cumulative
     *  probability a randomVM and randomPM are chosen.
     */
    private void choosePMforVM(vm) {
        Double randomProb = Math.random();
        vmSelected = vm;
        pmSelected = 0;
        Boolean finish=false;
        for (int p = 0; p < nPMs; p++) {
            if (normalizedProb[vmSelected][p] >= randomProb) {
                pmSelected = p;
                finish = true;
                break;
            }
        }
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
        }
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
                        freeSpaceRatio0 = freeSpace0 / pm.getResources().get(0);
                        freeSpaceRatio1 = freeSpace1 / pm.getResources().get(1);
                        freeSpaceRatio2 = freeSpace2 / pm.getResources().get(2);
                        heuristic[v][p] = (freeSpaceRatio0 + freeSpaceRatio1 + freeSpaceRatio2) / 3;
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
