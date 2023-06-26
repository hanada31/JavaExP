package com.iscas.JavaExP.client.soot;

import soot.toolkits.graph.Block;
import soot.toolkits.graph.DominatorNode;
import soot.toolkits.graph.DominatorTree;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.*;

import java.util.*;

/**
 * @Author hanada
 * @Date 2023/6/26 15:37
 * @Version 1.0
 */
public class MyHashMutablePDG extends HashMutablePDG {
    public MyHashMutablePDG(UnitGraph cfg) {
        super(cfg);
    }

    protected void constructPDG() {
        Hashtable<Block, Region> block2region = this.m_regionAnalysis.getBlock2RegionMap();
        DominatorTree<Block> pdom = this.m_regionAnalysis.getPostDominatorTree();
        DominatorTree<Block> dom = this.m_regionAnalysis.getDominatorTree();

        List<Region> regions2process = new LinkedList<Region>();
        Region topLevelRegion = this.m_regionAnalysis.getTopLevelRegion();
        this.m_strongRegionStartID = m_weakRegions.size();

        // This becomes the top-level region (or ENTRY region node)
        PDGNode pdgnode = new PDGNode(topLevelRegion, PDGNode.Type.REGION);
        this.addNode(pdgnode);
        this.m_obj2pdgNode.put(topLevelRegion, pdgnode);
        this.m_startNode = pdgnode;
        topLevelRegion.setParent(null);

        Set<Region> processedRegions = new HashSet<Region>();
        regions2process.add(topLevelRegion);

        // while there's a (weak) region to process
        while (!regions2process.isEmpty()) {
            Region r = regions2process.remove(0);
            processedRegions.add(r);

            // get the corresponding pdgnode
            pdgnode = this.m_obj2pdgNode.get(r);

            // For all the CFG nodes in the region, create the corresponding PDG node and edges, and process
            // them if they are in the dependence set of other regions, i.e. other regions depend on them.
            List<Block> blocks = r.getBlocks();
            Hashtable<Region, List<Block>> toBeRemoved = new Hashtable<Region, List<Block>>();
            PDGNode prevPDGNodeInRegion = null;
            PDGNode curNodeInRegion;
            for (Block a : blocks) {
                // Add the PDG node corresponding to the CFG block node.
                PDGNode pdgNodeOfA;
                if (!this.m_obj2pdgNode.containsKey(a)) {
                    pdgNodeOfA = new PDGNode(a, PDGNode.Type.CFGNODE);
                    this.addNode(pdgNodeOfA);
                    this.m_obj2pdgNode.put(a, pdgNodeOfA);
                } else {
                    pdgNodeOfA = this.m_obj2pdgNode.get(a);
                }
                this.addEdge(pdgnode, pdgNodeOfA, "dependency");
                pdgnode.addDependent(pdgNodeOfA);
                //
                curNodeInRegion = pdgNodeOfA;
                // For each successor B of A, if B does not post-dominate A, add all the
                // nodes on the path from B to the L in the post-dominator tree, where
                // L is the least common ancestor of A and B in the post-dominator tree
                // (L will be either A itself or the parent of A.).
                for (Block b : this.m_blockCFG.getSuccsOf(a)) {
                    if (b.equals(a)) {
                        // throw new RuntimeException("PDG construction: A and B are not supposed to be the same node!");
                        continue;
                    }

                    DominatorNode<Block> aDode = pdom.getDode(a);
                    DominatorNode<Block> bDode = pdom.getDode(b);

                    // If B post-dominates A, go to the next successor.
                    if (pdom.isDominatorOf(bDode, aDode)) {
                        continue;
                    }
                    List<Block> dependents = new ArrayList<Block>();

                    // FIXME: what if the parent is null?!!
                    DominatorNode<Block> aParentDode = aDode.getParent();
                    DominatorNode<Block> dode = bDode;
                    while (dode != aParentDode) {
                        if(dependents.size()>10000) break;
                        dependents.add(dode.getGode());
                        // This occurs if the CFG is multi-tailed and therefore the pdom is a forest.
                        if (dode.getParent() == null) {
                            // throw new RuntimeException("parent dode in pdom is null: dode is " + aDode);
                            break;
                        }
                        dode = dode.getParent();

                    }

                    // If node A is in the dependent list of A, then A is the header of a loop.
                    // Otherwise, A could still be the header of a loop or just a simple predicate.
                    //
                    // first make A's pdg node be a conditional (predicate) pdgnode, if it's not already.
                    if (pdgNodeOfA.getAttrib() != PDGNode.Attribute.CONDHEADER) {
                        PDGNode oldA = pdgNodeOfA;
                        pdgNodeOfA = new ConditionalPDGNode(pdgNodeOfA);
                        this.replaceInGraph(pdgNodeOfA, oldA);
                        pdgnode.removeDependent(oldA);
                        this.m_obj2pdgNode.put(a, pdgNodeOfA);
                        pdgnode.addDependent(pdgNodeOfA);
                        pdgNodeOfA.setAttrib(PDGNode.Attribute.CONDHEADER);

                        curNodeInRegion = pdgNodeOfA;
                    }

                    List<Block> copyOfDependents = new ArrayList<Block>(dependents);

                    // First, add the dependency for B and its corresponding region.
                    Region regionOfB = block2region.get(b);
                    PDGNode pdgnodeOfBRegion;
                    if (this.m_obj2pdgNode.containsKey(regionOfB)) {
                        pdgnodeOfBRegion = this.m_obj2pdgNode.get(regionOfB);
                    } else {
                        pdgnodeOfBRegion = new PDGNode(regionOfB, PDGNode.Type.REGION);
                        this.addNode(pdgnodeOfBRegion);
                        this.m_obj2pdgNode.put(regionOfB, pdgnodeOfBRegion);
                    }

                    // set the region hierarchy
                    regionOfB.setParent(r);
                    r.addChildRegion(regionOfB);

                    // add the dependency edges
                    this.addEdge(pdgNodeOfA, pdgnodeOfBRegion, "dependency");
                    pdgNodeOfA.addDependent(pdgnodeOfBRegion);
                    if (!processedRegions.contains(regionOfB)) {
                        regions2process.add(regionOfB);
                    }

                    // now remove b and all the nodes in the same weak region from the list of dependents
                    copyOfDependents.remove(b);
                    copyOfDependents.removeAll(regionOfB.getBlocks());

                    /*
                     * What remains here in the dependence set needs to be processed separately. For each node X remained in the
                     * dependency set, find the corresponding PDG region node and add a dependency edge from the region of B to the
                     * region of X. If X's weak region contains other nodes not in the dependency set of A, create a new region for X
                     * and add the proper dependency edges (this actually happens if X is the header of a loop and B is a predicate
                     * guarding a break/continue.)
                     *
                     * Note: it seems the only case that there is a node remained in the dependents is when there is a path from b to
                     * the header of a loop.
                     */
                    while (!copyOfDependents.isEmpty()) {
                        Block depB = copyOfDependents.remove(0);
                        Region rdepB = block2region.get(depB);

                        // Actually, there are cases when depB is not the header of a loop
                        // and therefore would not dominate the current node (A) and therefore
                        // might not have been created yet. This has happened when an inner
                        // loop breaks out of the outer loop but could have other cases too.
                        PDGNode depBPDGNode = this.m_obj2pdgNode.get(depB);
                        if (depBPDGNode == null) {
                            // First, add the dependency for depB and its corresponding region.
                            PDGNode pdgnodeOfdepBRegion;
                            if (this.m_obj2pdgNode.containsKey(rdepB)) {
                                pdgnodeOfdepBRegion = this.m_obj2pdgNode.get(rdepB);
                            } else {
                                pdgnodeOfdepBRegion = new PDGNode(rdepB, PDGNode.Type.REGION);
                                this.addNode(pdgnodeOfdepBRegion);
                                this.m_obj2pdgNode.put(rdepB, pdgnodeOfdepBRegion);
                            }

                            // set the region hierarchy
                            rdepB.setParent(regionOfB);
                            regionOfB.addChildRegion(rdepB);

                            // add the dependency edges
                            this.addEdge(pdgnodeOfBRegion, pdgnodeOfdepBRegion, "dependency");
                            pdgnodeOfBRegion.addDependent(pdgnodeOfdepBRegion);
                            if (!processedRegions.contains(rdepB)) {
                                regions2process.add(rdepB);
                            }

                            // now remove all the nodes in the same weak region from the list of dependents
                            copyOfDependents.removeAll(rdepB.getBlocks());
                        } else if (dependents.containsAll(rdepB.getBlocks())) {
                            /*
                             * If all the nodes in the weak region of depB are dependent on A, then add an edge from the region of B to the
                             * region of depB. Else, a new region has to be created to contain the dependences of depB, if not already
                             * created.
                             */
                            // Just add an edge to the pdg node of the existing depB region.
                            //
                            // add the dependency edges
                            // First, add the dependency for depB and its corresponding region.
                            PDGNode pdgnodeOfdepBRegion;
                            if (this.m_obj2pdgNode.containsKey(rdepB)) {
                                pdgnodeOfdepBRegion = this.m_obj2pdgNode.get(rdepB);
                            } else {
                                pdgnodeOfdepBRegion = new PDGNode(rdepB, PDGNode.Type.REGION);
                                this.addNode(pdgnodeOfdepBRegion);
                                this.m_obj2pdgNode.put(rdepB, pdgnodeOfdepBRegion);
                            }

                            // set the region hierarchy
                            // Do not set this because the region was created before so must have the
                            // proper parent already.
                            // rdepB.setParent(regionOfB);
                            // regionOfB.addChildRegion(rdepB);
                            this.addEdge(pdgnodeOfBRegion, pdgnodeOfdepBRegion, "dependency");
                            pdgnodeOfBRegion.addDependent(pdgnodeOfdepBRegion);
                            if (!processedRegions.contains(rdepB)) {
                                regions2process.add(rdepB);
                            }

                            // now remove all the nodes in the same weak region from the list of dependents
                            copyOfDependents.removeAll(rdepB.getBlocks());
                        } else {

                            PDGNode predPDGofdepB = this.getPredsOf(depBPDGNode).get(0);
                            if (!this.m_obj2pdgNode.containsKey(rdepB)) return;
                            PDGNode pdgnodeOfdepBRegion = this.m_obj2pdgNode.get(rdepB);
                            // If the loop header has not been separated from its weak region yet

                            if (predPDGofdepB == pdgnodeOfdepBRegion) {
                                // Create a new region to represent the whole loop. In fact, this is a strong region as opposed to the weak
                                // regions that were created in the RegionAnalysis. This strong region only contains the header of the loop,
                                // A, and is dependent on it. Also, A is dependent on this strong region as well.
                                Region newRegion = new Region(this.m_strongRegionStartID++, topLevelRegion.getSootMethod(),
                                        topLevelRegion.getSootClass(), this.m_cfg);
                                newRegion.add(depB);

                                this.m_strongRegions.add(newRegion);

                                // toBeRemoved.add(depB);
                                List<Block> blocks2BRemoved;
                                if (toBeRemoved.contains(predPDGofdepB)) {
                                    blocks2BRemoved = toBeRemoved.get(predPDGofdepB);
                                } else {
                                    blocks2BRemoved = new ArrayList<Block>();
                                    toBeRemoved.put(rdepB, blocks2BRemoved);
                                }
                                blocks2BRemoved.add(depB);

                                PDGNode newpdgnode = new LoopedPDGNode(newRegion, PDGNode.Type.REGION, depBPDGNode);
                                this.addNode(newpdgnode);
                                this.m_obj2pdgNode.put(newRegion, newpdgnode);
                                newpdgnode.setAttrib(PDGNode.Attribute.LOOPHEADER);
                                depBPDGNode.setAttrib(PDGNode.Attribute.LOOPHEADER);

                                this.removeEdge(pdgnodeOfdepBRegion, depBPDGNode, "dependency");
                                pdgnodeOfdepBRegion.removeDependent(depBPDGNode);
                                this.addEdge(pdgnodeOfdepBRegion, newpdgnode, "dependency");
                                this.addEdge(newpdgnode, depBPDGNode, "dependency");
                                pdgnodeOfdepBRegion.addDependent(newpdgnode);
                                newpdgnode.addDependent(depBPDGNode);

                                // If a is dependent on itself (simple loop)
                                if (depB == a) {
                                    PDGNode loopBodyPDGNode = this.getSuccsOf(depBPDGNode).get(0);
                                    this.addEdge(depBPDGNode, newpdgnode, "dependency-back");
                                    ((LoopedPDGNode) newpdgnode).setBody(loopBodyPDGNode);

                                    depBPDGNode.addBackDependent(newpdgnode);

                                    // This is needed to correctly adjust the prev/next pointers for the new loop node. We should not need
                                    // to adjust the old loop header node because the prev/next should not have been set previously for it.
                                    curNodeInRegion = newpdgnode;
                                } else {
                                    // this is a back-dependency
                                    pdgnodeOfBRegion.addBackDependent(newpdgnode);
                                    this.addEdge(pdgnodeOfBRegion, newpdgnode, "dependency-back");

                                    // Determine which dependent of the loop header is actually the loop body region
                                    PDGNode loopBodyPDGNode = null;
                                    for (PDGNode succRPDGNode : this.getSuccsOf(depBPDGNode)) {
                                        if (succRPDGNode.getType() == PDGNode.Type.REGION) {
                                            Region succR = (Region) succRPDGNode.getNode();
                                            Block h = succR.getBlocks().get(0);
                                            if (dom.isDominatorOf(dom.getDode(h), dom.getDode(a))) {
                                                loopBodyPDGNode = succRPDGNode;
                                                break;
                                            }
                                        }
                                    }
                                    if(loopBodyPDGNode==null) return;
                                    ((LoopedPDGNode) newpdgnode).setBody(loopBodyPDGNode);

                                    PDGNode prev = depBPDGNode.getPrev();
                                    if (prev != null) {
                                        this.removeEdge(prev, depBPDGNode, "controlflow");
                                        this.addEdge(prev, newpdgnode, "controlflow");
                                        prev.setNext(newpdgnode);
                                        newpdgnode.setPrev(prev);
                                        depBPDGNode.setPrev(null);

                                    }

                                    PDGNode next = depBPDGNode.getNext();
                                    if (next != null) {
                                        this.removeEdge(depBPDGNode, next, "controlflow");
                                        this.addEdge(newpdgnode, next, "controlflow");
                                        newpdgnode.setNext(next);
                                        next.setPrev(newpdgnode);
                                        depBPDGNode.setNext(null);
                                    }
                                }
                            } else {
                                // The strong region for the header has already been created and
                                // its corresponding PDGNode exist. Just add the dependency edge.
                                this.addEdge(pdgnodeOfBRegion, predPDGofdepB, "dependency-back");
                                // this is a back-dependency
                                pdgnodeOfBRegion.addBackDependent(predPDGofdepB);
                            }
                        }
                    }
                }
                // If there is a previous node in this region, add a control flow edge
                // to indicate the correct direction of control flow in the region.
                if (prevPDGNodeInRegion != null) {
                    this.addEdge(prevPDGNodeInRegion, curNodeInRegion, "controlflow");
                    prevPDGNodeInRegion.setNext(curNodeInRegion);
                    curNodeInRegion.setPrev(prevPDGNodeInRegion);
                }
                prevPDGNodeInRegion = curNodeInRegion;
            }

            // remove all the blocks marked to be removed from the region (to change a weak region
            // to a strong region.)
            for (Enumeration<Region> itr = toBeRemoved.keys(); itr.hasMoreElements();) {
                Region region = itr.nextElement();
                for (Block next : toBeRemoved.get(region)) {
                    region.remove(next);
                }
            }
        }
    }
}
