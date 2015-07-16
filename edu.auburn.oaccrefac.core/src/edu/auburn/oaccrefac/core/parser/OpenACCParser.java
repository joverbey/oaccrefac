package edu.auburn.oaccrefac.core.parser;

import edu.auburn.oaccrefac.core.parser.SyntaxException;                   import java.io.IOException;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;


import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.zip.Inflater;

import edu.auburn.oaccrefac.core.parser.OpenACCParser.*;
import edu.auburn.oaccrefac.core.parser.ParsingTables.*;

/**
 * An LALR(1) parser for OpenACC 1.0
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

    protected static final int NUM_STATES = 417;
    protected static final int NUM_PRODUCTIONS = 283;
    protected static final int NUM_TERMINALS = 83;
    protected static final int NUM_NONTERMINALS = 86;

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
        public static final Terminal LITERAL_STRING_COLON = new Terminal(0, "literal string colon");
        public static final Terminal LITERAL_STRING_QUESTION = new Terminal(1, "literal string question");
        public static final Terminal LITERAL_STRING_UPDATE = new Terminal(2, "literal string update");
        public static final Terminal LITERAL_STRING_PLUS = new Terminal(3, "literal string plus");
        public static final Terminal LITERAL_STRING_VBAR_VBAR = new Terminal(4, "literal string vbar vbar");
        public static final Terminal LITERAL_STRING_PERIOD = new Terminal(5, "literal string period");
        public static final Terminal LITERAL_STRING_HYPHEN = new Terminal(6, "literal string hyphen");
        public static final Terminal LITERAL_STRING_TILDE = new Terminal(7, "literal string tilde");
        public static final Terminal LITERAL_STRING_IF = new Terminal(8, "literal string if");
        public static final Terminal LITERAL_STRING_EXCLAMATION = new Terminal(9, "literal string exclamation");
        public static final Terminal END_OF_INPUT = new Terminal(10, "end of input");
        public static final Terminal LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPY = new Terminal(11, "literal string present underscoreor underscorecopy");
        public static final Terminal LITERAL_STRING_DEVICE_UNDERSCORERESIDENT = new Terminal(12, "literal string device underscoreresident");
        public static final Terminal LITERAL_STRING_AMPERSAND_AMPERSAND = new Terminal(13, "literal string ampersand ampersand");
        public static final Terminal LITERAL_STRING_AMPERSAND = new Terminal(14, "literal string ampersand");
        public static final Terminal LITERAL_STRING_SEQ = new Terminal(15, "literal string seq");
        public static final Terminal LITERAL_STRING_PRESENT = new Terminal(16, "literal string present");
        public static final Terminal LITERAL_STRING_MAX = new Terminal(17, "literal string max");
        public static final Terminal LITERAL_STRING_SIZEOF = new Terminal(18, "literal string sizeof");
        public static final Terminal LITERAL_STRING_GANG = new Terminal(19, "literal string gang");
        public static final Terminal LITERAL_STRING_COLLAPSE = new Terminal(20, "literal string collapse");
        public static final Terminal LITERAL_STRING_LESSTHAN_LESSTHAN = new Terminal(21, "literal string lessthan lessthan");
        public static final Terminal LITERAL_STRING_DECLARE = new Terminal(22, "literal string declare");
        public static final Terminal LITERAL_STRING_WAIT = new Terminal(23, "literal string wait");
        public static final Terminal LITERAL_STRING_PERCENT = new Terminal(24, "literal string percent");
        public static final Terminal SKIP = new Terminal(25, "skip");
        public static final Terminal LITERAL_STRING_REDUCTION = new Terminal(26, "literal string reduction");
        public static final Terminal LITERAL_STRING_NUM_UNDERSCOREGANGS = new Terminal(27, "literal string num underscoregangs");
        public static final Terminal LITERAL_STRING_LPAREN = new Terminal(28, "literal string lparen");
        public static final Terminal LITERAL_STRING_DEVICE = new Terminal(29, "literal string device");
        public static final Terminal LITERAL_STRING_HYPHEN_HYPHEN = new Terminal(30, "literal string hyphen hyphen");
        public static final Terminal LITERAL_STRING_KERNELS = new Terminal(31, "literal string kernels");
        public static final Terminal LITERAL_STRING_LESSTHAN_EQUALS = new Terminal(32, "literal string lessthan equals");
        public static final Terminal LITERAL_STRING_PCOPY = new Terminal(33, "literal string pcopy");
        public static final Terminal LITERAL_STRING_RPAREN = new Terminal(34, "literal string rparen");
        public static final Terminal INTEGER_CONSTANT = new Terminal(35, "integer constant");
        public static final Terminal LITERAL_STRING_PCOPYOUT = new Terminal(36, "literal string pcopyout");
        public static final Terminal LITERAL_STRING_COPY = new Terminal(37, "literal string copy");
        public static final Terminal LITERAL_STRING_GREATERTHAN_EQUALS = new Terminal(38, "literal string greaterthan equals");
        public static final Terminal LITERAL_STRING_HOST_UNDERSCOREDATA = new Terminal(39, "literal string host underscoredata");
        public static final Terminal CHARACTER_CONSTANT = new Terminal(40, "character constant");
        public static final Terminal LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECREATE = new Terminal(41, "literal string present underscoreor underscorecreate");
        public static final Terminal FLOATING_CONSTANT = new Terminal(42, "floating constant");
        public static final Terminal LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPYIN = new Terminal(43, "literal string present underscoreor underscorecopyin");
        public static final Terminal LITERAL_STRING_LESSTHAN = new Terminal(44, "literal string lessthan");
        public static final Terminal LITERAL_STRING_NUM_UNDERSCOREWORKERS = new Terminal(45, "literal string num underscoreworkers");
        public static final Terminal LITERAL_STRING_CREATE = new Terminal(46, "literal string create");
        public static final Terminal LITERAL_STRING_DEVICEPTR = new Terminal(47, "literal string deviceptr");
        public static final Terminal LITERAL_STRING_PLUS_PLUS = new Terminal(48, "literal string plus plus");
        public static final Terminal LITERAL_STRING_USE_UNDERSCOREDEVICE = new Terminal(49, "literal string use underscoredevice");
        public static final Terminal LITERAL_STRING_GREATERTHAN_GREATERTHAN = new Terminal(50, "literal string greaterthan greaterthan");
        public static final Terminal LITERAL_STRING_EXCLAMATION_EQUALS = new Terminal(51, "literal string exclamation equals");
        public static final Terminal LITERAL_STRING_VBAR = new Terminal(52, "literal string vbar");
        public static final Terminal LITERAL_STRING_VECTOR_UNDERSCORELENGTH = new Terminal(53, "literal string vector underscorelength");
        public static final Terminal LITERAL_STRING_WORKER = new Terminal(54, "literal string worker");
        public static final Terminal LITERAL_STRING_COPYOUT = new Terminal(55, "literal string copyout");
        public static final Terminal LITERAL_STRING_COPYIN = new Terminal(56, "literal string copyin");
        public static final Terminal PRAGMA_ACC = new Terminal(57, "pragma acc");
        public static final Terminal LITERAL_STRING_EQUALS_EQUALS = new Terminal(58, "literal string equals equals");
        public static final Terminal LITERAL_STRING_PCREATE = new Terminal(59, "literal string pcreate");
        public static final Terminal LITERAL_STRING_PCOPYIN = new Terminal(60, "literal string pcopyin");
        public static final Terminal IDENTIFIER = new Terminal(61, "identifier");
        public static final Terminal LITERAL_STRING_GREATERTHAN = new Terminal(62, "literal string greaterthan");
        public static final Terminal LITERAL_STRING_CACHE = new Terminal(63, "literal string cache");
        public static final Terminal LITERAL_STRING_PRIVATE = new Terminal(64, "literal string private");
        public static final Terminal LITERAL_STRING_CARET = new Terminal(65, "literal string caret");
        public static final Terminal LITERAL_STRING_DATA = new Terminal(66, "literal string data");
        public static final Terminal LITERAL_STRING_VECTOR = new Terminal(67, "literal string vector");
        public static final Terminal LITERAL_STRING_ASTERISK = new Terminal(68, "literal string asterisk");
        public static final Terminal LITERAL_STRING_HOST = new Terminal(69, "literal string host");
        public static final Terminal LITERAL_STRING_LBRACKET = new Terminal(70, "literal string lbracket");
        public static final Terminal LITERAL_STRING_RBRACKET = new Terminal(71, "literal string rbracket");
        public static final Terminal LITERAL_STRING_INDEPENDENT = new Terminal(72, "literal string independent");
        public static final Terminal LITERAL_STRING_HYPHEN_GREATERTHAN = new Terminal(73, "literal string hyphen greaterthan");
        public static final Terminal LITERAL_STRING_MIN = new Terminal(74, "literal string min");
        public static final Terminal LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPYOUT = new Terminal(75, "literal string present underscoreor underscorecopyout");
        public static final Terminal STRING_LITERAL = new Terminal(76, "string literal");
        public static final Terminal LITERAL_STRING_LOOP = new Terminal(77, "literal string loop");
        public static final Terminal LITERAL_STRING_COMMA = new Terminal(78, "literal string comma");
        public static final Terminal LITERAL_STRING_PARALLEL = new Terminal(79, "literal string parallel");
        public static final Terminal LITERAL_STRING_ASYNC = new Terminal(80, "literal string async");
        public static final Terminal LITERAL_STRING_FIRSTPRIVATE = new Terminal(81, "literal string firstprivate");
        public static final Terminal LITERAL_STRING_SLASH = new Terminal(82, "literal string slash");

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
        terminals.put(0, Terminal.LITERAL_STRING_COLON);
        terminals.put(1, Terminal.LITERAL_STRING_QUESTION);
        terminals.put(2, Terminal.LITERAL_STRING_UPDATE);
        terminals.put(3, Terminal.LITERAL_STRING_PLUS);
        terminals.put(4, Terminal.LITERAL_STRING_VBAR_VBAR);
        terminals.put(5, Terminal.LITERAL_STRING_PERIOD);
        terminals.put(6, Terminal.LITERAL_STRING_HYPHEN);
        terminals.put(7, Terminal.LITERAL_STRING_TILDE);
        terminals.put(8, Terminal.LITERAL_STRING_IF);
        terminals.put(9, Terminal.LITERAL_STRING_EXCLAMATION);
        terminals.put(10, Terminal.END_OF_INPUT);
        terminals.put(11, Terminal.LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPY);
        terminals.put(12, Terminal.LITERAL_STRING_DEVICE_UNDERSCORERESIDENT);
        terminals.put(13, Terminal.LITERAL_STRING_AMPERSAND_AMPERSAND);
        terminals.put(14, Terminal.LITERAL_STRING_AMPERSAND);
        terminals.put(15, Terminal.LITERAL_STRING_SEQ);
        terminals.put(16, Terminal.LITERAL_STRING_PRESENT);
        terminals.put(17, Terminal.LITERAL_STRING_MAX);
        terminals.put(18, Terminal.LITERAL_STRING_SIZEOF);
        terminals.put(19, Terminal.LITERAL_STRING_GANG);
        terminals.put(20, Terminal.LITERAL_STRING_COLLAPSE);
        terminals.put(21, Terminal.LITERAL_STRING_LESSTHAN_LESSTHAN);
        terminals.put(22, Terminal.LITERAL_STRING_DECLARE);
        terminals.put(23, Terminal.LITERAL_STRING_WAIT);
        terminals.put(24, Terminal.LITERAL_STRING_PERCENT);
        terminals.put(25, Terminal.SKIP);
        terminals.put(26, Terminal.LITERAL_STRING_REDUCTION);
        terminals.put(27, Terminal.LITERAL_STRING_NUM_UNDERSCOREGANGS);
        terminals.put(28, Terminal.LITERAL_STRING_LPAREN);
        terminals.put(29, Terminal.LITERAL_STRING_DEVICE);
        terminals.put(30, Terminal.LITERAL_STRING_HYPHEN_HYPHEN);
        terminals.put(31, Terminal.LITERAL_STRING_KERNELS);
        terminals.put(32, Terminal.LITERAL_STRING_LESSTHAN_EQUALS);
        terminals.put(33, Terminal.LITERAL_STRING_PCOPY);
        terminals.put(34, Terminal.LITERAL_STRING_RPAREN);
        terminals.put(35, Terminal.INTEGER_CONSTANT);
        terminals.put(36, Terminal.LITERAL_STRING_PCOPYOUT);
        terminals.put(37, Terminal.LITERAL_STRING_COPY);
        terminals.put(38, Terminal.LITERAL_STRING_GREATERTHAN_EQUALS);
        terminals.put(39, Terminal.LITERAL_STRING_HOST_UNDERSCOREDATA);
        terminals.put(40, Terminal.CHARACTER_CONSTANT);
        terminals.put(41, Terminal.LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECREATE);
        terminals.put(42, Terminal.FLOATING_CONSTANT);
        terminals.put(43, Terminal.LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPYIN);
        terminals.put(44, Terminal.LITERAL_STRING_LESSTHAN);
        terminals.put(45, Terminal.LITERAL_STRING_NUM_UNDERSCOREWORKERS);
        terminals.put(46, Terminal.LITERAL_STRING_CREATE);
        terminals.put(47, Terminal.LITERAL_STRING_DEVICEPTR);
        terminals.put(48, Terminal.LITERAL_STRING_PLUS_PLUS);
        terminals.put(49, Terminal.LITERAL_STRING_USE_UNDERSCOREDEVICE);
        terminals.put(50, Terminal.LITERAL_STRING_GREATERTHAN_GREATERTHAN);
        terminals.put(51, Terminal.LITERAL_STRING_EXCLAMATION_EQUALS);
        terminals.put(52, Terminal.LITERAL_STRING_VBAR);
        terminals.put(53, Terminal.LITERAL_STRING_VECTOR_UNDERSCORELENGTH);
        terminals.put(54, Terminal.LITERAL_STRING_WORKER);
        terminals.put(55, Terminal.LITERAL_STRING_COPYOUT);
        terminals.put(56, Terminal.LITERAL_STRING_COPYIN);
        terminals.put(57, Terminal.PRAGMA_ACC);
        terminals.put(58, Terminal.LITERAL_STRING_EQUALS_EQUALS);
        terminals.put(59, Terminal.LITERAL_STRING_PCREATE);
        terminals.put(60, Terminal.LITERAL_STRING_PCOPYIN);
        terminals.put(61, Terminal.IDENTIFIER);
        terminals.put(62, Terminal.LITERAL_STRING_GREATERTHAN);
        terminals.put(63, Terminal.LITERAL_STRING_CACHE);
        terminals.put(64, Terminal.LITERAL_STRING_PRIVATE);
        terminals.put(65, Terminal.LITERAL_STRING_CARET);
        terminals.put(66, Terminal.LITERAL_STRING_DATA);
        terminals.put(67, Terminal.LITERAL_STRING_VECTOR);
        terminals.put(68, Terminal.LITERAL_STRING_ASTERISK);
        terminals.put(69, Terminal.LITERAL_STRING_HOST);
        terminals.put(70, Terminal.LITERAL_STRING_LBRACKET);
        terminals.put(71, Terminal.LITERAL_STRING_RBRACKET);
        terminals.put(72, Terminal.LITERAL_STRING_INDEPENDENT);
        terminals.put(73, Terminal.LITERAL_STRING_HYPHEN_GREATERTHAN);
        terminals.put(74, Terminal.LITERAL_STRING_MIN);
        terminals.put(75, Terminal.LITERAL_STRING_PRESENT_UNDERSCOREOR_UNDERSCORECOPYOUT);
        terminals.put(76, Terminal.STRING_LITERAL);
        terminals.put(77, Terminal.LITERAL_STRING_LOOP);
        terminals.put(78, Terminal.LITERAL_STRING_COMMA);
        terminals.put(79, Terminal.LITERAL_STRING_PARALLEL);
        terminals.put(80, Terminal.LITERAL_STRING_ASYNC);
        terminals.put(81, Terminal.LITERAL_STRING_FIRSTPRIVATE);
        terminals.put(82, Terminal.LITERAL_STRING_SLASH);
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
        public static final Nonterminal ACC_KERNELS_LOOP_CLAUSE = new Nonterminal(1, "<acc kernels loop clause>");
        public static final Nonterminal PRIMARY_EXPRESSION = new Nonterminal(2, "<primary expression>");
        public static final Nonterminal CONSTANT = new Nonterminal(3, "<constant>");
        public static final Nonterminal ACC_CONSTRUCT = new Nonterminal(4, "<acc construct>");
        public static final Nonterminal ACC_DATA = new Nonterminal(5, "<acc data>");
        public static final Nonterminal ACC_COPY_CLAUSE = new Nonterminal(6, "<acc copy clause>");
        public static final Nonterminal ACC_LOOP = new Nonterminal(7, "<acc loop>");
        public static final Nonterminal ACC_WAIT_PARAMETER = new Nonterminal(8, "<acc wait parameter>");
        public static final Nonterminal ACC_PARALLEL_LOOP_CLAUSE = new Nonterminal(9, "<acc parallel loop clause>");
        public static final Nonterminal ACC_REDUCTION_OPERATOR = new Nonterminal(10, "<acc reduction operator>");
        public static final Nonterminal ACC_PRIVATE_CLAUSE = new Nonterminal(11, "<acc private clause>");
        public static final Nonterminal CAST_EXPRESSION = new Nonterminal(12, "<cast expression>");
        public static final Nonterminal UNARY_EXPRESSION = new Nonterminal(13, "<unary expression>");
        public static final Nonterminal POSTFIX_EXPRESSION = new Nonterminal(14, "<postfix expression>");
        public static final Nonterminal ACC_DEVICEPTR_CLAUSE = new Nonterminal(15, "<acc deviceptr clause>");
        public static final Nonterminal ACC_VECTORLENGTH_CLAUSE = new Nonterminal(16, "<acc vectorlength clause>");
        public static final Nonterminal ACC_PRESENTORCREATE_CLAUSE = new Nonterminal(17, "<acc presentorcreate clause>");
        public static final Nonterminal INCLUSIVE_OR_EXPRESSION = new Nonterminal(18, "<inclusive or expression>");
        public static final Nonterminal ACC_FIRSTPRIVATE_CLAUSE = new Nonterminal(19, "<acc firstprivate clause>");
        public static final Nonterminal ACC_DECLARE = new Nonterminal(20, "<acc declare>");
        public static final Nonterminal ACC_COPYIN_CLAUSE = new Nonterminal(21, "<acc copyin clause>");
        public static final Nonterminal ACC_PRESENT_CLAUSE = new Nonterminal(22, "<acc present clause>");
        public static final Nonterminal LOGICAL_AND_EXPRESSION = new Nonterminal(23, "<logical and expression>");
        public static final Nonterminal ACC_UPDATE_CLAUSE_LIST = new Nonterminal(25, "<acc update clause list>");
        public static final Nonterminal ACC_ASYNC_CLAUSE = new Nonterminal(26, "<acc async clause>");
        public static final Nonterminal ACC_PARALLEL_CLAUSE_LIST = new Nonterminal(27, "<acc parallel clause list>");
        public static final Nonterminal IDENTIFIER = new Nonterminal(28, "<identifier>");
        public static final Nonterminal ACC_KERNELS = new Nonterminal(29, "<acc kernels>");
        public static final Nonterminal ACC_NUMWORKERS_CLAUSE = new Nonterminal(30, "<acc numworkers clause>");
        public static final Nonterminal STRING_LITERAL_TERMINAL_LIST = new Nonterminal(31, "<STRING LITERAL terminal list>");
        public static final Nonterminal ACC_VECTOR_CLAUSE = new Nonterminal(32, "<acc vector clause>");
        public static final Nonterminal AND_EXPRESSION = new Nonterminal(33, "<and expression>");
        public static final Nonterminal ACC_DEVICERESIDENT_CLAUSE = new Nonterminal(34, "<acc deviceresident clause>");
        public static final Nonterminal ACC_KERNELS_CLAUSE = new Nonterminal(35, "<acc kernels clause>");
        public static final Nonterminal ACC_PRESENTORCOPY_CLAUSE = new Nonterminal(36, "<acc presentorcopy clause>");
        public static final Nonterminal ACC_WORKER_CLAUSE = new Nonterminal(37, "<acc worker clause>");
        public static final Nonterminal ACC_COLLAPSE_CLAUSE = new Nonterminal(38, "<acc collapse clause>");
        public static final Nonterminal ACC_GANG_CLAUSE = new Nonterminal(39, "<acc gang clause>");
        public static final Nonterminal ACC_HOST_CLAUSE = new Nonterminal(40, "<acc host clause>");
        public static final Nonterminal ACC_LOOP_CLAUSE = new Nonterminal(41, "<acc loop clause>");
        public static final Nonterminal ACC_USEDEVICE_CLAUSE = new Nonterminal(42, "<acc usedevice clause>");
        public static final Nonterminal ACC_KERNELS_LOOP_CLAUSE_LIST = new Nonterminal(43, "<acc kernels loop clause list>");
        public static final Nonterminal ACC_HOSTDATA_CLAUSE_LIST = new Nonterminal(44, "<acc hostdata clause list>");
        public static final Nonterminal ACC_REDUCTION_CLAUSE = new Nonterminal(45, "<acc reduction clause>");
        public static final Nonterminal ACC_DEVICE_CLAUSE = new Nonterminal(46, "<acc device clause>");
        public static final Nonterminal UNARY_OPERATOR = new Nonterminal(47, "<unary operator>");
        public static final Nonterminal ACC_WAIT = new Nonterminal(48, "<acc wait>");
        public static final Nonterminal ACC_COUNT = new Nonterminal(49, "<acc count>");
        public static final Nonterminal ACC_CACHE = new Nonterminal(50, "<acc cache>");
        public static final Nonterminal ACC_UPDATE = new Nonterminal(51, "<acc update>");
        public static final Nonterminal CONDITIONAL_EXPRESSION = new Nonterminal(52, "<conditional expression>");
        public static final Nonterminal ACC_CREATE_CLAUSE = new Nonterminal(53, "<acc create clause>");
        public static final Nonterminal ACC_PRESENTORCOPYIN_CLAUSE = new Nonterminal(54, "<acc presentorcopyin clause>");
        public static final Nonterminal SHIFT_EXPRESSION = new Nonterminal(55, "<shift expression>");
        public static final Nonterminal ACC_LOOP_CLAUSE_LIST = new Nonterminal(56, "<acc loop clause list>");
        public static final Nonterminal ACC_COPYOUT_CLAUSE = new Nonterminal(57, "<acc copyout clause>");
        public static final Nonterminal LOGICAL_OR_EXPRESSION = new Nonterminal(58, "<logical or expression>");
        public static final Nonterminal ACC_PARALLEL_LOOP = new Nonterminal(59, "<acc parallel loop>");
        public static final Nonterminal ACC_DATA_ITEM = new Nonterminal(60, "<acc data item>");
        public static final Nonterminal ACC_IF_CLAUSE = new Nonterminal(61, "<acc if clause>");
        public static final Nonterminal ACC_DATA_LIST = new Nonterminal(62, "<acc data list>");
        public static final Nonterminal IDENTIFIER_LIST = new Nonterminal(63, "<identifier list>");
        public static final Nonterminal ACC_PARALLEL = new Nonterminal(64, "<acc parallel>");
        public static final Nonterminal ACC_HOSTDATA = new Nonterminal(65, "<acc hostdata>");
        public static final Nonterminal ACC_PARALLEL_CLAUSE = new Nonterminal(66, "<acc parallel clause>");
        public static final Nonterminal ACC_DATA_CLAUSE_LIST = new Nonterminal(67, "<acc data clause list>");
        public static final Nonterminal EQUALITY_EXPRESSION = new Nonterminal(68, "<equality expression>");
        public static final Nonterminal ACC_KERNELS_CLAUSE_LIST = new Nonterminal(69, "<acc kernels clause list>");
        public static final Nonterminal ACC_DECLARE_CLAUSE = new Nonterminal(70, "<acc declare clause>");
        public static final Nonterminal RELATIONAL_EXPRESSION = new Nonterminal(71, "<relational expression>");
        public static final Nonterminal ACC_PARALLEL_LOOP_CLAUSE_LIST = new Nonterminal(72, "<acc parallel loop clause list>");
        public static final Nonterminal ACC_KERNELS_LOOP = new Nonterminal(73, "<acc kernels loop>");
        public static final Nonterminal EXPRESSION = new Nonterminal(74, "<expression>");
        public static final Nonterminal ACC_UPDATE_CLAUSE = new Nonterminal(75, "<acc update clause>");
        public static final Nonterminal ASSIGNMENT_EXPRESSION = new Nonterminal(76, "<assignment expression>");
        public static final Nonterminal ARGUMENT_EXPRESSION_LIST = new Nonterminal(77, "<argument expression list>");
        public static final Nonterminal ACC_DECLARE_CLAUSE_LIST = new Nonterminal(78, "<acc declare clause list>");
        public static final Nonterminal ADDITIVE_EXPRESSION = new Nonterminal(79, "<additive expression>");
        public static final Nonterminal ACC_NUMGANGS_CLAUSE = new Nonterminal(80, "<acc numgangs clause>");
        public static final Nonterminal ACC_DATA_CLAUSE = new Nonterminal(81, "<acc data clause>");
        public static final Nonterminal EXCLUSIVE_OR_EXPRESSION = new Nonterminal(82, "<exclusive or expression>");
        public static final Nonterminal CONSTANT_EXPRESSION = new Nonterminal(83, "<constant expression>");
        public static final Nonterminal ACC_PRESENTORCOPYOUT_CLAUSE = new Nonterminal(84, "<acc presentorcopyout clause>");
        public static final Nonterminal MULTIPLICATIVE_EXPRESSION = new Nonterminal(85, "<multiplicative expression>");

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
        public static final Production ACC_CONSTRUCT_12 = new Production(Nonterminal.ACC_CONSTRUCT, 0, "<acc-construct> ::= (empty)");
        public static final Production ACC_LOOP_13 = new Production(Nonterminal.ACC_LOOP, 3, "<acc-loop> ::= PRAGMA_ACC literal-string-loop <acc-loop-clause-list>");
        public static final Production ACC_LOOP_14 = new Production(Nonterminal.ACC_LOOP, 2, "<acc-loop> ::= PRAGMA_ACC literal-string-loop");
        public static final Production ACC_LOOP_CLAUSE_LIST_15 = new Production(Nonterminal.ACC_LOOP_CLAUSE_LIST, 1, "<acc-loop-clause-list> ::= <acc-loop-clause>");
        public static final Production ACC_LOOP_CLAUSE_LIST_16 = new Production(Nonterminal.ACC_LOOP_CLAUSE_LIST, 2, "<acc-loop-clause-list> ::= <acc-loop-clause-list> <acc-loop-clause>");
        public static final Production ACC_LOOP_CLAUSE_LIST_17 = new Production(Nonterminal.ACC_LOOP_CLAUSE_LIST, 3, "<acc-loop-clause-list> ::= <acc-loop-clause-list> literal-string-comma <acc-loop-clause>");
        public static final Production ACC_LOOP_CLAUSE_18 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= <acc-collapse-clause>");
        public static final Production ACC_LOOP_CLAUSE_19 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= <acc-gang-clause>");
        public static final Production ACC_LOOP_CLAUSE_20 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= <acc-worker-clause>");
        public static final Production ACC_LOOP_CLAUSE_21 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= <acc-vector-clause>");
        public static final Production ACC_LOOP_CLAUSE_22 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= literal-string-seq");
        public static final Production ACC_LOOP_CLAUSE_23 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= literal-string-independent");
        public static final Production ACC_LOOP_CLAUSE_24 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= <acc-private-clause>");
        public static final Production ACC_LOOP_CLAUSE_25 = new Production(Nonterminal.ACC_LOOP_CLAUSE, 1, "<acc-loop-clause> ::= <acc-reduction-clause>");
        public static final Production ACC_PARALLEL_26 = new Production(Nonterminal.ACC_PARALLEL, 3, "<acc-parallel> ::= PRAGMA_ACC literal-string-parallel <acc-parallel-clause-list>");
        public static final Production ACC_PARALLEL_27 = new Production(Nonterminal.ACC_PARALLEL, 2, "<acc-parallel> ::= PRAGMA_ACC literal-string-parallel");
        public static final Production ACC_PARALLEL_CLAUSE_LIST_28 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE_LIST, 1, "<acc-parallel-clause-list> ::= <acc-parallel-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_LIST_29 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE_LIST, 2, "<acc-parallel-clause-list> ::= <acc-parallel-clause-list> <acc-parallel-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_LIST_30 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE_LIST, 3, "<acc-parallel-clause-list> ::= <acc-parallel-clause-list> literal-string-comma <acc-parallel-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_31 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-if-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_32 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-async-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_33 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-numgangs-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_34 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-numworkers-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_35 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-vectorlength-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_36 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-reduction-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_37 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-copy-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_38 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-copyin-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_39 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-copyout-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_40 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-create-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_41 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-present-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_42 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-presentorcopy-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_43 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-presentorcopyin-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_44 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-presentorcopyout-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_45 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-presentorcreate-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_46 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-deviceptr-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_47 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-private-clause>");
        public static final Production ACC_PARALLEL_CLAUSE_48 = new Production(Nonterminal.ACC_PARALLEL_CLAUSE, 1, "<acc-parallel-clause> ::= <acc-firstprivate-clause>");
        public static final Production ACC_PARALLEL_LOOP_49 = new Production(Nonterminal.ACC_PARALLEL_LOOP, 4, "<acc-parallel-loop> ::= PRAGMA_ACC literal-string-parallel literal-string-loop <acc-parallel-loop-clause-list>");
        public static final Production ACC_PARALLEL_LOOP_50 = new Production(Nonterminal.ACC_PARALLEL_LOOP, 3, "<acc-parallel-loop> ::= PRAGMA_ACC literal-string-parallel literal-string-loop");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_LIST_51 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE_LIST, 1, "<acc-parallel-loop-clause-list> ::= <acc-parallel-loop-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_LIST_52 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE_LIST, 2, "<acc-parallel-loop-clause-list> ::= <acc-parallel-loop-clause-list> <acc-parallel-loop-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_LIST_53 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE_LIST, 3, "<acc-parallel-loop-clause-list> ::= <acc-parallel-loop-clause-list> literal-string-comma <acc-parallel-loop-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_54 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-collapse-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_55 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-gang-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_56 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-worker-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_57 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-vector-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_58 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= literal-string-seq");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_59 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= literal-string-independent");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_60 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-private-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_61 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-reduction-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_62 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-if-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_63 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-async-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_64 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-numgangs-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_65 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-numworkers-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_66 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-vectorlength-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_67 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-copy-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_68 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-copyin-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_69 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-copyout-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_70 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-create-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_71 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-present-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_72 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-presentorcopy-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_73 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-presentorcopyin-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_74 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-presentorcopyout-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_75 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-presentorcreate-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_76 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-deviceptr-clause>");
        public static final Production ACC_PARALLEL_LOOP_CLAUSE_77 = new Production(Nonterminal.ACC_PARALLEL_LOOP_CLAUSE, 1, "<acc-parallel-loop-clause> ::= <acc-firstprivate-clause>");
        public static final Production ACC_KERNELS_78 = new Production(Nonterminal.ACC_KERNELS, 3, "<acc-kernels> ::= PRAGMA_ACC literal-string-kernels <acc-kernels-clause-list>");
        public static final Production ACC_KERNELS_79 = new Production(Nonterminal.ACC_KERNELS, 2, "<acc-kernels> ::= PRAGMA_ACC literal-string-kernels");
        public static final Production ACC_KERNELS_CLAUSE_LIST_80 = new Production(Nonterminal.ACC_KERNELS_CLAUSE_LIST, 1, "<acc-kernels-clause-list> ::= <acc-kernels-clause>");
        public static final Production ACC_KERNELS_CLAUSE_LIST_81 = new Production(Nonterminal.ACC_KERNELS_CLAUSE_LIST, 2, "<acc-kernels-clause-list> ::= <acc-kernels-clause-list> <acc-kernels-clause>");
        public static final Production ACC_KERNELS_CLAUSE_LIST_82 = new Production(Nonterminal.ACC_KERNELS_CLAUSE_LIST, 3, "<acc-kernels-clause-list> ::= <acc-kernels-clause-list> literal-string-comma <acc-kernels-clause>");
        public static final Production ACC_KERNELS_CLAUSE_83 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-if-clause>");
        public static final Production ACC_KERNELS_CLAUSE_84 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-async-clause>");
        public static final Production ACC_KERNELS_CLAUSE_85 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-copy-clause>");
        public static final Production ACC_KERNELS_CLAUSE_86 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-copyin-clause>");
        public static final Production ACC_KERNELS_CLAUSE_87 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-copyout-clause>");
        public static final Production ACC_KERNELS_CLAUSE_88 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-create-clause>");
        public static final Production ACC_KERNELS_CLAUSE_89 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-present-clause>");
        public static final Production ACC_KERNELS_CLAUSE_90 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-presentorcopy-clause>");
        public static final Production ACC_KERNELS_CLAUSE_91 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-presentorcopyin-clause>");
        public static final Production ACC_KERNELS_CLAUSE_92 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-presentorcopyout-clause>");
        public static final Production ACC_KERNELS_CLAUSE_93 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-presentorcreate-clause>");
        public static final Production ACC_KERNELS_CLAUSE_94 = new Production(Nonterminal.ACC_KERNELS_CLAUSE, 1, "<acc-kernels-clause> ::= <acc-deviceptr-clause>");
        public static final Production ACC_KERNELS_LOOP_95 = new Production(Nonterminal.ACC_KERNELS_LOOP, 4, "<acc-kernels-loop> ::= PRAGMA_ACC literal-string-kernels literal-string-loop <acc-kernels-loop-clause-list>");
        public static final Production ACC_KERNELS_LOOP_96 = new Production(Nonterminal.ACC_KERNELS_LOOP, 3, "<acc-kernels-loop> ::= PRAGMA_ACC literal-string-kernels literal-string-loop");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_LIST_97 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE_LIST, 1, "<acc-kernels-loop-clause-list> ::= <acc-kernels-loop-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_LIST_98 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE_LIST, 2, "<acc-kernels-loop-clause-list> ::= <acc-kernels-loop-clause-list> <acc-kernels-loop-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_LIST_99 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE_LIST, 3, "<acc-kernels-loop-clause-list> ::= <acc-kernels-loop-clause-list> literal-string-comma <acc-kernels-loop-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_100 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-collapse-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_101 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-gang-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_102 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-worker-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_103 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-vector-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_104 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= literal-string-seq");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_105 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= literal-string-independent");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_106 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-private-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_107 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-reduction-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_108 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-if-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_109 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-async-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_110 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-copy-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_111 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-copyin-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_112 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-copyout-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_113 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-create-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_114 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-present-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_115 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-presentorcopy-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_116 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-presentorcopyin-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_117 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-presentorcopyout-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_118 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-presentorcreate-clause>");
        public static final Production ACC_KERNELS_LOOP_CLAUSE_119 = new Production(Nonterminal.ACC_KERNELS_LOOP_CLAUSE, 1, "<acc-kernels-loop-clause> ::= <acc-deviceptr-clause>");
        public static final Production ACC_DECLARE_120 = new Production(Nonterminal.ACC_DECLARE, 3, "<acc-declare> ::= PRAGMA_ACC literal-string-declare <acc-declare-clause-list>");
        public static final Production ACC_DECLARE_CLAUSE_LIST_121 = new Production(Nonterminal.ACC_DECLARE_CLAUSE_LIST, 1, "<acc-declare-clause-list> ::= <acc-declare-clause>");
        public static final Production ACC_DECLARE_CLAUSE_LIST_122 = new Production(Nonterminal.ACC_DECLARE_CLAUSE_LIST, 2, "<acc-declare-clause-list> ::= <acc-declare-clause-list> <acc-declare-clause>");
        public static final Production ACC_DECLARE_CLAUSE_LIST_123 = new Production(Nonterminal.ACC_DECLARE_CLAUSE_LIST, 3, "<acc-declare-clause-list> ::= <acc-declare-clause-list> literal-string-comma <acc-declare-clause>");
        public static final Production ACC_DECLARE_CLAUSE_124 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-copy-clause>");
        public static final Production ACC_DECLARE_CLAUSE_125 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-copyin-clause>");
        public static final Production ACC_DECLARE_CLAUSE_126 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-copyout-clause>");
        public static final Production ACC_DECLARE_CLAUSE_127 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-create-clause>");
        public static final Production ACC_DECLARE_CLAUSE_128 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-present-clause>");
        public static final Production ACC_DECLARE_CLAUSE_129 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-presentorcopy-clause>");
        public static final Production ACC_DECLARE_CLAUSE_130 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-presentorcopyin-clause>");
        public static final Production ACC_DECLARE_CLAUSE_131 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-presentorcopyout-clause>");
        public static final Production ACC_DECLARE_CLAUSE_132 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-presentorcreate-clause>");
        public static final Production ACC_DECLARE_CLAUSE_133 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-deviceptr-clause>");
        public static final Production ACC_DECLARE_CLAUSE_134 = new Production(Nonterminal.ACC_DECLARE_CLAUSE, 1, "<acc-declare-clause> ::= <acc-deviceresident-clause>");
        public static final Production ACC_DATA_135 = new Production(Nonterminal.ACC_DATA, 3, "<acc-data> ::= PRAGMA_ACC literal-string-data <acc-data-clause-list>");
        public static final Production ACC_DATA_CLAUSE_LIST_136 = new Production(Nonterminal.ACC_DATA_CLAUSE_LIST, 1, "<acc-data-clause-list> ::= <acc-data-clause>");
        public static final Production ACC_DATA_CLAUSE_LIST_137 = new Production(Nonterminal.ACC_DATA_CLAUSE_LIST, 2, "<acc-data-clause-list> ::= <acc-data-clause-list> <acc-data-clause>");
        public static final Production ACC_DATA_CLAUSE_LIST_138 = new Production(Nonterminal.ACC_DATA_CLAUSE_LIST, 3, "<acc-data-clause-list> ::= <acc-data-clause-list> literal-string-comma <acc-data-clause>");
        public static final Production ACC_DATA_CLAUSE_139 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-if-clause>");
        public static final Production ACC_DATA_CLAUSE_140 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-copy-clause>");
        public static final Production ACC_DATA_CLAUSE_141 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-copyin-clause>");
        public static final Production ACC_DATA_CLAUSE_142 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-copyout-clause>");
        public static final Production ACC_DATA_CLAUSE_143 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-create-clause>");
        public static final Production ACC_DATA_CLAUSE_144 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-present-clause>");
        public static final Production ACC_DATA_CLAUSE_145 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-presentorcopy-clause>");
        public static final Production ACC_DATA_CLAUSE_146 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-presentorcopyin-clause>");
        public static final Production ACC_DATA_CLAUSE_147 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-presentorcopyout-clause>");
        public static final Production ACC_DATA_CLAUSE_148 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-presentorcreate-clause>");
        public static final Production ACC_DATA_CLAUSE_149 = new Production(Nonterminal.ACC_DATA_CLAUSE, 1, "<acc-data-clause> ::= <acc-deviceptr-clause>");
        public static final Production ACC_HOSTDATA_150 = new Production(Nonterminal.ACC_HOSTDATA, 3, "<acc-hostdata> ::= PRAGMA_ACC literal-string-host-underscoredata <acc-hostdata-clause-list>");
        public static final Production ACC_HOSTDATA_CLAUSE_LIST_151 = new Production(Nonterminal.ACC_HOSTDATA_CLAUSE_LIST, 1, "<acc-hostdata-clause-list> ::= <acc-hostdata-clause>");
        public static final Production ACC_HOSTDATA_CLAUSE_LIST_152 = new Production(Nonterminal.ACC_HOSTDATA_CLAUSE_LIST, 2, "<acc-hostdata-clause-list> ::= <acc-hostdata-clause-list> <acc-hostdata-clause>");
        public static final Production ACC_HOSTDATA_CLAUSE_LIST_153 = new Production(Nonterminal.ACC_HOSTDATA_CLAUSE_LIST, 3, "<acc-hostdata-clause-list> ::= <acc-hostdata-clause-list> literal-string-comma <acc-hostdata-clause>");
        public static final Production ACC_HOSTDATA_CLAUSE_154 = new Production(Nonterminal.ACC_HOSTDATA_CLAUSE, 1, "<acc-hostdata-clause> ::= <acc-usedevice-clause>");
        public static final Production ACC_CACHE_155 = new Production(Nonterminal.ACC_CACHE, 5, "<acc-cache> ::= PRAGMA_ACC literal-string-cache literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_WAIT_156 = new Production(Nonterminal.ACC_WAIT, 3, "<acc-wait> ::= PRAGMA_ACC literal-string-wait <acc-wait-parameter>");
        public static final Production ACC_WAIT_157 = new Production(Nonterminal.ACC_WAIT, 2, "<acc-wait> ::= PRAGMA_ACC literal-string-wait");
        public static final Production ACC_WAIT_PARAMETER_158 = new Production(Nonterminal.ACC_WAIT_PARAMETER, 3, "<acc-wait-parameter> ::= literal-string-lparen <constant-expression> literal-string-rparen");
        public static final Production ACC_UPDATE_159 = new Production(Nonterminal.ACC_UPDATE, 3, "<acc-update> ::= PRAGMA_ACC literal-string-update <acc-update-clause-list>");
        public static final Production ACC_UPDATE_CLAUSE_LIST_160 = new Production(Nonterminal.ACC_UPDATE_CLAUSE_LIST, 1, "<acc-update-clause-list> ::= <acc-update-clause>");
        public static final Production ACC_UPDATE_CLAUSE_LIST_161 = new Production(Nonterminal.ACC_UPDATE_CLAUSE_LIST, 2, "<acc-update-clause-list> ::= <acc-update-clause-list> <acc-update-clause>");
        public static final Production ACC_UPDATE_CLAUSE_LIST_162 = new Production(Nonterminal.ACC_UPDATE_CLAUSE_LIST, 3, "<acc-update-clause-list> ::= <acc-update-clause-list> literal-string-comma <acc-update-clause>");
        public static final Production ACC_UPDATE_CLAUSE_163 = new Production(Nonterminal.ACC_UPDATE_CLAUSE, 1, "<acc-update-clause> ::= <acc-host-clause>");
        public static final Production ACC_UPDATE_CLAUSE_164 = new Production(Nonterminal.ACC_UPDATE_CLAUSE, 1, "<acc-update-clause> ::= <acc-device-clause>");
        public static final Production ACC_UPDATE_CLAUSE_165 = new Production(Nonterminal.ACC_UPDATE_CLAUSE, 1, "<acc-update-clause> ::= <acc-if-clause>");
        public static final Production ACC_UPDATE_CLAUSE_166 = new Production(Nonterminal.ACC_UPDATE_CLAUSE, 1, "<acc-update-clause> ::= <acc-async-clause>");
        public static final Production ACC_COLLAPSE_CLAUSE_167 = new Production(Nonterminal.ACC_COLLAPSE_CLAUSE, 2, "<acc-collapse-clause> ::= literal-string-collapse <acc-count>");
        public static final Production ACC_GANG_CLAUSE_168 = new Production(Nonterminal.ACC_GANG_CLAUSE, 2, "<acc-gang-clause> ::= literal-string-gang <acc-count>");
        public static final Production ACC_GANG_CLAUSE_169 = new Production(Nonterminal.ACC_GANG_CLAUSE, 1, "<acc-gang-clause> ::= literal-string-gang");
        public static final Production ACC_WORKER_CLAUSE_170 = new Production(Nonterminal.ACC_WORKER_CLAUSE, 2, "<acc-worker-clause> ::= literal-string-worker <acc-count>");
        public static final Production ACC_WORKER_CLAUSE_171 = new Production(Nonterminal.ACC_WORKER_CLAUSE, 1, "<acc-worker-clause> ::= literal-string-worker");
        public static final Production ACC_VECTOR_CLAUSE_172 = new Production(Nonterminal.ACC_VECTOR_CLAUSE, 2, "<acc-vector-clause> ::= literal-string-vector <acc-count>");
        public static final Production ACC_VECTOR_CLAUSE_173 = new Production(Nonterminal.ACC_VECTOR_CLAUSE, 1, "<acc-vector-clause> ::= literal-string-vector");
        public static final Production ACC_PRIVATE_CLAUSE_174 = new Production(Nonterminal.ACC_PRIVATE_CLAUSE, 4, "<acc-private-clause> ::= literal-string-private literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_FIRSTPRIVATE_CLAUSE_175 = new Production(Nonterminal.ACC_FIRSTPRIVATE_CLAUSE, 4, "<acc-firstprivate-clause> ::= literal-string-firstprivate literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_REDUCTION_CLAUSE_176 = new Production(Nonterminal.ACC_REDUCTION_CLAUSE, 6, "<acc-reduction-clause> ::= literal-string-reduction literal-string-lparen <acc-reduction-operator> literal-string-colon <identifier-list> literal-string-rparen");
        public static final Production ACC_IF_CLAUSE_177 = new Production(Nonterminal.ACC_IF_CLAUSE, 4, "<acc-if-clause> ::= literal-string-if literal-string-lparen <conditional-expression> literal-string-rparen");
        public static final Production ACC_ASYNC_CLAUSE_178 = new Production(Nonterminal.ACC_ASYNC_CLAUSE, 2, "<acc-async-clause> ::= literal-string-async <acc-count>");
        public static final Production ACC_ASYNC_CLAUSE_179 = new Production(Nonterminal.ACC_ASYNC_CLAUSE, 1, "<acc-async-clause> ::= literal-string-async");
        public static final Production ACC_NUMGANGS_CLAUSE_180 = new Production(Nonterminal.ACC_NUMGANGS_CLAUSE, 2, "<acc-numgangs-clause> ::= literal-string-num-underscoregangs <acc-count>");
        public static final Production ACC_NUMWORKERS_CLAUSE_181 = new Production(Nonterminal.ACC_NUMWORKERS_CLAUSE, 2, "<acc-numworkers-clause> ::= literal-string-num-underscoreworkers <acc-count>");
        public static final Production ACC_VECTORLENGTH_CLAUSE_182 = new Production(Nonterminal.ACC_VECTORLENGTH_CLAUSE, 2, "<acc-vectorlength-clause> ::= literal-string-vector-underscorelength <acc-count>");
        public static final Production ACC_COPY_CLAUSE_183 = new Production(Nonterminal.ACC_COPY_CLAUSE, 4, "<acc-copy-clause> ::= literal-string-copy literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_COPYIN_CLAUSE_184 = new Production(Nonterminal.ACC_COPYIN_CLAUSE, 4, "<acc-copyin-clause> ::= literal-string-copyin literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_COPYOUT_CLAUSE_185 = new Production(Nonterminal.ACC_COPYOUT_CLAUSE, 4, "<acc-copyout-clause> ::= literal-string-copyout literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_CREATE_CLAUSE_186 = new Production(Nonterminal.ACC_CREATE_CLAUSE, 4, "<acc-create-clause> ::= literal-string-create literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENT_CLAUSE_187 = new Production(Nonterminal.ACC_PRESENT_CLAUSE, 4, "<acc-present-clause> ::= literal-string-present literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCOPY_CLAUSE_188 = new Production(Nonterminal.ACC_PRESENTORCOPY_CLAUSE, 4, "<acc-presentorcopy-clause> ::= literal-string-present-underscoreor-underscorecopy literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCOPY_CLAUSE_189 = new Production(Nonterminal.ACC_PRESENTORCOPY_CLAUSE, 4, "<acc-presentorcopy-clause> ::= literal-string-pcopy literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCOPYIN_CLAUSE_190 = new Production(Nonterminal.ACC_PRESENTORCOPYIN_CLAUSE, 4, "<acc-presentorcopyin-clause> ::= literal-string-present-underscoreor-underscorecopyin literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCOPYIN_CLAUSE_191 = new Production(Nonterminal.ACC_PRESENTORCOPYIN_CLAUSE, 4, "<acc-presentorcopyin-clause> ::= literal-string-pcopyin literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCOPYOUT_CLAUSE_192 = new Production(Nonterminal.ACC_PRESENTORCOPYOUT_CLAUSE, 4, "<acc-presentorcopyout-clause> ::= literal-string-present-underscoreor-underscorecopyout literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCOPYOUT_CLAUSE_193 = new Production(Nonterminal.ACC_PRESENTORCOPYOUT_CLAUSE, 4, "<acc-presentorcopyout-clause> ::= literal-string-pcopyout literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCREATE_CLAUSE_194 = new Production(Nonterminal.ACC_PRESENTORCREATE_CLAUSE, 4, "<acc-presentorcreate-clause> ::= literal-string-present-underscoreor-underscorecreate literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_PRESENTORCREATE_CLAUSE_195 = new Production(Nonterminal.ACC_PRESENTORCREATE_CLAUSE, 4, "<acc-presentorcreate-clause> ::= literal-string-pcreate literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_DEVICEPTR_CLAUSE_196 = new Production(Nonterminal.ACC_DEVICEPTR_CLAUSE, 4, "<acc-deviceptr-clause> ::= literal-string-deviceptr literal-string-lparen <identifier-list> literal-string-rparen");
        public static final Production ACC_DEVICERESIDENT_CLAUSE_197 = new Production(Nonterminal.ACC_DEVICERESIDENT_CLAUSE, 4, "<acc-deviceresident-clause> ::= literal-string-device-underscoreresident literal-string-lparen <identifier-list> literal-string-rparen");
        public static final Production ACC_USEDEVICE_CLAUSE_198 = new Production(Nonterminal.ACC_USEDEVICE_CLAUSE, 4, "<acc-usedevice-clause> ::= literal-string-use-underscoredevice literal-string-lparen <identifier-list> literal-string-rparen");
        public static final Production ACC_HOST_CLAUSE_199 = new Production(Nonterminal.ACC_HOST_CLAUSE, 4, "<acc-host-clause> ::= literal-string-host literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_DEVICE_CLAUSE_200 = new Production(Nonterminal.ACC_DEVICE_CLAUSE, 4, "<acc-device-clause> ::= literal-string-device literal-string-lparen <acc-data-list> literal-string-rparen");
        public static final Production ACC_COUNT_201 = new Production(Nonterminal.ACC_COUNT, 3, "<acc-count> ::= literal-string-lparen <constant-expression> literal-string-rparen");
        public static final Production ACC_REDUCTION_OPERATOR_202 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-plus");
        public static final Production ACC_REDUCTION_OPERATOR_203 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-asterisk");
        public static final Production ACC_REDUCTION_OPERATOR_204 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-min");
        public static final Production ACC_REDUCTION_OPERATOR_205 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-max");
        public static final Production ACC_REDUCTION_OPERATOR_206 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-ampersand");
        public static final Production ACC_REDUCTION_OPERATOR_207 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-vbar");
        public static final Production ACC_REDUCTION_OPERATOR_208 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-caret");
        public static final Production ACC_REDUCTION_OPERATOR_209 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-ampersand-ampersand");
        public static final Production ACC_REDUCTION_OPERATOR_210 = new Production(Nonterminal.ACC_REDUCTION_OPERATOR, 1, "<acc-reduction-operator> ::= literal-string-vbar-vbar");
        public static final Production ACC_DATA_LIST_211 = new Production(Nonterminal.ACC_DATA_LIST, 1, "<acc-data-list> ::= <acc-data-item>");
        public static final Production ACC_DATA_LIST_212 = new Production(Nonterminal.ACC_DATA_LIST, 3, "<acc-data-list> ::= <acc-data-list> literal-string-comma <acc-data-item>");
        public static final Production ACC_DATA_ITEM_213 = new Production(Nonterminal.ACC_DATA_ITEM, 1, "<acc-data-item> ::= <identifier>");
        public static final Production ACC_DATA_ITEM_214 = new Production(Nonterminal.ACC_DATA_ITEM, 6, "<acc-data-item> ::= <identifier> literal-string-lbracket <constant-expression> literal-string-colon <constant-expression> literal-string-rbracket");
        public static final Production PRIMARY_EXPRESSION_215 = new Production(Nonterminal.PRIMARY_EXPRESSION, 1, "<primary-expression> ::= <identifier>");
        public static final Production PRIMARY_EXPRESSION_216 = new Production(Nonterminal.PRIMARY_EXPRESSION, 1, "<primary-expression> ::= <constant>");
        public static final Production PRIMARY_EXPRESSION_217 = new Production(Nonterminal.PRIMARY_EXPRESSION, 1, "<primary-expression> ::= <STRING-LITERAL-terminal-list>");
        public static final Production PRIMARY_EXPRESSION_218 = new Production(Nonterminal.PRIMARY_EXPRESSION, 3, "<primary-expression> ::= literal-string-lparen <expression> literal-string-rparen");
        public static final Production POSTFIX_EXPRESSION_219 = new Production(Nonterminal.POSTFIX_EXPRESSION, 1, "<postfix-expression> ::= <primary-expression>");
        public static final Production POSTFIX_EXPRESSION_220 = new Production(Nonterminal.POSTFIX_EXPRESSION, 4, "<postfix-expression> ::= <postfix-expression> literal-string-lbracket <expression> literal-string-rbracket");
        public static final Production POSTFIX_EXPRESSION_221 = new Production(Nonterminal.POSTFIX_EXPRESSION, 4, "<postfix-expression> ::= <postfix-expression> literal-string-lparen <argument-expression-list> literal-string-rparen");
        public static final Production POSTFIX_EXPRESSION_222 = new Production(Nonterminal.POSTFIX_EXPRESSION, 3, "<postfix-expression> ::= <postfix-expression> literal-string-lparen literal-string-rparen");
        public static final Production POSTFIX_EXPRESSION_223 = new Production(Nonterminal.POSTFIX_EXPRESSION, 3, "<postfix-expression> ::= <postfix-expression> literal-string-period <identifier>");
        public static final Production POSTFIX_EXPRESSION_224 = new Production(Nonterminal.POSTFIX_EXPRESSION, 3, "<postfix-expression> ::= <postfix-expression> literal-string-hyphen-greaterthan <identifier>");
        public static final Production POSTFIX_EXPRESSION_225 = new Production(Nonterminal.POSTFIX_EXPRESSION, 2, "<postfix-expression> ::= <postfix-expression> literal-string-plus-plus");
        public static final Production POSTFIX_EXPRESSION_226 = new Production(Nonterminal.POSTFIX_EXPRESSION, 2, "<postfix-expression> ::= <postfix-expression> literal-string-hyphen-hyphen");
        public static final Production ARGUMENT_EXPRESSION_LIST_227 = new Production(Nonterminal.ARGUMENT_EXPRESSION_LIST, 1, "<argument-expression-list> ::= <assignment-expression>");
        public static final Production ARGUMENT_EXPRESSION_LIST_228 = new Production(Nonterminal.ARGUMENT_EXPRESSION_LIST, 3, "<argument-expression-list> ::= <argument-expression-list> literal-string-comma <assignment-expression>");
        public static final Production UNARY_EXPRESSION_229 = new Production(Nonterminal.UNARY_EXPRESSION, 1, "<unary-expression> ::= <postfix-expression>");
        public static final Production UNARY_EXPRESSION_230 = new Production(Nonterminal.UNARY_EXPRESSION, 2, "<unary-expression> ::= literal-string-plus-plus <unary-expression>");
        public static final Production UNARY_EXPRESSION_231 = new Production(Nonterminal.UNARY_EXPRESSION, 2, "<unary-expression> ::= literal-string-hyphen-hyphen <unary-expression>");
        public static final Production UNARY_EXPRESSION_232 = new Production(Nonterminal.UNARY_EXPRESSION, 2, "<unary-expression> ::= <unary-operator> <cast-expression>");
        public static final Production UNARY_EXPRESSION_233 = new Production(Nonterminal.UNARY_EXPRESSION, 2, "<unary-expression> ::= literal-string-sizeof <unary-expression>");
        public static final Production UNARY_OPERATOR_234 = new Production(Nonterminal.UNARY_OPERATOR, 1, "<unary-operator> ::= literal-string-ampersand");
        public static final Production UNARY_OPERATOR_235 = new Production(Nonterminal.UNARY_OPERATOR, 1, "<unary-operator> ::= literal-string-asterisk");
        public static final Production UNARY_OPERATOR_236 = new Production(Nonterminal.UNARY_OPERATOR, 1, "<unary-operator> ::= literal-string-plus");
        public static final Production UNARY_OPERATOR_237 = new Production(Nonterminal.UNARY_OPERATOR, 1, "<unary-operator> ::= literal-string-hyphen");
        public static final Production UNARY_OPERATOR_238 = new Production(Nonterminal.UNARY_OPERATOR, 1, "<unary-operator> ::= literal-string-tilde");
        public static final Production UNARY_OPERATOR_239 = new Production(Nonterminal.UNARY_OPERATOR, 1, "<unary-operator> ::= literal-string-exclamation");
        public static final Production CAST_EXPRESSION_240 = new Production(Nonterminal.CAST_EXPRESSION, 1, "<cast-expression> ::= <unary-expression>");
        public static final Production MULTIPLICATIVE_EXPRESSION_241 = new Production(Nonterminal.MULTIPLICATIVE_EXPRESSION, 1, "<multiplicative-expression> ::= <cast-expression>");
        public static final Production MULTIPLICATIVE_EXPRESSION_242 = new Production(Nonterminal.MULTIPLICATIVE_EXPRESSION, 3, "<multiplicative-expression> ::= <multiplicative-expression> literal-string-asterisk <cast-expression>");
        public static final Production MULTIPLICATIVE_EXPRESSION_243 = new Production(Nonterminal.MULTIPLICATIVE_EXPRESSION, 3, "<multiplicative-expression> ::= <multiplicative-expression> literal-string-slash <cast-expression>");
        public static final Production MULTIPLICATIVE_EXPRESSION_244 = new Production(Nonterminal.MULTIPLICATIVE_EXPRESSION, 3, "<multiplicative-expression> ::= <multiplicative-expression> literal-string-percent <cast-expression>");
        public static final Production ADDITIVE_EXPRESSION_245 = new Production(Nonterminal.ADDITIVE_EXPRESSION, 1, "<additive-expression> ::= <multiplicative-expression>");
        public static final Production ADDITIVE_EXPRESSION_246 = new Production(Nonterminal.ADDITIVE_EXPRESSION, 3, "<additive-expression> ::= <additive-expression> literal-string-plus <multiplicative-expression>");
        public static final Production ADDITIVE_EXPRESSION_247 = new Production(Nonterminal.ADDITIVE_EXPRESSION, 3, "<additive-expression> ::= <additive-expression> literal-string-hyphen <multiplicative-expression>");
        public static final Production SHIFT_EXPRESSION_248 = new Production(Nonterminal.SHIFT_EXPRESSION, 1, "<shift-expression> ::= <additive-expression>");
        public static final Production SHIFT_EXPRESSION_249 = new Production(Nonterminal.SHIFT_EXPRESSION, 3, "<shift-expression> ::= <shift-expression> literal-string-lessthan-lessthan <additive-expression>");
        public static final Production SHIFT_EXPRESSION_250 = new Production(Nonterminal.SHIFT_EXPRESSION, 3, "<shift-expression> ::= <shift-expression> literal-string-greaterthan-greaterthan <additive-expression>");
        public static final Production RELATIONAL_EXPRESSION_251 = new Production(Nonterminal.RELATIONAL_EXPRESSION, 1, "<relational-expression> ::= <shift-expression>");
        public static final Production RELATIONAL_EXPRESSION_252 = new Production(Nonterminal.RELATIONAL_EXPRESSION, 3, "<relational-expression> ::= <relational-expression> literal-string-lessthan <shift-expression>");
        public static final Production RELATIONAL_EXPRESSION_253 = new Production(Nonterminal.RELATIONAL_EXPRESSION, 3, "<relational-expression> ::= <relational-expression> literal-string-greaterthan <shift-expression>");
        public static final Production RELATIONAL_EXPRESSION_254 = new Production(Nonterminal.RELATIONAL_EXPRESSION, 3, "<relational-expression> ::= <relational-expression> literal-string-lessthan-equals <shift-expression>");
        public static final Production RELATIONAL_EXPRESSION_255 = new Production(Nonterminal.RELATIONAL_EXPRESSION, 3, "<relational-expression> ::= <relational-expression> literal-string-greaterthan-equals <shift-expression>");
        public static final Production EQUALITY_EXPRESSION_256 = new Production(Nonterminal.EQUALITY_EXPRESSION, 1, "<equality-expression> ::= <relational-expression>");
        public static final Production EQUALITY_EXPRESSION_257 = new Production(Nonterminal.EQUALITY_EXPRESSION, 3, "<equality-expression> ::= <equality-expression> literal-string-equals-equals <relational-expression>");
        public static final Production EQUALITY_EXPRESSION_258 = new Production(Nonterminal.EQUALITY_EXPRESSION, 3, "<equality-expression> ::= <equality-expression> literal-string-exclamation-equals <relational-expression>");
        public static final Production AND_EXPRESSION_259 = new Production(Nonterminal.AND_EXPRESSION, 1, "<and-expression> ::= <equality-expression>");
        public static final Production AND_EXPRESSION_260 = new Production(Nonterminal.AND_EXPRESSION, 3, "<and-expression> ::= <and-expression> literal-string-ampersand <equality-expression>");
        public static final Production EXCLUSIVE_OR_EXPRESSION_261 = new Production(Nonterminal.EXCLUSIVE_OR_EXPRESSION, 1, "<exclusive-or-expression> ::= <and-expression>");
        public static final Production EXCLUSIVE_OR_EXPRESSION_262 = new Production(Nonterminal.EXCLUSIVE_OR_EXPRESSION, 3, "<exclusive-or-expression> ::= <exclusive-or-expression> literal-string-caret <and-expression>");
        public static final Production INCLUSIVE_OR_EXPRESSION_263 = new Production(Nonterminal.INCLUSIVE_OR_EXPRESSION, 1, "<inclusive-or-expression> ::= <exclusive-or-expression>");
        public static final Production INCLUSIVE_OR_EXPRESSION_264 = new Production(Nonterminal.INCLUSIVE_OR_EXPRESSION, 3, "<inclusive-or-expression> ::= <inclusive-or-expression> literal-string-vbar <exclusive-or-expression>");
        public static final Production LOGICAL_AND_EXPRESSION_265 = new Production(Nonterminal.LOGICAL_AND_EXPRESSION, 1, "<logical-and-expression> ::= <inclusive-or-expression>");
        public static final Production LOGICAL_AND_EXPRESSION_266 = new Production(Nonterminal.LOGICAL_AND_EXPRESSION, 3, "<logical-and-expression> ::= <logical-and-expression> literal-string-ampersand-ampersand <inclusive-or-expression>");
        public static final Production LOGICAL_OR_EXPRESSION_267 = new Production(Nonterminal.LOGICAL_OR_EXPRESSION, 1, "<logical-or-expression> ::= <logical-and-expression>");
        public static final Production LOGICAL_OR_EXPRESSION_268 = new Production(Nonterminal.LOGICAL_OR_EXPRESSION, 3, "<logical-or-expression> ::= <logical-or-expression> literal-string-vbar-vbar <logical-and-expression>");
        public static final Production CONDITIONAL_EXPRESSION_269 = new Production(Nonterminal.CONDITIONAL_EXPRESSION, 1, "<conditional-expression> ::= <logical-or-expression>");
        public static final Production CONDITIONAL_EXPRESSION_270 = new Production(Nonterminal.CONDITIONAL_EXPRESSION, 5, "<conditional-expression> ::= <logical-or-expression> literal-string-question <expression> literal-string-colon <conditional-expression>");
        public static final Production CONSTANT_271 = new Production(Nonterminal.CONSTANT, 1, "<constant> ::= INTEGER-CONSTANT");
        public static final Production CONSTANT_272 = new Production(Nonterminal.CONSTANT, 1, "<constant> ::= FLOATING-CONSTANT");
        public static final Production CONSTANT_273 = new Production(Nonterminal.CONSTANT, 1, "<constant> ::= CHARACTER-CONSTANT");
        public static final Production EXPRESSION_274 = new Production(Nonterminal.EXPRESSION, 1, "<expression> ::= <conditional-expression>");
        public static final Production ASSIGNMENT_EXPRESSION_275 = new Production(Nonterminal.ASSIGNMENT_EXPRESSION, 1, "<assignment-expression> ::= <conditional-expression>");
        public static final Production CONSTANT_EXPRESSION_276 = new Production(Nonterminal.CONSTANT_EXPRESSION, 1, "<constant-expression> ::= <conditional-expression>");
        public static final Production IDENTIFIER_LIST_277 = new Production(Nonterminal.IDENTIFIER_LIST, 1, "<identifier-list> ::= <identifier>");
        public static final Production IDENTIFIER_LIST_278 = new Production(Nonterminal.IDENTIFIER_LIST, 3, "<identifier-list> ::= <identifier-list> literal-string-comma <identifier>");
        public static final Production IDENTIFIER_279 = new Production(Nonterminal.IDENTIFIER, 1, "<identifier> ::= IDENTIFIER");
        public static final Production IDENTIFIER_280 = new Production(Nonterminal.IDENTIFIER, 1, "<identifier> ::= literal-string-data");
        public static final Production STRING_LITERAL_TERMINAL_LIST_281 = new Production(Nonterminal.STRING_LITERAL_TERMINAL_LIST, 2, "<STRING-LITERAL-terminal-list> ::= <STRING-LITERAL-terminal-list> STRING-LITERAL");
        public static final Production STRING_LITERAL_TERMINAL_LIST_282 = new Production(Nonterminal.STRING_LITERAL_TERMINAL_LIST, 1, "<STRING-LITERAL-terminal-list> ::= STRING-LITERAL");

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
        protected static final int ACC_LOOP_13_INDEX = 13;
        protected static final int ACC_LOOP_14_INDEX = 14;
        protected static final int ACC_LOOP_CLAUSE_LIST_15_INDEX = 15;
        protected static final int ACC_LOOP_CLAUSE_LIST_16_INDEX = 16;
        protected static final int ACC_LOOP_CLAUSE_LIST_17_INDEX = 17;
        protected static final int ACC_LOOP_CLAUSE_18_INDEX = 18;
        protected static final int ACC_LOOP_CLAUSE_19_INDEX = 19;
        protected static final int ACC_LOOP_CLAUSE_20_INDEX = 20;
        protected static final int ACC_LOOP_CLAUSE_21_INDEX = 21;
        protected static final int ACC_LOOP_CLAUSE_22_INDEX = 22;
        protected static final int ACC_LOOP_CLAUSE_23_INDEX = 23;
        protected static final int ACC_LOOP_CLAUSE_24_INDEX = 24;
        protected static final int ACC_LOOP_CLAUSE_25_INDEX = 25;
        protected static final int ACC_PARALLEL_26_INDEX = 26;
        protected static final int ACC_PARALLEL_27_INDEX = 27;
        protected static final int ACC_PARALLEL_CLAUSE_LIST_28_INDEX = 28;
        protected static final int ACC_PARALLEL_CLAUSE_LIST_29_INDEX = 29;
        protected static final int ACC_PARALLEL_CLAUSE_LIST_30_INDEX = 30;
        protected static final int ACC_PARALLEL_CLAUSE_31_INDEX = 31;
        protected static final int ACC_PARALLEL_CLAUSE_32_INDEX = 32;
        protected static final int ACC_PARALLEL_CLAUSE_33_INDEX = 33;
        protected static final int ACC_PARALLEL_CLAUSE_34_INDEX = 34;
        protected static final int ACC_PARALLEL_CLAUSE_35_INDEX = 35;
        protected static final int ACC_PARALLEL_CLAUSE_36_INDEX = 36;
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
        protected static final int ACC_PARALLEL_LOOP_49_INDEX = 49;
        protected static final int ACC_PARALLEL_LOOP_50_INDEX = 50;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_LIST_51_INDEX = 51;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_LIST_52_INDEX = 52;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_LIST_53_INDEX = 53;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_54_INDEX = 54;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_55_INDEX = 55;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_56_INDEX = 56;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_57_INDEX = 57;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_58_INDEX = 58;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_59_INDEX = 59;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_60_INDEX = 60;
        protected static final int ACC_PARALLEL_LOOP_CLAUSE_61_INDEX = 61;
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
        protected static final int ACC_KERNELS_78_INDEX = 78;
        protected static final int ACC_KERNELS_79_INDEX = 79;
        protected static final int ACC_KERNELS_CLAUSE_LIST_80_INDEX = 80;
        protected static final int ACC_KERNELS_CLAUSE_LIST_81_INDEX = 81;
        protected static final int ACC_KERNELS_CLAUSE_LIST_82_INDEX = 82;
        protected static final int ACC_KERNELS_CLAUSE_83_INDEX = 83;
        protected static final int ACC_KERNELS_CLAUSE_84_INDEX = 84;
        protected static final int ACC_KERNELS_CLAUSE_85_INDEX = 85;
        protected static final int ACC_KERNELS_CLAUSE_86_INDEX = 86;
        protected static final int ACC_KERNELS_CLAUSE_87_INDEX = 87;
        protected static final int ACC_KERNELS_CLAUSE_88_INDEX = 88;
        protected static final int ACC_KERNELS_CLAUSE_89_INDEX = 89;
        protected static final int ACC_KERNELS_CLAUSE_90_INDEX = 90;
        protected static final int ACC_KERNELS_CLAUSE_91_INDEX = 91;
        protected static final int ACC_KERNELS_CLAUSE_92_INDEX = 92;
        protected static final int ACC_KERNELS_CLAUSE_93_INDEX = 93;
        protected static final int ACC_KERNELS_CLAUSE_94_INDEX = 94;
        protected static final int ACC_KERNELS_LOOP_95_INDEX = 95;
        protected static final int ACC_KERNELS_LOOP_96_INDEX = 96;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_LIST_97_INDEX = 97;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_LIST_98_INDEX = 98;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_LIST_99_INDEX = 99;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_100_INDEX = 100;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_101_INDEX = 101;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_102_INDEX = 102;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_103_INDEX = 103;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_104_INDEX = 104;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_105_INDEX = 105;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_106_INDEX = 106;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_107_INDEX = 107;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_108_INDEX = 108;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_109_INDEX = 109;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_110_INDEX = 110;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_111_INDEX = 111;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_112_INDEX = 112;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_113_INDEX = 113;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_114_INDEX = 114;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_115_INDEX = 115;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_116_INDEX = 116;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_117_INDEX = 117;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_118_INDEX = 118;
        protected static final int ACC_KERNELS_LOOP_CLAUSE_119_INDEX = 119;
        protected static final int ACC_DECLARE_120_INDEX = 120;
        protected static final int ACC_DECLARE_CLAUSE_LIST_121_INDEX = 121;
        protected static final int ACC_DECLARE_CLAUSE_LIST_122_INDEX = 122;
        protected static final int ACC_DECLARE_CLAUSE_LIST_123_INDEX = 123;
        protected static final int ACC_DECLARE_CLAUSE_124_INDEX = 124;
        protected static final int ACC_DECLARE_CLAUSE_125_INDEX = 125;
        protected static final int ACC_DECLARE_CLAUSE_126_INDEX = 126;
        protected static final int ACC_DECLARE_CLAUSE_127_INDEX = 127;
        protected static final int ACC_DECLARE_CLAUSE_128_INDEX = 128;
        protected static final int ACC_DECLARE_CLAUSE_129_INDEX = 129;
        protected static final int ACC_DECLARE_CLAUSE_130_INDEX = 130;
        protected static final int ACC_DECLARE_CLAUSE_131_INDEX = 131;
        protected static final int ACC_DECLARE_CLAUSE_132_INDEX = 132;
        protected static final int ACC_DECLARE_CLAUSE_133_INDEX = 133;
        protected static final int ACC_DECLARE_CLAUSE_134_INDEX = 134;
        protected static final int ACC_DATA_135_INDEX = 135;
        protected static final int ACC_DATA_CLAUSE_LIST_136_INDEX = 136;
        protected static final int ACC_DATA_CLAUSE_LIST_137_INDEX = 137;
        protected static final int ACC_DATA_CLAUSE_LIST_138_INDEX = 138;
        protected static final int ACC_DATA_CLAUSE_139_INDEX = 139;
        protected static final int ACC_DATA_CLAUSE_140_INDEX = 140;
        protected static final int ACC_DATA_CLAUSE_141_INDEX = 141;
        protected static final int ACC_DATA_CLAUSE_142_INDEX = 142;
        protected static final int ACC_DATA_CLAUSE_143_INDEX = 143;
        protected static final int ACC_DATA_CLAUSE_144_INDEX = 144;
        protected static final int ACC_DATA_CLAUSE_145_INDEX = 145;
        protected static final int ACC_DATA_CLAUSE_146_INDEX = 146;
        protected static final int ACC_DATA_CLAUSE_147_INDEX = 147;
        protected static final int ACC_DATA_CLAUSE_148_INDEX = 148;
        protected static final int ACC_DATA_CLAUSE_149_INDEX = 149;
        protected static final int ACC_HOSTDATA_150_INDEX = 150;
        protected static final int ACC_HOSTDATA_CLAUSE_LIST_151_INDEX = 151;
        protected static final int ACC_HOSTDATA_CLAUSE_LIST_152_INDEX = 152;
        protected static final int ACC_HOSTDATA_CLAUSE_LIST_153_INDEX = 153;
        protected static final int ACC_HOSTDATA_CLAUSE_154_INDEX = 154;
        protected static final int ACC_CACHE_155_INDEX = 155;
        protected static final int ACC_WAIT_156_INDEX = 156;
        protected static final int ACC_WAIT_157_INDEX = 157;
        protected static final int ACC_WAIT_PARAMETER_158_INDEX = 158;
        protected static final int ACC_UPDATE_159_INDEX = 159;
        protected static final int ACC_UPDATE_CLAUSE_LIST_160_INDEX = 160;
        protected static final int ACC_UPDATE_CLAUSE_LIST_161_INDEX = 161;
        protected static final int ACC_UPDATE_CLAUSE_LIST_162_INDEX = 162;
        protected static final int ACC_UPDATE_CLAUSE_163_INDEX = 163;
        protected static final int ACC_UPDATE_CLAUSE_164_INDEX = 164;
        protected static final int ACC_UPDATE_CLAUSE_165_INDEX = 165;
        protected static final int ACC_UPDATE_CLAUSE_166_INDEX = 166;
        protected static final int ACC_COLLAPSE_CLAUSE_167_INDEX = 167;
        protected static final int ACC_GANG_CLAUSE_168_INDEX = 168;
        protected static final int ACC_GANG_CLAUSE_169_INDEX = 169;
        protected static final int ACC_WORKER_CLAUSE_170_INDEX = 170;
        protected static final int ACC_WORKER_CLAUSE_171_INDEX = 171;
        protected static final int ACC_VECTOR_CLAUSE_172_INDEX = 172;
        protected static final int ACC_VECTOR_CLAUSE_173_INDEX = 173;
        protected static final int ACC_PRIVATE_CLAUSE_174_INDEX = 174;
        protected static final int ACC_FIRSTPRIVATE_CLAUSE_175_INDEX = 175;
        protected static final int ACC_REDUCTION_CLAUSE_176_INDEX = 176;
        protected static final int ACC_IF_CLAUSE_177_INDEX = 177;
        protected static final int ACC_ASYNC_CLAUSE_178_INDEX = 178;
        protected static final int ACC_ASYNC_CLAUSE_179_INDEX = 179;
        protected static final int ACC_NUMGANGS_CLAUSE_180_INDEX = 180;
        protected static final int ACC_NUMWORKERS_CLAUSE_181_INDEX = 181;
        protected static final int ACC_VECTORLENGTH_CLAUSE_182_INDEX = 182;
        protected static final int ACC_COPY_CLAUSE_183_INDEX = 183;
        protected static final int ACC_COPYIN_CLAUSE_184_INDEX = 184;
        protected static final int ACC_COPYOUT_CLAUSE_185_INDEX = 185;
        protected static final int ACC_CREATE_CLAUSE_186_INDEX = 186;
        protected static final int ACC_PRESENT_CLAUSE_187_INDEX = 187;
        protected static final int ACC_PRESENTORCOPY_CLAUSE_188_INDEX = 188;
        protected static final int ACC_PRESENTORCOPY_CLAUSE_189_INDEX = 189;
        protected static final int ACC_PRESENTORCOPYIN_CLAUSE_190_INDEX = 190;
        protected static final int ACC_PRESENTORCOPYIN_CLAUSE_191_INDEX = 191;
        protected static final int ACC_PRESENTORCOPYOUT_CLAUSE_192_INDEX = 192;
        protected static final int ACC_PRESENTORCOPYOUT_CLAUSE_193_INDEX = 193;
        protected static final int ACC_PRESENTORCREATE_CLAUSE_194_INDEX = 194;
        protected static final int ACC_PRESENTORCREATE_CLAUSE_195_INDEX = 195;
        protected static final int ACC_DEVICEPTR_CLAUSE_196_INDEX = 196;
        protected static final int ACC_DEVICERESIDENT_CLAUSE_197_INDEX = 197;
        protected static final int ACC_USEDEVICE_CLAUSE_198_INDEX = 198;
        protected static final int ACC_HOST_CLAUSE_199_INDEX = 199;
        protected static final int ACC_DEVICE_CLAUSE_200_INDEX = 200;
        protected static final int ACC_COUNT_201_INDEX = 201;
        protected static final int ACC_REDUCTION_OPERATOR_202_INDEX = 202;
        protected static final int ACC_REDUCTION_OPERATOR_203_INDEX = 203;
        protected static final int ACC_REDUCTION_OPERATOR_204_INDEX = 204;
        protected static final int ACC_REDUCTION_OPERATOR_205_INDEX = 205;
        protected static final int ACC_REDUCTION_OPERATOR_206_INDEX = 206;
        protected static final int ACC_REDUCTION_OPERATOR_207_INDEX = 207;
        protected static final int ACC_REDUCTION_OPERATOR_208_INDEX = 208;
        protected static final int ACC_REDUCTION_OPERATOR_209_INDEX = 209;
        protected static final int ACC_REDUCTION_OPERATOR_210_INDEX = 210;
        protected static final int ACC_DATA_LIST_211_INDEX = 211;
        protected static final int ACC_DATA_LIST_212_INDEX = 212;
        protected static final int ACC_DATA_ITEM_213_INDEX = 213;
        protected static final int ACC_DATA_ITEM_214_INDEX = 214;
        protected static final int PRIMARY_EXPRESSION_215_INDEX = 215;
        protected static final int PRIMARY_EXPRESSION_216_INDEX = 216;
        protected static final int PRIMARY_EXPRESSION_217_INDEX = 217;
        protected static final int PRIMARY_EXPRESSION_218_INDEX = 218;
        protected static final int POSTFIX_EXPRESSION_219_INDEX = 219;
        protected static final int POSTFIX_EXPRESSION_220_INDEX = 220;
        protected static final int POSTFIX_EXPRESSION_221_INDEX = 221;
        protected static final int POSTFIX_EXPRESSION_222_INDEX = 222;
        protected static final int POSTFIX_EXPRESSION_223_INDEX = 223;
        protected static final int POSTFIX_EXPRESSION_224_INDEX = 224;
        protected static final int POSTFIX_EXPRESSION_225_INDEX = 225;
        protected static final int POSTFIX_EXPRESSION_226_INDEX = 226;
        protected static final int ARGUMENT_EXPRESSION_LIST_227_INDEX = 227;
        protected static final int ARGUMENT_EXPRESSION_LIST_228_INDEX = 228;
        protected static final int UNARY_EXPRESSION_229_INDEX = 229;
        protected static final int UNARY_EXPRESSION_230_INDEX = 230;
        protected static final int UNARY_EXPRESSION_231_INDEX = 231;
        protected static final int UNARY_EXPRESSION_232_INDEX = 232;
        protected static final int UNARY_EXPRESSION_233_INDEX = 233;
        protected static final int UNARY_OPERATOR_234_INDEX = 234;
        protected static final int UNARY_OPERATOR_235_INDEX = 235;
        protected static final int UNARY_OPERATOR_236_INDEX = 236;
        protected static final int UNARY_OPERATOR_237_INDEX = 237;
        protected static final int UNARY_OPERATOR_238_INDEX = 238;
        protected static final int UNARY_OPERATOR_239_INDEX = 239;
        protected static final int CAST_EXPRESSION_240_INDEX = 240;
        protected static final int MULTIPLICATIVE_EXPRESSION_241_INDEX = 241;
        protected static final int MULTIPLICATIVE_EXPRESSION_242_INDEX = 242;
        protected static final int MULTIPLICATIVE_EXPRESSION_243_INDEX = 243;
        protected static final int MULTIPLICATIVE_EXPRESSION_244_INDEX = 244;
        protected static final int ADDITIVE_EXPRESSION_245_INDEX = 245;
        protected static final int ADDITIVE_EXPRESSION_246_INDEX = 246;
        protected static final int ADDITIVE_EXPRESSION_247_INDEX = 247;
        protected static final int SHIFT_EXPRESSION_248_INDEX = 248;
        protected static final int SHIFT_EXPRESSION_249_INDEX = 249;
        protected static final int SHIFT_EXPRESSION_250_INDEX = 250;
        protected static final int RELATIONAL_EXPRESSION_251_INDEX = 251;
        protected static final int RELATIONAL_EXPRESSION_252_INDEX = 252;
        protected static final int RELATIONAL_EXPRESSION_253_INDEX = 253;
        protected static final int RELATIONAL_EXPRESSION_254_INDEX = 254;
        protected static final int RELATIONAL_EXPRESSION_255_INDEX = 255;
        protected static final int EQUALITY_EXPRESSION_256_INDEX = 256;
        protected static final int EQUALITY_EXPRESSION_257_INDEX = 257;
        protected static final int EQUALITY_EXPRESSION_258_INDEX = 258;
        protected static final int AND_EXPRESSION_259_INDEX = 259;
        protected static final int AND_EXPRESSION_260_INDEX = 260;
        protected static final int EXCLUSIVE_OR_EXPRESSION_261_INDEX = 261;
        protected static final int EXCLUSIVE_OR_EXPRESSION_262_INDEX = 262;
        protected static final int INCLUSIVE_OR_EXPRESSION_263_INDEX = 263;
        protected static final int INCLUSIVE_OR_EXPRESSION_264_INDEX = 264;
        protected static final int LOGICAL_AND_EXPRESSION_265_INDEX = 265;
        protected static final int LOGICAL_AND_EXPRESSION_266_INDEX = 266;
        protected static final int LOGICAL_OR_EXPRESSION_267_INDEX = 267;
        protected static final int LOGICAL_OR_EXPRESSION_268_INDEX = 268;
        protected static final int CONDITIONAL_EXPRESSION_269_INDEX = 269;
        protected static final int CONDITIONAL_EXPRESSION_270_INDEX = 270;
        protected static final int CONSTANT_271_INDEX = 271;
        protected static final int CONSTANT_272_INDEX = 272;
        protected static final int CONSTANT_273_INDEX = 273;
        protected static final int EXPRESSION_274_INDEX = 274;
        protected static final int ASSIGNMENT_EXPRESSION_275_INDEX = 275;
        protected static final int CONSTANT_EXPRESSION_276_INDEX = 276;
        protected static final int IDENTIFIER_LIST_277_INDEX = 277;
        protected static final int IDENTIFIER_LIST_278_INDEX = 278;
        protected static final int IDENTIFIER_279_INDEX = 279;
        protected static final int IDENTIFIER_280_INDEX = 280;
        protected static final int STRING_LITERAL_TERMINAL_LIST_281_INDEX = 281;
        protected static final int STRING_LITERAL_TERMINAL_LIST_282_INDEX = 282;

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
            ACC_LOOP_13,
            ACC_LOOP_14,
            ACC_LOOP_CLAUSE_LIST_15,
            ACC_LOOP_CLAUSE_LIST_16,
            ACC_LOOP_CLAUSE_LIST_17,
            ACC_LOOP_CLAUSE_18,
            ACC_LOOP_CLAUSE_19,
            ACC_LOOP_CLAUSE_20,
            ACC_LOOP_CLAUSE_21,
            ACC_LOOP_CLAUSE_22,
            ACC_LOOP_CLAUSE_23,
            ACC_LOOP_CLAUSE_24,
            ACC_LOOP_CLAUSE_25,
            ACC_PARALLEL_26,
            ACC_PARALLEL_27,
            ACC_PARALLEL_CLAUSE_LIST_28,
            ACC_PARALLEL_CLAUSE_LIST_29,
            ACC_PARALLEL_CLAUSE_LIST_30,
            ACC_PARALLEL_CLAUSE_31,
            ACC_PARALLEL_CLAUSE_32,
            ACC_PARALLEL_CLAUSE_33,
            ACC_PARALLEL_CLAUSE_34,
            ACC_PARALLEL_CLAUSE_35,
            ACC_PARALLEL_CLAUSE_36,
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
            ACC_PARALLEL_LOOP_49,
            ACC_PARALLEL_LOOP_50,
            ACC_PARALLEL_LOOP_CLAUSE_LIST_51,
            ACC_PARALLEL_LOOP_CLAUSE_LIST_52,
            ACC_PARALLEL_LOOP_CLAUSE_LIST_53,
            ACC_PARALLEL_LOOP_CLAUSE_54,
            ACC_PARALLEL_LOOP_CLAUSE_55,
            ACC_PARALLEL_LOOP_CLAUSE_56,
            ACC_PARALLEL_LOOP_CLAUSE_57,
            ACC_PARALLEL_LOOP_CLAUSE_58,
            ACC_PARALLEL_LOOP_CLAUSE_59,
            ACC_PARALLEL_LOOP_CLAUSE_60,
            ACC_PARALLEL_LOOP_CLAUSE_61,
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
            ACC_KERNELS_78,
            ACC_KERNELS_79,
            ACC_KERNELS_CLAUSE_LIST_80,
            ACC_KERNELS_CLAUSE_LIST_81,
            ACC_KERNELS_CLAUSE_LIST_82,
            ACC_KERNELS_CLAUSE_83,
            ACC_KERNELS_CLAUSE_84,
            ACC_KERNELS_CLAUSE_85,
            ACC_KERNELS_CLAUSE_86,
            ACC_KERNELS_CLAUSE_87,
            ACC_KERNELS_CLAUSE_88,
            ACC_KERNELS_CLAUSE_89,
            ACC_KERNELS_CLAUSE_90,
            ACC_KERNELS_CLAUSE_91,
            ACC_KERNELS_CLAUSE_92,
            ACC_KERNELS_CLAUSE_93,
            ACC_KERNELS_CLAUSE_94,
            ACC_KERNELS_LOOP_95,
            ACC_KERNELS_LOOP_96,
            ACC_KERNELS_LOOP_CLAUSE_LIST_97,
            ACC_KERNELS_LOOP_CLAUSE_LIST_98,
            ACC_KERNELS_LOOP_CLAUSE_LIST_99,
            ACC_KERNELS_LOOP_CLAUSE_100,
            ACC_KERNELS_LOOP_CLAUSE_101,
            ACC_KERNELS_LOOP_CLAUSE_102,
            ACC_KERNELS_LOOP_CLAUSE_103,
            ACC_KERNELS_LOOP_CLAUSE_104,
            ACC_KERNELS_LOOP_CLAUSE_105,
            ACC_KERNELS_LOOP_CLAUSE_106,
            ACC_KERNELS_LOOP_CLAUSE_107,
            ACC_KERNELS_LOOP_CLAUSE_108,
            ACC_KERNELS_LOOP_CLAUSE_109,
            ACC_KERNELS_LOOP_CLAUSE_110,
            ACC_KERNELS_LOOP_CLAUSE_111,
            ACC_KERNELS_LOOP_CLAUSE_112,
            ACC_KERNELS_LOOP_CLAUSE_113,
            ACC_KERNELS_LOOP_CLAUSE_114,
            ACC_KERNELS_LOOP_CLAUSE_115,
            ACC_KERNELS_LOOP_CLAUSE_116,
            ACC_KERNELS_LOOP_CLAUSE_117,
            ACC_KERNELS_LOOP_CLAUSE_118,
            ACC_KERNELS_LOOP_CLAUSE_119,
            ACC_DECLARE_120,
            ACC_DECLARE_CLAUSE_LIST_121,
            ACC_DECLARE_CLAUSE_LIST_122,
            ACC_DECLARE_CLAUSE_LIST_123,
            ACC_DECLARE_CLAUSE_124,
            ACC_DECLARE_CLAUSE_125,
            ACC_DECLARE_CLAUSE_126,
            ACC_DECLARE_CLAUSE_127,
            ACC_DECLARE_CLAUSE_128,
            ACC_DECLARE_CLAUSE_129,
            ACC_DECLARE_CLAUSE_130,
            ACC_DECLARE_CLAUSE_131,
            ACC_DECLARE_CLAUSE_132,
            ACC_DECLARE_CLAUSE_133,
            ACC_DECLARE_CLAUSE_134,
            ACC_DATA_135,
            ACC_DATA_CLAUSE_LIST_136,
            ACC_DATA_CLAUSE_LIST_137,
            ACC_DATA_CLAUSE_LIST_138,
            ACC_DATA_CLAUSE_139,
            ACC_DATA_CLAUSE_140,
            ACC_DATA_CLAUSE_141,
            ACC_DATA_CLAUSE_142,
            ACC_DATA_CLAUSE_143,
            ACC_DATA_CLAUSE_144,
            ACC_DATA_CLAUSE_145,
            ACC_DATA_CLAUSE_146,
            ACC_DATA_CLAUSE_147,
            ACC_DATA_CLAUSE_148,
            ACC_DATA_CLAUSE_149,
            ACC_HOSTDATA_150,
            ACC_HOSTDATA_CLAUSE_LIST_151,
            ACC_HOSTDATA_CLAUSE_LIST_152,
            ACC_HOSTDATA_CLAUSE_LIST_153,
            ACC_HOSTDATA_CLAUSE_154,
            ACC_CACHE_155,
            ACC_WAIT_156,
            ACC_WAIT_157,
            ACC_WAIT_PARAMETER_158,
            ACC_UPDATE_159,
            ACC_UPDATE_CLAUSE_LIST_160,
            ACC_UPDATE_CLAUSE_LIST_161,
            ACC_UPDATE_CLAUSE_LIST_162,
            ACC_UPDATE_CLAUSE_163,
            ACC_UPDATE_CLAUSE_164,
            ACC_UPDATE_CLAUSE_165,
            ACC_UPDATE_CLAUSE_166,
            ACC_COLLAPSE_CLAUSE_167,
            ACC_GANG_CLAUSE_168,
            ACC_GANG_CLAUSE_169,
            ACC_WORKER_CLAUSE_170,
            ACC_WORKER_CLAUSE_171,
            ACC_VECTOR_CLAUSE_172,
            ACC_VECTOR_CLAUSE_173,
            ACC_PRIVATE_CLAUSE_174,
            ACC_FIRSTPRIVATE_CLAUSE_175,
            ACC_REDUCTION_CLAUSE_176,
            ACC_IF_CLAUSE_177,
            ACC_ASYNC_CLAUSE_178,
            ACC_ASYNC_CLAUSE_179,
            ACC_NUMGANGS_CLAUSE_180,
            ACC_NUMWORKERS_CLAUSE_181,
            ACC_VECTORLENGTH_CLAUSE_182,
            ACC_COPY_CLAUSE_183,
            ACC_COPYIN_CLAUSE_184,
            ACC_COPYOUT_CLAUSE_185,
            ACC_CREATE_CLAUSE_186,
            ACC_PRESENT_CLAUSE_187,
            ACC_PRESENTORCOPY_CLAUSE_188,
            ACC_PRESENTORCOPY_CLAUSE_189,
            ACC_PRESENTORCOPYIN_CLAUSE_190,
            ACC_PRESENTORCOPYIN_CLAUSE_191,
            ACC_PRESENTORCOPYOUT_CLAUSE_192,
            ACC_PRESENTORCOPYOUT_CLAUSE_193,
            ACC_PRESENTORCREATE_CLAUSE_194,
            ACC_PRESENTORCREATE_CLAUSE_195,
            ACC_DEVICEPTR_CLAUSE_196,
            ACC_DEVICERESIDENT_CLAUSE_197,
            ACC_USEDEVICE_CLAUSE_198,
            ACC_HOST_CLAUSE_199,
            ACC_DEVICE_CLAUSE_200,
            ACC_COUNT_201,
            ACC_REDUCTION_OPERATOR_202,
            ACC_REDUCTION_OPERATOR_203,
            ACC_REDUCTION_OPERATOR_204,
            ACC_REDUCTION_OPERATOR_205,
            ACC_REDUCTION_OPERATOR_206,
            ACC_REDUCTION_OPERATOR_207,
            ACC_REDUCTION_OPERATOR_208,
            ACC_REDUCTION_OPERATOR_209,
            ACC_REDUCTION_OPERATOR_210,
            ACC_DATA_LIST_211,
            ACC_DATA_LIST_212,
            ACC_DATA_ITEM_213,
            ACC_DATA_ITEM_214,
            PRIMARY_EXPRESSION_215,
            PRIMARY_EXPRESSION_216,
            PRIMARY_EXPRESSION_217,
            PRIMARY_EXPRESSION_218,
            POSTFIX_EXPRESSION_219,
            POSTFIX_EXPRESSION_220,
            POSTFIX_EXPRESSION_221,
            POSTFIX_EXPRESSION_222,
            POSTFIX_EXPRESSION_223,
            POSTFIX_EXPRESSION_224,
            POSTFIX_EXPRESSION_225,
            POSTFIX_EXPRESSION_226,
            ARGUMENT_EXPRESSION_LIST_227,
            ARGUMENT_EXPRESSION_LIST_228,
            UNARY_EXPRESSION_229,
            UNARY_EXPRESSION_230,
            UNARY_EXPRESSION_231,
            UNARY_EXPRESSION_232,
            UNARY_EXPRESSION_233,
            UNARY_OPERATOR_234,
            UNARY_OPERATOR_235,
            UNARY_OPERATOR_236,
            UNARY_OPERATOR_237,
            UNARY_OPERATOR_238,
            UNARY_OPERATOR_239,
            CAST_EXPRESSION_240,
            MULTIPLICATIVE_EXPRESSION_241,
            MULTIPLICATIVE_EXPRESSION_242,
            MULTIPLICATIVE_EXPRESSION_243,
            MULTIPLICATIVE_EXPRESSION_244,
            ADDITIVE_EXPRESSION_245,
            ADDITIVE_EXPRESSION_246,
            ADDITIVE_EXPRESSION_247,
            SHIFT_EXPRESSION_248,
            SHIFT_EXPRESSION_249,
            SHIFT_EXPRESSION_250,
            RELATIONAL_EXPRESSION_251,
            RELATIONAL_EXPRESSION_252,
            RELATIONAL_EXPRESSION_253,
            RELATIONAL_EXPRESSION_254,
            RELATIONAL_EXPRESSION_255,
            EQUALITY_EXPRESSION_256,
            EQUALITY_EXPRESSION_257,
            EQUALITY_EXPRESSION_258,
            AND_EXPRESSION_259,
            AND_EXPRESSION_260,
            EXCLUSIVE_OR_EXPRESSION_261,
            EXCLUSIVE_OR_EXPRESSION_262,
            INCLUSIVE_OR_EXPRESSION_263,
            INCLUSIVE_OR_EXPRESSION_264,
            LOGICAL_AND_EXPRESSION_265,
            LOGICAL_AND_EXPRESSION_266,
            LOGICAL_OR_EXPRESSION_267,
            LOGICAL_OR_EXPRESSION_268,
            CONDITIONAL_EXPRESSION_269,
            CONDITIONAL_EXPRESSION_270,
            CONSTANT_271,
            CONSTANT_272,
            CONSTANT_273,
            EXPRESSION_274,
            ASSIGNMENT_EXPRESSION_275,
            CONSTANT_EXPRESSION_276,
            IDENTIFIER_LIST_277,
            IDENTIFIER_LIST_278,
            IDENTIFIER_279,
            IDENTIFIER_280,
            STRING_LITERAL_TERMINAL_LIST_281,
            STRING_LITERAL_TERMINAL_LIST_282,
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
                    // Cases 1 and 2
                    ASTAccNoConstruct node = new ASTAccNoConstruct();
                    return node;

                }
                case Production.ACC_LOOP_13_INDEX:
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
                case Production.ACC_LOOP_14_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccLoopNode node = new ASTAccLoopNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringLoop = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringLoop != null) node.hiddenLiteralStringLoop.setParent(node);
                    return node;

                }
                case Production.ACC_LOOP_CLAUSE_LIST_15_INDEX:
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
                case Production.ACC_LOOP_CLAUSE_LIST_16_INDEX:
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
                case Production.ACC_LOOP_CLAUSE_LIST_17_INDEX:
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
                case Production.ACC_LOOP_CLAUSE_18_INDEX:
                {
                    // Case 5
                    ASTAccCollapseClauseNode result = (ASTAccCollapseClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_LOOP_CLAUSE_19_INDEX:
                {
                    // Case 5
                    ASTAccGangClauseNode result = (ASTAccGangClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_LOOP_CLAUSE_20_INDEX:
                {
                    // Case 5
                    ASTAccWorkerClauseNode result = (ASTAccWorkerClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_LOOP_CLAUSE_21_INDEX:
                {
                    // Case 5
                    ASTAccVectorClauseNode result = (ASTAccVectorClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_LOOP_CLAUSE_22_INDEX:
                {
                    // Cases 1 and 2
                    CAccSeqClause node = new CAccSeqClause();
                    node.hiddenLiteralStringSeq = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringSeq != null) node.hiddenLiteralStringSeq.setParent(node);
                    return node;

                }
                case Production.ACC_LOOP_CLAUSE_23_INDEX:
                {
                    // Cases 1 and 2
                    CAccIndependentClause node = new CAccIndependentClause();
                    node.hiddenLiteralStringIndependent = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringIndependent != null) node.hiddenLiteralStringIndependent.setParent(node);
                    return node;

                }
                case Production.ACC_LOOP_CLAUSE_24_INDEX:
                {
                    // Case 5
                    ASTAccPrivateClauseNode result = (ASTAccPrivateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_LOOP_CLAUSE_25_INDEX:
                {
                    // Case 5
                    ASTAccReductionClauseNode result = (ASTAccReductionClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_26_INDEX:
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
                case Production.ACC_PARALLEL_27_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccParallelNode node = new ASTAccParallelNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringParallel = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringParallel != null) node.hiddenLiteralStringParallel.setParent(node);
                    return node;

                }
                case Production.ACC_PARALLEL_CLAUSE_LIST_28_INDEX:
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
                case Production.ACC_PARALLEL_CLAUSE_LIST_29_INDEX:
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
                case Production.ACC_PARALLEL_CLAUSE_LIST_30_INDEX:
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
                case Production.ACC_PARALLEL_CLAUSE_31_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_32_INDEX:
                {
                    // Case 5
                    ASTAccAsyncClauseNode result = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_33_INDEX:
                {
                    // Case 5
                    ASTAccNumgangsClauseNode result = (ASTAccNumgangsClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_34_INDEX:
                {
                    // Case 5
                    ASTAccNumworkersClauseNode result = (ASTAccNumworkersClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_35_INDEX:
                {
                    // Case 5
                    ASTAccVectorlengthClauseNode result = (ASTAccVectorlengthClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_36_INDEX:
                {
                    // Case 5
                    ASTAccReductionClauseNode result = (ASTAccReductionClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_37_INDEX:
                {
                    // Case 5
                    ASTAccCopyClauseNode result = (ASTAccCopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_38_INDEX:
                {
                    // Case 5
                    ASTAccCopyinClauseNode result = (ASTAccCopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_39_INDEX:
                {
                    // Case 5
                    ASTAccCopyoutClauseNode result = (ASTAccCopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_40_INDEX:
                {
                    // Case 5
                    ASTAccCreateClauseNode result = (ASTAccCreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_41_INDEX:
                {
                    // Case 5
                    ASTAccPresentClauseNode result = (ASTAccPresentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_42_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyClauseNode result = (ASTAccPresentorcopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_43_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyinClauseNode result = (ASTAccPresentorcopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_44_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyoutClauseNode result = (ASTAccPresentorcopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_45_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcreateClauseNode result = (ASTAccPresentorcreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_46_INDEX:
                {
                    // Case 5
                    ASTAccDeviceptrClauseNode result = (ASTAccDeviceptrClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_47_INDEX:
                {
                    // Case 5
                    ASTAccPrivateClauseNode result = (ASTAccPrivateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_CLAUSE_48_INDEX:
                {
                    // Case 5
                    ASTAccFirstprivateClauseNode result = (ASTAccFirstprivateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_49_INDEX:
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
                case Production.ACC_PARALLEL_LOOP_50_INDEX:
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
                case Production.ACC_PARALLEL_LOOP_CLAUSE_LIST_51_INDEX:
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
                case Production.ACC_PARALLEL_LOOP_CLAUSE_LIST_52_INDEX:
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
                case Production.ACC_PARALLEL_LOOP_CLAUSE_LIST_53_INDEX:
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
                case Production.ACC_PARALLEL_LOOP_CLAUSE_54_INDEX:
                {
                    // Case 5
                    ASTAccCollapseClauseNode result = (ASTAccCollapseClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_55_INDEX:
                {
                    // Case 5
                    ASTAccGangClauseNode result = (ASTAccGangClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_56_INDEX:
                {
                    // Case 5
                    ASTAccWorkerClauseNode result = (ASTAccWorkerClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_57_INDEX:
                {
                    // Case 5
                    ASTAccVectorClauseNode result = (ASTAccVectorClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_58_INDEX:
                {
                    // Cases 1 and 2
                    CAccSeqClause node = new CAccSeqClause();
                    node.hiddenLiteralStringSeq = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringSeq != null) node.hiddenLiteralStringSeq.setParent(node);
                    return node;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_59_INDEX:
                {
                    // Cases 1 and 2
                    CAccIndependentClause node = new CAccIndependentClause();
                    node.hiddenLiteralStringIndependent = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringIndependent != null) node.hiddenLiteralStringIndependent.setParent(node);
                    return node;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_60_INDEX:
                {
                    // Case 5
                    ASTAccPrivateClauseNode result = (ASTAccPrivateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_61_INDEX:
                {
                    // Case 5
                    ASTAccReductionClauseNode result = (ASTAccReductionClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_62_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_63_INDEX:
                {
                    // Case 5
                    ASTAccAsyncClauseNode result = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_64_INDEX:
                {
                    // Case 5
                    ASTAccNumgangsClauseNode result = (ASTAccNumgangsClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_65_INDEX:
                {
                    // Case 5
                    ASTAccNumworkersClauseNode result = (ASTAccNumworkersClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_66_INDEX:
                {
                    // Case 5
                    ASTAccVectorlengthClauseNode result = (ASTAccVectorlengthClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_67_INDEX:
                {
                    // Case 5
                    ASTAccCopyClauseNode result = (ASTAccCopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_68_INDEX:
                {
                    // Case 5
                    ASTAccCopyinClauseNode result = (ASTAccCopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_69_INDEX:
                {
                    // Case 5
                    ASTAccCopyoutClauseNode result = (ASTAccCopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_70_INDEX:
                {
                    // Case 5
                    ASTAccCreateClauseNode result = (ASTAccCreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_71_INDEX:
                {
                    // Case 5
                    ASTAccPresentClauseNode result = (ASTAccPresentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_72_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyClauseNode result = (ASTAccPresentorcopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_73_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyinClauseNode result = (ASTAccPresentorcopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_74_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyoutClauseNode result = (ASTAccPresentorcopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_75_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcreateClauseNode result = (ASTAccPresentorcreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_76_INDEX:
                {
                    // Case 5
                    ASTAccDeviceptrClauseNode result = (ASTAccDeviceptrClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_PARALLEL_LOOP_CLAUSE_77_INDEX:
                {
                    // Case 5
                    ASTAccFirstprivateClauseNode result = (ASTAccFirstprivateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_78_INDEX:
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
                case Production.ACC_KERNELS_79_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccKernelsNode node = new ASTAccKernelsNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringKernels = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringKernels != null) node.hiddenLiteralStringKernels.setParent(node);
                    return node;

                }
                case Production.ACC_KERNELS_CLAUSE_LIST_80_INDEX:
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
                case Production.ACC_KERNELS_CLAUSE_LIST_81_INDEX:
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
                case Production.ACC_KERNELS_CLAUSE_LIST_82_INDEX:
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
                case Production.ACC_KERNELS_CLAUSE_83_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_84_INDEX:
                {
                    // Case 5
                    ASTAccAsyncClauseNode result = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_85_INDEX:
                {
                    // Case 5
                    ASTAccCopyClauseNode result = (ASTAccCopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_86_INDEX:
                {
                    // Case 5
                    ASTAccCopyinClauseNode result = (ASTAccCopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_87_INDEX:
                {
                    // Case 5
                    ASTAccCopyoutClauseNode result = (ASTAccCopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_88_INDEX:
                {
                    // Case 5
                    ASTAccCreateClauseNode result = (ASTAccCreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_89_INDEX:
                {
                    // Case 5
                    ASTAccPresentClauseNode result = (ASTAccPresentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_90_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyClauseNode result = (ASTAccPresentorcopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_91_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyinClauseNode result = (ASTAccPresentorcopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_92_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyoutClauseNode result = (ASTAccPresentorcopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_93_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcreateClauseNode result = (ASTAccPresentorcreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_CLAUSE_94_INDEX:
                {
                    // Case 5
                    ASTAccDeviceptrClauseNode result = (ASTAccDeviceptrClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_95_INDEX:
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
                case Production.ACC_KERNELS_LOOP_96_INDEX:
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
                case Production.ACC_KERNELS_LOOP_CLAUSE_LIST_97_INDEX:
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
                case Production.ACC_KERNELS_LOOP_CLAUSE_LIST_98_INDEX:
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
                case Production.ACC_KERNELS_LOOP_CLAUSE_LIST_99_INDEX:
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
                case Production.ACC_KERNELS_LOOP_CLAUSE_100_INDEX:
                {
                    // Case 5
                    ASTAccCollapseClauseNode result = (ASTAccCollapseClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_101_INDEX:
                {
                    // Case 5
                    ASTAccGangClauseNode result = (ASTAccGangClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_102_INDEX:
                {
                    // Case 5
                    ASTAccWorkerClauseNode result = (ASTAccWorkerClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_103_INDEX:
                {
                    // Case 5
                    ASTAccVectorClauseNode result = (ASTAccVectorClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_104_INDEX:
                {
                    // Cases 1 and 2
                    CAccSeqClause node = new CAccSeqClause();
                    node.hiddenLiteralStringSeq = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringSeq != null) node.hiddenLiteralStringSeq.setParent(node);
                    return node;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_105_INDEX:
                {
                    // Cases 1 and 2
                    CAccIndependentClause node = new CAccIndependentClause();
                    node.hiddenLiteralStringIndependent = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringIndependent != null) node.hiddenLiteralStringIndependent.setParent(node);
                    return node;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_106_INDEX:
                {
                    // Case 5
                    ASTAccPrivateClauseNode result = (ASTAccPrivateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_107_INDEX:
                {
                    // Case 5
                    ASTAccReductionClauseNode result = (ASTAccReductionClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_108_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_109_INDEX:
                {
                    // Case 5
                    ASTAccAsyncClauseNode result = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_110_INDEX:
                {
                    // Case 5
                    ASTAccCopyClauseNode result = (ASTAccCopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_111_INDEX:
                {
                    // Case 5
                    ASTAccCopyinClauseNode result = (ASTAccCopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_112_INDEX:
                {
                    // Case 5
                    ASTAccCopyoutClauseNode result = (ASTAccCopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_113_INDEX:
                {
                    // Case 5
                    ASTAccCreateClauseNode result = (ASTAccCreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_114_INDEX:
                {
                    // Case 5
                    ASTAccPresentClauseNode result = (ASTAccPresentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_115_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyClauseNode result = (ASTAccPresentorcopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_116_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyinClauseNode result = (ASTAccPresentorcopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_117_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyoutClauseNode result = (ASTAccPresentorcopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_118_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcreateClauseNode result = (ASTAccPresentorcreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_KERNELS_LOOP_CLAUSE_119_INDEX:
                {
                    // Case 5
                    ASTAccDeviceptrClauseNode result = (ASTAccDeviceptrClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_120_INDEX:
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
                case Production.ACC_DECLARE_CLAUSE_LIST_121_INDEX:
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
                case Production.ACC_DECLARE_CLAUSE_LIST_122_INDEX:
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
                case Production.ACC_DECLARE_CLAUSE_LIST_123_INDEX:
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
                case Production.ACC_DECLARE_CLAUSE_124_INDEX:
                {
                    // Case 5
                    ASTAccCopyClauseNode result = (ASTAccCopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_125_INDEX:
                {
                    // Case 5
                    ASTAccCopyinClauseNode result = (ASTAccCopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_126_INDEX:
                {
                    // Case 5
                    ASTAccCopyoutClauseNode result = (ASTAccCopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_127_INDEX:
                {
                    // Case 5
                    ASTAccCreateClauseNode result = (ASTAccCreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_128_INDEX:
                {
                    // Case 5
                    ASTAccPresentClauseNode result = (ASTAccPresentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_129_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyClauseNode result = (ASTAccPresentorcopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_130_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyinClauseNode result = (ASTAccPresentorcopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_131_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyoutClauseNode result = (ASTAccPresentorcopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_132_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcreateClauseNode result = (ASTAccPresentorcreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_133_INDEX:
                {
                    // Case 5
                    ASTAccDeviceptrClauseNode result = (ASTAccDeviceptrClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DECLARE_CLAUSE_134_INDEX:
                {
                    // Case 5
                    ASTAccDeviceresidentClauseNode result = (ASTAccDeviceresidentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_135_INDEX:
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
                case Production.ACC_DATA_CLAUSE_LIST_136_INDEX:
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
                case Production.ACC_DATA_CLAUSE_LIST_137_INDEX:
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
                case Production.ACC_DATA_CLAUSE_LIST_138_INDEX:
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
                case Production.ACC_DATA_CLAUSE_139_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_140_INDEX:
                {
                    // Case 5
                    ASTAccCopyClauseNode result = (ASTAccCopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_141_INDEX:
                {
                    // Case 5
                    ASTAccCopyinClauseNode result = (ASTAccCopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_142_INDEX:
                {
                    // Case 5
                    ASTAccCopyoutClauseNode result = (ASTAccCopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_143_INDEX:
                {
                    // Case 5
                    ASTAccCreateClauseNode result = (ASTAccCreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_144_INDEX:
                {
                    // Case 5
                    ASTAccPresentClauseNode result = (ASTAccPresentClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_145_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyClauseNode result = (ASTAccPresentorcopyClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_146_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyinClauseNode result = (ASTAccPresentorcopyinClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_147_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcopyoutClauseNode result = (ASTAccPresentorcopyoutClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_148_INDEX:
                {
                    // Case 5
                    ASTAccPresentorcreateClauseNode result = (ASTAccPresentorcreateClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_DATA_CLAUSE_149_INDEX:
                {
                    // Case 5
                    ASTAccDeviceptrClauseNode result = (ASTAccDeviceptrClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_HOSTDATA_150_INDEX:
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
                case Production.ACC_HOSTDATA_CLAUSE_LIST_151_INDEX:
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
                case Production.ACC_HOSTDATA_CLAUSE_LIST_152_INDEX:
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
                case Production.ACC_HOSTDATA_CLAUSE_LIST_153_INDEX:
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
                case Production.ACC_HOSTDATA_CLAUSE_154_INDEX:
                {
                    // Case 5
                    ASTAccUsedeviceClauseNode result = (ASTAccUsedeviceClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_CACHE_155_INDEX:
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
                case Production.ACC_WAIT_156_INDEX:
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
                case Production.ACC_WAIT_157_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccWaitNode node = new ASTAccWaitNode();
                    node.pragmaAcc = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.pragmaAcc != null) node.pragmaAcc.setParent(node);
                    node.hiddenLiteralStringWait = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.hiddenLiteralStringWait != null) node.hiddenLiteralStringWait.setParent(node);
                    return node;

                }
                case Production.ACC_WAIT_PARAMETER_158_INDEX:
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
                case Production.ACC_UPDATE_159_INDEX:
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
                case Production.ACC_UPDATE_CLAUSE_LIST_160_INDEX:
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
                case Production.ACC_UPDATE_CLAUSE_LIST_161_INDEX:
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
                case Production.ACC_UPDATE_CLAUSE_LIST_162_INDEX:
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
                case Production.ACC_UPDATE_CLAUSE_163_INDEX:
                {
                    // Case 5
                    ASTAccHostClauseNode result = (ASTAccHostClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_UPDATE_CLAUSE_164_INDEX:
                {
                    // Case 5
                    ASTAccDeviceClauseNode result = (ASTAccDeviceClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_UPDATE_CLAUSE_165_INDEX:
                {
                    // Case 5
                    ASTAccIfClauseNode result = (ASTAccIfClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_UPDATE_CLAUSE_166_INDEX:
                {
                    // Case 5
                    ASTAccAsyncClauseNode result = (ASTAccAsyncClauseNode)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ACC_COLLAPSE_CLAUSE_167_INDEX:
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
                case Production.ACC_GANG_CLAUSE_168_INDEX:
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
                case Production.ACC_GANG_CLAUSE_169_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccGangClauseNode node = new ASTAccGangClauseNode();
                    node.hiddenLiteralStringGang = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringGang != null) node.hiddenLiteralStringGang.setParent(node);
                    return node;

                }
                case Production.ACC_WORKER_CLAUSE_170_INDEX:
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
                case Production.ACC_WORKER_CLAUSE_171_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccWorkerClauseNode node = new ASTAccWorkerClauseNode();
                    node.hiddenLiteralStringWorker = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringWorker != null) node.hiddenLiteralStringWorker.setParent(node);
                    return node;

                }
                case Production.ACC_VECTOR_CLAUSE_172_INDEX:
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
                case Production.ACC_VECTOR_CLAUSE_173_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccVectorClauseNode node = new ASTAccVectorClauseNode();
                    node.hiddenLiteralStringVector = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringVector != null) node.hiddenLiteralStringVector.setParent(node);
                    return node;

                }
                case Production.ACC_PRIVATE_CLAUSE_174_INDEX:
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
                case Production.ACC_FIRSTPRIVATE_CLAUSE_175_INDEX:
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
                case Production.ACC_REDUCTION_CLAUSE_176_INDEX:
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
                case Production.ACC_IF_CLAUSE_177_INDEX:
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
                case Production.ACC_ASYNC_CLAUSE_178_INDEX:
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
                case Production.ACC_ASYNC_CLAUSE_179_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccAsyncClauseNode node = new ASTAccAsyncClauseNode();
                    node.hiddenLiteralStringAsync = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringAsync != null) node.hiddenLiteralStringAsync.setParent(node);
                    return node;

                }
                case Production.ACC_NUMGANGS_CLAUSE_180_INDEX:
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
                case Production.ACC_NUMWORKERS_CLAUSE_181_INDEX:
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
                case Production.ACC_VECTORLENGTH_CLAUSE_182_INDEX:
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
                case Production.ACC_COPY_CLAUSE_183_INDEX:
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
                case Production.ACC_COPYIN_CLAUSE_184_INDEX:
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
                case Production.ACC_COPYOUT_CLAUSE_185_INDEX:
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
                case Production.ACC_CREATE_CLAUSE_186_INDEX:
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
                case Production.ACC_PRESENT_CLAUSE_187_INDEX:
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
                case Production.ACC_PRESENTORCOPY_CLAUSE_188_INDEX:
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
                case Production.ACC_PRESENTORCOPY_CLAUSE_189_INDEX:
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
                case Production.ACC_PRESENTORCOPYIN_CLAUSE_190_INDEX:
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
                case Production.ACC_PRESENTORCOPYIN_CLAUSE_191_INDEX:
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
                case Production.ACC_PRESENTORCOPYOUT_CLAUSE_192_INDEX:
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
                case Production.ACC_PRESENTORCOPYOUT_CLAUSE_193_INDEX:
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
                case Production.ACC_PRESENTORCREATE_CLAUSE_194_INDEX:
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
                case Production.ACC_PRESENTORCREATE_CLAUSE_195_INDEX:
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
                case Production.ACC_DEVICEPTR_CLAUSE_196_INDEX:
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
                case Production.ACC_DEVICERESIDENT_CLAUSE_197_INDEX:
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
                case Production.ACC_USEDEVICE_CLAUSE_198_INDEX:
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
                case Production.ACC_HOST_CLAUSE_199_INDEX:
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
                case Production.ACC_DEVICE_CLAUSE_200_INDEX:
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
                case Production.ACC_COUNT_201_INDEX:
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
                case Production.ACC_REDUCTION_OPERATOR_202_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_203_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_204_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_205_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_206_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_207_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_208_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_209_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_REDUCTION_OPERATOR_210_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.ACC_DATA_LIST_211_INDEX:
                {
                    // Case 7 with separator
                    ASTSeparatedListNode<ASTAccDataItemNode> list = new ASTSeparatedListNode<ASTAccDataItemNode>();
                    ASTAccDataItemNode elt = (ASTAccDataItemNode)valueStack.get(valueStackOffset + 0);
                    list.add(null, elt);
                    if (elt != null) elt.setParent(list);
                    return list;

                }
                case Production.ACC_DATA_LIST_212_INDEX:
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
                case Production.ACC_DATA_ITEM_213_INDEX:
                {
                    // Cases 1 and 2
                    ASTAccDataItemNode node = new ASTAccDataItemNode();
                    node.identifier = (ASTIdentifierNode)valueStack.get(valueStackOffset + 0);
                    if (node.identifier != null) node.identifier.setParent(node);
                    return node;

                }
                case Production.ACC_DATA_ITEM_214_INDEX:
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
                case Production.PRIMARY_EXPRESSION_215_INDEX:
                {
                    // Cases 1 and 2
                    CIdentifierExpression node = new CIdentifierExpression();
                    node.identifier = (ASTIdentifierNode)valueStack.get(valueStackOffset + 0);
                    if (node.identifier != null) node.identifier.setParent(node);
                    return node;

                }
                case Production.PRIMARY_EXPRESSION_216_INDEX:
                {
                    // Case 5
                    CConstantExpression result = (CConstantExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.PRIMARY_EXPRESSION_217_INDEX:
                {
                    // Cases 1 and 2
                    CStringLiteralExpression node = new CStringLiteralExpression();
                    node.literals = (IASTListNode<Token>)valueStack.get(valueStackOffset + 0);
                    if (node.literals != null) node.literals.setParent(node);
                    return node;

                }
                case Production.PRIMARY_EXPRESSION_218_INDEX:
                {
                    // Case 5
                    ASTExpressionNode result = (ASTExpressionNode)valueStack.get(valueStackOffset + 1);
                    result.prependToken((Token)valueStack.get(valueStackOffset + 0));
                    result.appendToken((Token)valueStack.get(valueStackOffset + 2));
                    return result;

                }
                case Production.POSTFIX_EXPRESSION_219_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.POSTFIX_EXPRESSION_220_INDEX:
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
                case Production.POSTFIX_EXPRESSION_221_INDEX:
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
                case Production.POSTFIX_EXPRESSION_222_INDEX:
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
                case Production.POSTFIX_EXPRESSION_223_INDEX:
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
                case Production.POSTFIX_EXPRESSION_224_INDEX:
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
                case Production.POSTFIX_EXPRESSION_225_INDEX:
                {
                    // Cases 1 and 2
                    CPostfixUnaryExpression node = new CPostfixUnaryExpression();
                    node.subexpression = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.subexpression != null) node.subexpression.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    return node;

                }
                case Production.POSTFIX_EXPRESSION_226_INDEX:
                {
                    // Cases 1 and 2
                    CPostfixUnaryExpression node = new CPostfixUnaryExpression();
                    node.subexpression = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.subexpression != null) node.subexpression.setParent(node);
                    node.operator = (Token)valueStack.get(valueStackOffset + 1);
                    if (node.operator != null) node.operator.setParent(node);
                    return node;

                }
                case Production.ARGUMENT_EXPRESSION_LIST_227_INDEX:
                {
                    // Case 7 with separator
                    ASTSeparatedListNode<IAssignmentExpression> list = new ASTSeparatedListNode<IAssignmentExpression>();
                    IAssignmentExpression elt = (IAssignmentExpression)valueStack.get(valueStackOffset + 0);
                    list.add(null, elt);
                    if (elt != null) elt.setParent(list);
                    return list;

                }
                case Production.ARGUMENT_EXPRESSION_LIST_228_INDEX:
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
                case Production.UNARY_EXPRESSION_229_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.UNARY_EXPRESSION_230_INDEX:
                {
                    // Cases 1 and 2
                    CPrefixUnaryExpression node = new CPrefixUnaryExpression();
                    node.operator = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.operator != null) node.operator.setParent(node);
                    node.subexpression = (ICExpression)valueStack.get(valueStackOffset + 1);
                    if (node.subexpression != null) node.subexpression.setParent(node);
                    return node;

                }
                case Production.UNARY_EXPRESSION_231_INDEX:
                {
                    // Cases 1 and 2
                    CPrefixUnaryExpression node = new CPrefixUnaryExpression();
                    node.operator = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.operator != null) node.operator.setParent(node);
                    node.subexpression = (ICExpression)valueStack.get(valueStackOffset + 1);
                    if (node.subexpression != null) node.subexpression.setParent(node);
                    return node;

                }
                case Production.UNARY_EXPRESSION_232_INDEX:
                {
                    // Cases 1 and 2
                    CPrefixUnaryExpression node = new CPrefixUnaryExpression();
                    node.operator = (Token)((Map<String, Object>)valueStack.get(valueStackOffset + 0)).get("operator");
                    if (node.operator != null) node.operator.setParent(node);
                    node.subexpression = (ICExpression)valueStack.get(valueStackOffset + 1);
                    if (node.subexpression != null) node.subexpression.setParent(node);
                    return node;

                }
                case Production.UNARY_EXPRESSION_233_INDEX:
                {
                    // Cases 1 and 2
                    CSizeofExpression node = new CSizeofExpression();
                    node.hiddenLiteralStringSizeof = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.hiddenLiteralStringSizeof != null) node.hiddenLiteralStringSizeof.setParent(node);
                    node.expression = (ICExpression)valueStack.get(valueStackOffset + 1);
                    if (node.expression != null) node.expression.setParent(node);
                    return node;

                }
                case Production.UNARY_OPERATOR_234_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.UNARY_OPERATOR_235_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.UNARY_OPERATOR_236_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.UNARY_OPERATOR_237_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.UNARY_OPERATOR_238_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.UNARY_OPERATOR_239_INDEX:
                {
                    // Cases 3 and 4
                    Map<String, Object> node = new HashMap<String, Object>();
                    node.put("operator", (Token)valueStack.get(valueStackOffset + 0));
                    ASTListNode<IASTNode> embeddedList = new ASTListNode<IASTNode>();
                    embeddedList.add((IASTNode)(node.get("operator")));
                    node.put("errorRecoveryList", embeddedList);
                    return node;

                }
                case Production.CAST_EXPRESSION_240_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.MULTIPLICATIVE_EXPRESSION_241_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.MULTIPLICATIVE_EXPRESSION_242_INDEX:
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
                case Production.MULTIPLICATIVE_EXPRESSION_243_INDEX:
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
                case Production.MULTIPLICATIVE_EXPRESSION_244_INDEX:
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
                case Production.ADDITIVE_EXPRESSION_245_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.ADDITIVE_EXPRESSION_246_INDEX:
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
                case Production.ADDITIVE_EXPRESSION_247_INDEX:
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
                case Production.SHIFT_EXPRESSION_248_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.SHIFT_EXPRESSION_249_INDEX:
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
                case Production.SHIFT_EXPRESSION_250_INDEX:
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
                case Production.RELATIONAL_EXPRESSION_251_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.RELATIONAL_EXPRESSION_252_INDEX:
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
                case Production.RELATIONAL_EXPRESSION_253_INDEX:
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
                case Production.RELATIONAL_EXPRESSION_254_INDEX:
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
                case Production.RELATIONAL_EXPRESSION_255_INDEX:
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
                case Production.EQUALITY_EXPRESSION_256_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.EQUALITY_EXPRESSION_257_INDEX:
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
                case Production.EQUALITY_EXPRESSION_258_INDEX:
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
                case Production.AND_EXPRESSION_259_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.AND_EXPRESSION_260_INDEX:
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
                case Production.EXCLUSIVE_OR_EXPRESSION_261_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.EXCLUSIVE_OR_EXPRESSION_262_INDEX:
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
                case Production.INCLUSIVE_OR_EXPRESSION_263_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.INCLUSIVE_OR_EXPRESSION_264_INDEX:
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
                case Production.LOGICAL_AND_EXPRESSION_265_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.LOGICAL_AND_EXPRESSION_266_INDEX:
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
                case Production.LOGICAL_OR_EXPRESSION_267_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.LOGICAL_OR_EXPRESSION_268_INDEX:
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
                case Production.CONDITIONAL_EXPRESSION_269_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.CONDITIONAL_EXPRESSION_270_INDEX:
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
                case Production.CONSTANT_271_INDEX:
                {
                    // Cases 1 and 2
                    CConstantExpression node = new CConstantExpression();
                    node.constant = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.constant != null) node.constant.setParent(node);
                    return node;

                }
                case Production.CONSTANT_272_INDEX:
                {
                    // Cases 1 and 2
                    CConstantExpression node = new CConstantExpression();
                    node.constant = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.constant != null) node.constant.setParent(node);
                    return node;

                }
                case Production.CONSTANT_273_INDEX:
                {
                    // Cases 1 and 2
                    CConstantExpression node = new CConstantExpression();
                    node.constant = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.constant != null) node.constant.setParent(node);
                    return node;

                }
                case Production.EXPRESSION_274_INDEX:
                {
                    // Cases 1 and 2
                    ASTExpressionNode node = new ASTExpressionNode();
                    node.conditionalExpression = (ICExpression)valueStack.get(valueStackOffset + 0);
                    if (node.conditionalExpression != null) node.conditionalExpression.setParent(node);
                    return node;

                }
                case Production.ASSIGNMENT_EXPRESSION_275_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.CONSTANT_EXPRESSION_276_INDEX:
                {
                    // Case 5
                    ICExpression result = (ICExpression)valueStack.get(valueStackOffset + 0);
                    return result;

                }
                case Production.IDENTIFIER_LIST_277_INDEX:
                {
                    // Case 7 with separator
                    ASTSeparatedListNode<ASTIdentifierNode> list = new ASTSeparatedListNode<ASTIdentifierNode>();
                    ASTIdentifierNode elt = (ASTIdentifierNode)valueStack.get(valueStackOffset + 0);
                    list.add(null, elt);
                    if (elt != null) elt.setParent(list);
                    return list;

                }
                case Production.IDENTIFIER_LIST_278_INDEX:
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
                case Production.IDENTIFIER_279_INDEX:
                {
                    // Cases 1 and 2
                    ASTIdentifierNode node = new ASTIdentifierNode();
                    node.identifier = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.identifier != null) node.identifier.setParent(node);
                    return node;

                }
                case Production.IDENTIFIER_280_INDEX:
                {
                    // Cases 1 and 2
                    ASTIdentifierNode node = new ASTIdentifierNode();
                    node.identifier = (Token)valueStack.get(valueStackOffset + 0);
                    if (node.identifier != null) node.identifier.setParent(node);
                    return node;

                }
                case Production.STRING_LITERAL_TERMINAL_LIST_281_INDEX:
                {
                    // Case 8
                    IASTListNode<Token> list = (IASTListNode<Token>)valueStack.get(valueStackOffset);
                    Token elt = (Token)valueStack.get(valueStackOffset + 1);
                    list.add(elt);
                    if (elt != null) elt.setParent(list);
                    return list;

                }
                case Production.STRING_LITERAL_TERMINAL_LIST_282_INDEX:
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

        protected static final int[] rowmap = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 10, 11, 12, 0, 13, 0, 14, 0, 15, 1, 16, 0, 0, 1, 2, 17, 18, 19, 3, 20, 4, 21, 22, 23, 5, 6, 7, 8, 9, 24, 10, 25, 26, 11, 27, 28, 29, 12, 30, 13, 31, 32, 14, 16, 33, 34, 35, 17, 36, 37, 38, 39, 18, 40, 41, 42, 43, 44, 45, 46, 47, 19, 48, 20, 49, 50, 51, 52, 53, 54, 21, 55, 56, 57, 58, 59, 60, 61, 62, 22, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 23, 78, 79, 80, 81, 24, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 25, 92, 93, 94, 95, 96, 26, 97, 98, 99, 27, 100, 28, 101, 102, 1, 2, 3, 4, 5, 6, 29, 7, 8, 9, 10, 103, 2, 11, 12, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 13, 30, 126, 14, 15, 127, 1, 16, 128, 129, 0, 0, 130, 17, 131, 132, 18, 133, 3, 62, 94, 134, 135, 0, 1, 2, 32, 3, 33, 4, 136, 34, 5, 35, 37, 6, 38, 7, 137, 39, 8, 138, 139, 140, 9, 40, 41, 42, 10, 141, 11, 12, 44, 142, 13, 143, 4, 144, 145, 146, 5, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 19, 20, 21, 3, 174, 14, 15, 16, 30, 175, 176, 177, 178, 179, 17, 180, 181, 182, 183, 184, 185, 186, 187, 6, 188, 18, 189, 190, 191, 192, 193, 12, 14, 15, 16, 17, 18, 19, 20, 21, 22, 194, 195, 196, 45, 46, 47, 49, 197, 50, 198, 51, 23, 199, 24, 52, 53, 54, 55, 200, 56, 57, 58, 59, 201, 60, 23, 19, 202, 61, 63, 64, 134, 203, 65, 66, 67, 204, 205, 206, 7, 207, 208, 209, 210, 25, 135, 68, 211, 212, 213, 26, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 27, 229, 230, 231, 232, 24, 233, 12, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 25, 261, 262, 69, 263, 70, 264, 71, 265, 266, 267, 14, 268 };
    protected static final int[] columnmap = { 0, 1, 1, 2, 3, 4, 5, 1, 6, 3, 7, 8, 4, 9, 10, 11, 12, 5, 9, 13, 14, 15, 15, 16, 16, 0, 17, 18, 19, 20, 20, 18, 21, 22, 23, 15, 24, 25, 26, 21, 16, 27, 18, 28, 29, 30, 31, 32, 33, 30, 34, 35, 36, 37, 38, 39, 40, 26, 41, 42, 43, 44, 45, 29, 46, 47, 48, 49, 50, 33, 51, 52, 53, 44, 34, 54, 37, 51, 55, 35, 56, 57, 58 };

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
            final int rows = 417;
            final int cols = 3;
            final int compressedBytes = 619;
            final int uncompressedBytes = 5005;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtVz1TwkAQ3cQzZqwYtEh5UllSWFBYiJUfdBb+nRtLq8hYpG" +
                "T4BdJpR8lvsPC3SD55F7JhQ4RxRlMwb5bdd3uXvbcb0hQ/V8lv" +
                "imkfWNOB4xA5+lAYi8+bodGDE9GpyQxp+nQWLCNcCmNzkPl0Eh" +
                "6/iA3B/mb6o8fDyJzOiQyp+G9FCacPq1XjPIeu4THn34anKW66" +
                "bpvcZpEePRxEYbhmvxw9HkWv47lsXzp7p24NtvkHMX8Y8+c18E" +
                "Q8xliJf1N+bi3kQfsgP5OWeBeciOE+nlzr487EqHMf6+RX4vSO" +
                "d+c2tuu82mcXsT+FJTnMon5Sb6GQZ5JpoGt4zPlLeNJHQRltha" +
                "3aa8qD+lP0gprcck0bm7+NufOfRReFtnNnaOv2ql/b59yv1Fhn" +
                "1dMD7yopsxtd8ud4OB/UNJgTvjz1PPXfe/fXWoRNtrXlHEMLT9" +
                "HUH/SGyz0ucrtrn9Wnp0zsc+tq1o786LNQ6RhT5rR8vNgn0MOS" +
                "Txqb5sPyL2OnfpDkv3Hvdzpby6dhTT6lPRbnY/GoFQ+OfBiLOe" +
                "BZWTqPcx1XA6h7YYY7c97/H8uxRHtxxi71heR/qAEX62EXmLaI" +
                "tWfXah/UzBdrHm6WQ0kzCztoowGOfWI2Z9BViSZItLGsV/2Kvo" +
                "8+XJ9qk4MEo6ahHXsEvvema2HBNI7l5mG8vy8wK0ruAjfXcd8I" +
                "H7AWh5GH88F5Q8IpwXhnm661NttUzCFwL7APIsb7hf06fxyhXk" +
                "nmFuy52PctDPOGNduI8GoO4Xo3YpwlOB+r73M9qOa7Iz8fiY7V" +
                "6c8mO+ZjyH6PuFblO/0G5qZ7ag==");
            
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
            final int rows = 165;
            final int cols = 30;
            final int compressedBytes = 3838;
            final int uncompressedBytes = 19801;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNq1XGmsnVUVfVprKRVfee/1tQ8lqJQyFayvMrVoz6VoqwxSQZ" +
                "RSUBOcUESc23Q4Hx0EO0GhQMCBhEERkYhgNCYGE38YFXGAWFON" +
                "Pwz+AIf2RxMS/nj22Wd9a+/vnkseTXw73z7r7LXXXvfcifvaW3" +
                "pjcbw5uTklrmpOjfPD6+KC5oTmpLgiLozvie+NI3E0LovD8ea4" +
                "Lc6Ok72xeEY8Mx4Z58WJODcuicfG4+KWZmGc05w+NBSXx+PjCW" +
                "k9MZ48lH7iyua0OBaXNgvi0UkZ4qJUWzyUf8JL4SXJuW9+mBkX" +
                "aCUulEpTuuLXpSdOpiv5ppx8U86+qWtSNakv+aZ68o3P6Ny4Dz" +
                "6JXaRd8tM7qnl7Up5RfI+MC4rTwiHzE7fnPNk7SnwTSr65kn1N" +
                "X3ve+Eyp7IOP+CY9zntQI65K1/wwPZ037eCrOO12xG2Jn0yXnP" +
                "dgPu9B9Y1bdEJ73oPlvAdFL76K8nkPim/eH9AQlHynJd+8K74Z" +
                "p93OzE+mS3wPZN8DOC8mFN8DxVdniu8BvZLvAZ2azj+zN1NyuZ" +
                "9fGxcU3N7P6Xm1Lu4q9/NMez/Ha3k/N++I13fv57jS3M8z8/08" +
                "My6OMe+n9aZJVt8nh5JvqsT79PmsvunaXXyn1R/fZplM0Odz80" +
                "76pv59can6xAeT7zQ8vr1ZvVmSy3lflV5HIaFD1jed95biO2vQ" +
                "edPuevs64nmL76xmWvKd1Z43xVC+8j3/alT8T7w1nek89DXvyn" +
                "k5eatRFJ9pzhfc3s8mZzS9N11y8X2N4GZFn+8edA8N+Oky+jrq" +
                "TW99p/uu8GJ4sbkwvJhu/wVpN0MrZMuU2xTr3mbM6GrS8zn3t+" +
                "8bHU1vRm+G5MIeQexu/R3oHnjeGbXzNhe1553hu8IfbHeYVZ8a" +
                "7yyvlouHpvjTfb9q7vJ8OKQhKD2f35feN/KuvG8cai7J6h9kfj" +
                "Jd8r5xKL9vHGrfN8qE8r5xqLxv6Mzk26yKb8vdi9I+vY6a9yfm" +
                "N+5WvL7cyvTMcq/fH5ZbveZlz7jcn9e+jpp7Y3p9NleCb25wvt" +
                "8cMHF14Z89vPu5uaqrD78zvfPDG/r/e5TfNx7T943wp5d73xj8" +
                "Phk35Lwo6cv7Rvi1Oe0HJIDdrX9Ca8012oE+9nOC5u7zqm/+H0" +
                "39QxLAzvenWmuu0w70sZ8TNFd97fynTP1KCWDn+zOtNTdpB/rY" +
                "zwmaq752/m9N/YMSwM73UdS0A33s5wTNVV87//emfrUEsPP9OW" +
                "ragT72c4Lmqq+d/3R8Ov4qJvf4VLhUIj4b02MeLs2qf8RfpPzL" +
                "+Nf4N61JB7KusTwz498ta33j/uJ1KXTpHXPE3J6rJIAr9/NVvR" +
                "HtQB/7OUFz3+e6ET+/udOoLpcArvherj3IXO0EzdX72cxv7jH1" +
                "KySAK75XaA8yVztBc9X3im5/+351cf3zc/Nafb9K12F+ftb3q7" +
                "gXn6/crb1MAtj5zkBNO9DHfk7QXD3vZd3+Ur9EAtj5HoGadqCP" +
                "/ZzQX63NT7eIr6PVEuV1tLryOlotF7Ou7nXUstXX0WrovG96fD" +
                "8SF4gvPj+Lb/rvUfGNk83V/Y8vfQd9jlVf+fzcXFl9fH8kQUQc" +
                "HxWsl8/osjsqmcm3+39J5HVNDt2tAWc61sjFrKsw2mtZVwVeA5" +
                "2/nxOT3kX08S2ce3z77p/8nmMf3/rnDTy+UBT8bwlZ03W37i3H" +
                "jj7fu7UHvY4zVTPvbtPxHwlZ03WH7i3Hjr73mSXag17na6pk4u" +
                "PV19cjEkTE5X3yEd3ZrD1eQyUz+Xb/XwlZ0zWn7GWdkyJjdNR/" +
                "wKjKVsu0OdqhM03H8xKypuso3ePzc8bL0DHA93ms9vOzVOX1m6" +
                "pLtUM+P8efmEfJPp/n6fNZ1jAvzJPnc8oDns9F3z6fRWWfz3na" +
                "vqTPz2edaW7ZCxKypmtu2cs6N0XG6Bhw3hfaOXN9tUybqx06c8" +
                "B5x8p5xwSHsXzesSmfd6xz3rF83rFy3jG9qr4jxVc+lYyEkew7" +
                "MmXfkY7vSPYdKb4jelXusYcliIjL6+hh3dmsPV5DJTN57N15R8" +
                "t5RwWH0Xze0Smfd7Rz3tF83tFy3lG9qvfzcPEdFhyGs+/wlH2H" +
                "O77D2Xe4+A7rVfWdKL4TgsNE9p2Ysu9Ex3ci+04U3wm9qr7jxX" +
                "dccBjPvuNT9h3v+I5n3/HiO65X5Xn1kAQRcXlePaQ7m7XHa6hk" +
                "Jo+9njdsyufdlFb9fLWp8vlqk3Ygl377+aplq5+visr5rs++69" +
                "Oqvusrvuu1A7n0W9+WrfoWlfO9MfvemFb1vbHie2OzXjq0L0+S" +
                "fuvbslXfonK+G7PvxrSq78aK70btQC791rdlq75F5Xy3Zt+taV" +
                "XfrRXfrdqBXPqtb8tWfYvK+W7JvlvSqr5bKr5btAO59Fvflq36" +
                "FpXz3ZB9N6RVfTdUfDdoB3Lpt74tW/UtKucbs29Mq/rGim/UDu" +
                "TSb31btupbVM53bfZdm1b1XVvxXasdyKXf+rZs1beonO+27Lst" +
                "req7reK7TTuQS7/1bdmqb1E53yb7NmlV36bi22gHcum3vi1b9S" +
                "0q6xtXZd+v6u+D+H3f+9Z/37e+9d/34Zt+33+y77ybs+/mtOp5" +
                "N1fOu1k7kEu/PW/LVs9bVPnPd45mJOYbWrEsu2TVHTMnUEOlV3" +
                "ul+X1/h0Q5747KeXfIxayrO2/LVs9bVH1/zrBbovjurvjulotZ" +
                "V+fbslXfourz3SNRfPdUfPfIxayr823Zqm9R9fnukii+uyq+u+" +
                "Ri1tX5tmzVt6j6fPdKFN+9Fd+9cjHr6nxbtupbVH2+2yWK7/aK" +
                "73a5mHV1vi1b9S2qPt/bJYrv7RXf2+Vi1tX5tmzVt6gqf371tf" +
                "/z+9U/q+fdKVHOu7NyXvnb9p3Murrztmz1vEXV9/n5FgngGq89" +
                "yFzthP6qZyv1WyWAa7z2IHO1E/qrnq3Ub5IArvHag8zVTuiver" +
                "ZSv00CuMZrDzJXO6G/6tlK/QQJYFtlTXfMts+zdoad362m+kkS" +
                "wLbKmu6YbZ9n7Qw7v1tN9VMlgG2VNd0x2z7P2hl2frea6idKAN" +
                "sqa7pjtn2etTPs/G411d8qAWyrrOmO2fZ51s6w87vVVD9dAthW" +
                "WdMds+3zrJ1h53erqX6cBLCtsqY7ZtvnWTvDzu9WU/14CWBbZU" +
                "13zLbPs3aGnd+tpvppEsC2yprumG2fZ+0MO79bTfVFEsC2ypru" +
                "mG2fZ+0MO79bTfU3SgDbKmu6Y7Z9nrUz7PxuNdUXSADbKmu6Y7" +
                "Z9nrUz7PxuNdVPlgC2VdZ0x2z7PGtn2Pndaqq/SQLYVlnTHbPt" +
                "86ydYed3q/nvf4+pf1+loM7nq5f7vop+X7Qzf1F8Ht9XcbfnLR" +
                "LAtsqa7phtn2ftDDu/W031UySAbZU13THbPs/aGXZ+t5rqb5YA" +
                "tlXWdMds+zxrZ9j5hdvPSI/vZFyQUfkeY8bp8S38ZLrke1/78/" +
                "e+9uvj26rL9xgTSo9vrqwM6RNsXKq8fI8x1dLjG/Z3bs9CCWBb" +
                "ZU13zLbPs3aGnd+tpvp8CWBbZU13zLbPs3aGnd+tpvoD4YGBfz" +
                "t1GEytt9Yd7gv3DVQcBjNVh3BvuHeg4jCYqTqE+8P9AxWHwUzV" +
                "Ib2Ovj3wb022DWS2TN23mVn9HsWD4cGBt/QwmKk6hFUSwDVee5" +
                "C52gn91dp8V/+oBBEweWWQuXq9V6LDzuz4flaCCJi8Mshcvd4r" +
                "0WFndnxvkCACJq8MMlev90p02Jkd3y9KEAGTVwaZq9d7JTrszI" +
                "7v5ySIgMkrg8zV670SHXZmx/daCSJg8sogc/V6r0SHndnx/YoE" +
                "ETB5ZZC5er1XosPO7Ph+RoIImLwyyFy93ivRYWd2fL8sQQRMXh" +
                "lkrl7vleiwMzu+10sQAZNXBpmr13slOuzMju/nJYiAySuDzNXr" +
                "vRIddmbH95MSRMDklUHm6vVeiQ47s+N7nQQRMHllkLl6vVeiw8" +
                "7s+H5CggiYvDLIXL3eK9FhZ3Z8PyVBBExeGWSuXu+V6LAzO75f" +
                "kCACJq8MMlev90p02Jkd349LEAGTVwaZq9d7JTrszI7vNRJEwO" +
                "SVQebq9V6JDjuz4/sxCSJg8sogc/V6r0SHndnx/ZIEETB5ZZC5" +
                "er1XosPO7Psc+2H9fb/77+nqv+93v+/d/+9Dc15aWPk+8LHVz7" +
                "GfliACJq8MMlev90p02Jkd38ckiIgl4/IZXXZHJTN57J3zXUOv" +
                "4OeVdQ9WhO9LEBGDx44ZnNVQyUyeCuM8m2uYrTvUpnCa2bW9n1" +
                "nVfU+CiBg8dszgrIZKZvJUtMrvShARg8eOGZzVUMlMnopW+R0J" +
                "ImLw2DGDsxoqmclTkavrNId1dt+5XevQgT72o8Oy/XrWwz5G2n" +
                "1LK5Zll6y6Y+YEaqj0aqvszWakyj1aab8DUjB47Jg5gRoqvdoq" +
                "w3MazbnhuXJ7nwMibs5plgrSnc2JO1snUJOrS2x/c1ZX2ZvLKL" +
                "e3RcTgsWPmBGqo9GqrDH9mlNvbImLw2DFzAjVUerVV9uYxyu1t" +
                "ETF47Jg5gRoqvdoqw18Y5fa2iBg8dsyc0Lwbeyq92ip7xzDK7W" +
                "0RMXjsmO0E7Kn0aqvsDTOKrkXE4LFj5gRqqPTqrrK9V2+WAK68" +
                "39ysPchc7YT+qmcr9SckiIjBY8cMzmqoZCZPRav8sQQRMXjsmM" +
                "FZDZXM5KlolcdKANsqa7pjtn2etTPs/G411R+XICIGjx1zeNyr" +
                "yWo/MvnQ92+QwnIJImLw2DGH5V5NVvuRyYflfb6LJYiIwWPHHB" +
                "Z7NVntRyYf+j65h3MliIjBY8cMzmqoZCZPRau8UIKIGDx2zOCs" +
                "hkpm8lS0yvMkiIjBY8cMzmqoZCZPRau8QIKIGDx2zOCshkpm8l" +
                "S0yoskiIjBY8cMzmqoZCZPRas8X4KIGDx2zOCshkpm8lS0yhUS" +
                "RMTgsWMOK7yarPYjkw99/xeVsESCiBg8dszgrIZKZvJUtMplEk" +
                "TE4LFjDsu8mqz2I5OnolX2JIiIwWPHHHpeTVb7kcmHvv9NTjhH" +
                "gogYPHbM4KyGSmbyVLTKlRJExOCxYw4rvZqs9iOTDyv7fM+WIC" +
                "IGjx0zOKuhkpk8Fa3yTAkiYvDYMYOzGiqZyVPRKs+SICIGjx0z" +
                "OKuhkpl82f8PX4IADw==");
            
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
            final int rows = 104;
            final int cols = 30;
            final int compressedBytes = 1623;
            final int uncompressedBytes = 12481;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrFmsuPFFUUxoeRTCaTCQkxGlmoC18xcaEgbghwS3yBmqgLNy" +
                "5UiC9ixOhON1VbVupO4wb+AB8r3295uDcujG+NwBYUMhMezqlb" +
                "X/3Oqe4Z6Wom3Sf11Xfud373Nk0P0zBMTfFIW61weOXqUGWegU" +
                "TJIVryHiscXrk6VJlnIFFyiJbcaYXDK1eHKvMMJEoO0ZJ3W+Hw" +
                "ytWhyjwDiZJDtOQ2KxxeuTpUmWcgUXIIPcrr023ljbX7t7yl3F" +
                "nuqv328pUm37h0bS7vWNKryg1Lure8ury2pfeVO8rryhuW3E3l" +
                "zfXKfbVuyXl1WXlr9Wi5qSwHfr1brHB45epQZZ6BRMkhWrJ+4P" +
                "DK1aHKPAOJkkO05C4rHF65OlSZZyBRcoiWPJAOTC3z6JMMmx1l" +
                "mkf1+NRYj+qxntwTEzp395jnPtmPK64Y79y+fLVnQq/zUxM69+" +
                "kJnfvMhM59djXOTY9YyQ/L84yUu99hcHXY/iM+3+cm9DrvXZXX" +
                "ebcVTp48J1LukY+kJvyeIz/f5yf0Or8woXP3rc656c1Rdhltuh" +
                "/RPN8XV+f7YFrPPa3PndYu4lezflgf9+z5fF+a0Pvq5Uv9OqdX" +
                "s+a7+sGZPKE55jXh00Ge9fR3LnOs+BSvGa/s4ecG57tk+p1q5l" +
                "uHV64OZQcYyEh7Mv1BNVzr8MrVoewAAxlpT6YTucw13Am5rteM" +
                "V03FucH5LllsoJrPmq3DK1eH+h3UQ0bak+lYLnPN8zwm1/Wa8a" +
                "qpODc43yXT8Vzmmvnjcl2vGa+ainOD88PJZnK/lfyQr7/9eUbK" +
                "3e8wuBrTIevXWMn7VdZyh/q5mPo9/P7d1aX1261weOXqUGWegU" +
                "TJIUb48/W1CX1feH1C576xKp9jD6aDy35P75FczAnpa7uschd9" +
                "nPGqzDOQKDlEvfq5XVa5iz7OeFXmGUiUHKJe/dguq9xFH2e8Kv" +
                "MMJEoOUa9+a5dV7qKPM16VeQYSJYeoVz+yyyp30ccZr8o8A4mS" +
                "Q7Tk+1Y4vKmuqJryHSRKrr5Z/dAuq9xFH2e8KvMMJEoOUa9+ap" +
                "dV7qKPM16VeQYSJYeoV7+xyyp30ccZr8o8A4mSQ9SrX9hllbvo" +
                "44xXZZ6BRMkh6tVP7LLKXfRxxqsyz0Ci5BD16pd2WeUu+jjjVZ" +
                "lnIFFyiJbcY4WTJ8+JlHvkI6kJv6djPrPLKnfRxxmvyjwDiZJD" +
                "tOR3Vji8qa6omvIdJEquvln9yi6r3EUfZ7wq8wwkSg5Rr35gl1" +
                "Xuoo8zXpV5BhIlh3CnHx7pX0kOj/o5ZziR3rHC4ZWrQ5V5BhIl" +
                "h6hXD2XNd/Wd53VIE5pjXhM+HeRZT4tpsXogLS591rufFVJHLa" +
                "r3KmKQYX4os5AWls5dcOcuWCfvzl1Q71XEIMP8MKaYL+ZN3Wfc" +
                "tMLPYeZ7/wRnfhy+uHzMnx/15NOf453bl6/eGvPvKe2/i6ZfKF" +
                "Z8ypTdc4eyAwxkpD2ZfqMarnV45epQdoCBjLQn069Uw7UOr1wd" +
                "yg4wkJH2ZDFbzJq699zsCu/H2dGS6sGYM1WsKdaYuh3W+M7tca" +
                "fmqvp/flQ7hjNy1V1+3+xcP11Mm7o9pn3XOXd6mXMdI1ef21l1" +
                "/VwxZ+r2mFvhdV42q7b+z58Yc5FP59N5U/e1fZ6u2uTXNee1nt" +
                "roGbm4b5dJZ9NZU7f/WbpwbjvntTnXMXJx3y6TzqVzpm7/c3Th" +
                "3HbOa3OuY+Tivl0mXUgXTN3+F+jCue2c1+Zcx8jFfbtMsbZYa+" +
                "reAWt913l3LJNU20mqbcPms6NPP1Ks+JQpu+cOZQcYyEh7Mp3J" +
                "Za7hzsiZrx7Ca8arCN9156uHI1PZz8RP5zLXzJ+WM+/ObWe8iv" +
                "Bdd74+1zF2bjFTzJi635OZ6t5lf39nRktYzY4+/ZPLHCs+xWvG" +
                "K3v4ucH5LplO5jLXzJ+U63rNeNVUnBuc75LpVC5zzfwpua7XjF" +
                "dNxbnB+eFkM7nZCodXrg5V5hlIlByiJd+1wuGVq0OVeQYSJYdo" +
                "ySPpyLKfQHskw2aHTaej6eiyRI9ktBNW5L4f83N7T764csy/p/" +
                "Tkq7cv1c/Z008UKz5lyu65Q9kBBjLSnkw/Uw3XOrxydSg7wEBG" +
                "uku2xHtWOLxydagyz0Ci5Lkv1o30vljX+x21bhw+/TXm11FPPv" +
                "0w5rnw/wFHPT+z");
            
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

    protected static int lookupValue(int row, int col)
    {
        if (row <= 164)
            return value[row][col];
        else if (row >= 165)
            return value1[row-165][col];
        else
            throw new IllegalArgumentException("Unexpected location requested in value1 lookup");
    }

    static
    {
        sigmapInit();
        valueInit();
        value1Init();
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

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 3, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 7, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 0, 10, 12, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 11, 0, 14, 15, 0, 16, 17, 0, 2, 18, 0, 0, 2, 0, 0, 19, 0, 0, 20, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 22, 0, 0, 0, 0, 0, 0, 23, 24, 0, 0, 0, 0, 25, 0, 0, 0, 13, 0, 0, 0, 14, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 26, 27, 28, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 29, 30, 31, 32, 0, 33, 0, 34, 35, 0, 36, 37, 38, 39, 40, 0, 41, 42, 43, 44, 0, 45, 0, 0, 0, 46, 47, 48, 0, 0, 49, 50, 51, 0, 0, 0, 17, 0, 0, 0, 0, 52, 0, 53, 0, 0, 0, 54, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 55, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 56, 0, 57, 0, 58, 0, 0, 0, 0, 0 };
    protected static final int[] columnmap = { 0, 0, 1, 2, 3, 4, 5, 6, 7, 0, 8, 9, 10, 11, 12, 13, 3, 14, 15, 4, 16, 17, 18, 19, 0, 1, 20, 0, 21, 22, 6, 23, 24, 25, 2, 26, 27, 28, 29, 30, 3, 31, 32, 1, 33, 34, 4, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 7, 48, 8, 2, 49, 50, 1, 1, 51, 52, 6, 53, 1, 54, 0, 10, 0, 3, 11, 55, 16, 3, 56, 57, 58, 59 };

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
            final int rows = 417;
            final int cols = 3;
            final int compressedBytes = 383;
            final int uncompressedBytes = 5005;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtV0FuwyAQXBBCUCmRLfXQI4l86NGq8gBUWVGu/UGfEvXS3P" +
                "KVPiGv8HuCTUKpA8auQxXXGfmwWtj1ehkGMwNGAL6Sg4TRAKO1" +
                "eAJaEGCwrRzPp4EE4LPQLjVHuanEmIH2mFgVRYsENoDT8oURWg" +
                "jYVUPv8AGkyjE1nPsJqjND8Jquxf7Rl2c4weplFL5RR37NhytB" +
                "Rsgjb5UPeu+08CGfL7hEXHC0ilYDqzm5ifil8gb4EOhDWi4tje" +
                "pVA34rl3sVC9vdX1NIeLS0ItRDTxs1/d/cy36T06d7wflTQK3h" +
                "xKnhjv6vuq6vT8Pt9XXD1nCv9ndaL+e7umhdDOQUeODLR1mD4c" +
                "YiG5Q/n/eL7XtetOvqHebs6N6fLtorpvSP3aKl/x5m/yLSsC19" +
                "qG0Ep3PcqSFO/8yc+z4emhrO+XUNSD2kYQtPDXoOWPPbbFcsb/" +
                "jDWkcubBnIY+ukz46035s9Z1e4I4x9o4T/Z+4YgXb9uF9c+nkW" +
                "5y58BBBJPHU=");
            
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
            final int rows = 59;
            final int cols = 30;
            final int compressedBytes = 986;
            final int uncompressedBytes = 7081;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrNmGdoFEEUx/9PE2ti70o0KihKLCAWUEFRsSuWaOy9YMHee+" +
                "9+MIq9odhBEBREEZQkiBKQQELyQTScqKixEmNd376dW3N3e+de" +
                "crfrwLy3b2Z3fjez/zc7CTrhPh6gHGLRAOWRQi3QHQ/xCI/RBE" +
                "2RiTi0RCtkIQk5KItcdEUe2qAteqAneqE3OqML+iAf8bgOQhmk" +
                "oSEaIR3dkIAMVEAiKqIynqA1shGDp3iG5+gAD25gO0ZiE7ZiIE" +
                "ZQPFVm7maM0jQMwVCNC4ZhONsdFCdRV7HJinsLnTnqI203NSkY" +
                "gEHiB4vdqAUUjNY0aoopfDWD61iO4qkKuqveiZgsfiqm6d6HO9" +
                "3gUvNi3NvquXEYL36C2EkW3JlcV2AJ24VYjFlYydyqmKd652MR" +
                "2zVYimXslxtc1bfaZ5z1Yu+oaDbmiJ8rdoEFd51P1EDmW82M+f" +
                "2K5/fLNukvF6loazHaXeX5/YpPEJtocWcHrodxlapLdES1nlD+" +
                "JE6LP4OzbM9RDYnOi72Ai7iEy+rOa2Jfqugojok/LvaUBfeK8l" +
                "5d3eP51uRZV5JWS12pJ5ItRnsVjq7Y6nn0wtCVajfyiHWFTENX" +
                "yGKfwzWXa56uK7+R8rm+EfXV0nWFdF1XyPDqCk+4ZnPlPNJ1BQ" +
                "/b1z66ql1SXZmRLV3hHZVh+wGf8BZfmVsHBar3PT7y74/BZ3zh" +
                "qFDlUZHYb/iOH/ip7vzlM+pviCdxRIFcirXQc91/65nqW+nZfM" +
                "6WnqmRGR0Rbr2geo4LrWdznLD1TO2Zq896TEn0bPbZ1LOs3HBz" +
                "n2xo9lruk6pvegiurX2Sxxvho+cEZ/RMIyV/lZ6ltUDlL+sZmY" +
                "ae+TtYaOQviiR//fSs56+6TtP1rOcvQc9fQ8/F85caUyw8NMdP" +
                "V02c0RXtl6u/+ZvoXP5SM81GMb4L0Sk836A7QnFdRYjWzo356n" +
                "oOYHTE44D71PfIcoz8CHF7RJtL/QK5Rv7KVaZq8fv+BudSX4nT" +
                "xWaY/T7fX/aeIPPt78Y60xDvfKPJ9cujUc7lkQ93tEvcFKe4of" +
                "Qc6fdLS9T9Hoe5S71cWvb/5G8gl5bb5ao43S8O4NIKd+ZLq3Qu" +
                "rY4Yd6XddbadY2s1Vwqtczd/S6crWh/N/PXnynpt0LnR3TcsuR" +
                "vtc2lTpNfZ3r5R0vnS5iD3RXmdaYtL3K0ucbe5w/XXFW336sqd" +
                "76/5O3a6xN0VPpd2lyZ/Q60z7XHi/GzB3esSd5/jfy8cCNHn4P" +
                "k5/HMO0v51zqHUMM85ByN+gjn0f/yfoVTnyRM2z7EnI8y1fY51" +
                "XFendC7+AEvvvyI=");
            
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

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    protected static final int[] columnmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

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
            final int rows = 417;
            final int cols = 3;
            final int compressedBytes = 28;
            final int uncompressedBytes = 5005;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtwTEBAAAAwqD1T20LL6AAAAAAgIsBE40AAQ==");
            
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
