/*******************************************************************************
 * Copyright (c) 2010, 2012 University of Illinois at Urbana-Champaign,
 * Auburn University, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    UIUC - Initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Utility methods for working with strings.
 * 
 * @author Jeff Overbey
 */
public class IOUtil {
	private IOUtil() {
		;
	}

	public static String read(Reader in) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int ch = in.read(); ch >= 0; ch = in.read()) {
			sb.append((char) ch);
		}
		in.close();
		return sb.toString();
	}

	public static String read(InputStream in) throws IOException {
		return read(new InputStreamReader(in));
	}

	public static String read(File file) throws IOException {
		return read(new BufferedReader(new FileReader(file)));
	}
}
