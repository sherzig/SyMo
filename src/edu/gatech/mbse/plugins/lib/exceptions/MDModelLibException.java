/**
 * This class is part of a common library for development of MagicDraw plug-ins
 */
package edu.gatech.mbse.plugins.lib.exceptions;

/**
 * MDModelLibException is the main class to be used for raising exceptions within
 * the scope of the library. It should be used for any exceptions that do not fit
 * into the category of commonly raised exceptions (e.g.
 * {@link IllegalArgumentException})
 * 
 * @author Sebastian Herzig
 * @version 0.1
 */
public class MDModelLibException extends Exception {

	private static final long serialVersionUID = 5316387091620198576L;

	public MDModelLibException() {
		
	}

	public MDModelLibException(String arg0) {
		super(arg0);
	}

	public MDModelLibException(Throwable arg0) {
		super(arg0);
	}

	public MDModelLibException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
