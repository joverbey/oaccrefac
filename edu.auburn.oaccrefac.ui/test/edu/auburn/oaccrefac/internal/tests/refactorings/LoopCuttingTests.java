package edu.auburn.oaccrefac.internal.tests.refactorings;

import java.io.File;
import java.util.LinkedList;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;
import org.junit.runners.Parameterized.Parameters;

import edu.auburn.oaccrefac.internal.ui.refactorings.LoopCuttingRefactoring;

public class LoopCuttingTests extends RefactoringTest<LoopCuttingRefactoring> {

    @Parameters(name = "{0}")
    public static Iterable<Object[]> generateParameters() throws Exception {
        return generateParameters("testcode/LoopCutting");
    }

    public LoopCuttingTests(String description, File fileContainingMarker, int markerOffset, String markerText) throws Exception {
        super(LoopCuttingRefactoring.class, fileContainingMarker, markerOffset, markerText);
    }

    @Override
    protected void configureRefactoring(LoopCuttingRefactoring refactoring, IFile file,
            TextSelection selection, LinkedList<String> markerFields) {
         String stripFactor = markerFields.removeFirst();
         refactoring.setCutFactor(Integer.parseInt(stripFactor));
    }
}
