package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.messages"; //$NON-NLS-1$
	public static String ForLoopRefactoring_LoopNotZeroBased;
	public static String ForLoopRefactoring_SelectAForLoop;
	public static String NullRefactoring_SelectedLoopInfo;
	public static String PragmaDirectiveRefactoring_SelectAPragma;
	public static String PragmaDirectiveRefactoring_SelectedPragmaInfo;
	public static String Refactoring_AnalyzingSelection;
	public static String Refactoring_CalculatingModifications;
	public static String Refactoring_CheckingInitialConditions;
	public static String Refactoring_DeterminingIfSafe;
	public static String Refactoring_DoneChckingInitialConditions;
	public static String Refactoring_InvalidSelection;
	public static String Refactoring_NoIndex;
	public static String Refactoring_WaitingForIndexer;
	public static String StatementsRefactoring_DeterminingIfSafe;
	public static String StatementsRefactoring_NoStatementsToRefactor;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
