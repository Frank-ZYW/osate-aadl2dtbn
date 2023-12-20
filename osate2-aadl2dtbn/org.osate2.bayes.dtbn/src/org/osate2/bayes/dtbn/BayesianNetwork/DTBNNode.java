package org.osate2.bayes.dtbn.BayesianNetwork;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import org.osate2.bayes.dtbn.Inference.factor.AbstractFactor;
import org.osate2.bayes.dtbn.Inference.factor.DenseFactor;
import org.osate2.bayes.dtbn.Inference.factor.arraywrapper.DoubleArrayWrapper;
import org.osate2.bayes.dtbn.Inference.util.BidirectionalMap;
import org.osate2.bayes.dtbn.Inference.util.MathUtils;

/**
 * DTBNNode class
 * -
 * Represent a variable node in discrete-time Bayesian network.
 * -
 * This implementation enclose a directed network structure by recording the
 * parents and children nodes.
 * -
 * This class takes partial responsibility of construction the network that will
 * be called from network class.
 */
public class DTBNNode {

    private final String name;
    private final Integer id;
    private List<DTBNNode> parents = new ArrayList<>();
    private List<DTBNNode> children = new ArrayList<>();
	private final String event; // event corresponding to the node
	private final String ciName; // the name of the component which the node belongs to
    private final String nodeType; // type of the node, must in enum NodeType

    /**
     * For standard Bayesian Net, outcomes are the values of variables that nodes can take,
     *     which in DTBN correspond to time slices.
     * e.g. Node A = 1 means event A occurring in 1.st time slice.
     */
    private int outcomes = 0; // equals to nTimeSlices
    private final List<String> outcomesList = new ArrayList<>(); // equals to [1, 2, 3, ... nTimeSlices]
    private final BidirectionalMap<String, Integer> outcomeIndices = new BidirectionalMap<>();
    private final AbstractFactor factor = new DenseFactor(); // the CPT of Node

    enum NodeType {
        BE, OR, XOR, AND, PAND, KorMore, Kof, KorLess; // BE(BasicEvent)
        public static boolean contains(String s) {
            s = s.replaceAll("\\s+", "");
            String [] patterns = {"^BE", "^OR", "^XOR", "^AND", "^PAND", "^\\d+orMore", "^\\d+orLess", "^\\d+of"};
            for(String pattern : patterns) {
				if (Pattern.matches(pattern, s)) {
					return true;
				}
			}
            return false;
        }
    }

    /**
     * Builder methods
     */

	public DTBNNode(String name, Integer id, String event, String ciName, String nodeType) {
        if (!NodeType.contains(nodeType)) {
			throw new RuntimeException("Node type must in BE, OR, XOR, AND, PAND, KorMore, Kof, KorLess.");
		}
        this.name = name;
        this.id = id;
		this.event = event;
		this.ciName = ciName;
        this.nodeType = nodeType;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

	public String getEvent() {
		return event;
	}

	public String getCiName() {
		return ciName;
	}

    public String getNodeType() {
        return nodeType;
    }

    public List<DTBNNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public List<DTBNNode> getParents() {
        return Collections.unmodifiableList(parents);
    }

    /**
     * Set parents of node, parents are ordered !!!
     */
    public void setParents(List<DTBNNode> parents) {
        // remove self from old parent's children
        for (DTBNNode oldParent : this.parents) {
			oldParent.children.remove(this);
		}
        // set new parents
        this.parents = parents;
        // add self to new parent's children
        for (DTBNNode p : parents) {
			p.children.add(this);
		}
        adjustFactorDimensions();
    }

    public void addOutcomes(String... names) {
        if (!Collections.disjoint(outcomesList, Arrays.asList(names))) {
			throw new IllegalArgumentException("Outcome already exists");
		}

        for (String name : names) {
            outcomeIndices.put(name, outcomes);
            outcomes++;
            outcomesList.add(name);
        }
        adjustFactorDimensions();
    }

    public int addOutcome(final String name) {
        addOutcomes(name);
        return outcomes - 1;
    }

    public int getOutcomeIndex(final String name) {
        try {
            return outcomeIndices.get(name);
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException(name, ex);
        }
    }

    public String getOutcomeName(final int index) {
        return outcomeIndices.getKey(index);
    }

    public int getOutcomeCount() {
        return outcomes;
    }

    public List<String> getOutcomes() {
        return Collections.unmodifiableList(outcomesList);
    }

    public AbstractFactor getFactor() {
        return factor;
    }

    /**
     * Fill CPT of BE nodes.
     * Must be called after the parents and outcomes, and the outcome of the parents are set.
     */
    public void setProbabilities(final double[] probabilities) {
        adjustFactorDimensions();
        if (probabilities.length != MathUtils.product(factor.getDimensions())) {
            throw new IllegalArgumentException("Probability table does not have expected size. Expected: "
                    + MathUtils.product(factor.getDimensions()) + "but got: " + probabilities.length);
        }
        factor.setValues(new DoubleArrayWrapper(probabilities));
    }

    /**
     * Fill CPT of gate nodes
     */
    public void setProbabilitiesByNodeType() {
        List<Double> probabilities = new ArrayList<>();
        // record the value(time slice) of the parent(evidence) nodes
        List<Integer> eTs = new ArrayList<>(Collections.nCopies(parents.size(),1));

        fillInOrder(probabilities, eTs, 0);

        if (probabilities.size() != (int) Math.pow(outcomes, parents.size() + 1)) {
			throw new RuntimeException("CPT size not correct.");
		}

        setProbabilities(probabilities.stream().mapToDouble(d -> d).toArray());
    }

    public void fillInOrder(List<Double> probabilities, List<Integer> eTs, Integer currentEvidenceIndex) {
        if (!currentEvidenceIndex.equals(parents.size())) {
            // not target turn
            for (int i = 1; i <= outcomes; i++) {
                eTs.set(currentEvidenceIndex, i);
                fillInOrder(probabilities, eTs, currentEvidenceIndex + 1);
            }
        } else {
            // target turn
            List<Integer> eTsCopy = new ArrayList<>(eTs); // time slices of evidence.events
            Collections.sort(eTsCopy); // Ascending order, min = eTs[0], max = eTs[length-1]

            double sumProbability = 0.0;

            for(int tTs = 1; tTs <= outcomes; tTs++) {

                double probability; // P(target't=i | evidence)

                if (tTs == outcomes) {
                    // P(target't=nTimeSlice | evidence) = 1 - sumProbability
                    probability = 1 - sumProbability;
                } else {
                    if (nodeType.contains("o")) {
                        // KorMore, KorLess, Kof gate
                        int k = Integer.parseInt(nodeType.replaceAll("[a-zA-z]", ""));
                        // replace number in nodeType
                        switch (nodeType.replaceAll("\\d+", "")) {
                            case "orMore" ->
                                // KorMore gate, the K largest value of eTs == tTs -> 1 else 0
                                    probability = eTsCopy.get(k - 1) == tTs ? 1.0 : 0.0;
                            case "orLess" ->
                                // KorLess gate, minETs == tTs && tTs appears in eTs <= k times -> 1 else 0
                                    probability = eTsCopy.get(0) == tTs && eTsCopy.get(k) > tTs ? 1.0 : 0.0;
                            case "of" ->
                                // Kof gate, the K largest value of eTs == tTs && element in eTs <= tTs appears k times -> 1 else 0
                                    probability = eTsCopy.get(k - 1) == tTs && eTsCopy.get(k) > tTs ? 1.0 : 0.0;
                            default -> throw new RuntimeException("Node type must in BE, OR, XOR, " +
                                    "AND, PAND, KorMore, Kof, KorLess.");
                        }
                    } else {
                        // OR, AND, XOR gate
                        switch (nodeType) {
                            case "OR" ->
                                // OR gate, minETs == tTs -> 1 else 0
                                    probability = eTsCopy.get(0) == tTs ? 1.0 : 0.0;
                            case "AND" ->
                                // AND gate, maxETs == tTs -> 1 else 0
                                    probability = eTsCopy.get(eTsCopy.size() - 1) == tTs ? 1.0 : 0.0;
                            case "XOR" ->
                                // XOR gate, minETs == tTs && tTs appears only once in ETs -> 1 else 0
                                    probability = eTsCopy.get(0) == tTs && eTsCopy.get(1) > tTs ? 1.0 : 0.0;
                            default -> throw new RuntimeException("Node type must in BE, OR, XOR, " +
                                    "AND, PAND, KorMore, Kof, KorLess.");
                        }
                    }
                    sumProbability += probability;
                }

                probabilities.add(probability);
            }
        }
    }

    public double[] getProbabilities() {
        return factor.getValues().toDoubleArray();
    }

    /**
     *  CPT must in order: [p1, p2, p3, ... (parents order), self]
     *  Dimensions: e.g. [p1:5 -> p2:3 -> p3:4 -> self:6]. The number represents
     *      the number of variable values that each parent node and itself can take.
     *      The advantage of this is that the probability can be obtained directly by
     *      calculating the index of the element in the array when the query
     *      condition is provided.
     */
    private void adjustFactorDimensions() {
        final int[] dimensions = new int[parents.size() + 1];
        final int[] dimensionIds = new int[parents.size() + 1];
        fillWithParentDimensions(dimensions, dimensionIds);
        insertSelf(dimensions, dimensionIds);
        factor.setDimensions(dimensions);
        factor.setDimensionIDs(dimensionIds);
    }

    private void insertSelf(final int[] dimensions, final int[] dimensionIds) {
        dimensions[dimensions.length - 1] = getOutcomeCount();
        dimensionIds[dimensionIds.length - 1] = getId();
    }

    private void fillWithParentDimensions(final int[] dimensions, final int[] dimensionIds) {
        for (ListIterator<DTBNNode> it = parents.listIterator(); it.hasNext();) {
            final DTBNNode p = it.next();
            dimensions[it.nextIndex() - 1] = p.getOutcomeCount();
            dimensionIds[it.nextIndex() - 1] = p.getId();
        }
    }

    @Override
	public String toString() {
        StringBuilder ret = new StringBuilder(String.format("Node: %s  Type: %s \n", name, nodeType));
        ret.append("CPT:\n");

        // record the value of the parent nodes
        List<Integer> pTs = new ArrayList<>(Collections.nCopies(parents.size(),1));
        toStringInOrder(ret, pTs, 0);

        return ret.toString();
    }

    public void toStringInOrder(StringBuilder ret, List<Integer> pTs, Integer currentParentIndex) {
        if (!currentParentIndex.equals(parents.size())) {
            // not self turn
            for (int i = 1; i <= outcomes; i++) {
                pTs.set(currentParentIndex, i);
                toStringInOrder(ret, pTs, currentParentIndex + 1);
            }
        } else {
            // self turn
            for (int i = 1; i <= outcomes; i++) {
                ret.append("[");

                int index = 0;
                for (int j = 0; j < parents.size(); j++) {
                    index += (pTs.get(j) - 1) * Math.pow(outcomes, parents.size() - j);
                    ret.append(String.format("%s't=%d, ", parents.get(j).getName(), pTs.get(j)));
                }
                index += i - 1;

                ret.append(String.format("%s't=%d", name, i));
                ret.append(String.format("] : %f\n", factor.getValue(index)));
            }
        }
    }

}
