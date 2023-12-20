package org.osate2.bayes.dtbn.Handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.xtext.ui.util.ResourceUtil;
import org.osate.aadl2.errormodel.FaultTree.FaultTree;
import org.osate.aadl2.errormodel.FaultTree.FaultTreeType;
import org.osate.aadl2.errormodel.faulttree.generation.Activator;
import org.osate.aadl2.errormodel.faulttree.generation.CreateFTAModel;
import org.osate.aadl2.errormodel.faulttree.util.SiriusUtil;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.modelsupport.EObjectURIWrapper;
import org.osate.aadl2.modelsupport.resources.OsateResourceUtil;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.osate.ui.dialogs.Dialog;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorBehaviorState;
import org.osate.xtext.aadl2.errormodel.errorModel.ErrorPropagation;
import org.osate.xtext.aadl2.errormodel.errorModel.TypeToken;
import org.osate.xtext.aadl2.errormodel.util.EMV2TypeSetUtil;
import org.osate.xtext.aadl2.errormodel.util.EMV2Util;
import org.osate2.bayes.dtbn.BayesianNetwork.DTBN;
import org.osate2.bayes.dtbn.Inference.DTBNInferer;
import org.osate2.bayes.dtbn.Transform.ModelToDTBN;
import org.osate2.bayes.dtbn.Util.Common;

import jxl.write.WriteException;

public final class DTBNHandler extends AbstractHandler {

	private static String ERROR_STATE_NAME = null;
	private static FaultTreeType FAULT_TREE_TYPE = FaultTreeType.FAULT_TREE;
	private static List<String> stateNames = null;

	private static Integer TASK_TIME = 0;
	private static Integer NUM_DIVIDE = 0;
	private static String INFER_ENGINE = null;
	private static String QUERY = null;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		/////////////////////////////

		// Fault Tree Created

		/////////////////////////////

		ComponentInstance target;
		InstanceObject object = getTarget(HandlerUtil.getCurrentSelection(event));
		if (object == null) {
			Dialog.showInfo("Bayesian Networks Analysis", "Please choose an instance model");
			return IStatus.ERROR;
		}
		SystemInstance si = object.getSystemInstance();
		if (object instanceof ComponentInstance) {
			target = (ComponentInstance) object;
		} else {
			target = si;
		}

		if (!EMV2Util.hasComponentErrorBehaviorTransitions(target) && !EMV2Util.hasCompositeErrorBehavior(target)
				&& !EMV2Util.hasOutgoingPropagations(target)) {
			Dialog.showInfo("Bayesian Networks Analysis",
					"Your system instance or selected component instance must have outgoing propagations, error state transitions, or composite error states.");
			return IStatus.ERROR;
		}
		stateNames = new ArrayList<String>();
		for (ErrorPropagation outprop : EMV2Util.getAllOutgoingErrorPropagations(target.getComponentClassifier())) {
			EList<TypeToken> result = EMV2TypeSetUtil.flattenTypesetElements(outprop.getTypeSet());
			for (TypeToken tt : result) {
				String epName = CreateFTAModel.prefixOutgoingPropagation + EMV2Util.getPrintName(outprop)
						+ EMV2Util.getPrintName(tt);
				if (!stateNames.contains(epName)) {
					stateNames.add(epName);
				}
			}
		}
		Collection<ErrorBehaviorState> states = EMV2Util.getAllErrorBehaviorStates(target);
		if (!states.isEmpty()) {
			ErrorBehaviorState head = states.iterator().next();
			for (ErrorBehaviorState ebs : EMV2Util.getAllErrorBehaviorStates(target)) {
				if (ebs != head) {
					stateNames.add(CreateFTAModel.prefixState + EMV2Util.getPrintName(ebs));
				}
			}
			stateNames.add(CreateFTAModel.prefixState + EMV2Util.getPrintName(head));
		}
		if (stateNames.isEmpty()) {
			Dialog.showInfo("Bayesian Networks Analysis",
					"Selected system must have error states or error propagations");
			return IStatus.ERROR;
		}

		final Display d = PlatformUI.getWorkbench().getDisplay();
		d.syncExec(() -> {
			IWorkbenchWindow window;
			Shell sh;

			window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			sh = window.getShell();

			DTBNDialog diag = new DTBNDialog(sh);
			diag.setValues(stateNames);
			diag.setTarget("'"
					+ (target instanceof SystemInstance ? target.getName() : target.getComponentInstancePath()) + "'");
			diag.open();
			ERROR_STATE_NAME = diag.getValue();
			FAULT_TREE_TYPE = FaultTreeType.FAULT_TREE;

			TASK_TIME = diag.getTaskTime();
			NUM_DIVIDE = diag.getNumDivide();
			INFER_ENGINE = diag.getInferEngine();
			QUERY = diag.getQuery();
		});

		if (ERROR_STATE_NAME != null) {
			if (FAULT_TREE_TYPE.equals(FaultTreeType.COMPOSITE_PARTS)
					&& ERROR_STATE_NAME.startsWith(CreateFTAModel.prefixOutgoingPropagation)) {
				Dialog.showInfo("Bayesian Networks Analysis", "Select error state for composite parts fault tree");
				return IStatus.ERROR;
			}
			if (FAULT_TREE_TYPE.equals(FaultTreeType.COMPOSITE_PARTS) && !EMV2Util.hasCompositeErrorBehavior(target)) {
				Dialog.showInfo("Bayesian Networks Analysis",
						"Selected system must have composite error states for composite parts fault tree analysis");
				return IStatus.ERROR;
			}

			FaultTree ftmodel = CreateFTAModel.createModel(target, ERROR_STATE_NAME, FAULT_TREE_TYPE);

			if (ftmodel == null) {
				Dialog.showInfo("Bayesian Networks Analysis",
						"No fault tree generated. Selected error propagation has no out propagation condition or path from an inner component");
				return IStatus.ERROR;
			}

			saveFaultTree(ftmodel);

			SiriusUtil.INSTANCE.autoOpenModel(ftmodel, ResourceUtil.getFile(si.eResource()).getProject(),
					"viewpoint:/org.osate.aadl2.errormodel.faulttree.design/FaultTree", "FaultTreeTable", "Fault Tree");

			/////////////////////////////

			// Bayesian Networks Analysis

			/////////////////////////////

			DTBN ftNet = ModelToDTBN.ftaToDTBN(ftmodel, TASK_TIME, NUM_DIVIDE);

			try {
				IPath graphPath = getReportPath(target, "DTBN", "png");
				IFile graphFile = ResourcesPlugin.getWorkspace().getRoot().getFile(graphPath);
				String fullPath = graphFile.getRawLocation().removeFileExtension().toOSString();
				Common.drawGraph(ftNet, fullPath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			DTBNInferer inferer = new DTBNInferer(ftNet, INFER_ENGINE);

			String lines[] = QUERY.split("\\r?\\n");
			String result = "";

			for (int i = 0; i < lines.length; i++) {
				String line = lines[i];
				if (!line.equals("")) {
					if (line.startsWith("#")) {
						continue;
					} else if (line.startsWith("P(")) {
						result += normalQuery(inferer, line); // normal query
					} else if (line.startsWith("MPC=?")) {
						result += mpcQuery(ftNet, inferer, line); // MPC query
					} else if (line.startsWith("tRAW=?")) {
						result += rawQuery(ftNet, inferer, line); // RAW query
					} else {
						throw new RuntimeException("Query is illegal !");
					}
				}
			}

			// write report
			try {
				writeReport(target, result);
			} catch (WriteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return Status.OK_STATUS;
		}
		return IStatus.ERROR;
	}

	private static String normalQuery(DTBNInferer inferer, String query) {
		// query & timed
		double start = System.currentTimeMillis();
		double[] results = inferer.query(query);
		double useTime = System.currentTimeMillis() - start;

		StringBuilder answer = new StringBuilder("Query -> " + query + ":\n");
		if (results.length <= 1) {
			answer.append(Arrays.toString(results)).append("\n");
		} else {
			answer.append("Probability density: ").append(Arrays.toString(results)).append("\n");

			// compute cumulative distribution
			double[] cum = new double[results.length];
			cum[0] = results[0];
			for (int i = 1; i < results.length; i++) {
				cum[i] = cum[i-1] + results[i];
			}

			answer.append("Cumulative distribution: ").append(Arrays.toString(cum)).append("\n");
		}

		answer.append("use ").append(useTime).append(" ms").append("\n\n");
		return answer.toString();
	}

	private static String mpcQuery(DTBN ftNet, DTBNInferer inferer, String query) {
		String completeQuery = query;
		query = query.replaceAll("MPC=\\?", "").replaceAll("\\[", "").replaceAll("]", "").replaceAll("\\s+", "");

		// query & timed
		double start = System.currentTimeMillis();
		double useTime;

		StringBuilder answer = new StringBuilder("Query -> " + completeQuery + ":\n");

		String rootNode = ftNet.getRootNodeName();
		String template = "P(%s't=*|%s't=%d)";
		Map<String, List<Double>> targetNodeOcrProb = new HashMap<>();

		String[] targetNode = query.split(",");
		for (String node : targetNode) {
			List<Double> criList = new ArrayList<>();
			for (int i = 1; i < ftNet.getNumTimeSlice(); i++) {
				double[] occurProb = inferer.query(String.format(template, node, rootNode, i));

				double cum = 0.0;
				for (int j = 0; j <= i - 1; j++) {
					cum += occurProb[j];
				}

				criList.add(cum);
			}
			targetNodeOcrProb.put(node, criList);
		}
		useTime = System.currentTimeMillis() - start;

		for (Map.Entry<String, List<Double>> entry : targetNodeOcrProb.entrySet()) {
			answer.append(entry.getKey()).append("\t=> ").append(entry.getValue()).append("\n");
		}

		answer.append("use ").append(useTime).append(" ms").append("\n\n");
		return answer.toString();
	}

	private static String rawQuery(DTBN ftNet, DTBNInferer inferer, String query) {
		String completeQuery = query;
		query = query.replaceAll("tRAW=\\?", "").replaceAll("\\[", "").replaceAll("]", "").replaceAll("\\s+", "");

		// query & timed
		double start = System.currentTimeMillis();
		double useTime;

		StringBuilder answer = new StringBuilder("Query -> " + completeQuery + ":\n");

		String rootNode = ftNet.getRootNodeName();
		String normalTemplate = "P(%s't=*)";
		String occurTemplate = "P(%s't=%d|%s't=%d)";
		Map<String, List<Double>> raw = new HashMap<>();

		double[] normalProb = inferer.query(String.format(normalTemplate, rootNode));

		String[] targetNode = query.split(",");
		for (String node : targetNode) {

			List<Double> deltaList = new ArrayList<>();
			for (int i = 1; i < ftNet.getNumTimeSlice(); i++) {
				double occur = inferer.query(String.format(occurTemplate, rootNode, i, node, i))[0];
				deltaList.add(occur - normalProb[i - 1]);
			}
			raw.put(node, deltaList);
		}
		useTime = System.currentTimeMillis() - start;

		for (Map.Entry<String, List<Double>> entry : raw.entrySet()) {
			answer.append(entry.getKey()).append("\t=> ").append(entry.getValue()).append("\n");
		}

		answer.append("use ").append(useTime).append(" ms").append("\n\n");
		return answer.toString();
	}

	/**
	 * Write report to hard disk
	 * @throws IOException, WriteException, CoreException
	**/
	public static void writeReport(EObject target, String result) throws IOException, WriteException, CoreException {
		// set BOM to support zh_cn
		String content = "\ufeff";

		// get report content
		content += result;

		// get report path
		IPath path = getReportPath(target, "query", "txt");

		// write report
		if (path != null) {
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			if (file != null) {
				final InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
				if (file.exists()) {
					file.setContents(input, true, true, null);
				} else {
					AadlUtil.makeSureFoldersExist(path);
					file.create(input, true, null);
				}
			}
		}
	}

	/**
	 * Get the save path of report file
	**/
	public static IPath getReportPath(EObject root, String filename, String fileExtension) {
		Resource res = root.eResource();
		URI uri = res.getURI();
		IPath path = OsateResourceUtil.toIFile(uri).getFullPath();

		path = path.removeFileExtension();
		filename = path.lastSegment() + "_" + filename;
		path = path.removeLastSegments(1).append("/reports/bayes/" + filename);
		path = path.addFileExtension(fileExtension);
		return path;
	}

	private InstanceObject getTarget(ISelection currentSelection) {
		if (currentSelection instanceof IStructuredSelection) {
			IStructuredSelection iss = (IStructuredSelection) currentSelection;
			if (iss.size() == 1) {
				Object obj = iss.getFirstElement();
				if (obj instanceof InstanceObject) {
					return (InstanceObject) obj;
				}
				if (obj instanceof EObjectURIWrapper) {
					EObject eObject = new ResourceSetImpl().getEObject(((EObjectURIWrapper) obj).getUri(), true);
					if (eObject instanceof InstanceObject) {
						return (InstanceObject) eObject;
					}
				}
				if (obj instanceof IFile) {
					URI uri = OsateResourceUtil.toResourceURI((IFile) obj);
					Resource res = new ResourceSetImpl().getResource(uri, true);
					EList<EObject> rl = res.getContents();
					if (!rl.isEmpty()) {
						return (InstanceObject) rl.get(0);
					}
				}
			}
		}
		return null;
	}

	public URI saveFaultTree(FaultTree ftamodel) {
		URI ftaURI = EcoreUtil.getURI(ftamodel.getInstanceRoot())
				.trimFragment()
				.trimFileExtension()
				.trimSegments(1)
				.appendSegment("reports")
				.appendSegment("fta")
				.appendSegment(ftamodel.getName())
				.appendFileExtension("faulttree");
		AadlUtil.makeSureFoldersExist(new Path(ftaURI.toPlatformString(true)));
		Resource res = ftamodel.getInstanceRoot().eResource().getResourceSet().createResource(ftaURI);
		res.getContents().add(ftamodel);
		try {
			res.save(null);
		} catch (IOException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
		}
		return EcoreUtil.getURI(ftamodel);
	}

}