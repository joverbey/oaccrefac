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
import org.eclipse.cdt.core.dom.ast.IASTStatement;

import edu.auburn.oaccrefac.core.dataflow.ReachingDefinitions;
import edu.auburn.oaccrefac.core.parser.ASTAccCopyinClauseNode;
import edu.auburn.oaccrefac.core.parser.ASTAccCopyoutClauseNode;
import edu.auburn.oaccrefac.core.parser.ASTAccCreateClauseNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataClauseListNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataItemNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataNode;
import edu.auburn.oaccrefac.core.parser.ASTAccKernelsClauseListNode;
import edu.auburn.oaccrefac.core.parser.ASTAccKernelsNode;
import edu.auburn.oaccrefac.core.parser.ASTAccParallelClauseListNode;
import edu.auburn.oaccrefac.core.parser.ASTAccParallelNode;
import edu.auburn.oaccrefac.core.parser.IAccConstruct;

public class OpenACCUtil {


    /**
     * Uses reaching definitions analysis to infer the appropriate copyin variables for a data construct
     * 
     * @param construct the children of the data construct
     * @param rd the reaching definitions analysis
     * @return a set of strings representing the inferred copied-in variables
     */
    public static Set<String> inferCopyout(ReachingDefinitions rd, IASTStatement... construct) {
        Set<String> copyout = new HashSet<String>();
        
        //all uses reached by definitions in the construct
        Set<IASTName> uses = rd.reachedUses(construct);
        
        //retain only those uses that are not in the construct
        for(IASTName use : uses) {
            if(!ASTUtil.inStatements(use, construct)) {
                copyout.add(use.getRawSignature());
            }
        }
        
        return copyout;
    }
    
    /**
     * Uses reaching definitions analysis to infer the appropriate copyout variables for a data construct
     * 
     * @param construct the children of the data construct
     * @param rd the reaching definitions analysis
     * @return a set of strings representing the inferred copied-out variables
     */
    public static Set<String> inferCopyin(ReachingDefinitions rd, IASTStatement... construct) {
        Set<String> copyin = new HashSet<String>();
        
        //all definitions reaching statements in the construct
        Set<IASTName> defs = rd.reachingDefinitions(construct);
        
        //if the definition is outside the construct, keep it
        for(IASTName def : defs) {
            if(!ASTUtil.inStatements(def, construct)) {
                copyin.add(def.getRawSignature());
            }
        }
        
        return copyin;
    }
  
    /**
     * Get the existing copyin set from a parsed OpenAcc construct 
     * TODO use reflection instead of four different methods?
     * @param construct the construct
     * @return a mapping from strings representing the variables being copied in to
     * the actual strings found in the set (i.e., may include a range, etc), or null
     * if this type of construct cannot have data clause nodes
     */
    public static Map<String, String> getCopyin(IAccConstruct construct) {
        if(construct instanceof ASTAccDataNode) {
            return getCopyin((ASTAccDataNode) construct);
        }
        else if(construct instanceof ASTAccParallelNode) {
            return getCopyin((ASTAccParallelNode) construct);
        }
        else if(construct instanceof ASTAccKernelsNode) {
            return getCopyin((ASTAccKernelsNode) construct);
        } 
        else {
            return null;
        }
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
     * Get the existing copyin set from a parsed parallel construct 
     * @param data the construct
     * @return a mapping from strings representing the variables being copied in to the actual strings found in the set (i.e., may include a range, etc)
     */
    public static Map<String, String> getCopyin(ASTAccParallelNode data) {
        Map<String, String> copyin = new TreeMap<String, String>();
        for(ASTAccParallelClauseListNode listNode : data.getAccParallelClauseList()) {
            if(listNode.getAccParallelClause() instanceof ASTAccCopyinClauseNode) {
                ASTAccCopyinClauseNode copyinClause = (ASTAccCopyinClauseNode) listNode.getAccParallelClause();
                for(ASTAccDataItemNode var : copyinClause.getAccDataList()) {
                    copyin.put(var.getIdentifier().getIdentifier().getText(), var.toString());
                }
            }
        }
        return copyin;
    }
    
    /**
     * Get the existing copyin set from a parsed kernels construct 
     * @param data the construct
     * @return a mapping from strings representing the variables being copied in to the actual strings found in the set (i.e., may include a range, etc)
     */
    public static Map<String, String> getCopyin(ASTAccKernelsNode data) {
        Map<String, String> copyin = new TreeMap<String, String>();
        for(ASTAccKernelsClauseListNode listNode : data.getAccKernelsClauseList()) {
            if(listNode.getAccKernelsClause() instanceof ASTAccCopyinClauseNode) {
                ASTAccCopyinClauseNode copyinClause = (ASTAccCopyinClauseNode) listNode.getAccKernelsClause();
                for(ASTAccDataItemNode var : copyinClause.getAccDataList()) {
                    copyin.put(var.getIdentifier().getIdentifier().getText(), var.toString());
                }
            }
        }
        return copyin;
    }
    
    /**
     * Get the existing copyout set from a parsed OpenAcc construct 
     * TODO use reflection instead of four different methods?
     * @param construct the construct
     * @return a mapping from strings representing the variables being copied out to
     * the actual strings found in the set (i.e., may include a range, etc), or null
     * if this type of construct cannot have data clause nodes
     */
    public static Map<String, String> getCopyout(IAccConstruct construct) {
        if(construct instanceof ASTAccDataNode) {
            return getCopyout((ASTAccDataNode) construct);
        }
        else if(construct instanceof ASTAccParallelNode) {
            return getCopyout((ASTAccParallelNode) construct);
        }
        else if(construct instanceof ASTAccKernelsNode) {
            return getCopyout((ASTAccKernelsNode) construct);
        } 
        else {
            return null;
        }
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
     * Get the existing copyout set from a parsed parallel construct 
     * @param parallel the construct
     * @return a mapping from strings representing the variables being copied out to the actual strings found in the set (i.e., may include a range, etc)
     */
    public static Map<String, String> getCopyout(ASTAccParallelNode parallel) {
        Map<String, String> copyout = new TreeMap<String, String>();
        for(ASTAccParallelClauseListNode listNode : parallel.getAccParallelClauseList()) {
            if(listNode.getAccParallelClause() instanceof ASTAccCopyoutClauseNode) {
                ASTAccCopyoutClauseNode copyoutClause = (ASTAccCopyoutClauseNode) listNode.getAccParallelClause();
                for(ASTAccDataItemNode var : copyoutClause.getAccDataList()) {
                    copyout.put(var.getIdentifier().getIdentifier().getText(), var.toString());
                }
            }
        }
        return copyout;
    }
    
    /**
     * Get the existing copyout set from a parsed kernels construct 
     * @param kernels the construct
     * @return a mapping from strings representing the variables being copied out to the actual strings found in the set (i.e., may include a range, etc)
     */
    public static Map<String, String> getCopyout(ASTAccKernelsNode kernels) {
        Map<String, String> copyout = new TreeMap<String, String>();
        for(ASTAccKernelsClauseListNode listNode : kernels.getAccKernelsClauseList()) {
            if(listNode.getAccKernelsClause() instanceof ASTAccCopyoutClauseNode) {
                ASTAccCopyoutClauseNode copyoutClause = (ASTAccCopyoutClauseNode) listNode.getAccKernelsClause();
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
