/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert - initial API and implementation
 *******************************************************************************/

package edu.auburn.oaccrefac.internal.core;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

import edu.auburn.oaccrefac.core.dataflow.ReachingDefinitions;
import edu.auburn.oaccrefac.core.parser.ASTAccCopyinClauseNode;
import edu.auburn.oaccrefac.core.parser.ASTAccCopyoutClauseNode;
import edu.auburn.oaccrefac.core.parser.ASTAccCreateClauseNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataClauseListNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataItemNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataNode;

public class OpenACCUtil {


    /**
     * Uses reaching definitions analysis to infer the appropriate copyin variables for a data construct
     * 
     * @param exp the children of the data construct
     * @param rd the reaching definitions analysis
     * @return a set of strings representing the inferred copied-in variables
     */
    public static Set<String> inferCopyout(IASTStatement[] exp, ReachingDefinitions rd) {
        Set<String> copyout = new HashSet<String>();
        
        //all uses reached by definitions in the construct
        Set<IASTName> uses = new HashSet<IASTName>();
        for(IASTStatement statement : exp) {
             uses.addAll(rd.reachedUses(statement));
        }
        
        //retain only those uses that are not in the construct
        for(IASTName use : uses) {
            if(!inConstruct(use, exp)) {
                copyout.add(use.getRawSignature());
            }
        }
        
        return copyout;
    }
    
    /**
     * Uses reaching definitions analysis to infer the appropriate copyout variables for a data construct
     * 
     * @param exp the children of the data construct
     * @param rd the reaching definitions analysis
     * @return a set of strings representing the inferred copied-out variables
     */
    public static Set<String> inferCopyin(IASTStatement[] exp, ReachingDefinitions rd) {
        Set<String> copyin = new HashSet<String>();
        
        //all definitions reaching statements in the construct
        Set<IASTName> defs = new HashSet<IASTName>();
        for(IASTStatement statement : exp) {
            defs.addAll(rd.reachingDefinitions(statement));
        }
        
        //if the definition is outside the construct, keep it
        for(IASTName def : defs) {
            if(!inConstruct(def, exp)) {
                copyin.add(def.getRawSignature());
            }
        }
        
        return copyin;
    }
  
    private static boolean inConstruct(IASTNode node, IASTStatement[] construct) {
        for(IASTStatement stmt : construct) {
            if(ASTUtil.isAncestor(stmt, node)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the existing copyin set from a parsed data construct 
     * @param data the construct
     * @return a mapping from strings representing the variables being copied in to the actual strings found in the set (i.e., may include a range, etc)
     */
    public static Map<String, String> getCopyin(ASTAccDataNode data) {
        Map<String, String> copyin = new TreeMap<String, String>();
        for(ASTAccDataClauseListNode listNode : data.getAccDataClauseList()) {
            if(listNode.getAccDataClause() instanceof ASTAccCopyinClauseNode) {
                ASTAccCopyinClauseNode copyinClause = (ASTAccCopyinClauseNode) listNode.getAccDataClause();
                for(ASTAccDataItemNode var : copyinClause.getAccDataList()) {
                    copyin.put(var.getIdentifier().getIdentifier().getText(), var.toString());
                }
            }
        }
        return copyin;
    }
    
    /**
     * Get the existing copyout set from a parsed data construct 
     * @param data the construct
     * @return a mapping from strings representing the variables being copied out to the actual strings found in the set (i.e., may include a range, etc)
     */
    public static Map<String, String> getCopyout(ASTAccDataNode data) {
        Map<String, String> copyout = new TreeMap<String, String>();
        for(ASTAccDataClauseListNode listNode : data.getAccDataClauseList()) {
            if(listNode.getAccDataClause() instanceof ASTAccCopyoutClauseNode) {
                ASTAccCopyoutClauseNode copyoutClause = (ASTAccCopyoutClauseNode) listNode.getAccDataClause();
                for(ASTAccDataItemNode var : copyoutClause.getAccDataList()) {
                    copyout.put(var.getIdentifier().getIdentifier().getText(), var.toString());
                }
            }
        }
        return copyout;
    }
    
    /**
     * Get the existing created set from a parsed data construct 
     * @param data the construct
     * @return a mapping from strings representing the variables being created to the actual strings found in the set (i.e., may include a range, etc)
     */
    public static Map<String, String> getCreate(ASTAccDataNode data) {
        Map<String, String> create = new TreeMap<String, String>();
        for(ASTAccDataClauseListNode listNode : data.getAccDataClauseList()) {
            if(listNode.getAccDataClause() instanceof ASTAccCreateClauseNode) {
                ASTAccCreateClauseNode createClause = (ASTAccCreateClauseNode) listNode.getAccDataClause();
                for(ASTAccDataItemNode var : createClause.getAccDataList()) {
                    create.put(var.getIdentifier().getIdentifier().getText(), var.toString());
                }
            }
        }
        return create;
    }
    
    private OpenACCUtil() {
    }
    
}
