package edu.auburn.oaccrefac.internal.ui.refactorings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dataflow.ReachingDefinitions;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;
import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.IntroduceDataConstructCheck;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

@SuppressWarnings("restriction")
public class IntroduceDataConstructRefactoring extends StatementsRefactoring {

    private IntroduceDataConstructCheck check;
    
    public IntroduceDataConstructRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        
        if (selection == null || tu.getResource() == null || project == null)
            initStatus.addFatalError("Invalid selection");
    }

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null;
    }
    
    @Override
    public void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new IntroduceDataConstructCheck(getStatements(), getStatementsAndComments());
        check.performChecks(initStatus, pm, null);
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
//        new IntroduceDataConstructAlteration(rewriter, check).change();
//        IASTFunctionDefinition f = ASTUtil.findNearestAncestor(getStatements()[0], IASTFunctionDefinition.class);
//        System.out.println(ASTUtil.isNameInScope("i", ((IASTCompoundStatement) f.getBody()).getScope()));
//        System.out.println(ASTUtil.isNameInScope("m", ((IASTCompoundStatement) ((IASTForStatement) getStatements()[0]).getBody()).getScope()));
        ReachingDefinitions rd;
        try {
            rd = new ReachingDefinitions(ASTUtil.findNearestAncestor(getStatements()[0], IASTFunctionDefinition.class));
        } catch (DependenceTestFailure e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        //i, j, a
        class NameGetter extends ASTVisitor {
            public Set<IASTName> names;
            NameGetter() {
                names = new HashSet<IASTName>();
                shouldVisitNames = true;
            }
            @Override
            public int visit(IASTName name) {
                names.add(name);
                return PROCESS_CONTINUE;
            }
        }
        
        class StmtExprGetter extends ASTVisitor {
            public Set<IASTNode> nodes;
            StmtExprGetter() {
                nodes = new HashSet<IASTNode>();
                shouldVisitStatements = true;
                shouldVisitExpressions = true;
            }
            @Override
            public int visit(IASTStatement name) {
                nodes.add(name);
                return PROCESS_CONTINUE;
            }
            @Override
            public int visit(IASTExpression name) {
                nodes.add(name);
                return PROCESS_CONTINUE;
            }
        }
        
        IASTTranslationUnit tu = getStatements()[0].getTranslationUnit();
        NameGetter nameGetter = new NameGetter();
        StmtExprGetter stmtExprGetter = new StmtExprGetter();
        tu.accept(nameGetter);
        tu.accept(stmtExprGetter);
        for(IASTName name : nameGetter.names) {
            System.out.println(name + " " + name.getFileLocation().getStartingLineNumber());
            for(IASTNode node : stmtExprGetter.nodes) {
                try {
                    boolean b = rd.reaches(name, node);
                    System.out.println(node.getClass().getSimpleName() + " at line " + node.getFileLocation().getStartingLineNumber());
                    System.out.println("\t" + b);
                }
                catch(Exception e) {
                    
                }
            }
        }
    }

}
