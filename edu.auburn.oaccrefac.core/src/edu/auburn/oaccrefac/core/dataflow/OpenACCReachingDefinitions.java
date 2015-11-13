package edu.auburn.oaccrefac.core.dataflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.codan.core.cxx.internal.model.cfg.ControlFlowGraphBuilder;
import org.eclipse.cdt.codan.core.model.cfg.IBasicBlock;
import org.eclipse.cdt.codan.core.model.cfg.ICfgData;
import org.eclipse.cdt.codan.core.model.cfg.IControlFlowGraph;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;

import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;
import edu.auburn.oaccrefac.internal.core.Pair;
import edu.auburn.oaccrefac.internal.core.dependence.LinearExpression;
import edu.auburn.oaccrefac.internal.core.dependence.VariableAccess;

@SuppressWarnings("restriction")
public class OpenACCReachingDefinitions {

    private IControlFlowGraph cfg;
    
    private Map<VariableAccess, Set<IASTStatement>> reachingDefs;
    private Map<IBasicBlock, RDVarSet> entrySets;
    private Map<IBasicBlock, RDVarSet> exitSets;
    
    public OpenACCReachingDefinitions(IASTFunctionDefinition func) throws DependenceTestFailure {
        this.cfg = new ControlFlowGraphBuilder().build(func);
        
        this.reachingDefs = new HashMap<VariableAccess, Set<IASTStatement>>();
        this.entrySets = new HashMap<IBasicBlock, RDVarSet>();
        this.exitSets = new HashMap<IBasicBlock, RDVarSet>();
        
        identifyReachingDefinitions(cfg);
        
    }
    
    private void identifyReachingDefinitions(IControlFlowGraph cfg) throws DependenceTestFailure {
        boolean changed;
        do {
            changed = false;
            
            Map<IBasicBlock, RDVarSet> newEntrySets = new HashMap<IBasicBlock, RDVarSet>();
            Map<IBasicBlock, RDVarSet> newExitSets = new HashMap<IBasicBlock, RDVarSet>();
            
            for (IBasicBlock bb : cfg.getNodes()) {
                
                RDVarSet bbEntry = new RDVarSet();
                RDVarSet bbExit = new RDVarSet();

                for(IBasicBlock pred : bb.getIncomingNodes()) {
                    if(exitSets.containsKey(pred)) {
                        bbEntry.union(exitSets.get(pred));
                    }
                }

                if(entrySets.containsKey(bb)) {
                    bbExit.union(entrySets.get(bb));
                }
                for(VariableAccess write : varsWrittenToIn(bb)) {
                    bbExit.killAll(write.getVariableName().resolveBinding());
                }
                for(VariableAccess write : varsWrittenToIn(bb)) {
                    bbExit.add(write.getVariableName().resolveBinding(), write.getEnclosingStatement());
                }
                
                changed = changed || !areEqual(bbEntry, entrySets.get(bb)) || !areEqual(bbExit, exitSets.get(bb));
                
                newEntrySets.put(bb, bbEntry);
                newExitSets.put(bb, bbExit);
            }
            
            for(IBasicBlock bb : newEntrySets.keySet()) {
                entrySets.put(bb, newEntrySets.get(bb));
            }
            for(IBasicBlock bb : newExitSets.keySet()) {
                exitSets.put(bb, newExitSets.get(bb));
            }
            
            
        } while (changed);
        return;
    }
    
    //FIXME finish this
    public boolean reaches(IASTStatement definition, VariableAccess use) {
        for(IBasicBlock bb : entrySets.keySet()) {
            Object data = ((ICfgData) bb).getData();
            if (data == null || !(data instanceof IASTNode)) {
                return false;
            }
            if(data.equals(use.getEnclosingStatement())) {
                return entrySets.get(bb).get(use.getVariableName().resolveBinding()).contains(definition);
            }
        }
        return true;
    }
    
    //TODO be sure the collectAccesses methods support everything or use some other means to kill/gens
    private List<VariableAccess> varsWrittenToIn(IBasicBlock bb) throws DependenceTestFailure {
        List<VariableAccess> allAccesses = new ArrayList<VariableAccess>();
        List<VariableAccess> writeAccesses = new ArrayList<VariableAccess>();
        Object data = ((ICfgData) bb).getData();
        if (data == null || !(data instanceof IASTNode))
            return writeAccesses;
        
        if(data instanceof IASTStatement)
            collectAccessesFromStatements(allAccesses, (IASTStatement) data);
        else if(data instanceof IASTExpression)
            collectAccessesFromExpression((IASTExpression) data, allAccesses);
        
        for(VariableAccess access : allAccesses) {
            if(access.isWrite()) {
                writeAccesses.add(access);
            }
        }
        return writeAccesses;
    }
    
    private boolean areEqual(RDVarSet first, RDVarSet second) {
        
        if(first == null && second == null) {
            return true;
        }
        else if(first == null || second == null) {
            return false;
        }
        
        if(first.isEmpty() && second.isEmpty()) {
            return true;
        }
        else if(first.isEmpty() || second.isEmpty()) {
            return false;
        }
        
        for(IBinding var : first.getMap().keySet()) {
            if(second.getMap().containsKey(var)) {
                for(IASTStatement statement : first.getMap().get(var)) {
                    if(!second.getMap().get(var).contains(statement)) {
                        return false;
                    }
                }
            }
            else {
                return false;
            }
        }
        for(IBinding var : second.getMap().keySet()) {
            if(first.getMap().containsKey(var)) {
                for(IASTStatement statement : second.getMap().get(var)) {
                    if(!first.getMap().get(var).contains(statement)) {
                        return false;
                    }
                }
            }
            else {
                return false;
            }
        }
        return true;
    }
    
    private void collectAccessesFromStatements(List<VariableAccess> variableAccesses, IASTStatement... statements) throws DependenceTestFailure {
        for (IASTStatement stmt : statements) {
            if (stmt instanceof IASTDeclarationStatement) {
                collectAccessesFrom((IASTDeclarationStatement) stmt, variableAccesses);
            } else if (stmt instanceof IASTForStatement) {
                collectAccessesFrom((IASTForStatement) stmt, variableAccesses);
            } else if (stmt instanceof IASTExpressionStatement) {
                collectAccessesFrom((IASTExpressionStatement) stmt, variableAccesses);
            } else if (stmt instanceof IASTNullStatement) {
                collectAccessesFrom((IASTNullStatement) stmt, variableAccesses);
            } else if (stmt instanceof IASTCompoundStatement) {
                collectAccessesFrom((IASTCompoundStatement) stmt, variableAccesses);
            } else {
                throw unsupported(stmt);
            }
        }
    }

    private void collectAccessesFrom(IASTNullStatement stmt, List<VariableAccess> variableAccesses) {
        // Nothing to do
    }

    private void collectAccessesFrom(IASTCompoundStatement stmt, List<VariableAccess> variableAccesses) throws DependenceTestFailure {
        collectAccessesFromStatements(variableAccesses, stmt.getStatements());
    }

    private void collectAccessesFrom(IASTForStatement stmt, List<VariableAccess> variableAccesses) throws DependenceTestFailure {
        ForStatementInquisitor forLoop = InquisitorFactory.getInquisitor(stmt);
        if (!forLoop.isCountedLoop()) {
            throw unsupported(stmt);
        }

        collectAccessesFromStatements(variableAccesses, stmt.getBody());
    }

    private void collectAccessesFrom(IASTDeclarationStatement stmt, List<VariableAccess> variableAccesses) throws DependenceTestFailure {
        if (!(stmt.getDeclaration() instanceof IASTSimpleDeclaration))
            throw unsupported(stmt);

        IASTSimpleDeclaration simpleDecl = (IASTSimpleDeclaration) stmt.getDeclaration();
        for (IASTDeclarator declarator : simpleDecl.getDeclarators()) {
            if (declarator.getNestedDeclarator() != null || declarator.getPointerOperators().length > 0)
                throw unsupported(stmt);

            variableAccesses.add(new VariableAccess(true, declarator.getName()));

            if (declarator.getInitializer() != null) {
                if (!(declarator.getInitializer() instanceof IASTEqualsInitializer))
                    throw unsupported(stmt);
                IASTEqualsInitializer eqInit = (IASTEqualsInitializer) declarator.getInitializer();
                IASTInitializerClause initializer = eqInit.getInitializerClause();
                if (!(initializer instanceof IASTExpression))
                    throw unsupported(stmt);
                collectAccessesFromExpression((IASTExpression) initializer, variableAccesses);
            }
        }
    }

    private void collectAccessesFrom(IASTExpressionStatement stmt, List<VariableAccess> variableAccesses) throws DependenceTestFailure {
        Pair<IASTExpression, IASTExpression> asgt = ASTUtil.getAssignment(stmt.getExpression());
        if (asgt != null) {
            collectAccessesFromAssignmentLHS(asgt.getFirst(), variableAccesses);
            collectAccessesFromExpression(asgt.getSecond(), variableAccesses);
            return;
        }

        Pair<IASTExpression, IASTExpression> assignEq = ASTUtil.getAssignEq(stmt.getExpression());
        if (assignEq != null) {
            // The variable on the LHS is both read and written
            collectAccessesFromAssignmentLHS(assignEq.getFirst(), variableAccesses);
            collectAccessesFromExpression(assignEq.getFirst(), variableAccesses);
            collectAccessesFromExpression(assignEq.getSecond(), variableAccesses);
            return;
        }

        IASTExpression incrDecr = ASTUtil.getIncrDecr(stmt.getExpression());
        if (incrDecr != null) {
            // The incremented variable/array element is both read and written
            collectAccessesFromAssignmentLHS(incrDecr, variableAccesses);
            collectAccessesFromExpression(incrDecr, variableAccesses);
            return;
        }

        throw unsupported(stmt);
    }

    private void collectAccessesFromAssignmentLHS(IASTExpression expr, List<VariableAccess> variableAccesses) throws DependenceTestFailure {
        IASTName scalar = ASTUtil.getIdExpression(expr);
        if (scalar != null) {
            variableAccesses.add(new VariableAccess(true, scalar));
            return;
        }

        Pair<IASTName, IASTName> fieldReference = ASTUtil.getSimpleFieldReference(expr);
        if (fieldReference != null) {
            IASTName owner = fieldReference.getFirst();
            IASTName field = fieldReference.getSecond();
            variableAccesses.add(new VariableAccess(false, owner));
            variableAccesses.add(new VariableAccess(true, field));
            return;
        }

        Pair<IASTName, LinearExpression[]> arrayAccess = ASTUtil.getMultidimArrayAccess(expr);
        if (arrayAccess != null) {
            variableAccesses.add(new VariableAccess(true, arrayAccess.getFirst(), arrayAccess.getSecond()));
            return;
        }

        throw unsupported(expr);
    }

    private void collectAccessesFromExpression(IASTExpression expr, List<VariableAccess> variableAccesses) throws DependenceTestFailure {
        if (expr instanceof IASTBinaryExpression) {
            collectAccessesFrom((IASTBinaryExpression) expr, variableAccesses);
        } else if (expr instanceof IASTUnaryExpression) {
            collectAccessesFrom((IASTUnaryExpression) expr, variableAccesses);
        } else if (expr instanceof IASTLiteralExpression) {
            collectAccessesFrom((IASTLiteralExpression) expr, variableAccesses);
        } else if (expr instanceof IASTIdExpression) {
            collectAccessesFrom((IASTIdExpression) expr, variableAccesses);
        } else if (expr instanceof IASTArraySubscriptExpression) {
            collectAccessesFrom((IASTArraySubscriptExpression) expr, variableAccesses);
        } else if (expr instanceof IASTFieldReference) {
            collectAccessesFrom((IASTFieldReference) expr, variableAccesses);
        } else {
            throw unsupported(expr);
        }
    }

    //TODO add support for assignments
    private void collectAccessesFrom(IASTBinaryExpression expr, List<VariableAccess> variableAccesses) throws DependenceTestFailure {
        switch (expr.getOperator()) {
        case IASTBinaryExpression.op_plus:
        case IASTBinaryExpression.op_minus:
        case IASTBinaryExpression.op_multiply:
        case IASTBinaryExpression.op_divide:
        case IASTBinaryExpression.op_modulo:
        case IASTBinaryExpression.op_binaryAnd:
        case IASTBinaryExpression.op_binaryOr:
        case IASTBinaryExpression.op_binaryXor:
        case IASTBinaryExpression.op_shiftLeft:
        case IASTBinaryExpression.op_shiftRight:
        case IASTBinaryExpression.op_greaterThan:
        case IASTBinaryExpression.op_greaterEqual:
        case IASTBinaryExpression.op_lessThan:
        case IASTBinaryExpression.op_lessEqual:
        case IASTBinaryExpression.op_equals:
        case IASTBinaryExpression.op_notequals:
            collectAccessesFromExpression(expr.getOperand1(), variableAccesses);
            collectAccessesFromExpression(expr.getOperand2(), variableAccesses);
            break;
        default:
            throw unsupported(expr);
        }
    }

    private void collectAccessesFrom(IASTUnaryExpression expr, List<VariableAccess> variableAccesses) throws DependenceTestFailure {
        switch (expr.getOperator()) {
        case IASTUnaryExpression.op_bracketedPrimary:
        case IASTUnaryExpression.op_plus:
        case IASTUnaryExpression.op_minus:
        case IASTUnaryExpression.op_not:
        case IASTUnaryExpression.op_tilde:
            collectAccessesFromExpression(expr.getOperand(), variableAccesses);
            break;
        default:
            throw unsupported(expr);
        }
    }

    private void collectAccessesFrom(IASTLiteralExpression expr, List<VariableAccess> variableAccesses) {
        // Nothing to do
    }

    private void collectAccessesFrom(IASTIdExpression expr, List<VariableAccess> variableAccesses) throws DependenceTestFailure {
        IASTName name = ASTUtil.getIdExpression(expr);
        if (name == null)
            throw unsupported(expr);
        variableAccesses.add(new VariableAccess(false, name));
    }

    private void collectAccessesFrom(IASTFieldReference expr, List<VariableAccess> variableAccesses) throws DependenceTestFailure {
        Pair<IASTName, IASTName> pair = ASTUtil.getSimpleFieldReference(expr);
        if (pair == null)
            throw unsupported(expr);

        IASTName owner = pair.getFirst();
        IASTName field = pair.getSecond();
        variableAccesses.add(new VariableAccess(false, owner));
        variableAccesses.add(new VariableAccess(false, field));
    }

    private void collectAccessesFrom(IASTArraySubscriptExpression expr, List<VariableAccess> variableAccesses) throws DependenceTestFailure {
        Pair<IASTName, LinearExpression[]> arrayAccess = ASTUtil.getMultidimArrayAccess(expr);
        if (arrayAccess == null)
            throw unsupported(expr);
        variableAccesses.add(new VariableAccess(false, arrayAccess.getFirst(), arrayAccess.getSecond()));

        collectAccessesFromExpression((IASTExpression) expr.getArgument(), variableAccesses);
    }

    private static DependenceTestFailure unsupported(IASTNode node) {
        return new DependenceTestFailure(String.format("Unsupported construct on line %d (%s) - %s",
                node.getFileLocation().getStartingLineNumber(), //
                node.getClass().getSimpleName(), ASTUtil.toString(node)));
    }
  
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(IBasicBlock bb : cfg.getNodes()) {
            Object data = ((ICfgData) bb).getData();
            if (data != null && data instanceof IASTNode) {
                sb.append(((IASTNode) data).getRawSignature() + " at " + ((IASTNode) data).getFileLocation().getStartingLineNumber() + ":");
                
                sb.append(System.lineSeparator());
                
                sb.append("\tEntries: ");
                if(entrySets.containsKey(bb)) {
                    sb.append(entrySets.get(bb));
                }
                
                sb.append(System.lineSeparator());
                
                sb.append("\tExits: ");
                if(exitSets.containsKey(bb)) {
                    sb.append(exitSets.get(bb));
                }
                
                sb.append(System.lineSeparator());
                
            }
        }
        return sb.toString();
    }
    
}
