/*******************************************************************************
 * Copyright (c) 2004, 2008, 2012, 2015 University of Illinois at Urbana-Champaign,
 * Auburn University, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.ptp.pldt.internal.tests.refactorings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
import org.eclipse.ptp.pldt.internal.tests.IOUtil;
import org.eclipse.ptp.pldt.internal.tests.Spawner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Base class for C refactoring tests.
 * <p>
 * The test suite is constructed by importing files from a directory in the source tree, searching its files for
 * <i>markers</i> of the form
 * 
 * <pre>
 * !&lt;&lt;&lt;&lt;&lt; fromLine, fromCol, toLine, toCol, ..., pass
 * </pre>
 * 
 * and adding one test case to the suite for each marker.
 * <p>
 * The first two fields in each marker are expected to be a line and column number; the third and fourth fields are also
 * a line and column number. The selection passed to the refactoring extends from the first line/column to the second
 * line/column.
 * <p>
 * The line and column numbers may be followed by an arbitrary number of fields that contain data specific to the
 * refactoring being tested. Many refactorings don't require any additional data. Subclasses can retrieve the data from
 * these fields by overriding {@link #configureRefactoring(CRefactoring, IFile, TextSelection, LinkedList)}.
 * <p>
 * In most cases, the final field must be either &quot;pass&quot; or &quot;fail&quot;, indicating whether the
 * refactoring should succeed or be blocked during precondition checking. This behavior can be changed by overriding
 * {@link #shouldFail(IFile, LinkedList)}.
 * <p>
 * If the refactoring is expected to succeed, the program will be compiled and run before and after the refactoring in
 * order to ensure that the refactoring actually preserved behavior. This can be prevented by overriding
 * {@link #shouldCompile(IFile)}.
 * <p>
 * Finally, if the refactoring succeeds, the source code produced by the refactoring is compared against the source code
 * in a result file (usually, the same as the input file, but with a .c.result filename extension). The location of the
 * result file can be changed by overriding {@link #resultFileFor(String)}.
 * <p>
 * Portions of this class are based on org.eclipse.cdt.core.tests.BaseTestFramework.
 * 
 * @author aniefer
 * @author Jeff Overbey
 *
 * @param <R>
 *            {@link CRefactoring} subclass under test
 */
@RunWith(Parameterized.class)
@SuppressWarnings("restriction")
public abstract class RefactoringTest<R extends CRefactoring> {
    private static final IProgressMonitor SYSOUT_PROGRESS_MONITOR = new NullProgressMonitor() {
        private String name = "";

        @Override
        public void beginTask(String name, int totalWork) {
            this.name = name;
            System.out.println(name);
        }

        @Override
        public void done() {
            System.out.println("Done " + name);
        }

        @Override
        public void subTask(String name) {
            System.out.println("- " + name);
        }
    };

    protected static final FilenameFilter C_FILENAME_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return new File(dir, name).isDirectory() && !name.equalsIgnoreCase("CVS") && !name.equalsIgnoreCase(".svn")
                    || name.endsWith(".c");
        }
    };

    private static final String MARKER = "/*<<<<<";

    private static final String MARKER_END = "*/";

    /**
     * String description, File fileContainingMarker, int markerOffset, String markerText
     * 
     * @param directory
     * @return
     * @throws Exception
     */
    protected static Iterable<Object[]> generateParameters(String directory) throws Exception {
        File fileOrDirectory = new File(directory);
        
        List<Object[]> result = new ArrayList<Object[]>();
        addTestsForFileOrDirectory(fileOrDirectory, result);
        
        if (result.size() == 0)
            throw new Exception(
                    String.format("No markers of the form %s found in %s", MARKER, fileOrDirectory.getName()));
        return result;
    }

    protected static void addTestsForFileOrDirectory(File fileOrDirectory, List<Object[]> result) throws Exception {
        if (!fileOrDirectory.exists())
            throw new FileNotFoundException(String.format("%s not found", fileOrDirectory.getAbsolutePath()));
        if (!fileOrDirectory.canRead())
            throw new IOException(String.format("%s cannot be read", fileOrDirectory.getAbsolutePath()));

        if (fileOrDirectory.isDirectory())
            for (File file : fileOrDirectory.listFiles(C_FILENAME_FILTER))
                addTestsForFileOrDirectory(file, result);
        else
            addTestForFile(fileOrDirectory, result);
    }

    protected static void addTestForFile(File file, List<Object[]> result) throws IOException, Exception {
        String fileContents = IOUtil.read(file);
        for (int index = fileContents.indexOf(MARKER); index >= 0; index = fileContents.indexOf(MARKER, index + 1)) {
            int endOfLine = fileContents.indexOf(MARKER_END, index);
            if (endOfLine < 0)
                endOfLine = fileContents.length();

            int nextMarker = fileContents.indexOf(MARKER, index + 1);
            if (nextMarker < 0)
                nextMarker = fileContents.length();

            int markerEnd = Math.min(endOfLine, nextMarker);

            String markerText = fileContents.substring(index + MARKER.length(), markerEnd).trim();
            String description = file + " " + markerText;
            if (description.startsWith("testcode/"))
                description = description.substring("testcode/".length());
            result.add(new Object[] { description, file, index, markerText });
        }
    }

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
    protected final File jioFileContainingMarker;
    private final String markerText;
    private final Class<R> refactoringClass;
    private String description;
    protected Map<String, IFile> files;

    protected RefactoringTest(Class<R> refactoringClass, File fileContainingMarker, int markerOffset, String markerText)
            throws Exception {
        this.jioFileContainingMarker = fileContainingMarker;
        this.markerText = markerText;
        this.refactoringClass = refactoringClass;
        this.description = fileContainingMarker.getName();
    }

    @Before
    public void setUp() throws Exception {
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

    @After
    public void tearDown() throws Exception {
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

    @Test
    public void test() throws Exception {
        this.files = importFiles(jioFileContainingMarker);

        IFile fileContainingMarker = files.get(jioFileContainingMarker.getName());
        assertNotNull(fileContainingMarker);

        LinkedList<String> markerFields = parseMarker(markerText);
        appendFilenameToDescription(markerFields);
        validateRemainingMarkerFields(markerFields);

        String origSauce = IOUtil.read(jioFileContainingMarker); //$NON-NLS-1$ //$NON-NLS-2$
        TextSelection selection = determineSelection(markerFields, createDocument(fileContainingMarker));
        R refactoring = createRefactoring(fileContainingMarker, selection);
        new CRefactoringContext(refactoring);

        RefactoringStatus status = refactoring.checkInitialConditions(SYSOUT_PROGRESS_MONITOR);
        configureRefactoring(refactoring, fileContainingMarker, selection, markerFields);
        if (!status.hasError()) {
            String before = shouldCompile(fileContainingMarker) ? compileAndRunProgram(files) : ""; //$NON-NLS-1$

            status = refactoring.checkFinalConditions(SYSOUT_PROGRESS_MONITOR);

            if (!status.hasError()) {
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
                compareAgainstResultFile(origSauce, selection);
            }
        }

        assertEquals(status.toString(), status.hasError(), shouldFail(fileContainingMarker, markerFields));
    }

    protected void validateRemainingMarkerFields(LinkedList<String> markerFields) {
        assertTrue(lastMarkerField(markerFields).equals(PASS) || lastMarkerField(markerFields).equals(FAIL));
    }

    protected boolean shouldFail(IFile fileContainingMarker, LinkedList<String> markerFields) {
        return lastMarkerField(markerFields).equals(FAIL);
    }

    protected Map<String, IFile> importFiles(File jioFileContainingMarker) throws Exception {
        Map<String, IFile> result = importAllFiles(jioFileContainingMarker.getParentFile());
        refreshProject();
        return result;
    }

    private Map<String, IFile> importAllFiles(File directory) throws Exception {
        Map<String, IFile> filesImported = new TreeMap<String, IFile>();
        for (File file : directory.listFiles(C_FILENAME_FILTER)) {
            IFile thisFile = importFile(file);
            filesImported.put(thisFile.getName(), thisFile);
        }
        return filesImported;
    }

    private IFile importFile(String fileName, String contents) throws Exception {
        IFile file = project.getProject().getFile(fileName);
        InputStream stream = new ByteArrayInputStream(contents.getBytes());

        if (file.exists())
            file.setContents(stream, false, false, SYSOUT_PROGRESS_MONITOR);
        else
            file.create(stream, false, SYSOUT_PROGRESS_MONITOR);

        return file;
    }

    private IFile importFile(String fileName, File fileToCopyIntoWorkspace) throws Exception {
        return importFile(fileName, IOUtil.read(fileToCopyIntoWorkspace));
    }

    protected final IFile importFile(File fileToCopyIntoWorkspace) throws Exception {
        return importFile(fileToCopyIntoWorkspace.getName(), fileToCopyIntoWorkspace);
    }

    protected String readWorkspaceFile(String filename) throws IOException, CoreException {
        return IOUtil.read(project.getFile(filename).getContents(true));
    }

    private IDocument createDocument(IFile file) throws IOException, CoreException {
        return new Document(readWorkspaceFile(file.getName()));
    }

    protected final void refreshProject() throws CoreException {
        project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
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
        IProgressMonitor pm = SYSOUT_PROGRESS_MONITOR;
        Change change = refactoring.createChange(pm);
        assertNotNull(description + " returned null Change object", change); //$NON-NLS-1$
        // assertTrue(description + " returned invalid Change object",
        // change.isValid(pm).isOK()); //$NON-NLS-1$
        change.perform(pm);
    }

    protected void compareAgainstResultFile(String originalSource, TextSelection selection) 
            throws IOException, URISyntaxException, CoreException {
        for (String filename : files.keySet()) {
            File resultFile = resultFileFor(filename);
            if (!resultFile.exists()) {
                fail("Refactoring succeeded, but result file " + resultFile.getName() + " does not exist");
                return;
            }
            String expected = IOUtil.read(resultFileFor(filename)).replaceAll("\\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
            String actual = readWorkspaceFile(filename).replaceAll("\\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
            if (!expected.equals(actual)) {
                System.err.println("Actual result does not match contents of .result file -- " + filename);
            }
            assertEquals(expected, actual);
        }
    }

    protected File resultFileFor(String filename) {
        return new File(jioFileContainingMarker.getParent() + File.separator + filename + ".result"); //$NON-NLS-1$
    }

    /**
     * @return true iff the program should be compiled and run using {@link #compileAndRunProgram(Map)}
     */
    protected boolean shouldCompile(IFile fileContainingMarker) {
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
        if (System.getenv("CC") != null) {
            args.add(System.getenv("CC"));
        } else {
            args.add("gcc");
        }
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
