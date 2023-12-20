package org.osate2.bayes.dtbn.Transform;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.emf.common.util.EList;
import org.osate.aadl2.BasicPropertyAssociation;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.errormodel.FaultTree.Event;
import org.osate.aadl2.errormodel.FaultTree.FaultTree;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorTypes;
import org.osate2.bayes.dtbn.BayesianNetwork.DTBN;
import org.osate2.bayes.dtbn.Util.PropertyParseUtil;
import org.osate2.bayes.dtbn.Util.Sampler;

public class ModelToDTBN {

	public static DTBN ftaToDTBN(FaultTree ftmodel, Integer taskTime, Integer nTaskSlice) {
		DTBN ftNet = new DTBN(nTaskSlice + 1);
		Sampler s = new Sampler(taskTime, nTaskSlice);
		Map<Integer, String> created = new HashMap<>();

		transByDFS(ftNet, s, ftmodel.getRoot(), created);
		return ftNet;
	}

	public static String transByDFS(DTBN ftNet, Sampler s, Event e, Map<Integer, String> created) {
		EList<Event> subEvents = e.getSubEvents();
		// the children of e in FTA is the parents of e in BN
		String[] parents = new String[subEvents.size()];

		// not leaf node
		if (!subEvents.isEmpty()) {
			// visit children
			for (int i = 0; i < subEvents.size(); i++) {
				parents[i] = transByDFS(ftNet, s, subEvents.get(i), created);
			}
		}

		// create self (for both BE & Gate node)

		Integer eHash = contentHashCode(e);
		if (created.containsKey(eHash)) {
			// event node has been created
			return created.get(eHash);
		}

		ComponentInstance nodeCi = (ComponentInstance) e.getRelatedInstanceObject();
		String ciName = nodeCi.getFullName();
		String event = e.getName();

		String logic;
		switch (e.getSubEventLogic().getName()) {
		case "Or" -> logic = "OR";
		case "And" -> logic = "AND";
		case "Xor" -> logic = "XOR";
		case "kOrmore" -> logic = String.valueOf(e.getK()) + "orMore";
		default -> throw new RuntimeException("other logic not support now");
		}

		String nodeType;
		switch (e.getType().getName()) {
		case "Basic" -> nodeType = "BE";
		case "Intermediate" -> nodeType = logic;
		default -> throw new RuntimeException("others node type not support now");
		}

		Double[] samples = null;
		if (nodeType == "BE") {
			// Basic event needs to provide a priori probabilities
			samples = getSamples(s, e);
		}

		Integer nodeId = ftNet.addNode(parents, event, ciName, nodeType, samples);
		created.put(eHash, String.valueOf(nodeId));
		return String.valueOf(nodeId);
	}

	public static Double[] getSamples(Sampler s, Event e) {
		Double[] samples = null;

		InstanceObject io = (InstanceObject) e.getRelatedInstanceObject();
		NamedElement ne = (NamedElement) e.getRelatedEMV2Object();
		ErrorTypes et = (ErrorTypes) e.getRelatedErrorType();

		EList<BasicPropertyAssociation> bayesField = PropertyParseUtil.getPropertyField("Bayes::Distribution", io, ne,
				et);

		if (bayesField != null) {
			String distribution = PropertyParseUtil.getStringProperty(bayesField, "Distribution", null);
			Map<String, Double> params = new HashMap<>();

			switch (distribution) {
			case "Exponential" -> {
				params.put("mean", PropertyParseUtil.getDoubleProperty(bayesField, "MeanValue", null));
			}
			case "Weibull" -> {
				params.put("shape", PropertyParseUtil.getDoubleProperty(bayesField, "ShapeParameter", null));
				params.put("scale", PropertyParseUtil.getDoubleProperty(bayesField, "ScaleParameter", null));
			}
			case "" -> {
				params.put("mean", PropertyParseUtil.getDoubleProperty(bayesField, "MeanValue", null));
				params.put("sd", PropertyParseUtil.getDoubleProperty(bayesField, "StandardDeviation", null));
			}
			default -> throw new RuntimeException("Distribution type must in Exponential, Normal, Weibull.");
			}

			if (params.containsValue(null)) {
				throw new RuntimeException("parameters must not be null.");
			}
			samples = s.sampling(distribution, params);
			return samples;
		}

		throw new RuntimeException("Basic Event must provide priori probabilities.");
	}

	public static int contentHashCode(Event e) {
		ComponentInstance nodeCi = (ComponentInstance) e.getRelatedInstanceObject();
		return Objects.hash(nodeCi.getFullName(), e.getK(), e.getAssignedProbability(), e.getComputedProbability(),
				e.getReferenceCount(), e.getType(), e.getSubEventLogic(), e.getSubEvents(),
				e.getRelatedEMV2Object(), e.getRelatedErrorType());
	}

}
