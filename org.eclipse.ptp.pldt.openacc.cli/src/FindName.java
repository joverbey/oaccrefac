import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class FindName {

    public static void main(String[] args) throws CoreException {
        String fileName = args[0];
        String[] results = getNames(fileName);
        for(String result : results){
            System.out.print(result + " ");
        }
    }

    private static String[] getNames(String fileNameIn) throws CoreException {
        ArrayList<String> comments = new ArrayList<String>();

        String fileContents = readFile(fileNameIn);
        IASTTranslationUnit translation = null;
        try {
            translation = ASTUtil.translationUnitForString(fileContents);
        } catch (CoreException ie) {
            ie.getCause();
        }

        // IASTStatement statement = ASTUtil.parseStatement(fileContents);
        for (IASTComment comment : translation.getComments()) {
            // int start = comment.getFileLocation().getStartingLineNumber();
            // int finish = comment.getFileLocation().getEndingLineNumber();
            String commentLower = String.valueOf(comment.getComment()).toLowerCase();
            comments.add(commentLower);
        }
        ArrayList<String> tempList = new ArrayList<String>();
        for (String comment : comments) {
            if (comment.startsWith("/* loop") || comment.startsWith("/* datacon")) {
                String tempString = comment.substring(3);
                tempString = tempString.substring(0, tempString.length() - 3);
                tempList.add(tempString);
            }
        }
        String[] results = new String[tempList.size()];
        results = tempList.toArray(results);
        return results;

    }

    private static String readFile(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            try {
                StringBuilder builder = new StringBuilder();
                String line = reader.readLine();

                while (line != null) {
                    builder.append(line);
                    builder.append("\n");
                    line = reader.readLine();
                }
                return builder.toString();
            } catch (IOException ie) {
                ie.printStackTrace();
                return ("File not opened correctly");
            } finally {
                try {
                    reader.close();
                } catch (IOException ie) {
                    System.err.println("File not closed correctly");
                }
            }
        } catch (FileNotFoundException ie) {
            return ("File not found.");
        }

    }

}
