/*******************************************************************************
 * Copyright (c) 2011, 2012 University of Illinois at Urbana-Champaign,
 * Auburn University, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     UIUC - Initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;

import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for a refactoring.
 * <p>
 * The test suite is constructed by importing files from a directory in the source tree, searching its files for
 * <i>markers,</i> and adding one test case to the suite for each marker.
 * <p>
 * The prefix and suffix of the marker are passed as the <code>marker</code> and <code>markerEnd</code> arguments to the
 * constructor. Assuming <code>marker</code> is &quot;!&lt;&lt;&lt;&lt;&lt;&quot; and <code>markerEnd</code> is
 * &quot;\n&quot;, markers are expected to have one of the following forms:
 * <ol>
 * <li><tt>!&lt;&lt;&lt;&lt;&lt; line, col, ..., pass</tt>
 * <li>
 * <tt>!&lt;&lt;&lt;&lt;&lt; fromLine, fromCol, toLine, toCol, ..., pass</tt>
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
 * 
 * @author Jeff Overbey
 * 
 * @see RefactoringTestCase
 * @see GeneralTestSuiteFromMarkers
 * 
 * @since 3.0
 */
@SuppressWarnings("restriction")
public abstract class RefactoringTestSuite<R extends CRefactoring> extends TestSuite {

    private static final FilenameFilter C_FILENAME_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return new File(dir, name).isDirectory() && !name.equalsIgnoreCase("CVS") && !name.equalsIgnoreCase(".svn")
                    || name.endsWith(".c");
        }
    };

    private static final String MARKER = "/*<<<<<";

    private static final String MARKER_END = "*/";

    private final String description;
    private final Class<R> refactoringClass;

    /**
     * Constructor. Creates this {@link TestSuite} and populates it with test cases.
     * 
     * @param description
     * @param marker
     * @param markerEnd
     * @param fileOrDirectory
     * @param filenameFilter
     * @param initializationData
     *            these arguments (if any) will be passed directly to {@link #initialize(Object...)} before adding tests
     *            to the test suite
     * @throws Exception
     */
    public RefactoringTestSuite(Class<R> clazz, String directory) throws Exception {
        File fileOrDirectory = new File(directory);

        this.description = "";
        this.refactoringClass = clazz;

        setName(getDescription(fileOrDirectory));

        addTestsForFileOrDirectory(fileOrDirectory, C_FILENAME_FILTER);

        if (countTestCases() == 0)
            throw new Exception(String.format("No markers of the form %s found in %s", MARKER,
                    fileOrDirectory.getName()));
    }

    private void addTestsForFileOrDirectory(File fileOrDirectory, FilenameFilter filenameFilter) throws Exception {
        if (!fileOrDirectory.exists())
            throw new FileNotFoundException(String.format("%s not found", fileOrDirectory.getAbsolutePath()));
        if (!fileOrDirectory.canRead())
            throw new IOException(String.format("%s cannot be read", fileOrDirectory.getAbsolutePath()));

        if (fileOrDirectory.isDirectory())
            for (File file : fileOrDirectory.listFiles(filenameFilter))
                addTestsForFileOrDirectory(file, filenameFilter);
        else
            addTestForFile(fileOrDirectory);
    }

    private void addTestForFile(File file) throws IOException, Exception {
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
            this.addTest(createTestFor(file, index, markerText));
        }
    }

    private String getDescription(File fileOrDirectory) {
        StringBuffer sb = new StringBuffer(256);
        sb.append(description);
        sb.append(' ');
        sb.append(fileOrDirectory.getName());
        String message = sb.toString();

        if (!fileOrDirectory.exists()) {
            message = String.format("NOTE: Some optional test files are not present: directory %s does not exist",
                    fileOrDirectory);
        }

        return message;
    }

    private Test createTestFor(File fileContainingMarker, int markerOffset, String markerText) throws Exception {
        TestSuite suite = new TestSuite(fileContainingMarker + " " + markerText);
        suite.addTest(new RefactoringTestCase<R>(fileContainingMarker, markerOffset, markerText, refactoringClass,
                C_FILENAME_FILTER) {
            @Override
            protected void configureRefactoring(R refactoring, IFile file, TextSelection selection,
                    LinkedList<String> markerFields) {
                // This allows subclasses of this class (RefactoringTestSuite) to override configureRefactoring (below)
                // rather than having to supply a custom subclass of RefactoringTestCase
                RefactoringTestSuite.this.configureRefactoring(refactoring, file, selection, markerFields);
            }
        });
        return suite;
    }

    /**
     * This method is invoked after initial preconditions have been checked (
     * {@link Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)} ) but before final
     * preconditions are checked ( {@link Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)}
     * ). Override if necessary to supply user input.
     */
    protected void configureRefactoring(R refactoring, IFile file, TextSelection selection,
            LinkedList<String> markerFields) {
        ;
    }
}
