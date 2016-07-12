/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Carl Worley (Auburn) - initial API and implementation
 *     Jacob Allen Neeley (Auburn) - initial API and implementation
 *******************************************************************************/

import static java.lang.Integer.parseInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;

import edu.auburn.oaccrefac.cli.dom.rewrite.ASTRewrite;

public class Main {

	private Main() {}

	public static void main(String[] args) {
		if (args.length < 1) {
			printUsage();
			System.exit(1);
		}
		CLIRefactoring<?, ?> refactoring = null;
		int argIndex = 1;
		String filename = null;
		String loopName = null;
		int startOfSelection = -1;
		int endOfSelection = -1;
		try {
			switch (args[0]) {
			// 2 args
			case "-tile": //$NON-NLS-1$
				boolean cut = false;
				String name1 = "", name2 = ""; //$NON-NLS-1$ //$NON-NLS-2$
				int width = 0, height = 0;
				for (int i = argIndex; i < args.length; i++) {
					if (args[i].equals("-cut") || args[i].equals("-c")) { //$NON-NLS-1$ //$NON-NLS-2$
						cut = true;
						argIndex++;
					}
				}
				if (cut) {
					width = parseInt(args[argIndex++]);
					for (int i = argIndex; i < args.length; i++) {
						if (args[i].equals("-name") || args[i].equals("-n")) { //$NON-NLS-1$ //$NON-NLS-2$
							name1 = args[++i];
							argIndex += 2;
						}
					}
				} else {
					width = parseInt(args[argIndex++]);
					height = parseInt(args[argIndex++]);
					for (int i = argIndex; i < args.length; i++) {
						if (args[i].equals("-name") || args[i].equals("-n")) { //$NON-NLS-1$ //$NON-NLS-2$
							name1 = args[++i];
							name2 = args[++i];
							argIndex += 3;
						}
					}
				}
				refactoring = new TileLoops(width, height, name1, name2, cut);
				break;
			case "-strip-mine": //$NON-NLS-1$
				String name = ""; //$NON-NLS-1$
				int factor = parseInt(args[argIndex++]);
				loop:
				for (int i = argIndex; i < args.length; i++) {
					switch (args[i]) {
					case "-name": //$NON-NLS-1$
						name = args[++i];
						argIndex += 2;
						break;
					default:
						break loop;
					}
				}
				refactoring = new StripMineLoop(factor, false, true, name, null);
				break;
			// 1 arg
			case "-interchange": //$NON-NLS-1$
				refactoring = new InterchangeLoops(parseInt(args[argIndex++]));
				break;
			case "-unroll": //$NON-NLS-1$
				refactoring = new UnrollLoop(parseInt(args[argIndex++]));
				break;
			case "-introduce-loop": //$NON-NLS-1$
				boolean kernels = false;
				loop:
				for (int i = argIndex; i < args.length; i++) {
					switch (args[i]) {
					case "-k": //$NON-NLS-1$
					case "-kernels": //$NON-NLS-1$
						kernels = true;
						argIndex++;
						break;
					default:
						break loop;
					}
				}
				refactoring = new IntroOpenACCLoop(kernels);
				break;
			// no args
			case "-distribute": //$NON-NLS-1$
				refactoring = new DistributeLoops();
				break;
			case "-fuse": //$NON-NLS-1$
				refactoring = new FuseLoops();
				break;
			case "-expand": //$NON-NLS-1$
				refactoring = new ExpandDataConstruct();
				break;
			case "-merge": //$NON-NLS-1$
				refactoring = new MergeDataConstructs();
				break;
			case "-introduce-routine": //$NON-NLS-1$
				refactoring = new IntroRoutine();
				break;
			case "-introduce-data": //$NON-NLS-1$
				refactoring = new IntroOpenACCDataConstruct();
				break;
			case "-introduce-atomic": //$NON-NLS-1$	
				refactoring = new IntroAtomic();
				break;
			default:
				throw new IllegalArgumentException(Messages.Main_RefactoringIsInvalid);
			}

			for (int i = argIndex; i < args.length; i++) {
				switch (args[i]) {
				case "-f": //$NON-NLS-1$
				case "--find": //$NON-NLS-1$
					loopName = args[++i];
					break;
				case "-pos": //$NON-NLS-1$
				case "--position": //$NON-NLS-1$
					startOfSelection = parseInt(args[++i]);
					if (refactoring instanceof CLISourceStatementsRefactoring) {
						//Check whether there are two pos args representing a range
						try {
							endOfSelection = parseInt(args[++i]);
						} catch (NumberFormatException e) {
							endOfSelection = -1;
							i--;
						} 
					}
					break;
				default:
					filename = args[i];
					break;
				}
			}
		} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
			printUsage();
			System.err.println(e);
			System.exit(1);
		}

		if (filename == null && (startOfSelection == -1 || loopName == null)) {
			printUsage();
			System.exit(1);
		}

		IASTTranslationUnit translationUnit = null;
        try {
            translationUnit = parse(filename);
        } catch (CoreException e) {
            System.err.printf(Messages.Main_UnableToParse, filename, e.getMessage());
            System.exit(2);
        }

        IASTStatement statement = null;
        IASTRewrite rw = ASTRewrite.create(translationUnit);
        if (startOfSelection != -1) {
        	statement = findStatementForPosition(translationUnit, startOfSelection);
        } else {
	        statement = findStatementToAutotune(translationUnit, loopName);        	
        }
        
        if (endOfSelection != -1) {
        	IASTStatement endStatement = findStatementForPosition(translationUnit, endOfSelection);
			IASTFileLocation fileLocation = endStatement.getFileLocation();
			((CLISourceStatementsRefactoring) refactoring).setRegionEnd(fileLocation.getNodeOffset() 
					+ fileLocation.getNodeLength());
        }

        if (statement == null) {
            System.err.println(
                    Messages.Main_PleaseAddComment);
            System.exit(3);
        }
        
        RefactoringStatus status = refactoring.performChecks(statement);
        if (status == null) {
        	System.err.println(Messages.Main_NoApplicableStatement);
        	System.exit(4);
        }

        printStatus(status);
        if (status.hasFatalError()) {
        	System.exit(4);
        }
        
        String error = refactoring.performAlteration(rw);
    	//System.err.print(error);
        if (error != null) {
            System.err.printf(Messages.Main_InternalErrorCreatingChange, error);
            System.exit(5);
        }
	}

	public static void printUsage() {
		System.err.println(Messages.Main_Usage);
	}
	
	public static void printStatus(RefactoringStatus status) {
		for (RefactoringStatusEntry entry : status.getEntries()) {
            System.err.println(entry);
        }
	}

    /**
     * Parses a file for the translation unit which represents it.
     * 
     * @param filename
     *            Name of file to parse.
     * @return Translation unit of file.
     * @throws CoreException
     *             If getting the translation unit fails.
     */
    private static IASTTranslationUnit parse(String filename) throws CoreException {
        IParserLogService log = new DefaultLogService();
        FileContent fileContent = FileContent.createForExternalFileLocation(filename);
        Map<String, String> definedSymbols = new HashMap<String, String>();
        String[] includePaths = new String[0];
        IScannerInfo scanInfo = new ScannerInfo(definedSymbols, includePaths);
        IncludeFileContentProvider fileContentProvider = IncludeFileContentProvider.getEmptyFilesProvider();
        IASTTranslationUnit translationUnit = GCCLanguage.getDefault().getASTTranslationUnit(fileContent, scanInfo,
                fileContentProvider, null, 0, log);
        return translationUnit;
    }

    /**
     * findStatementToAutotune should returns a statement of type S which will be passed to the alteration.
     * 
     * @param translationUnit
     *            Translation unit being altered
     * @param rw
     *            Rewriter for the translation unit.
     * @param loopName
     *            Name of statement to be altered
     * @return Statement to be altered.
     */
    private static final IASTStatement findStatementToAutotune(final IASTTranslationUnit translationUnit,
    		final String loopName) {
        class V extends ASTVisitor {
            private IASTStatement found = null;

            ArrayList<Integer> pragmaPositions;

            public V() {
                this.shouldVisitStatements = true;
                pragmaPositions = new ArrayList<>();
                for (IASTPreprocessorStatement statement : translationUnit.getAllPreprocessorStatements()) {
                    pragmaPositions.add(statement.getFileLocation().getStartingLineNumber());
                }
                Collections.sort(pragmaPositions);
            }

            @Override
            public int visit(IASTStatement statement) {
                if (found == null) {
                    int pragmasToSkip = 0;
                    int current = Collections.binarySearch(pragmaPositions,
                            statement.getFileLocation().getStartingLineNumber() - 1);
                    if (current >= 0) {
                        pragmasToSkip++;
                        while (current > 0 && pragmaPositions.get(current - 1) == pragmaPositions.get(current) - 1) {
                            current--;
                            pragmasToSkip++;
                        }
                    }
                    for (IASTComment comment : translationUnit.getComments()) {
                        int start = comment.getFileLocation().getStartingLineNumber();
                        int finish = comment.getFileLocation().getEndingLineNumber();
                        if (start - finish == 0
                                && start == statement.getFileLocation().getStartingLineNumber() - (pragmasToSkip + 1)) {
                            String commentLower = String.valueOf(comment.getComment()).toLowerCase();
                            if (loopName != null && commentLower.contains(loopName)) {
                                found = statement;
                                return PROCESS_ABORT;
                            } else {
                                if (commentLower.contains("autotune") || commentLower.contains("refactor")){ //$NON-NLS-1$ //$NON-NLS-2$
                                    found = statement;
                                    return PROCESS_ABORT;
                                }
                            }
                        }
                    }
                }
                
                return PROCESS_CONTINUE;
            }
        }
        V visitor = new V();
        translationUnit.accept(visitor);
        return visitor.found;
    }
    
    private static final IASTStatement findStatementForPosition(final IASTTranslationUnit translationUnit, int lineNum) {
    	class V extends ASTVisitor {
            private IASTStatement found = null;

            ArrayList<Integer> pragmaPositions;

            public V() {
                this.shouldVisitStatements = true;
                pragmaPositions = new ArrayList<>();
                for (IASTPreprocessorStatement statement : translationUnit.getAllPreprocessorStatements()) {
                    pragmaPositions.add(statement.getFileLocation().getStartingLineNumber());
                }
                Collections.sort(pragmaPositions);
            }

            @Override
            public int visit(IASTStatement statement) {
            	int line = statement.getFileLocation().getStartingLineNumber();
            	if (line == lineNum) {
            		found = statement;
            		return PROCESS_ABORT;
            	}
                return PROCESS_CONTINUE;
            }
        }
        V visitor = new V();
        translationUnit.accept(visitor);
        return visitor.found;
    }
}
