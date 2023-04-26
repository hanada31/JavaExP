package com.iscas.exceptionextractor.utils;

import com.iscas.exceptionextractor.model.analyzeModel.ControlDependOnUnit;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThrowStmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.IRegion;
import soot.toolkits.graph.pdg.PDGNode;
import soot.toolkits.graph.pdg.PDGRegion;
import soot.util.HashMultiMap;

import java.util.*;

/**
 * @Author hanada
 * @Date 2023/4/23 10:51
 * @Version 1.0
 */
public class PDGUtils extends HashMutablePDG {
    SootMethod sootMethod;
    Map<Unit, Block> unitToBlock;
    UnitGraph cfg;

    //maintain only direct control dependency: <unit1, unit2> where statement unit1 is control dependent of statement unit2
    HashMultiMap<Unit, Unit> CDMap;

    //maintain both direct and indirect control dependency: <unit1, set2> where statement unit1 is control dependent of all the statements in set2
    Map<Unit, HashSet<Unit>> CDSMap;

    public PDGUtils(SootMethod sootMethod, UnitGraph cfg) {
        super(cfg);
        this.cfg = cfg;
        this.CDMap = new HashMultiMap<Unit, Unit>();
        this.CDSMap = new HashMap<Unit, HashSet<Unit>>();
        this.sootMethod = sootMethod;
        buildUnitToBlockMap();
    }

    protected void buildUnitToBlockMap()
    {
        unitToBlock = new HashMap<Unit,Block>();

        for (Iterator<Block> iter = m_blockCFG.iterator(); iter.hasNext(); ) {
            Block b = iter.next();
            for (Iterator<Unit> unitIter = b.iterator(); unitIter.hasNext(); ) {
                Unit u = unitIter.next();

                // Check if this assumption holds
                assert(! unitToBlock.containsKey(u));

                unitToBlock.put(u, b);
            }
        }
    }

    /**
     * Finds the PDGNode that contains the unit u, or u if unit is not in the PDG
     *
     * @param u
     * @return the PDGNode that contains u
     */
    public PDGNode getPDGNodeFromUnit(Unit u)
    {
        Block b = unitToBlock.get(u);
        if (u != null) {
            return getPDGNode(b);
        }
        else {
            return null;
        }
    }

    public void analyzeControlDependency(){
        /* first step: get direct control dependency information */
        HashMutablePDG pdg = new HashMutablePDG(cfg);
        for(Iterator<PDGNode> it = pdg.iterator(); it.hasNext();){
            PDGNode controlNode = it.next();
            if(controlNode.getType() == PDGNode.Type.CFGNODE){
                //if it's CFGNODE, cast this pdg node to a block
                Block bk = (Block) controlNode.getNode();
                Unit tail = bk.getTail();
                //check whether the tail unit of the block contains controlling expression: ifStmt and SwitchStmt
                if(tail instanceof IfStmt || tail instanceof TableSwitchStmt || tail instanceof LookupSwitchStmt){
                    for(PDGRegion r: pdg.getPDGRegions()) {
                        PDGNode node = r.getCorrespondingPDGNode();
                        //check whether node is control dependent of controlNode
                        if(pdg.dependentOn(node, controlNode)){
                            IRegion region = (IRegion) node.getNode();
                            //all the units in this region are control dependent of tail
                            for(Unit u: region.getUnits()) {
                                CDMap.put(u, tail);
                            }
                        }
                    }
                }
            }
        }
        /* second step: calculate both indirect and direct control dependency maintained by CDSMap */
        for(Unit key: CDMap.keySet()){
            CDSMap.put(key, new HashSet<Unit>());
            addIntoCDSMap(key, key);
        }
    }

    public void analyzeThrowControlDependency(){
        /* first step: get direct control dependency information */
        HashMutablePDG pdg = new HashMutablePDG(cfg);
        for(Iterator<PDGNode> it = pdg.iterator(); it.hasNext();){
            PDGNode controlNode = it.next();
            if(controlNode.getType() == PDGNode.Type.CFGNODE){
                //if it's CFGNODE, cast this pdg node to a block
                Block bk = (Block) controlNode.getNode();
                Unit tail = bk.getTail();
                //check whether the tail unit of the block contains controlling expression: ifStmt and SwitchStmt
                if(tail instanceof IfStmt || tail instanceof TableSwitchStmt || tail instanceof LookupSwitchStmt){
                    for(Iterator<PDGNode> it2 = pdg.iterator(); it2.hasNext();){
                        PDGNode node = it2.next();
                        //check whether node is control dependent of controlNode
                        if(pdg.dependentOn(node, controlNode)){
                            IRegion region = (IRegion) node.getNode();
                            //all the units in this region are control dependent of tail
                            for(Unit u: region.getUnits()){
                                CDMap.put(u, tail);
                            }
                        }
                    }
                }
            }
        }

        /* second step: calculate both indirect and direct control dependency maintained by CDSMap */
        for(Unit key: CDMap.keySet()){
            if(!(key instanceof ThrowStmt)) continue; // only throw units are concerned
            CDSMap.put(key, new HashSet<Unit>());
            addIntoCDSMap(key, key);
        }
    }

    private void addIntoCDSMap(Unit key, Unit newKey) {
        if(CDMap.containsKey(newKey)){
            for(Unit val: CDMap.get(newKey)) {
                if(!CDSMap.get(key).contains(val)) {
                    CDSMap.get(key).add(val);
                    addIntoCDSMap(key, val);
                }
            }
        }
    }

    /**
     * getAllControlPathFromPDG
     * for control analysis
     * @param controlUnits
     * @param mPath
     * @return
     */
    public static List<ControlDependOnUnit> getControlPathFromPDG(HashSet<Unit> controlUnits, List<Unit> mPath) {
        List<ControlDependOnUnit> controlPath = new ArrayList<>();
        if(controlUnits ==null) return controlPath;
        for(Unit controlUnit: controlUnits){
            if(!(controlUnit instanceof  IfStmt)) continue;
            IfStmt ifStmt = (IfStmt) controlUnit;
            Unit ifTarget = ifStmt.getTarget();
            Unit ifNext = mPath.get(mPath.indexOf(ifStmt)+1);
            boolean satisfied = (ifTarget == ifNext)?true:false;
            controlPath.add(new ControlDependOnUnit(ifStmt, satisfied));
        }
        //for shortcut paths
        List<ControlDependOnUnit> dominatePathCopy = new ArrayList<>(controlPath);
        for(ControlDependOnUnit controlDependOnUnit : dominatePathCopy){
            if(!mPath.contains(controlDependOnUnit.getUnit()))
                controlPath.remove(controlDependOnUnit);
        }
        return controlPath;
    }
    public HashMultiMap<Unit, Unit> getCDMap() {
        return CDMap;
    }


    public Map<Unit, HashSet<Unit>> getCDSMap() {
        return CDSMap;
    }


    public void outputCDMap(){
        System.out.println(CDMap.toString());
    }

    public void outputCDSMap(){
        System.out.println(PrintUtils.printMap(CDSMap));
    }
}
