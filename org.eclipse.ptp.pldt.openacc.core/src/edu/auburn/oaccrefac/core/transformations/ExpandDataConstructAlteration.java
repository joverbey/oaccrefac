/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert - initial API and implementation
 *******************************************************************************/

package edu.auburn.oaccrefac.core.transformations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

import edu.auburn.oaccrefac.core.dataflow.ReachingDefinitions;
import edu.auburn.oaccrefac.core.parser.ASTAccCopyinClauseNode;
import edu.auburn.oaccrefac.core.parser.ASTAccCopyoutClauseNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataClauseListNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataNode;
import edu.auburn.oaccrefac.core.parser.IAccDataClause;
import edu.auburn.oaccrefac.core.parser.Token;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.OpenACCUtil;

public class ExpandDataConstructAlteration extends PragmaDirectiveAlteration<ExpandDataConstructCheck> {

    public ExpandDataConstructAlteration(IASTRewrite rewriter, ExpandDataConstructCheck check) {
        super(rewriter, check);
    }

    //TODO may be worth profiling this to get some speedup; takes a little time to run right now
    @Override
    protected void doChange() throws Exception {
        ReachingDefinitions rd = new ReachingDefinitions(ASTUtil.findNearestAncestor(getStatement(), IASTFunctionDefinition.class));

        Map<String, String> copyin = OpenACCUtil.getCopyin(check.getConstruct());
        Map<String, String> copyout = OpenACCUtil.getCopyout(check.getConstruct());
        Map<String, String> create = OpenACCUtil.getCreate(check.getConstruct());
        List<ASTAccDataClauseListNode> otherClauses = getOtherClauses(check.getConstruct());
        
        //using parent node
        int maxup = getMaxUp(getStatement());
        int maxdown = getMaxDown(getStatement());
        int osize;
        if(getStatement() instanceof IASTCompoundStatement) {
            osize = ((IASTCompoundStatement) getStatement()).getStatements().length;
        }
        else {
            osize = 1;
        }
        Set<String> ocopyin = OpenACCUtil.inferCopyin(rd, getStatementsInBody());
        Set<String> ocopyout = OpenACCUtil.inferCopyout(rd, getStatementsInBody());
        List<Expansion> expansions = new ArrayList<Expansion>();
        Set<String> gpuuses = getGpuVars(getStatement(), false);
        Set<String> gpudefs = getGpuVars(getStatement(), true);
        for(int i = 0; i <= maxup; i++) {
            for(int j = 0; j <= maxdown; j++) {
                IASTStatement[] expStmts = getExpansionStatements(i, j, getStatement());
                Set<String> expcopyin = OpenACCUtil.inferCopyin(rd, expStmts);
                Set<String> expcopyout = OpenACCUtil.inferCopyout(rd, expStmts);
                
                //be sure that for variables actually used on the gpu, the copyin set hasn't changed 
                if(areCopyDefsTheSame(gpuuses, ocopyin, expcopyin))
                //be sure that for variables defined on the gpu, the copyout set hasn't changed
                if(areCopyDefsTheSame(gpudefs, ocopyout, expcopyout)) 
                //be sure we don't add a declaration to this inner scope if it is used in the outer scope 
                if(!expansionAddsDeclarationIllegally(expStmts, getStatement(), rd))
                //be sure the copy sets haven't gotten larger (cp to our discovered copysets for the original, not the original code's)
                if(expcopyin.size() + expcopyout.size() <= ocopyin.size() + ocopyout.size())
                    expansions.add(new Expansion(expStmts, osize + i + j, expcopyin, expcopyout));
            }
        }

        //just pick the largest; could pick the largest with some comparison to copysize, or the one with the smallest copysize, etc
        Expansion largestexp = null;
        for(Expansion exp : expansions) {
           if(largestexp == null || exp.getSize() >= largestexp.getSize()) {
                largestexp = exp;
            }
        }
        
        String newConstruct = "";
        for(IASTStatement statement : largestexp.getStatements()) {
            for(IASTPreprocessorPragmaStatement pragma : ASTUtil.getLeadingPragmas(statement)) {
                if(!pragma.equals(getPragma()))
                    newConstruct += pragma.getRawSignature() + System.lineSeparator();
            }
            if(statement.equals(getStatement())) {
                newConstruct += decompound(statement.getRawSignature()) + System.lineSeparator();
            }
            else {
                newConstruct += statement.getRawSignature() + System.lineSeparator();
            }
        }

        List<String> copyinarr = new ArrayList<String>();
        for(String name : largestexp.getCopyin()) {
            //TODO later this "if" can be removed and create clauses can be actually generated like copyin/out are
            if (!create.containsKey(name)) {
                String actual = copyin.get(name);
                if (actual != null) {
                    copyinarr.add(actual);
                } else {
                    copyinarr.add(name);
                }
            }
        }
        
        List<String> copyoutarr = new ArrayList<String>();
        for(String name : largestexp.getCopyout()) {
          //TODO later this "if" can be removed and create clauses can be actually generated like copyin/out are
            if (!create.containsKey(name)) {
                String actual = copyout.get(name);
                if (actual != null) {
                    copyoutarr.add(actual);
                } else {
                    copyoutarr.add(name);
                }
            }
        }
        
        String pragma = generatePragma(copyinarr, copyoutarr, otherClauses);
        newConstruct = pragma + compound(newConstruct);

        this.remove(getPragma());
        
        IASTStatement[] exparr = largestexp.getStatements();
        int start = exparr[0].getFileLocation().getNodeOffset();
        int end = exparr[exparr.length - 1].getFileLocation().getNodeOffset() + exparr[exparr.length - 1].getFileLocation().getNodeLength(); 
        int len = end - start;
        this.replace(start,  len, newConstruct);
        finalizeChanges();
    }

    private IASTStatement[] getStatementsInBody() {
        if (getStatement() instanceof IASTCompoundStatement) {
            return ((IASTCompoundStatement) getStatement()).getStatements();
        } else {
            return new IASTStatement[] { getStatement() };
        }
    }
    
    private List<ASTAccDataClauseListNode> getOtherClauses(ASTAccDataNode data) {
        List<ASTAccDataClauseListNode> clauses = new ArrayList<ASTAccDataClauseListNode>();
        for(ASTAccDataClauseListNode listNode : data.getAccDataClauseList()) {
            IAccDataClause clause = listNode.getAccDataClause();
            if(!(clause instanceof ASTAccCopyinClauseNode)
                    && !(clause instanceof ASTAccCopyoutClauseNode)
                    //TODO enable create clause inference in the future
                    /* && !(clause instanceof ASTAccCreateClauseNode) */) {
                clauses.add(listNode);
            }
        }
        return clauses;
    }

    private String generatePragma(List<String> copyin, List<String> copyout, List<ASTAccDataClauseListNode> otherClauses) {
        StringBuilder prag = new StringBuilder(pragma("acc data") + " ");
        List<String> clauses = new ArrayList<String>();
        if(!copyin.isEmpty())
            clauses.add(copyin(copyin.toArray(new String[copyin.size()])));
        if(!copyout.isEmpty())
            clauses.add(copyout(copyout.toArray(new String[copyout.size()])));
        for(ASTAccDataClauseListNode clause : otherClauses) {
            Token tok = clause.findFirstToken();
            tok.setText("");
            clauses.add(clause.toString().trim());
        }
        for(int i = 0; i < clauses.size(); i++) {
            prag.append(clauses.get(i).toString().trim());
            if(i != clauses.size() - 1) {
                prag.append(", ");
            }
        }
        prag.append(System.lineSeparator());
        return prag.toString();
    }
    
    //TODO this seems to work, but maybe using IScope would be more "correct"?
    private boolean expansionAddsDeclarationIllegally(IASTStatement[] expStmts, IASTStatement origStmt,
            ReachingDefinitions rd) {
        //get all variable declarations in the expansion but not in the original construct
        Set<IASTDeclarationStatement> decls = new HashSet<IASTDeclarationStatement>(); 
        for(IASTStatement stmt : expStmts) {
            if(!stmt.equals(origStmt)) {
                decls.addAll(ASTUtil.find(stmt, IASTDeclarationStatement.class));
            }
        }
        
        //if a use reached by an added declaration is not in the expansion, return false
        for(IASTDeclarationStatement decl : decls) {
            Set<IASTName> uses = rd.reachedUses(decl);
            for(IASTName use : uses) {
                if(!expansionIncludesNode(expStmts, use)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean expansionIncludesNode(IASTStatement[] expStmts, IASTNode node) {
        for(IASTStatement stmt : expStmts) {
            if(ASTUtil.isAncestor(stmt, node)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> getGpuVars(IASTNode node, boolean shouldGetDefs) {
        Set<String> gpuVars = new HashSet<String>();
        getGpuVars(node, gpuVars, shouldGetDefs);
        return gpuVars;
    }
    
    private void getGpuVars(IASTNode node, Set<String> gpuVars, boolean shouldGetDefs) {
        if(node instanceof IASTStatement) {
            IASTStatement stmt = (IASTStatement) node;
            String[] prags = ASTUtil.getPragmas(stmt);
            for(String prag : prags) {
                //TODO should we parse this instead?
                if(prag.startsWith("#pragma acc parallel") || prag.startsWith("#pragma acc kernels")) {
                    List<IASTName> names = ASTUtil.getNames(stmt);
                    for(IASTName name : names) {
                        if(ASTUtil.isDefinition(name) && shouldGetDefs) {
                            gpuVars.add(name.getRawSignature());
                        }
                        else if(!ASTUtil.isDefinition(name) && !shouldGetDefs) {
                            gpuVars.add(name.getRawSignature());
                        }
                    }
                }
            }
        }
        
        for(IASTNode child : node.getChildren()) {
            getGpuVars(child, gpuVars, shouldGetDefs);
        }
        
    }

    private int getMaxDown(IASTStatement statement) {
        int i = 0;
        IASTNode next = statement;
        while(true) {
            next = ASTUtil.getNextSibling(next);
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

    private int getMaxUp(IASTStatement statement) {
        int i = 0;
        IASTNode prev = statement;
        while(true) {
            prev = ASTUtil.getPreviousSibling(prev);
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

    private IASTStatement[] getExpansionStatements(int stmtsUp, int stmtsDown, IASTStatement original) {
        List<IASTStatement> statements = new ArrayList<IASTStatement>();
        statements.add(original);
        
        IASTStatement current = original;
        for(int i = 0; i < stmtsUp; i++) {
            //TODO type checking
            current = (IASTStatement) ASTUtil.getPreviousSibling(current);
            statements.add(0, current);
        }
        
        current = original;
        for(int j = 0; j < stmtsDown; j++) {
            //TODO type checking
            current = (IASTStatement) ASTUtil.getNextSibling(current);
            statements.add(current);
        }
        return statements.toArray(new IASTStatement[statements.size()]);
    }

    private boolean areCopyDefsTheSame(Set<String> gpuvars, Set<String> ocopyin, Set<String> expcopyin) {
        //if a variable is in gpuvars and not in ocopyin, it shouldn't be in expcopyin: 
        //(G - (G ∩ O)) ∩ E = ∅
        //if a variable is in gpuvars and in ocopyin, it should be in expcopyin:
        //(G ∩ O) ⊆ E
        Set<String> G = new HashSet<String>(gpuvars);
        Set<String> O = new HashSet<String>(ocopyin);
        Set<String> E = new HashSet<String>(expcopyin);
        Set<String> GintO = new HashSet<String>(gpuvars);
        GintO.retainAll(O);
        Set<String> GminusGintO = new HashSet<String>(G);
        GminusGintO.removeAll(GintO);
        Set<String> GminusGintOintE = new HashSet<String>(GminusGintO);
        GminusGintOintE.retainAll(E);
        
        return GminusGintOintE.isEmpty() 
                && E.containsAll(GintO);
    }

    private class Expansion {
        
        private IASTStatement[] statements;
        private int size;
        private Set<String> copyin;
        private Set<String> copyout;
        
        public Expansion(IASTStatement[] statements, int size, Set<String> copyin, Set<String> copyout) {
            this.size = size;
            this.statements = statements;
            this.copyin = copyin;
            this.copyout = copyout;
        }

        public IASTStatement[] getStatements() {
            return statements;
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
