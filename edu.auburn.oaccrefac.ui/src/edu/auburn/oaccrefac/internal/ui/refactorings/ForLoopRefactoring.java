package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.ASTNode;
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
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

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

    
	public ForLoopRefactoring(ICElement element, ISelection selection, ICProject project) {
		super(element, selection, project);
		
		if (selection == null || tu.getResource() == null || project == null)
            initStatus.addFatalError("Invalid selection");
	}

	@Override
	protected abstract void collectModifications(IProgressMonitor pm, ModificationCollector collector)
			throws CoreException, OperationCanceledException;

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
        
		return initStatus;
	}

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
	 * Since the loop is found via 'findLoop', we are already
	 * assured that the loop is of type CASTForStatement, so
	 * we don't have to check every time.
	 * @return The selected for-loop for refactoring
	 */
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
	
	protected boolean supportedPattern(IASTForStatement matchee) throws CoreException {
	   /* 
	    for (String pattern : patterns) {
	        IASTForStatement forLoop = (IASTForStatement) ASTUtil.parseStatement(pattern);
	        IASTForStatement pattern_ast = forLoop.copy(CopyStyle.withoutLocations);
	        
	        if (pattern_ast.getInitializerStatement() instanceof IASTExpressionStatement)
	            ((IASTBinaryExpression)((IASTExpressionStatement)pattern_ast.getInitializerStatement()).getExpression()).setOperand2(new ArbitraryIntegerConstant());
	        else if (pattern_ast.getInitializerStatement() instanceof IASTDeclarationStatement) {
	            IASTDeclaration d = ((IASTDeclarationStatement)pattern_ast.getInitializerStatement()).getDeclaration();
	            IASTSimpleDeclaration sd = (IASTSimpleDeclaration)d;
	            IASTDeclarator decl = sd.getDeclarators()[0];
	            //This is really tedious...I know...thanks, CDT.
	            ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
	            decl.setInitializer(factory.newEqualsInitializer(new ArbitraryIntegerConstant()));
	            
	        }
	        ((IASTBinaryExpression)pattern_ast.getConditionExpression()).setOperand2(new ArbitraryIntegerConstant());
	        
	        //change iteration constant to arbitrary in cases of:
	        //     i+=<constant> and i=i+<constant>
	        if (pattern_ast.getIterationExpression() instanceof IASTBinaryExpression) {
    	        IASTBinaryExpression bin = (IASTBinaryExpression)pattern_ast.getIterationExpression();
    	        if (bin.getOperand2() instanceof IASTLiteralExpression) {
    	            //case: i+=<constant>
    	            bin.setOperand2(new ArbitraryIntegerConstant());
    	        } else {
    	            //case: i=i+<constant>
    	            IASTBinaryExpression plus = (IASTBinaryExpression)bin.getOperand2();
    	            plus.setOperand2(new ArbitraryIntegerConstant());
    	        }
	        }
	        pattern_ast.setBody(new ArbitraryStatement());
	        if (ASTMatcher.unify(pattern_ast, matchee) != null)
	            return true;
	    }
	    return false;
	    */
	    return true;
	} 
	
	protected IASTNode getNextSibling(IASTNode n) {
	    IASTNode[] chilluns = n.getParent().getChildren();
	    for (int i = 0; i < chilluns.length; i++) {
	        if (n == chilluns[i] && i < (chilluns.length-1)) {
	            return chilluns[i+1];
	        }
	    }
	    return null;
	}
}
