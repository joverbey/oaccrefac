package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTVisitor implements IASTVisitor
{
    protected void traverseChildren(IASTNode node)
    {
        for (IASTNode child : node.getChildren())
            child.accept(this);
    }

    public void visitASTNode(IASTNode node) {}
    public void visitToken(Token node) {}
    public void visitASTListNode(IASTListNode<?> node) { traverseChildren(node); }
    public void visitASTAccAsyncClauseNode(ASTAccAsyncClauseNode node) { traverseChildren(node); }
    public void visitASTAccAtomicNode(ASTAccAtomicNode node) { traverseChildren(node); }
    public void visitASTAccCacheNode(ASTAccCacheNode node) { traverseChildren(node); }
    public void visitASTAccCollapseClauseNode(ASTAccCollapseClauseNode node) { traverseChildren(node); }
    public void visitASTAccCopyClauseNode(ASTAccCopyClauseNode node) { traverseChildren(node); }
    public void visitASTAccCopyinClauseNode(ASTAccCopyinClauseNode node) { traverseChildren(node); }
    public void visitASTAccCopyoutClauseNode(ASTAccCopyoutClauseNode node) { traverseChildren(node); }
    public void visitASTAccCountNode(ASTAccCountNode node) { traverseChildren(node); }
    public void visitASTAccCreateClauseNode(ASTAccCreateClauseNode node) { traverseChildren(node); }
    public void visitASTAccDataClauseListNode(ASTAccDataClauseListNode node) { traverseChildren(node); }
    public void visitASTAccDataItemNode(ASTAccDataItemNode node) { traverseChildren(node); }
    public void visitASTAccDataNode(ASTAccDataNode node) { traverseChildren(node); }
    public void visitASTAccDeclareClauseListNode(ASTAccDeclareClauseListNode node) { traverseChildren(node); }
    public void visitASTAccDeclareNode(ASTAccDeclareNode node) { traverseChildren(node); }
    public void visitASTAccDefaultnoneClauseNode(ASTAccDefaultnoneClauseNode node) { traverseChildren(node); }
    public void visitASTAccDeleteClauseNode(ASTAccDeleteClauseNode node) { traverseChildren(node); }
    public void visitASTAccDeviceClauseNode(ASTAccDeviceClauseNode node) { traverseChildren(node); }
    public void visitASTAccDeviceptrClauseNode(ASTAccDeviceptrClauseNode node) { traverseChildren(node); }
    public void visitASTAccDeviceresidentClauseNode(ASTAccDeviceresidentClauseNode node) { traverseChildren(node); }
    public void visitASTAccEnterDataClauseListNode(ASTAccEnterDataClauseListNode node) { traverseChildren(node); }
    public void visitASTAccEnterDataNode(ASTAccEnterDataNode node) { traverseChildren(node); }
    public void visitASTAccExitDataClauseListNode(ASTAccExitDataClauseListNode node) { traverseChildren(node); }
    public void visitASTAccExitDataNode(ASTAccExitDataNode node) { traverseChildren(node); }
    public void visitASTAccFirstprivateClauseNode(ASTAccFirstprivateClauseNode node) { traverseChildren(node); }
    public void visitASTAccGangClauseNode(ASTAccGangClauseNode node) { traverseChildren(node); }
    public void visitASTAccHostClauseNode(ASTAccHostClauseNode node) { traverseChildren(node); }
    public void visitASTAccHostdataClauseListNode(ASTAccHostdataClauseListNode node) { traverseChildren(node); }
    public void visitASTAccHostdataNode(ASTAccHostdataNode node) { traverseChildren(node); }
    public void visitASTAccIfClauseNode(ASTAccIfClauseNode node) { traverseChildren(node); }
    public void visitASTAccKernelsClauseListNode(ASTAccKernelsClauseListNode node) { traverseChildren(node); }
    public void visitASTAccKernelsLoopClauseListNode(ASTAccKernelsLoopClauseListNode node) { traverseChildren(node); }
    public void visitASTAccKernelsLoopNode(ASTAccKernelsLoopNode node) { traverseChildren(node); }
    public void visitASTAccKernelsNode(ASTAccKernelsNode node) { traverseChildren(node); }
    public void visitASTAccLinkClauseNode(ASTAccLinkClauseNode node) { traverseChildren(node); }
    public void visitASTAccLoopClauseListNode(ASTAccLoopClauseListNode node) { traverseChildren(node); }
    public void visitASTAccLoopNode(ASTAccLoopNode node) { traverseChildren(node); }
    public void visitASTAccNoConstruct(ASTAccNoConstruct node) { traverseChildren(node); }
    public void visitASTAccNumgangsClauseNode(ASTAccNumgangsClauseNode node) { traverseChildren(node); }
    public void visitASTAccNumworkersClauseNode(ASTAccNumworkersClauseNode node) { traverseChildren(node); }
    public void visitASTAccParallelClauseListNode(ASTAccParallelClauseListNode node) { traverseChildren(node); }
    public void visitASTAccParallelLoopClauseListNode(ASTAccParallelLoopClauseListNode node) { traverseChildren(node); }
    public void visitASTAccParallelLoopNode(ASTAccParallelLoopNode node) { traverseChildren(node); }
    public void visitASTAccParallelNode(ASTAccParallelNode node) { traverseChildren(node); }
    public void visitASTAccPresentClauseNode(ASTAccPresentClauseNode node) { traverseChildren(node); }
    public void visitASTAccPresentorcopyClauseNode(ASTAccPresentorcopyClauseNode node) { traverseChildren(node); }
    public void visitASTAccPresentorcopyinClauseNode(ASTAccPresentorcopyinClauseNode node) { traverseChildren(node); }
    public void visitASTAccPresentorcopyoutClauseNode(ASTAccPresentorcopyoutClauseNode node) { traverseChildren(node); }
    public void visitASTAccPresentorcreateClauseNode(ASTAccPresentorcreateClauseNode node) { traverseChildren(node); }
    public void visitASTAccPrivateClauseNode(ASTAccPrivateClauseNode node) { traverseChildren(node); }
    public void visitASTAccReductionClauseNode(ASTAccReductionClauseNode node) { traverseChildren(node); }
    public void visitASTAccReductionOperatorNode(ASTAccReductionOperatorNode node) { traverseChildren(node); }
    public void visitASTAccRoutineClauseListNode(ASTAccRoutineClauseListNode node) { traverseChildren(node); }
    public void visitASTAccRoutineNode(ASTAccRoutineNode node) { traverseChildren(node); }
    public void visitASTAccSelfClauseNode(ASTAccSelfClauseNode node) { traverseChildren(node); }
    public void visitASTAccTileClauseNode(ASTAccTileClauseNode node) { traverseChildren(node); }
    public void visitASTAccUpdateClauseListNode(ASTAccUpdateClauseListNode node) { traverseChildren(node); }
    public void visitASTAccUpdateNode(ASTAccUpdateNode node) { traverseChildren(node); }
    public void visitASTAccUsedeviceClauseNode(ASTAccUsedeviceClauseNode node) { traverseChildren(node); }
    public void visitASTAccVectorClauseNode(ASTAccVectorClauseNode node) { traverseChildren(node); }
    public void visitASTAccVectorlengthClauseNode(ASTAccVectorlengthClauseNode node) { traverseChildren(node); }
    public void visitASTAccWaitClauseListNode(ASTAccWaitClauseListNode node) { traverseChildren(node); }
    public void visitASTAccWaitClauseNode(ASTAccWaitClauseNode node) { traverseChildren(node); }
    public void visitASTAccWaitNode(ASTAccWaitNode node) { traverseChildren(node); }
    public void visitASTAccWaitParameterNode(ASTAccWaitParameterNode node) { traverseChildren(node); }
    public void visitASTAccWorkerClauseNode(ASTAccWorkerClauseNode node) { traverseChildren(node); }
    public void visitASTExpressionNode(ASTExpressionNode node) { traverseChildren(node); }
    public void visitASTIdentifierNode(ASTIdentifierNode node) { traverseChildren(node); }
    public void visitASTUnaryOperatorNode(ASTUnaryOperatorNode node) { traverseChildren(node); }
    public void visitCAccAtomicCaptureClause(CAccAtomicCaptureClause node) { traverseChildren(node); }
    public void visitCAccAtomicReadClause(CAccAtomicReadClause node) { traverseChildren(node); }
    public void visitCAccAtomicUpdateClause(CAccAtomicUpdateClause node) { traverseChildren(node); }
    public void visitCAccAtomicWriteClause(CAccAtomicWriteClause node) { traverseChildren(node); }
    public void visitCAccAutoClause(CAccAutoClause node) { traverseChildren(node); }
    public void visitCAccBindClause(CAccBindClause node) { traverseChildren(node); }
    public void visitCAccIndependentClause(CAccIndependentClause node) { traverseChildren(node); }
    public void visitCAccNoHostClause(CAccNoHostClause node) { traverseChildren(node); }
    public void visitCAccSeqClause(CAccSeqClause node) { traverseChildren(node); }
    public void visitCArrayAccessExpression(CArrayAccessExpression node) { traverseChildren(node); }
    public void visitCBinaryExpression(CBinaryExpression node) { traverseChildren(node); }
    public void visitCConstantExpression(CConstantExpression node) { traverseChildren(node); }
    public void visitCElementAccessExpression(CElementAccessExpression node) { traverseChildren(node); }
    public void visitCFunctionCallExpression(CFunctionCallExpression node) { traverseChildren(node); }
    public void visitCIdentifierExpression(CIdentifierExpression node) { traverseChildren(node); }
    public void visitCPostfixUnaryExpression(CPostfixUnaryExpression node) { traverseChildren(node); }
    public void visitCPrefixUnaryExpression(CPrefixUnaryExpression node) { traverseChildren(node); }
    public void visitCSizeofExpression(CSizeofExpression node) { traverseChildren(node); }
    public void visitCStringLiteralExpression(CStringLiteralExpression node) { traverseChildren(node); }
    public void visitCTernaryExpression(CTernaryExpression node) { traverseChildren(node); }
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
