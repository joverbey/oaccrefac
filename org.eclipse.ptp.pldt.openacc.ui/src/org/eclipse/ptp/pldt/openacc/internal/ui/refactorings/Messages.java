package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.messages"; //$NON-NLS-1$
	public static String StatementsRefactoring_CalculatingModifications;
	public static String StatementsRefactoring_CheckingInitialConditions;
	public static String StatementsRefactoring_DeterminingIfSafe;
	public static String StatementsRefactoring_NoStatementsToRefactor;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
