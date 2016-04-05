package edu.auburn.oaccrefac.core.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.zip.Inflater;

import edu.auburn.oaccrefac.core.parser.OpenACCParser.ErrorRecoveryInfo;
import edu.auburn.oaccrefac.core.parser.OpenACCParser.Nonterminal;
import edu.auburn.oaccrefac.core.parser.OpenACCParser.Production;

/**
 * An LALR(1) parser for OpenACC 2.0
 */
@SuppressWarnings("all")
public class OpenACCParser
{
    /** Set this to <code>System.out</code> or another <code>OutputStream</code>
        to view debugging information */
    public OutputStream DEBUG = new OutputStream() { @Override public void write(int b) {} };

    public static void main(String[] args) throws Exception
    {
        System.out.println(new OpenACCParser().parse(new Lexer(System.in)));
    }

    protected static final int NUM_STATES = 523;
    protected static final int NUM_PRODUCTIONS = 355;
    protected static final int NUM_TERMINALS = 99;
    protected static final int NUM_NONTERMINALS = 104;

    /**
     * When the parser uses an error production to recover from a syntax error,
     * an instance of this class is used to hold information about the error
     * and the recovery.
     */
    public static final class ErrorRecoveryInfo
    {
        /**
         * The symbols that were discarded in order to recover
         * from the syntax error.
         */
        public final LinkedList<? extends Object> discardedSymbols;

        /**
         * The (lookahead) token that caused the syntax error.
         * recovery is being performed.
         */
        public final Token errorLookahead;

        /**
         * A list of terminal symbols were expected at the point where
         * the syntax error occurred.
         */
        public final List<Terminal> expectedLookaheadSymbols;

        /**
         * Which state the parser was in when it encountered the syntax error.
         */
        public final int errorState;

        protected ErrorRecoveryInfo(int errorState,
                                    Token errorLookahead,
                                    List<Terminal> expectedLookaheadSymbols)
        {
            this.errorState = errorState;
            this.errorLookahead = errorLookahead;
            this.expectedLookaheadSymbols = expectedLookaheadSymbols;
            this.discardedSymbols = new LinkedList<Object>();
        }

        public final <T> LinkedList<T> getDiscardedSymbols()
        {
            return (LinkedList<T>)discardedSymbols;
        }

        protected void prependDiscardedSymbol(Object symbol)
        {
            this.<Object>getDiscardedSymbols().addFirst(symbol);
        }

        protected void appendDiscardedSymbol(Object symbol)
        {
            this.<Object>getDiscardedSymbols().addLast(symbol);
        }

        /**
         * A human-readable description of the terminal symbols were
         * expected at the point where the syntax error occurred.
         *
         * @return a <code>String</code> (non-<code>null</code>)
         */
        public final String describeExpectedSymbols()
        {
            return describe(expectedLookaheadSymbols);
        }
    }

    /** The lexical analyzer. */
    protected ILexer lexer;

    /** This becomes set to true when we finish parsing, successfully or not. */
    protected boolean doneParsing;

    /** The parser stack, which contains states as well as values returned from user code. */
    protected ParserStack parserStack;

    /**
     * LR parsing tables.
     * <p>
     * This is an interface to the ACTION, GOTO, and error recovery tables
     * to use.  If a parser's underlying grammar has only one start symbol,
     * there will be only one set of parsing tables.  If there are multiple
     * start symbols, each one will have a different set of parsing tables.
     */
    protected ParsingTables parsingTables;

    /**
     * Information about the parser's successful recovery from a syntax error,
     * including what symbol caused the error and what tokens were discarded to
     * recover from that error.
     * <p>
     * This field is set to a non-<code>null</code> value only while error
     * recovery is being performed.
     */
    protected ErrorRecoveryInfo errorInfo;

    /**
     * Semantic actions to invoke after reduce actions.
     */
    protected SemanticActions semanticActions;

    /**
     * Parses a file using the given lexical analyzer (tokenizer).
     *
     * @param lexicalAnalyzer the lexical analyzer to read tokens from
     */
    public IAccConstruct parse(Reader input) throws IOException, SyntaxException, Exception
    {
        return parse(new Lexer(input));
    }

    public IAccConstruct parse(InputStream input) throws IOException, SyntaxException, Exception
    {
        return parse(new Lexer(input));
    }

    public IAccConstruct parse(String input) throws IOException, SyntaxException, Exception
    {
        return parse(new Lexer(input));
    }

    public IAccConstruct parse(ILexer lexicalAnalyzer) throws IOException, SyntaxException, Exception
    {
        return (IAccConstruct)parse(lexicalAnalyzer, ParsingTables.getInstance());
    }

    protected Object parse(ILexer lexicalAnalyzer, ParsingTables parsingTables) throws IOException, SyntaxException, Exception
    {
        if (lexicalAnalyzer == null)
            throw new IllegalArgumentException("Lexer cannot be null");

        this.lexer = lexicalAnalyzer;
        this.parsingTables = parsingTables;
        this.semanticActions = new SemanticActions();

        this.parserStack = new ParserStack();
        this.errorInfo = null;

        semanticActions.initialize();

        readNextToken();
        doneParsing = false;

            assert DEBUG("Parser is starting in state " + currentState() +
                " with lookahead " + lookahead().toString().replaceAll("\\r", "\\\\r").replaceAll("\\n", "\\\\n") + "\n");

        // Repeatedly determine the next action based on the current state
        while (!doneParsing)
        {
            assert parserStack.invariants();

            int code = parsingTables.getActionCode(currentState(), lookahead());

            int action = code & ParsingTables.ACTION_MASK;
            int value  = code & ParsingTables.VALUE_MASK;

            switch (action)
            {
                case ParsingTables.SHIFT_ACTION:
                    shiftAndGoToState(value);
                    break;

                case ParsingTables.REDUCE_ACTION:
                    reduce(value);
                    break;

                case ParsingTables.ACCEPT_ACTION:
                    accept();
                    break;

                default:
                    if (!attemptToRecoverFromSyntaxError())
                        syntaxError();
             }
        }

        semanticActions.deinitialize();

        // Return the value from the last piece of user code
        // executed in a completed parse
        return parserStack.topValue();
    }

    public void readNextToken() throws IOException, SyntaxException, Exception
    {
        parserStack.setLookahead(lexer.yylex());
    }

    /**
     * Shifts the next input symbol and changes the parser to the given state.
     *
     * @param state the state to transition to
     */
    protected void shiftAndGoToState(int state) throws IOException, SyntaxException, Exception
    {
        assert 0 <= state && state < NUM_STATES;

        assert DEBUG("Shifting " + lookahead().toString().replaceAll("\\r", "\\\\r").replaceAll("\\n", "\\\\n"));

        parserStack.push(state, lookahead());
        readNextToken();

        assert DEBUG("; parser is now in state " + currentState() +
            " with lookahead " + lookahead().toString().replaceAll("\\r", "\\\\r").replaceAll("\\n", "\\\\n") + "\n");

        assert parserStack.invariants();
    }

    /**
     * Reduces the top several symbols on the stack and transitions the parser
     * to a new state.
     * <p>
     * The number of symbols to reduce and the nonterminal to reduce to are
     * determined by the given production.  After that has been done, the next
     * state is determined by the top state on the <code>parserStack</code>.
     */
    protected void reduce(int productionIndex)
    {
        assert 0 <= productionIndex && productionIndex < NUM_PRODUCTIONS;

        assert DEBUG("Reducing by " + Production.get(productionIndex));

        int symbolsToPop = Production.get(productionIndex).length();

        assert parserStack.numValues() >= symbolsToPop;

        Stack<Object> valueStack = parserStack.getValueStack();
        int valueStackSize = valueStack.size();
        int valueStackOffset = valueStackSize - symbolsToPop;
        Object reduceToObject = semanticActions.handle(productionIndex,
                                                       valueStack,
                                                       valueStackOffset,
                                                       valueStackSize,
                                                       errorInfo);

        for (int i = 0; i < symbolsToPop; i++)
            parserStack.pop();

        Nonterminal reduceToNonterm = Production.get(productionIndex).getLHS();
        int nextState = parsingTables.getGoTo(currentState(), reduceToNonterm);

        assert DEBUG("; parser is now in state " + currentState() +
            " with lookahead " + lookahead().toString().replaceAll("\\r", "\\\\r").replaceAll("\\n", "\\\\n") + "\n");

        parserStack.push(nextState, reduceToObject);
        assert parserStack.invariants();
    }

    /**
     * Halts the parser, indicating that parsing has completed successfully.
     */
    protected void accept()
    {
        assert parserStack.invariants();

        assert DEBUG("Parsing completed successfully\n");

        doneParsing = true;
    }

    /**
     * Halts the parser, indicating that a syntax error was found and error
     * recovery did not succeed.
     */
    protected void syntaxError() throws IOException, SyntaxException, Exception
    {
        throw new SyntaxException(parserStack.getLookahead(), describeTerminalsExpectedInCurrentState());
    }

    /**
     * Returns a list of terminal symbols that would not immediately lead the
     * parser to an error state if they were to appear as the next token,
     * given the current state of the parser.
     * <p>
     * This method may be used to produce an informative error message (see
     * {@link #describeTerminalsExpectedInCurrentState()}.
     *
     * @return a list of <code>Terminal</code> symbols (possibly empty, but
     *         never <code>null</code>)
     */
    public List<Terminal> getTerminalsExpectedInCurrentState()
    {
        List<Terminal> result = new ArrayList<Terminal>();
        for (int i = 0; i < NUM_TERMINALS; i++)
            if (parsingTables.getActionCode(currentState(), i) != 0)
                result.add(terminals.get(i));
        return result;
    }

    /**
     * Returns a human-readable description of the terminal symbols that
     * would not immediately lead the parser to an error state if they
     * were to appear as the next token, given the current state of the
     * parser.
     * <p>
     * This method is generally used to produce an informative error message.
     * For other purposes, see {@link #getTerminalsExpectedInCurrentState()}.
     *
     * @return a (non-<code>null</code>) <code>String</code>
     */
    public String describeTerminalsExpectedInCurrentState()
    {
        return describe(getTerminalsExpectedInCurrentState());
    }

    /**
     * Returns a human-readable description of the terminal symbols that
     * are passed as an argument.
     * <p>
     * The terminal descriptions are determined by {@link Terminal#toString}
     * and are separated by commas.  If the list is empty (or <code>null</code>),
     * returns "(none)".
     *
     * @return a (non-<code>null</code>) <code>String</code>
     */
    public static String describe(List<Terminal> terminals)
    {
        if (terminals == null || terminals.isEmpty()) return "(none)";

        StringBuilder sb = new StringBuilder();
        for (Terminal t : terminals)
        {
            sb.append(", ");
            sb.append(t);
        }
        return sb.substring(2);
    }

    /**
     * Returns the current state (the state on top of the parser stack).
     *
     * @return the current state, 0 <= result < NUM_STATES
     */
    protected int currentState()
    {
        return parserStack.topState();
    }

    protected Token lookahead()
    {
        return parserStack.getLookahead();
    }

    /**
     * Uses error productions in the grammar to attempt to recover from a
     * syntax error.
     * <p>
     * States are popped from the stack until a &quot;known&quot; sequence
     * of symbols (those to the left of the &quot;(error)&quot; symbol in
     * an error production) is found.  Then, tokens are discarded until
     * the lookahead token for that production (the terminal following the
     * &quot;(error)&quot; symbol) is discovered.  Then all of the discarded
     * symbols and the lookahead are passed to the semantic action handler
     * for that error production, and parsing continues normally.
     *
     * @return true if, and only if, recovery was successful
     */
    protected boolean attemptToRecoverFromSyntaxError() throws IOException, SyntaxException, Exception
    {
        assert DEBUG("Syntax error detected; attempting to recover\n");

        errorInfo = new ErrorRecoveryInfo(currentState(), lookahead(), getTerminalsExpectedInCurrentState());
        Token originalLookahead = lookahead();

        while (!doneParsing)
        {
            int code = parsingTables.getRecoveryCode(currentState(), lookahead());

            int action = code & ParsingTables.ACTION_MASK;
            int value  = code & ParsingTables.VALUE_MASK;

            switch (action)
            {
               case ParsingTables.DISCARD_STATE_ACTION:
                   if (parserStack.numStates() > 1)
                       errorInfo.prependDiscardedSymbol(parserStack.pop());
                   doneParsing = parserStack.numStates() <= 1;
                   break;

                case ParsingTables.DISCARD_TERMINAL_ACTION:
                    errorInfo.appendDiscardedSymbol(lookahead());
                    readNextToken();
                    doneParsing = (lookahead().getTerminal() == Terminal.END_OF_INPUT);
                    break;

                case ParsingTables.RECOVER_ACTION:
                    errorInfo.appendDiscardedSymbol(lookahead());
                    semanticActions.onErrorRecovery(errorInfo);
                    reduce(value);
                    if (lookahead().getTerminal() != Terminal.END_OF_INPUT)
                        readNextToken(); // Skip past error production lookahead
                    errorInfo = null;
                    assert parserStack.numValues() >= 1;
                    assert parserStack.invariants();

                    assert DEBUG("Successfully recovered from syntax error\n");

                    return true;

                default:
                    throw new IllegalStateException();
            }
        }

        // Recovery failed
        parserStack.setLookahead(originalLookahead);
        errorInfo = null;
        doneParsing = true;

        assert DEBUG("Unable to recover from syntax error\n");

        return false;
    }

    /** Prints the given message to the {@link #DEBUG} <code>OutputStream</code> and
        returns <code>true</code> (so this may be used in <code>assert</code> statement) */
    protected boolean DEBUG(String message)
    {
        try
        {
            DEBUG.write(message.getBytes());
            DEBUG.flush();
        }
        catch (IOException e)
        {
            throw new Error(e);
        }
        return true;
    }

    /**
     * The parser stack, which contains states as well as values returned from user code.
     */
    protected static final class ParserStack
    {
        /** The next token to process (the lookahead). */
        protected Token lookahead;

        /**
         * A stack holding parser states.  Parser states are non-negative integers.
         * <p>
         * This stack operates in parallel with <code>valueStack</code> and always
         * contains exactly one more symbol than <code>valueStack</code>.
         */
        protected IntStack stateStack;

        /**
         * A stack holding objects returned from user code.
         * <p>
         * Textbook descriptions of LR parsers often show terminal and nonterminal
         * bothsymbols on the parser stack.  In actuality, terminals and
         * nonterminals are not stored: The objects returned from the
         * user's semantic actions are stored instead.  So when a reduce action is
         * made and the user's code, perhaps <code>return lhs + rhs</code>, is run,
         * this is where that result is stored.
         */
        protected Stack<Object> valueStack;

        /** Class invariants */
        public boolean invariants() { return stateStack.size() == valueStack.size() + 1; }

        public ParserStack()
        {
            this.stateStack = new IntStack();
            this.valueStack = new Stack<Object>();

            // The parser starts in state 0
            stateStack.push(0);
        }

        public ParserStack(ParserStack copyFrom)
        {
            this.stateStack = new IntStack(copyFrom.stateStack);

            this.valueStack = new Stack<Object>();
            this.valueStack.addAll(copyFrom.valueStack);
        }

        public void push(int state, Object lookahead)
        {
            stateStack.push(state);
            valueStack.push(lookahead);
        }

        public Stack<Object> getValueStack()
        {
            return valueStack;
        }

        public int numStates()
        {
            return stateStack.size();
        }

        public int numValues()
        {
            return valueStack.size();
        }

        public Object pop()
        {
            stateStack.pop();
            return valueStack.pop();
        }

        public int topState()
        {
            assert !stateStack.isEmpty();

            return stateStack.top();
        }

        public Object topValue()
        {
            assert !valueStack.isEmpty();

            return valueStack.peek();
        }

        public void setLookahead(Token lookahead)
        {
            this.lookahead = lookahead;
        }

        public Token getLookahead()
        {
            return this.lookahead;
        }

        @Override public String toString()
        {
            return this.valueStack.toString() + " with lookahead " + this.lookahead;
        }
    }


    /**
     * A terminal symbol in the grammar.
     * <p>
     * This class enumerates all of the terminal symbols in the grammar as
     * constant <code>Terminal</code> objects,
     */
    @SuppressWarnings("all")
    public static final class Terminal
    {
        public static final Terminal LITERAL_STRING_READ = new Terminal(0, "literal string read");
        public static final Terminal LITERAL_STRING_COLON = new Terminal(1, "literal string colon");
        public static final Terminal LITERAL_STRING_QUESTION = new Terminal(2, "literal string question");
        public static final Terminal LITERAL_STRING_PLUS = new Terminal(3, "literal string plus");
        public static final Terminal LITERAL_STRING_TILDE = new Terminal(4, "literal string tilde");
        public static final Terminal END_OF_INPUT = new Terminal(5, "end of input");
        public static final Terminal LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPY = new Terminal(6, "literal string present underscoreor underscorecopy");
        public static final Terminal LITERAL_STRING_DEVICE_UNDERSCORERESIDENT = new Terminal(7, "literal string device underscoreresident");
        public static final Terminal LITERAL_STRING_DELETE = new Terminal(8, "literal string delete");
        public static final Terminal LITERAL_STRING_AMPERSAND = new Terminal(9, "literal string ampersand");
        public static final Terminal LITERAL_STRING_SEQ = new Terminal(10, "literal string seq");
        public static final Terminal LITERAL_STRING_PRESENT = new Terminal(11, "literal string present");
        public static final Terminal LITERAL_STRING_MAX = new Terminal(12, "literal string max");
        public static final Terminal LITERAL_STRING_SIZEOF = new Terminal(13, "literal string sizeof");
        public static final Terminal LITERAL_STRING_COLLAPSE = new Terminal(14, "literal string collapse");
        public static final Terminal LITERAL_STRING_DECLARE = new Terminal(15, "literal string declare");
        public static final Terminal LITERAL_STRING_WAIT = new Terminal(16, "literal string wait");
        public static final Terminal LITERAL_STRING_PERCENT = new Terminal(17, "literal string percent");
        public static final Terminal LITERAL_STRING_REDUCTION = new Terminal(18, "literal string reduction");
        public static final Terminal LITERAL_STRING_LPAREN = new Terminal(19, "literal string lparen");
        public static final Terminal LITERAL_STRING_HYPHEN_HYPHEN = new Terminal(20, "literal string hyphen hyphen");
        public static final Terminal LITERAL_STRING_KERNELS = new Terminal(21, "literal string kernels");
        public static final Terminal LITERAL_STRING_PCOPY = new Terminal(22, "literal string pcopy");
        public static final Terminal LITERAL_STRING_COPY = new Terminal(23, "literal string copy");
        public static final Terminal LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECREATE = new Terminal(24, "literal string present underscoreor underscorecreate");
        public static final Terminal LITERAL_STRING_DEVICEPTR = new Terminal(25, "literal string deviceptr");
        public static final Terminal LITERAL_STRING_PLUS_PLUS = new Terminal(26, "literal string plus plus");
        public static final Terminal LITERAL_STRING_USE_UNDERSCOREDEVICE = new Terminal(27, "literal string use underscoredevice");
        public static final Terminal LITERAL_STRING_GREATERTHAN_GREATERTHAN = new Terminal(28, "literal string greaterthan greaterthan");
        public static final Terminal LITERAL_STRING_VBAR = new Terminal(29, "literal string vbar");
        public static final Terminal LITERAL_STRING_WORKER = new Terminal(30, "literal string worker");
        public static final Terminal LITERAL_STRING_COPYOUT = new Terminal(31, "literal string copyout");
        public static final Terminal LITERAL_STRING_PCREATE = new Terminal(32, "literal string pcreate");
        public static final Terminal LITERAL_STRING_PCOPYIN = new Terminal(33, "literal string pcopyin");
        public static final Terminal LITERAL_STRING_PRIVATE = new Terminal(34, "literal string private");
        public static final Terminal LITERAL_STRING_AUTO = new Terminal(35, "literal string auto");
        public static final Terminal LITERAL_STRING_DATA = new Terminal(36, "literal string data");
        public static final Terminal LITERAL_STRING_ENTER = new Terminal(37, "literal string enter");
        public static final Terminal LITERAL_STRING_ASTERISK = new Terminal(38, "literal string asterisk");
        public static final Terminal LITERAL_STRING_NOHOST = new Terminal(39, "literal string nohost");
        public static final Terminal LITERAL_STRING_HOST = new Terminal(40, "literal string host");
        public static final Terminal LITERAL_STRING_LBRACKET = new Terminal(41, "literal string lbracket");
        public static final Terminal LITERAL_STRING_HYPHEN_GREATERTHAN = new Terminal(42, "literal string hyphen greaterthan");
        public static final Terminal LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPYOUT = new Terminal(43, "literal string present underscoreor underscorecopyout");
        public static final Terminal LITERAL_STRING_LOOP = new Terminal(44, "literal string loop");
        public static final Terminal STRING_LITERAL = new Terminal(45, "string literal");
        public static final Terminal LITERAL_STRING_COMMA = new Terminal(46, "literal string comma");
        public static final Terminal LITERAL_STRING_PARALLEL = new Terminal(47, "literal string parallel");
        public static final Terminal LITERAL_STRING_TILE = new Terminal(48, "literal string tile");
        public static final Terminal LITERAL_STRING_FIRSTPRIVATE = new Terminal(49, "literal string firstprivate");
        public static final Terminal LITERAL_STRING_UPDATE = new Terminal(50, "literal string update");
        public static final Terminal LITERAL_STRING_VBAR_VBAR = new Terminal(51, "literal string vbar vbar");
        public static final Terminal LITERAL_STRING_PERIOD = new Terminal(52, "literal string period");
        public static final Terminal LITERAL_STRING_EXIT = new Terminal(53, "literal string exit");
        public static final Terminal LITERAL_STRING_HYPHEN = new Terminal(54, "literal string hyphen");
        public static final Terminal LITERAL_STRING_IF = new Terminal(55, "literal string if");
        public static final Terminal LITERAL_STRING_EXCLAMATION = new Terminal(56, "literal string exclamation");
        public static final Terminal LITERAL_STRING_AMPERSAND_AMPERSAND = new Terminal(57, "literal string ampersand ampersand");
        public static final Terminal LITERAL_STRING_WRITE = new Terminal(58, "literal string write");
        public static final Terminal LITERAL_STRING_GANG = new Terminal(59, "literal string gang");
        public static final Terminal LITERAL_STRING_LESSTHAN_LESSTHAN = new Terminal(60, "literal string lessthan lessthan");
        public static final Terminal LITERAL_STRING_LINK = new Terminal(61, "literal string link");
        public static final Terminal SKIP = new Terminal(62, "skip");
        public static final Terminal LITERAL_STRING_NUM_UNDERSCOREGANGS = new Terminal(63, "literal string num underscoregangs");
        public static final Terminal LITERAL_STRING_DEVICE = new Terminal(64, "literal string device");
        public static final Terminal LITERAL_STRING_DEFAULT = new Terminal(65, "literal string default");
        public static final Terminal LITERAL_STRING_LESSTHAN_EQUALS = new Terminal(66, "literal string lessthan equals");
        public static final Terminal LITERAL_STRING_ROUTINE = new Terminal(67, "literal string routine");
        public static final Terminal LITERAL_STRING_RPAREN = new Terminal(68, "literal string rparen");
        public static final Terminal INTEGER_CONSTANT = new Terminal(69, "integer constant");
        public static final Terminal LITERAL_STRING_PCOPYOUT = new Terminal(70, "literal string pcopyout");
        public static final Terminal LITERAL_STRING_GREATERTHAN_EQUALS = new Terminal(71, "literal string greaterthan equals");
        public static final Terminal LITERAL_STRING_HOST_UNDERSCOREDATA = new Terminal(72, "literal string host underscoredata");
        public static final Terminal LITERAL_STRING_CAPTURE = new Terminal(73, "literal string capture");
        public static final Terminal CHARACTER_CONSTANT = new Terminal(74, "character constant");
        public static final Terminal LITERAL_STRING_ATOMIC = new Terminal(75, "literal string atomic");
        public static final Terminal FLOATING_CONSTANT = new Terminal(76, "floating constant");
        public static final Terminal LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPYIN = new Terminal(77, "literal string present underscoreor underscorecopyin");
        public static final Terminal LITERAL_STRING_LESSTHAN = new Terminal(78, "literal string lessthan");
        public static final Terminal LITERAL_STRING_NUM_UNDERSCOREWORKERS = new Terminal(79, "literal string num underscoreworkers");
        public static final Terminal LITERAL_STRING_CREATE = new Terminal(80, "literal string create");
        public static final Terminal LITERAL_STRING_EXCLAMATION_EQUALS = new Terminal(81, "literal string exclamation equals");
        public static final Terminal LITERAL_STRING_VECTOR_UNDERSCORELENGTH = new Terminal(82, "literal string vector underscorelength");
        public static final Terminal LITERAL_STRING_COPYIN = new Terminal(83, "literal string copyin");
        public static final Terminal PRAGMA_ACC = new Terminal(84, "pragma acc");
        public static final Terminal LITERAL_STRING_EQUALS_EQUALS = new Terminal(85, "literal string equals equals");
        public static final Terminal IDENTIFIER = new Terminal(86, "identifier");
        public static final Terminal LITERAL_STRING_GREATERTHAN = new Terminal(87, "literal string greaterthan");
        public static final Terminal LITERAL_STRING_CACHE = new Terminal(88, "literal string cache");
        public static final Terminal LITERAL_STRING_SELF = new Terminal(89, "literal string self");
        public static final Terminal LITERAL_STRING_CARET = new Terminal(90, "literal string caret");
        public static final Terminal LITERAL_STRING_NONE = new Terminal(91, "literal string none");
        public static final Terminal LITERAL_STRING_VECTOR = new Terminal(92, "literal string vector");
        public static final Terminal LITERAL_STRING_RBRACKET = new Terminal(93, "literal string rbracket");
        public static final Terminal LITERAL_STRING_INDEPENDENT = new Terminal(94, "literal string independent");
        public static final Terminal LITERAL_STRING_MIN = new Terminal(95, "literal string min");
        public static final Terminal LITERAL_STRING_BIND = new Terminal(96, "literal string bind");
        public static final Terminal LITERAL_STRING_ASYNC = new Terminal(97, "literal string async");
        public static final Terminal LITERAL_STRING_SLASH = new Terminal(98, "literal string slash");

        protected int index;
        protected String description;

        protected Terminal(int index, String description)
        {
            assert 0 <= index && index < NUM_TERMINALS;

            this.index = index;
            this.description = description;
        }

        protected int getIndex()
        {
            return index;
        }

        @Override public String toString()
        {
            return description;
        }
    }

    protected static HashMap<Integer, Terminal> terminals = new HashMap<Integer, Terminal>();

    static
    {
        terminals.put(0, Terminal.LITERAL_STRING_READ);
        terminals.put(1, Terminal.LITERAL_STRING_COLON);
        terminals.put(2, Terminal.LITERAL_STRING_QUESTION);
        terminals.put(3, Terminal.LITERAL_STRING_PLUS);
        terminals.put(4, Terminal.LITERAL_STRING_TILDE);
        terminals.put(5, Terminal.END_OF_INPUT);
        terminals.put(6, Terminal.LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPY);
        terminals.put(7, Terminal.LITERAL_STRING_DEVICE_UNDERSCORERESIDENT);
        terminals.put(8, Terminal.LITERAL_STRING_DELETE);
        terminals.put(9, Terminal.LITERAL_STRING_AMPERSAND);
        terminals.put(10, Terminal.LITERAL_STRING_SEQ);
        terminals.put(11, Terminal.LITERAL_STRING_PRESENT);
        terminals.put(12, Terminal.LITERAL_STRING_MAX);
        terminals.put(13, Terminal.LITERAL_STRING_SIZEOF);
        terminals.put(14, Terminal.LITERAL_STRING_COLLAPSE);
        terminals.put(15, Terminal.LITERAL_STRING_DECLARE);
        terminals.put(16, Terminal.LITERAL_STRING_WAIT);
        terminals.put(17, Terminal.LITERAL_STRING_PERCENT);
        terminals.put(18, Terminal.LITERAL_STRING_REDUCTION);
        terminals.put(19, Terminal.LITERAL_STRING_LPAREN);
        terminals.put(20, Terminal.LITERAL_STRING_HYPHEN_HYPHEN);
        terminals.put(21, Terminal.LITERAL_STRING_KERNELS);
        terminals.put(22, Terminal.LITERAL_STRING_PCOPY);
        terminals.put(23, Terminal.LITERAL_STRING_COPY);
        terminals.put(24, Terminal.LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECREATE);
        terminals.put(25, Terminal.LITERAL_STRING_DEVICEPTR);
        terminals.put(26, Terminal.LITERAL_STRING_PLUS_PLUS);
        terminals.put(27, Terminal.LITERAL_STRING_USE_UNDERSCOREDEVICE);
        terminals.put(28, Terminal.LITERAL_STRING_GREATERTHAN_GREATERTHAN);
        terminals.put(29, Terminal.LITERAL_STRING_VBAR);
        terminals.put(30, Terminal.LITERAL_STRING_WORKER);
        terminals.put(31, Terminal.LITERAL_STRING_COPYOUT);
        terminals.put(32, Terminal.LITERAL_STRING_PCREATE);
        terminals.put(33, Terminal.LITERAL_STRING_PCOPYIN);
        terminals.put(34, Terminal.LITERAL_STRING_PRIVATE);
        terminals.put(35, Terminal.LITERAL_STRING_AUTO);
        terminals.put(36, Terminal.LITERAL_STRING_DATA);
        terminals.put(37, Terminal.LITERAL_STRING_ENTER);
        terminals.put(38, Terminal.LITERAL_STRING_ASTERISK);
        terminals.put(39, Terminal.LITERAL_STRING_NOHOST);
        terminals.put(40, Terminal.LITERAL_STRING_HOST);
        terminals.put(41, Terminal.LITERAL_STRING_LBRACKET);
        terminals.put(42, Terminal.LITERAL_STRING_HYPHEN_GREATERTHAN);
        terminals.put(43, Terminal.LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPYOUT);
        terminals.put(44, Terminal.LITERAL_STRING_LOOP);
        terminals.put(45, Terminal.STRING_LITERAL);
        terminals.put(46, Terminal.LITERAL_STRING_COMMA);
        terminals.put(47, Terminal.LITERAL_STRING_PARALLEL);
        terminals.put(48, Terminal.LITERAL_STRING_TILE);
        terminals.put(49, Terminal.LITERAL_STRING_FIRSTPRIVATE);
        terminals.put(50, Terminal.LITERAL_STRING_UPDATE);
        terminals.put(51, Terminal.LITERAL_STRING_VBAR_VBAR);
        terminals.put(52, Terminal.LITERAL_STRING_PERIOD);
        terminals.put(53, Terminal.LITERAL_STRING_EXIT);
        terminals.put(54, Terminal.LITERAL_STRING_HYPHEN);
        terminals.put(55, Terminal.LITERAL_STRING_IF);
        terminals.put(56, Terminal.LITERAL_STRING_EXCLAMATION);
        terminals.put(57, Terminal.LITERAL_STRING_AMPERSAND_AMPERSAND);
        terminals.put(58, Terminal.LITERAL_STRING_WRITE);
        terminals.put(59, Terminal.LITERAL_STRING_GANG);
        terminals.put(60, Terminal.LITERAL_STRING_LESSTHAN_LESSTHAN);
        terminals.put(61, Terminal.LITERAL_STRING_LINK);
        terminals.put(62, Terminal.SKIP);
        terminals.put(63, Terminal.LITERAL_STRING_NUM_UNDERSCOREGANGS);
        terminals.put(64, Terminal.LITERAL_STRING_DEVICE);
        terminals.put(65, Terminal.LITERAL_STRING_DEFAULT);
        terminals.put(66, Terminal.LITERAL_STRING_LESSTHAN_EQUALS);
        terminals.put(67, Terminal.LITERAL_STRING_ROUTINE);
        terminals.put(68, Terminal.LITERAL_STRING_RPAREN);
        terminals.put(69, Terminal.INTEGER_CONSTANT);
        terminals.put(70, Terminal.LITERAL_STRING_PCOPYOUT);
        terminals.put(71, Terminal.LITERAL_STRING_GREATERTHAN_EQUALS);
        terminals.put(72, Terminal.LITERAL_STRING_HOST_UNDERSCOREDATA);
        terminals.put(73, Terminal.LITERAL_STRING_CAPTURE);
        terminals.put(74, Terminal.CHARACTER_CONSTANT);
        terminals.put(75, Terminal.LITERAL_STRING_ATOMIC);
        terminals.put(76, Terminal.FLOATING_CONSTANT);
        terminals.put(77, Terminal.LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPYIN);
        terminals.put(78, Terminal.LITERAL_STRING_LESSTHAN);
        terminals.put(79, Terminal.LITERAL_STRING_NUM_UNDERSCOREWORKERS);
        terminals.put(80, Terminal.LITERAL_STRING_CREATE);
        terminals.put(81, Terminal.LITERAL_STRING_EXCLAMATION_EQUALS);
        terminals.put(82, Terminal.LITERAL_STRING_VECTOR_UNDERSCORELENGTH);
        terminals.put(83, Terminal.LITERAL_STRING_COPYIN);
        terminals.put(84, Terminal.PRAGMA_ACC);
        terminals.put(85, Terminal.LITERAL_STRING_EQUALS_EQUALS);
        terminals.put(86, Terminal.IDENTIFIER);
        terminals.put(87, Terminal.LITERAL_STRING_GREATERTHAN);
        terminals.put(88, Terminal.LITERAL_STRING_CACHE);
        terminals.put(89, Terminal.LITERAL_STRING_SELF);
        terminals.put(90, Terminal.LITERAL_STRING_CARET);
        terminals.put(91, Terminal.LITERAL_STRING_NONE);
        terminals.put(92, Terminal.LITERAL_STRING_VECTOR);
        terminals.put(93, Terminal.LITERAL_STRING_RBRACKET);
        terminals.put(94, Terminal.LITERAL_STRING_INDEPENDENT);
        terminals.put(95, Terminal.LITERAL_STRING_MIN);
        terminals.put(96, Terminal.LITERAL_STRING_BIND);
        terminals.put(97, Terminal.LITERAL_STRING_ASYNC);
        terminals.put(98, Terminal.LITERAL_STRING_SLASH);
    }


    /**
     * A nonterminal symbol in the grammar.
     * <p>
     * This class enumerates all of the nonterminal symbols in the grammar as
     * constant <code>Nonterminal</code> objects,
     */
    public static final class Nonterminal
    {
        public static final Nonterminal ACC_HOSTDATA_CLAUSE = new Nonterminal(0, "<acc hostdata clause>");
        public static final Nonterminal ACC_ATOMIC = new Nonterminal(1, "<acc atomic>");
        public static final Nonterminal PRIMARY_EXPRESSION = new Nonterminal(2, "<primary expression>");
        public static final Nonterminal ACC_CONSTRUCT = new Nonterminal(3, "<acc construct>");
        public static final Nonterminal ACC_WAIT_CLAUSE_LIST = new Nonterminal(4, "<acc wait clause list>");
        public static final Nonterminal ACC_REDUCTION_OPERATOR = new Nonterminal(5, "<acc reduction operator>");
        public static final Nonterminal ACC_PRIVATE_CLAUSE = new Nonterminal(6, "<acc private clause>");
        public static final Nonterminal POSTFIX_EXPRESSION = new Nonterminal(7, "<postfix expression>");
        public static final Nonterminal ACC_DEVICEPTR_CLAUSE = new Nonterminal(8, "<acc deviceptr clause>");
        public static final Nonterminal ACC_PRESENTORCREATE_CLAUSE = new Nonterminal(9, "<acc presentorcreate clause>");
        public static final Nonterminal ACC_FIRSTPRIVATE_CLAUSE = new Nonterminal(10, "<acc firstprivate clause>");
        public static final Nonterminal ACC_DECLARE = new Nonterminal(11, "<acc declare>");
        public static final Nonterminal LOGICAL_AND_EXPRESSION = new Nonterminal(12, "<logical and expression>");
        public static final Nonterminal ACC_ASYNC_CLAUSE = new Nonterminal(13, "<acc async clause>");
        public static final Nonterminal ACC_LINK_CLAUSE = new Nonterminal(14, "<acc link clause>");
        public static final Nonterminal IDENTIFIER = new Nonterminal(15, "<identifier>");
        public static final Nonterminal ACC_DEVICERESIDENT_CLAUSE = new Nonterminal(16, "<acc deviceresident clause>");
        public static final Nonterminal ACC_DEFAULTNONE_CLAUSE = new Nonterminal(17, "<acc defaultnone clause>");
        public static final Nonterminal ACC_KERNELS_CLAUSE = new Nonterminal(18, "<acc kernels clause>");
        public static final Nonterminal ACC_COLLAPSE_CLAUSE = new Nonterminal(19, "<acc collapse clause>");
        public static final Nonterminal ACC_DELETE_CLAUSE = new Nonterminal(20, "<acc delete clause>");
        public static final Nonterminal ACC_GANG_CLAUSE = new Nonterminal(21, "<acc gang clause>");
        public static final Nonterminal ACC_HOST_CLAUSE = new Nonterminal(22, "<acc host clause>");
        public static final Nonterminal ACC_REDUCTION_CLAUSE = new Nonterminal(23, "<acc reduction clause>");
        public static final Nonterminal ACC_DEVICE_CLAUSE = new Nonterminal(24, "<acc device clause>");
        public static final Nonterminal UNARY_OPERATOR = new Nonterminal(25, "<unary operator>");
        public static final Nonterminal ACC_CACHE = new Nonterminal(26, "<acc cache>");
        public static final Nonterminal ACC_ENTER_DATA = new Nonterminal(27, "<acc enter data>");
        public static final Nonterminal ACC_UPDATE = new Nonterminal(28, "<acc update>");
        public static final Nonterminal ACC_PRESENTORCOPYIN_CLAUSE = new Nonterminal(29, "<acc presentorcopyin clause>");
        public static final Nonterminal SHIFT_EXPRESSION = new Nonterminal(30, "<shift expression>");
        public static final Nonterminal LOGICAL_OR_EXPRESSION = new Nonterminal(31, "<logical or expression>");
        public static final Nonterminal ACC_DATA_ITEM = new Nonterminal(32, "<acc data item>");
        public static final Nonterminal IDENTIFIER_LIST = new Nonterminal(33, "<identifier list>");
        public static final Nonterminal ACC_HOSTDATA = new Nonterminal(34, "<acc hostdata>");
        public static final Nonterminal ACC_PARALLEL_CLAUSE = new Nonterminal(35, "<acc parallel clause>");
        public static final Nonterminal ACC_ROUTINE_CLAUSE = new Nonterminal(36, "<acc routine clause>");
        public static final Nonterminal ACC_KERNELS_CLAUSE_LIST = new Nonterminal(37, "<acc kernels clause list>");
        public static final Nonterminal ACC_KERNELS_LOOP = new Nonterminal(38, "<acc kernels loop>");
        public static final Nonterminal EXPRESSION = new Nonterminal(39, "<expression>");
        public static final Nonterminal ACC_UPDATE_CLAUSE = new Nonterminal(40, "<acc update clause>");
        public static final Nonterminal ACC_SELF_CLAUSE = new Nonterminal(41, "<acc self clause>");
        public static final Nonterminal ASSIGNMENT_EXPRESSION = new Nonterminal(42, "<assignment expression>");
        public static final Nonterminal ACC_DATA_CLAUSE = new Nonterminal(43, "<acc data clause>");
        public static final Nonterminal EXCLUSIVE_OR_EXPRESSION = new Nonterminal(44, "<exclusive or expression>");
        public static final Nonterminal CONSTANT_EXPRESSION = new Nonterminal(45, "<constant expression>");
        public static final Nonterminal ACC_ENTER_DATA_CLAUSE = new Nonterminal(46, "<acc enter data clause>");
        public static final Nonterminal MULTIPLICATIVE_EXPRESSION = new Nonterminal(47, "<multiplicative expression>");
        public static final Nonterminal ACC_ROUTINE_CLAUSE_LIST = new Nonterminal(48, "<acc routine clause list>");
        public static final Nonterminal ACC_KERNELS_LOOP_CLAUSE = new Nonterminal(49, "<acc kernels loop clause>");
        public static final Nonterminal CONSTANT = new Nonterminal(50, "<constant>");
        public static final Nonterminal ACC_DATA = new Nonterminal(51, "<acc data>");
        public static final Nonterminal ACC_COPY_CLAUSE = new Nonterminal(52, "<acc copy clause>");
        public static final Nonterminal ACC_LOOP = new Nonterminal(53, "<acc loop>");
        public static final Nonterminal ACC_WAIT_PARAMETER = new Nonterminal(54, "<acc wait parameter>");
        public static final Nonterminal ACC_PARALLEL_LOOP_CLAUSE = new Nonterminal(55, "<acc parallel loop clause>");
        public static final Nonterminal CAST_EXPRESSION = new Nonterminal(56, "<cast expression>");
        public static final Nonterminal UNARY_EXPRESSION = new Nonterminal(57, "<unary expression>");
        public static final Nonterminal ACC_VECTORLENGTH_CLAUSE = new Nonterminal(58, "<acc vectorlength clause>");
        public static final Nonterminal INCLUSIVE_OR_EXPRESSION = new Nonterminal(59, "<inclusive or expression>");
        public static final Nonterminal ACC_COPYIN_CLAUSE = new Nonterminal(60, "<acc copyin clause>");
        public static final Nonterminal ACC_PRESENT_CLAUSE = new Nonterminal(61, "<acc present clause>");
        public static final Nonterminal ACC_UPDATE_CLAUSE_LIST = new Nonterminal(63, "<acc update clause list>");
        public static final Nonterminal ACC_PARALLEL_CLAUSE_LIST = new Nonterminal(64, "<acc parallel clause list>");
        public static final Nonterminal ACC_KERNELS = new Nonterminal(65, "<acc kernels>");
        public static final Nonterminal ACC_NUMWORKERS_CLAUSE = new Nonterminal(66, "<acc numworkers clause>");
        public static final Nonterminal STRING_LITERAL_TERMINAL_LIST = new Nonterminal(67, "<STRING LITERAL terminal list>");
        public static final Nonterminal ACC_VECTOR_CLAUSE = new Nonterminal(68, "<acc vector clause>");
        public static final Nonterminal AND_EXPRESSION = new Nonterminal(69, "<and expression>");
        public static final Nonterminal ACC_PRESENTORCOPY_CLAUSE = new Nonterminal(70, "<acc presentorcopy clause>");
        public static final Nonterminal ACC_WORKER_CLAUSE = new Nonterminal(71, "<acc worker clause>");
        public static final Nonterminal ACC_LOOP_CLAUSE = new Nonterminal(72, "<acc loop clause>");
        public static final Nonterminal ACC_USEDEVICE_CLAUSE = new Nonterminal(73, "<acc usedevice clause>");
        public static final Nonterminal ACC_KERNELS_LOOP_CLAUSE_LIST = new Nonterminal(74, "<acc kernels loop clause list>");
        public static final Nonterminal ACC_HOSTDATA_CLAUSE_LIST = new Nonterminal(75, "<acc hostdata clause list>");
        public static final Nonterminal ACC_WAIT = new Nonterminal(76, "<acc wait>");
        public static final Nonterminal ACC_COUNT = new Nonterminal(77, "<acc count>");
        public static final Nonterminal ACC_EXIT_DATA_CLAUSE_LIST = new Nonterminal(78, "<acc exit data clause list>");
        public static final Nonterminal CONDITIONAL_EXPRESSION = new Nonterminal(79, "<conditional expression>");
        public static final Nonterminal ACC_CREATE_CLAUSE = new Nonterminal(80, "<acc create clause>");
        public static final Nonterminal ACC_LOOP_CLAUSE_LIST = new Nonterminal(81, "<acc loop clause list>");
        public static final Nonterminal ACC_ROUTINE = new Nonterminal(82, "<acc routine>");
        public static final Nonterminal ACC_COPYOUT_CLAUSE = new Nonterminal(83, "<acc copyout clause>");
        public static final Nonterminal ACC_PARALLEL_LOOP = new Nonterminal(84, "<acc parallel loop>");
        public static final Nonterminal ACC_IF_CLAUSE = new Nonterminal(85, "<acc if clause>");
        public static final Nonterminal ACC_DATA_LIST = new Nonterminal(86, "<acc data list>");
        public static final Nonterminal ACC_PARALLEL = new Nonterminal(87, "<acc parallel>");
        public static final Nonterminal ACC_ENTER_DATA_CLAUSE_LIST = new Nonterminal(88, "<acc enter data clause list>");
        public static final Nonterminal ACC_DATA_CLAUSE_LIST = new Nonterminal(89, "<acc data clause list>");
        public static final Nonterminal ACC_ATOMIC_CLAUSE = new Nonterminal(90, "<acc atomic clause>");
        public static final Nonterminal EQUALITY_EXPRESSION = new Nonterminal(91, "<equality expression>");
        public static final Nonterminal ACC_DECLARE_CLAUSE = new Nonterminal(92, "<acc declare clause>");
        public static final Nonterminal RELATIONAL_EXPRESSION = new Nonterminal(93, "<relational expression>");
        public static final Nonterminal ACC_PARALLEL_LOOP_CLAUSE_LIST = new Nonterminal(94, "<acc parallel loop clause list>");
        public static final Nonterminal ACC_EXIT_DATA = new Nonterminal(95, "<acc exit data>");
        public static final Nonterminal ACC_TILE_CLAUSE = new Nonterminal(96, "<acc tile clause>");
        public static final Nonterminal ACC_WAIT_CLAUSE = new Nonterminal(97, "<acc wait clause>");
        public static final Nonterminal ARGUMENT_EXPRESSION_LIST = new Nonterminal(98, "<argument expression list>");
        public static final Nonterminal ACC_DECLARE_CLAUSE_LIST = new Nonterminal(99, "<acc declare clause list>");
        public static final Nonterminal ADDITIVE_EXPRESSION = new Nonterminal(100, "<additive expression>");
        public static final Nonterminal ACC_NUMGANGS_CLAUSE = new Nonterminal(101, "<acc numgangs clause>");
        public static final Nonterminal ACC_EXIT_DATA_CLAUSE = new Nonterminal(102, "<acc exit data clause>");
        public static final Nonterminal ACC_PRESENTORCOPYOUT_CLAUSE = new Nonterminal(103, "<acc presentorcopyout clause>");

        protected int index;
        protected String description;

        protected Nonterminal(int index, String description)
        {
            assert 0 <= index && index < NUM_NONTERMINALS;

            this.index = index;
            this.description = description;
        }

        protected int getIndex()
        {
            return index;
        }

        @Override public String toString()
        {
            return description;
        }
    }

    /**
     * A production in the grammar.
     * <p>
     * This class enumerates all of the productions (including error recovery
     * productions) in the grammar as constant <code>Production</code> objects.
     */
    public static final class Production
    {
        protected Nonterminal lhs;
        protected int length;
        protected String description;

        protected Production(Nonterminal lhs, int length, String description)
        {
            assert lhs != null && length >= 0;

            this.lhs = lhs;
            this.length = length;
            this.description = description;
        }

        /**
         * Returns the nonterminal on the left-hand side of this production.
         *
         * @return the nonterminal on the left-hand side of this production
         */
        public Nonterminal getLHS()
        {
            return lhs;
        }

        /**
         * Returns the number of symbols on the right-hand side of this
         * production.  If it is an error recovery production, returns the
         * number of symbols preceding the lookahead symbol.
         *
         * @return the length of the production (non-negative)
         */
        public int length()
        {
            return length;
        }

        @Override public String toString()
        {
            return description;
        }

        public static Production get(int index)
        {
            assert 0 <= index && index < NUM_PRODUCTIONS;

            return Production.values[index];
        }

        public static final Production ACC_CONSTRUCT_1 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-loop>");
        public static final Production ACC_CONSTRUCT_2 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-parallel-loop>");
        public static final Production ACC_CONSTRUCT_3 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-kernels-loop>");
        public static final Production ACC_CONSTRUCT_4 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-parallel>");
        public static final Production ACC_CONSTRUCT_5 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-kernels>");
        public static final Production ACC_CONSTRUCT_6 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-data>");
        public static final Production ACC_CONSTRUCT_7 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-hostdata>");
        public static final Production ACC_CONSTRUCT_8 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-declare>");
        public static final Production ACC_CONSTRUCT_9 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-cache>");
        public static final Production ACC_CONSTRUCT_10 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-update>");
        public static final Production ACC_CONSTRUCT_11 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-wait>");
        public static final Production ACC_CONSTRUCT_12 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-enter-data>");
        public static final Production ACC_CONSTRUCT_13 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-exit-data>");
        public static final Production ACC_CONSTRUCT_14 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-routine>");
        public static final Production ACC_CONSTRUCT_15 = new Production(Nonterminal.ACC_CONSTRUCT, 1, "<acc-construct> ::= <acc-atomic>");
        public static final Production ACC_CONSTRUCT_16 = new Production(Nonterminal.ACC_CONSTRUCT, 0, "<acc-construct> ::= (empty)");
        public static final Production ACC_LOOP_17 = new Production(Nonterminal.ACC_LOOP, 3, "<acc-loop> ::= PRAGMA_ACC literal-string-loop <acc-loop-clause-list>");
        public static final Production ACC_LOOP_18 = new Production(Nonterminal.ACC_LOOP, 2, "<acc-loop> ::= PRAGMA_ACC literal-string-loop");
        public static final Production ACC_LOOP_CLAUSE_LIST_19 = new Production(Nonterminal.ACC_LOOP_CLAUSE_LIST, 1, "<acc-loop-clause-list> ::= <acc-loop-clause>");
        public static final Production ACC_LOOP_CLAUSE_LIST_20 = new Production(Nonterminal.ACC_LOOP_CLAUSE_LIST, 2, "<acc-loop-clause-list> ::= <acc-loop-clause-list> <acc-loop-clause>");
        public static final Production ACC_LOOP_CLAUSE_LIST_21 = new Production(Nonterminal.ACC_LOOP_CLAUSE_LIST, 3, "<acc-loop-clause-list> ::= <acc-loop-clause-list> literal-string-comma <acc-loop-clause>");
        public static final Production ACC_LOOP_CLAUSE_22 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= <acc-collapse-clause>");
        public static final Production ACC_LOOP_CLAUSE_23 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= <acc-gang-clause>");
        public static final Production ACC_LOOP_CLAUSE_24 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= <acc-worker-clause>");
        public static final Production ACC_LOOP_CLAUSE_25 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= <acc-vector-clause>");
        public static final Production ACC_LOOP_CLAUSE_26 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= literal-string-seq");
        public static final Production ACC_LOOP_CLAUSE_27 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= literal-string-auto");
        public static final Production ACC_LOOP_CLAUSE_28 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= <acc-tile-clause>");
        public static final Production ACC_LOOP_CLAUSE_29 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= literal-string-independent");
        public static final Production ACC_LOOP_CLAUSE_30 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= <acc-private-clause>");
        public static final Production ACC_LOOP_CLAUSE_31 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= <acc-reduction-clause>");
        public static final Production ACC_PARALLEL_32 = new Production(Nonterminal.ACC_PARALLEL, 3, "<acc-parallel> ::= PRAGMA_ACC literal-string-parallel <acc-parallel-clause-list>");
        public static final Production ACC_PARALLEL_33 = new Production(Nonterminal.ACC_PARALLEL, 2, "<acc-parallel> ::= PRAGMA_ACC literal-string-parallel");
        public static final Production ACC_PARALLEL_CLAUSE_LIST_34 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE_LIST, 1, "<acc-parallel-clause-list> ::= <acc-parallel-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_LIST_35 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE_LIST, 2, "<acc-parallel-clause-list> ::= <acc-parallel-clause-list> <acc-parallel-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_LIST_36 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE_LIST, 3, "<acc-parallel-clause-list> ::= <acc-parallel-clause-list> literal-string-comma <acc-parallel-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_37 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-if-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_38 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-async-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_39 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-wait-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_40 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-numgangs-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_41 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-numworkers-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_42 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-vectorlength-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_43 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-reduction-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_44 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-copy-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_45 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-copyin-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_46 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-copyout-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_47 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-create-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_48 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-present-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_49 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-presentorcopy-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_50 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-presentorcopyin-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_51 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-presentorcopyout-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_52 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-presentorcreate-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_53 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-deviceptr-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_54 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-private-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_55 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-firstprivate-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_56 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-defaultnone-clause>");
        public static final Production ACC_PARALLEL_LOOP_57 = new Production(Nonterminal.ACC_PARALLEL_LOOP, 4, "<acc-parallel-loop> ::= PRAGMA_ACC literal-string-parallel literal-string-loop <acc-parallel-loop-clause-list>");
        public static final Production ACC_PARALLEL_LOOP_58 = new Production(Nonterminal.ACC_PARALLEL_LOOP, 3, "<acc-parallel-loop> ::= PRAGMA_ACC literal-string-parallel literal-string-loop");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_LIST_59 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE_LIST, 1, "<acc-parallel-loop-clause-list> ::= <acc-parallel-loop-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_LIST_60 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE_LIST, 2, "<acc-parallel-loop-clause-list> ::= <acc-parallel-loop-clause-list> <acc-parallel-loop-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_LIST_61 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE_LIST, 3, "<acc-parallel-loop-clause-list> ::= <acc-parallel-loop-clause-list> literal-string-comma <acc-parallel-loop-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_62 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-collapse-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_63 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-gang-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_64 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-worker-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_65 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-vector-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_66 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= literal-string-seq");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_67 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= literal-string-auto");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_68 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-tile-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_69 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= literal-string-independent");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_70 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-private-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_71 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-reduction-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_72 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-if-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_73 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-async-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_74 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-wait-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_75 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-numgangs-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_76 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-numworkers-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_77 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-vectorlength-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_78 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-copy-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_79 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-copyin-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_80 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-copyout-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_81 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-create-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_82 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-present-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_83 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-presentorcopy-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_84 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-presentorcopyin-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_85 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-presentorcopyout-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_86 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-presentorcreate-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_87 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-deviceptr-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_88 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-firstprivate-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_89 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-defaultnone-clause>");
        public static final Production ACC_KERNELS_90 = new Production(Nonterminal.ACC_KERNELS, 3, "<acc-kernels> ::= PRAGMA_ACC literal-string-kernels <acc-kernels-clause-list>");
        public static final Production ACC_KERNELS_91 = new Production(Nonterminal.ACC_KERNELS, 2, "<acc-kernels> ::= PRAGMA_ACC literal-string-kernels");
        public static final Production ACC_KERNELS_CLAUSE_LIST_92 = new Production(Nonterminal.ACC_KERNELS_CLAUSE_LIST, 1, "<acc-kernels-clause-list> ::= <acc-kernels-clause>");
        public static final Production ACC_KERNELS_CLAUSE_LIST_93 = new Production(Nonterminal.ACC_KERNELS_CLAUSE_LIST, 2, "<acc-kernels-clause-list> ::= <acc-kernels-clause-list> <acc-kernels-clause>");
        public static final Production ACC_KERNELS_CLAUSE_LIST_94 = new Production(Nonterminal.ACC_KERNELS_CLAUSE_LIST, 3, "<acc-kernels-clause-list> ::= <acc-kernels-clause-list> literal-string-comma <acc-kernels-clause>");
        public static final Production ACC_KERNELS_CLAUSE_95 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-if-clause>");
        public static final Production ACC_KERNELS_CLAUSE_96 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-async-clause>");
        public static final Production ACC_KERNELS_CLAUSE_97 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-wait-clause>");
        public static final Production ACC_KERNELS_CLAUSE_98 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-copy-clause>");
        public static final Production ACC_KERNELS_CLAUSE_99 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-copyin-clause>");
        public static final Production ACC_KERNELS_CLAUSE_100 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-copyout-clause>");
        public static final Production ACC_KERNELS_CLAUSE_101 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-create-clause>");
        public static final Production ACC_KERNELS_CLAUSE_102 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-present-clause>");
        public static final Production ACC_KERNELS_CLAUSE_103 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-presentorcopy-clause>");
        public static final Production ACC_KERNELS_CLAUSE_104 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-presentorcopyin-clause>");
        public static final Production ACC_KERNELS_CLAUSE_105 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-presentorcopyout-clause>");
        public static final Production ACC_KERNELS_CLAUSE_106 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-presentorcreate-clause>");
        public static final Production ACC_KERNELS_CLAUSE_107 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-deviceptr-clause>");
        public static final Production ACC_KERNELS_CLAUSE_108 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-defaultnone-clause>");
        public static final Production ACC_KERNELS_LOOP_109 = new Production(Nonterminal.ACC_KERNELS_LOOP, 4, "<acc-kernels-loop> ::= PRAGMA_ACC literal-string-kernels literal-string-loop <acc-kernels-loop-clause-list>");
        public static final Production ACC_KERNELS_LOOP_110 = new Production(Nonterminal.ACC_KERNELS_LOOP, 3, "<acc-kernels-loop> ::= PRAGMA_ACC literal-string-kernels literal-string-loop");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_LIST_111 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE_LIST, 1, "<acc-kernels-loop-clause-list> ::= <acc-kernels-loop-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_LIST_112 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE_LIST, 2, "<acc-kernels-loop-clause-list> ::= <acc-kernels-loop-clause-list> <acc-kernels-loop-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_LIST_113 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE_LIST, 3, "<acc-kernels-loop-clause-list> ::= <acc-kernels-loop-clause-list> literal-string-comma <acc-kernels-loop-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_114 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-collapse-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_115 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-gang-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_116 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-worker-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_117 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-vector-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_118 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= literal-string-seq");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_119 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= literal-string-auto");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_120 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-tile-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_121 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= literal-string-independent");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_122 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-private-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_123 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-reduction-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_124 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-if-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_125 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-async-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_126 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-wait-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_127 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-copy-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_128 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-copyin-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_129 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-copyout-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_130 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-create-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_131 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-present-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_132 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-presentorcopy-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_133 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-presentorcopyin-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_134 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-presentorcopyout-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_135 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-presentorcreate-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_136 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-deviceptr-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_137 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-defaultnone-clause>");
        public static final Production ACC_DECLARE_138 = new Production(Nonterminal.ACC_DECLARE, 3, "<acc-declare> ::= PRAGMA_ACC literal-string-declare <acc-declare-clause-list>");
        public static final Production ACC_DECLARE_CLAUSE_LIST_139 = new Production(Nonterminal.ACC_DECLARE_CLAUSE_LIST, 1, "<acc-declare-clause-list> ::= <acc-declare-clause>");
        public static final Production ACC_DECLARE_CLAUSE_LIST_140 = new Production(Nonterminal.ACC_DECLARE_CLAUSE_LIST, 2, "<acc-declare-clause-list> ::= <acc-declare-clause-list> <acc-declare-clause>");
        public static final Production ACC_DECLARE_CLAUSE_LIST_141 = new Production(Nonterminal.ACC_DECLARE_CLAUSE_LIST, 3, "<acc-declare-clause-list> ::= <acc-declare-clause-list> literal-string-comma <acc-declare-clause>");
        public static final Production ACC_DECLARE_CLAUSE_142 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-copy-clause>");
        public static final Production ACC_DECLARE_CLAUSE_143 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-copyin-clause>");
        public static final Production ACC_DECLARE_CLAUSE_144 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-copyout-clause>");
        public static final Production ACC_DECLARE_CLAUSE_145 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-create-clause>");
        public static final Production ACC_DECLARE_CLAUSE_146 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-present-clause>");
        public static final Production ACC_DECLARE_CLAUSE_147 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-presentorcopy-clause>");
        public static final Production ACC_DECLARE_CLAUSE_148 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-presentorcopyin-clause>");
        public static final Production ACC_DECLARE_CLAUSE_149 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-presentorcopyout-clause>");
        public static final Production ACC_DECLARE_CLAUSE_150 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-presentorcreate-clause>");
        public static final Production ACC_DECLARE_CLAUSE_151 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-deviceptr-clause>");
        public static final Production ACC_DECLARE_CLAUSE_152 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-deviceresident-clause>");
        public static final Production ACC_DECLARE_CLAUSE_153 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-link-clause>");
        public static final Production ACC_DATA_154 = new Production(Nonterminal.ACC_DATA, 3, "<acc-data> ::= PRAGMA_ACC literal-string-data <acc-data-clause-list>");
        public static final Production ACC_DATA_CLAUSE_LIST_155 = new Production(Nonterminal.ACC_DATA_CLAUSE_LIST, 1, "<acc-data-clause-list> ::= <acc-data-clause>");
        public static final Production ACC_DATA_CLAUSE_LIST_156 = new Production(Nonterminal.ACC_DATA_CLAUSE_LIST, 2, "<acc-data-clause-list> ::= <acc-data-clause-list> <acc-data-clause>");
        public static final Production ACC_DATA_CLAUSE_LIST_157 = new Production(Nonterminal.ACC_DATA_CLAUSE_LIST, 3, "<acc-data-clause-list> ::= <acc-data-clause-list> literal-string-comma <acc-data-clause>");
        public static final Production ACC_DATA_CLAUSE_158 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-if-clause>");
        public static final Production ACC_DATA_CLAUSE_159 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-copy-clause>");
        public static final Production ACC_DATA_CLAUSE_160 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-copyin-clause>");
        public static final Production ACC_DATA_CLAUSE_161 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-copyout-clause>");
        public static final Production ACC_DATA_CLAUSE_162 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-create-clause>");
        public static final Production ACC_DATA_CLAUSE_163 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-present-clause>");
        public static final Production ACC_DATA_CLAUSE_164 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-presentorcopy-clause>");
        public static final Production ACC_DATA_CLAUSE_165 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-presentorcopyin-clause>");
        public static final Production ACC_DATA_CLAUSE_166 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-presentorcopyout-clause>");
        public static final Production ACC_DATA_CLAUSE_167 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-presentorcreate-clause>");
        public static final Production ACC_DATA_CLAUSE_168 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-deviceptr-clause>");
        public static final Production ACC_HOSTDATA_169 = new Production(Nonterminal.ACC_HOSTDATA, 3, "<acc-hostdata> ::= PRAGMA_ACC literal-string-host-underscoredata <acc-hostdata-clause-list>");
        public static final Production ACC_HOSTDATA_CLAUSE_LIST_170 = new Production(Nonterminal.ACC_HOSTDATA_CLAUSE_LIST, 1, "<acc-hostdata-clause-list> ::= <acc-hostdata-clause>");
        public static final Production ACC_HOSTDATA_CLAUSE_LIST_171 = new Production(Nonterminal.ACC_HOSTDATA_CLAUSE_LIST, 2, "<acc-hostdata-clause-list> ::= <acc-hostdata-clause-list> <acc-hostdata-clause>");
        public static final Production ACC_HOSTDATA_CLAUSE_LIST_172 = new Production(Nonterminal.ACC_HOSTDATA_CLAUSE_LIST, 3, "<acc-hostdata-clause-list> ::= <acc-hostdata-clause-list> literal-string-comma <acc-hostdata-clause>");
        public static final Production ACC_HOSTDATA_CLAUSE_173 = new Production(Nonterminal.ACC_HOSTDATA_CLAUSE, 1, "<acc-hostdata-clause> ::= <acc-usedevice-clause>");
        public static final Production ACC_CACHE_174 = new Production(Nonterminal.ACC_CACHE, 5, "<acc-cache> ::= PRAGMA_ACC literal-string-cache literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_WAIT_175 = new Production(Nonterminal.ACC_WAIT, 4, "<acc-wait> ::= PRAGMA_ACC literal-string-wait <acc-wait-parameter> <acc-wait-clause-list>");
        public static final Production ACC_WAIT_176 = new Production(Nonterminal.ACC_WAIT, 3, "<acc-wait> ::= PRAGMA_ACC literal-string-wait <acc-wait-parameter>");
        public static final Production ACC_WAIT_177 = new Production(Nonterminal.ACC_WAIT, 3, "<acc-wait> ::= PRAGMA_ACC literal-string-wait <acc-wait-clause-list>");
        public static final Production ACC_WAIT_178 = new Production(Nonterminal.ACC_WAIT, 2, "<acc-wait> ::= PRAGMA_ACC literal-string-wait");
        public static final Production ACC_WAIT_PARAMETER_179 = new Production(Nonterminal.ACC_WAIT_PARAMETER, 3, "<acc-wait-parameter> ::= literal-string-lparen <constant-expression> literal-string-rparen");
        public static final Production ACC_WAIT_CLAUSE_LIST_180 = new Production(Nonterminal.ACC_WAIT_CLAUSE_LIST, 1, "<acc-wait-clause-list> ::= <acc-async-clause>");
        public static final Production ACC_WAIT_CLAUSE_LIST_181 = new Production(Nonterminal.ACC_WAIT_CLAUSE_LIST, 2, "<acc-wait-clause-list> ::= <acc-wait-clause-list> <acc-async-clause>");
        public static final Production ACC_WAIT_CLAUSE_LIST_182 = new Production(Nonterminal.ACC_WAIT_CLAUSE_LIST, 3, "<acc-wait-clause-list> ::= <acc-wait-clause-list> literal-string-comma <acc-async-clause>");
        public static final Production ACC_UPDATE_183 = new Production(Nonterminal.ACC_UPDATE, 3, "<acc-update> ::= PRAGMA_ACC literal-string-update <acc-update-clause-list>");
        public static final Production ACC_UPDATE_CLAUSE_LIST_184 = new Production(Nonterminal.ACC_UPDATE_CLAUSE_LIST, 1, "<acc-update-clause-list> ::= <acc-update-clause>");
        public static final Production ACC_UPDATE_CLAUSE_LIST_185 = new Production(Nonterminal.ACC_UPDATE_CLAUSE_LIST, 2, "<acc-update-clause-list> ::= <acc-update-clause-list> <acc-update-clause>");
        public static final Production ACC_UPDATE_CLAUSE_LIST_186 = new Production(Nonterminal.ACC_UPDATE_CLAUSE_LIST, 3, "<acc-update-clause-list> ::= <acc-update-clause-list> literal-string-comma <acc-update-clause>");
        public static final Production ACC_UPDATE_CLAUSE_187 = new Production(Nonterminal.ACC_UPDATE_CLAUSE, 1, "<acc-update-clause> ::= <acc-async-clause>");
        public static final Production ACC_UPDATE_CLAUSE_188 = new Production(Nonterminal.ACC_UPDATE_CLAUSE, 1, "<acc-update-clause> ::= <acc-wait-clause>");
        public static final Production ACC_UPDATE_CLAUSE_189 = new Production(Nonterminal.ACC_UPDATE_CLAUSE, 1, "<acc-update-clause> ::= <acc-if-clause>");
        public static final Production ACC_UPDATE_CLAUSE_190 = new Production(Nonterminal.ACC_UPDATE_CLAUSE, 1, "<acc-update-clause> ::= <acc-self-clause>");
        public static final Production ACC_UPDATE_CLAUSE_191 = new Production(Nonterminal.ACC_UPDATE_CLAUSE, 1, "<acc-update-clause> ::= <acc-host-clause>");
        public static final Production ACC_UPDATE_CLAUSE_192 = new Production(Nonterminal.ACC_UPDATE_CLAUSE, 1, "<acc-update-clause> ::= <acc-device-clause>");
        public static final Production ACC_ENTER_DATA_193 = new Production(Nonterminal.ACC_ENTER_DATA, 4, "<acc-enter-data> ::= PRAGMA_ACC literal-string-enter literal-string-data <acc-enter-data-clause-list>");
        public static final Production ACC_ENTER_DATA_CLAUSE_LIST_194 = new Production(Nonterminal.ACC_ENTER_DATA_CLAUSE_LIST, 1, "<acc-enter-data-clause-list> ::= <acc-enter-data-clause>");
        public static final Production ACC_ENTER_DATA_CLAUSE_LIST_195 = new Production(Nonterminal.ACC_ENTER_DATA_CLAUSE_LIST, 2, "<acc-enter-data-clause-list> ::= <acc-enter-data-clause-list> <acc-enter-data-clause>");
        public static final Production ACC_ENTER_DATA_CLAUSE_LIST_196 = new Production(Nonterminal.ACC_ENTER_DATA_CLAUSE_LIST, 3, "<acc-enter-data-clause-list> ::= <acc-enter-data-clause-list> literal-string-comma <acc-enter-data-clause>");
        public static final Production ACC_ENTER_DATA_CLAUSE_197 = new Production(Nonterminal.ACC_ENTER_DATA_CLAUSE, 1, "<acc-enter-data-clause> ::= <acc-host-clause>");
        public static final Production ACC_ENTER_DATA_CLAUSE_198 = new Production(Nonterminal.ACC_ENTER_DATA_CLAUSE, 1, "<acc-enter-data-clause> ::= <acc-if-clause>");
        public static final Production ACC_ENTER_DATA_CLAUSE_199 = new Production(Nonterminal.ACC_ENTER_DATA_CLAUSE, 1, "<acc-enter-data-clause> ::= <acc-async-clause>");
        public static final Production ACC_ENTER_DATA_CLAUSE_200 = new Production(Nonterminal.ACC_ENTER_DATA_CLAUSE, 1, "<acc-enter-data-clause> ::= <acc-wait-clause>");
        public static final Production ACC_ENTER_DATA_CLAUSE_201 = new Production(Nonterminal.ACC_ENTER_DATA_CLAUSE, 1, "<acc-enter-data-clause> ::= <acc-copyin-clause>");
        public static final Production ACC_ENTER_DATA_CLAUSE_202 = new Production(Nonterminal.ACC_ENTER_DATA_CLAUSE, 1, "<acc-enter-data-clause> ::= <acc-create-clause>");
        public static final Production ACC_ENTER_DATA_CLAUSE_203 = new Production(Nonterminal.ACC_ENTER_DATA_CLAUSE, 1, "<acc-enter-data-clause> ::= <acc-presentorcopyin-clause>");
        public static final Production ACC_ENTER_DATA_CLAUSE_204 = new Production(Nonterminal.ACC_ENTER_DATA_CLAUSE, 1, "<acc-enter-data-clause> ::= <acc-presentorcreate-clause>");
        public static final Production ACC_EXIT_DATA_205 = new Production(Nonterminal.ACC_EXIT_DATA, 4, "<acc-exit-data> ::= PRAGMA_ACC literal-string-exit literal-string-data <acc-exit-data-clause-list>");
        public static final Production ACC_EXIT_DATA_CLAUSE_LIST_206 = new Production(Nonterminal.ACC_EXIT_DATA_CLAUSE_LIST, 1, "<acc-exit-data-clause-list> ::= <acc-exit-data-clause>");
        public static final Production ACC_EXIT_DATA_CLAUSE_LIST_207 = new Production(Nonterminal.ACC_EXIT_DATA_CLAUSE_LIST, 2, "<acc-exit-data-clause-list> ::= <acc-exit-data-clause-list> <acc-exit-data-clause>");
        public static final Production ACC_EXIT_DATA_CLAUSE_LIST_208 = new Production(Nonterminal.ACC_EXIT_DATA_CLAUSE_LIST, 3, "<acc-exit-data-clause-list> ::= <acc-exit-data-clause-list> literal-string-comma <acc-exit-data-clause>");
        public static final Production ACC_EXIT_DATA_CLAUSE_209 = new Production(Nonterminal.ACC_EXIT_DATA_CLAUSE, 1, "<acc-exit-data-clause> ::= <acc-host-clause>");
        public static final Production ACC_EXIT_DATA_CLAUSE_210 = new Production(Nonterminal.ACC_EXIT_DATA_CLAUSE, 1, "<acc-exit-data-clause> ::= <acc-if-clause>");
        public static final Production ACC_EXIT_DATA_CLAUSE_211 = new Production(Nonterminal.ACC_EXIT_DATA_CLAUSE, 1, "<acc-exit-data-clause> ::= <acc-async-clause>");
        public static final Production ACC_EXIT_DATA_CLAUSE_212 = new Production(Nonterminal.ACC_EXIT_DATA_CLAUSE, 1, "<acc-exit-data-clause> ::= <acc-wait-clause>");
        public static final Production ACC_EXIT_DATA_CLAUSE_213 = new Production(Nonterminal.ACC_EXIT_DATA_CLAUSE, 1, "<acc-exit-data-clause> ::= <acc-copyout-clause>");
        public static final Production ACC_EXIT_DATA_CLAUSE_214 = new Production(Nonterminal.ACC_EXIT_DATA_CLAUSE, 1, "<acc-exit-data-clause> ::= <acc-delete-clause>");
        public static final Production ACC_ROUTINE_215 = new Production(Nonterminal.ACC_ROUTINE, 3, "<acc-routine> ::= PRAGMA_ACC literal-string-routine <acc-routine-clause-list>");
        public static final Production ACC_ROUTINE_216 = new Production(Nonterminal.ACC_ROUTINE, 6, "<acc-routine> ::= PRAGMA_ACC literal-string-routine literal-string-lparen <identifier> literal-string-rparen <acc-routine-clause-list>");
        public static final Production ACC_ROUTINE_CLAUSE_LIST_217 = new Production(Nonterminal.ACC_ROUTINE_CLAUSE_LIST, 1, "<acc-routine-clause-list> ::= <acc-routine-clause>");
        public static final Production ACC_ROUTINE_CLAUSE_LIST_218 = new Production(Nonterminal.ACC_ROUTINE_CLAUSE_LIST, 2, "<acc-routine-clause-list> ::= <acc-routine-clause-list> <acc-routine-clause>");
        public static final Production ACC_ROUTINE_CLAUSE_LIST_219 = new Production(Nonterminal.ACC_ROUTINE_CLAUSE_LIST, 3, "<acc-routine-clause-list> ::= <acc-routine-clause-list> literal-string-comma <acc-routine-clause>");
        public static final Production ACC_ROUTINE_CLAUSE_220 = new Production(Nonterminal.ACC_ROUTINE_CLAUSE, 1, "<acc-routine-clause> ::= <acc-gang-clause>");
        public static final Production ACC_ROUTINE_CLAUSE_221 = new Production(Nonterminal.ACC_ROUTINE_CLAUSE, 1, "<acc-routine-clause> ::= <acc-worker-clause>");
        public static final Production ACC_ROUTINE_CLAUSE_222 = new Production(Nonterminal.ACC_ROUTINE_CLAUSE, 1, "<acc-routine-clause> ::= <acc-vector-clause>");
        public static final Production ACC_ROUTINE_CLAUSE_223 = new Production(Nonterminal.ACC_ROUTINE_CLAUSE, 1, "<acc-routine-clause> ::= literal-string-seq");
        public static final Production ACC_ROUTINE_CLAUSE_224 = new Production(Nonterminal.ACC_ROUTINE_CLAUSE, 4, "<acc-routine-clause> ::= literal-string-bind literal-string-lparen IDENTIFIER literal-string-rparen");
        public static final Production ACC_ROUTINE_CLAUSE_225 = new Production(Nonterminal.ACC_ROUTINE_CLAUSE, 4, "<acc-routine-clause> ::= literal-string-bind literal-string-lparen STRING-LITERAL literal-string-rparen");
        public static final Production ACC_ROUTINE_CLAUSE_226 = new Production(Nonterminal.ACC_ROUTINE_CLAUSE, 1, "<acc-routine-clause> ::= literal-string-nohost");
        public static final Production ACC_ATOMIC_227 = new Production(Nonterminal.ACC_ATOMIC, 2, "<acc-atomic> ::= PRAGMA_ACC literal-string-atomic");
        public static final Production ACC_ATOMIC_228 = new Production(Nonterminal.ACC_ATOMIC, 3, "<acc-atomic> ::= PRAGMA_ACC literal-string-atomic <acc-atomic-clause>");
        public static final Production ACC_ATOMIC_CLAUSE_229 = new Production(Nonterminal.ACC_ATOMIC_CLAUSE, 1, "<acc-atomic-clause> ::= literal-string-read");
        public static final Production ACC_ATOMIC_CLAUSE_230 = new Production(Nonterminal.ACC_ATOMIC_CLAUSE, 1, "<acc-atomic-clause> ::= literal-string-write");
        public static final Production ACC_ATOMIC_CLAUSE_231 = new Production(Nonterminal.ACC_ATOMIC_CLAUSE, 1, "<acc-atomic-clause> ::= literal-string-update");
        public static final Production ACC_ATOMIC_CLAUSE_232 = new Production(Nonterminal.ACC_ATOMIC_CLAUSE, 1, "<acc-atomic-clause> ::= literal-string-capture");
        public static final Production ACC_COLLAPSE_CLAUSE_233 = new Production(Nonterminal.ACC_COLLAPSE_CLAUSE, 2, "<acc-collapse-clause> ::= literal-string-collapse <acc-count>");
        public static final Production ACC_GANG_CLAUSE_234 = new Production(Nonterminal.ACC_GANG_CLAUSE, 2, "<acc-gang-clause> ::= literal-string-gang <acc-count>");
        public static final Production ACC_GANG_CLAUSE_235 = new Production(Nonterminal.ACC_GANG_CLAUSE, 1, "<acc-gang-clause> ::= literal-string-gang");
        public static final Production ACC_WORKER_CLAUSE_236 = new Production(Nonterminal.ACC_WORKER_CLAUSE, 2, "<acc-worker-clause> ::= literal-string-worker <acc-count>");
        public static final Production ACC_WORKER_CLAUSE_237 = new Production(Nonterminal.ACC_WORKER_CLAUSE, 1, "<acc-worker-clause> ::= literal-string-worker");
        public static final Production ACC_VECTOR_CLAUSE_238 = new Production(Nonterminal.ACC_VECTOR_CLAUSE, 2, "<acc-vector-clause> ::= literal-string-vector <acc-count>");
        public static final Production ACC_VECTOR_CLAUSE_239 = new Production(Nonterminal.ACC_VECTOR_CLAUSE, 1, "<acc-vector-clause> ::= literal-string-vector");
        public static final Production ACC_PRIVATE_CLAUSE_240 = new Production(Nonterminal.ACC_PRIVATE_CLAUSE, 4, "<acc-private-clause> ::= literal-string-private literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_FIRSTPRIVATE_CLAUSE_241 = new Production(Nonterminal.ACC_FIRSTPRIVATE_CLAUSE, 4, "<acc-firstprivate-clause> ::= literal-string-firstprivate literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_REDUCTION_CLAUSE_242 = new Production(Nonterminal.ACC_REDUCTION_CLAUSE, 6, "<acc-reduction-clause> ::= literal-string-reduction literal-string-lparen <acc-reduction-operator> literal-string-colon <identifier-list> literal-string-rparen");
        public static final Production ACC_IF_CLAUSE_243 = new Production(Nonterminal.ACC_IF_CLAUSE, 4, "<acc-if-clause> ::= literal-string-if literal-string-lparen <conditional-expression> literal-string-rparen");
        public static final Production ACC_ASYNC_CLAUSE_244 = new Production(Nonterminal.ACC_ASYNC_CLAUSE, 2, "<acc-async-clause> ::= literal-string-async <acc-count>");
        public static final Production ACC_ASYNC_CLAUSE_245 = new Production(Nonterminal.ACC_ASYNC_CLAUSE, 1, "<acc-async-clause> ::= literal-string-async");
        public static final Production ACC_WAIT_CLAUSE_246 = new Production(Nonterminal.ACC_WAIT_CLAUSE, 4, "<acc-wait-clause> ::= literal-string-wait literal-string-lparen <argument-expression-list> literal-string-rparen");
        public static final Production ACC_NUMGANGS_CLAUSE_247 = new Production(Nonterminal.ACC_NUMGANGS_CLAUSE, 2, "<acc-numgangs-clause> ::= literal-string-num-underscoregangs <acc-count>");
        public static final Production ACC_NUMWORKERS_CLAUSE_248 = new Production(Nonterminal.ACC_NUMWORKERS_CLAUSE, 2, "<acc-numworkers-clause> ::= literal-string-num-underscoreworkers <acc-count>");
        public static final Production ACC_VECTORLENGTH_CLAUSE_249 = new Production(Nonterminal.ACC_VECTORLENGTH_CLAUSE, 2, "<acc-vectorlength-clause> ::= literal-string-vector-underscorelength <acc-count>");
        public static final Production ACC_DELETE_CLAUSE_250 = new Production(Nonterminal.ACC_DELETE_CLAUSE, 4, "<acc-delete-clause> ::= literal-string-delete literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_COPY_CLAUSE_251 = new Production(Nonterminal.ACC_COPY_CLAUSE, 4, "<acc-copy-clause> ::= literal-string-copy literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_COPYIN_CLAUSE_252 = new Production(Nonterminal.ACC_COPYIN_CLAUSE, 4, "<acc-copyin-clause> ::= literal-string-copyin literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_COPYOUT_CLAUSE_253 = new Production(Nonterminal.ACC_COPYOUT_CLAUSE, 4, "<acc-copyout-clause> ::= literal-string-copyout literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_CREATE_CLAUSE_254 = new Production(Nonterminal.ACC_CREATE_CLAUSE, 4, "<acc-create-clause> ::= literal-string-create literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENT_CLAUSE_255 = new Production(Nonterminal.ACC_PRESENT_CLAUSE, 4, "<acc-present-clause> ::= literal-string-present literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCOPY_CLAUSE_256 = new Production(Nonterminal.ACC_PRESENTORCOPY_CLAUSE, 4, "<acc-presentorcopy-clause> ::= literal-string-present-underscoreor-underscorecopy literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCOPY_CLAUSE_257 = new Production(Nonterminal.ACC_PRESENTORCOPY_CLAUSE, 4, "<acc-presentorcopy-clause> ::= literal-string-pcopy literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCOPYIN_CLAUSE_258 = new Production(Nonterminal.ACC_PRESENTORCOPYIN_CLAUSE, 4, "<acc-presentorcopyin-clause> ::= literal-string-present-underscoreor-underscorecopyin literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCOPYIN_CLAUSE_259 = new Production(Nonterminal.ACC_PRESENTORCOPYIN_CLAUSE, 4, "<acc-presentorcopyin-clause> ::= literal-string-pcopyin literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCOPYOUT_CLAUSE_260 = new Production(Nonterminal.ACC_PRESENTORCOPYOUT_CLAUSE, 4, "<acc-presentorcopyout-clause> ::= literal-string-present-underscoreor-underscorecopyout literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCOPYOUT_CLAUSE_261 = new Production(Nonterminal.ACC_PRESENTORCOPYOUT_CLAUSE, 4, "<acc-presentorcopyout-clause> ::= literal-string-pcopyout literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCREATE_CLAUSE_262 = new Production(Nonterminal.ACC_PRESENTORCREATE_CLAUSE, 4, "<acc-presentorcreate-clause> ::= literal-string-present-underscoreor-underscorecreate literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCREATE_CLAUSE_263 = new Production(Nonterminal.ACC_PRESENTORCREATE_CLAUSE, 4, "<acc-presentorcreate-clause> ::= literal-string-pcreate literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_DEVICEPTR_CLAUSE_264 = new Production(Nonterminal.ACC_DEVICEPTR_CLAUSE, 4, "<acc-deviceptr-clause> ::= literal-string-deviceptr literal-string-lparen <identifier-list> literal-string-rparen");
        public static final Production ACC_DEVICERESIDENT_CLAUSE_265 = new Production(Nonterminal.ACC_DEVICERESIDENT_CLAUSE, 4, "<acc-deviceresident-clause> ::= literal-string-device-underscoreresident literal-string-lparen <identifier-list> literal-string-rparen");
        public static final Production ACC_USEDEVICE_CLAUSE_266 = new Production(Nonterminal.ACC_USEDEVICE_CLAUSE, 4, "<acc-usedevice-clause> ::= literal-string-use-underscoredevice literal-string-lparen <identifier-list> literal-string-rparen");
        public static final Production ACC_SELF_CLAUSE_267 = new Production(Nonterminal.ACC_SELF_CLAUSE, 4, "<acc-self-clause> ::= literal-string-self literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_HOST_CLAUSE_268 = new Production(Nonterminal.ACC_HOST_CLAUSE, 4, "<acc-host-clause> ::= literal-string-host literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_DEVICE_CLAUSE_269 = new Production(Nonterminal.ACC_DEVICE_CLAUSE, 4, "<acc-device-clause> ::= literal-string-device literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_DEFAULTNONE_CLAUSE_270 = new Production(Nonterminal.ACC_DEFAULTNONE_CLAUSE, 4, "<acc-defaultnone-clause> ::= literal-string-default literal-string-lparen literal-string-none literal-string-rparen");
        public static final Production ACC_LINK_CLAUSE_271 = new Production(Nonterminal.ACC_LINK_CLAUSE, 4, "<acc-link-clause> ::= literal-string-link literal-string-lparen <identifier-list> literal-string-rparen");
        public static final Production ACC_TILE_CLAUSE_272 = new Production(Nonterminal.ACC_TILE_CLAUSE, 4, "<acc-tile-clause> ::= literal-string-tile literal-string-lparen <argument-expression-list> literal-string-rparen");
        public static final Production ACC_COUNT_273 = new Production(Nonterminal.ACC_COUNT, 3, "<acc-count> ::= literal-string-lparen <constant-expression> literal-string-rparen");
        public static final Production ACC_REDUCTION_OPERATOR_274 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-plus");
        public static final Production ACC_REDUCTION_OPERATOR_275 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-asterisk");
        public static final Production ACC_REDUCTION_OPERATOR_276 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-min");
        public static final Production ACC_REDUCTION_OPERATOR_277 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-max");
        public static final Production ACC_REDUCTION_OPERATOR_278 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-ampersand");
        public static final Production ACC_REDUCTION_OPERATOR_279 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-vbar");
        public static final Production ACC_REDUCTION_OPERATOR_280 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-caret");
        public static final Production ACC_REDUCTION_OPERATOR_281 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-ampersand-ampersand");
        public static final Production ACC_REDUCTION_OPERATOR_282 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-vbar-vbar");
        public static final Production ACC_DATA_LIST_283 = new Production(Nonterminal.ACC_DATA_LIST, 1, "<acc-data-list> ::= <acc-data-item>");
        public static final Production ACC_DATA_LIST_284 = new Production(Nonterminal.ACC_DATA_LIST, 3, "<acc-data-list> ::= <acc-data-list> literal-string-comma <acc-data-item>");
        public static final Production ACC_DATA_ITEM_285 = new Production(Nonterminal.ACC_DATA_ITEM, 1, "<acc-data-item> ::= <identifier>");
        public static final Production ACC_DATA_ITEM_286 = new Production(Nonterminal.ACC_DATA_ITEM, 6, "<acc-data-item> ::= <identifier> literal-string-lbracket <constant-expression> literal-string-colon <constant-expression> literal-string-rbracket");
        public static final Production PRIMARY_EXPRESSION_287 = new Production(Nonterminal.PRIMARY_EXPRESSION, 1, "<primary-expression> ::= <identifier>");
        public static final Production PRIMARY_EXPRESSION_288 = new Production(Nonterminal.PRIMARY_EXPRESSION, 1, "<primary-expression> ::= <constant>");
        public static final Production PRIMARY_EXPRESSION_289 = new Production(Nonterminal.PRIMARY_EXPRESSION, 1, "<primary-expression> ::= <STRING-LITERAL-terminal-list>");
        public static final Production PRIMARY_EXPRESSION_290 = new Production(Nonterminal.PRIMARY_EXPRESSION, 3, "<primary-expression> ::= literal-string-lparen <expression> literal-string-rparen");
        public static final Production POSTFIX_EXPRESSION_291 = new Production(Nonterminal.POSTFIX_EXPRESSION, 1, "<postfix-expression> ::= <primary-expression>");
        public static final Production POSTFIX_EXPRESSION_292 = new Production(Nonterminal.POSTFIX_EXPRESSION, 4, "<postfix-expression> ::= <postfix-expression> literal-string-lbracket <expression> literal-string-rbracket");
        public static final Production POSTFIX_EXPRESSION_293 = new Production(Nonterminal.POSTFIX_EXPRESSION, 4, "<postfix-expression> ::= <postfix-expression> literal-string-lparen <argument-expression-list> literal-string-rparen");
        public static final Production POSTFIX_EXPRESSION_294 = new Production(Nonterminal.POSTFIX_EXPRESSION, 3, "<postfix-expression> ::= <postfix-expression> literal-string-lparen literal-string-rparen");
        public static final Production POSTFIX_EXPRESSION_295 = new Production(Nonterminal.POSTFIX_EXPRESSION, 3, "<postfix-expression> ::= <postfix-expression> literal-string-period <identifier>");
        public static final Production POSTFIX_EXPRESSION_296 = new Production(Nonterminal.POSTFIX_EXPRESSION, 3, "<postfix-expression> ::= <postfix-expression> literal-string-hyphen-greaterthan <identifier>");
        public static final Production POSTFIX_EXPRESSION_297 = new Production(Nonterminal.POSTFIX_EXPRESSION, 2, "<postfix-expression> ::= <postfix-expression> literal-string-plus-plus");
        public static final Production POSTFIX_EXPRESSION_298 = new Production(Nonterminal.POSTFIX_EXPRESSION, 2, "<postfix-expression> ::= <postfix-expression> literal-string-hyphen-hyphen");
        public static final Production ARGUMENT_EXPRESSION_LIST_299 = new Production(Nonterminal.ARGUMENT_EXPRESSION_LIST, 1, "<argument-expression-list> ::= <assignment-expression>");
        public static final Production ARGUMENT_EXPRESSION_LIST_300 = new Production(Nonterminal.ARGUMENT_EXPRESSION_LIST, 3, "<argument-expression-list> ::= <argument-expression-list> literal-string-comma <assignment-expression>");
        public static final Production UNARY_EXPRESSION_301 = new Production(Nonterminal.UNARY_EXPRESSION, 1, "<unary-expression> ::= <postfix-expression>");
        public static final Production UNARY_EXPRESSION_302 = new Production(Nonterminal.UNARY_EXPRESSION, 2, "<unary-expression> ::= literal-string-plus-plus <unary-expression>");
        public static final Production UNARY_EXPRESSION_303 = new Production(Nonterminal.UNARY_EXPRESSION, 2, "<unary-expression> ::= literal-string-hyphen-hyphen <unary-expression>");
        public static final Production UNARY_EXPRESSION_304 = new Production(Nonterminal.UNARY_EXPRESSION, 2, "<unary-expression> ::= <unary-operator> <cast-expression>");
        public static final Production UNARY_EXPRESSION_305 = new Production(Nonterminal.UNARY_EXPRESSION, 2, "<unary-expression> ::= literal-string-sizeof <unary-expression>");
        public static final Production UNARY_OPERATOR_306 = new Production(Nonterminal.UNARY_OPERATOR, 1, "<unary-operator> ::= literal-string-ampersand");
        public static final Production UNARY_OPERATOR_307 = new Production(Nonterminal.UNARY_OPERATOR, 1, "<unary-operator> ::= literal-string-asterisk");
        public static final Production UNARY_OPERATOR_308 = new Production(Nonterminal.UNARY_OPERATOR, 1, "<unary-operator> ::= literal-string-plus");
        public static final Production UNARY_OPERATOR_309 = new Production(Nonterminal.UNARY_OPERATOR, 1, "<unary-operator> ::= literal-string-hyphen");
        public static final Production UNARY_OPERATOR_310 = new Production(Nonterminal.UNARY_OPERATOR, 1, "<unary-operator> ::= literal-string-tilde");
        public static final Production UNARY_OPERATOR_311 = new Production(Nonterminal.UNARY_OPERATOR, 1, "<unary-operator> ::= literal-string-exclamation");
        public static final Production CAST_EXPRESSION_312 = new Production(Nonterminal.CAST_EXPRESSION, 1, "<cast-expression> ::= <unary-expression>");
        public static final Production MULTIPLICATIVE_EXPRESSION_313 = new Production(Nonterminal.MULTIPLICATIVE_EXPRESSION, 1, "<multiplicative-expression> ::= <cast-expression>");
        public static final Production MULTIPLICATIVE_EXPRESSION_314 = new Production(Nonterminal.MULTIPLICATIVE_EXPRESSION, 3, "<multiplicative-expression> ::= <multiplicative-expression> literal-string-asterisk <cast-expression>");
        public static final Production MULTIPLICATIVE_EXPRESSION_315 = new Production(Nonterminal.MULTIPLICATIVE_EXPRESSION, 3, "<multiplicative-expression> ::= <multiplicative-expression> literal-string-slash <cast-expression>");
        public static final Production MULTIPLICATIVE_EXPRESSION_316 = new Production(Nonterminal.MULTIPLICATIVE_EXPRESSION, 3, "<multiplicative-expression> ::= <multiplicative-expression> literal-string-percent <cast-expression>");
        public static final Production ADDITIVE_EXPRESSION_317 = new Production(Nonterminal.ADDITIVE_EXPRESSION, 1, "<additive-expression> ::= <multiplicative-expression>");
        public static final Production ADDITIVE_EXPRESSION_318 = new Production(Nonterminal.ADDITIVE_EXPRESSION, 3, "<additive-expression> ::= <additive-expression> literal-string-plus <multiplicative-expression>");
        public static final Production ADDITIVE_EXPRESSION_319 = new Production(Nonterminal.ADDITIVE_EXPRESSION, 3, "<additive-expression> ::= <additive-expression> literal-string-hyphen <multiplicative-expression>");
        public static final Production SHIFT_EXPRESSION_320 = new Production(Nonterminal.SHIFT_EXPRESSION, 1, "<shift-expression> ::= <additive-expression>");
        public static final Production SHIFT_EXPRESSION_321 = new Production(Nonterminal.SHIFT_EXPRESSION, 3, "<shift-expression> ::= <shift-expression> literal-string-lessthan-lessthan <additive-expression>");
        public static final Production SHIFT_EXPRESSION_322 = new Production(Nonterminal.SHIFT_EXPRESSION, 3, "<shift-expression> ::= <shift-expression> literal-string-greaterthan-greaterthan <additive-expression>");
        public static final Production RELATIONAL_EXPRESSION_323 = new Production(Nonterminal.RELATIONAL_EXPRESSION, 1, "<relational-expression> ::= <shift-expression>");
        public static final Production RELATIONAL_EXPRESSION_324 = new Production(Nonterminal.RELATIONAL_EXPRESSION, 3, "<relational-expression> ::= <relational-expression> literal-string-lessthan <shift-expression>");
        public static final Production RELATIONAL_EXPRESSION_325 = new Production(Nonterminal.RELATIONAL_EXPRESSION, 3, "<relational-expression> ::= <relational-expression> literal-string-greaterthan <shift-expression>");
        public static final Production RELATIONAL_EXPRESSION_326 = new Production(Nonterminal.RELATIONAL_EXPRESSION, 3, "<relational-expression> ::= <relational-expression> literal-string-lessthan-equals <shift-expression>");
        public static final Production RELATIONAL_EXPRESSION_327 = new Production(Nonterminal.RELATIONAL_EXPRESSION, 3, "<relational-expression> ::= <relational-expression> literal-string-greaterthan-equals <shift-expression>");
        public static final Production EQUALITY_EXPRESSION_328 = new Production(Nonterminal.EQUALITY_EXPRESSION, 1, "<equality-expression> ::= <relational-expression>");
        public static final Production EQUALITY_EXPRESSION_329 = new Production(Nonterminal.EQUALITY_EXPRESSION, 3, "<equality-expression> ::= <equality-expression> literal-string-equals-equals <relational-expression>");
        public static final Production EQUALITY_EXPRESSION_330 = new Production(Nonterminal.EQUALITY_EXPRESSION, 3, "<equality-expression> ::= <equality-expression> literal-string-exclamation-equals <relational-expression>");
        public static final Production AND_EXPRESSION_331 = new Production(Nonterminal.AND_EXPRESSION, 1, "<and-expression> ::= <equality-expression>");
        public static final Production AND_EXPRESSION_332 = new Production(Nonterminal.AND_EXPRESSION, 3, "<and-expression> ::= <and-expression> literal-string-ampersand <equality-expression>");
        public static final Production EXCLUSIVE_OR_EXPRESSION_333 = new Production(Nonterminal.EXCLUSIVE_OR_EXPRESSION, 1, "<exclusive-or-expression> ::= <and-expression>");
        public static final Production EXCLUSIVE_OR_EXPRESSION_334 = new Production(Nonterminal.EXCLUSIVE_OR_EXPRESSION, 3, "<exclusive-or-expression> ::= <exclusive-or-expression> literal-string-caret <and-expression>");
        public static final Production INCLUSIVE_OR_EXPRESSION_335 = new Production(Nonterminal.INCLUSIVE_OR_EXPRESSION, 1, "<inclusive-or-expression> ::= <exclusive-or-expression>");
        public static final Production INCLUSIVE_OR_EXPRESSION_336 = new Production(Nonterminal.INCLUSIVE_OR_EXPRESSION, 3, "<inclusive-or-expression> ::= <inclusive-or-expression> literal-string-vbar <exclusive-or-expression>");
        public static final Production LOGICAL_AND_EXPRESSION_337 = new Production(Nonterminal.LOGICAL_AND_EXPRESSION, 1, "<logical-and-expression> ::= <inclusive-or-expression>");
        public static final Production LOGICAL_AND_EXPRESSION_338 = new Production(Nonterminal.LOGICAL_AND_EXPRESSION, 3, "<logical-and-expression> ::= <logical-and-expression> literal-string-ampersand-ampersand <inclusive-or-expression>");
        public static final Production LOGICAL_OR_EXPRESSION_339 = new Production(Nonterminal.LOGICAL_OR_EXPRESSION, 1, "<logical-or-expression> ::= <logical-and-expression>");
        public static final Production LOGICAL_OR_EXPRESSION_340 = new Production(Nonterminal.LOGICAL_OR_EXPRESSION, 3, "<logical-or-expression> ::= <logical-or-expression> literal-string-vbar-vbar <logical-and-expression>");
        public static final Production CONDITIONAL_EXPRESSION_341 = new Production(Nonterminal.CONDITIONAL_EXPRESSION, 1, "<conditional-expression> ::= <logical-or-expression>");
        public static final Production CONDITIONAL_EXPRESSION_342 = new Production(Nonterminal.CONDITIONAL_EXPRESSION, 5, "<conditional-expression> ::= <logical-or-expression> literal-string-question <expression> literal-string-colon <conditional-expression>");
        public static final Production CONSTANT_343 = new Production(Nonterminal.CONSTANT, 1, "<constant> ::= INTEGER-CONSTANT");
        public static final Production CONSTANT_344 = new Production(Nonterminal.CONSTANT, 1, "<constant> ::= FLOATING-CONSTANT");
        public static final Production CONSTANT_345 = new Production(Nonterminal.CONSTANT, 1, "<constant> ::= CHARACTER-CONSTANT");
        public static final Production EXPRESSION_346 = new Production(Nonterminal.EXPRESSION, 1, "<expression> ::= <conditional-expression>");
        public static final Production ASSIGNMENT_EXPRESSION_347 = new Production(Nonterminal.ASSIGNMENT_EXPRESSION, 1, "<assignment-expression> ::= <conditional-expression>");
        public static final Production CONSTANT_EXPRESSION_348 = new Production(Nonterminal.CONSTANT_EXPRESSION, 1, "<constant-expression> ::= <conditional-expression>");
        public static final Production IDENTIFIER_LIST_349 = new Production(Nonterminal.IDENTIFIER_LIST, 1, "<identifier-list> ::= <identifier>");
        public static final Production IDENTIFIER_LIST_350 = new Production(Nonterminal.IDENTIFIER_LIST, 3, "<identifier-list> ::= <identifier-list> literal-string-comma <identifier>");
        public static final Production IDENTIFIER_351 = new Production(Nonterminal.IDENTIFIER, 1, "<identifier> ::= IDENTIFIER");
        public static final Production IDENTIFIER_352 = new Production(Nonterminal.IDENTIFIER, 1, "<identifier> ::= literal-string-data");
        public static final Production STRING_LITERAL_TERMINAL_LIST_353 = new Production(Nonterminal.STRING_LITERAL_TERMINAL_LIST, 2, "<STRING-LITERAL-terminal-list> ::= <STRING-LITERAL-terminal-list> STRING-LITERAL");
        public static final Production STRING_LITERAL_TERMINAL_LIST_354 = new Production(Nonterminal.STRING_LITERAL_TERMINAL_LIST, 1, "<STRING-LITERAL-terminal-list> ::= STRING-LITERAL");

        protected static final int ACC_CONSTRUCT_1_INDEX = 1;
        protected static final int ACC_CONSTRUCT_2_INDEX = 2;
        protected static final int ACC_CONSTRUCT_3_INDEX = 3;
        protected static final int ACC_CONSTRUCT_4_INDEX = 4;
        protected static final int ACC_CONSTRUCT_5_INDEX = 5;
        protected static final int ACC_CONSTRUCT_6_INDEX = 6;
        protected static final int ACC_CONSTRUCT_7_INDEX = 7;
        protected static final int ACC_CONSTRUCT_8_INDEX = 8;
        protected static final int ACC_CONSTRUCT_9_INDEX = 9;
        protected static final int ACC_CONSTRUCT_10_INDEX = 10;
        protected static final int ACC_CONSTRUCT_11_INDEX = 11;
        protected static final int ACC_CONSTRUCT_12_INDEX = 12;
        protected static final int ACC_CONSTRUCT_13_INDEX = 13;
        protected static final int ACC_CONSTRUCT_14_INDEX = 14;
        protected static final int ACC_CONSTRUCT_15_INDEX = 15;
        protected static final int ACC_CONSTRUCT_16_INDEX = 16;
        protected static final int ACC_LOOP_17_INDEX = 17;
        protected static final int ACC_LOOP_18_INDEX = 18;
        protected static final int ACC_LOOP_CLAUSE_LIST_19_INDEX = 19;
        protected static final int ACC_LOOP_CLAUSE_LIST_20_INDEX = 20;
        protected static final int ACC_LOOP_CLAUSE_LIST_21_INDEX = 21;
        protected static final int ACC_LOOP_CLAUSE_22_INDEX = 22;
        protected static final int ACC_LOOP_CLAUSE_23_INDEX = 23;
        protected static final int ACC_LOOP_CLAUSE_24_INDEX = 24;
        protected static final int ACC_LOOP_CLAUSE_25_INDEX = 25;
        protected static final int ACC_LOOP_CLAUSE_26_INDEX = 26;
        protected static final int ACC_LOOP_CLAUSE_27_INDEX = 27;
        protected static final int ACC_LOOP_CLAUSE_28_INDEX = 28;
        protected static final int ACC_LOOP_CLAUSE_29_INDEX = 29;
        protected static final int ACC_LOOP_CLAUSE_30_INDEX = 30;
        protected static final int ACC_LOOP_CLAUSE_31_INDEX = 31;
        protected static final int ACC_PARALLEL_32_INDEX = 32;
        protected static final int ACC_PARALLEL_33_INDEX = 33;
        protected static final int ACC_PARALLEL_CLAUSE_LIST_34_INDEX = 34;
        protected static final int ACC_PARALLEL_CLAUSE_LIST_35_INDEX = 35;
        protected static final int ACC_PARALLEL_CLAUSE_LIST_36_INDEX = 36;
        protected static final int ACC_PARALLEL_CLAUSE_37_INDEX = 37;
        protected static final int ACC_PARALLEL_CLAUSE_38_INDEX = 38;
        protected static final int ACC_PARALLEL_CLAUSE_39_INDEX = 39;
        protected static final int ACC_PARALLEL_CLAUSE_40_INDEX = 40;
        protected static final int ACC_PARALLEL_CLAUSE_41_INDEX = 41;
        protected static final int ACC_PARALLEL_CLAUSE_42_INDEX = 42;
        protected static final int ACC_PARALLEL_CLAUSE_43_INDEX = 43;
        protected static final int ACC_PARALLEL_CLAUSE_44_INDEX = 44;
        protected static final int ACC_PARALLEL_CLAUSE_45_INDEX = 45;
        protected static final int ACC_PARALLEL_CLAUSE_46_INDEX = 46;
        protected static final int ACC_PARALLEL_CLAUSE_47_INDEX = 47;
        protected static final int ACC_PARALLEL_CLAUSE_48_INDEX = 48;
        protected static final int ACC_PARALLEL_CLAUSE_49_INDEX = 49;
        protected static final int ACC_PARALLEL_CLAUSE_50_INDEX = 50;
        protected static final int ACC_PARALLEL_CLAUSE_51_INDEX = 51;
        protected static final int ACC_PARALLEL_CLAUSE_52_INDEX = 52;
        protected static final int ACC_PARALLEL_CLAUSE_53_INDEX = 53;
        protected static final int ACC_PARALLEL_CLAUSE_54_INDEX = 54;
        protected static final int ACC_PARALLEL_CLAUSE_55_INDEX = 55;
        protected static final int ACC_PARALLEL_CLAUSE_56_INDEX = 56;
        protected static final int ACC_PARALLEL_LOOP_57_INDEX = 57;
        protected static final int ACC_PARALLEL_LOOP_58_INDEX = 58;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_LIST_59_INDEX = 59;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_LIST_60_INDEX = 60;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_LIST_61_INDEX = 61;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_62_INDEX = 62;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_63_INDEX = 63;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_64_INDEX = 64;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_65_INDEX = 65;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_66_INDEX = 66;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_67_INDEX = 67;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_68_INDEX = 68;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_69_INDEX = 69;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_70_INDEX = 70;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_71_INDEX = 71;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_72_INDEX = 72;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_73_INDEX = 73;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_74_INDEX = 74;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_75_INDEX = 75;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_76_INDEX = 76;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_77_INDEX = 77;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_78_INDEX = 78;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_79_INDEX = 79;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_80_INDEX = 80;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_81_INDEX = 81;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_82_INDEX = 82;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_83_INDEX = 83;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_84_INDEX = 84;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_85_INDEX = 85;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_86_INDEX = 86;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_87_INDEX = 87;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_88_INDEX = 88;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_89_INDEX = 89;
        protected static final int ACC_KERNELS_90_INDEX = 90;
        protected static final int ACC_KERNELS_91_INDEX = 91;
        protected static final int ACC_KERNELS_CLAUSE_LIST_92_INDEX = 92;
        protected static final int ACC_KERNELS_CLAUSE_LIST_93_INDEX = 93;
        protected static final int ACC_KERNELS_CLAUSE_LIST_94_INDEX = 94;
        protected static final int ACC_KERNELS_CLAUSE_95_INDEX = 95;
        protected static final int ACC_KERNELS_CLAUSE_96_INDEX = 96;
        protected static final int ACC_KERNELS_CLAUSE_97_INDEX = 97;
        protected static final int ACC_KERNELS_CLAUSE_98_INDEX = 98;
        protected static final int ACC_KERNELS_CLAUSE_99_INDEX = 99;
        protected static final int ACC_KERNELS_CLAUSE_100_INDEX = 100;
        protected static final int ACC_KERNELS_CLAUSE_101_INDEX = 101;
        protected static final int ACC_KERNELS_CLAUSE_102_INDEX = 102;
        protected static final int ACC_KERNELS_CLAUSE_103_INDEX = 103;
        protected static final int ACC_KERNELS_CLAUSE_104_INDEX = 104;
        protected static final int ACC_KERNELS_CLAUSE_105_INDEX = 105;
        protected static final int ACC_KERNELS_CLAUSE_106_INDEX = 106;
        protected static final int ACC_KERNELS_CLAUSE_107_INDEX = 107;
        protected static final int ACC_KERNELS_CLAUSE_108_INDEX = 108;
        protected static final int ACC_KERNELS_LOOP_109_INDEX = 109;
        protected static final int ACC_KERNELS_LOOP_110_INDEX = 110;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_LIST_111_INDEX = 111;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_LIST_112_INDEX = 112;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_LIST_113_INDEX = 113;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_114_INDEX = 114;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_115_INDEX = 115;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_116_INDEX = 116;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_117_INDEX = 117;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_118_INDEX = 118;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_119_INDEX = 119;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_120_INDEX = 120;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_121_INDEX = 121;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_122_INDEX = 122;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_123_INDEX = 123;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_124_INDEX = 124;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_125_INDEX = 125;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_126_INDEX = 126;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_127_INDEX = 127;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_128_INDEX = 128;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_129_INDEX = 129;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_130_INDEX = 130;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_131_INDEX = 131;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_132_INDEX = 132;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_133_INDEX = 133;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_134_INDEX = 134;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_135_INDEX = 135;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_136_INDEX = 136;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_137_INDEX = 137;
        protected static final int ACC_DECLARE_138_INDEX = 138;
        protected static final int ACC_DECLARE_CLAUSE_LIST_139_INDEX = 139;
        protected static final int ACC_DECLARE_CLAUSE_LIST_140_INDEX = 140;
        protected static final int ACC_DECLARE_CLAUSE_LIST_141_INDEX = 141;
        protected static final int ACC_DECLARE_CLAUSE_142_INDEX = 142;
        protected static final int ACC_DECLARE_CLAUSE_143_INDEX = 143;
        protected static final int ACC_DECLARE_CLAUSE_144_INDEX = 144;
        protected static final int ACC_DECLARE_CLAUSE_145_INDEX = 145;
        protected static final int ACC_DECLARE_CLAUSE_146_INDEX = 146;
        protected static final int ACC_DECLARE_CLAUSE_147_INDEX = 147;
        protected static final int ACC_DECLARE_CLAUSE_148_INDEX = 148;
        protected static final int ACC_DECLARE_CLAUSE_149_INDEX = 149;
        protected static final int ACC_DECLARE_CLAUSE_150_INDEX = 150;
        protected static final int ACC_DECLARE_CLAUSE_151_INDEX = 151;
        protected static final int ACC_DECLARE_CLAUSE_152_INDEX = 152;
        protected static final int ACC_DECLARE_CLAUSE_153_INDEX = 153;
        protected static final int ACC_DATA_154_INDEX = 154;
        protected static final int ACC_DATA_CLAUSE_LIST_155_INDEX = 155;
        protected static final int ACC_DATA_CLAUSE_LIST_156_INDEX = 156;
        protected static final int ACC_DATA_CLAUSE_LIST_157_INDEX = 157;
        protected static final int ACC_DATA_CLAUSE_158_INDEX = 158;
        protected static final int ACC_DATA_CLAUSE_159_INDEX = 159;
        protected static final int ACC_DATA_CLAUSE_160_INDEX = 160;
        protected static final int ACC_DATA_CLAUSE_161_INDEX = 161;
        protected static final int ACC_DATA_CLAUSE_162_INDEX = 162;
        protected static final int ACC_DATA_CLAUSE_163_INDEX = 163;
        protected static final int ACC_DATA_CLAUSE_164_INDEX = 164;
        protected static final int ACC_DATA_CLAUSE_165_INDEX = 165;
        protected static final int ACC_DATA_CLAUSE_166_INDEX = 166;
        protected static final int ACC_DATA_CLAUSE_167_INDEX = 167;
        protected static final int ACC_DATA_CLAUSE_168_INDEX = 168;
        protected static final int ACC_HOSTDATA_169_INDEX = 169;
        protected static final int ACC_HOSTDATA_CLAUSE_LIST_170_INDEX = 170;
        protected static final int ACC_HOSTDATA_CLAUSE_LIST_171_INDEX = 171;
        protected static final int ACC_HOSTDATA_CLAUSE_LIST_172_INDEX = 172;
        protected static final int ACC_HOSTDATA_CLAUSE_173_INDEX = 173;
        protected static final int ACC_CACHE_174_INDEX = 174;
        protected static final int ACC_WAIT_175_INDEX = 175;
        protected static final int ACC_WAIT_176_INDEX = 176;
        protected static final int ACC_WAIT_177_INDEX = 177;
        protected static final int ACC_WAIT_178_INDEX = 178;
        protected static final int ACC_WAIT_PARAMETER_179_INDEX = 179;
        protected static final int ACC_WAIT_CLAUSE_LIST_180_INDEX = 180;
        protected static final int ACC_WAIT_CLAUSE_LIST_181_INDEX = 181;
        protected static final int ACC_WAIT_CLAUSE_LIST_182_INDEX = 182;
        protected static final int ACC_UPDATE_183_INDEX = 183;
        protected static final int ACC_UPDATE_CLAUSE_LIST_184_INDEX = 184;
        protected static final int ACC_UPDATE_CLAUSE_LIST_185_INDEX = 185;
        protected static final int ACC_UPDATE_CLAUSE_LIST_186_INDEX = 186;
        protected static final int ACC_UPDATE_CLAUSE_187_INDEX = 187;
        protected static final int ACC_UPDATE_CLAUSE_188_INDEX = 188;
        protected static final int ACC_UPDATE_CLAUSE_189_INDEX = 189;
        protected static final int ACC_UPDATE_CLAUSE_190_INDEX = 190;
        protected static final int ACC_UPDATE_CLAUSE_191_INDEX = 191;
        protected static final int ACC_UPDATE_CLAUSE_192_INDEX = 192;
        protected static final int ACC_ENTER_DATA_193_INDEX = 193;
        protected static final int ACC_ENTER_DATA_CLAUSE_LIST_194_INDEX = 194;
        protected static final int ACC_ENTER_DATA_CLAUSE_LIST_195_INDEX = 195;
        protected static final int ACC_ENTER_DATA_CLAUSE_LIST_196_INDEX = 196;
        protected static final int ACC_ENTER_DATA_CLAUSE_197_INDEX = 197;
        protected static final int ACC_ENTER_DATA_CLAUSE_198_INDEX = 198;
        protected static final int ACC_ENTER_DATA_CLAUSE_199_INDEX = 199;
        protected static final int ACC_ENTER_DATA_CLAUSE_200_INDEX = 200;
        protected static final int ACC_ENTER_DATA_CLAUSE_201_INDEX = 201;
        protected static final int ACC_ENTER_DATA_CLAUSE_202_INDEX = 202;
        protected static final int ACC_ENTER_DATA_CLAUSE_203_INDEX = 203;
        protected static final int ACC_ENTER_DATA_CLAUSE_204_INDEX = 204;
        protected static final int ACC_EXIT_DATA_205_INDEX = 205;
        protected static final int ACC_EXIT_DATA_CLAUSE_LIST_206_INDEX = 206;
        protected static final int ACC_EXIT_DATA_CLAUSE_LIST_207_INDEX = 207;
        protected static final int ACC_EXIT_DATA_CLAUSE_LIST_208_INDEX = 208;
        protected static final int ACC_EXIT_DATA_CLAUSE_209_INDEX = 209;
        protected static final int ACC_EXIT_DATA_CLAUSE_210_INDEX = 210;
        protected static final int ACC_EXIT_DATA_CLAUSE_211_INDEX = 211;
        protected static final int ACC_EXIT_DATA_CLAUSE_212_INDEX = 212;
        protected static final int ACC_EXIT_DATA_CLAUSE_213_INDEX = 213;
        protected static final int ACC_EXIT_DATA_CLAUSE_214_INDEX = 214;
        protected static final int ACC_ROUTINE_215_INDEX = 215;
        protected static final int ACC_ROUTINE_216_INDEX = 216;
        protected static final int ACC_ROUTINE_CLAUSE_LIST_217_INDEX = 217;
        protected static final int ACC_ROUTINE_CLAUSE_LIST_218_INDEX = 218;
        protected static final int ACC_ROUTINE_CLAUSE_LIST_219_INDEX = 219;
        protected static final int ACC_ROUTINE_CLAUSE_220_INDEX = 220;
        protected static final int ACC_ROUTINE_CLAUSE_221_INDEX = 221;
        protected static final int ACC_ROUTINE_CLAUSE_222_INDEX = 222;
        protected static final int ACC_ROUTINE_CLAUSE_223_INDEX = 223;
        protected static final int ACC_ROUTINE_CLAUSE_224_INDEX = 224;
        protected static final int ACC_ROUTINE_CLAUSE_225_INDEX = 225;
        protected static final int ACC_ROUTINE_CLAUSE_226_INDEX = 226;
        protected static final int ACC_ATOMIC_227_INDEX = 227;
        protected static final int ACC_ATOMIC_228_INDEX = 228;
        protected static final int ACC_ATOMIC_CLAUSE_229_INDEX = 229;
        protected static final int ACC_ATOMIC_CLAUSE_230_INDEX = 230;
        protected static final int ACC_ATOMIC_CLAUSE_231_INDEX = 231;
        protected static final int ACC_ATOMIC_CLAUSE_232_INDEX = 232;
        protected static final int ACC_COLLAPSE_CLAUSE_233_INDEX = 233;
        protected static final int ACC_GANG_CLAUSE_234_INDEX = 234;
        protected static final int ACC_GANG_CLAUSE_235_INDEX = 235;
        protected static final int ACC_WORKER_CLAUSE_236_INDEX = 236;
        protected static final int ACC_WORKER_CLAUSE_237_INDEX = 237;
        protected static final int ACC_VECTOR_CLAUSE_238_INDEX = 238;
        protected static final int ACC_VECTOR_CLAUSE_239_INDEX = 239;
        protected static final int ACC_PRIVATE_CLAUSE_240_INDEX = 240;
        protected static final int ACC_FIRSTPRIVATE_CLAUSE_241_INDEX = 241;
        protected static final int ACC_REDUCTION_CLAUSE_242_INDEX = 242;
        protected static final int ACC_IF_CLAUSE_243_INDEX = 243;
        protected static final int ACC_ASYNC_CLAUSE_244_INDEX = 244;
        protected static final int ACC_ASYNC_CLAUSE_245_INDEX = 245;
        protected static final int ACC_WAIT_CLAUSE_246_INDEX = 246;
        protected static final int ACC_NUMGANGS_CLAUSE_247_INDEX = 247;
        protected static final int ACC_NUMWORKERS_CLAUSE_248_INDEX = 248;
        protected static final int ACC_VECTORLENGTH_CLAUSE_249_INDEX = 249;
        protected static final int ACC_DELETE_CLAUSE_250_INDEX = 250;
        protected static final int ACC_COPY_CLAUSE_251_INDEX = 251;
        protected static final int ACC_COPYIN_CLAUSE_252_INDEX = 252;
        protected static final int ACC_COPYOUT_CLAUSE_253_INDEX = 253;
        protected static final int ACC_CREATE_CLAUSE_254_INDEX = 254;
        protected static final int ACC_PRESENT_CLAUSE_255_INDEX = 255;
        protected static final int ACC_PRESENTORCOPY_CLAUSE_256_INDEX = 256;
        protected static final int ACC_PRESENTORCOPY_CLAUSE_257_INDEX = 257;
        protected static final int ACC_PRESENTORCOPYIN_CLAUSE_258_INDEX = 258;
        protected static final int ACC_PRESENTORCOPYIN_CLAUSE_259_INDEX = 259;
        protected static final int ACC_PRESENTORCOPYOUT_CLAUSE_260_INDEX = 260;
        protected static final int ACC_PRESENTORCOPYOUT_CLAUSE_261_INDEX = 261;
        protected static final int ACC_PRESENTORCREATE_CLAUSE_262_INDEX = 262;
        protected static final int ACC_PRESENTORCREATE_CLAUSE_263_INDEX = 263;
        protected static final int ACC_DEVICEPTR_CLAUSE_264_INDEX = 264;
        protected static final int ACC_DEVICERESIDENT_CLAUSE_265_INDEX = 265;
        protected static final int ACC_USEDEVICE_CLAUSE_266_INDEX = 266;
        protected static final int ACC_SELF_CLAUSE_267_INDEX = 267;
        protected static final int ACC_HOST_CLAUSE_268_INDEX = 268;
        protected static final int ACC_DEVICE_CLAUSE_269_INDEX = 269;
        protected static final int ACC_DEFAULTNONE_CLAUSE_270_INDEX = 270;
        protected static final int ACC_LINK_CLAUSE_271_INDEX = 271;
        protected static final int ACC_TILE_CLAUSE_272_INDEX = 272;
        protected static final int ACC_COUNT_273_INDEX = 273;
        protected static final int ACC_REDUCTION_OPERATOR_274_INDEX = 274;
        protected static final int ACC_REDUCTION_OPERATOR_275_INDEX = 275;
        protected static final int ACC_REDUCTION_OPERATOR_276_INDEX = 276;
        protected static final int ACC_REDUCTION_OPERATOR_277_INDEX = 277;
        protected static final int ACC_REDUCTION_OPERATOR_278_INDEX = 278;
        protected static final int ACC_REDUCTION_OPERATOR_279_INDEX = 279;
        protected static final int ACC_REDUCTION_OPERATOR_280_INDEX = 280;
        protected static final int ACC_REDUCTION_OPERATOR_281_INDEX = 281;
        protected static final int ACC_REDUCTION_OPERATOR_282_INDEX = 282;
        protected static final int ACC_DATA_LIST_283_INDEX = 283;
        protected static final int ACC_DATA_LIST_284_INDEX = 284;
        protected static final int ACC_DATA_ITEM_285_INDEX = 285;
        protected static final int ACC_DATA_ITEM_286_INDEX = 286;
        protected static final int PRIMARY_EXPRESSION_287_INDEX = 287;
        protected static final int PRIMARY_EXPRESSION_288_INDEX = 288;
        protected static final int PRIMARY_EXPRESSION_289_INDEX = 289;
        protected static final int PRIMARY_EXPRESSION_290_INDEX = 290;
        protected static final int POSTFIX_EXPRESSION_291_INDEX = 291;
        protected static final int POSTFIX_EXPRESSION_292_INDEX = 292;
        protected static final int POSTFIX_EXPRESSION_293_INDEX = 293;
        protected static final int POSTFIX_EXPRESSION_294_INDEX = 294;
        protected static final int POSTFIX_EXPRESSION_295_INDEX = 295;
        protected static final int POSTFIX_EXPRESSION_296_INDEX = 296;
        protected static final int POSTFIX_EXPRESSION_297_INDEX = 297;
        protected static final int POSTFIX_EXPRESSION_298_INDEX = 298;
        protected static final int ARGUMENT_EXPRESSION_LIST_299_INDEX = 299;
        protected static final int ARGUMENT_EXPRESSION_LIST_300_INDEX = 300;
        protected static final int UNARY_EXPRESSION_301_INDEX = 301;
        protected static final int UNARY_EXPRESSION_302_INDEX = 302;
        protected static final int UNARY_EXPRESSION_303_INDEX = 303;
        protected static final int UNARY_EXPRESSION_304_INDEX = 304;
        protected static final int UNARY_EXPRESSION_305_INDEX = 305;
        protected static final int UNARY_OPERATOR_306_INDEX = 306;
        protected static final int UNARY_OPERATOR_307_INDEX = 307;
        protected static final int UNARY_OPERATOR_308_INDEX = 308;
        protected static final int UNARY_OPERATOR_309_INDEX = 309;
        protected static final int UNARY_OPERATOR_310_INDEX = 310;
        protected static final int UNARY_OPERATOR_311_INDEX = 311;
        protected static final int CAST_EXPRESSION_312_INDEX = 312;
        protected static final int MULTIPLICATIVE_EXPRESSION_313_INDEX = 313;
        protected static final int MULTIPLICATIVE_EXPRESSION_314_INDEX = 314;
        protected static final int MULTIPLICATIVE_EXPRESSION_315_INDEX = 315;
        protected static final int MULTIPLICATIVE_EXPRESSION_316_INDEX = 316;
        protected static final int ADDITIVE_EXPRESSION_317_INDEX = 317;
        protected static final int ADDITIVE_EXPRESSION_318_INDEX = 318;
        protected static final int ADDITIVE_EXPRESSION_319_INDEX = 319;
        protected static final int SHIFT_EXPRESSION_320_INDEX = 320;
        protected static final int SHIFT_EXPRESSION_321_INDEX = 321;
        protected static final int SHIFT_EXPRESSION_322_INDEX = 322;
        protected static final int RELATIONAL_EXPRESSION_323_INDEX = 323;
        protected static final int RELATIONAL_EXPRESSION_324_INDEX = 324;
        protected static final int RELATIONAL_EXPRESSION_325_INDEX = 325;
        protected static final int RELATIONAL_EXPRESSION_326_INDEX = 326;
        protected static final int RELATIONAL_EXPRESSION_327_INDEX = 327;
        protected static final int EQUALITY_EXPRESSION_328_INDEX = 328;
        protected static final int EQUALITY_EXPRESSION_329_INDEX = 329;
        protected static final int EQUALITY_EXPRESSION_330_INDEX = 330;
        protected static final int AND_EXPRESSION_331_INDEX = 331;
        protected static final int AND_EXPRESSION_332_INDEX = 332;
        protected static final int EXCLUSIVE_OR_EXPRESSION_333_INDEX = 333;
        protected static final int EXCLUSIVE_OR_EXPRESSION_334_INDEX = 334;
        protected static final int INCLUSIVE_OR_EXPRESSION_335_INDEX = 335;
        protected static final int INCLUSIVE_OR_EXPRESSION_336_INDEX = 336;
        protected static final int LOGICAL_AND_EXPRESSION_337_INDEX = 337;
        protected static final int LOGICAL_AND_EXPRESSION_338_INDEX = 338;
        protected static final int LOGICAL_OR_EXPRESSION_339_INDEX = 339;
        protected static final int LOGICAL_OR_EXPRESSION_340_INDEX = 340;
        protected static final int CONDITIONAL_EXPRESSION_341_INDEX = 341;
        protected static final int CONDITIONAL_EXPRESSION_342_INDEX = 342;
        protected static final int CONSTANT_343_INDEX = 343;
        protected static final int CONSTANT_344_INDEX = 344;
        protected static final int CONSTANT_345_INDEX = 345;
        protected static final int EXPRESSION_346_INDEX = 346;
        protected static final int ASSIGNMENT_EXPRESSION_347_INDEX = 347;
        protected static final int CONSTANT_EXPRESSION_348_INDEX = 348;
        protected static final int IDENTIFIER_LIST_349_INDEX = 349;
        protected static final int IDENTIFIER_LIST_350_INDEX = 350;
        protected static final int IDENTIFIER_351_INDEX = 351;
        protected static final int IDENTIFIER_352_INDEX = 352;
        protected static final int STRING_LITERAL_TERMINAL_LIST_353_INDEX = 353;
        protected static final int STRING_LITERAL_TERMINAL_LIST_354_INDEX = 354;

        protected static final Production[] values = new Production[]
        {
            null, // Start production for augmented grammar
            ACC_CONSTRUCT_1,
            ACC_CONSTRUCT_2,
            ACC_CONSTRUCT_3,
            ACC_CONSTRUCT_4,
            ACC_CONSTRUCT_5,
            ACC_CONSTRUCT_6,
            ACC_CONSTRUCT_7,
            ACC_CONSTRUCT_8,
            ACC_CONSTRUCT_9,
            ACC_CONSTRUCT_10,
            ACC_CONSTRUCT_11,
            ACC_CONSTRUCT_12,
            ACC_CONSTRUCT_13,
            ACC_CONSTRUCT_14,
            ACC_CONSTRUCT_15,
            ACC_CONSTRUCT_16,
            ACC_LOOP_17,
            ACC_LOOP_18,
            ACC_LOOP_CLAUSE_LIST_19,
            ACC_LOOP_CLAUSE_LIST_20,
            ACC_LOOP_CLAUSE_LIST_21,
            ACC_LOOP_CLAUSE_22,
            ACC_LOOP_CLAUSE_23,
            ACC_LOOP_CLAUSE_24,
            ACC_LOOP_CLAUSE_25,
            ACC_LOOP_CLAUSE_26,
            ACC_LOOP_CLAUSE_27,
            ACC_LOOP_CLAUSE_28,
            ACC_LOOP_CLAUSE_29,
            ACC_LOOP_CLAUSE_30,
            ACC_LOOP_CLAUSE_31,
            ACC_PARALLEL_32,
            ACC_PARALLEL_33,
            ACC_PARALLEL_CLAUSE_LIST_34,
            ACC_PARALLEL_CLAUSE_LIST_35,
            ACC_PARALLEL_CLAUSE_LIST_36,
            ACC_PARALLEL_CLAUSE_37,
            ACC_PARALLEL_CLAUSE_38,
            ACC_PARALLEL_CLAUSE_39,
            ACC_PARALLEL_CLAUSE_40,
            ACC_PARALLEL_CLAUSE_41,
            ACC_PARALLEL_CLAUSE_42,
            ACC_PARALLEL_CLAUSE_43,
            ACC_PARALLEL_CLAUSE_44,
            ACC_PARALLEL_CLAUSE_45,
            ACC_PARALLEL_CLAUSE_46,
            ACC_PARALLEL_CLAUSE_47,
            ACC_PARALLEL_CLAUSE_48,
            ACC_PARALLEL_CLAUSE_49,
            ACC_PARALLEL_CLAUSE_50,
            ACC_PARALLEL_CLAUSE_51,
            ACC_PARALLEL_CLAUSE_52,
            ACC_PARALLEL_CLAUSE_53,
            ACC_PARALLEL_CLAUSE_54,
            ACC_PARALLEL_CLAUSE_55,
            ACC_PARALLEL_CLAUSE_56,
            ACC_PARALLEL_LOOP_57,
            ACC_PARALLEL_LOOP_58,
            ACC_PARALLEL_LOOP_CLAUSE_LIST_59,
            ACC_PARALLEL_LOOP_CLAUSE_LIST_60,
            ACC_PARALLEL_LOOP_CLAUSE_LIST_61,
            ACC_PARALLEL_LOOP_CLAUSE_62,
            ACC_PARALLEL_LOOP_CLAUSE_63,
            ACC_PARALLEL_LOOP_CLAUSE_64,
            ACC_PARALLEL_LOOP_CLAUSE_65,
            ACC_PARALLEL_LOOP_CLAUSE_66,
            ACC_PARALLEL_LOOP_CLAUSE_67,
            ACC_PARALLEL_LOOP_CLAUSE_68,
            ACC_PARALLEL_LOOP_CLAUSE_69,
            ACC_PARALLEL_LOOP_CLAUSE_70,
            ACC_PARALLEL_LOOP_CLAUSE_71,
            ACC_PARALLEL_LOOP_CLAUSE_72,
            ACC_PARALLEL_LOOP_CLAUSE_73,
            ACC_PARALLEL_LOOP_CLAUSE_74,
            ACC_PARALLEL_LOOP_CLAUSE_75,
            ACC_PARALLEL_LOOP_CLAUSE_76,
            ACC_PARALLEL_LOOP_CLAUSE_77,
            ACC_PARALLEL_LOOP_CLAUSE_78,
            ACC_PARALLEL_LOOP_CLAUSE_79,
            ACC_PARALLEL_LOOP_CLAUSE_80,
            ACC_PARALLEL_LOOP_CLAUSE_81,
            ACC_PARALLEL_LOOP_CLAUSE_82,
            ACC_PARALLEL_LOOP_CLAUSE_83,
            ACC_PARALLEL_LOOP_CLAUSE_84,
            ACC_PARALLEL_LOOP_CLAUSE_85,
            ACC_PARALLEL_LOOP_CLAUSE_86,
            ACC_PARALLEL_LOOP_CLAUSE_87,
            ACC_PARALLEL_LOOP_CLAUSE_88,
            ACC_PARALLEL_LOOP_CLAUSE_89,
            ACC_KERNELS_90,
            ACC_KERNELS_91,
            ACC_KERNELS_CLAUSE_LIST_92,
            ACC_KERNELS_CLAUSE_LIST_93,
            ACC_KERNELS_CLAUSE_LIST_94,
            ACC_KERNELS_CLAUSE_95,
            ACC_KERNELS_CLAUSE_96,
            ACC_KERNELS_CLAUSE_97,
            ACC_KERNELS_CLAUSE_98,
            ACC_KERNELS_CLAUSE_99,
            ACC_KERNELS_CLAUSE_100,
            ACC_KERNELS_CLAUSE_101,
            ACC_KERNELS_CLAUSE_102,
            ACC_KERNELS_CLAUSE_103,
            ACC_KERNELS_CLAUSE_104,
            ACC_KERNELS_CLAUSE_105,
            ACC_KERNELS_CLAUSE_106,
            ACC_KERNELS_CLAUSE_107,
            ACC_KERNELS_CLAUSE_108,
            ACC_KERNELS_LOOP_109,
            ACC_KERNELS_LOOP_110,
            ACC_KERNELS_LOOP_CLAUSE_LIST_111,
            ACC_KERNELS_LOOP_CLAUSE_LIST_112,
            ACC_KERNELS_LOOP_CLAUSE_LIST_113,
            ACC_KERNELS_LOOP_CLAUSE_114,
            ACC_KERNELS_LOOP_CLAUSE_115,
            ACC_KERNELS_LOOP_CLAUSE_116,
            ACC_KERNELS_LOOP_CLAUSE_117,
            ACC_KERNELS_LOOP_CLAUSE_118,
            ACC_KERNELS_LOOP_CLAUSE_119,
            ACC_KERNELS_LOOP_CLAUSE_120,
            ACC_KERNELS_LOOP_CLAUSE_121,
            ACC_KERNELS_LOOP_CLAUSE_122,
            ACC_KERNELS_LOOP_CLAUSE_123,
            ACC_KERNELS_LOOP_CLAUSE_124,
            ACC_KERNELS_LOOP_CLAUSE_125,
            ACC_KERNELS_LOOP_CLAUSE_126,
            ACC_KERNELS_LOOP_CLAUSE_127,
            ACC_KERNELS_LOOP_CLAUSE_128,
            ACC_KERNELS_LOOP_CLAUSE_129,
            ACC_KERNELS_LOOP_CLAUSE_130,
            ACC_KERNELS_LOOP_CLAUSE_131,
            ACC_KERNELS_LOOP_CLAUSE_132,
            ACC_KERNELS_LOOP_CLAUSE_133,
            ACC_KERNELS_LOOP_CLAUSE_134,
            ACC_KERNELS_LOOP_CLAUSE_135,
            ACC_KERNELS_LOOP_CLAUSE_136,
            ACC_KERNELS_LOOP_CLAUSE_137,
            ACC_DECLARE_138,
            ACC_DECLARE_CLAUSE_LIST_139,
            ACC_DECLARE_CLAUSE_LIST_140,
            ACC_DECLARE_CLAUSE_LIST_141,
            ACC_DECLARE_CLAUSE_142,
            ACC_DECLARE_CLAUSE_143,
            ACC_DECLARE_CLAUSE_144,
            ACC_DECLARE_CLAUSE_145,
            ACC_DECLARE_CLAUSE_146,
            ACC_DECLARE_CLAUSE_147,
            ACC_DECLARE_CLAUSE_148,
            ACC_DECLARE_CLAUSE_149,
            ACC_DECLARE_CLAUSE_150,
            ACC_DECLARE_CLAUSE_151,
            ACC_DECLARE_CLAUSE_152,
            ACC_DECLARE_CLAUSE_153,
            ACC_DATA_154,
            ACC_DATA_CLAUSE_LIST_155,
            ACC_DATA_CLAUSE_LIST_156,
            ACC_DATA_CLAUSE_LIST_157,
            ACC_DATA_CLAUSE_158,
            ACC_DATA_CLAUSE_159,
            ACC_DATA_CLAUSE_160,
            ACC_DATA_CLAUSE_161,
            ACC_DATA_CLAUSE_162,
            ACC_DATA_CLAUSE_163,
            ACC_DATA_CLAUSE_164,
            ACC_DATA_CLAUSE_165,
            ACC_DATA_CLAUSE_166,
            ACC_DATA_CLAUSE_167,
            ACC_DATA_CLAUSE_168,
            ACC_HOSTDATA_169,
            ACC_HOSTDATA_CLAUSE_LIST_170,
            ACC_HOSTDATA_CLAUSE_LIST_171,
            ACC_HOSTDATA_CLAUSE_LIST_172,
            ACC_HOSTDATA_CLAUSE_173,
            ACC_CACHE_174,
            ACC_WAIT_175,
            ACC_WAIT_176,
            ACC_WAIT_177,
            ACC_WAIT_178,
            ACC_WAIT_PARAMETER_179,
            ACC_WAIT_CLAUSE_LIST_180,
            ACC_WAIT_CLAUSE_LIST_181,
            ACC_WAIT_CLAUSE_LIST_182,
            ACC_UPDATE_183,
            ACC_UPDATE_CLAUSE_LIST_184,
            ACC_UPDATE_CLAUSE_LIST_185,
            ACC_UPDATE_CLAUSE_LIST_186,
            ACC_UPDATE_CLAUSE_187,
            ACC_UPDATE_CLAUSE_188,
            ACC_UPDATE_CLAUSE_189,
            ACC_UPDATE_CLAUSE_190,
            ACC_UPDATE_CLAUSE_191,
            ACC_UPDATE_CLAUSE_192,
            ACC_ENTER_DATA_193,
            ACC_ENTER_DATA_CLAUSE_LIST_194,
            ACC_ENTER_DATA_CLAUSE_LIST_195,
            ACC_ENTER_DATA_CLAUSE_LIST_196,
            ACC_ENTER_DATA_CLAUSE_197,
            ACC_ENTER_DATA_CLAUSE_198,
            ACC_ENTER_DATA_CLAUSE_199,
            ACC_ENTER_DATA_CLAUSE_200,
            ACC_ENTER_DATA_CLAUSE_201,
            ACC_ENTER_DATA_CLAUSE_202,
            ACC_ENTER_DATA_CLAUSE_203,
            ACC_ENTER_DATA_CLAUSE_204,
            ACC_EXIT_DATA_205,
            ACC_EXIT_DATA_CLAUSE_LIST_206,
            ACC_EXIT_DATA_CLAUSE_LIST_207,
            ACC_EXIT_DATA_CLAUSE_LIST_208,
            ACC_EXIT_DATA_CLAUSE_209,
            ACC_EXIT_DATA_CLAUSE_210,
            ACC_EXIT_DATA_CLAUSE_211,
            ACC_EXIT_DATA_CLAUSE_212,
            ACC_EXIT_DATA_CLAUSE_213,
            ACC_EXIT_DATA_CLAUSE_214,
            ACC_ROUTINE_215,
            ACC_ROUTINE_216,
            ACC_ROUTINE_CLAUSE_LIST_217,
            ACC_ROUTINE_CLAUSE_LIST_218,
            ACC_ROUTINE_CLAUSE_LIST_219,
            ACC_ROUTINE_CLAUSE_220,
            ACC_ROUTINE_CLAUSE_221,
            ACC_ROUTINE_CLAUSE_222,
            ACC_ROUTINE_CLAUSE_223,
            ACC_ROUTINE_CLAUSE_224,
            ACC_ROUTINE_CLAUSE_225,
            ACC_ROUTINE_CLAUSE_226,
            ACC_ATOMIC_227,
            ACC_ATOMIC_228,
            ACC_ATOMIC_CLAUSE_229,
            ACC_ATOMIC_CLAUSE_230,
            ACC_ATOMIC_CLAUSE_231,
            ACC_ATOMIC_CLAUSE_232,
            ACC_COLLAPSE_CLAUSE_233,
            ACC_GANG_CLAUSE_234,
            ACC_GANG_CLAUSE_235,
            ACC_WORKER_CLAUSE_236,
            ACC_WORKER_CLAUSE_237,
            ACC_VECTOR_CLAUSE_238,
            ACC_VECTOR_CLAUSE_239,
            ACC_PRIVATE_CLAUSE_240,
            ACC_FIRSTPRIVATE_CLAUSE_241,
            ACC_REDUCTION_CLAUSE_242,
            ACC_IF_CLAUSE_243,
            ACC_ASYNC_CLAUSE_244,
            ACC_ASYNC_CLAUSE_245,
            ACC_WAIT_CLAUSE_246,
            ACC_NUMGANGS_CLAUSE_247,
            ACC_NUMWORKERS_CLAUSE_248,
            ACC_VECTORLENGTH_CLAUSE_249,
            ACC_DELETE_CLAUSE_250,
            ACC_COPY_CLAUSE_251,
            ACC_COPYIN_CLAUSE_252,
            ACC_COPYOUT_CLAUSE_253,
            ACC_CREATE_CLAUSE_254,
            ACC_PRESENT_CLAUSE_255,
            ACC_PRESENTORCOPY_CLAUSE_256,
            ACC_PRESENTORCOPY_CLAUSE_257,
            ACC_PRESENTORCOPYIN_CLAUSE_258,
            ACC_PRESENTORCOPYIN_CLAUSE_259,
            ACC_PRESENTORCOPYOUT_CLAUSE_260,
            ACC_PRESENTORCOPYOUT_CLAUSE_261,
            ACC_PRESENTORCREATE_CLAUSE_262,
            ACC_PRESENTORCREATE_CLAUSE_263,
            ACC_DEVICEPTR_CLAUSE_264,
            ACC_DEVICERESIDENT_CLAUSE_265,
            ACC_USEDEVICE_CLAUSE_266,
            ACC_SELF_CLAUSE_267,
            ACC_HOST_CLAUSE_268,
            ACC_DEVICE_CLAUSE_269,
            ACC_DEFAULTNONE_CLAUSE_270,
            ACC_LINK_CLAUSE_271,
            ACC_TILE_CLAUSE_272,
            ACC_COUNT_273,
            ACC_REDUCTION_OPERATOR_274,
            ACC_REDUCTION_OPERATOR_275,
            ACC_REDUCTION_OPERATOR_276,
            ACC_REDUCTION_OPERATOR_277,
            ACC_REDUCTION_OPERATOR_278,
            ACC_REDUCTION_OPERATOR_279,
            ACC_REDUCTION_OPERATOR_280,
            ACC_REDUCTION_OPERATOR_281,
            ACC_REDUCTION_OPERATOR_282,
            ACC_DATA_LIST_283,
            ACC_DATA_LIST_284,
            ACC_DATA_ITEM_285,
            ACC_DATA_ITEM_286,
            PRIMARY_EXPRESSION_287,
            PRIMARY_EXPRESSION_288,
            PRIMARY_EXPRESSION_289,
            PRIMARY_EXPRESSION_290,
            POSTFIX_EXPRESSION_291,
            POSTFIX_EXPRESSION_292,
            POSTFIX_EXPRESSION_293,
            POSTFIX_EXPRESSION_294,
            POSTFIX_EXPRESSION_295,
            POSTFIX_EXPRESSION_296,
            POSTFIX_EXPRESSION_297,
            POSTFIX_EXPRESSION_298,
            ARGUMENT_EXPRESSION_LIST_299,
            ARGUMENT_EXPRESSION_LIST_300,
            UNARY_EXPRESSION_301,
            UNARY_EXPRESSION_302,
            UNARY_EXPRESSION_303,
            UNARY_EXPRESSION_304,
            UNARY_EXPRESSION_305,
            UNARY_OPERATOR_306,
            UNARY_OPERATOR_307,
            UNARY_OPERATOR_308,
            UNARY_OPERATOR_309,
            UNARY_OPERATOR_310,
            UNARY_OPERATOR_311,
            CAST_EXPRESSION_312,
            MULTIPLICATIVE_EXPRESSION_313,
            MULTIPLICATIVE_EXPRESSION_314,
            MULTIPLICATIVE_EXPRESSION_315,
            MULTIPLICATIVE_EXPRESSION_316,
            ADDITIVE_EXPRESSION_317,
            ADDITIVE_EXPRESSION_318,
            ADDITIVE_EXPRESSION_319,
            SHIFT_EXPRESSION_320,
            SHIFT_EXPRESSION_321,
            SHIFT_EXPRESSION_322,
            RELATIONAL_EXPRESSION_323,
            RELATIONAL_EXPRESSION_324,
            RELATIONAL_EXPRESSION_325,
            RELATIONAL_EXPRESSION_326,
            RELATIONAL_EXPRESSION_327,
            EQUALITY_EXPRESSION_328,
            EQUALITY_EXPRESSION_329,
            EQUALITY_EXPRESSION_330,
            AND_EXPRESSION_331,
            AND_EXPRESSION_332,
            EXCLUSIVE_OR_EXPRESSION_333,
            EXCLUSIVE_OR_EXPRESSION_334,
            INCLUSIVE_OR_EXPRESSION_335,
            INCLUSIVE_OR_EXPRESSION_336,
            LOGICAL_AND_EXPRESSION_337,
            LOGICAL_AND_EXPRESSION_338,
            LOGICAL_OR_EXPRESSION_339,
            LOGICAL_OR_EXPRESSION_340,
            CONDITIONAL_EXPRESSION_341,
            CONDITIONAL_EXPRESSION_342,
            CONSTANT_343,
            CONSTANT_344,
            CONSTANT_345,
            EXPRESSION_346,
            ASSIGNMENT_EXPRESSION_347,
            CONSTANT_EXPRESSION_348,
            IDENTIFIER_LIST_349,
            IDENTIFIER_LIST_350,
            IDENTIFIER_351,
            IDENTIFIER_352,
            STRING_LITERAL_TERMINAL_LIST_353,
            STRING_LITERAL_TERMINAL_LIST_354,
        };
    }

    /**
     * A stack of integers that will grow automatically as necessary.
     * <p>
     * Integers are stored as primitives rather than <code>Integer</code>
     * objects in order to increase efficiency.
     */
    protected static class IntStack
    {
        /** The contents of the stack. */
        protected int[] stack;

        /**
         * The number of elements on the stack.
         * <p>
         * It is always the case that <code>size <= stack.length</code>.
         */
        protected int size;

        /**
         * Constructor.  Creates a stack of integers with a reasonable
         * initial capacity, which will grow as necessary.
         */
        public IntStack()
        {
            this(64); // Heuristic
        }

        /**
         * Constructor.  Creates a stack of integers with the given initial
         * capacity, which will grow as necessary.
         *
         * @param initialCapacity the number of elements the stack should
         *                        initially accommodate before resizing itself
         */
        public IntStack(int initialCapacity)
        {
            if (initialCapacity <= 0)
                throw new IllegalArgumentException("Initial stack capacity " +
                    "must be a positive integer (not " + initialCapacity + ")");

            this.stack = new int[initialCapacity];
            this.size = 0;
        }

        /**
         * Copy construct.  Creates a stack of integers which is a copy of
         * the given <code>IntStack</code>, but which may be modified separately.
         */
        public IntStack(IntStack copyFrom)
        {
            this(copyFrom.stack.length);
            this.size = copyFrom.size;
            System.arraycopy(copyFrom.stack, 0, this.stack, 0, size);
        }

        /**
         * Increases the capacity of the stack, if necessary, to hold at least
         * <code>minCapacity</code> elements.
         * <p>
         * The resizing heuristic is from <code>java.util.ArrayList</code>.
         *
         * @param minCapacity the total number of elements the stack should
         *                    accommodate before resizing itself
         */
        public void ensureCapacity(int minCapacity)
        {
            if (minCapacity <= this.stack.length) return;

            int newCapacity = Math.max((this.stack.length * 3) / 2 + 1, minCapacity);
            int[] newStack = new int[newCapacity];
            System.arraycopy(this.stack, 0, newStack, 0, this.size);
            this.stack = newStack;
        }

        /**
         * Pushes the given value onto the top of the stack.
         *
         * @param value the value to push
         */
        public void push(int value)
        {
            ensureCapacity(this.size + 1);
            this.stack[this.size++] = value;
        }

        /**
         * Returns the value on the top of the stack, but leaves that value
         * on the stack.
         *
         * @return the value on the top of the stack
         *
         * @throws IllegalStateException if the stack is empty
         */
        public int top()
        {
            if (this.size == 0)
                throw new IllegalStateException("Stack is empty");

            return this.stack[this.size - 1];
        }

        /**
         * Removes the value on the top of the stack and returns it.
         *
         * @return the value that has been removed from the stack
         *
         * @throws IllegalStateException if the stack is empty
         */
        public int pop()
        {
            if (this.size == 0)
                throw new IllegalStateException("Stack is empty");

            return this.stack[--this.size];
        }

        /**
         * Returns true if, and only if, the given value exists on the stack
         * (not necessarily on top).
         *
         * @param the value to search for
         *
         * @return true iff the value is on the stack
         */
        public boolean contains(int value)
        {
            for (int i = 0; i < this.size; i++)
                if (this.stack[i] == value)
                    return true;

            return false;
        }

        /**
         * Returns true if, and only if, the stack is empty.
         *
         * @return true if there are no elements on this stack
         */
        public boolean isEmpty()
        {
            return this.size == 0;
        }

        /**
         * Removes all elements from this stack, settings its size to 0.
         */
        public void clear()
        {
            this.size = 0;
        }

        /**
         * Returns the number of elements on this stack.
         *
         * @return the number of elements on this stack (non-negative)
         */
        public int size()
        {
            return this.size;
        }

        /**
         * Returns the value <code>index</code> elements from the bottom
         * of the stack.
         *
         * @return the value at index <code>index</code> the stack
         *
         * @throws IllegalArgumentException if index is out of range
         */
        public int get(int index)
        {
            if (index < 0 || index >= this.size)
                throw new IllegalArgumentException("index out of range");

            return this.stack[index];
        }

        @Override public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < this.size; i++)
            {
                if (i > 0) sb.append(", ");
                sb.append(this.stack[i]);
            }
            sb.append("]");
            return sb.toString();
        }

        @Override
        public int hashCode()
        {
            return 31 * size + Arrays.hashCode(stack);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final IntStack other = (IntStack)obj;
            if (size != other.size) return false;
            return Arrays.equals(stack, other.stack);
        }
    }

    /**
     * This is the interface that a lexical analyzer is expected to implement.
     */
    public static interface ILexer
    {
        /**
         * Returns the next token the parser should process.  The last token
         * returned must be <code>Terminal.END_OF_INPUT</code>.
         *
         * @return a token for the parser to process
         *
         * @throws IOException, SyntaxException, Exception
         */
        Token yylex() throws IOException, SyntaxException, Exception;

        /**
         * Returns a description of the position of the last token returned by
         * {@link #yylex()}.
         *
         * This string will be appended to the end of diagnostic/error messages
         * and usually has a form similar to &quot; (line n, column m)&quot;.
         *
         * @return a description of the last token's position
         */
        String describeLastTokenPos();
    }
}

@SuppressWarnings("all")
final class SemanticActions
{
    public void initialize()
    {
            
    }

    public void deinitialize()
    {
            
    }

    public void onErrorRecovery(ErrorRecoveryInfo errorInfo)
    {
                    for (int i = 0; i < errorInfo.discardedSymbols.size(); i++)
                    {
                        if (errorInfo.discardedSymbols.get(i) instanceof HashMap)
                        {
                            HashMap map = (HashMap)errorInfo.discardedSymbols.get(i);
                            IASTListNode<IASTNode> errorRecoveryList = (IASTListNode<IASTNode>)map.get("errorRecoveryList");
                            errorInfo.<IASTNode>getDiscardedSymbols().set(i, errorRecoveryList);
                            for (IASTNode n : errorRecoveryList)
                                if (n != null)
                                    n.setParent(errorRecoveryList);
                        }
                    }
    }
        public Object handle(int productionIndex, List<Object> valueStack, int valueStackOffset, int valueStackSize, ErrorRecoveryInfo errorInfo)
        {
            switch (productionIndex)
            {
                case Production.ACC_CONSTRUCT_1_INDEX:
                {
                    // Case 5
                    ASTAccLoopNode result = (ASTAccLoopNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_2_INDEX:
                {
                    // Case 5
                    ASTAccParallelLoopNode result = (ASTAccParallelLoopNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_3_INDEX:
                {
                    // Case 5
                    ASTAccKernelsLoopNode result = (ASTAccKernelsLoopNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_4_INDEX:
                {
                    // Case 5
                    ASTAccParallelNode result = (ASTAccParallelNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_5_INDEX:
                {
                    // Case 5
                    ASTAccKernelsNode result = (ASTAccKernelsNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_6_INDEX:
                {
                    // Case 5
                    ASTAccDataNode result = (ASTAccDataNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_7_INDEX:
                {
                    // Case 5
                    ASTAccHostdataNode result = (ASTAccHostdataNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_8_INDEX:
                {
                    // Case 5
                    ASTAccDeclareNode result = (ASTAccDeclareNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_9_INDEX:
                {
                    // Case 5
                    ASTAccCacheNode result = (ASTAccCacheNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_10_INDEX:
                {
                    // Case 5
                    ASTAccUpdateNode result = (ASTAccUpdateNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_11_INDEX:
                {
                    // Case 5
                    ASTAccWaitNode result = (ASTAccWaitNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_12_INDEX:
                {
                    // Case 5
                    ASTAccEnterDataNode result = (ASTAccEnterDataNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_13_INDEX:
                {
                    // Case 5
                    ASTAccExitDataNode result = (ASTAccExitDataNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_14_INDEX:
                {
                    // Case 5
                    ASTAccRoutineNode result = (ASTAccRoutineNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_15_INDEX:
                {
                    // Case 5
                    ASTAccAtomicNode result = (ASTAccAtomicNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CONSTRUCT_16_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccNoConstruct node = new ASTAccNoConstruct();
                    return node;

                }
                case Production.ACC_LOOP_17_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccLoopNode node = new ASTAccLoopNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringLoop = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLoop != null) node.hiddenLiteralStringLoop.setParent(node);
                    node.accLoopClauseList = (IASTListNode<ASTAccLoopClauseListNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accLoopClauseList != null) node.accLoopClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_LOOP_18_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccLoopNode node = new ASTAccLoopNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringLoop = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLoop != null) node.hiddenLiteralStringLoop.setParent(node);
                    return node;

                }
                case Production.ACC_LOOP_CLAUSE_LIST_19_INDEX:
                {
                    // Case 10
                    ASTAccLoopClauseListNode node = new ASTAccLoopClauseListNode();
                    node.accLoopClause = (IAccLoopClause)valueStack.get(valueStackOffset + 0);
                    if (node.accLoopClause != null) node.accLoopClause.setParent(node);
                    ASTListNode<ASTAccLoopClauseListNode> list = new ASTListNode<ASTAccLoopClauseListNode>();
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_LOOP_CLAUSE_LIST_20_INDEX:
                {
                    // Case 11
                    ASTAccLoopClauseListNode node = new ASTAccLoopClauseListNode();
                    node.accLoopClause = (IAccLoopClause)valueStack.get(valueStackOffset + 1);
                    if (node.accLoopClause != null) node.accLoopClause.setParent(node);
                    ASTListNode<ASTAccLoopClauseListNode> list = (ASTListNode<ASTAccLoopClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_LOOP_CLAUSE_LIST_21_INDEX:
                {
                    // Case 11
                    ASTAccLoopClauseListNode node = new ASTAccLoopClauseListNode();
                    node.hiddenLiteralStringComma = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringComma != null) node.hiddenLiteralStringComma.setParent(node);
                    node.accLoopClause = (IAccLoopClause)valueStack.get(valueStackOffset + 2);
                    if (node.accLoopClause != null) node.accLoopClause.setParent(node);
                    ASTListNode<ASTAccLoopClauseListNode> list = (ASTListNode<ASTAccLoopClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_LOOP_CLAUSE_22_INDEX:
                {
                    // Case 5
                    ASTAccCollapseClauseNode result = (ASTAccCollapseClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_LOOP_CLAUSE_23_INDEX:
                {
                    // Case 5
                    ASTAccGangClauseNode result = (ASTAccGangClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_LOOP_CLAUSE_24_INDEX:
                {
                    // Case 5
                    ASTAccWorkerClauseNode result = (ASTAccWorkerClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_LOOP_CLAUSE_25_INDEX:
                {
                    // Case 5
                    ASTAccVectorClauseNode result = (ASTAccVectorClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_LOOP_CLAUSE_26_INDEX:
                {
                    // Cases 1 and 2
                    CAccSeqClause node = new CAccSeqClause();
                    node.hiddenLiteralStringSeq = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringSeq != null) node.hiddenLiteralStringSeq.setParent(node);
                    return node;

                }
                case Production.ACC_LOOP_CLAUSE_27_INDEX:
                {
                    // Cases 1 and 2
                    CAccAutoClause node = new CAccAutoClause();
                    node.hiddenLiteralStringAuto = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringAuto != null) node.hiddenLiteralStringAuto.setParent(node);
                    return node;

                }
                case Production.ACC_LOOP_CLAUSE_28_INDEX:
                {
                    // Case 5
                    ASTAccTileClauseNode result = (ASTAccTileClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_LOOP_CLAUSE_29_INDEX:
                {
                    // Cases 1 and 2
                    CAccIndependentClause node = new CAccIndependentClause();
                    node.hiddenLiteralStringIndependent = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringIndependent != null) node.hiddenLiteralStringIndependent.setParent(node);
                    return node;

                }
                case Production.ACC_LOOP_CLAUSE_30_INDEX:
                {
                    // Case 5
                    ASTAccPrivateClauseNode result = (ASTAccPrivateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_LOOP_CLAUSE_31_INDEX:
                {
                    // Case 5
                    ASTAccReductionClauseNode result = (ASTAccReductionClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_32_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccParallelNode node = new ASTAccParallelNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringParallel = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringParallel != null) node.hiddenLiteralStringParallel.setParent(node);
                    node.accParallelClauseList = (IASTListNode<ASTAccParallelClauseListNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accParallelClauseList != null) node.accParallelClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_PARALLEL_33_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccParallelNode node = new ASTAccParallelNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringParallel = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringParallel != null) node.hiddenLiteralStringParallel.setParent(node);
                    return node;

                }
                case Production.ACC_PARALLEL_CLAUSE_LIST_34_INDEX:
                {
                    // Case 10
                    ASTAccParallelClauseListNode node = new ASTAccParallelClauseListNode();
                    node.accParallelClause = (IAccParallelClause)valueStack.get(valueStackOffset + 0);
                    if (node.accParallelClause != null) node.accParallelClause.setParent(node);
                    ASTListNode<ASTAccParallelClauseListNode> list = new ASTListNode<ASTAccParallelClauseListNode>();
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_PARALLEL_CLAUSE_LIST_35_INDEX:
                {
                    // Case 11
                    ASTAccParallelClauseListNode node = new ASTAccParallelClauseListNode();
                    node.accParallelClause = (IAccParallelClause)valueStack.get(valueStackOffset + 1);
                    if (node.accParallelClause != null) node.accParallelClause.setParent(node);
                    ASTListNode<ASTAccParallelClauseListNode> list = (ASTListNode<ASTAccParallelClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_PARALLEL_CLAUSE_LIST_36_INDEX:
                {
                    // Case 11
                    ASTAccParallelClauseListNode node = new ASTAccParallelClauseListNode();
                    node.hiddenLiteralStringComma = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringComma != null) node.hiddenLiteralStringComma.setParent(node);
                    node.accParallelClause = (IAccParallelClause)valueStack.get(valueStackOffset + 2);
                    if (node.accParallelClause != null) node.accParallelClause.setParent(node);
                    ASTListNode<ASTAccParallelClauseListNode> list = (ASTListNode<ASTAccParallelClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_PARALLEL_CLAUSE_37_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_38_INDEX:
                {
                    // Case 5
                    ASTAccAsyncClauseNode result = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_39_INDEX:
                {
                    // Case 5
                    ASTAccWaitClauseNode result = (ASTAccWaitClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_40_INDEX:
                {
                    // Case 5
                    ASTAccNumgangsClauseNode result = (ASTAccNumgangsClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_41_INDEX:
                {
                    // Case 5
                    ASTAccNumworkersClauseNode result = (ASTAccNumworkersClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_42_INDEX:
                {
                    // Case 5
                    ASTAccVectorlengthClauseNode result = (ASTAccVectorlengthClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_43_INDEX:
                {
                    // Case 5
                    ASTAccReductionClauseNode result = (ASTAccReductionClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_44_INDEX:
                {
                    // Case 5
                    ASTAccCopyClauseNode result = (ASTAccCopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_45_INDEX:
                {
                    // Case 5
                    ASTAccCopyinClauseNode result = (ASTAccCopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_46_INDEX:
                {
                    // Case 5
                    ASTAccCopyoutClauseNode result = (ASTAccCopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_47_INDEX:
                {
                    // Case 5
                    ASTAccCreateClauseNode result = (ASTAccCreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_48_INDEX:
                {
                    // Case 5
                    ASTAccPresentClauseNode result = (ASTAccPresentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_49_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyClauseNode result = (ASTAccPresentorcopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_50_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyinClauseNode result = (ASTAccPresentorcopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_51_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyoutClauseNode result = (ASTAccPresentorcopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_52_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcreateClauseNode result = (ASTAccPresentorcreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_53_INDEX:
                {
                    // Case 5
                    ASTAccDeviceptrClauseNode result = (ASTAccDeviceptrClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_54_INDEX:
                {
                    // Case 5
                    ASTAccPrivateClauseNode result = (ASTAccPrivateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_55_INDEX:
                {
                    // Case 5
                    ASTAccFirstprivateClauseNode result = (ASTAccFirstprivateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_56_INDEX:
                {
                    // Case 5
                    ASTAccDefaultnoneClauseNode result = (ASTAccDefaultnoneClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_57_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccParallelLoopNode node = new ASTAccParallelLoopNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringParallel = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringParallel != null) node.hiddenLiteralStringParallel.setParent(node);
                    node.hiddenLiteralStringLoop = (Token)valueStack.get(valueStackOffset + 2);
                    if (node.hiddenLiteralStringLoop != null) node.hiddenLiteralStringLoop.setParent(node);
                    node.accParallelLoopClauseList = (IASTListNode<ASTAccParallelLoopClauseListNode>)valueStack.get(valueStackOffset + 3);
                    if (node.accParallelLoopClauseList != null) node.accParallelLoopClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_PARALLEL_LOOP_58_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccParallelLoopNode node = new ASTAccParallelLoopNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringParallel = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringParallel != null) node.hiddenLiteralStringParallel.setParent(node);
                    node.hiddenLiteralStringLoop = (Token)valueStack.get(valueStackOffset + 2);
                    if (node.hiddenLiteralStringLoop != null) node.hiddenLiteralStringLoop.setParent(node);
                    return node;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_LIST_59_INDEX:
                {
                    // Case 10
                    ASTAccParallelLoopClauseListNode node = new ASTAccParallelLoopClauseListNode();
                    node.accParallelLoopClause = (IAccParallelLoopClause)valueStack.get(valueStackOffset + 0);
                    if (node.accParallelLoopClause != null) node.accParallelLoopClause.setParent(node);
                    ASTListNode<ASTAccParallelLoopClauseListNode> list = new ASTListNode<ASTAccParallelLoopClauseListNode>();
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_LIST_60_INDEX:
                {
                    // Case 11
                    ASTAccParallelLoopClauseListNode node = new ASTAccParallelLoopClauseListNode();
                    node.accParallelLoopClause = (IAccParallelLoopClause)valueStack.get(valueStackOffset + 1);
                    if (node.accParallelLoopClause != null) node.accParallelLoopClause.setParent(node);
                    ASTListNode<ASTAccParallelLoopClauseListNode> list = (ASTListNode<ASTAccParallelLoopClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_LIST_61_INDEX:
                {
                    // Case 11
                    ASTAccParallelLoopClauseListNode node = new ASTAccParallelLoopClauseListNode();
                    node.hiddenLiteralStringComma = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringComma != null) node.hiddenLiteralStringComma.setParent(node);
                    node.accParallelLoopClause = (IAccParallelLoopClause)valueStack.get(valueStackOffset + 2);
                    if (node.accParallelLoopClause != null) node.accParallelLoopClause.setParent(node);
                    ASTListNode<ASTAccParallelLoopClauseListNode> list = (ASTListNode<ASTAccParallelLoopClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_62_INDEX:
                {
                    // Case 5
                    ASTAccCollapseClauseNode result = (ASTAccCollapseClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_63_INDEX:
                {
                    // Case 5
                    ASTAccGangClauseNode result = (ASTAccGangClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_64_INDEX:
                {
                    // Case 5
                    ASTAccWorkerClauseNode result = (ASTAccWorkerClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_65_INDEX:
                {
                    // Case 5
                    ASTAccVectorClauseNode result = (ASTAccVectorClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_66_INDEX:
                {
                    // Cases 1 and 2
                    CAccSeqClause node = new CAccSeqClause();
                    node.hiddenLiteralStringSeq = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringSeq != null) node.hiddenLiteralStringSeq.setParent(node);
                    return node;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_67_INDEX:
                {
                    // Cases 1 and 2
                    CAccAutoClause node = new CAccAutoClause();
                    node.hiddenLiteralStringAuto = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringAuto != null) node.hiddenLiteralStringAuto.setParent(node);
                    return node;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_68_INDEX:
                {
                    // Case 5
                    ASTAccTileClauseNode result = (ASTAccTileClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_69_INDEX:
                {
                    // Cases 1 and 2
                    CAccIndependentClause node = new CAccIndependentClause();
                    node.hiddenLiteralStringIndependent = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringIndependent != null) node.hiddenLiteralStringIndependent.setParent(node);
                    return node;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_70_INDEX:
                {
                    // Case 5
                    ASTAccPrivateClauseNode result = (ASTAccPrivateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_71_INDEX:
                {
                    // Case 5
                    ASTAccReductionClauseNode result = (ASTAccReductionClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_72_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_73_INDEX:
                {
                    // Case 5
                    ASTAccAsyncClauseNode result = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_74_INDEX:
                {
                    // Case 5
                    ASTAccWaitClauseNode result = (ASTAccWaitClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_75_INDEX:
                {
                    // Case 5
                    ASTAccNumgangsClauseNode result = (ASTAccNumgangsClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_76_INDEX:
                {
                    // Case 5
                    ASTAccNumworkersClauseNode result = (ASTAccNumworkersClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_77_INDEX:
                {
                    // Case 5
                    ASTAccVectorlengthClauseNode result = (ASTAccVectorlengthClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_78_INDEX:
                {
                    // Case 5
                    ASTAccCopyClauseNode result = (ASTAccCopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_79_INDEX:
                {
                    // Case 5
                    ASTAccCopyinClauseNode result = (ASTAccCopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_80_INDEX:
                {
                    // Case 5
                    ASTAccCopyoutClauseNode result = (ASTAccCopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_81_INDEX:
                {
                    // Case 5
                    ASTAccCreateClauseNode result = (ASTAccCreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_82_INDEX:
                {
                    // Case 5
                    ASTAccPresentClauseNode result = (ASTAccPresentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_83_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyClauseNode result = (ASTAccPresentorcopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_84_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyinClauseNode result = (ASTAccPresentorcopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_85_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyoutClauseNode result = (ASTAccPresentorcopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_86_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcreateClauseNode result = (ASTAccPresentorcreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_87_INDEX:
                {
                    // Case 5
                    ASTAccDeviceptrClauseNode result = (ASTAccDeviceptrClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_88_INDEX:
                {
                    // Case 5
                    ASTAccFirstprivateClauseNode result = (ASTAccFirstprivateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_89_INDEX:
                {
                    // Case 5
                    ASTAccDefaultnoneClauseNode result = (ASTAccDefaultnoneClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_90_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccKernelsNode node = new ASTAccKernelsNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringKernels = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringKernels != null) node.hiddenLiteralStringKernels.setParent(node);
                    node.accKernelsClauseList = (IASTListNode<ASTAccKernelsClauseListNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accKernelsClauseList != null) node.accKernelsClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_KERNELS_91_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccKernelsNode node = new ASTAccKernelsNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringKernels = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringKernels != null) node.hiddenLiteralStringKernels.setParent(node);
                    return node;

                }
                case Production.ACC_KERNELS_CLAUSE_LIST_92_INDEX:
                {
                    // Case 10
                    ASTAccKernelsClauseListNode node = new ASTAccKernelsClauseListNode();
                    node.accKernelsClause = (IAccKernelsClause)valueStack.get(valueStackOffset + 0);
                    if (node.accKernelsClause != null) node.accKernelsClause.setParent(node);
                    ASTListNode<ASTAccKernelsClauseListNode> list = new ASTListNode<ASTAccKernelsClauseListNode>();
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_KERNELS_CLAUSE_LIST_93_INDEX:
                {
                    // Case 11
                    ASTAccKernelsClauseListNode node = new ASTAccKernelsClauseListNode();
                    node.accKernelsClause = (IAccKernelsClause)valueStack.get(valueStackOffset + 1);
                    if (node.accKernelsClause != null) node.accKernelsClause.setParent(node);
                    ASTListNode<ASTAccKernelsClauseListNode> list = (ASTListNode<ASTAccKernelsClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_KERNELS_CLAUSE_LIST_94_INDEX:
                {
                    // Case 11
                    ASTAccKernelsClauseListNode node = new ASTAccKernelsClauseListNode();
                    node.hiddenLiteralStringComma = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringComma != null) node.hiddenLiteralStringComma.setParent(node);
                    node.accKernelsClause = (IAccKernelsClause)valueStack.get(valueStackOffset + 2);
                    if (node.accKernelsClause != null) node.accKernelsClause.setParent(node);
                    ASTListNode<ASTAccKernelsClauseListNode> list = (ASTListNode<ASTAccKernelsClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_KERNELS_CLAUSE_95_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_96_INDEX:
                {
                    // Case 5
                    ASTAccAsyncClauseNode result = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_97_INDEX:
                {
                    // Case 5
                    ASTAccWaitClauseNode result = (ASTAccWaitClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_98_INDEX:
                {
                    // Case 5
                    ASTAccCopyClauseNode result = (ASTAccCopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_99_INDEX:
                {
                    // Case 5
                    ASTAccCopyinClauseNode result = (ASTAccCopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_100_INDEX:
                {
                    // Case 5
                    ASTAccCopyoutClauseNode result = (ASTAccCopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_101_INDEX:
                {
                    // Case 5
                    ASTAccCreateClauseNode result = (ASTAccCreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_102_INDEX:
                {
                    // Case 5
                    ASTAccPresentClauseNode result = (ASTAccPresentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_103_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyClauseNode result = (ASTAccPresentorcopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_104_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyinClauseNode result = (ASTAccPresentorcopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_105_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyoutClauseNode result = (ASTAccPresentorcopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_106_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcreateClauseNode result = (ASTAccPresentorcreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_107_INDEX:
                {
                    // Case 5
                    ASTAccDeviceptrClauseNode result = (ASTAccDeviceptrClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_108_INDEX:
                {
                    // Case 5
                    ASTAccDefaultnoneClauseNode result = (ASTAccDefaultnoneClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_109_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccKernelsLoopNode node = new ASTAccKernelsLoopNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringKernels = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringKernels != null) node.hiddenLiteralStringKernels.setParent(node);
                    node.hiddenLiteralStringLoop = (Token)valueStack.get(valueStackOffset + 2);
                    if (node.hiddenLiteralStringLoop != null) node.hiddenLiteralStringLoop.setParent(node);
                    node.accKernelsLoopClauseList = (IASTListNode<ASTAccKernelsLoopClauseListNode>)valueStack.get(valueStackOffset + 3);
                    if (node.accKernelsLoopClauseList != null) node.accKernelsLoopClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_KERNELS_LOOP_110_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccKernelsLoopNode node = new ASTAccKernelsLoopNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringKernels = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringKernels != null) node.hiddenLiteralStringKernels.setParent(node);
                    node.hiddenLiteralStringLoop = (Token)valueStack.get(valueStackOffset + 2);
                    if (node.hiddenLiteralStringLoop != null) node.hiddenLiteralStringLoop.setParent(node);
                    return node;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_LIST_111_INDEX:
                {
                    // Case 10
                    ASTAccKernelsLoopClauseListNode node = new ASTAccKernelsLoopClauseListNode();
                    node.accKernelsLoopClause = (IAccKernelsLoopClause)valueStack.get(valueStackOffset + 0);
                    if (node.accKernelsLoopClause != null) node.accKernelsLoopClause.setParent(node);
                    ASTListNode<ASTAccKernelsLoopClauseListNode> list = new ASTListNode<ASTAccKernelsLoopClauseListNode>();
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_LIST_112_INDEX:
                {
                    // Case 11
                    ASTAccKernelsLoopClauseListNode node = new ASTAccKernelsLoopClauseListNode();
                    node.accKernelsLoopClause = (IAccKernelsLoopClause)valueStack.get(valueStackOffset + 1);
                    if (node.accKernelsLoopClause != null) node.accKernelsLoopClause.setParent(node);
                    ASTListNode<ASTAccKernelsLoopClauseListNode> list = (ASTListNode<ASTAccKernelsLoopClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_LIST_113_INDEX:
                {
                    // Case 11
                    ASTAccKernelsLoopClauseListNode node = new ASTAccKernelsLoopClauseListNode();
                    node.hiddenLiteralStringComma = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringComma != null) node.hiddenLiteralStringComma.setParent(node);
                    node.accKernelsLoopClause = (IAccKernelsLoopClause)valueStack.get(valueStackOffset + 2);
                    if (node.accKernelsLoopClause != null) node.accKernelsLoopClause.setParent(node);
                    ASTListNode<ASTAccKernelsLoopClauseListNode> list = (ASTListNode<ASTAccKernelsLoopClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_114_INDEX:
                {
                    // Case 5
                    ASTAccCollapseClauseNode result = (ASTAccCollapseClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_115_INDEX:
                {
                    // Case 5
                    ASTAccGangClauseNode result = (ASTAccGangClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_116_INDEX:
                {
                    // Case 5
                    ASTAccWorkerClauseNode result = (ASTAccWorkerClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_117_INDEX:
                {
                    // Case 5
                    ASTAccVectorClauseNode result = (ASTAccVectorClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_118_INDEX:
                {
                    // Cases 1 and 2
                    CAccSeqClause node = new CAccSeqClause();
                    node.hiddenLiteralStringSeq = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringSeq != null) node.hiddenLiteralStringSeq.setParent(node);
                    return node;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_119_INDEX:
                {
                    // Cases 1 and 2
                    CAccAutoClause node = new CAccAutoClause();
                    node.hiddenLiteralStringAuto = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringAuto != null) node.hiddenLiteralStringAuto.setParent(node);
                    return node;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_120_INDEX:
                {
                    // Case 5
                    ASTAccTileClauseNode result = (ASTAccTileClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_121_INDEX:
                {
                    // Cases 1 and 2
                    CAccIndependentClause node = new CAccIndependentClause();
                    node.hiddenLiteralStringIndependent = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringIndependent != null) node.hiddenLiteralStringIndependent.setParent(node);
                    return node;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_122_INDEX:
                {
                    // Case 5
                    ASTAccPrivateClauseNode result = (ASTAccPrivateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_123_INDEX:
                {
                    // Case 5
                    ASTAccReductionClauseNode result = (ASTAccReductionClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_124_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_125_INDEX:
                {
                    // Case 5
                    ASTAccAsyncClauseNode result = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_126_INDEX:
                {
                    // Case 5
                    ASTAccWaitClauseNode result = (ASTAccWaitClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_127_INDEX:
                {
                    // Case 5
                    ASTAccCopyClauseNode result = (ASTAccCopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_128_INDEX:
                {
                    // Case 5
                    ASTAccCopyinClauseNode result = (ASTAccCopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_129_INDEX:
                {
                    // Case 5
                    ASTAccCopyoutClauseNode result = (ASTAccCopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_130_INDEX:
                {
                    // Case 5
                    ASTAccCreateClauseNode result = (ASTAccCreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_131_INDEX:
                {
                    // Case 5
                    ASTAccPresentClauseNode result = (ASTAccPresentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_132_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyClauseNode result = (ASTAccPresentorcopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_133_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyinClauseNode result = (ASTAccPresentorcopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_134_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyoutClauseNode result = (ASTAccPresentorcopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_135_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcreateClauseNode result = (ASTAccPresentorcreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_136_INDEX:
                {
                    // Case 5
                    ASTAccDeviceptrClauseNode result = (ASTAccDeviceptrClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_137_INDEX:
                {
                    // Case 5
                    ASTAccDefaultnoneClauseNode result = (ASTAccDefaultnoneClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_138_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccDeclareNode node = new ASTAccDeclareNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringDeclare = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringDeclare != null) node.hiddenLiteralStringDeclare.setParent(node);
                    node.accDeclareClauseList = (IASTListNode<ASTAccDeclareClauseListNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDeclareClauseList != null) node.accDeclareClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_DECLARE_CLAUSE_LIST_139_INDEX:
                {
                    // Case 10
                    ASTAccDeclareClauseListNode node = new ASTAccDeclareClauseListNode();
                    node.accDeclareClause = (IAccDeclareClause)valueStack.get(valueStackOffset + 0);
                    if (node.accDeclareClause != null) node.accDeclareClause.setParent(node);
                    ASTListNode<ASTAccDeclareClauseListNode> list = new ASTListNode<ASTAccDeclareClauseListNode>();
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_DECLARE_CLAUSE_LIST_140_INDEX:
                {
                    // Case 11
                    ASTAccDeclareClauseListNode node = new ASTAccDeclareClauseListNode();
                    node.accDeclareClause = (IAccDeclareClause)valueStack.get(valueStackOffset + 1);
                    if (node.accDeclareClause != null) node.accDeclareClause.setParent(node);
                    ASTListNode<ASTAccDeclareClauseListNode> list = (ASTListNode<ASTAccDeclareClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_DECLARE_CLAUSE_LIST_141_INDEX:
                {
                    // Case 11
                    ASTAccDeclareClauseListNode node = new ASTAccDeclareClauseListNode();
                    node.hiddenLiteralStringComma = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringComma != null) node.hiddenLiteralStringComma.setParent(node);
                    node.accDeclareClause = (IAccDeclareClause)valueStack.get(valueStackOffset + 2);
                    if (node.accDeclareClause != null) node.accDeclareClause.setParent(node);
                    ASTListNode<ASTAccDeclareClauseListNode> list = (ASTListNode<ASTAccDeclareClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_DECLARE_CLAUSE_142_INDEX:
                {
                    // Case 5
                    ASTAccCopyClauseNode result = (ASTAccCopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_143_INDEX:
                {
                    // Case 5
                    ASTAccCopyinClauseNode result = (ASTAccCopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_144_INDEX:
                {
                    // Case 5
                    ASTAccCopyoutClauseNode result = (ASTAccCopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_145_INDEX:
                {
                    // Case 5
                    ASTAccCreateClauseNode result = (ASTAccCreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_146_INDEX:
                {
                    // Case 5
                    ASTAccPresentClauseNode result = (ASTAccPresentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_147_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyClauseNode result = (ASTAccPresentorcopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_148_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyinClauseNode result = (ASTAccPresentorcopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_149_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyoutClauseNode result = (ASTAccPresentorcopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_150_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcreateClauseNode result = (ASTAccPresentorcreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_151_INDEX:
                {
                    // Case 5
                    ASTAccDeviceptrClauseNode result = (ASTAccDeviceptrClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_152_INDEX:
                {
                    // Case 5
                    ASTAccDeviceresidentClauseNode result = (ASTAccDeviceresidentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_153_INDEX:
                {
                    // Case 5
                    ASTAccLinkClauseNode result = (ASTAccLinkClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_154_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccDataNode node = new ASTAccDataNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringData = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringData != null) node.hiddenLiteralStringData.setParent(node);
                    node.accDataClauseList = (IASTListNode<ASTAccDataClauseListNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataClauseList != null) node.accDataClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_DATA_CLAUSE_LIST_155_INDEX:
                {
                    // Case 10
                    ASTAccDataClauseListNode node = new ASTAccDataClauseListNode();
                    node.accDataClause = (IAccDataClause)valueStack.get(valueStackOffset + 0);
                    if (node.accDataClause != null) node.accDataClause.setParent(node);
                    ASTListNode<ASTAccDataClauseListNode> list = new ASTListNode<ASTAccDataClauseListNode>();
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_DATA_CLAUSE_LIST_156_INDEX:
                {
                    // Case 11
                    ASTAccDataClauseListNode node = new ASTAccDataClauseListNode();
                    node.accDataClause = (IAccDataClause)valueStack.get(valueStackOffset + 1);
                    if (node.accDataClause != null) node.accDataClause.setParent(node);
                    ASTListNode<ASTAccDataClauseListNode> list = (ASTListNode<ASTAccDataClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_DATA_CLAUSE_LIST_157_INDEX:
                {
                    // Case 11
                    ASTAccDataClauseListNode node = new ASTAccDataClauseListNode();
                    node.hiddenLiteralStringComma = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringComma != null) node.hiddenLiteralStringComma.setParent(node);
                    node.accDataClause = (IAccDataClause)valueStack.get(valueStackOffset + 2);
                    if (node.accDataClause != null) node.accDataClause.setParent(node);
                    ASTListNode<ASTAccDataClauseListNode> list = (ASTListNode<ASTAccDataClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_DATA_CLAUSE_158_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_159_INDEX:
                {
                    // Case 5
                    ASTAccCopyClauseNode result = (ASTAccCopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_160_INDEX:
                {
                    // Case 5
                    ASTAccCopyinClauseNode result = (ASTAccCopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_161_INDEX:
                {
                    // Case 5
                    ASTAccCopyoutClauseNode result = (ASTAccCopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_162_INDEX:
                {
                    // Case 5
                    ASTAccCreateClauseNode result = (ASTAccCreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_163_INDEX:
                {
                    // Case 5
                    ASTAccPresentClauseNode result = (ASTAccPresentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_164_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyClauseNode result = (ASTAccPresentorcopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_165_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyinClauseNode result = (ASTAccPresentorcopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_166_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyoutClauseNode result = (ASTAccPresentorcopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_167_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcreateClauseNode result = (ASTAccPresentorcreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_168_INDEX:
                {
                    // Case 5
                    ASTAccDeviceptrClauseNode result = (ASTAccDeviceptrClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_HOSTDATA_169_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccHostdataNode node = new ASTAccHostdataNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringHostUnderscoredata = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringHostUnderscoredata != null) node.hiddenLiteralStringHostUnderscoredata.setParent(node);
                    node.accHostdataClauseList = (IASTListNode<ASTAccHostdataClauseListNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accHostdataClauseList != null) node.accHostdataClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_HOSTDATA_CLAUSE_LIST_170_INDEX:
                {
                    // Case 10
                    ASTAccHostdataClauseListNode node = new ASTAccHostdataClauseListNode();
                    node.accHostdataClause = (IAccHostdataClause)valueStack.get(valueStackOffset + 0);
                    if (node.accHostdataClause != null) node.accHostdataClause.setParent(node);
                    ASTListNode<ASTAccHostdataClauseListNode> list = new ASTListNode<ASTAccHostdataClauseListNode>();
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_HOSTDATA_CLAUSE_LIST_171_INDEX:
                {
                    // Case 11
                    ASTAccHostdataClauseListNode node = new ASTAccHostdataClauseListNode();
                    node.accHostdataClause = (IAccHostdataClause)valueStack.get(valueStackOffset + 1);
                    if (node.accHostdataClause != null) node.accHostdataClause.setParent(node);
                    ASTListNode<ASTAccHostdataClauseListNode> list = (ASTListNode<ASTAccHostdataClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_HOSTDATA_CLAUSE_LIST_172_INDEX:
                {
                    // Case 11
                    ASTAccHostdataClauseListNode node = new ASTAccHostdataClauseListNode();
                    node.hiddenLiteralStringComma = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringComma != null) node.hiddenLiteralStringComma.setParent(node);
                    node.accHostdataClause = (IAccHostdataClause)valueStack.get(valueStackOffset + 2);
                    if (node.accHostdataClause != null) node.accHostdataClause.setParent(node);
                    ASTListNode<ASTAccHostdataClauseListNode> list = (ASTListNode<ASTAccHostdataClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_HOSTDATA_CLAUSE_173_INDEX:
                {
                    // Case 5
                    ASTAccUsedeviceClauseNode result = (ASTAccUsedeviceClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CACHE_174_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccCacheNode node = new ASTAccCacheNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringCache = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringCache != null) node.hiddenLiteralStringCache.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 2);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 3);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 4);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_WAIT_175_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccWaitNode node = new ASTAccWaitNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringWait = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringWait != null) node.hiddenLiteralStringWait.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 2)).get("hiddenLiteralStringLparen");
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.waitParameter = (IConstantExpression)((Map<String, Object>)valueStack.get(valueStackOffset + 2)).get("waitParameter");
                    if (node.waitParameter != null) node.waitParameter.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 2)).get("hiddenLiteralStringRparen");
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    node.accWaitClauseList = (IASTListNode<ASTAccWaitClauseListNode>)valueStack.get(valueStackOffset + 3);
                    if (node.accWaitClauseList != null) node.accWaitClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_WAIT_176_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccWaitNode node = new ASTAccWaitNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringWait = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringWait != null) node.hiddenLiteralStringWait.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 2)).get("hiddenLiteralStringLparen");
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.waitParameter = (IConstantExpression)((Map<String, Object>)valueStack.get(valueStackOffset + 2)).get("waitParameter");
                    if (node.waitParameter != null) node.waitParameter.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 2)).get("hiddenLiteralStringRparen");
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_WAIT_177_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccWaitNode node = new ASTAccWaitNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringWait = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringWait != null) node.hiddenLiteralStringWait.setParent(node);
                    node.accWaitClauseList = (IASTListNode<ASTAccWaitClauseListNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accWaitClauseList != null) node.accWaitClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_WAIT_178_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccWaitNode node = new ASTAccWaitNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringWait = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringWait != null) node.hiddenLiteralStringWait.setParent(node);
                    return node;

                }
                case Production.ACC_WAIT_PARAMETER_179_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("hiddenLiteralStringLparen", (Token)valueStack.get(valueStackOffset + 0));
                    node.put("waitParameter", (IConstantExpression)valueStack.get(valueStackOffset + 1));
                    node.put("hiddenLiteralStringRparen", (Token)valueStack.get(valueStackOffset + 2));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("hiddenLiteralStringLparen")));
                    embeddedList.add((IASTNode)(node.get("waitParameter")));
                    embeddedList.add((IASTNode)(node.get("hiddenLiteralStringRparen")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_WAIT_CLAUSE_LIST_180_INDEX:
                {
                    // Case 10
                    ASTAccWaitClauseListNode node = new ASTAccWaitClauseListNode();
                    node.accAsyncClause = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 0);
                    if (node.accAsyncClause != null) node.accAsyncClause.setParent(node);
                    ASTListNode<ASTAccWaitClauseListNode> list = new ASTListNode<ASTAccWaitClauseListNode>();
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_WAIT_CLAUSE_LIST_181_INDEX:
                {
                    // Case 11
                    ASTAccWaitClauseListNode node = new ASTAccWaitClauseListNode();
                    node.accAsyncClause = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 1);
                    if (node.accAsyncClause != null) node.accAsyncClause.setParent(node);
                    ASTListNode<ASTAccWaitClauseListNode> list = (ASTListNode<ASTAccWaitClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_WAIT_CLAUSE_LIST_182_INDEX:
                {
                    // Case 11
                    ASTAccWaitClauseListNode node = new ASTAccWaitClauseListNode();
                    node.hiddenLiteralStringComma = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringComma != null) node.hiddenLiteralStringComma.setParent(node);
                    node.accAsyncClause = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 2);
                    if (node.accAsyncClause != null) node.accAsyncClause.setParent(node);
                    ASTListNode<ASTAccWaitClauseListNode> list = (ASTListNode<ASTAccWaitClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_UPDATE_183_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccUpdateNode node = new ASTAccUpdateNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringUpdate = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringUpdate != null) node.hiddenLiteralStringUpdate.setParent(node);
                    node.accUpdateClauseList = (IASTListNode<ASTAccUpdateClauseListNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accUpdateClauseList != null) node.accUpdateClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_UPDATE_CLAUSE_LIST_184_INDEX:
                {
                    // Case 10
                    ASTAccUpdateClauseListNode node = new ASTAccUpdateClauseListNode();
                    node.accUpdateClause = (IAccUpdateClause)valueStack.get(valueStackOffset + 0);
                    if (node.accUpdateClause != null) node.accUpdateClause.setParent(node);
                    ASTListNode<ASTAccUpdateClauseListNode> list = new ASTListNode<ASTAccUpdateClauseListNode>();
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_UPDATE_CLAUSE_LIST_185_INDEX:
                {
                    // Case 11
                    ASTAccUpdateClauseListNode node = new ASTAccUpdateClauseListNode();
                    node.accUpdateClause = (IAccUpdateClause)valueStack.get(valueStackOffset + 1);
                    if (node.accUpdateClause != null) node.accUpdateClause.setParent(node);
                    ASTListNode<ASTAccUpdateClauseListNode> list = (ASTListNode<ASTAccUpdateClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_UPDATE_CLAUSE_LIST_186_INDEX:
                {
                    // Case 11
                    ASTAccUpdateClauseListNode node = new ASTAccUpdateClauseListNode();
                    node.hiddenLiteralStringComma = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringComma != null) node.hiddenLiteralStringComma.setParent(node);
                    node.accUpdateClause = (IAccUpdateClause)valueStack.get(valueStackOffset + 2);
                    if (node.accUpdateClause != null) node.accUpdateClause.setParent(node);
                    ASTListNode<ASTAccUpdateClauseListNode> list = (ASTListNode<ASTAccUpdateClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_UPDATE_CLAUSE_187_INDEX:
                {
                    // Case 5
                    ASTAccAsyncClauseNode result = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_UPDATE_CLAUSE_188_INDEX:
                {
                    // Case 5
                    ASTAccWaitClauseNode result = (ASTAccWaitClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_UPDATE_CLAUSE_189_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_UPDATE_CLAUSE_190_INDEX:
                {
                    // Case 5
                    ASTAccSelfClauseNode result = (ASTAccSelfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_UPDATE_CLAUSE_191_INDEX:
                {
                    // Case 5
                    ASTAccHostClauseNode result = (ASTAccHostClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_UPDATE_CLAUSE_192_INDEX:
                {
                    // Case 5
                    ASTAccDeviceClauseNode result = (ASTAccDeviceClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_ENTER_DATA_193_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccEnterDataNode node = new ASTAccEnterDataNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringEnter = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringEnter != null) node.hiddenLiteralStringEnter.setParent(node);
                    node.hiddenLiteralStringData = (Token)valueStack.get(valueStackOffset + 2);
                    if (node.hiddenLiteralStringData != null) node.hiddenLiteralStringData.setParent(node);
                    node.accEnterDataClauseList = (IASTListNode<ASTAccEnterDataClauseListNode>)valueStack.get(valueStackOffset + 3);
                    if (node.accEnterDataClauseList != null) node.accEnterDataClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_ENTER_DATA_CLAUSE_LIST_194_INDEX:
                {
                    // Case 10
                    ASTAccEnterDataClauseListNode node = new ASTAccEnterDataClauseListNode();
                    node.accEnterDataClause = (IAccEnterDataClause)valueStack.get(valueStackOffset + 0);
                    if (node.accEnterDataClause != null) node.accEnterDataClause.setParent(node);
                    ASTListNode<ASTAccEnterDataClauseListNode> list = new ASTListNode<ASTAccEnterDataClauseListNode>();
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_ENTER_DATA_CLAUSE_LIST_195_INDEX:
                {
                    // Case 11
                    ASTAccEnterDataClauseListNode node = new ASTAccEnterDataClauseListNode();
                    node.accEnterDataClause = (IAccEnterDataClause)valueStack.get(valueStackOffset + 1);
                    if (node.accEnterDataClause != null) node.accEnterDataClause.setParent(node);
                    ASTListNode<ASTAccEnterDataClauseListNode> list = (ASTListNode<ASTAccEnterDataClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_ENTER_DATA_CLAUSE_LIST_196_INDEX:
                {
                    // Case 11
                    ASTAccEnterDataClauseListNode node = new ASTAccEnterDataClauseListNode();
                    node.hiddenLiteralStringComma = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringComma != null) node.hiddenLiteralStringComma.setParent(node);
                    node.accEnterDataClause = (IAccEnterDataClause)valueStack.get(valueStackOffset + 2);
                    if (node.accEnterDataClause != null) node.accEnterDataClause.setParent(node);
                    ASTListNode<ASTAccEnterDataClauseListNode> list = (ASTListNode<ASTAccEnterDataClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_ENTER_DATA_CLAUSE_197_INDEX:
                {
                    // Case 5
                    ASTAccHostClauseNode result = (ASTAccHostClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_ENTER_DATA_CLAUSE_198_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_ENTER_DATA_CLAUSE_199_INDEX:
                {
                    // Case 5
                    ASTAccAsyncClauseNode result = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_ENTER_DATA_CLAUSE_200_INDEX:
                {
                    // Case 5
                    ASTAccWaitClauseNode result = (ASTAccWaitClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_ENTER_DATA_CLAUSE_201_INDEX:
                {
                    // Case 5
                    ASTAccCopyinClauseNode result = (ASTAccCopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_ENTER_DATA_CLAUSE_202_INDEX:
                {
                    // Case 5
                    ASTAccCreateClauseNode result = (ASTAccCreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_ENTER_DATA_CLAUSE_203_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyinClauseNode result = (ASTAccPresentorcopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_ENTER_DATA_CLAUSE_204_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcreateClauseNode result = (ASTAccPresentorcreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_EXIT_DATA_205_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccExitDataNode node = new ASTAccExitDataNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringExit = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringExit != null) node.hiddenLiteralStringExit.setParent(node);
                    node.hiddenLiteralStringData = (Token)valueStack.get(valueStackOffset + 2);
                    if (node.hiddenLiteralStringData != null) node.hiddenLiteralStringData.setParent(node);
                    node.accExitDataClauseList = (IASTListNode<ASTAccExitDataClauseListNode>)valueStack.get(valueStackOffset + 3);
                    if (node.accExitDataClauseList != null) node.accExitDataClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_EXIT_DATA_CLAUSE_LIST_206_INDEX:
                {
                    // Case 10
                    ASTAccExitDataClauseListNode node = new ASTAccExitDataClauseListNode();
                    node.accExitDataClause = (IAccExitDataClause)valueStack.get(valueStackOffset + 0);
                    if (node.accExitDataClause != null) node.accExitDataClause.setParent(node);
                    ASTListNode<ASTAccExitDataClauseListNode> list = new ASTListNode<ASTAccExitDataClauseListNode>();
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_EXIT_DATA_CLAUSE_LIST_207_INDEX:
                {
                    // Case 11
                    ASTAccExitDataClauseListNode node = new ASTAccExitDataClauseListNode();
                    node.accExitDataClause = (IAccExitDataClause)valueStack.get(valueStackOffset + 1);
                    if (node.accExitDataClause != null) node.accExitDataClause.setParent(node);
                    ASTListNode<ASTAccExitDataClauseListNode> list = (ASTListNode<ASTAccExitDataClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_EXIT_DATA_CLAUSE_LIST_208_INDEX:
                {
                    // Case 11
                    ASTAccExitDataClauseListNode node = new ASTAccExitDataClauseListNode();
                    node.hiddenLiteralStringComma = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringComma != null) node.hiddenLiteralStringComma.setParent(node);
                    node.accExitDataClause = (IAccExitDataClause)valueStack.get(valueStackOffset + 2);
                    if (node.accExitDataClause != null) node.accExitDataClause.setParent(node);
                    ASTListNode<ASTAccExitDataClauseListNode> list = (ASTListNode<ASTAccExitDataClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_EXIT_DATA_CLAUSE_209_INDEX:
                {
                    // Case 5
                    ASTAccHostClauseNode result = (ASTAccHostClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_EXIT_DATA_CLAUSE_210_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_EXIT_DATA_CLAUSE_211_INDEX:
                {
                    // Case 5
                    ASTAccAsyncClauseNode result = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_EXIT_DATA_CLAUSE_212_INDEX:
                {
                    // Case 5
                    ASTAccWaitClauseNode result = (ASTAccWaitClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_EXIT_DATA_CLAUSE_213_INDEX:
                {
                    // Case 5
                    ASTAccCopyoutClauseNode result = (ASTAccCopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_EXIT_DATA_CLAUSE_214_INDEX:
                {
                    // Case 5
                    ASTAccDeleteClauseNode result = (ASTAccDeleteClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_ROUTINE_215_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccRoutineNode node = new ASTAccRoutineNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringRoutine = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringRoutine != null) node.hiddenLiteralStringRoutine.setParent(node);
                    node.accRoutineClauseList = (IASTListNode<ASTAccRoutineClauseListNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accRoutineClauseList != null) node.accRoutineClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_ROUTINE_216_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccRoutineNode node = new ASTAccRoutineNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringRoutine = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringRoutine != null) node.hiddenLiteralStringRoutine.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 2);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.name = (ASTIdentifierNode)valueStack.get(valueStackOffset + 3);
                    if (node.name != null) node.name.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 4);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    node.accRoutineClauseList = (IASTListNode<ASTAccRoutineClauseListNode>)valueStack.get(valueStackOffset + 5);
                    if (node.accRoutineClauseList != null) node.accRoutineClauseList.setParent(node);
                    return node;

                }
                case Production.ACC_ROUTINE_CLAUSE_LIST_217_INDEX:
                {
                    // Case 10
                    ASTAccRoutineClauseListNode node = new ASTAccRoutineClauseListNode();
                    node.accRoutineClause = (IAccRoutineClause)valueStack.get(valueStackOffset + 0);
                    if (node.accRoutineClause != null) node.accRoutineClause.setParent(node);
                    ASTListNode<ASTAccRoutineClauseListNode> list = new ASTListNode<ASTAccRoutineClauseListNode>();
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_ROUTINE_CLAUSE_LIST_218_INDEX:
                {
                    // Case 11
                    ASTAccRoutineClauseListNode node = new ASTAccRoutineClauseListNode();
                    node.accRoutineClause = (IAccRoutineClause)valueStack.get(valueStackOffset + 1);
                    if (node.accRoutineClause != null) node.accRoutineClause.setParent(node);
                    ASTListNode<ASTAccRoutineClauseListNode> list = (ASTListNode<ASTAccRoutineClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_ROUTINE_CLAUSE_LIST_219_INDEX:
                {
                    // Case 11
                    ASTAccRoutineClauseListNode node = new ASTAccRoutineClauseListNode();
                    node.hiddenLiteralStringComma = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringComma != null) node.hiddenLiteralStringComma.setParent(node);
                    node.accRoutineClause = (IAccRoutineClause)valueStack.get(valueStackOffset + 2);
                    if (node.accRoutineClause != null) node.accRoutineClause.setParent(node);
                    ASTListNode<ASTAccRoutineClauseListNode> list = (ASTListNode<ASTAccRoutineClauseListNode>)valueStack.get(valueStackOffset);
                    list.add(node);
                    node.setParent(list);
                    return list;

                }
                case Production.ACC_ROUTINE_CLAUSE_220_INDEX:
                {
                    // Case 5
                    ASTAccGangClauseNode result = (ASTAccGangClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_ROUTINE_CLAUSE_221_INDEX:
                {
                    // Case 5
                    ASTAccWorkerClauseNode result = (ASTAccWorkerClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_ROUTINE_CLAUSE_222_INDEX:
                {
                    // Case 5
                    ASTAccVectorClauseNode result = (ASTAccVectorClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_ROUTINE_CLAUSE_223_INDEX:
                {
                    // Cases 1 and 2
                    CAccSeqClause node = new CAccSeqClause();
                    node.hiddenLiteralStringSeq = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringSeq != null) node.hiddenLiteralStringSeq.setParent(node);
                    return node;

                }
                case Production.ACC_ROUTINE_CLAUSE_224_INDEX:
                {
                    // Cases 1 and 2
                    CAccBindClause node = new CAccBindClause();
                    node.hiddenLiteralStringBind = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringBind != null) node.hiddenLiteralStringBind.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.name = (Token)valueStack.get(valueStackOffset + 2);
                    if (node.name != null) node.name.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_ROUTINE_CLAUSE_225_INDEX:
                {
                    // Cases 1 and 2
                    CAccBindClause node = new CAccBindClause();
                    node.hiddenLiteralStringBind = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringBind != null) node.hiddenLiteralStringBind.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.name = (Token)valueStack.get(valueStackOffset + 2);
                    if (node.name != null) node.name.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_ROUTINE_CLAUSE_226_INDEX:
                {
                    // Cases 1 and 2
                    CAccNoHostClause node = new CAccNoHostClause();
                    node.hiddenLiteralStringNohost = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringNohost != null) node.hiddenLiteralStringNohost.setParent(node);
                    return node;

                }
                case Production.ACC_ATOMIC_227_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccAtomicNode node = new ASTAccAtomicNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringAtomic = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringAtomic != null) node.hiddenLiteralStringAtomic.setParent(node);
                    return node;

                }
                case Production.ACC_ATOMIC_228_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccAtomicNode node = new ASTAccAtomicNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringAtomic = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringAtomic != null) node.hiddenLiteralStringAtomic.setParent(node);
                    node.accAtomicClause = (IAccAtomicClause)valueStack.get(valueStackOffset + 2);
                    if (node.accAtomicClause != null) node.accAtomicClause.setParent(node);
                    return node;

                }
                case Production.ACC_ATOMIC_CLAUSE_229_INDEX:
                {
                    // Cases 1 and 2
                    CAccAtomicReadClause node = new CAccAtomicReadClause();
                    node.hiddenLiteralStringRead = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringRead != null) node.hiddenLiteralStringRead.setParent(node);
                    return node;

                }
                case Production.ACC_ATOMIC_CLAUSE_230_INDEX:
                {
                    // Cases 1 and 2
                    CAccAtomicWriteClause node = new CAccAtomicWriteClause();
                    node.hiddenLiteralStringWrite = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringWrite != null) node.hiddenLiteralStringWrite.setParent(node);
                    return node;

                }
                case Production.ACC_ATOMIC_CLAUSE_231_INDEX:
                {
                    // Cases 1 and 2
                    CAccAtomicUpdateClause node = new CAccAtomicUpdateClause();
                    node.hiddenLiteralStringUpdate = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringUpdate != null) node.hiddenLiteralStringUpdate.setParent(node);
                    return node;

                }
                case Production.ACC_ATOMIC_CLAUSE_232_INDEX:
                {
                    // Cases 1 and 2
                    CAccAtomicCaptureClause node = new CAccAtomicCaptureClause();
                    node.hiddenLiteralStringCapture = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringCapture != null) node.hiddenLiteralStringCapture.setParent(node);
                    return node;

                }
                case Production.ACC_COLLAPSE_CLAUSE_233_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccCollapseClauseNode node = new ASTAccCollapseClauseNode();
                    node.hiddenLiteralStringCollapse = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringCollapse != null) node.hiddenLiteralStringCollapse.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringLparen");
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.count = (IConstantExpression)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("count");
                    if (node.count != null) node.count.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringRparen");
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_GANG_CLAUSE_234_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccGangClauseNode node = new ASTAccGangClauseNode();
                    node.hiddenLiteralStringGang = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringGang != null) node.hiddenLiteralStringGang.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringLparen");
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.count = (IConstantExpression)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("count");
                    if (node.count != null) node.count.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringRparen");
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_GANG_CLAUSE_235_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccGangClauseNode node = new ASTAccGangClauseNode();
                    node.hiddenLiteralStringGang = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringGang != null) node.hiddenLiteralStringGang.setParent(node);
                    return node;

                }
                case Production.ACC_WORKER_CLAUSE_236_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccWorkerClauseNode node = new ASTAccWorkerClauseNode();
                    node.hiddenLiteralStringWorker = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringWorker != null) node.hiddenLiteralStringWorker.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringLparen");
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.count = (IConstantExpression)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("count");
                    if (node.count != null) node.count.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringRparen");
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_WORKER_CLAUSE_237_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccWorkerClauseNode node = new ASTAccWorkerClauseNode();
                    node.hiddenLiteralStringWorker = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringWorker != null) node.hiddenLiteralStringWorker.setParent(node);
                    return node;

                }
                case Production.ACC_VECTOR_CLAUSE_238_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccVectorClauseNode node = new ASTAccVectorClauseNode();
                    node.hiddenLiteralStringVector = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringVector != null) node.hiddenLiteralStringVector.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringLparen");
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.count = (IConstantExpression)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("count");
                    if (node.count != null) node.count.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringRparen");
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_VECTOR_CLAUSE_239_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccVectorClauseNode node = new ASTAccVectorClauseNode();
                    node.hiddenLiteralStringVector = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringVector != null) node.hiddenLiteralStringVector.setParent(node);
                    return node;

                }
                case Production.ACC_PRIVATE_CLAUSE_240_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccPrivateClauseNode node = new ASTAccPrivateClauseNode();
                    node.hiddenLiteralStringPrivate = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringPrivate != null) node.hiddenLiteralStringPrivate.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_FIRSTPRIVATE_CLAUSE_241_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccFirstprivateClauseNode node = new ASTAccFirstprivateClauseNode();
                    node.hiddenLiteralStringFirstprivate = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringFirstprivate != null) node.hiddenLiteralStringFirstprivate.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_REDUCTION_CLAUSE_242_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccReductionClauseNode node = new ASTAccReductionClauseNode();
                    node.hiddenLiteralStringReduction = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringReduction != null) node.hiddenLiteralStringReduction.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.operator = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 2)).get("operator");
                    if (node.operator != null) node.operator.setParent(node);
                    node.hiddenLiteralStringColon = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringColon != null) node.hiddenLiteralStringColon.setParent(node);
                    node.identifierList = (IASTListNode<ASTIdentifierNode>)valueStack.get(valueStackOffset + 4);
                    if (node.identifierList != null) node.identifierList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 5);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_IF_CLAUSE_243_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccIfClauseNode node = new ASTAccIfClauseNode();
                    node.hiddenLiteralStringIf = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringIf != null) node.hiddenLiteralStringIf.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.conditionalExpression = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.conditionalExpression != null) node.conditionalExpression.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_ASYNC_CLAUSE_244_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccAsyncClauseNode node = new ASTAccAsyncClauseNode();
                    node.hiddenLiteralStringAsync = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringAsync != null) node.hiddenLiteralStringAsync.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringLparen");
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.count = (IConstantExpression)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("count");
                    if (node.count != null) node.count.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringRparen");
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_ASYNC_CLAUSE_245_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccAsyncClauseNode node = new ASTAccAsyncClauseNode();
                    node.hiddenLiteralStringAsync = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringAsync != null) node.hiddenLiteralStringAsync.setParent(node);
                    return node;

                }
                case Production.ACC_WAIT_CLAUSE_246_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccWaitClauseNode node = new ASTAccWaitClauseNode();
                    node.hiddenLiteralStringWait = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringWait != null) node.hiddenLiteralStringWait.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.argList = (IASTListNode<IAssignmentExpression>)valueStack.get(valueStackOffset + 2);
                    if (node.argList != null) node.argList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_NUMGANGS_CLAUSE_247_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccNumgangsClauseNode node = new ASTAccNumgangsClauseNode();
                    node.hiddenLiteralStringNumUnderscoregangs = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringNumUnderscoregangs != null) node.hiddenLiteralStringNumUnderscoregangs.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringLparen");
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.count = (IConstantExpression)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("count");
                    if (node.count != null) node.count.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringRparen");
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_NUMWORKERS_CLAUSE_248_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccNumworkersClauseNode node = new ASTAccNumworkersClauseNode();
                    node.hiddenLiteralStringNumUnderscoreworkers = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringNumUnderscoreworkers != null) node.hiddenLiteralStringNumUnderscoreworkers.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringLparen");
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.count = (IConstantExpression)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("count");
                    if (node.count != null) node.count.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringRparen");
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_VECTORLENGTH_CLAUSE_249_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccVectorlengthClauseNode node = new ASTAccVectorlengthClauseNode();
                    node.hiddenLiteralStringVectorUnderscorelength = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringVectorUnderscorelength != null) node.hiddenLiteralStringVectorUnderscorelength.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringLparen");
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.count = (IConstantExpression)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("count");
                    if (node.count != null) node.count.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 1)).get("hiddenLiteralStringRparen");
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_DELETE_CLAUSE_250_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccDeleteClauseNode node = new ASTAccDeleteClauseNode();
                    node.hiddenLiteralStringDelete = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringDelete != null) node.hiddenLiteralStringDelete.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_COPY_CLAUSE_251_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccCopyClauseNode node = new ASTAccCopyClauseNode();
                    node.hiddenLiteralStringCopy = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringCopy != null) node.hiddenLiteralStringCopy.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_COPYIN_CLAUSE_252_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccCopyinClauseNode node = new ASTAccCopyinClauseNode();
                    node.hiddenLiteralStringCopyin = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringCopyin != null) node.hiddenLiteralStringCopyin.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_COPYOUT_CLAUSE_253_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccCopyoutClauseNode node = new ASTAccCopyoutClauseNode();
                    node.hiddenLiteralStringCopyout = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringCopyout != null) node.hiddenLiteralStringCopyout.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_CREATE_CLAUSE_254_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccCreateClauseNode node = new ASTAccCreateClauseNode();
                    node.hiddenLiteralStringCreate = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringCreate != null) node.hiddenLiteralStringCreate.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_PRESENT_CLAUSE_255_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccPresentClauseNode node = new ASTAccPresentClauseNode();
                    node.hiddenLiteralStringPresent = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringPresent != null) node.hiddenLiteralStringPresent.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_PRESENTORCOPY_CLAUSE_256_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccPresentorcopyClauseNode node = new ASTAccPresentorcopyClauseNode();
                    node.hiddenLiteralStringPresentUnderscoreorUnderscorecopy = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringPresentUnderscoreorUnderscorecopy != null) node.hiddenLiteralStringPresentUnderscoreorUnderscorecopy.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_PRESENTORCOPY_CLAUSE_257_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccPresentorcopyClauseNode node = new ASTAccPresentorcopyClauseNode();
                    node.hiddenLiteralStringPcopy = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringPcopy != null) node.hiddenLiteralStringPcopy.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_PRESENTORCOPYIN_CLAUSE_258_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccPresentorcopyinClauseNode node = new ASTAccPresentorcopyinClauseNode();
                    node.hiddenLiteralStringPresentUnderscoreorUnderscorecopyin = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringPresentUnderscoreorUnderscorecopyin != null) node.hiddenLiteralStringPresentUnderscoreorUnderscorecopyin.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_PRESENTORCOPYIN_CLAUSE_259_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccPresentorcopyinClauseNode node = new ASTAccPresentorcopyinClauseNode();
                    node.hiddenLiteralStringPcopyin = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringPcopyin != null) node.hiddenLiteralStringPcopyin.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_PRESENTORCOPYOUT_CLAUSE_260_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccPresentorcopyoutClauseNode node = new ASTAccPresentorcopyoutClauseNode();
                    node.hiddenLiteralStringPresentUnderscoreorUnderscorecopyout = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringPresentUnderscoreorUnderscorecopyout != null) node.hiddenLiteralStringPresentUnderscoreorUnderscorecopyout.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_PRESENTORCOPYOUT_CLAUSE_261_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccPresentorcopyoutClauseNode node = new ASTAccPresentorcopyoutClauseNode();
                    node.hiddenLiteralStringPcopyout = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringPcopyout != null) node.hiddenLiteralStringPcopyout.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_PRESENTORCREATE_CLAUSE_262_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccPresentorcreateClauseNode node = new ASTAccPresentorcreateClauseNode();
                    node.hiddenLiteralStringPresentUnderscoreorUnderscorecreate = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringPresentUnderscoreorUnderscorecreate != null) node.hiddenLiteralStringPresentUnderscoreorUnderscorecreate.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_PRESENTORCREATE_CLAUSE_263_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccPresentorcreateClauseNode node = new ASTAccPresentorcreateClauseNode();
                    node.hiddenLiteralStringPcreate = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringPcreate != null) node.hiddenLiteralStringPcreate.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_DEVICEPTR_CLAUSE_264_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccDeviceptrClauseNode node = new ASTAccDeviceptrClauseNode();
                    node.hiddenLiteralStringDeviceptr = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringDeviceptr != null) node.hiddenLiteralStringDeviceptr.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.identifierList = (IASTListNode<ASTIdentifierNode>)valueStack.get(valueStackOffset + 2);
                    if (node.identifierList != null) node.identifierList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_DEVICERESIDENT_CLAUSE_265_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccDeviceresidentClauseNode node = new ASTAccDeviceresidentClauseNode();
                    node.hiddenLiteralStringDeviceUnderscoreresident = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringDeviceUnderscoreresident != null) node.hiddenLiteralStringDeviceUnderscoreresident.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.identifierList = (IASTListNode<ASTIdentifierNode>)valueStack.get(valueStackOffset + 2);
                    if (node.identifierList != null) node.identifierList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_USEDEVICE_CLAUSE_266_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccUsedeviceClauseNode node = new ASTAccUsedeviceClauseNode();
                    node.hiddenLiteralStringUseUnderscoredevice = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringUseUnderscoredevice != null) node.hiddenLiteralStringUseUnderscoredevice.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.identifierList = (IASTListNode<ASTIdentifierNode>)valueStack.get(valueStackOffset + 2);
                    if (node.identifierList != null) node.identifierList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_SELF_CLAUSE_267_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccSelfClauseNode node = new ASTAccSelfClauseNode();
                    node.hiddenLiteralStringSelf = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringSelf != null) node.hiddenLiteralStringSelf.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_HOST_CLAUSE_268_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccHostClauseNode node = new ASTAccHostClauseNode();
                    node.hiddenLiteralStringHost = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringHost != null) node.hiddenLiteralStringHost.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_DEVICE_CLAUSE_269_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccDeviceClauseNode node = new ASTAccDeviceClauseNode();
                    node.hiddenLiteralStringDevice = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringDevice != null) node.hiddenLiteralStringDevice.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.accDataList = (IASTListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset + 2);
                    if (node.accDataList != null) node.accDataList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_DEFAULTNONE_CLAUSE_270_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccDefaultnoneClauseNode node = new ASTAccDefaultnoneClauseNode();
                    node.hiddenLiteralStringDefault = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringDefault != null) node.hiddenLiteralStringDefault.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.hiddenLiteralStringNone = (Token)valueStack.get(valueStackOffset + 2);
                    if (node.hiddenLiteralStringNone != null) node.hiddenLiteralStringNone.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_LINK_CLAUSE_271_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccLinkClauseNode node = new ASTAccLinkClauseNode();
                    node.hiddenLiteralStringLink = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringLink != null) node.hiddenLiteralStringLink.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.identifierList = (IASTListNode<ASTIdentifierNode>)valueStack.get(valueStackOffset + 2);
                    if (node.identifierList != null) node.identifierList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_TILE_CLAUSE_272_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccTileClauseNode node = new ASTAccTileClauseNode();
                    node.hiddenLiteralStringTile = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringTile != null) node.hiddenLiteralStringTile.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.list = (IASTListNode<IAssignmentExpression>)valueStack.get(valueStackOffset + 2);
                    if (node.list != null) node.list.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.ACC_COUNT_273_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("hiddenLiteralStringLparen", (Token)valueStack.get(valueStackOffset + 0));
                    node.put("count", (IConstantExpression)valueStack.get(valueStackOffset + 1));
                    node.put("hiddenLiteralStringRparen", (Token)valueStack.get(valueStackOffset + 2));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("hiddenLiteralStringLparen")));
                    embeddedList.add((IASTNode)(node.get("count")));
                    embeddedList.add((IASTNode)(node.get("hiddenLiteralStringRparen")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_274_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_275_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_276_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_277_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_278_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_279_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_280_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_281_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_282_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_DATA_LIST_283_INDEX:
                {
                    // Case 7 with separator
                    ASTSeparatedListNode<ASTAccDataItemNode> list = new ASTSeparatedListNode<ASTAccDataItemNode>();
                    ASTAccDataItemNode elt = (ASTAccDataItemNode)valueStack.get(valueStackOffset + 0);
                    list.add(null, elt);
                    if (elt != null) elt.setParent(list);
                    return list;

                }
                case Production.ACC_DATA_LIST_284_INDEX:
                {
                    // Case 8 with separator
                    ASTSeparatedListNode<ASTAccDataItemNode> list = (ASTSeparatedListNode<ASTAccDataItemNode>)valueStack.get(valueStackOffset);
                    Token token = (Token)valueStack.get(valueStackOffset + 1);
                    ASTAccDataItemNode elt = (ASTAccDataItemNode)valueStack.get(valueStackOffset + 2);
                    list.add(token, elt);
                    token.setParent(list);
                    if (elt != null) elt.setParent(list);
                    return list;

                }
                case Production.ACC_DATA_ITEM_285_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccDataItemNode node = new ASTAccDataItemNode();
                    node.identifier = (ASTIdentifierNode)valueStack.get(valueStackOffset + 0);
                    if (node.identifier != null) node.identifier.setParent(node);
                    return node;

                }
                case Production.ACC_DATA_ITEM_286_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccDataItemNode node = new ASTAccDataItemNode();
                    node.identifier = (ASTIdentifierNode)valueStack.get(valueStackOffset + 0);
                    if (node.identifier != null) node.identifier.setParent(node);
                    node.hiddenLiteralStringLbracket = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLbracket != null) node.hiddenLiteralStringLbracket.setParent(node);
                    node.lowerBound = (IConstantExpression)valueStack.get(valueStackOffset + 2);
                    if (node.lowerBound != null) node.lowerBound.setParent(node);
                    node.hiddenLiteralStringColon = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringColon != null) node.hiddenLiteralStringColon.setParent(node);
                    node.count = (IConstantExpression)valueStack.get(valueStackOffset + 4);
                    if (node.count != null) node.count.setParent(node);
                    node.hiddenLiteralStringRbracket = (Token)valueStack.get(valueStackOffset + 5);
                    if (node.hiddenLiteralStringRbracket != null) node.hiddenLiteralStringRbracket.setParent(node);
                    return node;

                }
                case Production.PRIMARY_EXPRESSION_287_INDEX:
                {
                    // Cases 1 and 2
                    CIdentifierExpression node = new CIdentifierExpression();
                    node.identifier = (ASTIdentifierNode)valueStack.get(valueStackOffset + 0);
                    if (node.identifier != null) node.identifier.setParent(node);
                    return node;

                }
                case Production.PRIMARY_EXPRESSION_288_INDEX:
                {
                    // Case 5
                    CConstantExpression result = (CConstantExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.PRIMARY_EXPRESSION_289_INDEX:
                {
                    // Cases 1 and 2
                    CStringLiteralExpression node = new CStringLiteralExpression();
                    node.literals = (IASTListNode<Token>)valueStack.get(valueStackOffset + 0);
                    if (node.literals != null) node.literals.setParent(node);
                    return node;

                }
                case Production.PRIMARY_EXPRESSION_290_INDEX:
                {
                    // Case 5
                    ASTExpressionNode result = (ASTExpressionNode)valueStack.get(valueStackOffset + 1);
                    result.prependToken((Token)valueStack.get(valueStackOffset + 0));
                    result.appendToken((Token)valueStack.get(valueStackOffset + 2));
                    return result;

                }
                case Production.POSTFIX_EXPRESSION_291_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.POSTFIX_EXPRESSION_292_INDEX:
                {
                    // Cases 1 and 2
                    CArrayAccessExpression node = new CArrayAccessExpression();
                    node.array = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.array != null) node.array.setParent(node);
                    node.hiddenLiteralStringLbracket = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLbracket != null) node.hiddenLiteralStringLbracket.setParent(node);
                    node.subscript = (ASTExpressionNode)valueStack.get(valueStackOffset + 2);
                    if (node.subscript != null) node.subscript.setParent(node);
                    node.hiddenLiteralStringRbracket = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRbracket != null) node.hiddenLiteralStringRbracket.setParent(node);
                    return node;

                }
                case Production.POSTFIX_EXPRESSION_293_INDEX:
                {
                    // Cases 1 and 2
                    CFunctionCallExpression node = new CFunctionCallExpression();
                    node.function = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.function != null) node.function.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.argumentExpressionList = (IASTListNode<IAssignmentExpression>)valueStack.get(valueStackOffset + 2);
                    if (node.argumentExpressionList != null) node.argumentExpressionList.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.POSTFIX_EXPRESSION_294_INDEX:
                {
                    // Cases 1 and 2
                    CFunctionCallExpression node = new CFunctionCallExpression();
                    node.function = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.function != null) node.function.setParent(node);
                    node.hiddenLiteralStringLparen = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLparen != null) node.hiddenLiteralStringLparen.setParent(node);
                    node.hiddenLiteralStringRparen = (Token)valueStack.get(valueStackOffset + 2);
                    if (node.hiddenLiteralStringRparen != null) node.hiddenLiteralStringRparen.setParent(node);
                    return node;

                }
                case Production.POSTFIX_EXPRESSION_295_INDEX:
                {
                    // Cases 1 and 2
                    CElementAccessExpression node = new CElementAccessExpression();
                    node.structure = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.structure != null) node.structure.setParent(node);
                    node.hiddenLiteralStringPeriod = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringPeriod != null) node.hiddenLiteralStringPeriod.setParent(node);
                    node.identifier = (ASTIdentifierNode)valueStack.get(valueStackOffset + 2);
                    if (node.identifier != null) node.identifier.setParent(node);
                    return node;

                }
                case Production.POSTFIX_EXPRESSION_296_INDEX:
                {
                    // Cases 1 and 2
                    CElementAccessExpression node = new CElementAccessExpression();
                    node.structure = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.structure != null) node.structure.setParent(node);
                    node.arrow = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.arrow != null) node.arrow.setParent(node);
                    node.identifier = (ASTIdentifierNode)valueStack.get(valueStackOffset + 2);
                    if (node.identifier != null) node.identifier.setParent(node);
                    return node;

                }
                case Production.POSTFIX_EXPRESSION_297_INDEX:
                {
                    // Cases 1 and 2
                    CPostfixUnaryExpression node = new CPostfixUnaryExpression();
                    node.subexpression = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.subexpression != null) node.subexpression.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    return node;

                }
                case Production.POSTFIX_EXPRESSION_298_INDEX:
                {
                    // Cases 1 and 2
                    CPostfixUnaryExpression node = new CPostfixUnaryExpression();
                    node.subexpression = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.subexpression != null) node.subexpression.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    return node;

                }
                case Production.ARGUMENT_EXPRESSION_LIST_299_INDEX:
                {
                    // Case 7 with separator
                    ASTSeparatedListNode<IAssignmentExpression> list = new ASTSeparatedListNode<IAssignmentExpression>();
                    IAssignmentExpression elt = (IAssignmentExpression)valueStack.get(valueStackOffset + 0);
                    list.add(null, elt);
                    if (elt != null) elt.setParent(list);
                    return list;

                }
                case Production.ARGUMENT_EXPRESSION_LIST_300_INDEX:
                {
                    // Case 8 with separator
                    ASTSeparatedListNode<IAssignmentExpression> list = (ASTSeparatedListNode<IAssignmentExpression>)valueStack.get(valueStackOffset);
                    Token token = (Token)valueStack.get(valueStackOffset + 1);
                    IAssignmentExpression elt = (IAssignmentExpression)valueStack.get(valueStackOffset + 2);
                    list.add(token, elt);
                    token.setParent(list);
                    if (elt != null) elt.setParent(list);
                    return list;

                }
                case Production.UNARY_EXPRESSION_301_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.UNARY_EXPRESSION_302_INDEX:
                {
                    // Cases 1 and 2
                    CPrefixUnaryExpression node = new CPrefixUnaryExpression();
                    node.operator = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.operator != null) node.operator.setParent(node);
                    node.subexpression = (ICExpression)valueStack.get(valueStackOffset + 1);
                    if (node.subexpression != null) node.subexpression.setParent(node);
                    return node;

                }
                case Production.UNARY_EXPRESSION_303_INDEX:
                {
                    // Cases 1 and 2
                    CPrefixUnaryExpression node = new CPrefixUnaryExpression();
                    node.operator = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.operator != null) node.operator.setParent(node);
                    node.subexpression = (ICExpression)valueStack.get(valueStackOffset + 1);
                    if (node.subexpression != null) node.subexpression.setParent(node);
                    return node;

                }
                case Production.UNARY_EXPRESSION_304_INDEX:
                {
                    // Cases 1 and 2
                    CPrefixUnaryExpression node = new CPrefixUnaryExpression();
                    node.operator = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 0)).get("operator");
                    if (node.operator != null) node.operator.setParent(node);
                    node.subexpression = (ICExpression)valueStack.get(valueStackOffset + 1);
                    if (node.subexpression != null) node.subexpression.setParent(node);
                    return node;

                }
                case Production.UNARY_EXPRESSION_305_INDEX:
                {
                    // Cases 1 and 2
                    CSizeofExpression node = new CSizeofExpression();
                    node.hiddenLiteralStringSizeof = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringSizeof != null) node.hiddenLiteralStringSizeof.setParent(node);
                    node.expression = (ICExpression)valueStack.get(valueStackOffset + 1);
                    if (node.expression != null) node.expression.setParent(node);
                    return node;

                }
                case Production.UNARY_OPERATOR_306_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.UNARY_OPERATOR_307_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.UNARY_OPERATOR_308_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.UNARY_OPERATOR_309_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.UNARY_OPERATOR_310_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.UNARY_OPERATOR_311_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.CAST_EXPRESSION_312_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.MULTIPLICATIVE_EXPRESSION_313_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.MULTIPLICATIVE_EXPRESSION_314_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.MULTIPLICATIVE_EXPRESSION_315_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.MULTIPLICATIVE_EXPRESSION_316_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.ADDITIVE_EXPRESSION_317_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ADDITIVE_EXPRESSION_318_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.ADDITIVE_EXPRESSION_319_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.SHIFT_EXPRESSION_320_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.SHIFT_EXPRESSION_321_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.SHIFT_EXPRESSION_322_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.RELATIONAL_EXPRESSION_323_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.RELATIONAL_EXPRESSION_324_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.RELATIONAL_EXPRESSION_325_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.RELATIONAL_EXPRESSION_326_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.RELATIONAL_EXPRESSION_327_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.EQUALITY_EXPRESSION_328_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.EQUALITY_EXPRESSION_329_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.EQUALITY_EXPRESSION_330_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.AND_EXPRESSION_331_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.AND_EXPRESSION_332_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.EXCLUSIVE_OR_EXPRESSION_333_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.EXCLUSIVE_OR_EXPRESSION_334_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.INCLUSIVE_OR_EXPRESSION_335_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.INCLUSIVE_OR_EXPRESSION_336_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.LOGICAL_AND_EXPRESSION_337_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.LOGICAL_AND_EXPRESSION_338_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.LOGICAL_OR_EXPRESSION_339_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.LOGICAL_OR_EXPRESSION_340_INDEX:
                {
                    // Cases 1 and 2
                    CBinaryExpression node = new CBinaryExpression();
                    node.lhs = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.lhs != null) node.lhs.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    node.rhs = (ICExpression)valueStack.get(valueStackOffset + 2);
                    if (node.rhs != null) node.rhs.setParent(node);
                    return node;

                }
                case Production.CONDITIONAL_EXPRESSION_341_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.CONDITIONAL_EXPRESSION_342_INDEX:
                {
                    // Cases 1 and 2
                    CTernaryExpression node = new CTernaryExpression();
                    node.testExpression = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.testExpression != null) node.testExpression.setParent(node);
                    node.hiddenLiteralStringQuestion = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringQuestion != null) node.hiddenLiteralStringQuestion.setParent(node);
                    node.thenExpression = (ASTExpressionNode)valueStack.get(valueStackOffset + 2);
                    if (node.thenExpression != null) node.thenExpression.setParent(node);
                    node.hiddenLiteralStringColon = (Token)valueStack.get(valueStackOffset + 3);
                    if (node.hiddenLiteralStringColon != null) node.hiddenLiteralStringColon.setParent(node);
                    node.elseExpression = (ICExpression)valueStack.get(valueStackOffset + 4);
                    if (node.elseExpression != null) node.elseExpression.setParent(node);
                    return node;

                }
                case Production.CONSTANT_343_INDEX:
                {
                    // Cases 1 and 2
                    CConstantExpression node = new CConstantExpression();
                    node.constant = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.constant != null) node.constant.setParent(node);
                    return node;

                }
                case Production.CONSTANT_344_INDEX:
                {
                    // Cases 1 and 2
                    CConstantExpression node = new CConstantExpression();
                    node.constant = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.constant != null) node.constant.setParent(node);
                    return node;

                }
                case Production.CONSTANT_345_INDEX:
                {
                    // Cases 1 and 2
                    CConstantExpression node = new CConstantExpression();
                    node.constant = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.constant != null) node.constant.setParent(node);
                    return node;

                }
                case Production.EXPRESSION_346_INDEX:
                {
                    // Cases 1 and 2
                    ASTExpressionNode node = new ASTExpressionNode();
                    node.conditionalExpression = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.conditionalExpression != null) node.conditionalExpression.setParent(node);
                    return node;

                }
                case Production.ASSIGNMENT_EXPRESSION_347_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.CONSTANT_EXPRESSION_348_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.IDENTIFIER_LIST_349_INDEX:
                {
                    // Case 7 with separator
                    ASTSeparatedListNode<ASTIdentifierNode> list = new ASTSeparatedListNode<ASTIdentifierNode>();
                    ASTIdentifierNode elt = (ASTIdentifierNode)valueStack.get(valueStackOffset + 0);
                    list.add(null, elt);
                    if (elt != null) elt.setParent(list);
                    return list;

                }
                case Production.IDENTIFIER_LIST_350_INDEX:
                {
                    // Case 8 with separator
                    ASTSeparatedListNode<ASTIdentifierNode> list = (ASTSeparatedListNode<ASTIdentifierNode>)valueStack.get(valueStackOffset);
                    Token token = (Token)valueStack.get(valueStackOffset + 1);
                    ASTIdentifierNode elt = (ASTIdentifierNode)valueStack.get(valueStackOffset + 2);
                    list.add(token, elt);
                    token.setParent(list);
                    if (elt != null) elt.setParent(list);
                    return list;

                }
                case Production.IDENTIFIER_351_INDEX:
                {
                    // Cases 1 and 2
                    ASTIdentifierNode node = new ASTIdentifierNode();
                    node.identifier = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.identifier != null) node.identifier.setParent(node);
                    return node;

                }
                case Production.IDENTIFIER_352_INDEX:
                {
                    // Cases 1 and 2
                    ASTIdentifierNode node = new ASTIdentifierNode();
                    node.identifier = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.identifier != null) node.identifier.setParent(node);
                    return node;

                }
                case Production.STRING_LITERAL_TERMINAL_LIST_353_INDEX:
                {
                    // Case 8
                    IASTListNode<Token> list = (IASTListNode<Token>)valueStack.get(valueStackOffset);
                    Token elt = (Token)valueStack.get(valueStackOffset + 1);
                    list.add(elt);
                    if (elt != null) elt.setParent(list);
                    return list;

                }
                case Production.STRING_LITERAL_TERMINAL_LIST_354_INDEX:
                {
                    // Case 7
                    IASTListNode<Token> list = new ASTListNode<Token>();
                    Token elt = (Token)valueStack.get(valueStackOffset + 0);
                    list.add(elt);
                    if (elt != null) elt.setParent(list);
                    return list;

                }
                default:
                    throw new IllegalStateException();
            }
        }
}


@SuppressWarnings("all")
final class ParsingTables
{
    // Constants used for accessing both the ACTION table and the error recovery table
    public static final int ACTION_MASK   = 0xC000;  // 1100 0000 0000 0000
    public static final int VALUE_MASK    = 0x3FFF;  // 0011 1111 1111 1111

    // Constants used for accessing the ACTION table
    public static final int SHIFT_ACTION  = 0x8000;  // 1000 0000 0000 0000
    public static final int REDUCE_ACTION = 0x4000;  // 0100 0000 0000 0000
    public static final int ACCEPT_ACTION = 0xC000;  // 1100 0000 0000 0000

    // Constants used for accessing the error recovery table
    public static final int DISCARD_STATE_ACTION    = 0x0000;  // 0000 0000 0000 0000
    public static final int DISCARD_TERMINAL_ACTION = 0x8000;  // 1000 0000 0000 0000
    public static final int RECOVER_ACTION          = 0x4000;  // 0100 0000 0000 0000

    private static ParsingTables instance = null;

    public static ParsingTables getInstance()
    {
        if (instance == null)
            instance = new ParsingTables();
        return instance;
    }

    public int getActionCode(int state, Token lookahead)
    {
        return ActionTable.getActionCode(state, lookahead);
    }

    public int getActionCode(int state, int lookaheadTokenIndex)
    {
        return ActionTable.get(state, lookaheadTokenIndex);
    }

    public int getGoTo(int state, Nonterminal nonterminal)
    {
        return GoToTable.getGoTo(state, nonterminal);
    }

    public int getRecoveryCode(int state, Token lookahead)
    {
        return RecoveryTable.getRecoveryCode(state, lookahead);
    }

    /**
     * The ACTION table.
     * <p>
     * The ACTION table maps a state and an input symbol to one of four
     * actions: shift, reduce, accept, or error.
     */
    protected static final class ActionTable
    {
        /**
         * Returns the action the parser should take if it is in the given state
         * and has the given symbol as its lookahead.
         * <p>
         * The result value should be interpreted as follows:
         * <ul>
         *   <li> If <code>result & ACTION_MASK == SHIFT_ACTION</code>,
         *        shift the terminal and go to state number
         *        <code>result & VALUE_MASK</code>.
         *   <li> If <code>result & ACTION_MASK == REDUCE_ACTION</code>,
         *        reduce by production number <code>result & VALUE_MASK</code>.
         *   <li> If <code>result & ACTION_MASK == ACCEPT_ACTION</code>,
         *        parsing has completed successfully.
         *   <li> Otherwise, a syntax error has been found.
         * </ul>
         *
         * @return a code for the action to take (see above)
         */
        protected static int getActionCode(int state, Token lookahead)
        {
            assert 0 <= state && state < OpenACCParser.NUM_STATES;
            assert lookahead != null;

            return get(state, lookahead.getTerminal().getIndex());
        }

        protected static final int[] rowmap = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 0, 14, 15, 16, 0, 0, 17, 0, 18, 1, 1, 2, 19, 2, 20, 21, 0, 1, 22, 23, 24, 2, 25, 26, 27, 28, 29, 3, 4, 30, 31, 32, 5, 33, 6, 34, 35, 7, 8, 9, 10, 11, 36, 12, 37, 13, 38, 39, 14, 40, 41, 42, 15, 16, 43, 17, 19, 44, 45, 46, 20, 47, 48, 49, 50, 51, 52, 53, 54, 55, 21, 56, 57, 58, 59, 60, 3, 22, 61, 62, 63, 23, 24, 64, 65, 66, 67, 68, 25, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 26, 84, 85, 4, 86, 87, 88, 89, 90, 91, 27, 92, 93, 94, 95, 96, 97, 28, 98, 99, 100, 29, 30, 101, 102, 31, 103, 104, 105, 106, 32, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 33, 118, 119, 120, 121, 122, 123, 34, 124, 125, 126, 35, 127, 128, 129, 130, 3, 4, 131, 1, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 132, 2, 18, 19, 21, 22, 0, 133, 134, 36, 135, 45, 84, 121, 136, 0, 1, 2, 3, 37, 38, 4, 137, 39, 5, 40, 41, 6, 42, 7, 138, 43, 8, 139, 140, 141, 9, 44, 47, 48, 10, 142, 11, 12, 49, 143, 13, 144, 5, 50, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 51, 23, 24, 155, 6, 25, 156, 7, 0, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 52, 190, 191, 192, 1, 0, 193, 53, 194, 26, 195, 196, 197, 198, 8, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 27, 2, 230, 0, 14, 15, 16, 17, 231, 18, 19, 20, 21, 25, 26, 27, 28, 29, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 22, 54, 55, 56, 57, 243, 244, 58, 59, 28, 245, 29, 60, 61, 62, 63, 246, 64, 65, 66, 67, 247, 68, 23, 24, 248, 69, 70, 71, 136, 249, 72, 73, 74, 250, 30, 9, 251, 252, 253, 254, 255, 256, 257, 258, 259, 31, 260, 10, 261, 11, 30, 262, 12, 14, 15, 16, 17, 18, 19, 20, 21, 22, 263, 264, 265, 266, 12, 267, 268, 32, 33, 3, 31, 137, 75, 32, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 24, 289, 12, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 76, 312, 313, 314, 315, 316, 317, 318, 319, 33, 320, 321, 322, 323, 324, 325, 326, 327, 25, 328, 77, 329, 330, 331, 332, 333, 78, 334, 335, 14, 336 };
    protected static final int[] columnmap = { 0, 1, 0, 2, 0, 3, 4, 5, 6, 7, 8, 9, 0, 6, 10, 6, 11, 5, 12, 13, 14, 10, 15, 16, 17, 18, 19, 14, 20, 21, 22, 23, 24, 25, 26, 27, 28, 12, 29, 19, 30, 6, 28, 31, 20, 32, 33, 26, 34, 32, 27, 35, 30, 30, 36, 37, 20, 38, 2, 39, 40, 40, 0, 41, 42, 43, 42, 33, 44, 21, 45, 46, 34, 4, 35, 36, 38, 47, 48, 49, 50, 51, 52, 53, 37, 54, 55, 56, 41, 46, 57, 42, 58, 59, 60, 43, 61, 62, 63 };

    public static int get(int row, int col)
    {
        if (isErrorEntry(row, col))
            return 0;
        else if (columnmap[col] % 2 == 0)
            return lookupValue(rowmap[row], columnmap[col]/2) >>> 16;
        else
            return lookupValue(rowmap[row], columnmap[col]/2) & 0xFFFF;
    }

    protected static boolean isErrorEntry(int row, int col)
    {
        final int INT_BITS = 32;
        int sigmapRow = row;

        int sigmapCol = col / INT_BITS;
        int bitNumberFromLeft = col % INT_BITS;
        int sigmapMask = 0x1 << (INT_BITS - bitNumberFromLeft - 1);

        return (lookupSigmap(sigmapRow, sigmapCol) & sigmapMask) == 0;
    }

    protected static int[][] sigmap = null;

    protected static void sigmapInit()
    {
        try
        {
            final int rows = 523;
            final int cols = 4;
            final int compressedBytes = 888;
            final int uncompressedBytes = 8369;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWUtu2zAQHU2UTw0vGAEFjK7UwosW6KLICdQgAYKeoMugi6" +
                "5zBCXNAYxeIRcIkgvYzUV6hOQGFWkrJN8wpmm5hhNECwEPHM6M" +
                "+Jl5M8qpffbMOyf/ee6YskvqvxmSGlG91Hz5KBdcmndZUjXFW2" +
                "prMlaUcz6a6bPiWoTNeEbtuF359mncrDP9rrT8jvo1GQ8yOmzk" +
                "q4B8/qHkL1RrIz0y8leTv4Ov2eH29VS+VJxNfd6rpfv6exXx4j" +
                "gwf7X7BfoR7+r1K+z6xnBsfqr+mH+peN3+dl2/VP+C54XtfXD3" +
                "35z3m9u7+9+T/T/b1980HhwNyl7Odf6Rg/N3jH17n9aN8ftWr7" +
                "+ux008oMf7j+vZ3OoLGy9iOL4/afpS7Zl4Vth49tKx+P6Dq7t7" +
                "da6a8V5QvtbBP7P3Q8d3duL7we3dQzHZb+J7bxwYT8Vi/xPnp/" +
                "qTag/1x86vyX+FzX+IU+XXrW/duLP/5jxP92cZe3lJnLHlJzHc" +
                "db7U1/IpBj7GT/AfcQDnzn9uGPM58tlV2HPzfZAPOBjz/87Nlc" +
                "cPzqrvn/nHu5NPfDwsl8AOpzamTyvqNyTdjBts/FGP42dV1Wcu" +
                "Huejf4hRHu2j/Km2w6pq7cX0N/7O5I+GFNZHrr6o/Zm+9vsXWs" +
                "+fFlt/aLiI/3K99PoXJ559Vz/5+rGGkvrQX38/kG9hfSb5Z8PH" +
                "Liwf2zSM/sbjG/I1v/5EeeQrrLHy61M3Tpr8UDh85xVvFM4137" +
                "xw+OZ/xiJ/w7j2bx6fJOh3qJMmN5HSU99TYL7IHzAuzrdZH4fP" +
                "xDDwG9ZYAT9K0feKnzUW8VbzTZrPPz3SE+oJMuDDp8cDfFXOf0" +
                "FY9p+66UP+GeMX6fwmjQ+2uP9EPsf5OL5qf7vr8/kcjiMfx/Xv" +
                "6g/2G7vqk/06vL9+/w752qrvg+Rz/rjsT/n+Ij9g6FeJfA79Ks" +
                "EvIX9XgRpwk3BsfYP9Dyf+M/RDxP6E4r/4B+RgyB+B/oT3xPg/" +
                "5ofdA1NPv239XQgXDr6JyOP/BZyfam8RfQlY9xMeZv2EcbD+9D" +
                "GeF6yvg7+22I1faf0JrHexPhcY+gein5GOvf5CrJ5GjP2AmLyo" +
                "z6HfE6t3hXxdn2v5Oh8t9L8izO/mxL9AvOhUTwBepp/p+hOLb/" +
                "F+ieQXXjyLrEcwv7PN7yH+PO/+0D+QVf0M");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupSigmap(int row, int col)
    {
        return sigmap[row][col];
    }

    protected static int[][] value = null;

    protected static void valueInit()
    {
        try
        {
            final int rows = 152;
            final int cols = 32;
            final int compressedBytes = 4035;
            final int uncompressedBytes = 19457;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNqlnGuQXEUVgBcEVBRUAiEPQgIRQhYSII/NRgqqb1KxolQqFR" +
                "MUIdSGfbHsUv5Ro7A++kY0EpCQkCAYah/ZF4IhICCB3ewuTymg" +
                "Sn9IIQjEIgpUKKSw4I+Wjz59+vQ53bfvzoRM1zl9Tp/HN33nzp" +
                "3ZyUzyPdn6/D71eV2nF+tT8nv1Xn2RnqYn6yn6AT1bL9df1nP1" +
                "BXqPPi3fq2/UC/USvVTP0FP1qfk9erpelu/Tp+tJ+W/0yfrEfE" +
                "hv0ZfomXlPfn+2Xq+oqdG1Neam51u9qKYmf8zIA7ozW2/8+/RX" +
                "amqy5dnymhr1aV0HOdlyfZPh32KiWj/IfFP1bRMj/s36Bn2F7b" +
                "nM1myGHnqdsbbk34EVu7rCSK2NzLd6Ea6b1U4jeY295d/NLjP8" +
                "z8L+bUezf6OvNPKwEb9/G3N8a19vpMHyTX2+EfhGzP6N3gCreo" +
                "VujfePN8O/zO9/KQx1PFggeq/NAP4jLnuukT2QpW80FvEPGr/B" +
                "HrOl2MXsf6negp7vVguz2f9Su/+l+oDN7LS+4RuvHoY61mTX53" +
                "OyescE/u9qxA2yzNpCv4eDWb3j12PU8OvJC4W14dfzquHXZ2uy" +
                "Neb4fw4ff7t6kZsf5eNvH6kfAZ/37x//NSiGv0bc3zWp808foK" +
                "jPuzS71PA/QZ7Y/76a4EYx2j88/rQOYvhUvcGttFJ2rq3+MfAp" +
                "H/ZvrLXZWsM/Lty/5T8W7t/GUvtfi6LX5bnP25itnXD/az1/Zb" +
                "bS8E9wz7+VxDfPv8dDvo255x/z9RCs29g60GbuyX9i+Cv1ivxn" +
                "uja/UW81599KvcOcfyv1gfynlO/4q7JVNTXjdJx+Lo7/MJ//lr" +
                "/KemL/7vivQjH8VeLRwuzE/inf8VWm8l+oo9z+VX4CXH/s/kfk" +
                "/iGPrz/5dHH8lbn+qPwWw1fm+afsykYjK4zUgm/2r+zzT5nzH/" +
                "p0mvzTHX91ttoc/xPt7uf4xw/2vz86/1aHzz+//9Uutg60zC7e" +
                "YP/5rRQFvrGnGv7RdP7lyu9/ND7/xP5X8+Nv9GZr2+sfPP5w/u" +
                "Vb6fyDx9/oHXz+6c5NR+Vfc/u/GIY6xpC3gSfu61iw+4tlDPaf" +
                "b6cIRcuEtTn+F/OqYd5u7Elm/59MHKvxmgluuP/yW/542fGH26" +
                "bjReZHhv+pRO5T1fHz4SR/ZCJ+cGQnG/5nbE1wxumnq+NnM1Lx" +
                "9GqRr7+YnaTPUQf1WYXIMxPQ5wj+9CR/etX7n2b2/9tE7lerPP" +
                "6jyeM/Vpnvnn+nGP6Viev/s/L5p79Xev2/ytWtE503ZldNdP1n" +
                "vn41O1m/pE6y/l/16/omM+P7r7f1Oz7zT/ov+h/w/sN68P7roH" +
                "7L2ofyYb1Zv6j/bOw/wPPf7X+8fP+604h/rcpONfs/I96/4f8z" +
                "2H8/7z98/aHzn/bvrj9PlF9/ouM/xfDvTdzX/6RWi49/Oiv/Y9" +
                "Xnn3knq/Ykcv+bWk3wk1nZFRPxZXQT9Ph1Ivd/enY1fP1Qcv8H" +
                "q93/puMM/++JDkcf0fXvyWr59hi+k+hw7BE9/586LP7fEh3OO6" +
                "L9P31Y/LcTHeYfEf+Zw+K/lehwwREd/2cPi38rDLJd7vO0Tisy" +
                "D/lgc1RW54c4xrqUvw0G2YK/TUZknuNv4/ywOv+AY6xL+TtgkC" +
                "34O2RE5jn+Ds4Pq/N/cYx1Kf82GGQL/m0yIvMc/zbOD6vzf3OM" +
                "dSn/lzDIdh0uo3VaCfMoV/rS45jUJfw7YJDt+JfTOq2EeZQrfe" +
                "lxTOrCM+JVIy+prTDg9d/kbXUR8/qP6yZiX/8hBgOPP7z+m8gh" +
                "8On1n6vRQo914v0f8rfDcPztgr/dRYhvPcHfbvjbA/52z99OHu" +
                "siP1sEom6GkS0CT91MEVrHCMZgUBR8yCFPVqOFHutsUYG/GETd" +
                "BSNbDJ66iyK0jhGMwaAo+JBDnqxGCz3W2eICvw5E3QkjqwNP3U" +
                "kRWscIxmBQFHzIIU9Wo4Ue66yuwF8Com6HkS0BT91OEVrHCMZg" +
                "UBR8yCFPVqOFHmvIiPgLQNQW+PwvWwAeff4EHr3/BNvG/Ptv5y" +
                "8zOU+SZ1cu8Wd24v03ZCTP/50w3Pm/U5z/O12Ezn/rifN/pzn/" +
                "dwbn/06//53ksU6c/wtB1INoZwtlhD2wZYyj2UL9e/LCjOS70o" +
                "Xx++/smzDUA2SJXOHFMbkCGu0ykZoz7eePrqf6SMFf4R+hhcO+" +
                "/jibs+RK7MkO4Uya+rC4x/9h+vtLHJ3C31/h+x/6+wtq+fGf+B" +
                "Y+/rYS+bvVbuKD5c+/3Tj8+bcbM/35t9vwdyPf2AEf1jGXden1" +
                "v1t1e3634Hfj8PxuzPT8bsPvdvzuiN9NvViX8gfUgOcPCP4ADs" +
                "8fwEzPHzD8AccfiPgD1It1Kb9X9Xp+r+D34vD8Xsz0/F7D73X8" +
                "3ojfS71Yl77/uUfdw7Z7/zGA62FMephLvlwPY1KX7L9Ldfn9d4" +
                "n9d+Hw++/CTL//LrP/Lrf/rmj/XdSLdenxH1JDnj8k+EM4PH8I" +
                "Mz1/yPCHHH8o4g9RL9al/F/B5x/I9//+Yfj+358cX37+A3y9TB" +
                "/SHyBfrwv5lT//Efwe1eP33yP234PD778HM/3+e8z+e9z+e6L9" +
                "91Av1qX8PtXn+X2C34fD8/sw0/P7DL/P8fsifh/1Yl3K71f9nt" +
                "8v+P04PL8fMz2/3/D7Hb8/4vdTL9al/F1ql+fvEvxdODx/F2Z6" +
                "/i7D3+X4uyQ/fxnWMZd1KX9QDXr+oOAP4vD8Qcz0/EHDH3T8wW" +
                "j/g9SLdSl/X/j6B37l1z+bdchq4O8jvtoXzqWvf/s8fzTij1bF" +
                "H7X8Uccf9fzRcC7lj3r+SMQfqYo/Yvkjjj/i+SPhXMof8fz9EX" +
                "9/Vfz9lr/f8fd7/v5wLuXv9/yxiD9WFX/M8sccf8zzx8K5lD/m" +
                "+eMRf7wq/rjljzv+uOePh3Mpf9zzHw359vP/inz49z99KD8qvv" +
                "7Dvz/bubXyvz84/nC0/+Gq9j9s9z/s9j/s9z8czqX7H/b8FtXi" +
                "rz8tKPb604LDX39aMNNff1oMv8XxW/j6Qz0wl3XEb/H8RtXo+Y" +
                "0olt+Iw/MbMdPzGw2/0fEbBb+RReqI3+j5barN89tQLL8Nh+e3" +
                "Yabntxl+m+O3CX4bi9QRv83zm1Wz5zejWH4zDs9vxkzPbzb8Zs" +
                "dvFvxmFqkjfrPnd6gOz+9AsfwOHJ7fgZme32H4HY7fIfgdLFJH" +
                "/A7Pb1ANnt+AYvkNODy/ATM9v8HwGxy/QfAbWKSO+A2e367aPb" +
                "8dxfLbcXh+O2Z6frvhtzt+u+C3s0gd8ds9v0k1eX4TiuU34fD8" +
                "Jsz0/CbDb3L8JsFvYpE64jd5fqtq9fxWFMtvxeH5rZjp+a2G3+" +
                "r4rYLfyiJ1xG/1/CvC9990/a3m/Xd+TPH9t95Yzftv/PdHe082" +
                "qA1so9B6GJNenFtcxzWpg/1z36vV1Wyj0HoYk16cW1zHNakDPv" +
                "e9Tl3HNgqthzHpxbnFdVyTOuBz3/VqPdsotB7GpBfnFtdxTeqA" +
                "z32vUdewjULrYUx6cW5xHdekDvhmJZsFQ31L16HF338Cz3/+aW" +
                "Py+09uBb5/MEtvtvY6I1uyWXZlo5EVRmrB1/OtXoQxs9ppOPb7" +
                "T7Bi78m16lp/r65FofUwJr04t7iOa1IH++e+06VFUsifTrOMSk" +
                "9Nr6niVuyv3lfvs4XDfv7ovTAWZ6NdJqypiir9ylRpkRTu91Sa" +
                "ZVR6qap0nzBTfcFfE/33TxKfFYnvn4i/dM/F75/ZyJZq+OH3Ty" +
                "z/NGmRFO73aTTLqPRSVek+YaY6XVokhbrTaZZR6aWq0n3CTDVD" +
                "WiSFuhk0y6j01Iyq+IX+aoq0SAp1U2iWUemlqtJ9wkw1WVokhb" +
                "rJNMuo9FJV6T5hpnpPvccWDvv8914Yi7PRLhPWVEWVfuVUaZEU" +
                "7vepNMuo9FJV6T5hppokLZJC3SSaZVR6qap0nzBTzZQWSaFuJs" +
                "0yKr1UVbpPmKmmSYukUDeNZhmVXqoq3SfMVO+qd9nCYc8/74Wx" +
                "OBvtMmFNVVTpV85X57PNPsxhLOVzHddLkR2DXXOf89R5bLMPcx" +
                "hL+VzH9VJkx4DPfRaoBWyzD3MYS/lcx/VSZMeAz33mq/lssw9z" +
                "GEv5XMf1UmTHgM996lQd2+zDHMZSPtdxvRTZMeBzn8VqMdvswx" +
                "zGUj7Xcb0U2THgc5+z1Flssw9zGEv5XMf1UmTHgM99alUt2+zD" +
                "HMZSPtdxvRTZMeBzn0VqEdvswxzGUj7Xcb0U2THgc58lagnb7M" +
                "McxlI+13G9FNkx4HOfM9WZbLMPcxhL+VzH9VJkx4DPfeapeWyz" +
                "D3MYS/lcx/VSZMeAz30uUBewzT7MYSzlcx3XS5EdAz73OVudzT" +
                "b7MIexlM91XC9Fdgz43GdW8fvv1k7+/kv775jZ77+7q0j4/Xf+" +
                "/El+/z36O4g/f5qr5rLNPsxhLOVzHddLkR2D/XOfOWoO2+zDHM" +
                "ZSPtdxvRTZMeBzn3pVzzb7MIexlM91XC9Fdgz43OdCdSHb7MMc" +
                "xlI+13G9FNkx4HOfc9Q5bLMPcxhL+VzH9VJkx4BvVrLZMNSXdB" +
                "1a/Psz8PznTzYmf3/mVuD3h7P1ZmuvM7LFSI+RjUZWGKk1Ar8/" +
                "nG1/fzgbebrTcOzvzzL3Cwe1UPlvpoFFPsxhLOVzHddLkR2D/X" +
                "Ofc9W5bLMPcxhL+VzH9VJkx4DPfV4vWkHm65ViE1VX7qZe9tek" +
                "vclPbPbIz3/CW/51kVf15z/w+2fBf61oBff4tUqxiaord1NvFK" +
                "2g4o1KsYmqK3dTbxatoOLNSrGJqit3U68UraDilUqxiaord1MH" +
                "ilZQcaBSbKLqyt3Ufcm8+6rqVlVWhR63wCA7XueV0MNc6UuPY1" +
                "KX8B85gvv+yJFXqr0fnw/ff/6Ylfz+60P1IWi0cNA62TIWRsmT" +
                "HcKZNPVhyS6HoR4ii++d9OKYXAGNdplIzZmG/5Dx5sFQ95u1ef" +
                "n1mfgbIb8h/4G3vw9ZwVW/0+T/0HadB4I6LVJzJq5kDTCs7S0X" +
                "E14ckyug0S4TqTnTdZkJw9recjHhxTG5AhrtMpGaM12XDTCs7S" +
                "0XE14ckyug0S4TqTnTdTkDhrW95WLCi2O8km8GjXaZSM2Zrksj" +
                "DGt7y8WEF8d4JdRpkZozXZdvwLC2t1xMeHFMroBGu0yk5kxeN8" +
                "/Eu9Xd/upwt7hC3o0j9GWc8+V6GJO65Er8fLkfxtTz7KNFftwj" +
                "zijGRacXosgLaRs89tEiP+4RZxTjotNzUeS5tA0e+2iRH/eIM4" +
                "px0Sn6taz0w5h6ln20yFeJX9zKjGJcdHoxiryYtsFjHy3y4x5x" +
                "RjEuOj0RvTJfKezgcwv6/o+17VU8vyP8/CXITv7/T4nX/+jX0t" +
                "IPY+oZ9tEiXyV+cS0zinG78n+17rBG");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value1 = null;

    protected static void value1Init()
    {
        try
        {
            final int rows = 152;
            final int cols = 32;
            final int compressedBytes = 2580;
            final int uncompressedBytes = 19457;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrFWduP3VUVHvFKE0NI4MnEByEimNA2EA0pgf2bzoUZCi3DcE" +
                "sDxj/AQGi5Ftrf6Q3Qqi++NjHGxBdeNUETH+ChVqUN1wf8CyC1" +
                "+mQhXs/e317n+/bav316hlrmrHxrvstea8/lzEynZ24uPsLpue" +
                "KhuszCaWow036HP1Hnsul1l7w+zKOiBjPtd/gTdS6bTrnk1DCP" +
                "ihrMtN/hT9S5bHrNJa/NzfDAKTvLmdqZNp/Yo+FRcsD8MlPlz9" +
                "Y+PO3F/dz7eHicDKXKZ3Q4C66+ae11mpxD4RAZSpXP6HAWXH3T" +
                "2us0Oc+H58lQqnxGh7Pg6pvWXqfJeTm8TIZS5TM6nAVX37T2Ok" +
                "3OS+ElMpQqn9HhLLj6prXXaXL60JOhVPmMDmfB1TetvU6TcyQc" +
                "IUOp8hkdzoKrb1p7nSbn6fA0GUqVz+hwFlx909rrNDkvhhfJUK" +
                "p8Roez4Oqb1l6nyXkqPEWGUuUzOpwFV9+09jpNzivhFTKUKp/R" +
                "4Sy4+qa112lyjoajZChVPqPDWXD1TWuv0+TsD/vJUKp8Roez4O" +
                "qb1l6nyXkyPEmGUuUzOpwFV9+09jpNzr6wjwylymd0OAuuvmnt" +
                "dZqcH/TfARt9td/Rn+j39ulnU7+zX+pv7Lch64/2t/Tf7W/rfz" +
                "T6WnbmxxO/6NPP0X59jB9mf/yd3S+OcVNSN6d+q93WHxhPfX38" +
                "dmVy/4FwgAylymd0OAuuvmntdZqcw+EwGUqVz+hwFlx909rrND" +
                "nHwjEylCqf0eEsuPqmtddpckZhRIZS5TM6nAVX37T2Ok3OC+EF" +
                "MpQqn9HhLLj6prXXaXKeCc+QoVT5jA5nwdU3rb1Ok3MwHCRDqf" +
                "IZHc6Cq29ae50m59nwLBlKlc/ocBZcfdPa6zQ5z4XnyFCqfEaH" +
                "s+Dqm9Zep8k5Ho6ToVT5jA5nwdU3rb1Ok/Nm+tl7w+Qn5N7hv9" +
                "j6Wwr1vTT1y6zW5zb4kJ+/77KXzP3F9m6tzGvNTPn7k/e9x14y" +
                "N/FercxrzUy5n/e9zV4yN/F2rcxrzUy5n/e9z14yN/F+rcxrzU" +
                "y5n/e9xV4yN/FWrcxrzUy5n/edYS+ZmzhTK/NaM1Pu533vsJfM" +
                "TbxTK/NaM1Pu533nw3ky1DTlT4O3wG5TNjlxrlFmqN7fa+ytpq" +
                "qGpob3lCfDuXCODDVN+dPgLbDblE1OnI/CR2SoacqfBm+B3aZs" +
                "cuJ8GD4kQ6nymXfAW2C3KSI7H4ePyVCqfOYd8BbYbYrIzifhEz" +
                "KUKp95B7wFdpsisnNduI6cOr4tsyHNOc4rdGPxrOeeC+ECGUqV" +
                "z7wD3gK7TRHZuTfcS4ZS5TPvgLfAblNEdnaEHWQoVT7zDngL7D" +
                "ZFZGcpLJGhVPnMO+AtsNsUkZ2HwkNkKFU+8w54C+w2RWTnnnAP" +
                "GUqVz7wD3gK7TRHZeTA8SIZS5TPvgLfAblNEdh4OD5OhVPnMO+" +
                "AtsNsUkZ3dYTcZSpXPvAPeArtNEdlZC2tkKFU+8w54C+w2RWRn" +
                "PsyToVT5zDvgLbDbFJGdlbBChlLlM++At8BuU0R2doVdZChVPv" +
                "MOeAvsNkVkpwsdGUqVz7wD3gK7TRHZeSQ8QoZS5TPvgLfAblNE" +
                "du4P95OhVPnMO+AtsNsUkZ3xgwwPVT7zDngL7DZFZOeOcAcZSp" +
                "XPvAPeArtNEdm5M9xJhlLlM++At8BuU0R2lsMyGUqVz7wD3gK7" +
                "TRHZuS/cR4ZS5TPvgLfAblNEdtbDOhlKlc+8A94Cu00R2dkT9p" +
                "ChVPnMO+AtsNsUkZ27wl1kKFU+8w54C+w2RWTntsnrL2Gm1192" +
                "2+sv/a9Gvy5ff+l/Xr7+0v80vv7S/8y9/rJW/P/fzrCTDKXKZ9" +
                "4Bb4HdpojsLIQFMpQqn3kHvAV2myKysxpWyVCqfOYd8BbYbYrI" +
                "zmJYJEOp8pl3wFtgtykiO3eHu8lQqnzmHfAW2G2KyM4D4QEylC" +
                "qfeQe8BXabIrLzQc2Kv9Q/uFg2bfri28Krg+denWnbTKcu32P0" +
                "u0H31Cbf/4dNvv/0Jt//x8/s/t8Pun/a0P+E/ziWce/TKRXOql" +
                "bFTPtn9vn/8yZ//d/c5PvPbPL9Zzf0/PvNp7//08/Gye7mWFHF" +
                "Djb0sFPq8C3SFrTzZN56Y6zEJyxnonymTuzgLWjnybzlpliJT1" +
                "jORPlMndjBW9DOk3nL9liJbwcan//tPjONOZsegnaezFu/Hyvx" +
                "CcuZKJ/RKfswtPNk3rI1VuJbgcbHv9VnpjFn00PQzpN567ZYiW" +
                "8DGvdv85lpzNn0ELTzpO4IJ8PJyXflSfkOPYkqteY8r36ZaW/8" +
                "JHijrcssvEENZtrv8CfqfGhi449u7+zu5Xh0357dvSy/f84Nun" +
                "/d5N9/5zf5/r9t6Dfhb6fraVN2ljO1M9ut/9eP/+8b+vgfC4+R" +
                "A+aXmSp/tvbhaS9vnbAnwhNkKFU+o8NZcPVNa6/T5JxlL5l7j8" +
                "/WyrxwdqNfObnvWmWG6vy19lZTVUNTw3tmO3kJP3/+scnf/xc2" +
                "9JW4PlxPTh3fltmQ5hznFbqxvHXCbg+3k6FU+cw74C2w2xRx2T" +
                "7/n2zo8/+XmtX5tGza9MW3dVsGf4dvmen3/5ZL/xx2X4pF5pWe" +
                "ap/lnhLstofIzhWxyLzSU+2z3FOC3fYQ+Svyn1hkXump9lnuKc" +
                "Fue4j8Hn0lFplXemrIAeeeEuw2ReT36N8ossjVV+VTU+YYp2fd" +
                "9hDZ+VcsMq/0VPss95Rgtz1E/oxcGcu4fF9cqUl5zs6qVsVMe+" +
                "P7LxWZV3Mu8w4495Rgn5OTc+p8ORaZV3qqfZZ7SrDbHiI7n49F" +
                "5pWeap/lnhLstofIz4j/xiLzSk8NOeDcU4Ldpoj8Hl0Vy7g8L6" +
                "7SpDxnZ1WrYqa98fz7Qiwyr/TUkAPOPSXYbYrIzudikXmlp4Yc" +
                "cO4pwW5TRHa+GIvMKz015IBzTwl2myLyM+Kfsci80lNDDjj3lG" +
                "C3KSK/R1d3V8cOhjJ/SA2nuqF8a91uk3S+mx9NXpno5pvP0PlW" +
                "Fv3xjhPGamjHHu7qxg+5v2ve37Wy6I93nDBWQzv2cFe33q1f4r" +
                "/d1oHxv3lHpTt95vL+/XX4ihnf+2/GKhmT+tTQnJ1tQTtP5i3f" +
                "ipX4hOVMlM/UiR28Be08mbfcECvxCcuZKJ+pEzt4C9p5Mm/Z0+" +
                "2Z8Su1p+0j0xPtraOfaNqtdCsytTLl/pW2j8xOjI6Aj46PcZRJ" +
                "8o7xfPJWu1XZtTrl/tW2j8xOpPtXi/sn+fj+Vd3VrXVrM36fHW" +
                "zcvwZYpzvlY5mk3VK3JP5S8/Z9rSz6yGIf7adTJqbpJm+hW5Bd" +
                "C1Pub2TRRxZ7un+hhHbcx13dYrcouxan3N/Ioo8s9nT/YgntuI" +
                "+7uuVuWXYtT7m/kUUfWezp/uUS2nEfd3W7ul2ya9eU58yuto+s" +
                "vWnUp36oPJ/UN2KVjEl9amjOzragnSez8z+hKyN3");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value1 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value1[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int[][] value2 = null;

    protected static void value2Init()
    {
        try
        {
            final int rows = 33;
            final int cols = 32;
            final int compressedBytes = 578;
            final int uncompressedBytes = 4225;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNq1VTtOQ0EMfAUFUgBxAEQFHSdIQti9AGVK7kBNkyAKRAccAy" +
                "EOgETFASgp4QZ8JAoq2PU64/Wu814UWMvOeMbjFY8XxR/432h+" +
                "z/SyaQjVDk/hTK9YYdVKWTEZd1z7UYjZ/SPz/pHWZvePWLVSVk" +
                "ym+w/9odh53phHzmmeNDlhTcv52A1DEKI0XEOtcU8+dtdSVkym" +
                "rf0QEfcpjfv7WuOefOyupayYTFsHISIeUBr3D7TGPfnYXUtZMS" +
                "l3uE/3GSohCuYZSy1XuZMb8k+uvAeZmMf8b5N9rrlH9IS41zv0" +
                "RKmLTV/uK1RCFMwzllqucic35J9ceQ8y/R/WUHOkcYi6q3TzrN" +
                "5ebvbrSllvOhya4ll4SmaeP6KeUnqd7u/JWXhKZp4/og2/AUQh" +
                "O61phrCVqOxCpjfi2B0DUchOa2DgJSx57mUt1cg8oeZIfWOeyo" +
                "45y2OfxR21M72vsacri+zwm34TiEJ2WtMMYStR2YVMT+LNvQFR" +
                "yE5rmiFsJSq7kInZd/tAFLLTmmYIW4nKLmRi3t07EIXstKYZwl" +
                "aisguZmNcSZW/qa5s2z92+zb2UKHO8tGnz3O3b3DOjyV3NMbmN" +
                "9az6/RuLuYsu909OYr1p/uT4re7sfxx/1J2tzO2EyBGUcqrm41" +
                "krZcVk2rIbIuIZSprotCaZUAlbKSsmwS/5/Pe6s+Y34hs1R7W5" +
                "usvytN269O/fQ/X3b7Xz8xsv+fzHi2/VqvtwH0AUstOaZghbic" +
                "ou5B+8f9vd2cr5AbsbtLs=");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value2 = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value2[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupValue(int row, int col)
    {
        if (row <= 151)
            return value[row][col];
        else if (row >= 152 && row <= 303)
            return value1[row-152][col];
        else if (row >= 304)
            return value2[row-304][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in value2 lookup");
    }

    static
    {
        sigmapInit();
        valueInit();
        value1Init();
        value2Init();
    }
    }

    /**
     * The GOTO table.
     * <p>
     * The GOTO table maps a state and a nonterminal to a new state.
     * It is used when the parser reduces.  Suppose, for example, the parser
     * is reducing by the production <code>A ::= B C D</code>.  Then it
     * will pop three symbols from the <code>stateStack</code> and three symbols
     * from the <code>valueStack</code>.  It will look at the value now on top
     * of the state stack (call it <i>n</i>), and look up the entry for
     * <i>n</i> and <code>A</code> in the GOTO table to determine what state
     * it should transition to.
     */
    protected static final class GoToTable
    {
        /**
         * Returns the state the parser should transition to if the given
         * state is on top of the <code>stateStack</code> after popping
         * symbols corresponding to the right-hand side of the given production.
         *
         * @return the state to transition to (0 <= result < OpenACCParser.NUM_STATES)
         */
        protected static int getGoTo(int state, Nonterminal nonterminal)
        {
            assert 0 <= state && state < OpenACCParser.NUM_STATES;
            assert nonterminal != null;

            return get(state, nonterminal.getIndex());
        }

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 3, 0, 0, 4, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 1, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 9, 0, 10, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 3, 0, 4, 0, 0, 5, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 11, 0, 0, 7, 0, 0, 0, 12, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 1, 2, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 7, 16, 17, 18, 19, 13, 0, 14, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 22, 0, 0, 0, 0, 0, 0, 23, 24, 0, 0, 0, 0, 25, 0, 0, 0, 13, 26, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 27, 28, 29, 0, 16, 30, 0, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 31, 0, 32, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 0, 0, 0, 0, 0, 0, 33, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 34, 35, 36, 37, 0, 0, 38, 39, 40, 0, 41, 42, 43, 44, 45, 0, 46, 47, 48, 49, 0, 50, 0, 0, 0, 51, 52, 53, 0, 0, 54, 55, 56, 0, 0, 22, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 23, 0, 24, 57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 25, 0, 0, 0, 0, 6, 58, 0, 59, 60, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 61, 0, 0, 0, 0, 0, 0, 0, 0, 62, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 63, 0, 0, 0, 0, 0, 64, 0, 0, 0, 0 };
    protected static final int[] columnmap = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 10, 11, 12, 13, 14, 15, 1, 0, 16, 0, 17, 1, 18, 2, 19, 20, 21, 22, 23, 24, 25, 3, 3, 26, 4, 5, 2, 27, 0, 4, 7, 0, 1, 28, 29, 0, 30, 2, 4, 31, 32, 33, 34, 35, 4, 36, 37, 10, 38, 39, 40, 0, 5, 2, 41, 13, 42, 43, 44, 45, 46, 47, 48, 0, 49, 50, 51, 2, 52, 53, 54, 55, 56, 57, 20, 11, 58, 4, 3, 59, 60, 61, 62, 2, 63, 64, 21, 1, 65, 66, 15, 4, 67 };

    public static int get(int row, int col)
    {
        if (isErrorEntry(row, col))
            return -1;
        else if (columnmap[col] % 2 == 0)
            return lookupValue(rowmap[row], columnmap[col]/2) >>> 16;
        else
            return lookupValue(rowmap[row], columnmap[col]/2) & 0xFFFF;
    }

    protected static boolean isErrorEntry(int row, int col)
    {
        final int INT_BITS = 32;
        int sigmapRow = row;

        int sigmapCol = col / INT_BITS;
        int bitNumberFromLeft = col % INT_BITS;
        int sigmapMask = 0x1 << (INT_BITS - bitNumberFromLeft - 1);

        return (lookupSigmap(sigmapRow, sigmapCol) & sigmapMask) == 0;
    }

    protected static int[][] sigmap = null;

    protected static void sigmapInit()
    {
        try
        {
            final int rows = 523;
            final int cols = 4;
            final int compressedBytes = 523;
            final int uncompressedBytes = 8369;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtmTtSwzAQhn9pVEgQZsyM6Z1UZMgBKB0eM5Q09BzFBQegyA" +
                "k4SWCSnkPkIEhyYhvZ8SO2FQW8RZKN16v1Wtr9LD97uJ3AR8in" +
                "BINUSrT78Zw7FOjPr4gBfETxzi+lypn6k6ZWSzB46vgiLEw4o5" +
                "G0kYcYQqWvXhnT/haYF5hTXOlvEYU6NrqZMw989iHtH2OH4PKI" +
                "aHu94XDv/4Vk5m8vBYH9VjkrtxsL3OMi+PYJfO5CftL1CysF84" +
                "Eyucjlpb9FTi7BuF4hqVenJrK+oqS+di109SLHexqdBwt8Fubz" +
                "bGvo467BOsr3g0zttjIPjuyfHTle5sZ03vX/SdL/+42fbtR8Fr" +
                "MbLOi6o0SqOi9sJYzk9QgGs5XxEDHQyDy/ht70/Lbjlfqr4oG2" +
                "4zVcx871f0PGRKUvCL0TxT3b8av7SUR6P7sef+tvebA/g+/0/B" +
                "M9zr+U7zrhJx3vNI73us56rOK5KnvL/OS6JHyHPXznHL9veRMV" +
                "vOlKfg2+ZU3XA5r1e2v8NUhPPJzwKdY2BszNJ8s8O0j/vERivu" +
                "BFusk3QpZVpaMePxScn+vnTZ5nZHxq/OVufK3H8bK9ekm8GXvw" +
                "A/QSfwfxVTb/e3Ttv3b+DX6s0m3znXM8Y/JDx/106DfG+xtSvq" +
                "+Xe14e3uoN8pf7caY/OrE/RGy/Km3Hl33vb3S+f/cDkAJ7PA==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupSigmap(int row, int col)
    {
        return sigmap[row][col];
    }

    protected static int[][] value = null;

    protected static void valueInit()
    {
        try
        {
            final int rows = 65;
            final int cols = 34;
            final int compressedBytes = 1323;
            final int uncompressedBytes = 8841;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVV2tsFFUU/s5WKAW0xbdGI0UlKhACGkWkVrQCWkAUtdbdWn" +
                "lU3g9Boj8qJDUa0UgQRR7yCBCJmMZENPrbfxojAdFWtFbb7KpN" +
                "bTSCQqE6nnvmsTOzs8tOd5kdb3LPPXPvndlvz/nON3cwHANwCC" +
                "WYSgewDq0Yh/EoQzuqUYFOVOIFvIiX0QNCBEUYgyP4CqUYiG78" +
                "ht/xJYoxCoMxDUfRgmOYgIk4D3E0IoG70ISXUI4RGIYvcBij8T" +
                "yGYCyG4nyMRBsmoQODsBaT0YUqHMRjOIDZmob3NW6YhYdk/EzT" +
                "qFa898Q+KHamltIww+Y/gkdlbOZeg3fZlqtrupG96bLygObRUE" +
                "t1mIt5FEMM8zkedSoefN/jaEAFRc148M45eILtGI9njLL5HA8Z" +
                "G7lzPOgmAwffh9GyMtba+6nYtWKr8AxWYyXxb1CNzCzBMhmf5p" +
                "l6WzyWYhXbRR44Ftr85VhhxWONiofeiH8dC2RlsWc8nsNW7MFO" +
                "jsc2+gSbeOYNbMFb9CR2YBfHY5/Cgf1sN2M3240ez9hg87fjbQ" +
                "vHXhuOcXz9mqy87onjHe5/8L4YPmdvnckPmoNqtlG+NvPijx+N" +
                "wo8m2m/gGJ+OH0ZeanEGvdTHOE7pPOW5k3zfXPTpOKy8/I2/2J" +
                "7QMjacNsZmscl43MxXx89yr+LpPILih8lT9hRP61Fp4fDH02bh" +
                "aRLHrak8paW2+6qoCKtVXqwZ4SnNF55GbTt98VT8Na59GXlKl1" +
                "CZwkED2Q7iXkKD2TbQBToOuogupkt5vJKuYnt56jOo2OYPoaEy" +
                "lnIfRhc69l0m9grX3VeLvYauo+tpOD1FI13rN5h1a1xfS+VaHh" +
                "qNSMMOrluJxy3sJ+t2QbJuec533Yq/17UvY91SBVXRTMYxgdHc" +
                "zn0iTaI7aCFNprspSrNpKk2j+3h+BnHlUrXHP7zN5t9JlTLew/" +
                "1emuLYd7/Y6a67Z4l9mPubDp7q+rGI3Dz1pR/i17j2ZXy/GKhi" +
                "LpSL5T0X1QJrtAW9bJfYsJ0U2+fkqT8d81zLRsditFP8OgPdMj" +
                "S48iI6dg7jsdtTx5YHrWP8a7qO7XHo2IpC6Jjg2OdYXSk6Fg1W" +
                "x3CI1z9UOsbnU9ExPp+yjvH51NAxPp8aOqbOpzLXTavU+dTUMe" +
                "N8KjqGOPeEU8f4fOrQMbRx7zD8Ll3HBMdHSsfQqusY+6xj6DR1" +
                "DD3eOqZwmDpGz5o6hrh/HaNu9CocxnNb7XWLTmu+x7tuFQ7riU" +
                "1m3ap4+K1beoXKJB5/Kp5yPISnaFc8VTh0nnI8DJ4m88Jd8qLz" +
                "VM+LzlOORykSTp7SeidPzbwonqJL56ngOG7GI/m+tcfDm6eOeL" +
                "xqzcb989Tix6nc+EEb0ZILP+RpDn7I2C7WFg/LS8mLMS95ScYD" +
                "CdfvbXb9A0e98Po22h4Ijq2ZcaR538ppJsj3bRocO0KCY1fQOJ" +
                "gfH4SYHwdDkpePC4EjNS/GlUdeHOuuvGSjp55ROBISHMw4OhqQ" +
                "nq4/O09TcdDX6XEYo08cKbF06/o34YgH60dLePQjDX++1ULR6F" +
                "h49cPkKX2XuW798rS/OPKhH/R9SHC05Y7Dq17oh+DyQu351/V+" +
                "4fgxJDh+CgmOjnDgyFe9UGduPHU8K56feOR6HqNE4XBkmxf6+d" +
                "zqR7rvffqlcHlx4Pg1JDi6csfh8f1yokDfUf94zhb8Oyqb8yn9" +
                "6+d8isM5nE8DjEWkKKDvhqzO65EBnvGo/z/ww188IsX90dNscE" +
                "RK8slT/Ae9q5qw");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupValue(int row, int col)
    {
        return value[row][col];
    }

    static
    {
        sigmapInit();
        valueInit();
    }
    }

    /**
     * The error recovery table.
     * <p>
     * See {@link #attemptToRecoverFromSyntaxError()} for a description of the
     * error recovery algorithm.
     * <p>
     * This table takes the state on top of the stack and the current lookahead
     * symbol and returns what action should be taken.  The result value should
     * be interpreted as follows:
     * <ul>
     *   <li> If <code>result & ACTION_MASK == DISCARD_STATE_ACTION</code>,
     *        pop a symbol from the parser stacks; a &quot;known&quot; sequence
     *        of symbols has not been found.
     *   <li> If <code>result & ACTION_MASK == DISCARD_TERMINAL_ACTION</code>,
     *        a &quot;known&quot; sequence of symbols has been found, and we
     *        are looking for the error lookahead symbol.  Shift the terminal.
     *   <li> If <code>result & ACTION_MASK == RECOVER_ACTION</code>, we have
     *        matched the error recovery production
     *        <code>Production.values[result & VALUE_MASK]</code>, so reduce
     *        by that production (including the lookahead symbol), and then
     *        continue with normal parsing.
     * </ul>
     * If it is not possible to recover from a syntax error, either the state
     * stack will be emptied or the end of input will be reached before a
     * RECOVER_ACTION is found.
     *
     * @return a code for the action to take (see above)
     */
    protected static final class RecoveryTable
    {
        protected static int getRecoveryCode(int state, Token lookahead)
        {
            assert 0 <= state && state < OpenACCParser.NUM_STATES;
            assert lookahead != null;

            return get(state, lookahead.getTerminal().getIndex());
        }

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    protected static final int[] columnmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    public static int get(int row, int col)
    {
        if (isErrorEntry(row, col))
            return 0;
        else if (columnmap[col] % 2 == 0)
            return lookupValue(rowmap[row], columnmap[col]/2) >>> 16;
        else
            return lookupValue(rowmap[row], columnmap[col]/2) & 0xFFFF;
    }

    protected static boolean isErrorEntry(int row, int col)
    {
        final int INT_BITS = 32;
        int sigmapRow = row;

        int sigmapCol = col / INT_BITS;
        int bitNumberFromLeft = col % INT_BITS;
        int sigmapMask = 0x1 << (INT_BITS - bitNumberFromLeft - 1);

        return (lookupSigmap(sigmapRow, sigmapCol) & sigmapMask) == 0;
    }

    protected static int[][] sigmap = null;

    protected static void sigmapInit()
    {
        try
        {
            final int rows = 523;
            final int cols = 4;
            final int compressedBytes = 31;
            final int uncompressedBytes = 8369;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtwTEBAAAAwqD1T20LL6AAAAAAAAAAAM4GILEAAQ==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupSigmap(int row, int col)
    {
        return sigmap[row][col];
    }

    protected static int[][] value = null;

    protected static void valueInit()
    {
        try
        {
            final int rows = 1;
            final int cols = 1;
            final int compressedBytes = 7;
            final int uncompressedBytes = 5;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNpjYAACAA==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupValue(int row, int col)
    {
        return value[row][col];
    }

    static
    {
        sigmapInit();
        valueInit();
    }
    }

    protected static final int base64Decode(byte[] decodeIntoBuffer, String encodedString)
    {
        int[] encodedBuffer = new int[4];
        int bytesDecoded = 0;
        int inputLength = encodedString.length();

        if (inputLength % 4 != 0) throw new IllegalArgumentException("Invalid Base64-encoded data (wrong length)");

        for (int inputOffset = 0; inputOffset < inputLength; inputOffset += 4)
        {
            int padding = 0;

            for (int i = 0; i < 4; i++)
            {
                char value = encodedString.charAt(inputOffset + i);
                if (value >= 'A' && value <= 'Z')
                    encodedBuffer[i] = value - 'A';
                else if (value >= 'a' && value <= 'z')
                    encodedBuffer[i] = value - 'a' + 26;
                else if (value >= '0' && value <= '9')
                    encodedBuffer[i] = value - '0' + 52;
                else if (value == '+')
                    encodedBuffer[i] = 62;
                else if (value == '/')
                    encodedBuffer[i] = 63;
                else if (value == '=')
                    { encodedBuffer[i] = 0; padding++; }
                else throw new IllegalArgumentException("Invalid character " + value + " in Base64-encoded data");
            }

            assert 0 <= padding && padding <= 2;

            decodeIntoBuffer[bytesDecoded+0] = (byte)(  ((encodedBuffer[0] & 0x3F) <<  2)
                                                      | ((encodedBuffer[1] & 0x30) >>> 4));
            if (padding < 2)
               decodeIntoBuffer[bytesDecoded+1] = (byte)(  ((encodedBuffer[1] & 0x0F) <<  4)
                                                         | ((encodedBuffer[2] & 0x3C) >>> 2));

            if (padding < 1)
               decodeIntoBuffer[bytesDecoded+2] = (byte)(  ((encodedBuffer[2] & 0x03) <<  6)
                                                         |  (encodedBuffer[3] & 0x3F));

            bytesDecoded += (3 - padding);
        }

        return bytesDecoded;
    }
}
