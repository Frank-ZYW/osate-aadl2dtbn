package org.osate2.bayes.dtbn.Handler;

import java.util.List;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DTBNDialog extends TitleAreaDialog {

	private String value;
	private List<String> values;
	private Combo errorMode;

	private Integer ttValue = 0;
	private Text taskTime;

	private Integer ndValue = 1;
	private Text numDivide;

	private String ieValue;
	private String[] ieValues = { "jtree", "lbp" };
	private Combo inferEngine;

	private String qValue = "";
	private Text query;

	private String target = "";

	public DTBNDialog(Shell parentShell) {
		super(parentShell);
	}

	public void setValues(java.util.List<String> v) {
		values = v;
	}

	public void setTarget(String targetname) {
		target = targetname;
	}

	@Override
	public void create() {
		super.create();
		setTitle("Fault-Tree Analysis" + (target.isEmpty() ? "" : " for " + target));
		setMessage("Select error state for Parts Fault Tree, out propagation or state for flow based fault tree.",
				IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);

		//////////////// line 1

		Composite line1 = new Composite(area, SWT.NONE);
		GridLayout layout1 = new GridLayout(2, false);

		line1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		line1.setLayout(layout1);

		Label labelErrorMode = new Label(line1, SWT.NONE);
		labelErrorMode.setText("Select Error State or Outgoing Propagation");

		errorMode = new Combo(line1, SWT.READ_ONLY | SWT.BORDER);
		String val[] = new String[values.size()];
		for (int i = 0; i < values.size(); i++) {
			val[i] = values.get(i);
		}
		errorMode.setItems(val);
		errorMode.select(0);

		//////////////// line 2

		Composite line2 = new Composite(area, SWT.NONE);
		GridLayout layout2 = new GridLayout(6, false);

		line2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		line2.setLayout(layout2);

		Label labelTaskTime = new Label(line2, SWT.NONE);
		labelTaskTime.setText("Task Time (hour)");
		taskTime = new Text(line2, SWT.SINGLE | SWT.BORDER | SWT.RIGHT);

		Label labelNumDivide = new Label(line2, SWT.NONE);
		labelNumDivide.setText("Number of time slices");
		numDivide = new Text(line2, SWT.SINGLE | SWT.BORDER | SWT.RIGHT);

		Label labelInfer = new Label(line2, SWT.NONE);
		labelInfer.setText("Choose DTBN infer engine");
		inferEngine = new Combo(line2, SWT.READ_ONLY | SWT.BORDER);
		inferEngine.setItems(ieValues);
		inferEngine.select(0);

		////////////////////// line3

		Composite line3 = new Composite(area, SWT.NONE);
		GridLayout layout3 = new GridLayout(1, false);

		line3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		line3.setLayout(layout3);

		Label labelQuery = new Label(line3, SWT.NONE);
		labelQuery.setText("Input query by line");

		query = new Text(line3, SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
		query.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		return area;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	private void saveInput() {
		value = errorMode.getText();
		ttValue = Integer.parseInt(taskTime.getText());
		ndValue = Integer.parseInt(numDivide.getText());
		ieValue = inferEngine.getText();
		qValue = query.getText();
	}

	@Override
	protected void okPressed() {
		saveInput();
		super.okPressed();
	}

	public String getValue() {
		return value;
	}

	public Integer getTaskTime() {
		return ttValue;
	}

	public Integer getNumDivide() {
		return ndValue;
	}

	public String getInferEngine() {
		return ieValue;
	}

	public String getQuery() {
		return qValue;
	}

}
