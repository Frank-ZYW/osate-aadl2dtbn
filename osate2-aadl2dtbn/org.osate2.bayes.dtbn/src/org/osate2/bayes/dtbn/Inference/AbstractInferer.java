/**
 * Copyright (c) 2011 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Michael Kutschke - initial API and implementation.
 */
package org.osate2.bayes.dtbn.Inference;

import java.util.HashMap;
import java.util.Map;

import org.osate2.bayes.dtbn.BayesianNetwork.DTBN;
import org.osate2.bayes.dtbn.BayesianNetwork.DTBNNode;
import org.osate2.bayes.dtbn.Inference.factor.FactorFactory;

public abstract class AbstractInferer implements IBayesInferer {

    protected Map<DTBNNode, String> evidence = new HashMap<DTBNNode, String>();

    protected double[][] beliefs;
    protected boolean beliefsValid;

    protected FactorFactory factory = FactorFactory.defaultFactory();

    public void setFactorFactory(FactorFactory factory) {
        this.factory = factory;

    }

    public FactorFactory getFactory() {
        return factory;
    }

    @Override
    public void addEvidence(final DTBNNode node, final String outcome) {
        evidence.put(node, outcome);
        beliefsValid = false;
    }

    @Override
    public double[] getBeliefs(final DTBNNode node) {
        if (!beliefsValid) {
            beliefsValid = true;
            updateBeliefs();
        }
        return beliefs[node.getId()];
    }

    @Override
    public void setNetwork(final DTBN bayesNet) {
        beliefs = new double[bayesNet.getNodes().size()][];
        for (final DTBNNode n : bayesNet.getNodes()) {
            beliefs[n.getId()] = new double[n.getOutcomeCount()];
        }
        this.factory.setReferenceNetwork(bayesNet);
    }

    @Override
    public void setEvidence(final Map<DTBNNode, String> evidence) {
        this.evidence = evidence;
        beliefsValid = false;
    }

    @Override
    public Map<DTBNNode, String> getEvidence() {
        return evidence;
    }

    protected abstract void updateBeliefs();

}
