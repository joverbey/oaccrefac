package edu.auburn.oaccrefac.internal.ui.refactorings;

import java.util.ArrayList;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTForStatement;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.patternmatching.ASTMatcher;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryIntegerConstant;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

/**
 * Class is meant to be an abstract base class for all ForLoop
 * transformation refactorings. It includes all methods that would
 * be synonomous in all for-loop refactorings. The only method that
 * is different in the refactorings is the 'collectModifications' 
 * method, which is to be overridden in order to make the magic happen.
 *
 */
@SuppressWarnings("restriction")
public abstract class ForLoopRefactoring extends CRefactoring {

    private SubMonitor m_progress;
    private IASTTranslationUnit m_ast;
    private IASTForStatement m_forloop;
    
    //Patterns of for loops that are acceptable to refactor...
    private static String[] patterns = {
            "for (i = 0; i < 1; i++) ;",
            "for (int i = 0; i < 1; i++) ;",
            "for (i = 0; i <= 1; i++) ;",
            "for (int i = 0; i <= 1; i++) ;",
            "for (i = 0; i < 1; i+=1) ;",
            "for (int i = 0; i < 1; i+=1) ;",
            "for (i = 0; i <= 1; i+=1) ;",
            "for (int i = 0; i <= 1; i+=1) ;",
            "for (i = 0; i < 1; i=i+1) ;",
            "for (int i = 0; i < 1; i=i+1) ;",
            "for (i = 0; i <= 1; i=i+1) ;",
            "for (int i = 0; i <= 1; i=i+1) ;",
    };

    /**
     * The constructor for ForLoopRefactoring takes items for refactoring and tosses them
     * up to the super class as well as does some checks to make sure things are capiche.
     */
	public ForLoopRefactoring(ICElement element, ISelection selection, ICProject project) {
		super(element, selection, project);
		
		if (selection == null || tu.getResource() == null || project == null)
            initStatus.addFatalError("Invalid selection");
	}
	
	/**
	 * This is the abstract method that is the implementation for all refactorings.
	 * Override this method in inherited classes and use the rewriter to collect changes
	 * for refactoring.
	 * Tip: Make bigger changes at a time -- making a ton of small replacements and additions
	 *      in the rewriter may cause issues with overwritting text edits and nodes that don't
	 *      appear to be in the AST. You can create new nodes using a node factory of the form
	 *      ICNodeFactory factory = ASTNodeFactoryFactory.getCDefaultFactory();
	 * Tip: While some cases it may be helpful to do the above, see 'LoopInterchangeRefactoring'
	 *      for a case in which it is more practical to simply use the replace method on the
	 *      tree instead.
	 * Tip: Trying to add nodes from an existing AST (say, from the refactored code) into a
	 *      node created from the factory may give you a 'this node is frozen' error. When
	 *      adding a node from an existing AST to one created from a factory, call 'node'.copy()
	 *      to create an unfrozen copy of the node.
	 * @param rewriter
	 * @param pm
	 */
	protected abstract void refactor(ASTRewrite rewriter, IProgressMonitor pm);

	/**
	 * This method is the driver for the refactoring implementation that you defined above.
	 * It does some initialization before the refactoring (that is also similar to all
	 * for loop refactorings) and then calls your implementation.
	 */
	@Override
	protected void collectModifications(IProgressMonitor pm, ModificationCollector collector)
			throws CoreException, OperationCanceledException {
	    
	    pm.subTask("Calculating modifications...");   
	    ASTRewrite rewriter = collector.rewriterForTranslationUnit(getAST());
	    //Other initilization stuff here...
	    
	    refactor(rewriter, pm);
	}

	/**
	 * Checks some initial conditions based on the element to be refactored. 
	 * The method is typically called by the UI to perform an initial checks 
	 * after an action has been executed. The refactoring has to be considered 
	 * as not being executable if the returned status has the severity of 
	 * RefactoringStatus#FATAL. 
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		pm.subTask("Waiting for indexer...");
		prepareIndexer(pm);
		pm.subTask("Analyzing selection...");
		
		m_progress = SubMonitor.convert(pm, 10);
        m_ast = getAST(tu, m_progress.newChild(9));
        m_forloop = findLoop(m_ast);
		
        if (!supportedPattern(m_forloop)) {
            initStatus.addFatalError("Loop form not supported!");
        }
        
        if (containsBreakorContinue(m_forloop)) {
            initStatus.addFatalError("Cannot refactor -- loop contains "
                    + "iteration augment statement (break or continue)");
        }
        
		return initStatus;
	}

	/**
	 * Indexes the project if the project has not already been indexed.
	 * Something to do with references???
	 * @param pm
	 * @throws CoreException
	 */
	private void prepareIndexer(IProgressMonitor pm) throws CoreException  {
		IIndexManager im = CCorePlugin.getIndexManager();
		while (!im.isProjectIndexed(project)) {
			im.joinIndexer(500, pm);
			if (pm.isCanceled())
				throw new CoreException(new Status(IStatus.ERROR, CUIPlugin.PLUGIN_ID, "Cannot continue.  No index."));
		}
		if (!im.isProjectIndexed(project))
			throw new CoreException(new Status(IStatus.ERROR, CUIPlugin.PLUGIN_ID, "Cannot continue.  No index."));
	}	
	
	/**
	 * This function finds the first CASTForStatement (for-loop) within a
	 * selection. It defines a private class (Visitor) to traverse the
	 * translation unit AST (which is an AST for our source file). When
	 * we call ast.accept(v), it traverses the tree to find the first 
	 * acceptance within the 'selectedRegion' (a protected variable from
	 * CRefactoring). 
	 * @param ast -- our AST to traverse
	 * @return CASTForStatement to perform refactoring on
	 */
	protected CASTForStatement findLoop(IASTTranslationUnit ast) {
		class Visitor extends ASTVisitor {
			private CASTForStatement loop = null;

			public Visitor() {
				shouldVisitStatements = true;
			}

			@Override
			public int visit(IASTStatement statement) {
				if (statement instanceof CASTForStatement) {
					CASTForStatement for_stmt = (CASTForStatement) statement;
					//Make sure we are getting the correct loop by checking the statement's 
					//offset with the selected text's region in the project.
					int begin_offset = selectedRegion.getOffset();
					int end_offset = selectedRegion.getOffset() + selectedRegion.getLength();
					if (for_stmt.getOffset() >= begin_offset
						&& (for_stmt.getOffset() < end_offset)) {
						loop = for_stmt;
						return PROCESS_ABORT;
					} else {
						//Otherwise skip this statement
						return PROCESS_CONTINUE;
					}
				}
				return PROCESS_CONTINUE;
			}
		}
		Visitor v = new Visitor();
		ast.accept(v);
		return v.loop;
	}
	
	/**
	 * Method takes in a tree and traverses to determine whether the
	 * tree contains a break or continue statement.
	 * @param tree -- the tree to traverse
	 * @return -- true/false on successful find
	 */
	private boolean containsBreakorContinue(IASTNode tree) {
	    class UnsupportedVisitor extends ASTVisitor {
	        public UnsupportedVisitor() {
	            shouldVisitStatements = true;
	            shouldVisitExpressions = true;
	        }
	        
	        @Override
	        public int visit(IASTStatement statement) {
	            if (statement instanceof IASTBreakStatement
	             || statement instanceof IASTContinueStatement)
	                return PROCESS_ABORT;
	            return PROCESS_CONTINUE;
	        }
	    }
	    return (!tree.accept(new UnsupportedVisitor()));
	}
	
	/**
	 * This method takes a tree and finds the first IASTName occurrence within the
	 * tree. This was a helper method that was used in LoopUnrolling that could be
	 * applied to other situations, so it was placed here for utility.
	 * @param tree -- tree in which to traverse
	 * @return IASTName node of first variable name found
	 */
    protected IASTName findFirstName(IASTNode tree) {
        class Visitor extends ASTVisitor {
            private IASTName varname = null;
            
            public Visitor() {
                shouldVisitNames = true;
            }
            
            @Override
            public int visit(IASTName visitor) {
                varname = visitor;
                return PROCESS_ABORT;
            }
        }
        Visitor v = new Visitor();
        tree.accept(v);
        return v.varname;
    }
	
    /**
     * Method matches the parameter with all of the patterns defined in
     * the pattern string array above. It parses each string into a corresponding
     * AST and then uses a pattern matching utility to match if the pattern
     * is loosely synonymous to the matchee.
     * (Basically, we have to check some pre-conditions before refactoring or else
     *   we could run into some hairy cases such as an array subscript expression
     *   being in the initializer statement...which would be a nightmare to refactor).
     * @param matchee -- tree or AST to match
     * @return Boolean describing whether the matchee matches any supported pattern
     * @throws CoreException
     */
	protected boolean supportedPattern(IASTForStatement matchee) throws CoreException {
	    class LiteralReplacer extends ASTVisitor {
            public LiteralReplacer() {
                shouldVisitExpressions = true;
            }
            
            @Override
            public int visit(IASTExpression visitor) {
                if (visitor instanceof IASTLiteralExpression && visitor.getParent() != null) {
                    IASTLiteralExpression expr = (IASTLiteralExpression) visitor;                    
                    if (expr.getParent() instanceof IASTBinaryExpression)
                        ((IASTBinaryExpression)expr.getParent()).setOperand2(new ArbitraryIntegerConstant());
                    else if (expr.getParent() instanceof IASTEqualsInitializer)
                        ((IASTEqualsInitializer)expr.getParent()).setInitializerClause(new ArbitraryIntegerConstant());
                }
                return PROCESS_CONTINUE;
            }
        }
	    
	    for (String pattern : patterns) {
	        IASTForStatement forLoop = (IASTForStatement) ASTUtil.parseStatement(pattern);
	        IASTForStatement pattern_ast = forLoop.copy(CopyStyle.withoutLocations);
	        pattern_ast.accept(new LiteralReplacer());
	        pattern_ast.setBody(new ArbitraryStatement());
            if (ASTMatcher.unify(pattern_ast, matchee) != null)
                return true;
	    }
	    return false;
	} 
	
	//*************************************************************************
    //Getters & Setters
	
    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        // TODO Auto-generated method stub
        return null;
    }
	
    protected CASTForStatement getLoop() {
        return (CASTForStatement) m_forloop;
    }
    
    protected SubMonitor getProgress() {
        return m_progress;
    }
    
    protected IASTTranslationUnit getAST() {
        return m_ast;
    }
    
    protected IASTLiteralExpression getUpperBound() {
        IASTExpression cond = m_forloop.getConditionExpression();
        if (cond instanceof IASTBinaryExpression) {
            IASTBinaryExpression binary_cond = (IASTBinaryExpression) cond;
            return (IASTLiteralExpression) (binary_cond.getChildren()[1]);
        } else {
            return null;
        }
    }
    
    protected int getUpperBoundValue() {
        return Integer.parseInt(new String(getUpperBound().getValue()));
    }
	
	/**
	 * This method (which baffles me as to why there isn't one of these
	 * in the IASTNode class, but whatever) returns the next sibling 
	 * after itself with respect to its parent.
	 * @param n node in which to find next sibling
	 * @return IASTNode of next sibling or null if last child
	 */
	protected IASTNode getNextSibling(IASTNode n) {
	    if (n.getParent() != null) {
    	    IASTNode[] chilluns = n.getParent().getChildren();
    	    for (int i = 0; i < chilluns.length; i++) {
    	        if (n == chilluns[i] && i < (chilluns.length-1)) {
    	            return chilluns[i+1];
    	        }
    	    }
	    }
	    return null;
	}
	
	//*************************************************************************
	
	/**
     * The NameVisitor visits all names within a tree. It concatenates
     * all names into a private variable called 'name_list'.
     */
    protected class NameVisitor extends ASTVisitor {
        private ArrayList<IASTName> name_list = null;
        
        public NameVisitor() {
            name_list = new ArrayList<IASTName>();
            //want to find names within access expressions
            shouldVisitNames = true;
        }
        
        public ArrayList<IASTName> getNames() {
            return name_list;
        }
        
        @Override
        public int visit(IASTName visitor) {
            name_list.add(visitor);
            return PROCESS_CONTINUE;
        }
    }
	
}
