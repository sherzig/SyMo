/**
 * 
 */
package edu.gatech.mbse.plugins.lib.sysml;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;

import edu.gatech.mbse.plugins.lib.exceptions.MDModelLibException;

/**
 * MDProjectHandler is a wrapper class to access some rather "nested" functions in the scope of
 * handling a MagicDraw project in a quick and efficient way.
 * 
 * @author Sebastian Herzig
 * @version 0.1
 */
public class MDProjectHandler {

	/**
	 * Function that returns the currently active project in the workspace. If no project is
	 * loaded, the function throws an exception.
	 * 
	 * @return An object of type {@link Project} that represents the currently active project
	 * @throws MDModelLibException
	 */
	public Project getActiveProject() throws MDModelLibException {
		if(Application.getInstance().getProject() == null)
			throw new MDModelLibException("No project is currently active");
		
		return Application.getInstance().getProject();
	}
	
}
