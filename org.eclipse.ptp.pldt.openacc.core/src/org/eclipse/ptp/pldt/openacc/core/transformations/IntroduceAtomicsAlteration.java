package org.eclipse.ptp.pldt.openacc.core.transformations;

public class IntroduceAtomicsAlteration extends SourceStatementsAlteration<IntroduceAtomicsCheck> {

	private int type;
	
    public IntroduceAtomicsAlteration(IASTRewrite rewriter, IntroduceAtomicsCheck check) {
        super(rewriter, check);
        type = check.getType();
    }

	@Override
	protected void doChange() throws Exception {
		int offset = getStatements()[0].getFileLocation().getNodeOffset();
		String pragma = "#pragma acc atomic ";
		switch (type) {
		case IntroduceAtomicsCheck.READ:
			pragma += "read";
			break;
		case IntroduceAtomicsCheck.WRITE:
			pragma += "write";
			break;
		case IntroduceAtomicsCheck.UPDATE:
			pragma += "update";
			break;
		case IntroduceAtomicsCheck.NONE:
			return;
		}
		insert(offset, pragma + System.lineSeparator());
		finalizeChanges();
	}
}
