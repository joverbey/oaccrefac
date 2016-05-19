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

package org.eclipse.ptp.pldt.openacc.internal.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitions;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCopyinClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCopyoutClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCreateClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataClauseListNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataItemNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsClauseListNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelClauseListNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelNode;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccConstruct;
import org.eclipse.ptp.pldt.openacc.core.parser.OpenACCParser;

public class OpenACCUtil {

    public static Set<IBinding> simpleInferCopyin(ReachingDefinitions rd, IASTStatement... construct) {
    	Set<IBinding> copyins = new HashSet<IBinding>();
    	class ParFinder extends ASTVisitor {
    		ParFinder() {
    			shouldVisitStatements = true;
    		}
    		@Override
    		public int visit(IASTStatement statement) {
    			if(isAccConstruct(statement, null)) {
    				Set<IASTName> rds = rd.reachingDefinitions(statement);
    				for(IASTName rd : rds) {
    					if(!ASTUtil.isAncestor(rd, construct)) {
    						copyins.add(rd.resolveBinding());
    					}
    				}
    			}
    			return PROCESS_CONTINUE;
    		}
    	}
    	
    	for(IASTStatement statement : construct) {
    		statement.accept(new ParFinder());
    	}
    	
    	return copyins;
    }
    
    public static Set<IBinding> simpleInferCopyout(ReachingDefinitions rd, IASTStatement... construct) {
    	Set<IBinding> copyouts = new HashSet<IBinding>();
    	class ParFinder extends ASTVisitor {
    		ParFinder() {
    			shouldVisitStatements = true;
    		}
    		@Override
    		public int visit(IASTStatement statement) {
    			if(isAccConstruct(statement, null)) {
    				Set<IASTName> rus = rd.reachedUses(statement);
    				for(IASTName rd : rus) {
    					if(!ASTUtil.isAncestor(rd, construct)) {
    						copyouts.add(rd.resolveBinding());
    					}
    				}
    			}
    			return PROCESS_CONTINUE;
    		}
    	}
    	
    	for(IASTStatement statement : construct) {
    		statement.accept(new ParFinder());
    	}
    	
    	return copyouts;
    }

    public static <T extends IAccConstruct> boolean isAccConstruct(IASTStatement statement, Class<T> accClazz) {
    	for(IASTPreprocessorPragmaStatement pragma : ASTUtil.getLeadingPragmas(statement)) {
    		IAccConstruct construct = null;
    		try {
    			construct = new OpenACCParser().parse(pragma.getRawSignature());
    		}
    		catch(Exception e) {
    			continue;
    		}
    		if((construct != null) && (accClazz.isInstance(construct))) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public static boolean isAccConstruct(IASTStatement statement) {
    	for(IASTPreprocessorPragmaStatement pragma : ASTUtil.getLeadingPragmas(statement)) {
    		try {
    			new OpenACCParser().parse(pragma.getRawSignature());
    		}
    		catch(Exception e) {
    			continue;
    		}
    		return true;
    	}
    	return false;
    }
  
    public static boolean isAccAccelConstruct(IASTStatement statement) {
    	return isAccConstruct(statement, ASTAccParallelNode.class) || 
    			isAccConstruct(statement, ASTAccParallelLoopNode.class) || 
    			isAccConstruct(statement, ASTAccKernelsNode.class) ||
    			isAccConstruct(statement, ASTAccKernelsLoopNode.class);
    }

    /**
     * Get the existing copyin set from a parsed OpenAcc construct 
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
  
    public static Set<String> getGpuVars(IASTNode node, boolean shouldGetDefs) {
        Set<String> gpuVars = new HashSet<String>();
        getGpuVars(node, gpuVars, shouldGetDefs);
        return gpuVars;
    }
    
    private static void getGpuVars(IASTNode node, Set<String> gpuVars, boolean shouldGetDefs) {
        if(node instanceof IASTStatement) {
            IASTStatement stmt = (IASTStatement) node;
            String[] prags = ASTUtil.getPragmas(stmt);
            for(String prag : prags) {
                //TODO should we parse this instead?
                if(prag.startsWith("#pragma acc parallel") || prag.startsWith("#pragma acc kernels")) {
                    List<IASTName> names = ASTUtil.getNames(stmt);
                    for(IASTName name : names) {
                        if(ASTUtil.isDefinition(name) && shouldGetDefs) {
                            gpuVars.add(name.getRawSignature());
                        }
                        else if(!ASTUtil.isDefinition(name) && !shouldGetDefs) {
                            gpuVars.add(name.getRawSignature());
                        }
                    }
                }
            }
        }
        
        for(IASTNode child : node.getChildren()) {
            getGpuVars(child, gpuVars, shouldGetDefs);
        }
        
    }
    
    
    private OpenACCUtil() {
    }
    
}
