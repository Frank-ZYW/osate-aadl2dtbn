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

import java.util.Map;

import org.osate2.bayes.dtbn.BayesianNetwork.DTBN;
import org.osate2.bayes.dtbn.BayesianNetwork.DTBNNode;

public interface IBayesInferer {

    void setNetwork(DTBN bayesNet);

    void setEvidence(Map<DTBNNode, String/*outcome*/> evidence);

    void addEvidence(DTBNNode node, String outcome);

    Map<DTBNNode, String> getEvidence();

    double[] getBeliefs(DTBNNode node);

}
