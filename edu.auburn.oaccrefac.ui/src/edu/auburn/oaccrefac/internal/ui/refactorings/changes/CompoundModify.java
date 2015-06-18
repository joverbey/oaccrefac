package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import java.util.LinkedList;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

public class CompoundModify extends CompoundChange {

    public CompoundModify(IASTCompoundStatement compound) {
        super(compound);
    }

    @Override
    protected IASTCompoundStatement doChange() {
        return rebuildCompound();
    }
    
    public IASTStatement insertBefore(IASTStatement toInsert, IASTStatement before) {
        LinkedList<IASTStatement> chilluns = getStatementList();
        int index = 0;
        for (index = 0; index < chilluns.size(); index++) {
            if (chilluns.get(index).equals(before))
                break;
        }
        
        if (index >= chilluns.size())
            chilluns.addLast(toInsert);
        else
            getStatementList().add(index, toInsert);
        
        return toInsert;
    }
    
    public IASTStatement insertAfter(IASTStatement toInsert, IASTStatement after) {
        LinkedList<IASTStatement> chilluns = getStatementList();
        int index = 0;
        for (index = 0; index < chilluns.size(); index++) {
            if (chilluns.get(index).equals(after)) {
                index++;
                break;
            }
        }
        
        if (index >= chilluns.size())
            chilluns.addLast(toInsert);
        else
            getStatementList().add(index, toInsert);
        
        return toInsert;
    }
    
    public IASTStatement insertAt(IASTStatement toInsert, int position) {
        getStatementList().add(position, toInsert);
        return toInsert;
    }
    
    public IASTStatement append(IASTStatement toAppend) {
        getStatementList().addLast(toAppend);
        return toAppend;
    }
    
    public IASTStatement removePosition(int position) {
        LinkedList<IASTStatement> chilluns = getStatementList();
        IASTStatement statement = chilluns.get(position);
        chilluns.remove(position);
        return statement;
    }
    
    public IASTStatement remove(IASTStatement node) {
        LinkedList<IASTStatement> chilluns = getStatementList();
        IASTStatement statement = null;
        int index = chilluns.indexOf(node);
        if (index != -1) {
            statement = chilluns.get(index);
            chilluns.remove(index);
        }
        return statement;
    }
    
    public IASTStatement replace(IASTStatement toReplace, IASTStatement replacement) {
        LinkedList<IASTStatement> chilluns = getStatementList();
        IASTStatement statement = null;
        int index = chilluns.indexOf(toReplace);
        if (index != -1) {
            statement = chilluns.get(index);
            chilluns.add(index, replacement);
            chilluns.remove(index+1);
        }
        return statement;
    }

}
