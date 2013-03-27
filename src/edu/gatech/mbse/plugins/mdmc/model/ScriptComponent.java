/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.model;

import java.util.ArrayList;
import java.util.Iterator;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;
import com.phoenix_int.ModelCenter.Assembly;
import com.phoenix_int.ModelCenter.ModelCenterException;
import com.phoenix_int.ModelCenter.Variable;
import com.phoenix_int.ModelCenter.Variant;

import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterFileManipulator;
import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;
import edu.gatech.mbse.plugins.mdmc.controller.SynchronizationEngine;

/**
 * @author Sebastian
 *
 */
public class ScriptComponent {

	private String name_ = "";
	private String scriptBody_ = "";
	private ArrayList<ModelCenterVariable> variables_ = null;
	private String componentFullName_ = "";
	private Element owningModel_ = null;
	
	/**
	 * 
	 */
	public ScriptComponent() {
		setVariables(new ArrayList<ModelCenterVariable>());
	}
	
	/**
	 * 
	 * @param name
	 */
	public ScriptComponent(String name, Element owningModel) {
		this();
		
		// Set the name of the script
		setName(name);
		
		// Set the owner
		setOwningModel(owningModel);
	}
	
	/**
	 * Function that extracts variables from a given formula. Note that this function is relatively
	 * primitive in its current state and assumes VBScript to be used (or a similar language)
	 */
	public void extractVariablesFromScriptBody() {
		// First clear the list of variables
		variables_.clear();
		
		// Now parse the script body
		String scriptBody = getScriptBody();
		
		// Extract all variables from the script body
		extractVariables(scriptBody);
		
		// Next, update the variables that are output variables
		determineOutputVariables(scriptBody);
	}
	
	/**
	 * 
	 * @param scriptBody
	 */
	private void extractVariables(String scriptBody) {
		// Prepare the script body for easy extraction of variables - we want to make sure that all characters
		// that cannot be part of a modelcenter variable are removed and replaced with a space. Then add spaces
		// at the beginning and end of each line. This leads to each variable being surrounded by at least one
		// space on each side. Note that if a variable has the same name as a keyword, this will lead to an error
		scriptBody = scriptBody.replace("\\r", "");
		scriptBody = scriptBody.replace("\\t", "");
		scriptBody = scriptBody.replace("\\n", " ");
		scriptBody = scriptBody.replaceAll("[^A-Za-z0-9_]", " ");
		scriptBody = " " + scriptBody + " ";
		
		// Get a list of variables that are available in the ModelCenter model
		if(getOwningModel() != null) {
			for(Iterator<Element> iter = getOwningModel().getOwnedElement().iterator(); iter.hasNext(); ) {
				Element nextElement = iter.next();
				
				// Check whether we found a variable
				if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterVariable(nextElement)) {
					// Check whether it is referenced in the script
					String variableName = ((NamedElement)nextElement).getName();
					
					if(scriptBody.contains(" " + variableName + " ")) {
						System.out.println("Found variable " + variableName);
						
						// Get the MC variable type
						String variableType = SynchronizationEngine.getModelCenterTypeForSysMLType((Port)nextElement);
						
						// Get an initial value for the variant
						Variant value = SynchronizationEngine.getInitialValueForModelCenterType(variableType);
						
						// TODO: dimension size?
						
						// Add those to the list of variables of the script
						this.getVariables().add(new ModelCenterVariable(variableName, variableType, true, value));
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param scriptBody
	 */
	private void determineOutputVariables(String scriptBody) {
		// Remove some unnecessary characters and prepare for single character split
		scriptBody = scriptBody.replace("\\r", "");
		scriptBody = scriptBody.replace("\\t", "");
		scriptBody = scriptBody.replace("\\n", "\n");
		
		// Split the script into individual lines
		String[] lines = scriptBody.split("\n");
		
		// Note that the following code is a relatively naive approach at finding output variables, but an
		// effective one for relatively simple scripts
		for(int i=0; i<lines.length; i++) {
			if(lines[i].contains("=") && !(lines[i].contains("=="))) {
				String outputVariable = lines[i].split("=")[0];
				
				// Get first in dot hierarchy
				if(outputVariable.contains("."))
					outputVariable = outputVariable.split(".")[0];
				
				// In case array elements are accessed (VBScript specific)
				if(outputVariable.contains("("))
					outputVariable = outputVariable.split("(")[0];
				
				// Replace any spaces
				outputVariable = outputVariable.replaceAll("[ ]+", "");
				
				System.out.println("Found an output variable: " + outputVariable);
				
				// Update variable direction to "output"
				updateVariableDirection(outputVariable, false);
			}
		}
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name_;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name_ = name;
	}

	/**
	 * Formats (if necessary) the script body and returns the result
	 *  
	 * @return the scriptBody
	 */
	public String getScriptBody() {
		// Check whether the main method exists in the script body
		if(!scriptBody_.contains("sub run")) {
			scriptBody_ = "sub run\\r\\n\\t" + scriptBody_.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\\r\\nend sub\\r\\n";
		}
		
		// Add "generated" line
		if(!scriptBody_.startsWith("'!GENERATED - DO NOT REMOVE THIS LINE!\\r\\n")) {
			// Note: this tag is necessary to identify which scripts were/are generated automatically during
			// synchronization (see SynchronizationEngine class)
			scriptBody_ = "'!GENERATED - DO NOT REMOVE THIS LINE!\\r\\n" + scriptBody_;
		}
		
		return scriptBody_;
	}

	/**
	 * @param scriptBody the scriptBody to set
	 */
	public void setScriptBody(String scriptBody) {
		this.scriptBody_ = scriptBody;
	}

	/**
	 * @return the variables
	 */
	public ArrayList<ModelCenterVariable> getVariables() {
		return variables_;
	}

	/**
	 * @param variables the variables to set
	 */
	public void setVariables(ArrayList<ModelCenterVariable> variables) {
		this.variables_ = variables;
	}
	
	/**
	 * @return the variables
	 */
	public ArrayList<ModelCenterVariable> getInputVariables() {
		ArrayList<ModelCenterVariable> vars = new ArrayList<ModelCenterVariable>();
		
		for(Iterator<ModelCenterVariable> iter = getVariables().iterator(); iter.hasNext(); ) {
			ModelCenterVariable nextVar = iter.next();
			
			if(nextVar.isInput())
				vars.add(nextVar);
		}
			
		return vars;
	}
	
	/**
	 * @return the variables
	 */
	public ArrayList<ModelCenterVariable> getOutputVariables() {
		ArrayList<ModelCenterVariable> vars = new ArrayList<ModelCenterVariable>();
		
		for(Iterator<ModelCenterVariable> iter = getVariables().iterator(); iter.hasNext(); ) {
			ModelCenterVariable nextVar = iter.next();
			
			if(!nextVar.isInput())
				vars.add(nextVar);
		}
			
		return vars;
	}
	
	/**
	 * Updates the direction of the variable. The direction is determined by analyzing the contents of the
	 * script
	 * 
	 * @param variable
	 * @param isInput
	 */
	private void updateVariableDirection(String variable, boolean isInput) {
		// Search for variable
		for(Iterator<ModelCenterVariable> iter = getVariables().iterator(); iter.hasNext(); ) {
			ModelCenterVariable nextVar = iter.next();
			
			// Check whether we found the correct variable
			if(nextVar.getName().equals(variable)) {
				nextVar.setInput(isInput);
				
				break;
			}
		}
	}

	/**
	 * @return the componentFullName
	 */
	public String getComponentFullName() {
		return componentFullName_;
	}

	/**
	 * @param componentFullName the componentFullName to set
	 */
	public void setComponentFullName(String componentFullName) {
		this.componentFullName_ = componentFullName;
	}

	/**
	 * @return the owningModel
	 */
	public Element getOwningModel() {
		return owningModel_;
	}

	/**
	 * @param owningModel the owningModel to set
	 */
	public void setOwningModel(Element owningModel) {
		this.owningModel_ = owningModel;
	}
	
}
