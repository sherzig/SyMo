/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.model;

import java.util.ArrayList;
import java.util.Iterator;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;
import com.phoenix_int.ModelCenter.Assembly;
import com.phoenix_int.ModelCenter.ModelCenterException;
import com.phoenix_int.ModelCenter.Variant;

import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;

/**
 * @author Sebastian
 *
 */
public class ModelCenterModelInstance {

	private Property correspondingProperty_ = null;
	private ArrayList<ModelCenterVariable> outputVariables_ = null;
	private ArrayList<ModelCenterVariable> inputVariables_ = null;
	private InstanceSpecification instanceSpecification_ = null;
	private String fullNameInFinalModelCenterFile_ = "";
	
	/**
	 * 
	 */
	public ModelCenterModelInstance() {
		this.setOutputVariables(new ArrayList<ModelCenterVariable>());
		this.setInputVariables(new ArrayList<ModelCenterVariable>());
	}
	
	/**
	 * 
	 * @param modelCenterModel
	 * @param instanceSpec
	 */
	public ModelCenterModelInstance(Property modelCenterModel, InstanceSpecification instanceSpec) {
		this(modelCenterModel, instanceSpec, true);
	}
	
	/**
	 * 
	 * @param modelCenterModel
	 * @param instanceSpec
	 * @param extractValues
	 */
	public ModelCenterModelInstance(Property modelCenterModel, InstanceSpecification instanceSpec, boolean extractValues) {
		this();
		
		try {
			if(extractValues == true)
				parseModelCenterModelAndExtractOutputs(modelCenterModel);
		}
		catch (ModelCenterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		setCorrespondingProperty(modelCenterModel);
		setCorrespondingInstanceSpecification(instanceSpec);
	}
	
	/**
	 * 
	 * @param modelCenterModel
	 * @throws ModelCenterException 
	 */
	private void parseModelCenterModelAndExtractOutputs(Property modelCenterModel) throws ModelCenterException {
		Assembly currentAssembly = ModelCenterPlugin.getModelCenterInstance().getModel();
		
		for(Iterator<Element> iter=modelCenterModel.getType().getOwnedElement().iterator(); iter.hasNext(); ) {
			Element nextElement = iter.next();
			
			if(nextElement instanceof Port) {
				if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterOutputVariable(nextElement)) {
					Variant value = ModelCenterPlugin.getModelCenterInstance().getValue(currentAssembly.getVariable(((Port)nextElement).getName()).getFullName());
					
					System.out.println("Detected an output for " + ((Port)nextElement).getName() + " with value: " + value.toString());
					
					addOutputVariable((Port)nextElement, value);
				}
			}
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public Property getCorrespondingProperty() {
		return this.correspondingProperty_;
	}
	
	/**
	 * 
	 * @param correspondingProperty
	 */
	public void setCorrespondingProperty(Property correspondingProperty) {
		this.correspondingProperty_ = correspondingProperty;
	}

	/**
	 * @return the outputVariables
	 */
	public ArrayList<ModelCenterVariable> getOutputVariables() {
		return outputVariables_;
	}
	
	/**
	 * 
	 * @param variable
	 * @return
	 */
	public ModelCenterVariable getOutputVariableInstanceForVariable(Port variable) {
		for(int i=0; i<getOutputVariables().size(); i++)
			if(getOutputVariables().get(i).getCorrespondingPort() == variable)
				return getOutputVariables().get(i);
		
		return null;
	}

	/**
	 * @param arrayList the outputVariables to set
	 */
	public void setOutputVariables(ArrayList<ModelCenterVariable> arrayList) {
		this.outputVariables_ = arrayList;
	}
	
	/**
	 * Add an output variable
	 * 
	 * @param correspondingPort
	 */
	public void addOutputVariable(Port correspondingPort) {
		ModelCenterVariable newOutput = new ModelCenterVariable();
		newOutput.setCorrespondingPort(correspondingPort);
		
		this.getOutputVariables().add(newOutput);
	}
	
	/**
	 * Add an output variable and its value
	 * 
	 * @param correspondingPort
	 * @param value
	 */
	public void addOutputVariable(Port correspondingPort, Variant value) {
		addOutputVariable(correspondingPort);
		
		getOutputVariableInstanceForVariable(correspondingPort).setValue(value);
	}
	
	/**
	 * Add an output variable with multiple predefined values
	 * 
	 * @param correspondingPort
	 * @param values
	 */
	public void addOutputVariable(Port correspondingPort, ArrayList<Variant> values) {
		addOutputVariable(correspondingPort);
		
		getOutputVariableInstanceForVariable(correspondingPort).setValues(values);
	}

	/**
	 * @return the inputVariables
	 */
	public ArrayList<ModelCenterVariable> getInputVariables() {
		return inputVariables_;
	}
	
	/**
	 * 
	 * @param variable
	 * @return
	 */
	public ModelCenterVariable getInputVariableInstanceForVariable(Port variable) {
		for(int i=0; i<getInputVariables().size(); i++)
			if(getInputVariables().get(i).getCorrespondingPort() == variable)
				return getInputVariables().get(i);
		
		return null;
	}

	/**
	 * @param inputVariables the inputVariables to set
	 */
	public void setInputVariables(ArrayList<ModelCenterVariable> inputVariables) {
		this.inputVariables_ = inputVariables;
	}
	
	/**
	 * Add an input variable
	 * 
	 * @param correspondingPort
	 */
	public void addInputVariable(Port correspondingPort) {
		ModelCenterVariable newInput = new ModelCenterVariable();
		newInput.setCorrespondingPort(correspondingPort);
		
		this.getInputVariables().add(newInput);
	}
	
	/**
	 * Add an input variable and its value
	 * 
	 * @param correspondingPort
	 * @param value
	 */
	public void addInputVariable(Port correspondingPort, Variant value) {
		addInputVariable(correspondingPort);
		
		getInputVariableInstanceForVariable(correspondingPort).setValue(value);
	}
	
	/**
	 * Add an input variable with multiple predefined values
	 * 
	 * @param correspondingPort
	 * @param values
	 */
	public void addInputVariable(Port correspondingPort, ArrayList<Variant> values) {
		addInputVariable(correspondingPort);
		
		getInputVariableInstanceForVariable(correspondingPort).setValues(values);
	}

	/**
	 * @return the instanceSpecification
	 */
	public InstanceSpecification getCorrespondingInstanceSpecification() {
		return instanceSpecification_;
	}

	/**
	 * @param instanceSpecification the instanceSpecification to set
	 */
	public void setCorrespondingInstanceSpecification(InstanceSpecification instanceSpecification) {
		this.instanceSpecification_ = instanceSpecification;
	}

	/**
	 * 
	 */
	public boolean isUsage(Property property, InstanceSpecification instanceSpec) {
		if(this.getCorrespondingProperty() == property && this.getCorrespondingInstanceSpecification() == instanceSpec)
			return true;
		
		return false;
	}

	/**
	 * @return the fullNameInFinalModelCenterFile
	 */
	public String getFullNameInFinalModelCenterFile() {
		return fullNameInFinalModelCenterFile_;
	}

	/**
	 * @param fullNameInFinalModelCenterFile the fullNameInFinalModelCenterFile to set
	 */
	public void setFullNameInFinalModelCenterFile(String fullNameInFinalModelCenterFile) {
		this.fullNameInFinalModelCenterFile_ = fullNameInFinalModelCenterFile;
	}
	
}
