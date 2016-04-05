package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class GenericASTVisitor implements IASTVisitor
{
    protected void traverseChildren(IASTNode node)
    {
        for (IASTNode child : node.getChildren())
            child.accept(this);
    }

    @Override public void visitASTNode(IASTNode node) { traverseChildren(node); }
    @Override public void visitToken(Token node) {}
    @Override public void visitASTListNode(IASTListNode<?> node) {}
    @Override public void visitASTAccAsyncClauseNode(ASTAccAsyncClauseNode node) {}
    @Override public void visitASTAccAtomicNode(ASTAccAtomicNode node) {}
    @Override public void visitASTAccCacheNode(ASTAccCacheNode node) {}
    @Override public void visitASTAccCollapseClauseNode(ASTAccCollapseClauseNode node) {}
    @Override public void visitASTAccCopyClauseNode(ASTAccCopyClauseNode node) {}
    @Override public void visitASTAccCopyinClauseNode(ASTAccCopyinClauseNode node) {}
    @Override public void visitASTAccCopyoutClauseNode(ASTAccCopyoutClauseNode node) {}
    @Override public void visitASTAccCountNode(ASTAccCountNode node) {}
    @Override public void visitASTAccCreateClauseNode(ASTAccCreateClauseNode node) {}
    @Override public void visitASTAccDataClauseListNode(ASTAccDataClauseListNode node) {}
    @Override public void visitASTAccDataItemNode(ASTAccDataItemNode node) {}
    @Override public void visitASTAccDataNode(ASTAccDataNode node) {}
    @Override public void visitASTAccDeclareClauseListNode(ASTAccDeclareClauseListNode node) {}
    @Override public void visitASTAccDeclareNode(ASTAccDeclareNode node) {}
    @Override public void visitASTAccDefaultnoneClauseNode(ASTAccDefaultnoneClauseNode node) {}
    @Override public void visitASTAccDeleteClauseNode(ASTAccDeleteClauseNode node) {}
    @Override public void visitASTAccDeviceClauseNode(ASTAccDeviceClauseNode node) {}
    @Override public void visitASTAccDeviceptrClauseNode(ASTAccDeviceptrClauseNode node) {}
    @Override public void visitASTAccDeviceresidentClauseNode(ASTAccDeviceresidentClauseNode node) {}
    @Override public void visitASTAccEnterDataClauseListNode(ASTAccEnterDataClauseListNode node) {}
    @Override public void visitASTAccEnterDataNode(ASTAccEnterDataNode node) {}
    @Override public void visitASTAccExitDataClauseListNode(ASTAccExitDataClauseListNode node) {}
    @Override public void visitASTAccExitDataNode(ASTAccExitDataNode node) {}
    @Override public void visitASTAccFirstprivateClauseNode(ASTAccFirstprivateClauseNode node) {}
    @Override public void visitASTAccGangClauseNode(ASTAccGangClauseNode node) {}
    @Override public void visitASTAccHostClauseNode(ASTAccHostClauseNode node) {}
    @Override public void visitASTAccHostdataClauseListNode(ASTAccHostdataClauseListNode node) {}
    @Override public void visitASTAccHostdataNode(ASTAccHostdataNode node) {}
    @Override public void visitASTAccIfClauseNode(ASTAccIfClauseNode node) {}
    @Override public void visitASTAccKernelsClauseListNode(ASTAccKernelsClauseListNode node) {}
    @Override public void visitASTAccKernelsLoopClauseListNode(ASTAccKernelsLoopClauseListNode node) {}
    @Override public void visitASTAccKernelsLoopNode(ASTAccKernelsLoopNode node) {}
    @Override public void visitASTAccKernelsNode(ASTAccKernelsNode node) {}
    @Override public void visitASTAccLinkClauseNode(ASTAccLinkClauseNode node) {}
    @Override public void visitASTAccLoopClauseListNode(ASTAccLoopClauseListNode node) {}
    @Override public void visitASTAccLoopNode(ASTAccLoopNode node) {}
    @Override public void visitASTAccNoConstruct(ASTAccNoConstruct node) {}
    @Override public void visitASTAccNumgangsClauseNode(ASTAccNumgangsClauseNode node) {}
    @Override public void visitASTAccNumworkersClauseNode(ASTAccNumworkersClauseNode node) {}
    @Override public void visitASTAccParallelClauseListNode(ASTAccParallelClauseListNode node) {}
    @Override public void visitASTAccParallelLoopClauseListNode(ASTAccParallelLoopClauseListNode node) {}
    @Override public void visitASTAccParallelLoopNode(ASTAccParallelLoopNode node) {}
    @Override public void visitASTAccParallelNode(ASTAccParallelNode node) {}
    @Override public void visitASTAccPresentClauseNode(ASTAccPresentClauseNode node) {}
    @Override public void visitASTAccPresentorcopyClauseNode(ASTAccPresentorcopyClauseNode node) {}
    @Override public void visitASTAccPresentorcopyinClauseNode(ASTAccPresentorcopyinClauseNode node) {}
    @Override public void visitASTAccPresentorcopyoutClauseNode(ASTAccPresentorcopyoutClauseNode node) {}
    @Override public void visitASTAccPresentorcreateClauseNode(ASTAccPresentorcreateClauseNode node) {}
    @Override public void visitASTAccPrivateClauseNode(ASTAccPrivateClauseNode node) {}
    @Override public void visitASTAccReductionClauseNode(ASTAccReductionClauseNode node) {}
    @Override public void visitASTAccReductionOperatorNode(ASTAccReductionOperatorNode node) {}
    @Override public void visitASTAccRoutineClauseListNode(ASTAccRoutineClauseListNode node) {}
    @Override public void visitASTAccRoutineNode(ASTAccRoutineNode node) {}
    @Override public void visitASTAccSelfClauseNode(ASTAccSelfClauseNode node) {}
    @Override public void visitASTAccTileClauseNode(ASTAccTileClauseNode node) {}
    @Override public void visitASTAccUpdateClauseListNode(ASTAccUpdateClauseListNode node) {}
    @Override public void visitASTAccUpdateNode(ASTAccUpdateNode node) {}
    @Override public void visitASTAccUsedeviceClauseNode(ASTAccUsedeviceClauseNode node) {}
    @Override public void visitASTAccVectorClauseNode(ASTAccVectorClauseNode node) {}
    @Override public void visitASTAccVectorlengthClauseNode(ASTAccVectorlengthClauseNode node) {}
    @Override public void visitASTAccWaitClauseListNode(ASTAccWaitClauseListNode node) {}
    @Override public void visitASTAccWaitClauseNode(ASTAccWaitClauseNode node) {}
    @Override public void visitASTAccWaitNode(ASTAccWaitNode node) {}
    @Override public void visitASTAccWaitParameterNode(ASTAccWaitParameterNode node) {}
    @Override public void visitASTAccWorkerClauseNode(ASTAccWorkerClauseNode node) {}
    @Override public void visitASTExpressionNode(ASTExpressionNode node) {}
    @Override public void visitASTIdentifierNode(ASTIdentifierNode node) {}
    @Override public void visitASTUnaryOperatorNode(ASTUnaryOperatorNode node) {}
    @Override public void visitCAccAtomicCaptureClause(CAccAtomicCaptureClause node) {}
    @Override public void visitCAccAtomicReadClause(CAccAtomicReadClause node) {}
    @Override public void visitCAccAtomicUpdateClause(CAccAtomicUpdateClause node) {}
    @Override public void visitCAccAtomicWriteClause(CAccAtomicWriteClause node) {}
    @Override public void visitCAccAutoClause(CAccAutoClause node) {}
    @Override public void visitCAccBindClause(CAccBindClause node) {}
    @Override public void visitCAccIndependentClause(CAccIndependentClause node) {}
    @Override public void visitCAccNoHostClause(CAccNoHostClause node) {}
    @Override public void visitCAccSeqClause(CAccSeqClause node) {}
    @Override public void visitCArrayAccessExpression(CArrayAccessExpression node) {}
    @Override public void visitCBinaryExpression(CBinaryExpression node) {}
    @Override public void visitCConstantExpression(CConstantExpression node) {}
    @Override public void visitCElementAccessExpression(CElementAccessExpression node) {}
    @Override public void visitCFunctionCallExpression(CFunctionCallExpression node) {}
    @Override public void visitCIdentifierExpression(CIdentifierExpression node) {}
    @Override public void visitCPostfixUnaryExpression(CPostfixUnaryExpression node) {}
    @Override public void visitCPrefixUnaryExpression(CPrefixUnaryExpression node) {}
    @Override public void visitCSizeofExpression(CSizeofExpression node) {}
    @Override public void visitCStringLiteralExpression(CStringLiteralExpression node) {}
    @Override public void visitCTernaryExpression(CTernaryExpression node) {}
    @Override public void visitIAccAtomicClause(IAccAtomicClause node) {}
    @Override public void visitIAccConstruct(IAccConstruct node) {}
    @Override public void visitIAccDataClause(IAccDataClause node) {}
    @Override public void visitIAccDeclareClause(IAccDeclareClause node) {}
    @Override public void visitIAccEnterDataClause(IAccEnterDataClause node) {}
    @Override public void visitIAccExitDataClause(IAccExitDataClause node) {}
    @Override public void visitIAccHostdataClause(IAccHostdataClause node) {}
    @Override public void visitIAccKernelsClause(IAccKernelsClause node) {}
    @Override public void visitIAccKernelsLoopClause(IAccKernelsLoopClause node) {}
    @Override public void visitIAccLoopClause(IAccLoopClause node) {}
    @Override public void visitIAccParallelClause(IAccParallelClause node) {}
    @Override public void visitIAccParallelLoopClause(IAccParallelLoopClause node) {}
    @Override public void visitIAccRoutineClause(IAccRoutineClause node) {}
    @Override public void visitIAccUpdateClause(IAccUpdateClause node) {}
    @Override public void visitIAssignmentExpression(IAssignmentExpression node) {}
    @Override public void visitICExpression(ICExpression node) {}
    @Override public void visitIConstantExpression(IConstantExpression node) {}
}
