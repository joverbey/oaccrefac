/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jacob Allen Neeley (Auburn) - initial API and implementation
 *******************************************************************************/
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class FindName {

    public static void main(String[] args) throws IOException, CoreException {
        String fileName = args[0];
        String[] results = getNames(fileName);
        for(String result : results){
            System.out.print(result + " "); //$NON-NLS-1$
        }
    }

    private static String[] getNames(String fileNameIn) throws IOException, CoreException {
        ArrayList<String> comments = new ArrayList<String>();

        String fileContents = readFile(fileNameIn);
        IASTTranslationUnit translation = ASTUtil.translationUnitForString(fileContents);

        // IASTStatement statement = ASTUtil.parseStatement(fileContents);
        for (IASTComment comment : translation.getComments()) {
            // int start = comment.getFileLocation().getStartingLineNumber();
            // int finish = comment.getFileLocation().getEndingLineNumber();
            String commentLower = String.valueOf(comment.getComment()).toLowerCase();
            comments.add(commentLower);
        }
        ArrayList<String> tempList = new ArrayList<String>();
        for (String comment : comments) {
            if (comment.startsWith("/* loop") || comment.startsWith("/* datacon")) { //$NON-NLS-1$ //$NON-NLS-2$
                String tempString = comment.substring(3);
                tempString = tempString.substring(0, tempString.length() - 3);
                tempList.add(tempString);
            }
        }
        String[] results = new String[tempList.size()];
        results = tempList.toArray(results);
        return results;

    }

    private static String readFile(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        try {
            StringBuilder builder = new StringBuilder();
            String line = reader.readLine();

            while (line != null) {
                builder.append(line);
                builder.append("\n"); //$NON-NLS-1$
                line = reader.readLine();
            }
            return builder.toString();
        } finally {
            reader.close();
        }
    }
}
