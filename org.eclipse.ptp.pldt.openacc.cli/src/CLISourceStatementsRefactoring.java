/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.transformations.Check;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public abstract class CLISourceStatementsRefactoring<P extends RefactoringParams, C extends Check<P>> 
		extends CLIRefactoring<P, C> {

	private IASTTranslationUnit ast;
	private IASTStatement[] statements;
    private IASTNode[] allEnclosedNodes;
	private int regionEnd = -1;
    
    @Override 
    public RefactoringStatus performChecks(IASTStatement statement) {
    	discoverStatementsFromRegion(statement);
    	return super.performChecks(statement);
    }
    
    public void setRegionEnd(int regionEnd) {
    	this.regionEnd = regionEnd;
    }
    
	private void discoverStatementsFromRegion(IASTStatement statement) {
		this.ast = statement.getTranslationUnit();
        int regionBegin = statement.getFileLocation().getNodeOffset();//selectedRegion.getOffset();
        if (regionEnd == -1) {
        	regionEnd = statement.getFileLocation().getNodeLength() + regionBegin;
        }

        List<IASTStatement> statements = collectStatements(regionBegin, regionEnd);
        List<IASTComment> comments = collectComments(regionBegin, regionEnd, statements);
        List<IASTPreprocessorStatement> preprocs = collectPreprocessorStatements(regionBegin, regionEnd, statements);
        List<IASTFunctionDefinition> definitions = collectFunctionDefinitions(regionBegin, regionEnd);
        List<IASTNode> allEnclosedNodes = new LinkedList<IASTNode>();
        
        allEnclosedNodes.addAll(statements);
        allEnclosedNodes.addAll(comments);
        allEnclosedNodes.addAll(preprocs);
        allEnclosedNodes.addAll(definitions);
        
        Collections.sort(statements, ASTUtil.FORWARD_COMPARATOR);
        Collections.sort(allEnclosedNodes, ASTUtil.FORWARD_COMPARATOR);

        this.statements = statements.toArray(new IASTStatement[statements.size()]);
        this.allEnclosedNodes = allEnclosedNodes.toArray(new IASTNode[allEnclosedNodes.size()]);

    }
    
    private List<IASTStatement> collectStatements(int regionBegin, int regionEnd) {
        List<IASTStatement> all = ASTUtil.find(ast, IASTStatement.class);
        List<IASTStatement> statements = new LinkedList<>();
        //filter out statements not within the region bounds
        for (IASTStatement stmt : all) {
            int stmtBegin = stmt.getFileLocation().getNodeOffset();
            int stmtEnd = stmtBegin + stmt.getFileLocation().getNodeLength();
            if (stmtBegin >= regionBegin && stmtEnd <= regionEnd) {
                statements.add(stmt);
            }
        }

        //filter out statements that are children of other statements in the region
        Set<IASTStatement> children = new HashSet<IASTStatement>();
        for (IASTStatement child : statements) {
            for (IASTStatement parent : statements) {
                if (ASTUtil.isStrictAncestor(child, parent)) {
                    children.add(child);
                }
            }
        }
        for (Iterator<IASTStatement> iterator = statements.iterator(); iterator.hasNext();) {
            if (children.contains(iterator.next())) {
                iterator.remove();
            }
        }
        
        return statements;
    }
    
    private List<IASTComment> collectComments(int regionBegin, int regionEnd, List<IASTStatement> statements) {
        List<IASTComment> comments = new LinkedList<IASTComment>();
        
        //filter out comments that are not within region bounds
        for (IASTComment comment : ast.getComments()) {
            int commentBegin = comment.getFileLocation().getNodeOffset();
            int commentEnd = commentBegin + comment.getFileLocation().getNodeLength();
            if (commentBegin >= regionBegin && commentEnd <= regionEnd) {
                comments.add(comment);
            }
        }

        //filter out comments that are inside of statements in the region
        Set<IASTComment> containedComments = new HashSet<IASTComment>();
        for (IASTComment com : comments) {
            for (IASTStatement stmt : statements) {
                if (ASTUtil.doesNodeLexicallyContain(stmt, com)) {
                    containedComments.add(com);
                }
            }
        }
        for (Iterator<IASTComment> iterator = comments.iterator(); iterator.hasNext();) {
            if (containedComments.contains(iterator.next())) {
                iterator.remove();
            }
        }
        
        return comments;
    }
    
    private List<IASTFunctionDefinition> collectFunctionDefinitions(int regionBegin, int regionEnd) {
        List<IASTFunctionDefinition> all = ASTUtil.find(ast, IASTFunctionDefinition.class);
        List<IASTFunctionDefinition> definitions = new LinkedList<>();
        //filter out statements not within the region bounds
        for (IASTFunctionDefinition stmt : all) {
            int stmtBegin = stmt.getFileLocation().getNodeOffset();
            int stmtEnd = stmtBegin + stmt.getFileLocation().getNodeLength();
            if (stmtBegin >= regionBegin && stmtEnd <= regionEnd) {
                definitions.add(stmt);
            }
        }
		return definitions;
    }

    private List<IASTPreprocessorStatement> collectPreprocessorStatements(int regionBegin, int regionEnd, List<IASTStatement> statements) {
        List<IASTPreprocessorStatement> preprocs = new LinkedList<IASTPreprocessorStatement>();
        
        //filter out comments that are not within region bounds
        for (IASTPreprocessorStatement preproc : ast.getAllPreprocessorStatements()) {
            int preprocBegin = preproc.getFileLocation().getNodeOffset();
            int preprocEnd = preprocBegin + preproc.getFileLocation().getNodeLength();
            if (preprocBegin >= regionBegin && preprocEnd <= regionEnd) {
                preprocs.add(preproc);
            }
        }

        //filter out comments that are inside of statements in the region
        Set<IASTPreprocessorStatement> containedPreprocs = new HashSet<IASTPreprocessorStatement>();
        for (IASTPreprocessorStatement com : preprocs) {
            for (IASTStatement stmt : statements) {
                if (ASTUtil.doesNodeLexicallyContain(stmt, com)) {
                    containedPreprocs.add(com);
                }
            }
        }
        for (Iterator<IASTPreprocessorStatement> iterator = preprocs.iterator(); iterator.hasNext();) {
            if (containedPreprocs.contains(iterator.next())) {
                iterator.remove();
            }
        }
        
        return preprocs;
    }
    
    public IASTStatement[] getStatements() {
        return statements;
    }

    public IASTNode[] getAllEnclosedNodes() {
        return allEnclosedNodes;
    }
}
