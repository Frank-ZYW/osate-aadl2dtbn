package org.osate2.bayes.dtbn.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osate2.bayes.dtbn.BayesianNetwork.DTBN;
import org.osate2.bayes.dtbn.BayesianNetwork.DTBNNode;

/**
 * Class implementing commonly used static functions.
 */
public class Common {

    /**
     * This function will parse a string in the format used in the assignment
     * to a generic format that is used in the network implementation.
     * For example,
     * -
     * "P(A't=1|B't=2,C't=3)" will be converted into "A't=1|B't=2,C't=3".
     */
    public static String parseQuery(String query) {
        String[] q = query.split("\\(")[1].split("\\)")[0].split("\\|");

        StringBuilder ret = new StringBuilder(q[0] + " | ");
        if (q.length > 1) {
            String[] evidences = q[1].split(",");
            for (String evidence : evidences) {
				ret.append(evidence).append(", ");
			}
            if (evidences.length > 0) {
				ret = new StringBuilder(ret.substring(0, ret.length() - 2));
			}
        }
        return ret.toString();
    }

    /**
     * Parse an evidence string into event list.
     * @param evidenceStr - e.g. "A't=1, B't=2"
     */
    public static List<Pair<String>> parseEvidence(String evidenceStr) {
        String[] events = evidenceStr.replaceAll("\\s+", "").split(",");

        List<Pair<String>> evidence = new ArrayList<>();
        for (String event : events) {
			if (!event.isEmpty()) {
				evidence.add(parseEvent(event));
			}
		}
        return evidence;
    }

    /**
     * Parse a string into an event pair <node, timeSlice>.
     * @param eventStr - e.g. "A't=3"
     */
    public static Pair<String> parseEvent(String eventStr) {
        String[] parts = eventStr.replaceAll("\\s+", "").split("'");
        if (parts.length != 2) {
			throw new RuntimeException("Expected string like \"A't=n\", received " + eventStr);
		}

        String nodeName = parts[0];
        String timeSlice = parts[1].replace("t=", "");
        return new Pair<>(nodeName, timeSlice);
    }

    /**
     * Draw BN Graph by graphviz thought Dot file
     */
	public static void drawGraph(DTBN net, String fullPath) throws IOException, InterruptedException {
        Graph g = new Graph();

        Set<Pair<String>> edges = new HashSet<>();
        for(DTBNNode v : net.getNodes()) {
            // add nodes
			g.setNode(v.getName(), String.format("label=\"%s(%s)\"", v.getCiName(), v.getName()));
            for(DTBNNode parent : v.getParents()) {
				edges.add(new Pair<>(parent.getName(), v.getName()));
			}
            for(DTBNNode child : v.getChildren()) {
				edges.add(new Pair<>(v.getName(), child.getName()));
			}
        }
        // add edges
        for(Pair<String> edge : edges) {
			g.addEdge(edge.u1, edge.u2);
		}

        // storage Dot file
		FileOp.writeFile(fullPath + ".dot", g.getDot());

        // execute by shell
		String execution = String.format("dot -Tpng %s.dot -o %s.png", fullPath, fullPath);
        Process process;
        process = Runtime.getRuntime().exec(execution);
        process.waitFor();
        process.exitValue();
    }

}
