package edu.auburn.oaccrefac.core.transformations;

import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import edu.auburn.oaccrefac.core.dataflow.ReachingDefinitions;
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
//        Set<String> copyin = new TreeSet<String>();
//        Set<String> copyout = new TreeSet<String>();
        int replStart = getStatement().getFileLocation().getNodeOffset();
        int replEnd = replStart + getStatement().getFileLocation().getNodeLength();
        String newStatement = decompound(getStatement().getRawSignature());

/*
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
*/
        
        //expand upward until adding the statement adds another definition that reaches into the construct (conservative, but could enlarge copyin/copyout)
        //or until the added statement is a variable declaration (could break scoping if definition reaches statement outside construct) 
        IASTNode currentStatement = getStatement();
        do {
            currentStatement = ASTUtil.getPreviousSibling(currentStatement);
            if(currentStatement == null) break;
            if(currentStatement instanceof IASTDeclarationStatement) break; 
            
            Set<IASTNode> defsReachingAddedStatement = rd.reachingDefinitions(currentStatement);
            
            //this should actually include defs reaching any statements "added" on 
            //previous iterations, but we only add if there are no extra reaching defs, 
            //so this set is sufficient
            Set<IASTNode> defsReachingConstruct = rd.reachingDefinitions(getStatement());
            //i.e., if the added statement isn't reached by any extra definitions
            if(defsReachingConstruct.containsAll(defsReachingAddedStatement)) {
            //and if currentStatement isnt a declaration that reaches outside the statement    
               replStart = currentStatement.getFileLocation().getNodeOffset(); 
               newStatement = currentStatement.getRawSignature() + System.lineSeparator() + newStatement;
            }
            else {
                //we've added a definition, so we stop expanding
                /* TODO actually, we need to make sure that we aren't adding a definition
                 * of a variable that is already being copied in; if we are, expansion can 
                 * continue */
                break;
            }
            
        } while(true);
        
        //expand downward until the copy sets change/may change; see all TODO comments above
        currentStatement = getStatement();
        do {
            currentStatement = ASTUtil.getNextSibling(currentStatement);
            if(currentStatement == null) break;
            if(currentStatement instanceof IASTDeclarationStatement) break; 
            
            Set<IASTNode> defsReachingAddedStatement = rd.reachingDefinitions(currentStatement);
            Set<IASTNode> defsReachingConstruct = rd.reachingDefinitions(getStatement());

            if (defsReachingConstruct.containsAll(defsReachingAddedStatement)) {
                replEnd = currentStatement.getFileLocation().getNodeOffset() + currentStatement.getFileLocation().getNodeLength();
                newStatement = newStatement + System.lineSeparator() + currentStatement.getRawSignature();
            }
            else {
                break;
            }
            
        } while(true);
        
        String newPragma = getPragma().getRawSignature();
        newStatement = newPragma + System.lineSeparator() + compound(newStatement);
        
        int realStart = replStart < getPragma().getFileLocation().getNodeOffset() ? replStart : getPragma().getFileLocation().getNodeOffset();
        this.replace(realStart, replEnd - realStart, newStatement);
        finalizeChanges();
//        //TODO remove existing pragma, adjust copy sets, replace compilable code with new construct, build new pragma and add it in  
//        
//        this.remove(getPragma());
//        //with current "expansion policy" there is no need to adjust copy sets
//        //similarly, the new pragma is the same as the old one
//        this.replace(replStart, replEnd - replStart, compound(newStatement));
//        this.insert(replStart, getPragma().getRawSignature() + System.lineSeparator());
//        finalizeChanges();
        
    }
    
}
