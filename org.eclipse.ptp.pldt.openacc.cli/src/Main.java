
import static java.lang.Integer.parseInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTComment;
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
		int row = -1;
		try {
			switch (args[0]) {
			// 2 args
			case "-tile":
				boolean cut = false;
				String name1 = "", name2 = "";
				int width = 0, height = 0;
				for (int i = argIndex; i < args.length; i++) {
					if (args[i].equals("-cut") || args[i].equals("-c")) {
						cut = true;
						argIndex++;
					}
				}
				if (cut) {
					width = parseInt(args[argIndex++]);
					for (int i = argIndex; i < args.length; i++) {
						if (args[i].equals("-name") || args[i].equals("-n")) {
							name1 = args[++i];
							argIndex += 2;
						}
					}
				} else {
					width = parseInt(args[argIndex++]);
					height = parseInt(args[argIndex++]);
					for (int i = argIndex; i < args.length; i++) {
						if (args[i].equals("-name") || args[i].equals("-n")) {
							name1 = args[++i];
							name2 = args[++i];
							argIndex += 3;
						}
					}
				}
				refactoring = new TileLoops(width, height, name1, name2, cut);
				break;
			case "-strip-mine":
				String name = "";
				int factor = parseInt(args[argIndex++]);
				loop:
				for (int i = argIndex; i < args.length; i++) {
					switch (args[i]) {
					case "-name":
						name = args[++i];
						argIndex += 2;
						break;
					default:
						break loop;
					}
				}
				refactoring = new StripMineLoop(factor, name);
				break;
			// 1 arg
			case "-interchange":
				refactoring = new InterchangeLoops(parseInt(args[argIndex++]));
				break;
			case "-unroll":
				refactoring = new UnrollLoop(parseInt(args[argIndex++]));
				break;
			case "-introduce-loop":
				boolean kernels = false;
				loop:
				for (int i = argIndex; i < args.length; i++) {
					switch (args[i]) {
					case "-k":
					case "-kernels":
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
			case "-distribute":
				refactoring = new DistributeLoops();
				break;
			case "-fuse":
				refactoring = new FuseLoops();
				break;
			case "-introduce-default-none":
				refactoring = new IntroduceDefaultNone();
				break;
			default:
				throw new IllegalArgumentException("Specified refactoring is invalid");
			}

			for (int i = argIndex; i < args.length; i++) {
				switch (args[i]) {
				case "-ln":
				case "--loop-name":
					loopName = args[++i];
					break;
				case "-pos":
				case "--position":
					row = parseInt(args[++i]);
					break;
				default:
					filename = args[i];
					break;
				}
			}
		} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
			printUsage();
			System.exit(1);
		}

		if (filename == null && (row == -1 || loopName == null)) {
			printUsage();
			System.exit(1);
		}

		IASTTranslationUnit translationUnit = null;
        try {
            translationUnit = parse(filename);
        } catch (CoreException e) {
            System.err.printf("Unable to parse %s (%s)\n", filename, e.getMessage());
            System.exit(2);
        }

        IASTStatement statement = null;
        IASTRewrite rw = ASTRewrite.create(translationUnit);
        if (row != -1) {
        	statement = findStatementForPosition(translationUnit, row);
        } else {
	        statement = findStatementToAutotune(translationUnit, loopName);        	
        }

        if (statement == null) {
            System.err.println(
                    "Please add a comment containing the loop name entered immediately above the loop to refactor.");
            System.exit(3);
        }
        
        RefactoringStatus status = refactoring.performChecks(statement);
        if (status == null) {
        	System.err.println("No applicable statement found at selection.");
        	System.exit(4);
        }

        printStatus(status);
        if (status.hasFatalError()) {
        	System.exit(4);
        }
        
        String error = refactoring.performAlteration(rw);
        if (error != null) {
            System.err.printf("Internal error creating change: %s\n", error);
            System.exit(5);
        }
	}

	public static void printUsage() {
		System.err.println(new StringBuilder()
				.append("Usage: Main <refactoring> <refactoring args> <options> <filename>\n")
				.append("  Refactorings:\n")
				.append("    DistributeLoops            - Break up one loop with independent statements to many loops\n")
				.append("    FuseLoops                  - Join loops with independent statements into one loo\n")
				.append("    InterchangeLoops           - Swap nested loops\n")
				.append("    IntroduceDefaultNone       - add default(none) to pragma\n")
				.append("    IntroduceACCLoop [-kernels]\n")
				.append("                               - change loop to acc parallel loop\n")
				.append("                      -kernels:  (optional) change loop to acc kernels loop\n")
				.append("    StripMine <strip_factor> [-name <new_name>] \n")
				.append("                               - insert loop with strip_factor iterations\n")
				.append("                <strip_factor>:  the number of times to strip the loop iterations\n")
				.append("              -name <new_name>:  (optional) the new variable name\n")
				.append("    TileLoops [-cut] <width> <height> [-name <new_name>] - break up loop into tiles of width by height\n")
				.append("                          -cut:  (optional) specifies that a loop cut intead of a tile\n")
				.append("                                 must be placed at beginning\n")
				.append("                       <width>:  the width of the tiles, or cut factor if cut\n")
				.append("                      <height>:  the height of the tiles, omit for cut\n")
				.append("              -name <new_name>:  (optional) the new variable name(s)\n")
				.append("                                 one for cut, two otherwise\n")
				.append("    Unroll <factor>            - unroll loop by factor\n")
				.append("                      <factor>:  the number of loop iterations to unroll\n")
				.append("  Options:\n")
				.append("    -ln  or --loop-name <name> - the name of the loop to refactor\n")
				.append("    -pos or --position <line> 	- the line number of the statement"));
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
                                if (commentLower.contains("autotune") || commentLower.contains("refactor")){
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
