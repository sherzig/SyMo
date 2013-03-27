/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.controller;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JOptionPane;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.plugins.PluginsHelper;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceValue;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Slot;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;
import com.phoenix_int.ModelCenter.Array;
import com.phoenix_int.ModelCenter.Assembly;
import com.phoenix_int.ModelCenter.ModelCenterException;
import com.phoenix_int.ModelCenter.Variable;
import com.phoenix_int.ModelCenter.Variant;

/**
 * @author Sebastian
 *
 */
public class ModelCenterValuesImporter {
	
	private InstanceHandler instanceHandler_ = null;

	/**
	 * Default constructor
	 */
	public ModelCenterValuesImporter() {
		this.setInstanceHandler(new InstanceHandler());
	}

	/**
	 * Loads the "full" ModelCenter model representation of the SysML model, finds the part 
	 * @param instanceSpec
	 * @throws ModelCenterException 
	 */
	public void updateInstanceValues(InstanceSpecification instanceSpec, String filename) throws ModelCenterException {
		// First, load the ModelCenter model
		ModelCenterPlugin.getModelCenterInstance().loadModel(filename);
		
		// Now check whether we can find the part of the ModelCenter model that we need (note that this is the
		// root node in most cases - but if we are updating only part of an instance, this method is better
		Assembly requiredRootAssembly = findStartingAssembly(ModelCenterPlugin.getModelCenterInstance().getModel(), instanceSpec);
		
		// If none is found, look the other way around (this can happen if a user selected to generate a
		// model of a part of the instance, but tries and synchronize by right clicking on the top most
		// instance part
		if(requiredRootAssembly == null) {
			requiredRootAssembly = ModelCenterPlugin.getModelCenterInstance().getModel();
			
			instanceSpec = findStartingInstanceSpec(instanceSpec, requiredRootAssembly);
		}
		
		if(instanceSpec != null && requiredRootAssembly != null) {
			// Now go traverse through the assembly and its subassemblies to find values
			traverseAssemblyAndUpdateInstanceValues(requiredRootAssembly, instanceSpec);
		}
		else {
			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Failed to import values: ModelCenter model and instance do not seem to match", "ModelCenter Plugin - Error Importing Values", JOptionPane.ERROR_MESSAGE);
		}
		
		// Finally, close the ModelCenter model again
		ModelCenterPlugin.getModelCenterInstance().closeModel();
	}
	
	/**
	 * 
	 * @param startingPoint
	 * @param instanceSpec
	 * @return
	 * @throws ModelCenterException 
	 */
	private Assembly findStartingAssembly(Assembly startingPoint, InstanceSpecification instanceSpec) throws ModelCenterException {
		String instanceName = instanceSpec.getName();
		
		// Check whether we found the starting point
		if(startingPoint.getName().equals(ModelCenterPlugin.toModelCenterSafeName(instanceName))) {
			// Found the correct assembly
			return startingPoint;
		}
		else {
			// Otherwise go through subassemblies and check whether those are equivalent to the instance
			for(int i=0; i<startingPoint.getNumAssemblies(); i++) {
				Assembly rootInstance = findStartingAssembly(startingPoint.getAssembly(i), instanceSpec);
				
				if(rootInstance != null) {
					// Assembly found
					return rootInstance;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param instanceSpec
	 * @param startingPoint
	 * @return
	 * @throws ModelCenterException
	 */
	private InstanceSpecification findStartingInstanceSpec(InstanceSpecification instanceSpec, Assembly startingPoint) throws ModelCenterException {
		String instanceName = instanceSpec.getName();
		
		// Check whether we found the starting point
		if(ModelCenterPlugin.toModelCenterSafeName(instanceName).equals(startingPoint.getName())) {
			// Found the correct assembly
			return instanceSpec;
		}
		else {
			// Otherwise go through slots of instance, find references to other instances and go through those
			// recursively
			for(Iterator<Slot> slotIter = instanceSpec.getSlot().iterator(); slotIter.hasNext(); ) {
				Slot nextSlot = slotIter.next();
				
				if(nextSlot.getDefiningFeature() != null && nextSlot.getDefiningFeature() instanceof Property && nextSlot.hasValue()) {
					for(Iterator<ValueSpecification> valueIter = nextSlot.getValue().iterator(); valueIter.hasNext(); ) {
						ValueSpecification nextValueSpec = valueIter.next();
						
						if(nextValueSpec instanceof InstanceValue) {
							InstanceSpecification rootPoint = findStartingInstanceSpec(((InstanceValue)nextValueSpec).getInstance(), startingPoint);
							
							if(rootPoint != null)
								return rootPoint;
						}
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param requiredRootAssembly
	 * @param instanceSpec
	 * @throws ModelCenterException 
	 */
	private void traverseAssemblyAndUpdateInstanceValues(Assembly currentAssembly, InstanceSpecification instanceSpec) throws ModelCenterException {
		// First go through the variables
		for(int i=0; i<currentAssembly.getNumVariables(); i++) {
			ArrayList<Variant> valuesToSet = extractValues(currentAssembly.getVariable(i));
			
			// Set the corresponding instance value
			findInstanceForVariableAndSetValues(currentAssembly.getVariable(i), instanceSpec, valuesToSet);
		}
		
		// Now go through the assemblies
		for(int i=0; i<currentAssembly.getNumAssemblies(); i++) {
			// Find the instance specification that resembles our assembly so that we can access the values
			InstanceSpecification subInstanceSpec = findSubInstanceSpecificationForAssembly(currentAssembly.getAssembly(i), instanceSpec);
			
			if(subInstanceSpec != null) {
				traverseAssemblyAndUpdateInstanceValues(currentAssembly.getAssembly(i), subInstanceSpec);
			}
			else {
				// Throw an out-of-sync error
			}
		}
	}

	/**
	 * 
	 * @param variable
	 * @param instanceSpec
	 * @param valuesToSet
	 * @throws ModelCenterException 
	 */
	private void findInstanceForVariableAndSetValues(Variable variable, InstanceSpecification instanceSpec, ArrayList<Variant> valuesToSet) throws ModelCenterException {
		// Go through slots and their values to find parts
		for(Iterator<Slot> slotIter = instanceSpec.getSlot().iterator(); slotIter.hasNext(); ) {
			Slot currentSlot = slotIter.next();
			
			// If we find a property to be the defining feature of the slot then ...
			if(currentSlot.getDefiningFeature() != null && currentSlot.getDefiningFeature() instanceof Property) {
				// Check whether it has value
				if(currentSlot.hasValue()) {
					// And finally go through the values and see whether we can find an InstanceValue
					for(Iterator<ValueSpecification> valueIter = currentSlot.getValue().iterator(); valueIter.hasNext(); ) {
						ValueSpecification nextValueSpec = valueIter.next();
						
						if(nextValueSpec instanceof LiteralSpecification) {
							// Check whether we found the correct assembly
							if(ModelCenterPlugin.toModelCenterSafeName(currentSlot.getDefiningFeature().getName()).equals(variable.getName())) {
								// Found the correct slot
								this.getInstanceHandler().fillSlotWithValues(currentSlot, valuesToSet);
								
								// Found the entry
								return;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param variable
	 * @return
	 * @throws ModelCenterException
	 */
	private ArrayList<Variant> extractValues(Variable variable) throws ModelCenterException {
		ArrayList<Variant> values = new ArrayList<Variant>();
		
		// Since these represent instance values, no matrices are possible - only vectors (i.e. 1 dimensional
		// arrays only)
		if(variable instanceof Array) {
			Array arrayValue = (Array)variable;
			String unSupportedDimensions = "";
			
			// Just to make sure, add a ,* for all dimensions above the first
			// TODO: Does the plugin work if this is the case?
			for(int j=1; j<arrayValue.getNumDimensions(); j++)
				unSupportedDimensions += ",*";
			
			// Now extract values for the first dimension
			for(int j=0; j<arrayValue.getLength(0); j++)
				values.add(ModelCenterPlugin.getModelCenterInstance().getValue(variable.getFullName() + "[" + j + unSupportedDimensions + "]"));
		}
		else {
			// Extract the single value
			values.add(ModelCenterPlugin.getModelCenterInstance().getValue(variable.getFullName()));
		}
		
		return values;
	}

	/**
	 * 
	 * @param assembly
	 * @param instanceSpec
	 * @return
	 * @throws ModelCenterException 
	 */
	private InstanceSpecification findSubInstanceSpecificationForAssembly(Assembly assembly, InstanceSpecification instanceSpec) throws ModelCenterException {
		if(instanceSpec == null || instanceSpec.getSlot() == null)
			return null;
		
		// Go through slots and their values to find parts
		for(Iterator<Slot> slotIter = instanceSpec.getSlot().iterator(); slotIter.hasNext(); ) {
			Slot currentSlot = slotIter.next();
			
			// If we find a property to be the defining feature of the slot then ...
			if(currentSlot.getDefiningFeature() != null && currentSlot.getDefiningFeature() instanceof Property) {
				// Check whether it has value
				if(currentSlot.hasValue()) {
					// And finally go through the values and see whether we can find an InstanceValue
					for(Iterator<ValueSpecification> valueIter = currentSlot.getValue().iterator(); valueIter.hasNext(); ) {
						ValueSpecification nextValueSpec = valueIter.next();
						
						if(nextValueSpec instanceof InstanceValue) {
							// Check whether we found the correct assembly
							if(ModelCenterPlugin.toModelCenterSafeName(((InstanceValue) nextValueSpec).getInstance().getName()).equals(assembly.getName())) {
								return ((InstanceValue) nextValueSpec).getInstance(); 
							}
						}
					}
				}
			}
		}
		
		return null;
	}

	/**
	 * @return the instanceHandler
	 */
	public InstanceHandler getInstanceHandler() {
		return instanceHandler_;
	}

	/**
	 * @param instanceHandler the instanceHandler to set
	 */
	public void setInstanceHandler(InstanceHandler instanceHandler) {
		this.instanceHandler_ = instanceHandler;
	}

}
