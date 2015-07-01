package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

public abstract class Change<T extends IASTNode> {

    private T m_node;
    private IProgressMonitor m_pm;
    
    //Internal preprocessor context map
    private Map<IASTNode, List<String> > m_pp_context;
    
    public Change(T nodeToChange) {
        m_node = nodeToChange;
        m_pp_context = new HashMap<>();
    }
    
    public final RefactoringStatus checkConditions(RefactoringStatus init) {
        return this.checkConditions(init, null);
    }
    
    public final RefactoringStatus checkConditions(RefactoringStatus init, IProgressMonitor pm) {
        if (m_pm == null) {
            m_pm = new NullProgressMonitor();
        }
        
        if (m_node == null) {
            init.addFatalError("Change Error: node to be changed cannot be null!");
            return init;
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
    
    /**
     * This method performs the change on the intended node. This 
     * is to be used internally by other changes.
     * @return the changed node.
     * @throws IllegalArgumentException if the node to be changed is frozen
     */
    protected final T change(Change<?> context) {
        //Set this map to context's map
        this.setPreprocessorContext(context.getPreprocessorContext());
        
        if (m_node.isFrozen()) {
            throw new IllegalArgumentException("Error -- changes within changes"
                    + " cannot occur to frozen nodes.");
        }
        return doChange(m_node);
    }
    
    @SuppressWarnings("unchecked")
    public final ASTRewrite change(ASTRewrite rewriter) {
        IASTNode changed = null;
        T argNode = m_node;
        if (m_node.isFrozen()) {
            argNode = (T) m_node.copy(CopyStyle.withLocations);
        }
        
        mapPreprocessors(m_node);
        replacePreprocessors(m_node, argNode);
        changed = doChange(argNode);
        rewriter = rewriter.replace(getOriginal(), changed, null);
        rewriter = insertPreprocessors(rewriter);
        return rewriter;
    }
    
    protected final void mapPreprocessors(IASTNode node) {
        class PPMapPopulator extends ASTVisitor {
            
            public Map<IASTNode, List<String>> pp_map = new HashMap<IASTNode, List<String>>();
            
            public PPMapPopulator() {
                shouldVisitStatements = true;
            }
            
            @Override
            public int visit(IASTStatement stmt) {
                //TODO handle compound statements as well as for loops
                List<String> pragSigs = new ArrayList<String>();
                if(stmt instanceof IASTForStatement) {
                    ForStatementInquisitor inq = InquisitorFactory.getInquisitor((IASTForStatement) stmt);
                    for(IASTPreprocessorPragmaStatement prag : inq.getLeadingPragmas()) {
                         pragSigs.add(prag.getRawSignature());
                    }
                }
                pp_map.put(stmt, pragSigs);
                return PROCESS_CONTINUE;
            }     
        }
        
        PPMapPopulator pop = new PPMapPopulator();
        node.accept(pop);
        setPreprocessorContext(pop.pp_map);
    }
    
    protected final void replacePreprocessors(IASTNode original, IASTNode copy) {
        if(original == copy) {
            return;
        }
        
        class Remapper extends ASTVisitor {
            
            IASTNode original;
            IASTNode copy;
            
            public Remapper(IASTNode original, IASTNode copy) {
                shouldVisitStatements = true;
                this.original = original;
                this.copy = copy;
            }
            
            @Override
            public int visit(IASTStatement stmt) {
                
                //find corresponding node in copy
                class CopyFinder extends ASTVisitor {
                    
                    IASTStatement foundCopy = null;
                    IASTNode original;
                    
                    public CopyFinder(IASTNode original) {
                        shouldVisitStatements = true;
                        this.original = original;
                    }
                    
                    @Override
                    public int visit(IASTStatement stmt) {
                        if(stmt.getOriginalNode() == original) {
                            foundCopy = stmt;
                            System.out.println("found copy: " + stmt + " of original: " + original);
                            return PROCESS_ABORT;
                        }
                        return PROCESS_CONTINUE;
                    }
                }
                
                //replace original map key with copy
                List<String> prags = m_pp_context.get(stmt);
                if(prags != null) {
                    System.out.println("searching for copy of " + original);
                    m_pp_context.remove(prags);
                    CopyFinder cf = new CopyFinder(original);
                    copy.accept(cf);
                    m_pp_context.put(cf.foundCopy, prags);
                }
                return PROCESS_CONTINUE;
                
            }
            
        }

        Remapper remapper = new Remapper(original, copy);
        original.accept(remapper);
        
    }
    
    protected final ASTRewrite insertPreprocessors(ASTRewrite rewriter) {
        for(IASTNode key : m_pp_context.keySet()) {
            List<String> prags = m_pp_context.get(key);
            for(String prag : prags) {
                IASTNode pragNode = rewriter.createLiteralNode(prag);
                rewriter.insertBefore(key.getParent(), key, pragNode, null);
            }
        }
        return rewriter;
    }

    /**
     * Abstract method describes the implementation that all changes must
     * define. This method takes in a loop and changes it in respect to
     * it's intended purpose.
     * @param loop -- the loop in which to change
     * @return reference to changed loop
     */
    protected abstract T doChange(T nodeToChange);
    
    /**
     * Abstract method for checking the initial conditions for change objects. For
     * example, in this method, it will check the inputs to the constructor to ensure
     * that the inputs are valid.
     * @param init -- Status object that may or may not be changed depending on error
     * @return -- Reference to changed status object
     */
    protected abstract RefactoringStatus doCheckConditions(RefactoringStatus init);
    
    public T getOriginal() { return m_node; }
    public void setProgressMonitor(IProgressMonitor pm) {
        m_pm = pm;
    }
    public IProgressMonitor getProgressMonitor() {
        return m_pm;
    }
    protected void setPreprocessorContext(Map<IASTNode, List<String>> in) {
        if (in != null) {
            m_pp_context = in;
        }
    }
    protected Map<IASTNode, List<String>> getPreprocessorContext() {
        return m_pp_context;
    }
    
}
