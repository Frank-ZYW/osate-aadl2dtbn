package org.osate2.bayes.dtbn.BayesianNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * DTBN
 * -
 *         An implementation of discrete-time Bayesian Network.
 *         The network is constructed upon a directed acyclic graph.
 * -
 *         This class is the Java implementation of the network
 * -
 *         This implementation relies on string named variables to differentiate
 *         and identify variables. Builder creation pattern is used to support
 *         complex network construction.
 */
public class DTBN implements Iterable<String> {

    // the underlying map recording all variables.
    private final Map<String, DTBNNode> nodesMap = new LinkedHashMap<>();
    // the underlying list recording all variables.
    private final List<DTBNNode> nodesList = new ArrayList<>();
    // number of time slices
    private final Integer nTimeSlice;

    /**
     * The constructor.
     */
    public DTBN(Integer nTimeSlice) {
        if (!(nTimeSlice >= 2)) {
			throw new RuntimeException("Number of time slice must >= 2.");
		}

        this.nTimeSlice = nTimeSlice;
    }

    /**
     * Get number of time slices.
     */
    public Integer getNumTimeSlice() {
        return nTimeSlice;
    }

    /**
     * Add node to the network from strings. A node should be added when all of
     * its parents have been added.
     * -
     * This is a builder function where build parts functions are forwarded to
     * other methods, upon a frailer of building, the operations will be
     * reverted.
     *
     * @param name
     *            - a string to represent the variable name like "name"
     * @param parents
     *            - string array denotes the parent nodes of this variable i.e.
     *            the dependent variables, like ["a", "b", "c", "weather"]
     * @param nodeType
     *            - a string to represent the variable type (Basic event or Boolean Gate)
     * @param probabilities
     *            - string array describes the conditional or unconditional
     *            probabilities of the variable, for example [0.8, 0.1, 0.1]
     */
	public Integer addNode(String[] parents, String event, String ciName, String nodeType, Double[] probabilities) {
		// name & id are automatically generated
		Integer id = nodesList.size();
		String name = String.valueOf(id);
		DTBNNode var = new DTBNNode(name, nodesList.size(), event, ciName, nodeType);
        nodesList.add(var);
        nodesMap.put(name, var);
        try {
            // add outcomes
            String[] tsStrList = new String[nTimeSlice];
            for (int i = 1; i <= nTimeSlice; i++) {
				tsStrList[i - 1] = String.valueOf(i);
			}
            var.addOutcomes(tsStrList);

            // add parents & children (children add is inner)
            List<DTBNNode> parentsNodes = new ArrayList<>();
            try {
                for (String pStr : parents) {
					parentsNodes.add(getNode(pStr));
				}
            } catch (RuntimeException e) {
                throw new RuntimeException("The specified parent node \"" + name + "\" does not exist (yet).");
            }
            var.setParents(parentsNodes);

            // add probability
            if (nodeType.equals("BE")) {
                // var is basic event node
                var.setProbabilities(Stream.of(probabilities).
                        mapToDouble(Double::doubleValue).toArray());
            } else {
                // var is gate node
                var.setProbabilitiesByNodeType();
            }
        } catch (RuntimeException e) {
            nodesList.remove(var);
            nodesMap.remove(name);
            throw e;
        }
		return id;
    }

    /**
     * Check if network contains a variable of give name.
     */
    public boolean hasNode(String name) {
        return nodesMap.containsKey(name);
    }

    /**
     * Get the variable of given name.
     */
    public DTBNNode getNode(String name) {
        if (nodesMap.containsKey(name)) {
			return nodesMap.get(name);
		} else {
			throw new RuntimeException("No such variable <" + name + ">.");
		}
    }

    /**
     * Get the variable of given index.
     */
    public DTBNNode getNode(int id) {
        return nodesList.get(id);
    }

    /**
     * Get nodes list.
     */
    public List<DTBNNode> getNodes() {
        return Collections.unmodifiableList(nodesList);
    }

    /**
     * Get leaf nodes name list.
     */
    public List<String> getLeafNodesName() {
        List<String> leafNodes = new ArrayList<>();
        for (DTBNNode node : nodesList) {
			if (node.getParents().size() == 0) {
				leafNodes.add(node.getName());
			}
		}
        return leafNodes;
    }

	/**
	 * Get root node name.
	 */
	public String getRootNodeName() {
		for (DTBNNode node : nodesList) {
			if (node.getChildren().size() == 0) {
				return node.getName();
			}
		}
		throw new RuntimeException("No root in DTBN !");
	}

    @Override
    public Iterator<String> iterator() {
        return nodesMap.keySet().iterator();
    }

}
