package org.osate2.bayes.dtbn.Util;

import java.util.ArrayList;
import java.util.HashMap;

/** Create Graph of BN by Dot Language **/
public class Graph {

    private final ArrayList<String> edges;
    private final HashMap<String, String> nodes;

    public Graph() {
        this.edges = new ArrayList<>();
        this.nodes = new HashMap<>();
    }

    public String getDot() {
        StringBuilder sb = new StringBuilder(500);
        sb.append("digraph g{\n\n");
        for(String edge : this.edges) {
			sb.append(edge);
		}
        sb.append("\n");
        for(String node : this.nodes.values()) {
			sb.append(node);
		}
        sb.append("\n}\n");
        return sb.toString();
    }

    public void addEdge(String nodeFrom, String nodeTo, String... attrs) {
        StringBuilder sb = new StringBuilder(50);
        sb.append(nodeFrom).append(" -> ").append(nodeTo);
        if(attrs.length > 0) {
            sb.append(" [");
            for (String attr : attrs) {
				sb.append(attr);
			}
            sb.append("];\n");
        } else {
            sb.append(";\n");
        }
        this.edges.add(sb.toString());
    }

    public void setNode(String nodeName, String... attrs) {
        StringBuilder sb = new StringBuilder(50);
        sb.append(nodeName);
        if(attrs.length > 0) {
            sb.append(" [");
            for (String attr : attrs) {
				sb.append(attr);
			}
            sb.append("];\n");
        } else {
            sb.append(";\n");
        }
        this.nodes.put(nodeName, sb.toString());
    }

}
