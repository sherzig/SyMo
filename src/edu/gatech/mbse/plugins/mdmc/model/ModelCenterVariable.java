/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.model;

import java.util.ArrayList;

import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;
import com.phoenix_int.ModelCenter.Variant;

/**
 * @author Sebastian
 *
 */
public class ModelCenterVariable {

	private String name = "";
	private String type = "";
	private boolean isInput = true;
	private Port correspondingPort_ = null;
	private ArrayList<Variant> values_ = null;
	
	/**
	 * 
	 */
	public ModelCenterVariable() {
		this.setValues(new ArrayList<Variant>());
	}
	
	/**
	 * 
	 */
	public ModelCenterVariable(String name, String type, boolean isInput, Variant value) {
		this();
		
		// Set properties
		setName(name);
		setType(type);
		setInput(isInput);
		setValue(value);
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the isInput
	 */
	public boolean isInput() {
		return isInput;
	}

	/**
	 * @param isInput the isInput to set
	 */
	public void setInput(boolean isInput) {
		this.isInput = isInput;
	}

	/**
	 * 
	 * @return
	 */
	public Port getCorrespondingPort() {
		return this.correspondingPort_;
	}
	
	/**
	 * 
	 * @param correspondingPort
	 */
	public void setCorrespondingPort(Port correspondingPort) {
		this.correspondingPort_ = correspondingPort;
	}
	
	/**
	 * 
	 * @return
	 */
	public ArrayList<Variant> getValues() {
		return this.values_;
	}
	
	/**
	 * 
	 * @param values
	 */
	public void setValues(ArrayList<Variant> values) {
		this.values_ = values;
	}
	
	/**
	 * 
	 * @param value
	 */
	public void addValue(Variant value) {
		if(getValues() != null && value != null)
			getValues().add(value);
	}
	
	/**
	 * Helper function for cases when output is only one value
	 * 
	 * @param value
	 */
	public void setValue(Variant value) {
		if(getValues() != null && value != null) {
			// Clear list
			clearValues();
			
			// Add value as first value
			addValue(value);
		}
	}
	
	/**
	 * Helper function for cases when output is only one value
	 * 
	 * @return
	 */
	public Variant getValue() {
		return getValues().get(0);
	}
	
	/**
	 * Clears the list of values (convenience interface function)
	 */
	public void clearValues() {
		if(getValues() != null)
			getValues().clear();
	}
	
}
