/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Auburn University - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.parser;

@SuppressWarnings("all")
public interface IASTVisitor
{
    void visitASTNode(IASTNode node);
    void visitToken(Token node);
    void visitASTListNode(IASTListNode<?> node);
    void visitASTAccAsyncClauseNode(ASTAccAsyncClauseNode node);
    void visitASTAccAtomicNode(ASTAccAtomicNode node);
    void visitASTAccCacheNode(ASTAccCacheNode node);
    void visitASTAccCollapseClauseNode(ASTAccCollapseClauseNode node);
    void visitASTAccCopyClauseNode(ASTAccCopyClauseNode node);
    void visitASTAccCopyinClauseNode(ASTAccCopyinClauseNode node);
    void visitASTAccCopyoutClauseNode(ASTAccCopyoutClauseNode node);
    void visitASTAccCountNode(ASTAccCountNode node);
    void visitASTAccCreateClauseNode(ASTAccCreateClauseNode node);
    void visitASTAccDataClauseListNode(ASTAccDataClauseListNode node);
    void visitASTAccDataItemNode(ASTAccDataItemNode node);
    void visitASTAccDataNode(ASTAccDataNode node);
    void visitASTAccDeclareClauseListNode(ASTAccDeclareClauseListNode node);
    void visitASTAccDeclareNode(ASTAccDeclareNode node);
    void visitASTAccDefaultnoneClauseNode(ASTAccDefaultnoneClauseNode node);
    void visitASTAccDeleteClauseNode(ASTAccDeleteClauseNode node);
    void visitASTAccDeviceClauseNode(ASTAccDeviceClauseNode node);
    void visitASTAccDeviceptrClauseNode(ASTAccDeviceptrClauseNode node);
    void visitASTAccDeviceresidentClauseNode(ASTAccDeviceresidentClauseNode node);
    void visitASTAccEnterDataClauseListNode(ASTAccEnterDataClauseListNode node);
    void visitASTAccEnterDataNode(ASTAccEnterDataNode node);
    void visitASTAccExitDataClauseListNode(ASTAccExitDataClauseListNode node);
    void visitASTAccExitDataNode(ASTAccExitDataNode node);
    void visitASTAccFirstprivateClauseNode(ASTAccFirstprivateClauseNode node);
    void visitASTAccGangClauseNode(ASTAccGangClauseNode node);
    void visitASTAccHostClauseNode(ASTAccHostClauseNode node);
    void visitASTAccHostdataClauseListNode(ASTAccHostdataClauseListNode node);
    void visitASTAccHostdataNode(ASTAccHostdataNode node);
    void visitASTAccIfClauseNode(ASTAccIfClauseNode node);
    void visitASTAccKernelsClauseListNode(ASTAccKernelsClauseListNode node);
    void visitASTAccKernelsLoopClauseListNode(ASTAccKernelsLoopClauseListNode node);
    void visitASTAccKernelsLoopNode(ASTAccKernelsLoopNode node);
    void visitASTAccKernelsNode(ASTAccKernelsNode node);
    void visitASTAccLinkClauseNode(ASTAccLinkClauseNode node);
    void visitASTAccLoopClauseListNode(ASTAccLoopClauseListNode node);
    void visitASTAccLoopNode(ASTAccLoopNode node);
    void visitASTAccNoConstruct(ASTAccNoConstruct node);
    void visitASTAccNumgangsClauseNode(ASTAccNumgangsClauseNode node);
    void visitASTAccNumworkersClauseNode(ASTAccNumworkersClauseNode node);
    void visitASTAccParallelClauseListNode(ASTAccParallelClauseListNode node);
    void visitASTAccParallelLoopClauseListNode(ASTAccParallelLoopClauseListNode node);
    void visitASTAccParallelLoopNode(ASTAccParallelLoopNode node);
    void visitASTAccParallelNode(ASTAccParallelNode node);
    void visitASTAccPresentClauseNode(ASTAccPresentClauseNode node);
    void visitASTAccPresentorcopyClauseNode(ASTAccPresentorcopyClauseNode node);
    void visitASTAccPresentorcopyinClauseNode(ASTAccPresentorcopyinClauseNode node);
    void visitASTAccPresentorcopyoutClauseNode(ASTAccPresentorcopyoutClauseNode node);
    void visitASTAccPresentorcreateClauseNode(ASTAccPresentorcreateClauseNode node);
    void visitASTAccPrivateClauseNode(ASTAccPrivateClauseNode node);
    void visitASTAccReductionClauseNode(ASTAccReductionClauseNode node);
    void visitASTAccReductionOperatorNode(ASTAccReductionOperatorNode node);
    void visitASTAccRoutineClauseListNode(ASTAccRoutineClauseListNode node);
    void visitASTAccRoutineNode(ASTAccRoutineNode node);
    void visitASTAccSelfClauseNode(ASTAccSelfClauseNode node);
    void visitASTAccTileClauseNode(ASTAccTileClauseNode node);
    void visitASTAccUpdateClauseListNode(ASTAccUpdateClauseListNode node);
    void visitASTAccUpdateNode(ASTAccUpdateNode node);
    void visitASTAccUsedeviceClauseNode(ASTAccUsedeviceClauseNode node);
    void visitASTAccVectorClauseNode(ASTAccVectorClauseNode node);
    void visitASTAccVectorlengthClauseNode(ASTAccVectorlengthClauseNode node);
    void visitASTAccWaitClauseListNode(ASTAccWaitClauseListNode node);
    void visitASTAccWaitClauseNode(ASTAccWaitClauseNode node);
    void visitASTAccWaitNode(ASTAccWaitNode node);
    void visitASTAccWaitParameterNode(ASTAccWaitParameterNode node);
    void visitASTAccWorkerClauseNode(ASTAccWorkerClauseNode node);
    void visitASTExpressionNode(ASTExpressionNode node);
    void visitASTIdentifierNode(ASTIdentifierNode node);
    void visitASTUnaryOperatorNode(ASTUnaryOperatorNode node);
    void visitCAccAtomicCaptureClause(CAccAtomicCaptureClause node);
    void visitCAccAtomicReadClause(CAccAtomicReadClause node);
    void visitCAccAtomicUpdateClause(CAccAtomicUpdateClause node);
    void visitCAccAtomicWriteClause(CAccAtomicWriteClause node);
    void visitCAccAutoClause(CAccAutoClause node);
    void visitCAccBindClause(CAccBindClause node);
    void visitCAccIndependentClause(CAccIndependentClause node);
    void visitCAccNoHostClause(CAccNoHostClause node);
    void visitCAccSeqClause(CAccSeqClause node);
    void visitCArrayAccessExpression(CArrayAccessExpression node);
    void visitCBinaryExpression(CBinaryExpression node);
    void visitCConstantExpression(CConstantExpression node);
    void visitCElementAccessExpression(CElementAccessExpression node);
    void visitCFunctionCallExpression(CFunctionCallExpression node);
    void visitCIdentifierExpression(CIdentifierExpression node);
    void visitCPostfixUnaryExpression(CPostfixUnaryExpression node);
    void visitCPrefixUnaryExpression(CPrefixUnaryExpression node);
    void visitCSizeofExpression(CSizeofExpression node);
    void visitCStringLiteralExpression(CStringLiteralExpression node);
    void visitCTernaryExpression(CTernaryExpression node);
    void visitIAccAtomicClause(IAccAtomicClause node);
    void visitIAccConstruct(IAccConstruct node);
    void visitIAccDataClause(IAccDataClause node);
    void visitIAccDeclareClause(IAccDeclareClause node);
    void visitIAccEnterDataClause(IAccEnterDataClause node);
    void visitIAccExitDataClause(IAccExitDataClause node);
    void visitIAccHostdataClause(IAccHostdataClause node);
    void visitIAccKernelsClause(IAccKernelsClause node);
    void visitIAccKernelsLoopClause(IAccKernelsLoopClause node);
    void visitIAccLoopClause(IAccLoopClause node);
    void visitIAccParallelClause(IAccParallelClause node);
    void visitIAccParallelLoopClause(IAccParallelLoopClause node);
    void visitIAccRoutineClause(IAccRoutineClause node);
    void visitIAccUpdateClause(IAccUpdateClause node);
    void visitIAssignmentExpression(IAssignmentExpression node);
    void visitICExpression(ICExpression node);
    void visitIConstantExpression(IConstantExpression node);
}
