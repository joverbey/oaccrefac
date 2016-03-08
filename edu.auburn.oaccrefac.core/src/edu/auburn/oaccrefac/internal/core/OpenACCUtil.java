package edu.auburn.oaccrefac.internal.core;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

import edu.auburn.oaccrefac.core.dataflow.ReachingDefinitions;

public class OpenACCUtil {


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
    
    private OpenACCUtil() {
    }
    
}
