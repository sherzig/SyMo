/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.controller;

import java.util.ArrayList;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

/**
 * <code>WarningItem</code>'s are objects representing warnings that were generated during the process of
 * checking the model prior to a run or transformation.
 * 
 * @author Sebastian
 */
public class WarningItem {
	
	private Severity severity_ = Severity.MC_SEVERITY_INFO;
	private String description_ = "";
	private String explanation_ = "";
	private Class affectedModelCenterModel_ = null;
	private ArrayList<Element> affectedElements_ = null;
	
	/**
	 * Constructor
	 * 
	 * @param severity
	 * @param description
	 * @param explanation	A more detailed description + solution proposal
	 */
	public WarningItem(Severity severity, String description, String explanation) {
		// Set severity and description
		this.setSeverity(severity);
		this.setDescription(description);
		this.setExplanation(explanation);
		
		// Defaults
		this.setAffectedElements(new ArrayList<Element>());
	}
	
	/**
	 * Constructor allowing one to specify not only the severity and description, but also an affected
	 * ModelCenter model and two affected elements
	 * 
	 * @param severity
	 * @param description
	 * @param explanation					A more detailed description + solution proposal
	 * @param affectedModelCenterModel
	 * @param affectedElement1
	 * @param affectedElement2
	 */
	public WarningItem(Severity severity, String description, String explanation, Class affectedModelCenterModel, Element affectedElement1, Element affectedElement2) {
		this(severity, description, explanation);
		
		// Set affected ModelCenter model
		this.setAffectedModelCenterModel(affectedModelCenterModel);
		
		// Add affected elements
		this.getAffectedElements().add(affectedElement1);
		this.getAffectedElements().add(affectedElement2);
	}
	
	/**
	 * @return the severity
	 */
	public Severity getSeverity() {
		return severity_;
	}
	
	/**
	 * @param severity the severity to set
	 */
	public void setSeverity(Severity severity) {
		this.severity_ = severity;
	}
	
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description_;
	}
	
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description_ = description;
	}
	
	/**
	 * @return the explanation
	 */
	public String getExplanation() {
		return explanation_;
	}

	/**
	 * @param explanation the explanation to set
	 */
	public void setExplanation(String explanation) {
		this.explanation_ = explanation;
	}

	/**
	 * @return the affectedModelCenterModel
	 */
	public Class getAffectedModelCenterModel() {
		return affectedModelCenterModel_;
	}
	
	/**
	 * @param affectedModelCenterModel the affectedModelCenterModel to set
	 */
	public void setAffectedModelCenterModel(Class affectedModelCenterModel) {
		this.affectedModelCenterModel_ = affectedModelCenterModel;
	}
	
	/**
	 * @return the affectedElements
	 */
	public ArrayList<Element> getAffectedElements() {
		return affectedElements_;
	}
	
	/**
	 * @param affectedElements the affectedElements to set
	 */
	public void setAffectedElements(ArrayList<Element> affectedElements) {
		this.affectedElements_ = affectedElements;
	}
	
}
