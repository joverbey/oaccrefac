package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("serial")
public class SyntaxException extends Exception {
    public SyntaxException(Token lookahead, String terminalsExpectedInCurrentState) {
        super(String.format("Unexpected %s; expected one of: %s", lookahead, terminalsExpectedInCurrentState));
    }
}
