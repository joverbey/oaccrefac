package edu.auburn.oaccrefac.internal.core;

import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTAttribute;
import org.eclipse.cdt.core.dom.ast.IASTAttributeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.parser.IToken;

//TODO be sure this should actually implement IASTForStatement
public class EnhancedASTForStatement implements IASTForStatement {

    private IASTForStatement statement;

    public EnhancedASTForStatement(IASTForStatement statement) {
        //TODO decide if we want to call ForLoopUtil methods here initially 
        //and store results in fields or if we should do it on the fly 
        //when the information is needed
        this.statement = statement;
    }

    //TODO be sure you actually want access to all of these method about the for statement
    
    @Override
    public IASTAttributeSpecifier[] getAttributeSpecifiers() {
        return statement.getAttributeSpecifiers();
    }

    @Override
    public void addAttributeSpecifier(IASTAttributeSpecifier attributeSpecifier) {
        statement.addAttributeSpecifier(attributeSpecifier);        
    }

    @Override
    public IASTAttribute[] getAttributes() {
        return statement.getAttributes();
    }

    @Override
    public void addAttribute(IASTAttribute attribute) {
        statement.addAttribute(attribute);
    }

    @Override
    public IASTTranslationUnit getTranslationUnit() {
        return statement.getTranslationUnit();
    }

    @Override
    public IASTNodeLocation[] getNodeLocations() {
        return statement.getNodeLocations();
    }

    @Override
    public IASTFileLocation getFileLocation() {
        return statement.getFileLocation();
    }

    @Override
    public String getContainingFilename() {
        return statement.getContainingFilename();
    }

    @Override
    public boolean isPartOfTranslationUnitFile() {
        return statement.isPartOfTranslationUnitFile();
    }

    @Override
    public IASTNode getParent() {
        return statement.getParent();
    }

    @Override
    public IASTNode[] getChildren() {
        return statement.getChildren();
    }

    @Override
    public void setParent(IASTNode node) {
        statement.setParent(node);
    }

    @Override
    public ASTNodeProperty getPropertyInParent() {
        return statement.getPropertyInParent();
    }

    @Override
    public void setPropertyInParent(ASTNodeProperty property) {
        statement.setPropertyInParent(property);
    }

    @Override
    public boolean accept(ASTVisitor visitor) {
        return statement.accept(visitor);
    }

    @Override
    public String getRawSignature() {
        return statement.getRawSignature();
    }

    @Override
    public boolean contains(IASTNode node) {
        return statement.contains(node);
    }

    @Override
    public IToken getLeadingSyntax() throws ExpansionOverlapsBoundaryException, UnsupportedOperationException {
        return statement.getLeadingSyntax();
    }

    @Override
    public IToken getTrailingSyntax() throws ExpansionOverlapsBoundaryException, UnsupportedOperationException {
        return statement.getTrailingSyntax();
    }

    @Override
    public IToken getSyntax() throws ExpansionOverlapsBoundaryException {
        return statement.getSyntax();
    }

    @Override
    public boolean isFrozen() {
        return statement.isFrozen();
    }

    @Override
    public boolean isActive() {
        return statement.isActive();
    }

    @Override
    public IASTNode getOriginalNode() {
        return statement.getOriginalNode();
    }

    @Override
    public IASTStatement getInitializerStatement() {
        return statement.getInitializerStatement();
    }

    @Override
    public void setInitializerStatement(IASTStatement statement) {
        this.statement.setInitializerStatement(statement);
    }

    @Override
    public IASTExpression getConditionExpression() {
        return statement.getConditionExpression();
    }

    @Override
    public void setConditionExpression(IASTExpression condition) {
        statement.setConditionExpression(condition);
    }

    @Override
    public IASTExpression getIterationExpression() {
        return statement.getIterationExpression();
    }

    @Override
    public void setIterationExpression(IASTExpression iterator) {
        statement.setIterationExpression(iterator);
    }

    @Override
    public IASTStatement getBody() {
        return statement.getBody();
    }

    @Override
    public void setBody(IASTStatement statement) {
        this.setBody(statement);
    }

    @Override
    public IScope getScope() {
        return statement.getScope();
    }

    @Override
    public IASTForStatement copy() {
        return statement.copy();
    }

    @Override
    public IASTForStatement copy(CopyStyle style) {
        return statement.copy(style);
    }
    
    //TODO move ForLoopUtil stuff to this class
    
}
