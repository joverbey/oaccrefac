/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.internal.core;

public enum FunctionLevel {
	SEQ {
		@Override
		public FunctionLevel next() {
			return SEQ; //Should never have a level higher than seq unless forced by inherent restrictions.
		};
		
		@Override
		public String toString() {
			return "seq";  //$NON-NLS-1$
		};
	},
	VECTOR {
		
		@Override
		public FunctionLevel next() {
			return WORKER;
		}
		
		@Override
		public String toString() {
			return "vector";  //$NON-NLS-1$
		};
	},
	WORKER {
		
		@Override
		public FunctionLevel next() {
			return GANG;
		}
		
		@Override
		public String toString() {
			return "worker";  //$NON-NLS-1$
		};
	},
	GANG {
		@Override 
		public FunctionLevel next() {
			return null;
		};
		
		@Override
		public String toString() {
			return "gang";  //$NON-NLS-1$
		};
	};
	
	public abstract FunctionLevel next();
}
