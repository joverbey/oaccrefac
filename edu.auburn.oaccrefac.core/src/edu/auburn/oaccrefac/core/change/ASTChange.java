package edu.auburn.oaccrefac.core.change;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEditGroup;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

public abstract class ASTChange {

    private ASTRewrite m_rewriter;
    private TextEditGroup teg;
    private IProgressMonitor m_pm;
    
    //Internal preprocessor context map
    private Map<IASTNode, List<String> > m_pp_context;
    
    public ASTChange(ASTRewrite rewriter) {
        m_rewriter = rewriter;
        m_pp_context = new HashMap<>();
        teg = new TextEditGroup("refactoring");
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
        if (m_rewriter == null) {
            throw new IllegalArgumentException("Rewriter cannot be null!");
        }
        
        return doChange(m_rewriter);
    }

    protected abstract ASTRewrite doChange(ASTRewrite rewriter);
    protected abstract RefactoringStatus doCheckConditions(RefactoringStatus init);
    
    protected ASTRewrite safeReplace(ASTRewrite rewriter, 
            IASTNode node, IASTNode replacement) {
        return rewriter.replace(node, replacement, teg);
    }
    
    protected ASTRewrite safeInsertBefore(ASTRewrite rewriter,
            IASTNode parent, IASTNode insertionPoint, IASTNode newNode) {
        return rewriter.insertBefore(parent, insertionPoint, newNode, teg);
    }
    
    /** Limited for now to for loops only, since getLeadingPragmas is in the ForLoopInquisitor */
    protected void reassociatePragmas(IASTNode oldNode, IASTNode newNode) {
        if(oldNode instanceof IASTForStatement && newNode instanceof IASTForStatement) {
            List<String> prags = new ArrayList<String>();
            for(IASTPreprocessorStatement pp : InquisitorFactory.getInquisitor((IASTForStatement) oldNode).getLeadingPragmas()) {
                prags.add(pp.getRawSignature());
            }
            m_pp_context.put(newNode, prags);
        }
        else {
            throw new UnsupportedOperationException("Currently only support pragmas on for loops");
        }
    }
    
    protected void insertPragma(String pragma, IASTNode node) {
        if(!pragma.startsWith("#pragma")) {
            throw new IllegalArgumentException("String is not a pragma");
        }
        m_pp_context.put(node, Arrays.asList(pragma + System.lineSeparator()));
    }
    
    
    protected void safeRemove(ASTRewrite rewriter, IASTNode node) {
        rewriter.remove(node, teg);
    }
    
    protected void writePragmaChanges(ASTRewrite rewriter) {
        Iterator<Entry<IASTNode, List<String>>> it = m_pp_context.entrySet().iterator();
        while (it.hasNext()) {
            Entry<IASTNode, List<String>> pair = it.next();
            for(String prag : pair.getValue()) {
                rewriter.insertBefore(
                        pair.getKey().getParent(), 
                        pair.getKey(), 
                        rewriter.createLiteralNode(prag + System.lineSeparator()), null);
            }
        }
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
