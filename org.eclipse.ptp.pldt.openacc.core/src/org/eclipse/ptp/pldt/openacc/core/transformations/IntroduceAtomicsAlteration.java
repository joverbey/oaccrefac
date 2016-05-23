package org.eclipse.ptp.pldt.openacc.core.transformations;

public class IntroduceAtomicsAlteration extends SourceStatementsAlteration<IntroAtomicsCheck> {

	private int type;
	
    public IntroduceAtomicsAlteration(IASTRewrite rewriter, IntroAtomicsCheck check) {
        super(rewriter, check);
        type = check.getType();
    }

	@Override
	protected void doChange() throws Exception {
		int offset = getStatements()[0].getFileLocation().getNodeOffset();
		String pragma = "#pragma acc atomic ";
		switch (type) {
		case IntroAtomicsCheck.READ:
			pragma += "read";
			break;
		case IntroAtomicsCheck.WRITE:
			pragma += "write";
			break;
		case IntroAtomicsCheck.UPDATE:
			pragma += "update";
			break;
		case IntroAtomicsCheck.NONE:
			return;
		}
		insert(offset, pragma + System.lineSeparator());
		finalizeChanges();
	}
}
