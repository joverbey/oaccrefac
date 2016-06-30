package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ptp.pldt.openacc.core.transformations.messages"; //$NON-NLS-1$
	public static String ForLoopCheck_CannotAnalyzeDependences;
	public static String ForLoopCheck_CannotRefactor;
	public static String FuseLoopsCheck_CannotFuseDifferentPragmas;
	public static String FuseLoopsCheck_DefinitionsMayConflict;
	public static String FuseLoopsCheck_DependencesNotAnalyzed;
	public static String FuseLoopsCheck_FusionCreatesDependence;
	public static String FuseLoopsCheck_MustBeTwoLoops;
	public static String IntroOpenACCLoopCheck_CannotParallelizeCarriesDependence;
	public static String IntroOpenACCLoopCheck_KernelsCannotInParallelRegion;
	public static String IntroOpenACCLoopCheck_ParallelCannotInKernelsRegion;
	public static String IntroOpenACCLoopCheck_PragmaCannotBeAddedHasPragma;
	public static String IntroRoutineCheck_CannotFindFunctionDefinition;
	public static String LoopCuttingCheck_CannotCutCarriesDependence;
	public static String LoopCuttingCheck_DivisibleByIterationFactor;
	public static String LoopCuttingCheck_FactorMustBeGreater;
	public static String LoopCuttingCheck_InvalidCutFactor;
	public static String LoopCuttingCheck_NameAlreadyExists;
	public static String StripMineCheck_FactorMustBeGreaterAndDivisible;
	public static String StripMineCheck_InnerNameAlreadyExists;
	public static String StripMineCheck_InvalidStripFactor;
	public static String StripMineCheck_OuterNameAlreadyExists;
	public static String StripMineCheck_OverflowWillOccur;
	public static String TileLoopsCheck_HeightMustBe;
	public static String TileLoopsCheck_LoopContainsPragma;
	public static String TileLoopsCheck_MustBeTwoLoops;
	public static String TileLoopsCheck_OnlyPerfectlyNestedLoops;
	public static String TileLoopsCheck_WidthMustBe;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
