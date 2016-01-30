package edu.auburn.oaccrefac.internal.core.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.core.dataflow.ReachingDefinitions;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import junit.framework.TestCase;

public class ReachingDefinitionsTest extends TestCase {

    IASTTranslationUnit tu;
    ReachingDefinitions rd;
    
    @Override
    public void setUp() {
        String func = "int main() { \n"                         
                + "    int a[8]; \n"                            
                + "    for(int i = 0; i < 4; i++) { \n"         
                + "        for(int j = 0; j < 3; j++) { \n"     
                + "            a[i] = 1; \n"                    
                + "        } \n"                                
                + "    } \n"                                    
                + "    a[0] = 0; \n" 
                + "    int x = 1; \n"
                + "    x--;"
                + "    return 0; \n"                            
                + "}";
        try {
            tu = ASTUtil.translationUnitForString(func);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        
        rd = new ReachingDefinitions(ASTUtil.findOne(tu, IASTFunctionDefinition.class));
    }
    
    private IASTName getName(String varname, int occurrence) {
        List<IASTName> names = ASTUtil.getNames(tu);
        int oc = 0;
        for(IASTName name : names) {
            if(name.getRawSignature().equals(varname)) {
                oc++;
                if(oc == occurrence) {
                    return name;
                }
            }
        }
        return null;
    }
    
    private boolean setContainsNodeByRawSignature(String sig, Set<IASTNode> nodes) {
        for(IASTNode node : nodes) {
            if(node.getRawSignature().equals(sig)) {
                return true;
            }
        }
        return false;
    }
    
    /** 
     * int a[8]; 
     */
    public void testA1() {
        IASTName a = getName("a", 1);
        Set<IASTNode> defs = rd.reachingDefinitions(a);
        assertTrue(defs.isEmpty());
    }
    
    /** 
     * a[i] = 1; 
     */
    public void testA2() {
        IASTName a = getName("a", 2);
        Set<IASTNode> defs = rd.reachingDefinitions(a);
        assertTrue(setContainsNodeByRawSignature("int a[8];", defs));
        assertTrue(setContainsNodeByRawSignature("a[i] = 1;", defs));
        assertTrue(defs.size() == 2);
    }
    
    /** 
     * a[0] = 0; 
     */
    public void testA3() {
        IASTName a = getName("a", 3);
        Set<IASTNode> defs = rd.reachingDefinitions(a);
        assertTrue(setContainsNodeByRawSignature("int a[8];", defs));
        assertTrue(setContainsNodeByRawSignature("a[i] = 1;", defs));
        assertTrue(defs.size() == 2);
    }
    
    /**
     * int i = 0;
     */
    public void testI1() {
        IASTName i = getName("i", 1);
        Set<IASTNode> defs = rd.reachingDefinitions(i);
        assertTrue(defs.isEmpty());
    }
    
    /**
     * i < 4;
     */
    public void testI2() {
        IASTName i = getName("i", 2);
        Set<IASTNode> defs = rd.reachingDefinitions(i);
        assertTrue(setContainsNodeByRawSignature("int i = 0;", defs));
        assertTrue(setContainsNodeByRawSignature("i++", defs));
        assertTrue(defs.size() == 2);
    }
    
    /**
     * i++
     */
    public void testI3() {
        IASTName i = getName("i", 2);
        Set<IASTNode> defs = rd.reachingDefinitions(i);
        assertTrue(setContainsNodeByRawSignature("int i = 0;", defs));
        assertTrue(setContainsNodeByRawSignature("i++", defs));
        assertTrue(defs.size() == 2);
    }
    
    /**
     * int j = 0;
     */
    public void testJ1() {
        IASTName j = getName("j", 1);
        Set<IASTNode> defs = rd.reachingDefinitions(j);
        assertTrue(setContainsNodeByRawSignature("int j = 0;", defs));
        assertTrue(setContainsNodeByRawSignature("j++", defs));
        assertTrue(defs.size() == 2);
    }
    
    /**
     * j < 0;
     */
    public void testJ2() {
        IASTName j = getName("j", 2);
        Set<IASTNode> defs = rd.reachingDefinitions(j);
        assertTrue(setContainsNodeByRawSignature("int j = 0;", defs));
        assertTrue(setContainsNodeByRawSignature("j++", defs));
        assertTrue(defs.size() == 2);
    }
    
    /**
     * j++
     */
    public void testJ3() {
        IASTName j = getName("j", 3);
        Set<IASTNode> defs = rd.reachingDefinitions(j);
        assertTrue(setContainsNodeByRawSignature("int j = 0;", defs));
        assertTrue(setContainsNodeByRawSignature("j++", defs));
        assertTrue(defs.size() == 2);
    }
    
    /**
     * int x = 1;
     */
    public void testX1() {
        IASTName j = getName("x", 1);
        Set<IASTNode> defs = rd.reachingDefinitions(j);
        assertTrue(defs.isEmpty());
    }
    
    /**
     * x--;
     */
    public void testX2() {
        IASTName j = getName("x", 2);
        Set<IASTNode> defs = rd.reachingDefinitions(j);
        assertTrue(setContainsNodeByRawSignature("int x = 1;", defs));
        assertTrue(defs.size() == 1);
    }
    
}
