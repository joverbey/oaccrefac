package edu.auburn.oaccrefac.core.change;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEditGroup;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

public abstract class ASTChange {

    private IASTRewrite m_rewriter;
    private IProgressMonitor m_pm;
    private TextEditGroup m_teg;
    
    public ASTChange(IASTRewrite rewriter) {
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

    protected final IASTRewrite change(ASTChange context) {
        if (m_rewriter == null) {
            throw new IllegalArgumentException("Rewriter cannot be null!");
        }
        
        return doChange(m_rewriter);
    }
    
    public final IASTRewrite change() {
        if (m_rewriter == null) {
            throw new IllegalArgumentException("Rewriter cannot be null!");
        }
        
        return doChange(m_rewriter);
    }

    protected abstract IASTRewrite doChange(IASTRewrite rewriter);
    protected abstract RefactoringStatus doCheckConditions(RefactoringStatus init);
    
    protected IASTRewrite safeReplace(IASTRewrite rewriter, 
            IASTNode node, IASTNode replacement) {
        return rewriter.replace(node, replacement, m_teg);
    }
    
    protected IASTRewrite safeInsertBefore(IASTRewrite rewriter,
            IASTNode parent, IASTNode insertionPoint, IASTNode newNode) {
        return rewriter.insertBefore(parent, insertionPoint, newNode, m_teg);
    }
    
    protected void safeRemove(IASTRewrite rewriter, IASTNode node) {
        rewriter.remove(node, m_teg);
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
        m_rewriter.insertBefore(node.getTranslationUnit(), node.getTranslationUnit().getChildren()[0], m_rewriter.createLiteralNode(""), m_teg);
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
        m_rewriter.insertBefore(node.getTranslationUnit(), node.getTranslationUnit().getChildren()[0], m_rewriter.createLiteralNode(""), m_teg);
    }
    protected void removePragmas(IASTNode node) {
        if(!(node instanceof IASTForStatement)) {
            throw new UnsupportedOperationException("Currently only support pragmas on for loops");
        }
        for(IASTPreprocessorPragmaStatement prag : InquisitorFactory.getInquisitor((IASTForStatement) node).getLeadingPragmas()) {
            DeleteEdit de = new DeleteEdit(prag.getFileLocation().getNodeOffset(), prag.getFileLocation().getNodeLength() + 1);
            m_teg.addTextEdit(de);
        }
        m_rewriter.insertBefore(node.getTranslationUnit(), node.getTranslationUnit().getChildren()[0], m_rewriter.createLiteralNode(""), m_teg);
    }
    
    public void setRewriter(IASTRewrite rewriter) {
        m_rewriter = rewriter;
    }
    public IASTRewrite getRewriter() { return m_rewriter; }
    
    public void setProgressMonitor(IProgressMonitor pm) {
        m_pm = pm;
    }
    public IProgressMonitor getProgressMonitor() { return m_pm; }
    
}
