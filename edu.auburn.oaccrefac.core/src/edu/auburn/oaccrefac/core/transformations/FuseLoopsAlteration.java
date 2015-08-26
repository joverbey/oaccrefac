package edu.auburn.oaccrefac.core.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.patternmatching.ASTMatcher;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

/**
 * Inheriting from {@link ForLoopAlteration}, this class defines a loop fusion refactoring algorithm. Loop fusion takes the
 * bodies of two identical for-loop headers and places them in one.
 * 
 * <p>
 * As of now, these loop headers MUST be completely identical and MUST be right next to each other. The reason they must
 * be next to each other is because there could be statements between the two loops that could change the meaning of the
 * program if two loops were merged into one.
 * </p>
 * 
 * <p>
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 10; i++) {
 *     a[i] = b[i] + c[i];
 * }
 * for (int i = 0; i < 10; i++) {
 *     b[i - 1] = a[i];
 * }
 * </pre>
 * 
 * Refactors to:
 * 
 * <pre>
 * for (int i = 0; i < 10; i++) {
 *     a[i] = b[i] + c[i];
 *     b[i - 1] = a[i];
 * }
 * </pre>
 * </p>
 *
 * @author Adam Eichelkraut
 *
 */
public class FuseLoopsAlteration extends ForLoopAlteration {

    private IASTForStatement first;
    private IASTForStatement second;
    private Map<String, IASTName> modifiedVariableDecls;
    private Set<String> usedNames;

    /**
     * Constructor that takes a for-loop to perform fusion on
     * 
     * @param rewriter
     *            -- base rewriter for loop
     * @param loop
     *            -- loop to be fizzed
     */
    public FuseLoopsAlteration(IASTTranslationUnit tu, IASTRewrite rewriter, IASTForStatement loop) {
        super(tu, rewriter, loop);
        first = loop;
        second = (IASTForStatement) ASTUtil.getNextSibling(first);
        modifiedVariableDecls = new HashMap<String, IASTName>();
        usedNames = new HashSet<String>();
    }

    @Override
    protected void doCheckConditions(RefactoringStatus init, IProgressMonitor pm) {
        // This gets the selected loop to re-factor.
        boolean found = false;
        IASTNode newnode = first;

        // Create pattern for matching loop headers
        IASTForStatement pattern = first.copy();
        pattern.setBody(new ArbitraryStatement());

        if (ASTUtil.getNextSibling(newnode) != null) {
            newnode = ASTUtil.getNextSibling(newnode);
            second = ASTUtil.findOne(newnode, IASTForStatement.class);
            found = (second != null);

            // Check to make sure the first and second loops have same headers
            Map<String, String> varmap = ASTMatcher.unify(pattern, second);
            if (varmap != null) {
                for (String key : varmap.keySet()) {
                    // The map returned contains name mapping that
                    // tells which names would make the two patterns equal
                    if (!varmap.get(key).equals(key)) {
                        found = false;
                    }
                }
                found = true;
            } else {
                found = false;
            }
        }

        if (!found) {
            init.addFatalError("There is no for loop for fusion to be possible.");
            return;
        }

        return;
    }

    // FIXME figure out what to do with pragmas on each loop
    @Override
    protected void doChange() {
        remove(second.getFileLocation().getNodeOffset(), second.getFileLocation().getNodeLength());
        List<IASTPreprocessorPragmaStatement> prags = getPragmas(second);
        Collections.reverse(prags);
        for (IASTPreprocessorPragmaStatement prag : prags) {
            remove(prag.getFileLocation().getNodeOffset(),
                    prag.getFileLocation().getNodeLength() + System.lineSeparator().length());
        }
        String body = "";

        for (int i = getBodyObjects(first).length - 1; i >= 0; i--) {
            IASTNode bodyObject = getBodyObjects(first)[i];
            body = bodyObject.getRawSignature() + System.lineSeparator() + body;
        }
        for (IASTNode bodyObject : getBodyObjects(second)) {
            String stmt;
            if (bodyObject instanceof IASTStatement) {
                stmt = getModifiedStatement((IASTStatement) bodyObject, getNameModifications(first, second));
            } else {
                stmt = bodyObject.getRawSignature();
            }
            body += stmt + System.lineSeparator();
        }

        body = compound(body);

        this.replace(first.getBody(), body);

        for (int i = getBodyObjects(first).length - 1; i >= 0; i--) {
            IASTNode bodyObject = getBodyObjects(first)[i];
            if (!(first.getBody() instanceof IASTCompoundStatement) && bodyObject instanceof IASTComment) {
                this.remove(bodyObject.getFileLocation().getNodeOffset(),
                        bodyObject.getFileLocation().getNodeLength() + System.lineSeparator().length());
            }
        }

        finalizeChanges();

    }

    private Map<IASTName, String> getNameModifications(IASTForStatement loop1, IASTForStatement loop2) {
        IASTStatement[] body1 = getBodyStatements(loop1);
        IASTStatement[] body2 = getBodyStatements(loop2);
        Map<IASTName, String> map = new HashMap<IASTName, String>();
        for (IASTStatement stmt1 : body1) {
            for (IASTStatement stmt2 : body2) {
                if (stmt1 instanceof IASTDeclarationStatement && stmt2 instanceof IASTDeclarationStatement
                        && ((IASTDeclarationStatement) stmt1).getDeclaration() instanceof IASTSimpleDeclaration
                        && ((IASTDeclarationStatement) stmt2).getDeclaration() instanceof IASTSimpleDeclaration) {
                    IASTSimpleDeclaration decl1 = (IASTSimpleDeclaration) ((IASTDeclarationStatement) stmt1)
                            .getDeclaration();
                    IASTSimpleDeclaration decl2 = (IASTSimpleDeclaration) ((IASTDeclarationStatement) stmt2)
                            .getDeclaration();

                    mapNewNamesToIdenticalDeclarators(decl1, decl2, loop1, loop2, map);

                }
            }
        }
        return map;
    }

    private IASTStatement[] getBodyStatements(IASTForStatement loop) {
        if (loop.getBody() instanceof IASTCompoundStatement) {
            return ((IASTCompoundStatement) loop.getBody()).getStatements();
        } else {
            return new IASTStatement[] { loop.getBody() };
        }
    }

    // gets statements AND comments from a loop body in forward order
    private IASTNode[] getBodyObjects(IASTForStatement loop) {
        List<IASTNode> objects = new ArrayList<IASTNode>();
        objects.addAll(Arrays.asList(getBodyStatements(loop)));
        for (IASTComment comment : loop.getTranslationUnit().getComments()) {
            // if the comment's offset is in between the end of the loop header and the end of the loop body
            if (comment.getFileLocation()
                    .getNodeOffset() > loop.getIterationExpression().getFileLocation().getNodeOffset()
                            + loop.getIterationExpression().getFileLocation().getNodeLength() + ")".length()
                    && comment.getFileLocation().getNodeOffset() < loop.getBody().getFileLocation().getNodeOffset()
                            + loop.getBody().getFileLocation().getNodeLength()) {
                objects.add(comment);
            }
        }
        Collections.sort(objects, new Comparator<IASTNode>() {

            @Override
            public int compare(IASTNode node1, IASTNode node2) {
                return node1.getFileLocation().getNodeOffset() - node2.getFileLocation().getNodeOffset();
            }

        });

        return objects.toArray(new IASTNode[objects.size()]);

    }

    private String newName(String name, IScope scope1, IScope scope2) {
        for (int i = 0; true; i++) {
            String newNameStr = name + "_" + i;
            IASTName newName = ASTNodeFactoryFactory.getDefaultCNodeFactory().newName(newNameStr.toCharArray());
            if (!ASTUtil.isNameInScope(newName, scope1) && !ASTUtil.isNameInScope(newName, scope2)) {
                return newNameStr;
            }
        }
    }

    private void mapNewNamesToIdenticalDeclarators(IASTSimpleDeclaration decl1, IASTSimpleDeclaration decl2,
            IASTForStatement loop1, IASTForStatement loop2, Map<IASTName, String> map) {
        for (IASTDeclarator d1 : decl1.getDeclarators()) {
            for (IASTDeclarator d2 : decl2.getDeclarators()) {
                if (d1.getName().getRawSignature().equals(d2.getName().getRawSignature())) {

                    String newName = newName(d2.getName().getRawSignature(),
                            ((IASTCompoundStatement) loop1.getBody()).getScope(),
                            ((IASTCompoundStatement) loop2.getBody()).getScope());
                    List<IASTName> uses = getUses(d2, loop2);

                    for (IASTName use : uses) {
                        map.put(use, newName);
                    }

                }
            }
        }
    }

    private List<IASTName> getUses(IASTDeclarator decl, IASTForStatement loop) {

        class VariableUseFinder extends ASTVisitor {

            private IASTName decl;
            public List<IASTName> uses;

            public VariableUseFinder(IASTName decl) {
                this.decl = decl;
                this.uses = new ArrayList<IASTName>();
                shouldVisitNames = true;
            }

            @Override
            public int visit(IASTName name) {
                if (name.resolveBinding().equals(decl.resolveBinding())) {
                    uses.add(name);
                }
                return PROCESS_CONTINUE;
            }

        }

        VariableUseFinder finder = new VariableUseFinder(decl.getName());
        loop.accept(finder);
        return finder.uses;

    }

    private String getModifiedStatement(IASTStatement original, Map<IASTName, String> map) {

        StringBuilder sb = new StringBuilder(original.getRawSignature());

        // references in reverse order of file location
        IASTName[] references = getReferencesToModify(original, map.keySet());

        for (IASTName reference : references) {
            int offsetIntoStatement = reference.getFileLocation().getNodeOffset()
                    - original.getFileLocation().getNodeOffset();
            sb.replace(offsetIntoStatement, offsetIntoStatement + reference.getFileLocation().getNodeLength(),
                    map.get(reference));
        }

        return sb.toString();
    }

    // returns in reverse order of file location
    private IASTName[] getReferencesToModify(IASTStatement original, Set<IASTName> allRefs) {
        List<IASTName> refsToMod = new ArrayList<IASTName>();
        for (IASTName ref : allRefs) {
            if (ASTUtil.isAncestor(original, ref)) {
                refsToMod.add(ref);
            }
        }
        Collections.sort(refsToMod, new Comparator<IASTName>() {

            @Override
            public int compare(IASTName name1, IASTName name2) {
                return name2.getFileLocation().getNodeOffset() - name1.getFileLocation().getNodeOffset();
            }

        });

        return refsToMod.toArray(new IASTName[refsToMod.size()]);
    }

}
