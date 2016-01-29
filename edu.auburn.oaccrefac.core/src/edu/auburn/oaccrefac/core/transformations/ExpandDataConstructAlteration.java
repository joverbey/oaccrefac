package edu.auburn.oaccrefac.core.transformations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

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
        
        /*
        picks the largest expansion such that the copysize hasnt increased and values copied in and out havet changed
        */

        //using parent node
        int maxup = getMaxUp(getStatement());
        int maxdown = getMaxDown(getStatement());
        int ocopyinsize = copyin.size();
        int ocopyoutsize = copyout.size();
        int osize;
        if(getStatement() instanceof IASTCompoundStatement) {
            osize = ((IASTCompoundStatement) getStatement()).getStatements().length;
        }
        else {
            osize = 1;
        }
        List<Expansion> expansions = new ArrayList<Expansion>();
        Set<String> gpuvars = getGpuVars(getStatement());
        for(int i = 0; i < maxup; i++) {
            for(int j = 0; i < maxdown; j++) {
                IASTStatement expstart = getExpansionStart(i, getStatement());
                IASTStatement expend = getExpansionEnd(j, getStatement());
                Set<String> expcopyin = getCopyin(expstart, expend, rd);
                Set<String> expcopyout = getCopyout(expstart, expend, rd);
                Expansion exp = new Expansion(expstart, expend, osize + i + j, expcopyin, expcopyout); //num items in copyset for this expansion
                if(exp.getCopyin().size() <= ocopyinsize)
                if(exp.getCopyout().size() <= ocopyoutsize)
                if(areCopyinDefsTheSame(gpuvars, exp, getStatement(), rd)) //be sure set of defs reaching into the construct hasnt changed for variables in both the original and the expansion copyin sets
                if(areCopyoutDefsTheSame(gpuvars, exp, getStatement(), rd)) //be sure set of defs reaching out the construct hasnt changed for variables in both the original and the expansion copyout sets
                //if a var is used on the gpu inside the construct and was copied in, we cant remove it from the copyin set in the expansion
                if(isAIntersectedWithBASubsetOfC(gpuvars, copyin, expcopyin))
                //if a var is used on the gpu inside the construct and was not copied in, we cannot add it to the copyset
                //logically, this is equivalent to the contrapositive: 
                //if a var is used on the gpu inside the construct and is in the expansion's copyin set, it must be in the original copyin set as well 
                //i guess the real question is "is the intersection of A and B a subset of C?"
                if(isAIntersectedWithBASubsetOfC(gpuvars, expcopyin, copyin))
                    expansions.add(exp);
                //copyin.containsAll(new HashSet<String>(gpuvars).retainAll(expcopyin))
            }
        }
        //just picks the largest; could pick the largest with some comparison to copysize, or the one with the smallest copysize, etc
        Expansion largestexp = null;
        for(Expansion exp : expansions) {
           if(largestexp == null || exp.getSize() >= largestexp.getSize()) {
                largestexp = exp;
            }
        }
        
        //TODO we now have the expansion to use, so use the info stored in it to make the alteration to the code
        
    }
    
    private Set<String> getGpuVars(IASTStatement statement) {
        // TODO Auto-generated method stub
        return null;
    }

    private int getMaxDown(IASTStatement statement) {
        int i = 0;
        while(true) {
            IASTNode prev = ASTUtil.getPreviousSibling(statement);
            if(prev == null) {
                break;
            }
            //also break if we hit another acc construct
            if(prev instanceof IASTStatement) {
                String[] pragmas = ASTUtil.getPragmas((IASTStatement) prev);
                if(pragmas.length == 1) {
                    //TODO should we parse it instead?
                    if(pragmas[0].startsWith("#pragma acc")) {
                        break;
                    }
                }
            }
            i++;
        }
        return i;
    }

    private int getMaxUp(IASTStatement statement) {
        int i = 0;
        while(true) {
            IASTNode next = ASTUtil.getNextSibling(statement);
            if(next == null) {
                break;
            }
            //also break if we hit another acc construct
            if(next instanceof IASTStatement) {
                String[] pragmas = ASTUtil.getPragmas((IASTStatement) next);
                if(pragmas.length == 1) {
                    //TODO should we parse it instead?
                    if(pragmas[0].startsWith("#pragma acc")) {
                        break;
                    }
                }
            }
            i++;
        }
        return i;
    }

    private Set<String> getCopyout(IASTStatement expstart, IASTStatement expend, ReachingDefinitions rd) {
        // TODO Auto-generated method stub
        return null;
    }

    private Set<String> getCopyin(IASTStatement expstart, IASTStatement expend, ReachingDefinitions rd) {
        // TODO Auto-generated method stub
        return null;
    }

    private IASTStatement getExpansionEnd(int stmtsDown, IASTStatement original) {
        IASTStatement end = original;
        for(int i = 0; i < stmtsDown; i++) {
            //TODO potential typechecking issue
            end = (IASTStatement) ASTUtil.getNextSibling(original);
        }
        return end;
    }

    private IASTStatement getExpansionStart(int stmtsUp, IASTStatement original) {
        IASTStatement end = original;
        for(int i = 0; i < stmtsUp; i++) {
            //TODO potential typechecking issue
            end = (IASTStatement) ASTUtil.getPreviousSibling(original);
        }
        return end;
    }

    private boolean areCopyoutDefsTheSame(Set<String> gpuvars, Expansion exp, IASTStatement statement,
            ReachingDefinitions rd) {
        // TODO Auto-generated method stub
        return false;
    }

    private boolean areCopyinDefsTheSame(Set<String> gpuvars, Expansion exp, IASTStatement statement,
            ReachingDefinitions rd) {
        // TODO Auto-generated method stub
        return false;
    }

    private boolean isAIntersectedWithBASubsetOfC(Set<String> a, Set<String> b, Set<String> c) {
        Set<String> intersection = new HashSet<String>(a);
        intersection.retainAll(b);
        return c.containsAll(intersection);
    }
    
//    private boolean areCopyinDefsTheSame(Set<String> ocopyset, Set<String> newcopyset, ReachingDefinitions rd) {
//        Set<String> varsInBoth = new HashSet<String>(ocopyset);
//        varsInBoth.retainAll(newcopyset);
//        Set<IASTNode> reachingDefinitions = rd.reachingDefinitions(getStatement());
//        for(String var : varsInBoth) {
//            for(IASTNode definition : reachingDefinitions) {
//                if(/*definition is a definition of var*/) {
//                    /*
//                     * if(definition is outside the original construct and inside the expansion || 
//                     * definition is inside the original construct and outside the expansion) {
//                     *     return false;
//                     * }
//                     */
//                }
//            }
//        }
//        return true;
//    }
    
    private class Expansion {
        
        private IASTStatement startOffset;
        private IASTStatement endOffset;
        private int size;
        private Set<String> copyin;
        private Set<String> copyout;
        
        public Expansion(IASTStatement first, IASTStatement last, int size, Set<String> copyin, Set<String> copyout) {
            this.startOffset = first;
            this.endOffset = last;
            this.size = size;
            this.copyin = copyin;
            this.copyout = copyout;
        }

        public IASTStatement getStartOffset() {
            return startOffset;
        }

        public IASTStatement getEndOffset() {
            return endOffset;
        }

        public int getSize() {
            return size;
        }

        public Set<String> getCopyin() {
            return copyin;
        }

        public Set<String> getCopyout() {
            return copyout;
        }
        
    }
    
}
