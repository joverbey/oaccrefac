package org.eclipse.ptp.pldt.openacc.core.dependence;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ptp.pldt.openacc.core.dependence.messages"; //$NON-NLS-1$
	public static String AddressTakenAnalysis_PointsToAnalysis;
	public static String DependenceAnalysis_AnalyzingDependences;
	public static String DependenceAnalysis_AnalyzingLine;
	public static String DependenceAnalysis_DependenceTestCancelled;
	public static String DependenceAnalysis_FindingVariableAccesses;
	public static String DependenceAnalysis_LoopCannotBeAnalyzed;
	public static String DependenceAnalysis_UnsupportedConstruct;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
