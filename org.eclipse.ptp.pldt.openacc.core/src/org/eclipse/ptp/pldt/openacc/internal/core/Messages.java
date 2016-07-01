package org.eclipse.ptp.pldt.openacc.internal.core;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ptp.pldt.openacc.internal.core.messages"; //$NON-NLS-1$
	public static String FunctionNode_CannotContainStaticVariables;
	public static String FunctionNode_CannotFindFunctionDefinitions;
	public static String FunctionNode_CannotParsePreprocessorStatement;
	public static String FunctionNode_Children;
	public static String FunctionNode_InconsistentCallLevels;
	public static String FunctionNode_InconsistentLevelsofParallelism;
	public static String FunctionNode_Level;
	public static String FunctionNode_Null;
	public static String FunctionNode_RecursiveMustBeSequential;
	public static String FunctionNode_Root;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
