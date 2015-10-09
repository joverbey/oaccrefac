package edu.auburn.oaccrefac.core.transformations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.NullProgressMonitor;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.dependence.VariableAccess;

public class IntroduceDataConstructAlteration extends SourceStatementsAlteration<IntroduceDataConstructCheck> {

    public IntroduceDataConstructAlteration(IASTRewrite rewriter, IntroduceDataConstructCheck check) {
        super(rewriter, check);
    }

    @Override
    protected void doChange() throws Exception {
        IASTStatement[] stmts = getStatements();
        IASTStatement[] funcStatements = ((IASTCompoundStatement) ASTUtil.findNearestAncestor(stmts[0], IASTFunctionDefinition.class).getBody()).getStatements();
        DependenceAnalysis dep = new DependenceAnalysis(new NullProgressMonitor(), funcStatements);
        List<VariableAccess> copyin = new ArrayList<VariableAccess>(inferCopyins(dep));
        List<VariableAccess> copyout = new ArrayList<VariableAccess>(inferCopyouts(dep));
        String[] copyinStr = new String[copyin.size()];
        String[] copyoutStr = new String[copyout.size()];
        for(int i = 0; i < copyinStr.length; i++) {
            copyinStr[i] = copyin.get(i).getVariableName().toString();
        }
        for(int i = 0; i < copyoutStr.length; i++) {
            copyoutStr[i] = copyout.get(i).getVariableName().toString();
        }
        
        String origRegion = "";
        for (IASTStatement stmt : stmts) {
            origRegion += stmt.getRawSignature();
        }
        StringBuilder replacement = new StringBuilder();
        replacement.append(pragma("acc data"));
        replacement.append(copyin.isEmpty() ? "" : copyin(copyinStr));
        replacement.append(copyout.isEmpty() ? "" : copyout(copyoutStr));
        replacement.append(System.lineSeparator());
        replacement.append(compound(decompound(origRegion)));
        this.replace(getOffset(), getLength(), replacement.toString());
        finalizeChanges();
    }

    private boolean inRegion(IASTStatement statement) {
        for (IASTStatement stmtInRegion : getStatements()) {
            if (ASTUtil.isAncestor(stmtInRegion, statement)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean variableAlreadyInSet(VariableAccess access, Set<VariableAccess> accesses) {
        for(VariableAccess var : accesses) {
            try {
                if(var.refersToSameVariableAs(access)) {
                    return true;
                }
            } catch (DependenceTestFailure e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private Set<VariableAccess> inferCopyins(DependenceAnalysis dep) {
        Set<VariableAccess> copyin = new HashSet<VariableAccess>();
        for (VariableAccess write : dep.getVariableAccesses()) {
            for (VariableAccess read : dep.getVariableAccesses()) {
                if (dep.reaches(write, read)) {
                    if (inRegion(read.getEnclosingStatement()) && !inRegion(write.getEnclosingStatement())
                            && !variableAlreadyInSet(write, copyin)) {
                        copyin.add(write);
                    }
                }
            }
        }
        return copyin;
    }

    private Set<VariableAccess> inferCopyouts(DependenceAnalysis dep) {
        Set<VariableAccess> copyout = new HashSet<VariableAccess>();
        for (VariableAccess write : dep.getVariableAccesses()) {
            for (VariableAccess read : dep.getVariableAccesses()) {
                if (dep.reaches(write, read)) {
                    if (inRegion(write.getEnclosingStatement()) && !inRegion(read.getEnclosingStatement())
                            && !variableAlreadyInSet(read, copyout)) {
                        copyout.add(read);
                    }
                }
            }
        }
        return copyout;
    }

}
