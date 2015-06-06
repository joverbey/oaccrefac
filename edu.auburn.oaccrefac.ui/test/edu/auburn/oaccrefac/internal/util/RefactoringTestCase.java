/*******************************************************************************
 * Copyright (c) 2004, 2008, 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Markus Schorn (Wind River Systems)
 *     Jeff Overbey
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoringContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import junit.framework.TestCase;

/**
 * Test case for a refactoring (used in a {@link GeneralTestSuiteFromMarkers}).
 * <p>
 * Most clients will want to subclass {@link RefactoringTestSuite} instead of manually constructing a suite of
 * {@link RefactoringTestCase}s.
 * <p>
 * Assuming the marker prefix is &quot;!&lt;&lt;&lt;&lt;&lt;&quot; and the marker suffix is &quot;\n&quot;, markers are
 * expected to have one of the following forms:
 * <ol>
 * <li><tt>!&lt;&lt;&lt;&lt;&lt; line, col, ..., pass</tt>
 * <li><tt>!&lt;&lt;&lt;&lt;&lt; fromLine, fromCol, toLine, toCol, ..., pass</tt>
 * </ol>
 * That is, the first two fields in each marker are expected to be a line and column number; the text selection passed
 * to the refactoring will be the offset of that line and column. The third fourth fields may also be a line and column
 * number; then, the selection passed to the refactoring will extend from the first line/column to the second
 * line/column.
 * <p>
 * The line and column numbers may be followed by an arbitrary number of fields that contain data specific to the
 * refactoring being invoked. Many refactorings don't require any additional data; the Extract Local Variable test suite
 * uses one field for the new variable declaration; the Add ONLY to USE Statement test suite uses these fields to list
 * the module entities to add; etc.
 * <p>
 * The final field must be either &quot;pass&quot;, &quot;fail-initial&quot;, or &quot;fail-final&quot;, indicating
 * whether the refactoring should succeed, fail its initial precondition check, or fail its final precondition check.
 * <p>
 * If the refactoring is expected to succeed, the program may be compiled and run before and after the refactoring in
 * order to ensure that the refactoring actually preserved behavior. See the documentation for
 * {@link RefactoringTestCase} for more information.
 * <p>
 * Portions of this class are based on org.eclipse.cdt.core.tests.BaseTestFramework.
 * 
 * @author aniefer
 * @author Jeff Overbey
 * 
 * @see RefactoringTestSuite
 * @see GeneralTestSuiteFromMarkers
 * 
 * @since 3.0
 */
@SuppressWarnings("restriction")
public abstract class RefactoringTestCase<R extends CRefactoring> extends TestCase {
    /**
     * Text of the last marker field when a refactoring is pass all precondition checks
     */
    private static final String PASS = "pass"; //$NON-NLS-1$
    /**
     * Text of the last marker field when a refactoring is expected to fail initial precondition check
     */
    private static final String FAIL = "fail"; //$NON-NLS-1$

    /** Used to give each project a new name */
    private static int n = 0;

    private IProject project;
    private final File jioFileContainingMarker;
    private final String markerText;
    private final Class<R> refactoringClass;
    private final FilenameFilter filenameFilter;
    private String description;
    private Map<String, IFile> files;

    public RefactoringTestCase(File fileContainingMarker, int markerOffset, String markerText,
            Class<R> refactoringClass, FilenameFilter filenameFilter) throws Exception {
        super("test"); //$NON-NLS-1$
        this.jioFileContainingMarker = fileContainingMarker;
        this.markerText = markerText;
        this.refactoringClass = refactoringClass;
        this.filenameFilter = filenameFilter;
        this.description = fileContainingMarker.getName();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (project != null)
            return;

        project = createProject("TestProject" + (++n)); //$NON-NLS-1$
        if (project == null)
            fail("Unable to create project"); //$NON-NLS-1$
    }

    private static IProject createProject(final String projectName) throws CoreException {
        class CreateProject implements IWorkspaceRunnable {
            IProject project = null;

            public void run(IProgressMonitor monitor) throws CoreException {
                IWorkspace workspace = ResourcesPlugin.getWorkspace();
                project = workspace.getRoot().getProject(projectName);
                if (!project.exists())
                    project.create(monitor);
                else
                    project.refreshLocal(IResource.DEPTH_INFINITE, null);
                if (!project.isOpen())
                    project.open(monitor);
                CCorePlugin.getDefault().mapCProjectOwner(project, "TestProject", false);
            }
        }

        CreateProject runnable = new CreateProject();
        runnable.run(new NullProgressMonitor());
        return runnable.project;
    }

    @Override
    protected void tearDown() throws Exception {
        if (project == null || !project.exists())
            return;

        try {
            project.delete(true, true, new NullProgressMonitor());
        } catch (Throwable e) {
            project.close(new NullProgressMonitor());
        } finally {
            project = null;
        }
    }

    public void test() throws Exception {
        this.files = importFiles();

        IFile fileContainingMarker = files.get(jioFileContainingMarker.getName());
        assertNotNull(fileContainingMarker);

        LinkedList<String> markerFields = parseMarker(markerText);
        appendFilenameToDescription(markerFields);
        assertTrue(lastMarkerField(markerFields).equals(PASS) || lastMarkerField(markerFields).equals(FAIL));

        TextSelection selection = determineSelection(markerFields, createDocument(fileContainingMarker));
        R refactoring = createRefactoring(fileContainingMarker, selection);
        new CRefactoringContext(refactoring);

        RefactoringStatus status = refactoring.checkInitialConditions(new NullProgressMonitor());
        if (!status.hasFatalError()) {
            String before = shouldCompile(fileContainingMarker) ? compileAndRunProgram(files) : ""; //$NON-NLS-1$

            configureRefactoring(refactoring, fileContainingMarker, selection, markerFields);
            status = refactoring.checkFinalConditions(new NullProgressMonitor());

            if (!status.hasFatalError()) {
                performChange(refactoring);
                refreshProject();

                if (!status.hasError() && shouldCompile(fileContainingMarker)) {
                    String after = compileAndRunProgram(files);
                    if (!before.equals(after)) {
                        System.err.println(
                                "Refactored program has different runtime behavior -- " + fileContainingMarker);
                    }
                    assertEquals(before, after);
                }
                compareAgainstResultFile();
            }
        }

        assertEquals(status.hasError(), lastMarkerField(markerFields).equals(FAIL));
    }

    private Map<String, IFile> importFiles() throws Exception {
        Map<String, IFile> result = importAllFiles(jioFileContainingMarker.getParentFile(), filenameFilter);
        refreshProject();
        return result;
    }

    private Map<String, IFile> importAllFiles(File directory, FilenameFilter filenameFilter) throws Exception {
        Map<String, IFile> filesImported = new TreeMap<String, IFile>();
        for (File file : directory.listFiles(filenameFilter)) {
            IFile thisFile = importFile(file);
            filesImported.put(thisFile.getName(), thisFile);
        }
        return filesImported;
    }

    private IFile importFile(String fileName, String contents) throws Exception {
        IFile file = project.getProject().getFile(fileName);
        InputStream stream = new ByteArrayInputStream(contents.getBytes());

        if (file.exists())
            file.setContents(stream, false, false, new NullProgressMonitor());
        else
            file.create(stream, false, new NullProgressMonitor());

        return file;
    }

    private IFile importFile(String fileName, File fileToCopyIntoWorkspace) throws Exception {
        return importFile(fileName, IOUtil.read(fileToCopyIntoWorkspace));
    }

    private IFile importFile(File fileToCopyIntoWorkspace) throws Exception {
        return importFile(fileToCopyIntoWorkspace.getName(), fileToCopyIntoWorkspace);
    }

    private String readWorkspaceFile(String filename) throws IOException, CoreException {
        return IOUtil.read(project.getFile(filename).getContents(true));
    }

    private IDocument createDocument(IFile file) throws IOException, CoreException {
        return new Document(readWorkspaceFile(file.getName()));
    }

    private void refreshProject() throws CoreException {
        NullProgressMonitor pm = new NullProgressMonitor();
        project.refreshLocal(IResource.DEPTH_INFINITE, pm);
    }

    public static LinkedList<String> parseMarker(String markerText) {
        LinkedList<String> result = new LinkedList<String>();
        for (String field : markerText.split(",")) //$NON-NLS-1$
            result.add(field.trim());
        return result;
    }

    public static TextSelection determineSelection(LinkedList<String> markerFields, IDocument document)
            throws BadLocationException {
        if (markerFields.size() < 2)
            throw new IllegalArgumentException();

        int fromLine = Integer.parseInt(markerFields.removeFirst());
        int fromCol = Integer.parseInt(markerFields.removeFirst());
        int toLine = fromLine;
        int toCol = fromCol;
        if (markerFields.size() >= 2 && isInteger(markerFields.get(0)) && isInteger(markerFields.get(1))) {
            toLine = Integer.parseInt(markerFields.removeFirst());
            toCol = Integer.parseInt(markerFields.removeFirst());
        }

        IRegion fromLineRegion = document.getLineInformation(fromLine - 1);
        IRegion toLineRegion = document.getLineInformation(toLine - 1);

        int fromOffset = fromLineRegion.getOffset() + fromCol - 1;
        int toOffset = toLineRegion.getOffset() + toCol - 1;
        int length = toOffset - fromOffset;

        return new TextSelection(document, fromOffset, length);
    }

    /**
     * @return true iff {@link Integer#parseInt(String)} can successfully parse the given string can be parsed as an
     *         integer
     */
    private static boolean isInteger(String string) {
        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * This method is invoked to instantiate the refactoring class.
     */
    private R createRefactoring(IFile file, TextSelection selection) throws Exception {
        Constructor<R> ctor = refactoringClass.getConstructor(ICElement.class, ISelection.class, ICProject.class);
        ITranslationUnit tu = (ITranslationUnit) CoreModel.getDefault().create(file);
        return (R) ctor.newInstance(tu, selection, tu.getCProject());
    }

    private void appendFilenameToDescription(LinkedList<String> markerStrings) {
        description += " (" + jioFileContainingMarker.getName() + " " + markerStrings + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private String lastMarkerField(LinkedList<String> markerFields) {
        return markerFields.getLast();
    }

    /**
     * This method is invoked after initial preconditions have been checked (
     * {@link Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)} ) but before final
     * preconditions are checked ( {@link Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)}
     * ). Override if necessary to supply user input.
     */
    protected void configureRefactoring(R refactoring, IFile file, TextSelection selection,
            LinkedList<String> markerFields) {
    }

    private void performChange(Refactoring refactoring) throws CoreException {
        IProgressMonitor pm = new NullProgressMonitor();
        Change change = refactoring.createChange(pm);
        assertNotNull(description + " returned null Change object", change); //$NON-NLS-1$
        // assertTrue(description + " returned invalid Change object",
        // change.isValid(pm).isOK()); //$NON-NLS-1$
        change.perform(pm);
    }

    private void compareAgainstResultFile() throws IOException, URISyntaxException, CoreException {
        for (String filename : files.keySet()) {
            if (resultFileFor(filename).exists()) {
                String expected = IOUtil.read(resultFileFor(filename)).replaceAll("\\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
                String actual = readWorkspaceFile(filename).replaceAll("\\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
                if (!expected.equals(actual)) {
                    System.err.println("Actual result does not match contents of .result file -- " + filename);
                }
                assertEquals(expected, actual);
            }
        }
    }

    private File resultFileFor(String filename) {
        return new File(jioFileContainingMarker.getParent() + File.separator + filename + ".result"); //$NON-NLS-1$
    }

    /**
     * @return true iff the program should be compiled and run using {@link #compileAndRunProgram(Map)}
     */
    private boolean shouldCompile(IFile fileContainingMarker) {
        return true;
    }

    /**
     * Compiles and runs the test program.
     * <p>
     * This method is invoked iff {@link #shouldCompile(IFile)} returns <code>true</code>.
     * 
     * @return the output of running the program
     * 
     * @throws Exception
     *             if compilation or execution fails
     */
    private String compileAndRunProgram(Map<String, IFile> files) throws Exception {
        Spawner.SHOW_OUTPUT_ON_ERROR = true;
        File cwd = files.values().iterator().next().getParent().getLocation().toFile();

        List<String> args = new ArrayList<String>();
        args.add("gcc");
        args.add("-o");
        args.add("a.out");
        args.add("-std=c99");
        args.addAll(files.keySet());
        args.add("-lm");
        // addAdditionalCCArgs(args);
        Spawner.run(cwd, args);

        return Spawner.run(cwd, cwd.getAbsolutePath() + File.separator + "a.out");
    }
}
