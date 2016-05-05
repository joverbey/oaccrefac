package org.eclipse.ptp.pldt.openacc.core.dependence;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * 
 * 
 * @author William Hester
 */
public class AtomicsDependenceAnalysis {
	
	private final Set<IVariable> variables = new HashSet<>();
	
	public static AtomicsDependenceAnalysis forStatement(IASTStatement statement, IProgressMonitor monitor) {
		return null;
	}
	
	private AtomicsDependenceAnalysis(IASTStatement statement, IProgressMonitor monitor) {
		if (statement == null) {
			throw new IllegalArgumentException("statement must not be null");
		}
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask("Atomics Analysis", IProgressMonitor.UNKNOWN);
		try {
			performAnalysis(statement, monitor);
		} catch (Exception e) {
			throw e;
		} finally {
			monitor.done();
		}
	}
	
	private void performAnalysis(IASTNode current, IProgressMonitor monitor) {
		monitor.worked(1);
		if (current instanceof IASTName) {
			IBinding binding = ((IASTName) current).resolveBinding();
			if (binding instanceof IVariable) {
				variables.add((IVariable) binding);
			}
		}
		if (current instanceof IASTExpressionStatement) {
			IASTExpressionStatement statement = (IASTExpressionStatement) current;
//			performAnalysis(statement.)
			IASTExpression expression = statement.getExpression();
		}
	}
	
}
