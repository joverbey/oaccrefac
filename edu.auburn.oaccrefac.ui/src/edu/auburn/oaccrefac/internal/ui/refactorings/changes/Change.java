package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTCompoundStatement;
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
        IASTNode parent = argNode.getParent();
        if (m_node.isFrozen()) {
            //need locations to use getOriginalNode() for some reason...
            parent = argNode.getParent().copy(CopyStyle.withLocations);
            for(IASTNode child : parent.getChildren()) {
                if(child.getOriginalNode() == m_node) {
                    argNode = (T) child;
                    break;
                }
            }
        }
        mapPreprocessors(m_node);
        replacePreprocessors(argNode);
        changed = doChange(argNode);
        rewriter = rewriter.replace(getOriginal().getParent(), changed.getParent(), null);
        for(IASTNode key : m_pp_context.keySet()) {
            List<String> prags = m_pp_context.get(key);
            for(String prag : prags) {
                IASTNode pragNode = rewriter.createLiteralNode(prag);
                rewriter.insertBefore(key.getParent(), key, pragNode, null);
            }
        }
        return rewriter;
    }
    
    //maps the preprocessor statements for the node and its children to the appropriate nodes
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
    
    //replaces the nodes in the current node-preprocessor map with the node copies from the given tree
    protected final void replacePreprocessors(IASTNode copy) {

        //go over copy tree mapping originals to copies
        //go over entire m_pp map replacing originals with copies
        
        class OrigCopyMapper extends ASTVisitor {
            
            public Map<IASTNode, IASTNode> nodes = new HashMap<IASTNode, IASTNode>(); 
            
            public OrigCopyMapper() {
                shouldVisitStatements = true;
            }
            
            @Override
            public int visit(IASTStatement stmt) {
                nodes.put(stmt.getOriginalNode(), stmt);
                return PROCESS_CONTINUE;
            }
            
        }
       
        OrigCopyMapper mapper = new OrigCopyMapper();
        copy.accept(mapper);
        Map<IASTNode, List<String>> contextCopy = new HashMap<IASTNode, List<String>>();
        for(IASTNode orig : m_pp_context.keySet()) {
            List<String> prag = m_pp_context.get(orig);
            IASTNode copiedNode = mapper.nodes.get(orig);
            if(prag != null && copiedNode != null) {
                contextCopy.put(copiedNode, prag);
            }
        }
        m_pp_context = contextCopy;
        
    }
    
    //uses the rewrites and current node-preprocessor map add preprocessor statements into the tree
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
