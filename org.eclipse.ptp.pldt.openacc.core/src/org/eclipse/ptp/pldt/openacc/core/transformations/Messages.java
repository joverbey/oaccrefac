package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ptp.pldt.openacc.core.transformations.messages"; //$NON-NLS-1$
	public static String AbstractTileLoopsCheck_LoopFormNotSupported;
	public static String DistributeLoopsCheck_BodyNotCompound;
	public static String DistributeLoopsCheck_IsolatesDeclarationStatement;
	public static String DistributeLoopsCheck_LoopCarriesDependence;
	public static String DistributeLoopsCheck_LoopIsAccConstruct;
	public static String DistributeLoopsCheck_OnlyOneStatement;
	public static String Expand_Above;
	public static String Expand_Below;
	public static String Expand_ConstructWillNotIncludeBadCtlFlowStatements;
	public static String Expand_ConstructWillNotIncludeOtherOACCConstruct;
	public static String Expand_ConstructWillNotIncludeStatementToChangeDataTransfer;
	public static String ExpandDataConstruct_CopyIdentifierWillBeRemoved;
	public static String ExpandDataConstruct_CopyinIdentifierWillBeRemoved;
	public static String ExpandDataConstruct_CopyoutIdentifierWillBeRemoved;
	public static String ExpandDataConstruct_CreateIdentifierWillBeRemoved;
	public static String ExpandDataConstructCheck_MustBeDataConstruct;
	public static String ExpandDataConstructCheck_PromoteDataTransferProblemIndexVar;
	public static String ExpandDataConstructCheck_PromoteDataTransferProblemNonIndexVar;
	public static String ForLoopCheck_CannotAnalyzeDependences;
	public static String ForLoopCheck_CannotRefactor;
	public static String FuseLoopsCheck_CannotFuseDifferentPragmas;
	public static String FuseLoopsCheck_DefinitionsMayConflict;
	public static String FuseLoopsCheck_DependencesNotAnalyzed;
	public static String FuseLoopsCheck_FusionCreatesDependence;
	public static String FuseLoopsCheck_MustBeTwoLoops;
	public static String InterchangeLoopsCheck_Depth;
	public static String InterchangeLoopsCheck_InterchangingSelectedLoopWith;
	public static String InterchangeLoopsCheck_NoForLoopAtNestDepth;
	public static String InterchangeLoopsCheck_OfLoopNest;
	public static String InterchangeLoopsCheck_OnlyPerfectlyNested;
	public static String InterchangeLoopsCheck_PerfectlyNestedLoopHeadersOfFirst;
	public static String InterchangeLoopsCheck_Period;
	public static String InterchangeLoopsCheck_SecondForMustBeWithin;
	public static String InterchangeLoopsCheck_WillChangeDependenceStructure;
	public static String IntroAtomicCheck_MustBeExpression;
	public static String IntroAtomicCheck_MustBeInsideForLoop;
	public static String IntroAtomicCheck_NoStatementsSelected;
	public static String IntroAtomicCheck_NoSuitableAtomicTypeFound;
	public static String IntroAtomicCheck_NotInParallelRegion;
	public static String IntroDataConstructCheck_ConditionalWriteMayCauseIncorrectDataTransfer;
	public static String IntroDataConstructCheck_MustBeInConditionalOrSurroundIfAndElse;
	public static String IntroDataConstructCheck_NoDataTransfer;
	public static String IntroDataConstructCheck_WillContainBadBreak;
	public static String IntroDataConstructCheck_WillContainBadContinue;
	public static String IntroDataConstructCheck_WillContainGotoStatement;
	public static String IntroDataConstructCheck_WillContainReturnStatement;
	public static String IntroDataConstructCheck_WillNotSurroundAnyStatements;
	public static String IntroDataConstructCheck_WouldSurroundDeclarationScopeErrors;
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
	public static String MergeDataConstructsCheck_MustBeDataConstruct;
	public static String MergeDataConstructsCheck_MustBeFollowedByDataConstruct;
	public static String MergeDataConstructsCheck_ShouldBeCompoundStatements;
	public static String MergeDataConstructsCheck_VariableShadowingMayOccur;
	public static String NullCheck_DependencesCouldNotBeAnalyzed;
	public static String NullCheck_LoopContainsUnsupportedStatement;
	public static String NullCheck_LoopFormNotSupported;
	public static String NullCheck_LoopIsPerfectNest;
	public static String NullCheck_LoopUpperBound;
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
	public static String UnrollLoopCheck_CannotDetermineLowerBound;
	public static String UnrollLoopCheck_CannotDetermineUpperBound;
	public static String UnrollLoopCheck_CantDetermineLowerBound;
	public static String UnrollLoopCheck_CantDetermineUpperBound;
	public static String UnrollLoopCheck_CantUnrollMoreTimesThanLoopRuns;
	public static String UnrollLoopCheck_IndexVariableChangedInBody;
	public static String UnrollLoopCheck_InvalidLoopUnrollingFactor;
	public static String UnrollLoopCheck_InvalidUnrollFactor;
	public static String UnrollLoopCheck_LoopBodyIsEmpty;
	public static String UnrollLoopCheck_LoopContainsUnsupported;
	public static String UnrollLoopCheck_LoopContainsUnsupportedStatement;
	public static String UnrollLoopCheck_LoopFormNotSupported;
	public static String UnrollLoopCheck_LoopIndexVariableChanged;
	public static String UnrollLoopCheck_NothingToUnroll;
	public static String UnrollLoopCheck_TooManyTimes;
	public static String UnrollLoopCheck_UpperBoundIsNotConstant;
	public static String UnrollLoopCheck_UpperBoundNotConstant;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
