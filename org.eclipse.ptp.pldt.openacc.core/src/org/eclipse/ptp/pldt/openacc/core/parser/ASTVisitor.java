package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTVisitor implements IASTVisitor
{
    protected void traverseChildren(IASTNode node)
    {
        for (IASTNode child : node.getChildren())
            child.accept(this);
    }

    @Override public void visitASTNode(IASTNode node) {}
    @Override public void visitToken(Token node) {}
    @Override public void visitASTListNode(IASTListNode<?> node) { traverseChildren(node); }
    @Override public void visitASTAccAsyncClauseNode(ASTAccAsyncClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccAtomicNode(ASTAccAtomicNode node) { traverseChildren(node); }
    @Override public void visitASTAccCacheNode(ASTAccCacheNode node) { traverseChildren(node); }
    @Override public void visitASTAccCollapseClauseNode(ASTAccCollapseClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccCopyClauseNode(ASTAccCopyClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccCopyinClauseNode(ASTAccCopyinClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccCopyoutClauseNode(ASTAccCopyoutClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccCountNode(ASTAccCountNode node) { traverseChildren(node); }
    @Override public void visitASTAccCreateClauseNode(ASTAccCreateClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccDataClauseListNode(ASTAccDataClauseListNode node) { traverseChildren(node); }
    @Override public void visitASTAccDataItemNode(ASTAccDataItemNode node) { traverseChildren(node); }
    @Override public void visitASTAccDataNode(ASTAccDataNode node) { traverseChildren(node); }
    @Override public void visitASTAccDeclareClauseListNode(ASTAccDeclareClauseListNode node) { traverseChildren(node); }
    @Override public void visitASTAccDeclareNode(ASTAccDeclareNode node) { traverseChildren(node); }
    @Override public void visitASTAccDefaultnoneClauseNode(ASTAccDefaultnoneClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccDeleteClauseNode(ASTAccDeleteClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccDeviceClauseNode(ASTAccDeviceClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccDeviceptrClauseNode(ASTAccDeviceptrClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccDeviceresidentClauseNode(ASTAccDeviceresidentClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccEnterDataClauseListNode(ASTAccEnterDataClauseListNode node) { traverseChildren(node); }
    @Override public void visitASTAccEnterDataNode(ASTAccEnterDataNode node) { traverseChildren(node); }
    @Override public void visitASTAccExitDataClauseListNode(ASTAccExitDataClauseListNode node) { traverseChildren(node); }
    @Override public void visitASTAccExitDataNode(ASTAccExitDataNode node) { traverseChildren(node); }
    @Override public void visitASTAccFirstprivateClauseNode(ASTAccFirstprivateClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccGangClauseNode(ASTAccGangClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccHostClauseNode(ASTAccHostClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccHostdataClauseListNode(ASTAccHostdataClauseListNode node) { traverseChildren(node); }
    @Override public void visitASTAccHostdataNode(ASTAccHostdataNode node) { traverseChildren(node); }
    @Override public void visitASTAccIfClauseNode(ASTAccIfClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccKernelsClauseListNode(ASTAccKernelsClauseListNode node) { traverseChildren(node); }
    @Override public void visitASTAccKernelsLoopClauseListNode(ASTAccKernelsLoopClauseListNode node) { traverseChildren(node); }
    @Override public void visitASTAccKernelsLoopNode(ASTAccKernelsLoopNode node) { traverseChildren(node); }
    @Override public void visitASTAccKernelsNode(ASTAccKernelsNode node) { traverseChildren(node); }
    @Override public void visitASTAccLinkClauseNode(ASTAccLinkClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccLoopClauseListNode(ASTAccLoopClauseListNode node) { traverseChildren(node); }
    @Override public void visitASTAccLoopNode(ASTAccLoopNode node) { traverseChildren(node); }
    @Override public void visitASTAccNoConstruct(ASTAccNoConstruct node) { traverseChildren(node); }
    @Override public void visitASTAccNumgangsClauseNode(ASTAccNumgangsClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccNumworkersClauseNode(ASTAccNumworkersClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccParallelClauseListNode(ASTAccParallelClauseListNode node) { traverseChildren(node); }
    @Override public void visitASTAccParallelLoopClauseListNode(ASTAccParallelLoopClauseListNode node) { traverseChildren(node); }
    @Override public void visitASTAccParallelLoopNode(ASTAccParallelLoopNode node) { traverseChildren(node); }
    @Override public void visitASTAccParallelNode(ASTAccParallelNode node) { traverseChildren(node); }
    @Override public void visitASTAccPresentClauseNode(ASTAccPresentClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccPresentorcopyClauseNode(ASTAccPresentorcopyClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccPresentorcopyinClauseNode(ASTAccPresentorcopyinClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccPresentorcopyoutClauseNode(ASTAccPresentorcopyoutClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccPresentorcreateClauseNode(ASTAccPresentorcreateClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccPrivateClauseNode(ASTAccPrivateClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccReductionClauseNode(ASTAccReductionClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccReductionOperatorNode(ASTAccReductionOperatorNode node) { traverseChildren(node); }
    @Override public void visitASTAccRoutineClauseListNode(ASTAccRoutineClauseListNode node) { traverseChildren(node); }
    @Override public void visitASTAccRoutineNode(ASTAccRoutineNode node) { traverseChildren(node); }
    @Override public void visitASTAccSelfClauseNode(ASTAccSelfClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccTileClauseNode(ASTAccTileClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccUpdateClauseListNode(ASTAccUpdateClauseListNode node) { traverseChildren(node); }
    @Override public void visitASTAccUpdateNode(ASTAccUpdateNode node) { traverseChildren(node); }
    @Override public void visitASTAccUsedeviceClauseNode(ASTAccUsedeviceClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccVectorClauseNode(ASTAccVectorClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccVectorlengthClauseNode(ASTAccVectorlengthClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccWaitClauseListNode(ASTAccWaitClauseListNode node) { traverseChildren(node); }
    @Override public void visitASTAccWaitClauseNode(ASTAccWaitClauseNode node) { traverseChildren(node); }
    @Override public void visitASTAccWaitNode(ASTAccWaitNode node) { traverseChildren(node); }
    @Override public void visitASTAccWaitParameterNode(ASTAccWaitParameterNode node) { traverseChildren(node); }
    @Override public void visitASTAccWorkerClauseNode(ASTAccWorkerClauseNode node) { traverseChildren(node); }
    @Override public void visitASTExpressionNode(ASTExpressionNode node) { traverseChildren(node); }
    @Override public void visitASTIdentifierNode(ASTIdentifierNode node) { traverseChildren(node); }
    @Override public void visitASTUnaryOperatorNode(ASTUnaryOperatorNode node) { traverseChildren(node); }
    @Override public void visitCAccAtomicCaptureClause(CAccAtomicCaptureClause node) { traverseChildren(node); }
    @Override public void visitCAccAtomicReadClause(CAccAtomicReadClause node) { traverseChildren(node); }
    @Override public void visitCAccAtomicUpdateClause(CAccAtomicUpdateClause node) { traverseChildren(node); }
    @Override public void visitCAccAtomicWriteClause(CAccAtomicWriteClause node) { traverseChildren(node); }
    @Override public void visitCAccAutoClause(CAccAutoClause node) { traverseChildren(node); }
    @Override public void visitCAccBindClause(CAccBindClause node) { traverseChildren(node); }
    @Override public void visitCAccIndependentClause(CAccIndependentClause node) { traverseChildren(node); }
    @Override public void visitCAccNoHostClause(CAccNoHostClause node) { traverseChildren(node); }
    @Override public void visitCAccSeqClause(CAccSeqClause node) { traverseChildren(node); }
    @Override public void visitCArrayAccessExpression(CArrayAccessExpression node) { traverseChildren(node); }
    @Override public void visitCBinaryExpression(CBinaryExpression node) { traverseChildren(node); }
    @Override public void visitCConstantExpression(CConstantExpression node) { traverseChildren(node); }
    @Override public void visitCElementAccessExpression(CElementAccessExpression node) { traverseChildren(node); }
    @Override public void visitCFunctionCallExpression(CFunctionCallExpression node) { traverseChildren(node); }
    @Override public void visitCIdentifierExpression(CIdentifierExpression node) { traverseChildren(node); }
    @Override public void visitCPostfixUnaryExpression(CPostfixUnaryExpression node) { traverseChildren(node); }
    @Override public void visitCPrefixUnaryExpression(CPrefixUnaryExpression node) { traverseChildren(node); }
    @Override public void visitCSizeofExpression(CSizeofExpression node) { traverseChildren(node); }
    @Override public void visitCStringLiteralExpression(CStringLiteralExpression node) { traverseChildren(node); }
    @Override public void visitCTernaryExpression(CTernaryExpression node) { traverseChildren(node); }
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
