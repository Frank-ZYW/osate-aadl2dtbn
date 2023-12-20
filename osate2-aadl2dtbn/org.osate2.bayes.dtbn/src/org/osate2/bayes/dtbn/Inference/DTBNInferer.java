package org.osate2.bayes.dtbn.Inference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.DoubleStream;

import org.osate2.bayes.dtbn.BayesianNetwork.DTBN;
import org.osate2.bayes.dtbn.Inference.JTree.JunctionTreeAlgorithm;
import org.osate2.bayes.dtbn.Inference.LBP.LoopyBeliefPropagation;
import org.osate2.bayes.dtbn.Inference.util.MPE;
import org.osate2.bayes.dtbn.Util.Common;
import org.osate2.bayes.dtbn.Util.Pair;

public class DTBNInferer {

    private final DTBN net;
    private final String inferEngine; // jtree or lbp

    public DTBNInferer(DTBN net, String inferEngine) {
        this.net = net;
        this.inferEngine = inferEngine;
    }

    /**
     * single query
     */
    public double [] query(String queryStr) {
        IBayesInferer infer;

        switch (inferEngine) {
            case "jtree" -> infer = new JunctionTreeAlgorithm();
            case "lbp" -> infer = new LoopyBeliefPropagation();
            default -> throw new RuntimeException("Infer method must be jtree or lbp.");
        }
        infer.setNetwork(net);

        // Get target & evidence
        String[] parts = Common.parseQuery(queryStr).split("\\|");
        Pair<String> target = Common.parseEvent(parts[0]);
        List<Pair<String>> evidence = Common.parseEvidence(parts[1]);

        for (Pair<String> event : evidence) {
			infer.addEvidence(net.getNode(event.u1), event.u2);
		}

        // infer
        double [] beliefs = infer.getBeliefs(net.getNode(target.u1));

        if (target.u2.equals("*")) {
			return beliefs;
		}
        return Arrays.copyOfRange(beliefs, Integer.parseInt(target.u2) - 1, Integer.parseInt(target.u2));
    }

    /**
     * get MPE (Most Probable Explanation) for query
     */
    public MPE getMPE(String queryStr) {
        List<String> leafNodes = net.getLeafNodesName();
        String evidence = Common.parseQuery(queryStr).split("\\|")[0];

        // record the value of the leaf nodes
        List<Integer> explanation = new ArrayList<>(Collections.nCopies(leafNodes.size(),0));
        double probability = 1.0;

        // query P(leaf1't=*|evidence), P(leaf2't=*|evidence) ... P(leafn't=*|evidence) & get max value of each
        for (int i = 0; i <leafNodes.size(); i++) {
            String subQuery = String.format("P(%s't=* | %s)", leafNodes.get(i), evidence);
            List<Double> probabilities = DoubleStream.of(query(subQuery)).boxed().toList();

            double max = Collections.max(probabilities);
            probability *= max;
            explanation.set(i, probabilities.indexOf(max) + 1);
        }

        return new MPE(leafNodes, explanation, probability);
    }

}
