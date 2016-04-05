package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("serial")
public class SyntaxException extends Exception {
    public SyntaxException(Token lookahead, String terminalsExpectedInCurrentState) {
        super(String.format("Unexpected %s; expected one of: %s", lookahead, terminalsExpectedInCurrentState));
    }
}
