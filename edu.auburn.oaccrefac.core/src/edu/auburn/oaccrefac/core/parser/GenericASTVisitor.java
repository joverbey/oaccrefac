package edu.auburn.oaccrefac.core.parser;

import edu.auburn.oaccrefac.core.parser.SyntaxException;                   import java.io.IOException;

@SuppressWarnings("all")
public class GenericASTVisitor implements IASTVisitor
{
    protected void traverseChildren(IASTNode node)
    {
        for (IASTNode child : node.getChildren())
            child.accept(this);
    }

    public void visitASTNode(IASTNode node) { traverseChildren(node); }
    public void visitToken(Token node) {}
    public void visitASTListNode(IASTListNode<?> node) {}
    public void visitASTAccAsyncClauseNode(ASTAccAsyncClauseNode node) {}
    public void visitASTAccAtomicNode(ASTAccAtomicNode node) {}
    public void visitASTAccCacheNode(ASTAccCacheNode node) {}
    public void visitASTAccCollapseClauseNode(ASTAccCollapseClauseNode node) {}
    public void visitASTAccCopyClauseNode(ASTAccCopyClauseNode node) {}
    public void visitASTAccCopyinClauseNode(ASTAccCopyinClauseNode node) {}
    public void visitASTAccCopyoutClauseNode(ASTAccCopyoutClauseNode node) {}
    public void visitASTAccCountNode(ASTAccCountNode node) {}
    public void visitASTAccCreateClauseNode(ASTAccCreateClauseNode node) {}
    public void visitASTAccDataClauseListNode(ASTAccDataClauseListNode node) {}
    public void visitASTAccDataItemNode(ASTAccDataItemNode node) {}
    public void visitASTAccDataNode(ASTAccDataNode node) {}
    public void visitASTAccDeclareClauseListNode(ASTAccDeclareClauseListNode node) {}
    public void visitASTAccDeclareNode(ASTAccDeclareNode node) {}
    public void visitASTAccDefaultnoneClauseNode(ASTAccDefaultnoneClauseNode node) {}
    public void visitASTAccDeleteClauseNode(ASTAccDeleteClauseNode node) {}
    public void visitASTAccDeviceClauseNode(ASTAccDeviceClauseNode node) {}
    public void visitASTAccDeviceptrClauseNode(ASTAccDeviceptrClauseNode node) {}
    public void visitASTAccDeviceresidentClauseNode(ASTAccDeviceresidentClauseNode node) {}
    public void visitASTAccEnterDataClauseListNode(ASTAccEnterDataClauseListNode node) {}
    public void visitASTAccEnterDataNode(ASTAccEnterDataNode node) {}
    public void visitASTAccExitDataClauseListNode(ASTAccExitDataClauseListNode node) {}
    public void visitASTAccExitDataNode(ASTAccExitDataNode node) {}
    public void visitASTAccFirstprivateClauseNode(ASTAccFirstprivateClauseNode node) {}
    public void visitASTAccGangClauseNode(ASTAccGangClauseNode node) {}
    public void visitASTAccHostClauseNode(ASTAccHostClauseNode node) {}
    public void visitASTAccHostdataClauseListNode(ASTAccHostdataClauseListNode node) {}
    public void visitASTAccHostdataNode(ASTAccHostdataNode node) {}
    public void visitASTAccIfClauseNode(ASTAccIfClauseNode node) {}
    public void visitASTAccKernelsClauseListNode(ASTAccKernelsClauseListNode node) {}
    public void visitASTAccKernelsLoopClauseListNode(ASTAccKernelsLoopClauseListNode node) {}
    public void visitASTAccKernelsLoopNode(ASTAccKernelsLoopNode node) {}
    public void visitASTAccKernelsNode(ASTAccKernelsNode node) {}
    public void visitASTAccLinkClauseNode(ASTAccLinkClauseNode node) {}
    public void visitASTAccLoopClauseListNode(ASTAccLoopClauseListNode node) {}
    public void visitASTAccLoopNode(ASTAccLoopNode node) {}
    public void visitASTAccNoConstruct(ASTAccNoConstruct node) {}
    public void visitASTAccNumgangsClauseNode(ASTAccNumgangsClauseNode node) {}
    public void visitASTAccNumworkersClauseNode(ASTAccNumworkersClauseNode node) {}
    public void visitASTAccParallelClauseListNode(ASTAccParallelClauseListNode node) {}
    public void visitASTAccParallelLoopClauseListNode(ASTAccParallelLoopClauseListNode node) {}
    public void visitASTAccParallelLoopNode(ASTAccParallelLoopNode node) {}
    public void visitASTAccParallelNode(ASTAccParallelNode node) {}
    public void visitASTAccPresentClauseNode(ASTAccPresentClauseNode node) {}
    public void visitASTAccPresentorcopyClauseNode(ASTAccPresentorcopyClauseNode node) {}
    public void visitASTAccPresentorcopyinClauseNode(ASTAccPresentorcopyinClauseNode node) {}
    public void visitASTAccPresentorcopyoutClauseNode(ASTAccPresentorcopyoutClauseNode node) {}
    public void visitASTAccPresentorcreateClauseNode(ASTAccPresentorcreateClauseNode node) {}
    public void visitASTAccPrivateClauseNode(ASTAccPrivateClauseNode node) {}
    public void visitASTAccReductionClauseNode(ASTAccReductionClauseNode node) {}
    public void visitASTAccReductionOperatorNode(ASTAccReductionOperatorNode node) {}
    public void visitASTAccRoutineClauseListNode(ASTAccRoutineClauseListNode node) {}
    public void visitASTAccRoutineNode(ASTAccRoutineNode node) {}
    public void visitASTAccSelfClauseNode(ASTAccSelfClauseNode node) {}
    public void visitASTAccTileClauseNode(ASTAccTileClauseNode node) {}
    public void visitASTAccUpdateClauseListNode(ASTAccUpdateClauseListNode node) {}
    public void visitASTAccUpdateNode(ASTAccUpdateNode node) {}
    public void visitASTAccUsedeviceClauseNode(ASTAccUsedeviceClauseNode node) {}
    public void visitASTAccVectorClauseNode(ASTAccVectorClauseNode node) {}
    public void visitASTAccVectorlengthClauseNode(ASTAccVectorlengthClauseNode node) {}
    public void visitASTAccWaitClauseListNode(ASTAccWaitClauseListNode node) {}
    public void visitASTAccWaitClauseNode(ASTAccWaitClauseNode node) {}
    public void visitASTAccWaitNode(ASTAccWaitNode node) {}
    public void visitASTAccWaitParameterNode(ASTAccWaitParameterNode node) {}
    public void visitASTAccWorkerClauseNode(ASTAccWorkerClauseNode node) {}
    public void visitASTExpressionNode(ASTExpressionNode node) {}
    public void visitASTIdentifierNode(ASTIdentifierNode node) {}
    public void visitASTUnaryOperatorNode(ASTUnaryOperatorNode node) {}
    public void visitCAccAtomicCaptureClause(CAccAtomicCaptureClause node) {}
    public void visitCAccAtomicReadClause(CAccAtomicReadClause node) {}
    public void visitCAccAtomicUpdateClause(CAccAtomicUpdateClause node) {}
    public void visitCAccAtomicWriteClause(CAccAtomicWriteClause node) {}
    public void visitCAccAutoClause(CAccAutoClause node) {}
    public void visitCAccBindClause(CAccBindClause node) {}
    public void visitCAccIndependentClause(CAccIndependentClause node) {}
    public void visitCAccNoHostClause(CAccNoHostClause node) {}
    public void visitCAccSeqClause(CAccSeqClause node) {}
    public void visitCArrayAccessExpression(CArrayAccessExpression node) {}
    public void visitCBinaryExpression(CBinaryExpression node) {}
    public void visitCConstantExpression(CConstantExpression node) {}
    public void visitCElementAccessExpression(CElementAccessExpression node) {}
    public void visitCFunctionCallExpression(CFunctionCallExpression node) {}
    public void visitCIdentifierExpression(CIdentifierExpression node) {}
    public void visitCPostfixUnaryExpression(CPostfixUnaryExpression node) {}
    public void visitCPrefixUnaryExpression(CPrefixUnaryExpression node) {}
    public void visitCSizeofExpression(CSizeofExpression node) {}
    public void visitCStringLiteralExpression(CStringLiteralExpression node) {}
    public void visitCTernaryExpression(CTernaryExpression node) {}
    public void visitIAccAtomicClause(IAccAtomicClause node) {}
    public void visitIAccConstruct(IAccConstruct node) {}
    public void visitIAccDataClause(IAccDataClause node) {}
    public void visitIAccDeclareClause(IAccDeclareClause node) {}
    public void visitIAccEnterDataClause(IAccEnterDataClause node) {}
    public void visitIAccExitDataClause(IAccExitDataClause node) {}
    public void visitIAccHostdataClause(IAccHostdataClause node) {}
    public void visitIAccKernelsClause(IAccKernelsClause node) {}
    public void visitIAccKernelsLoopClause(IAccKernelsLoopClause node) {}
    public void visitIAccLoopClause(IAccLoopClause node) {}
    public void visitIAccParallelClause(IAccParallelClause node) {}
    public void visitIAccParallelLoopClause(IAccParallelLoopClause node) {}
    public void visitIAccRoutineClause(IAccRoutineClause node) {}
    public void visitIAccUpdateClause(IAccUpdateClause node) {}
    public void visitIAssignmentExpression(IAssignmentExpression node) {}
    public void visitICExpression(ICExpression node) {}
    public void visitIConstantExpression(IConstantExpression node) {}
}
