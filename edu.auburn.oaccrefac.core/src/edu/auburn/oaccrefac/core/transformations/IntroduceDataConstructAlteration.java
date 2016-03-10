package edu.auburn.oaccrefac.core.transformations;

import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

import edu.auburn.oaccrefac.core.dataflow.ReachingDefinitions;
import edu.auburn.oaccrefac.core.parser.IAccConstruct;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.OpenACCUtil;

public class IntroduceDataConstructAlteration extends SourceStatementsAlteration<IntroduceDataConstructCheck> {

    public IntroduceDataConstructAlteration(IASTRewrite rewriter, IntroduceDataConstructCheck check) {
        super(rewriter, check);
    }

    @Override
    protected void doChange() throws Exception {
        IASTStatement[] stmts = getStatements();
        ReachingDefinitions rd = new ReachingDefinitions(ASTUtil.findNearestAncestor(stmts[0], IASTFunctionDefinition.class));
        Set<String> copyin = OpenACCUtil.inferCopyin(rd, getStatements());
        Set<String> copyout = OpenACCUtil.inferCopyout(rd, getStatements());

        //TODO remove vars in copyin/copyout from the existing copyin/out sets
        //also modify existing create set as necessary
        
        String origRegion = "";
        for (IASTNode node : getStatementsAndComments()) {
            origRegion += node.getRawSignature();
        }
        StringBuilder replacement = new StringBuilder();
        replacement.append(pragma("acc data"));
        if(!copyin.isEmpty()) {
            replacement.append(" ");
            replacement.append(copyin(copyin.toArray(new String[copyin.size()])));
        }
        if(!copyout.isEmpty()) {
            replacement.append(" ");
            replacement.append(copyout(copyout.toArray(new String[copyout.size()])));
        }
        replacement.append(System.lineSeparator());
        replacement.append(compound(decompound(origRegion)));
        this.replace(getOffset(), getLength(), replacement.toString());
        finalizeChanges();
    }

}
