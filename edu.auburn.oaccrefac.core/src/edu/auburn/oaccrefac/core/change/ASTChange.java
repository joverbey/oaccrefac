package edu.auburn.oaccrefac.core.change;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTLiteralNode;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

public abstract class ASTChange {

    private ASTRewrite m_rewriter;
    private IProgressMonitor m_pm;
    private TextEditGroup m_teg;
    
    public ASTChange(ASTRewrite rewriter) {
        m_rewriter = rewriter;
        m_teg = new TextEditGroup("edits");
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
        return rewriter.replace(node, replacement, null);
    }
    
    protected ASTRewrite safeInsertBefore(ASTRewrite rewriter,
            IASTNode parent, IASTNode insertionPoint, IASTNode newNode) {
        return rewriter.insertBefore(parent, insertionPoint, newNode, null);
    }
    
    protected void safeRemove(ASTRewrite rewriter, IASTNode node) {
        rewriter.remove(node, null);
    }
    
    
    protected String[] getPragmas(IASTNode node) {
        if(!(node instanceof IASTForStatement)) {
            throw new UnsupportedOperationException("Currently only support pragmas on for loops");
        }
        List<IASTPreprocessorPragmaStatement> p = InquisitorFactory.getInquisitor((IASTForStatement) node).getLeadingPragmas();
        String[] pragCode = new String[p.size()];
        for(int i = 0; i < pragCode.length; i++) {
            pragCode[i] = p.get(i).getRawSignature();
        }
        return pragCode; 
    }
    protected void insertPragmas(IASTNode node, String... pragmas) {
        if(!(node instanceof IASTForStatement)) {
            throw new UnsupportedOperationException("Currently only support pragmas on for loops");
        }
        for(String pragma : pragmas) {
            if(!pragma.startsWith("#pragma")) {
                throw new IllegalArgumentException("String is not a pragma");
            }
            InsertEdit ie = new InsertEdit(node.getFileLocation().getNodeOffset(), pragma + System.lineSeparator());
            m_teg.addTextEdit(ie);
        }
    }
    protected void deepRemovePragmas(IASTNode node) {
        class PragmaRemover extends ASTVisitor {
            
            public PragmaRemover() { 
                shouldVisitStatements = true;
            }
            
            @Override
            public int visit(IASTStatement stmt) {
                if(stmt instanceof IASTForStatement) {
                    for(IASTPreprocessorPragmaStatement prag : InquisitorFactory.getInquisitor((IASTForStatement) stmt).getLeadingPragmas()) {
                        DeleteEdit de = new DeleteEdit(prag.getFileLocation().getNodeOffset(), prag.getFileLocation().getNodeLength() + 1);
                        m_teg.addTextEdit(de);
                    }
                }
                return PROCESS_CONTINUE;
            }
        }
        node.accept(new PragmaRemover());
    }
    protected void removePragmas(IASTNode node) {
        if(!(node instanceof IASTForStatement)) {
            throw new UnsupportedOperationException("Currently only support pragmas on for loops");
        }
        for(IASTPreprocessorPragmaStatement prag : InquisitorFactory.getInquisitor((IASTForStatement) node).getLeadingPragmas()) {
            DeleteEdit de = new DeleteEdit(prag.getFileLocation().getNodeOffset(), prag.getFileLocation().getNodeLength() + 1);
            m_teg.addTextEdit(de);
        }
    }
    protected void finalizePragmas(IASTTranslationUnit tu) {
        m_rewriter.insertBefore(tu, tu.getChildren()[0], m_rewriter.createLiteralNode(""), m_teg);
    }
    
    public void setRewriter(ASTRewrite rewriter) {
        m_rewriter = rewriter;
    }
    public ASTRewrite getRewriter() { return m_rewriter; }
    
    public void setProgressMonitor(IProgressMonitor pm) {
        m_pm = pm;
    }
    public IProgressMonitor getProgressMonitor() { return m_pm; }
    
}
