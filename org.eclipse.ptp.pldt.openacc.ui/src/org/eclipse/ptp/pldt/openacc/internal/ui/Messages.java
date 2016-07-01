package org.eclipse.ptp.pldt.openacc.internal.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ptp.pldt.openacc.internal.ui.messages"; //$NON-NLS-1$
	public static String ColumnedLoopRefactoringWizardPage_Cutting;
	public static String ColumnedLoopRefactoringWizardPage_Tiling;
	public static String LoopRefactoringWizardPage_Parallel;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
