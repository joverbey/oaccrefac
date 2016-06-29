import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "messages"; //$NON-NLS-1$
	public static String Main_InternalErrorCreatingChange;
	public static String Main_NoApplicableStatement;
	public static String Main_PleaseAddComment;
	public static String Main_RefactoringIsInvalid;
	public static String Main_UnableToParse;
	public static String Main_Usage;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
