package org.eclipse.ptp.pldt.openacc.internal.ui.actions.refactoring;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ptp.pldt.openacc.internal.ui.actions.refactoring.messages"; //$NON-NLS-1$
	public static String FuseLoopsDelegate_FuseLoops;
	public static String IntroOpenACCLoopDelegate_IntroduceOpenACCLoop;
	public static String IntroOpenACCLoopDelegate_Kernels;
	public static String IntroRoutineDelegate_IntroduceOpenACCRoutine;
	public static String StripMineLoopDelegate_HandleOverflow;
	public static String StripMineLoopDelegate_InnerIndexVariableName;
	public static String StripMineLoopDelegate_OuterIndexVariableName;
	public static String StripMineLoopDelegate_StripMineLoop;
	public static String StripMineLoopDelegate_StripSize;
	public static String StripMineLoopDelegate_ZeroBased;
	public static String TileLoopsDelegate_CutFactor;
	public static String TileLoopsDelegate_IndexVariableName;
	public static String TileLoopsDelegate_InnerIndexVariable;
	public static String TileLoopsDelegate_OuterIndexVariable;
	public static String TileLoopsDelegate_TileHeight;
	public static String TileLoopsDelegate_TileLoops;
	public static String TileLoopsDelegate_TileWidth;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
