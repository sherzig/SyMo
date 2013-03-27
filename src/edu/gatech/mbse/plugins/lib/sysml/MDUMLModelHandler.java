/**
 * This class is part of a common library for development of MagicDraw plug-ins
 */
package edu.gatech.mbse.plugins.lib.sysml;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;

/**
 * MDUMLHandler is a class that takes care of identifying and creating UML elements inside
 * the currently loaded project / model in MagicDraw. Its purpose is to serve as a base class
 * for handling other modeling languages, but can also be used to perform operations on UML
 * models.
 * 
 * @author Sebastian Herzig
 * @version 0.1
 */
public class MDUMLModelHandler extends MDProjectHandler {

	/**
	 * Function that checks whether or not a given element is an instance of a class
	 * 
	 * @param	e	A model element of base type Element 
	 * @return		true if the object is a UML class, false otherwise
	 */
	public boolean isUMLClass(Element e) {
		if(e instanceof Class)
			return true;
		
		return false;
	}
	
}
