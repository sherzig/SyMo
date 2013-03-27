/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.controller;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.impl.ElementsFactory;

import edu.gatech.mbse.plugins.mdmc.exceptions.ModelCenterProfileNotLoaded;

/**
 * @author Sebastian
 *
 */
public class MDModelFactory {
	
	/**
	 * Check whether profile is loaded
	 * 
	 * @throws ModelCenterProfileNotLoaded
	 */
	private void checkForProfile() throws ModelCenterProfileNotLoaded {
		if(StereotypesHelper.getProfile(Application.getInstance().getProject(), "ModelCenter") == null)
			throw new ModelCenterProfileNotLoaded();
	}
	
	/**
	 * Create a ModelCenter root model of type "data model"
	 * 
	 * @param name
	 * @throws ModelCenterProfileNotLoaded
	 * @return
	 */
	public Class createDataModel(String name) throws ModelCenterProfileNotLoaded {
		// Check whether profile is loaded
		checkForProfile();
		
		// Create a new class and set its name
		Class newDataModel = getElementsFactory().createClassInstance();
		
		// Set name if specified
		if(!name.equals(""))
			newDataModel.setName(name);
		
		// Add the corresponding stereotypes
		StereotypesHelper.addStereotypeByString(newDataModel, "ModelCenterDataModel");
		
		return newDataModel;
	}
	
	/**
	 * Create a generic variable
	 * 
	 * @param name
	 * @throws ModelCenterProfileNotLoaded
	 * @return
	 */
	public Port createVariable(String name) throws ModelCenterProfileNotLoaded {
		// Check whether profile is loaded
		checkForProfile();
				
		// Create a new class and set its name
		Port newVariable = getElementsFactory().createPortInstance();
		
		// Set name if specified
		if(!name.equals(""))
			newVariable.setName(name);
		
		// Add the corresponding stereotypes
		StereotypesHelper.addStereotypeByString(newVariable, "ModelCenterVariable");
		StereotypesHelper.addStereotypeByString(newVariable, "ConstraintParameter");	// Unfortunately this is necessary, even though the stereotype ModelCenterVariable is a generalization of ConstraintParameter
		
		// Check whether stereotype was applied
		if(!StereotypesHelper.getStereotypes(newVariable).get(0).getName().equals("ModelCenterVariable"))
			throw new ModelCenterProfileNotLoaded();
		
		return newVariable;
	}
	
	/**
	 * Create an input variable
	 * 
	 * @param name
	 * @return
	 * @throws ModelCenterProfileNotLoaded
	 */
	public Port createInputVariable(String name) throws ModelCenterProfileNotLoaded {
		// First, create a new variable
		Port newPort = createVariable(name);
		
		// Now set the stereotype property "Is Input" to true
		Stereotype variableStereotype = StereotypesHelper.getStereotype(Application.getInstance().getProject(), "ModelCenterVariable");
		StereotypesHelper.setStereotypePropertyValue(newPort, variableStereotype, "isInput", true);
		
		return newPort;
	}
	
	/**
	 * Create an output variable
	 * 
	 * @param name
	 * @return
	 * @throws ModelCenterProfileNotLoaded
	 */
	public Port createOutputVariable(String name) throws ModelCenterProfileNotLoaded {
		// First, create a new variable
		Port newPort = createVariable(name);
		
		// Now set the stereotype property "Is Input" to true
		Stereotype variableStereotype = StereotypesHelper.getStereotype(Application.getInstance().getProject(), "ModelCenterVariable");
		StereotypesHelper.setStereotypePropertyValue(newPort, variableStereotype, "isInput", false);
		
		return newPort;
	}
	
	/**
	 * Create a script
	 * 
	 * @param name
	 * @return
	 * @throws ModelCenterProfileNotLoaded
	 */
	public Constraint createScript(String name) throws ModelCenterProfileNotLoaded {
		// Check whether profile is loaded
		checkForProfile();
				
		// Create a new class and set its name
		Constraint newScript = getElementsFactory().createConstraintInstance();
		
		// Set name if specified
		if(!name.equals(""))
			newScript.setName(name);
		
		// Add the corresponding stereotypes
		StereotypesHelper.addStereotypeByString(newScript, "ModelCenterScript");
		
		// Check whether stereotype was applied
		if(!StereotypesHelper.getStereotypes(newScript).get(0).getName().equals("ModelCenterScript"))
			throw new ModelCenterProfileNotLoaded();
		
		return newScript;
	}
	
	/**
	 * Returns an object for the element creation factory provided by MagicDraw
	 * 
	 * @return
	 */
	private ElementsFactory getElementsFactory() {
		return Application.getInstance().getProject().getElementsFactory();
	}

}
