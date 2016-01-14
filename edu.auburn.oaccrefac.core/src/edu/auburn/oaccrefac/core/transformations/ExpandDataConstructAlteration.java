package edu.auburn.oaccrefac.core.transformations;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;

import edu.auburn.oaccrefac.core.dataflow.ReachingDefinitions;
import edu.auburn.oaccrefac.core.parser.ASTAccCopyinClauseNode;
import edu.auburn.oaccrefac.core.parser.ASTAccCopyoutClauseNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataClauseListNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataItemNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataNode;
import edu.auburn.oaccrefac.core.parser.IAccConstruct;
import edu.auburn.oaccrefac.core.parser.OpenACCParser;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class ExpandDataConstructAlteration extends PragmaDirectiveAlteration<ExpandDataConstructCheck> {

    public ExpandDataConstructAlteration(IASTRewrite rewriter, ExpandDataConstructCheck check) {
        super(rewriter, check);
    }

    /* 
    *   expanding upward can change the copyin set AND the copyout set
    *     copyin if pulling in a use reached by an outer definition that did not previously reach into the construct
    *     copyout if pulling in a new definition that reaches outside the construct
    *   if either of these conditions for changing a set occurs, stop the expansion
    * 
    *   expanding downward can similarly change both
    *     copyin if pulling in a use reached by an outer definition that did not previously reach into the construct
    *     copyout if pulling in a new definition that reaches outside the construct
    *   again, if either of these for changing a set occurs, stop the expansion
    */ 
    
    @Override
    protected void doChange() throws Exception {
        ReachingDefinitions rd = new ReachingDefinitions(ASTUtil.findNearestAncestor(getStatement(), IASTFunctionDefinition.class));

        Set<String> copyin = new TreeSet<String>();
        Set<String> copyout = new TreeSet<String>();
        
        IAccConstruct construct = new OpenACCParser().parse(getPragma().getRawSignature());
        //TODO do a check in the check class for the type of the node?
        if(!(construct instanceof ASTAccDataNode)) {
            throw new IllegalStateException("The pragma must be an acc data construct");
        }
        ASTAccDataNode data = (ASTAccDataNode) construct;
        
        for(ASTAccDataClauseListNode listNode : data.getAccDataClauseList()) {
            if(listNode.getAccDataClause() instanceof ASTAccCopyinClauseNode) {
                ASTAccCopyinClauseNode copyinClause = (ASTAccCopyinClauseNode) listNode.getAccDataClause();
                for(ASTAccDataItemNode var : copyinClause.getAccDataList()) {
                    copyin.add(var.getIdentifier().getIdentifier().getText());
                }
            }
            else if(listNode.getAccDataClause() instanceof ASTAccCopyoutClauseNode) {
                ASTAccCopyoutClauseNode copyoutClause = (ASTAccCopyoutClauseNode) listNode.getAccDataClause();
                for(ASTAccDataItemNode var : copyoutClause.getAccDataList()) {
                    copyout.add(var.getIdentifier().getIdentifier().getText());
                }
            }
        }
        
        
    }
    
    private class Expansion {
        
        //number of statements in original construct
        private int origSize;
        
        private int startOffset;
        private int endOffset;

        //number of statements above the original construct that this expansion includes
        private int upSize;
        
        //number of statements below the original construct that this expansion includes
        private int downSize;
        private Set<String> copyin;
        private Set<String> copyout;
        
        public Expansion() {
            
        }
        
    }
    
}
