package edu.auburn.oaccrefac.core.change;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;

public abstract class ASTChange {

    private ASTRewrite m_rewriter;
    private IProgressMonitor m_pm;
    
    //Internal preprocessor context map
    private Map<IASTNode, List<String> > m_pp_context;
    
    public ASTChange(ASTRewrite rewriter) {
        m_rewriter = rewriter;
        m_pp_context = new HashMap<>();
    }
    
    public final RefactoringStatus checkConditions(RefactoringStatus init) {
        return this.checkConditions(init, null);
    }
    
    public final RefactoringStatus checkConditions(RefactoringStatus init, IProgressMonitor pm) {
        if (m_pm == null) {
            m_pm = new NullProgressMonitor();
        }
        
        return doCheckConditions(init);
    }

    protected DependenceAnalysis performDependenceAnalysis(RefactoringStatus status, 
            IProgressMonitor pm, IASTStatement... statements) {
        try {
            return new DependenceAnalysis(pm, statements);
        } catch (DependenceTestFailure e) {
            status.addError("Dependences could not be analyzed.  " + e.getMessage());
            return null;
        }
    }

    protected final ASTRewrite change(ASTChange context) {
        if (m_rewriter == null) {
            throw new IllegalArgumentException("Rewriter cannot be null!");
        }
        
        //Set this map to context's map
        this.setPreprocessorContext(context.getPreprocessorContext());

        return doChange(m_rewriter);
    }
    
    public final ASTRewrite change() {
        return doChange(m_rewriter);
    }

    protected abstract ASTRewrite doChange(ASTRewrite rewriter);
    protected abstract RefactoringStatus doCheckConditions(RefactoringStatus init);
    
    protected ASTRewrite safeReplace(ASTRewrite rewriter, 
            IASTNode node, IASTNode replacement) {
        
        class MapCleaner extends ASTVisitor {
            public MapCleaner() {
                shouldVisitStatements = true;
            }
            @Override
            public int visit(IASTStatement stmt) {
                m_pp_context.remove(stmt);
                return PROCESS_CONTINUE;
            }
        }
        
        m_pp_context.put(replacement, m_pp_context.remove(node));
        node.accept(new MapCleaner());
        return rewriter.replace(node, replacement, null);
    }
    
    protected ASTRewrite safeInsertBefore(ASTRewrite rewriter,
            IASTNode parent, IASTNode insertionPoint, IASTNode newNode) {
                
        return rewriter.insertBefore(parent, insertionPoint, newNode, null);
    }
    
    protected void safeRemove(ASTRewrite rewriter, IASTNode node) {
        rewriter.remove(node, null);
    }
    
    public void setRewriter(ASTRewrite rewriter) {
        m_rewriter = rewriter;
    }
    public ASTRewrite getRewriter() { return m_rewriter; }
    
    public void setProgressMonitor(IProgressMonitor pm) {
        m_pm = pm;
    }
    public IProgressMonitor getProgressMonitor() { return m_pm; }
    protected void setPreprocessorContext(Map<IASTNode, List<String>> in) {
        if (in != null) {
            m_pp_context = in;
        }
    }
    protected Map<IASTNode, List<String>> getPreprocessorContext() {
        return m_pp_context;
    }
    
}
