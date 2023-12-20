package org.osate2.bayes.dtbn.Inference.util;

import java.util.Collections;
import java.util.List;

/**
 * MPE (Most Probable Explanation)
 */
public class MPE {

    private final List<String> leafNodes; // leaf node names (orderly)
    private final List<Integer> explanation; // a group of time slices of leafs
    private final Double probability;

    public MPE(List<String> leafNodes, List<Integer> explanation, Double probability) {
        this.leafNodes = leafNodes;
        this.explanation = explanation;
        this.probability = probability;
    }

    public List<String> getLeafNodes() {
        return Collections.unmodifiableList(leafNodes);
    }

    public List<Integer> getExplanation() {
        return Collections.unmodifiableList(explanation);
    }

    public Double getProbability() {
        return probability;
    }

    /**
     * Explanation to str
     */
    @Override
	public String toString() {
        StringBuilder ret = new StringBuilder("[");
        for (int i = 0; i < leafNodes.size(); i++) {
			ret.append(String.format("%s't=%d, ", leafNodes.get(i), explanation.get(i)));
		}
        ret = new StringBuilder(ret.substring(0, ret.length() - 2));
        ret.append("] : ").append(probability);
        return ret.toString();
    }

}
