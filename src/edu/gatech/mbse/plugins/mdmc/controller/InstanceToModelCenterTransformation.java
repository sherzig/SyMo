/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.DataType;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Generalization;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceValue;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.MultiplicityElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Slot;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.StructuralFeature;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Type;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.ConnectableElement;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.Connector;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.ConnectorEnd;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;
import com.phoenix_int.ModelCenter.Array;
import com.phoenix_int.ModelCenter.Assembly;
import com.phoenix_int.ModelCenter.ModelCenter;
import com.phoenix_int.ModelCenter.ModelCenterException;
import com.phoenix_int.ModelCenter.Variable;
import com.phoenix_int.ModelCenter.Variant;

import edu.gatech.mbse.plugins.mdmc.exceptions.InstanceValuesNotDefinedException;
import edu.gatech.mbse.plugins.mdmc.model.ModelCenterModelInstance;

/**
 * @author Sebastian
 *
 */
public class InstanceToModelCenterTransformation {
	
	private ModelCenterFileManipulator modelCenterFileManipulator_ = null;
	private ConnectorHandler connectorHandler_ = null;
	private ArrayList<ModelCenterModelInstance> modelCenterModelInstances_ = null;
	private InstanceHandler instanceHandler_ = null;
	private String outputFilename_ = Application.getInstance().getProject().getDirectory() + "\\Full Model.pxc";

	/**
	 * 
	 */
	public InstanceToModelCenterTransformation() {
		this.setModelCenterFileManipulator(new ModelCenterFileManipulator());
		this.setConnectorHandler(new ConnectorHandler());
		this.setModelCenterModelInstances(new ArrayList<ModelCenterModelInstance>());
		this.setInstanceHandler(new InstanceHandler());
	}
	
	/**
	 * 
	 * @param instanceSpec
	 * @return
	 * @throws ModelCenterException
	 */
	public void createBaseModel(InstanceSpecification instanceSpec, Tree tree) throws ModelCenterException {
		// Initialize
		getConnectorHandler().setTree(tree);
		getConnectorHandler().rebuildConnectorsList();
		
		getModelCenterModelInstances().clear();
		
		ModelCenterPlugin.getModelCenterInstance().newModel();
		ModelCenterPlugin.getModelCenterInstance().getModel().rename(ModelCenterPlugin.toModelCenterSafeName(instanceSpec.getName()));
		
		// Save the model
		ModelCenterPlugin.getModelCenterInstance().saveModelAs(getOutputFilename());
		
		// Create the model from the SysML model information available
		createModelCenterModelFromInstanceRecursively(instanceSpec, ModelCenterPlugin.getModelCenterInstance().getModel(), false);
		
		// Save the model
		ModelCenterPlugin.getModelCenterInstance().saveModelAs(getOutputFilename());
		
		// And close it
		ModelCenterPlugin.getModelCenterInstance().closeModel();
	}
	
	/**
	 * 
	 * @param instanceSpec
	 * @throws ModelCenterException
	 */
	public void createLinks(InstanceSpecification instanceSpec) throws ModelCenterException {
		// Load the base model
		ModelCenterPlugin.getModelCenterInstance().loadModel(getOutputFilename());
		
		// Now create the links
		createLinksBetweenModelElements(instanceSpec, instanceSpec, ModelCenterPlugin.getModelCenterInstance().getModel());
		
		// Save the model again
		ModelCenterPlugin.getModelCenterInstance().saveModelAs(getOutputFilename());
		
		// And close it
		ModelCenterPlugin.getModelCenterInstance().closeModel();
	}
	
	/**
	 * 
	 * @param modelCenterUsage
	 * @param baseInstanceSpec The instance spec for which one of the classifiers contains a modelcenter model
	 * @throws ModelCenterException
	 */
	public void insertSpecificModelCenterUsage(Property modelCenterUsage, InstanceSpecification baseInstanceSpec, InstanceSpecification rootInstanceSpec) throws ModelCenterException {
		// Load the base model
		ModelCenterPlugin.getModelCenterInstance().loadModel(getOutputFilename());
		
		// Now create the links
		insertSingleModelCenterUsage(modelCenterUsage, baseInstanceSpec, rootInstanceSpec, ModelCenterPlugin.getModelCenterInstance().getModel());
		
		// Save the model again
		ModelCenterPlugin.getModelCenterInstance().saveModelAs(getOutputFilename());
		
		// And close it
		ModelCenterPlugin.getModelCenterInstance().closeModel();
	}
	
	/**
	 * 
	 * @param instanceSpec
	 * @return
	 * @throws ModelCenterException
	 */
	public void createModelCenterModelFromInstance(InstanceSpecification instanceSpec, Tree tree) throws ModelCenterException {
		// Initialize
		getConnectorHandler().setTree(tree);
		getConnectorHandler().rebuildConnectorsList();
		
		getModelCenterModelInstances().clear();
		
		ModelCenterPlugin.getModelCenterInstance().newModel();
		ModelCenterPlugin.getModelCenterInstance().getModel().rename(ModelCenterPlugin.toModelCenterSafeName(instanceSpec.getName()));
		
		// Create the model from the SysML model information available
		createModelCenterModelFromInstanceRecursively(instanceSpec, ModelCenterPlugin.getModelCenterInstance().getModel(), true);
		
		// Now create the links
		createLinksBetweenModelElements(instanceSpec, instanceSpec, ModelCenterPlugin.getModelCenterInstance().getModel());
		
		// TODO: Instantiate values for this method
		
		ModelCenterPlugin.getModelCenterInstance().saveModelAs(getOutputFilename());
	}
	
	/**
	 * Find generalizing elements by going through the generalization hierarchy of a given
	 * classifier
	 * 
	 * @param classifier
	 * @return
	 */
	private List<Classifier> findGeneralizations(List<Classifier> classifiers) {
		// Create a new list and add the elements from the current list of classifiers to it
		ArrayList<Classifier> allClassifiers = new ArrayList<Classifier>();
		
		// Go through basic classifiers
		for(Iterator<Classifier> iter=classifiers.iterator(); iter.hasNext(); ) {
			Classifier nextClassifier = iter.next();
			
			allClassifiers.add(nextClassifier);
			
			// Add all generalization elements
			allClassifiers.addAll(extractGeneralizationElements(nextClassifier));
		}
		
		return allClassifiers;
	}
	
	/**
	 * Go through hierachy of generalizations for a single element
	 * 
	 * @param classifier
	 * @return
	 */
	private List<Classifier> extractGeneralizationElements(Classifier classifier) {
		ArrayList<Classifier> allClassifiers = new ArrayList<Classifier>();
		
		if(classifier.getGeneralization() != null) {
			// Go through generalization relationship
			for(Iterator<Generalization> genIter=classifier.getGeneralization().iterator(); genIter.hasNext(); ) {
				Generalization nextGeneralization = genIter.next();
				
				// Get the target generalizations
				for(Iterator<Element> elIter=nextGeneralization.getTarget().iterator(); elIter.hasNext(); ) {
					Classifier nextClassifier = (Classifier)elIter.next();
					
					// Add the found target
					allClassifiers.add(nextClassifier);
					
					// Check for more generalizations
					allClassifiers.addAll(extractGeneralizationElements(nextClassifier));
				}
			}
		}
			
		return allClassifiers;
	}
	
	/**
	 * 
	 * @param instanceSpec
	 * @param curAssembly
	 * @return
	 * @throws ModelCenterException 
	 */
	private Assembly createModelCenterModelFromInstanceRecursively(InstanceSpecification instanceSpec, Assembly curAssembly, boolean includeModelCenterModelUsages) throws ModelCenterException {
		// Go through element and see whether the sub-elements / properties are ModelCenter models
		List<Classifier> classifiers = findGeneralizations(instanceSpec.getClassifier());
			
		// Go through all classifiers of the instance specification
		for(int j=0; j<classifiers.size(); j++) {
			Classifier classifier = classifiers.get(j);
				
			// Search recursively to look for elements
			if(classifier instanceof Element) {
				Element toParse = (Element)classifier;
				
				// Now go through the owned elements of the class that we are looking at
				for(Iterator<Element> elementIterator = toParse.getOwnedElement().iterator(); elementIterator.hasNext(); ) {
					Element nextElement = elementIterator.next();
					
					// If the sub-element is a property, check whether it is a modelcenter model
					if(nextElement instanceof Property) {
						Property currentProperty = (Property)nextElement;
						
						if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(currentProperty.getType())) {
							// Data models will be imported as assemblies
							System.out.println("Found a modelcenter model / assembly: " + currentProperty.getName());
							
							if(includeModelCenterModelUsages) {
								// Add it to the current assembly
								curAssembly = getModelCenterFileManipulator().insertRootAssembly(currentProperty, curAssembly);
								
								// Add it to the internal list so that we can extract the "full names" faster later on
								ModelCenterModelInstance newMCInstance = new ModelCenterModelInstance(currentProperty, instanceSpec, false);
								newMCInstance.setFullNameInFinalModelCenterFile(curAssembly.getFullName() + "." + getModelCenterFileManipulator().getModelCenterModelRootNodeName());
								getModelCenterModelInstances().add(newMCInstance);
							}
							else {
								System.out.println("... however, it was not yet added to the file");
							}
						}
					}
				}
			}
		}
		
		for(Iterator<Slot> iter = instanceSpec.getSlot().iterator(); iter.hasNext(); ) {
			Slot nextSlot = iter.next();
			Variable varToSet = null;
			
			if(nextSlot.getDefiningFeature() != null && StereotypesHelper.hasStereotype(nextSlot.getDefiningFeature(), "ValueProperty")) {
				// Data types (e.g. value properties) will become variables
				System.out.println("Found a datatype / variable: " + nextSlot.getDefiningFeature().getName());
				
				// Get the multiplicity
				String multiplicity = ModelHelper.getMultiplicity((MultiplicityElement)nextSlot.getDefiningFeature());
				
				// Interpret the multiplicity and set values accordingly
	    		if(multiplicity == null || multiplicity.equals("") || multiplicity.equals("1") || multiplicity.equals("0")) {
	    			varToSet = curAssembly.addVariable(ModelCenterPlugin.toModelCenterSafeName(nextSlot.getDefiningFeature().getName()), toModelCenterType(((Property)nextSlot.getDefiningFeature()).getType()));
	    		}
	    		else {
	    			varToSet = curAssembly.addVariable(ModelCenterPlugin.toModelCenterSafeName(nextSlot.getDefiningFeature().getName()), toModelCenterType(((Property)nextSlot.getDefiningFeature()).getType()) + "[]");
	    		}
			}
			
			// Find value, which could also be a reference to an instance
			if(nextSlot.hasValue()) {
				if(varToSet != null) {
					List<ValueSpecification> value = nextSlot.getValue();
					
					// Add the values for the variable created earlier
					ArrayList<Variant> values = new ArrayList<Variant>();
					
					values.addAll(getInstanceHandler().extractValuesFromSlotValues(nextSlot.getDefiningFeature(), value));
					
					System.out.println("Setting for " + nextSlot.getDefiningFeature().getName());
					
					// The below is copied from SolveAction - perhaps find a better way to handle this
					if(values == null || values.size() <= 0) {
						// This case can happen when there are specific parts of the object that are not
						// instantiated, e.g. a specific value is not instantiated
						System.out.println("Some inputs have not been defined properly!!!");
					}
					else {
						// Set the value inside the ModelCenter model
						if(values.size() > 1) {
							// TODO: Need to set size to zero before every run
							//((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).setSize(curSize + values.size());
							
							long curSize = ((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).getSize();
							((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).setSize(curSize + values.size());
							
							for(int x=0; x<values.size(); x++) {
								if(values.get(x).getType() == Variant.BOOLEAN_ARRAY || values.get(x).getType() == Variant.DOUBLE_ARRAY || values.get(x).getType() == Variant.INT_ARRAY || values.get(x).getType() == Variant.LONG_ARRAY || values.get(x).getType() == Variant.STRING_ARRAY) {
									int secondDimensionLength = 0;
									
									if(values.get(x).getType() == Variant.BOOLEAN_ARRAY)
										secondDimensionLength = values.get(x).booleanArrayValue().length;
									else if(values.get(x).getType() == Variant.DOUBLE_ARRAY)
										secondDimensionLength = values.get(x).doubleArrayValue().length;
									else if(values.get(x).getType() == Variant.INT_ARRAY)
										secondDimensionLength = values.get(x).intArrayValue().length;
									else if(values.get(x).getType() == Variant.LONG_ARRAY)
										secondDimensionLength = values.get(x).longArrayValue().length;
									else if(values.get(x).getType() == Variant.STRING_ARRAY)
										secondDimensionLength = values.get(x).stringArrayValue().length;
									
									// Set the number of dimensions required
									if(((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).getNumDimensions() < 2) {
										((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).setNumDimensions(2);
									}
									
									curSize = ((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).getSize(0);
									// TODO: Fix the next line again when you have time
									((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).setSize(curSize + values.size(), 0);
									((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).setSize(secondDimensionLength, 1);
									
									// Now set the values for this variant
									for(int k=0; x<secondDimensionLength; k++) {
										if(values.get(x).getType() == Variant.BOOLEAN_ARRAY)
											ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[" + (x + curSize) + "," + k + "]", Boolean.toString(values.get(x).booleanArrayValue()[k]));
										else if(values.get(x).getType() == Variant.DOUBLE_ARRAY)
											ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[" + (x + curSize) + "," + k + "]", Double.toString(values.get(x).doubleArrayValue()[k]));
										else if(values.get(x).getType() == Variant.INT_ARRAY)
											ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[" + (x + curSize) + "," + k + "]", Integer.toString(values.get(x).intArrayValue()[k]));
										else if(values.get(x).getType() == Variant.LONG_ARRAY)
											ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[" + (x + curSize) + "," + k + "]", Long.toString(values.get(x).longArrayValue()[k]));
										else if(values.get(x).getType() == Variant.STRING_ARRAY)
											ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[" + (x + curSize) + "," + k + "]", values.get(x).stringArrayValue()[k]);
									}
								}
								else {
									if(values.get(x).getType() == Variant.BOOLEAN)
										ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[" + (x + curSize) + "]", Boolean.toString(values.get(x).booleanValue()));
									else if(values.get(x).getType() == Variant.DOUBLE)
										ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[" + (x + curSize) + "]", Double.toString(values.get(x).doubleValue()));
									else if(values.get(x).getType() == Variant.INT)
										ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[" + (x + curSize) + "]", Integer.toString(values.get(x).intValue()));
									else if(values.get(x).getType() == Variant.LONG)
										ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[" + (x + curSize) + "]", Long.toString(values.get(x).longValue()));
									else if(values.get(x).getType() == Variant.STRING)
										ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[" + (x + curSize) + "]", values.get(x).stringValue());
								}
							}
						}
						else {
							if(values.get(0).getType() == Variant.BOOLEAN_ARRAY || values.get(0).getType() == Variant.DOUBLE_ARRAY || values.get(0).getType() == Variant.INT_ARRAY || values.get(0).getType() == Variant.LONG_ARRAY || values.get(0).getType() == Variant.STRING_ARRAY) {
								int length = 0;
								
								if(values.get(0).getType() == Variant.BOOLEAN_ARRAY)
									length = values.get(0).booleanArrayValue().length;
								else if(values.get(0).getType() == Variant.DOUBLE_ARRAY)
									length = values.get(0).doubleArrayValue().length;
								else if(values.get(0).getType() == Variant.INT_ARRAY)
									length = values.get(0).intArrayValue().length;
								else if(values.get(0).getType() == Variant.LONG_ARRAY)
									length = values.get(0).longArrayValue().length;
								else if(values.get(0).getType() == Variant.STRING_ARRAY)
									length = values.get(0).stringArrayValue().length;
								
								int oldSize = 0;
								
								if(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()) instanceof Array) {
									// Set the number of dimensions required
									if(((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).getNumDimensions() < 2) {
										((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).setNumDimensions(2);
										
										// Set size for dimension 0
										((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).setSize(length, 0);
										
										// Set size for dimension 1
										((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).setSize(1, 1);
									}
									else {
										oldSize = (int)((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).getSize(1);
										((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).setSize(1+oldSize, 1);
									}
								}
								
								for(int x=0; x<length; x++) {
									String addition = "[" + x + "," + oldSize + "]";
											
									if(!(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()) instanceof Array))
										addition = "";
									
									// Set the vector length - in this case 1 
									if(values.get(0).getType() == Variant.BOOLEAN_ARRAY)
										ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + addition, Boolean.toString(values.get(0).booleanArrayValue()[x]));
									else if(values.get(0).getType() == Variant.DOUBLE_ARRAY)
										ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + addition, Double.toString(values.get(0).doubleArrayValue()[x]));
									else if(values.get(0).getType() == Variant.INT_ARRAY)
										ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + addition, Integer.toString(values.get(0).intArrayValue()[x]));
									else if(values.get(0).getType() == Variant.LONG_ARRAY)
										ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + addition, Long.toString(values.get(0).longArrayValue()[x]));
									else if(values.get(0).getType() == Variant.STRING_ARRAY)
										ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + addition, values.get(0).stringArrayValue()[x]);
								}
							}
							else {
								if(values.get(0).getType() == Variant.BOOLEAN)
									ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName(), Boolean.toString(values.get(0).booleanValue()));
								else if(values.get(0).getType() == Variant.DOUBLE)
									ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName(), Double.toString(values.get(0).doubleValue()));
								else if(values.get(0).getType() == Variant.INT)
									ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName(), Integer.toString(values.get(0).intValue()));
								else if(values.get(0).getType() == Variant.LONG)
									ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName(), Long.toString(values.get(0).longValue()));
								else if(values.get(0).getType() == Variant.STRING)
									ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName(), values.get(0).stringValue());
							}
						}
					}
				}
				else {
					for(Iterator<ValueSpecification> valueIter = nextSlot.getValue().iterator(); valueIter.hasNext(); ) {
						ValueSpecification nextValueSpec = valueIter.next();
						
						if(nextValueSpec instanceof InstanceValue) {
							String newAssemblyName = ((InstanceValue)nextValueSpec).getInstance().getName();
							
							// ModelCenter only allows characters A-Z, a-z and 0-9
							// Hence remove all those characters
							newAssemblyName = ModelCenterPlugin.toModelCenterSafeName(newAssemblyName);
							
							createModelCenterModelFromInstanceRecursively(((InstanceValue)nextValueSpec).getInstance(), curAssembly.addAssembly(newAssemblyName), includeModelCenterModelUsages);
						}
					}
				}
			}
		}
		
		return curAssembly;
	}
	
	/**
	 * 
	 * @param instanceSpec
	 * @param curAssembly
	 * @param includeModelCenterModelUsages
	 * @return
	 * @throws ModelCenterException
	 */
	private boolean insertSingleModelCenterUsage(Property modelCenterUsage, InstanceSpecification baseInstanceSpec, InstanceSpecification rootInstanceSpec, Assembly curAssembly) throws ModelCenterException {
		// Go through element and see whether the sub-elements / properties are ModelCenter models
		List<Classifier> classifiers = findGeneralizations(rootInstanceSpec.getClassifier());
			
		// Go through all classifiers of the instance specification
		for(int j=0; j<classifiers.size(); j++) {
			Classifier classifier = classifiers.get(j);
				
			// Search recursively to look for elements
			if(classifier instanceof Element) {
				Element toParse = (Element)classifier;
				
				// Now go through the owned elements of the class that we are looking at
				for(Iterator<Element> elementIterator = toParse.getOwnedElement().iterator(); elementIterator.hasNext(); ) {
					Element nextElement = elementIterator.next();
					
					// If the sub-element is a property, check whether it is a modelcenter model
					if(nextElement instanceof Property) {
						Property currentProperty = (Property)nextElement;
						
						if(currentProperty == modelCenterUsage && rootInstanceSpec == baseInstanceSpec && ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(currentProperty.getType())) {
							// Data models will be imported as assemblies
							System.out.println("Inserting model after it was solved: " + currentProperty.getType().getName() + " at " + curAssembly.getFullName());
							
							// Add it to the current assembly
							curAssembly = getModelCenterFileManipulator().insertRootAssembly(currentProperty, curAssembly);
							
							// Add it to the internal list so that we can extract the "full names" faster later on
							ModelCenterModelInstance newMCInstance = new ModelCenterModelInstance(currentProperty, baseInstanceSpec, false);
							newMCInstance.setFullNameInFinalModelCenterFile(curAssembly.getFullName() + "." + getModelCenterFileManipulator().getModelCenterModelRootNodeName());
							getModelCenterModelInstances().add(newMCInstance);
							
							// Done, exit function
							return true;
						}
					}
				}
			}
		}
		
		for(Iterator<Slot> iter = rootInstanceSpec.getSlot().iterator(); iter.hasNext(); ) {
			Slot nextSlot = iter.next();
			
			// Find value, which could also be a reference to an instance
			if(nextSlot.hasValue()) {
				for(Iterator<ValueSpecification> valueIter = nextSlot.getValue().iterator(); valueIter.hasNext(); ) {
					ValueSpecification nextValueSpec = valueIter.next();
					
					if(nextValueSpec instanceof InstanceValue) {
						String newAssemblyName = ((InstanceValue)nextValueSpec).getInstance().getName();
						
						// ModelCenter only allows characters A-Z, a-z and 0-9
						// Hence remove all those characters
						newAssemblyName = ModelCenterPlugin.toModelCenterSafeName(newAssemblyName);
						
						if(curAssembly.getNumAssemblies() <= 0 || curAssembly.getAssembly(newAssemblyName) == null) {
							curAssembly.addAssembly(newAssemblyName);
						}
						
						// Check whether it was inserted
						boolean retVal = insertSingleModelCenterUsage(modelCenterUsage, baseInstanceSpec, ((InstanceValue)nextValueSpec).getInstance(), curAssembly.getAssembly(newAssemblyName));
						
						// If yes, exit function, else continue search
						if(retVal == true)
							return true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Creates the links between model elements inside the ModelCenter file based on the information given
	 * in the SysML model
	 * 
	 * @param instanceSpec
	 * @throws ModelCenterException 
	 */
	private void createLinksBetweenModelElements(InstanceSpecification rootInstanceSpec, InstanceSpecification instanceSpec, Assembly currentAssembly) throws ModelCenterException {
		// Go through element and see whether the sub-elements / properties are ModelCenter models
		List<Classifier> classifiers = findGeneralizations(instanceSpec.getClassifier());
		
		// Find model center model usages in the classifiers of this instance specification and create links
		findModelCenterModelsInClassifiersAndCreateLinks(classifiers, currentAssembly, rootInstanceSpec, instanceSpec);
		
		// Iterate through instance
		for(Iterator<Slot> iter = instanceSpec.getSlot().iterator(); iter.hasNext(); ) {
			Slot nextSlot = iter.next();
			
			// Find value, which could also be a reference to an instance
			if(nextSlot.hasValue()) {
				for(Iterator<ValueSpecification> valueIter = nextSlot.getValue().iterator(); valueIter.hasNext(); ) {
					ValueSpecification nextValueSpec = valueIter.next();
					
					// Check to see whether we found an instance value
					if(nextValueSpec instanceof InstanceValue) {
						String newAssemblyName = ((InstanceValue)nextValueSpec).getInstance().getName();
						
						// ModelCenter only allows characters A-Z, a-z and 0-9
						// Hence remove all those characters
						newAssemblyName = ModelCenterPlugin.toModelCenterSafeName(newAssemblyName);
						
						createLinksBetweenModelElements(rootInstanceSpec, ((InstanceValue)nextValueSpec).getInstance(), currentAssembly.getAssembly(newAssemblyName));
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param classifiers
	 * @throws ModelCenterException 
	 */
	private void findModelCenterModelsInClassifiersAndCreateLinks(List<Classifier> classifiers, Assembly currentAssembly, InstanceSpecification rootInstanceSpec, InstanceSpecification instanceSpec) throws ModelCenterException {
		// Go through all classifiers of the instance specification
		for(int j=0; j<classifiers.size(); j++) {
			Classifier classifier = classifiers.get(j);
				
			// Search recursively to look for elements
			if(classifier instanceof Element) {
				Element toParse = (Element)classifier;
				
				findModelCenterModelsInElementAndCreateLinks(toParse, currentAssembly, rootInstanceSpec, instanceSpec);
			}
		}
	}
	
	/**
	 * 
	 * @param toParse
	 * @param currentAssembly
	 * @param rootInstanceSpec
	 * @param instanceSpec
	 * @throws ModelCenterException 
	 */
	private void findModelCenterModelsInElementAndCreateLinks(Element toParse, Assembly currentAssembly, InstanceSpecification rootInstanceSpec, InstanceSpecification instanceSpec) throws ModelCenterException {
		// Now go through the owned elements of the class that we are looking at
		for(Iterator<Element> elementIterator = toParse.getOwnedElement().iterator(); elementIterator.hasNext(); ) {
			Element nextElement = elementIterator.next();
			
			// If the sub-element is a property, check whether it is a modelcenter model
			if(nextElement instanceof Property) {
				Property currentProperty = (Property)nextElement;
				
				if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(currentProperty.getType())) {
					// Establish links for this ModelCenter model usage
					createLinksForModel(currentProperty, currentAssembly, rootInstanceSpec, instanceSpec);
					
					// Check for possible sub models
					// TODO: This is not yet supported
					/*
					String subAssemblyName = ModelCenterPlugin.toModelCenterSafeName(currentProperty.getName());
					
					if(subAssemblyName.equals("")) {
						subAssemblyName = ModelCenterPlugin.toModelCenterSafeName(currentProperty.getType().getName());
					}
					
					findModelCenterModelsInElementAndCreateLinks(currentProperty.getType(), currentAssembly.getAssembly(subAssemblyName), rootInstanceSpec, instanceSpec);
					*/
				}
			}
		}
	}

	/**
	 * 
	 * @param property
	 * @return
	 */
	private ArrayList<String> findInputVariableLocations(Element e, Property modelCenterModelUsage, InstanceSpecification currentInstance, InstanceSpecification instanceSpec, String currentPath) {
		ArrayList<String> locations = new ArrayList<String>();
		
		currentPath += ModelCenterPlugin.toModelCenterSafeName(currentInstance.getName()) + ".";
		
		System.out.println("Element qualified name: " + ((NamedElement)e).getQualifiedName() + " (at path: " + currentPath + ")");
		
		if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterOutputVariable(e) && modelCenterModelUsage != null) {
			// Go through classifiers to see whether we have any ModelCenter models that this elements may
			// connect to (i.e. this is important if one modelcenter model connectors to another)
			List<Classifier> classifiers = findGeneralizations(currentInstance.getClassifier());
			
			for(Iterator<Classifier> classIter = classifiers.iterator(); classIter.hasNext(); ) {
				Classifier nextClassifier = classIter.next();
				
				// Search recursively to look for elements
				if(nextClassifier instanceof Element) {
					Element toParse = (Element)nextClassifier;
					
					// Now go through the owned elements of the class that we are looking at
					for(Iterator<Element> elementIterator = toParse.getOwnedElement().iterator(); elementIterator.hasNext(); ) {
						Element nextElement = elementIterator.next();
					
						// If the sub-element is a property, check whether it is a modelcenter model
						if(nextElement instanceof Property) {
							if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(((Property)nextElement).getType())) {
								if((Property)nextElement == modelCenterModelUsage) {
									String nameOfAssembly = modelCenterModelUsage.getName();
									
									if(nameOfAssembly.equals(""))
										nameOfAssembly = modelCenterModelUsage.getType().getName();
									
									String pathAdd = ModelCenterPlugin.toModelCenterSafeName(nameOfAssembly) + ".";
									
									// TODO: If its nested deeper, this wont work anymore! Include a loop
									locations.add(currentPath + pathAdd + ((NamedElement)e).getName());
									System.out.println("-> Added to MC list of elements that this connects to: " + currentPath + pathAdd + ((NamedElement)e).getName());
								}
							}
						}
					}
				}
			}
		}
		
		// Travers through the slots
		for(Iterator<Slot> slotIter = currentInstance.getSlot().iterator(); slotIter.hasNext(); ) {
			// Slots can contain:
			// 1) Instances of value properties (usually a literal string)
			// 2) Instance values as reference to other instance specifications
			// TODO: Array of literal strings
			Slot nextSlot = slotIter.next();
			
			// Get the next slot value for this defining feature - this supports composition
        	// and aggregation!
			System.out.println("Defining feature: " + nextSlot.getDefiningFeature().getQualifiedName());
        	if(nextSlot.getDefiningFeature() == e || nextSlot.getDefiningFeature().getRedefinedElement().contains(e)) {
        		// TODO: Get all values if there are multiple values attached to this element
        		//locations.add(currentPath + ((NamedElement)e).getName());
        		// Changed to defining feature name
        		locations.add(currentPath + nextSlot.getDefiningFeature().getName());
        		System.out.println("-> Added to list of elements that this connects to: " + currentPath + ((NamedElement)e).getName());
        	}
        	else {
				List<ValueSpecification> value = nextSlot.getValue();
				
				// Check whether we are on the instance level in which the modelcenter model usage
				// is that we are considering
				boolean inCriticalLevel = false;
				
				for(int i=0; i<value.size(); i++) {
					ValueSpecification valueSpecification = value.get(i);
			        
			        // Check whether a value was found or a part
			        if(valueSpecification instanceof InstanceValue) {
			        	InstanceValue instanceVal = (InstanceValue)valueSpecification;
			        	
			        	if(instanceVal.getInstance() == instanceSpec) {
				        	// Break the loop if we are on the same "level" - imagine several subsystems, each has a modelcenter
				        	// model usage. In order to not account for all connections of all modelcenter usages on the same
				        	// level, we need to make sure we follow a path up that is reachable by the single usage
				        	// and not by all of them (i.e. for every one in the subsystems)
			        		locations.addAll(findInputVariableLocations(e, modelCenterModelUsage, instanceVal.getInstance(), instanceSpec, currentPath));
			        		
			        		inCriticalLevel = true;
			        		
			        		break;
			        	}
			        }
				}
				
				if(inCriticalLevel == false) {
					// Otheriwse just iterate through values and see whether we can find property values
					for(int i=0; i<value.size(); i++) {
				        ValueSpecification valueSpecification = value.get(i);
				        
				        // Check whether a value was found or a part
				        if(valueSpecification instanceof InstanceValue) {
				        	// Subsystem that can contain more elements
				        	InstanceValue instanceVal = (InstanceValue)valueSpecification;
				        	
				        	locations.addAll(findInputVariableLocations(e, modelCenterModelUsage, instanceVal.getInstance(), instanceSpec, currentPath));
				        }
				    }
				}
        	}
		}
		
		return locations;
	}

	/**
	 * Create links between the variables of a specific model center usage that is part of a specific instance
	 * specification
	 * 
	 * @param modelCenterModelUsage
	 * @param currentAssembly
	 * @param rootInstanceSpec
	 * @param instanceSpec
	 */
	private void createLinksForModel(Property modelCenterModelUsage, Assembly currentAssembly, InstanceSpecification rootInstanceSpec, InstanceSpecification instanceSpec) {
		// First, go through all elements that the model center model representation owns
		for(Iterator<Element> elementIter = modelCenterModelUsage.getType().getOwnedElement().iterator(); elementIter.hasNext(); ) {
			Element nextElement = elementIter.next();
			
			// Check whether the current element that we are considering is a port and is a variable of the
			// modelcenter model
			if(nextElement instanceof Port && ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterVariable(nextElement)) {
				// If it is, get all connectors that have this modelcenter variable at one of its ends
				ArrayList<Connector> connectors = getConnectorHandler().getConnectorsForElementAndPartWithPort(nextElement, modelCenterModelUsage);
				
				// Go through the connectors one by one
				for(Iterator<Connector> iter = connectors.iterator(); iter.hasNext(); ) {
					Connector nextConnector = iter.next();
					
					List <ConnectorEnd> list = nextConnector.getEnd();
					
					// Grab the first two ends
					ConnectorEnd modelCenterConnectorEnd = list.get(0);
					ConnectorEnd oppositeConnectorEnd = list.get(1);
					
					// Find out which one of the ends is the opposite side of the modelcenter variable
					if(oppositeConnectorEnd.getPartWithPort() == modelCenterModelUsage) {
						// Looks like we need to switch ends
						modelCenterConnectorEnd = list.get(1);
						oppositeConnectorEnd = list.get(0);
					}
					
					ConnectableElement oppositeSide = oppositeConnectorEnd.getRole();
					
					System.out.println("Looking at connector from usage: " + modelCenterModelUsage.getName() + " in instance: " + instanceSpec.getName() + " - Connector: " + modelCenterConnectorEnd.getPartWithPort().getName() + "::" + modelCenterConnectorEnd.getRole().getName() + " connected to " + oppositeConnectorEnd.getRole().getName());
					
					// TODO: If both ends are modelcenter models, then only consider if the current port that
					// we are looking at is an input - otherwise consider input AND output
					if(isPartOfCurrentInstance(oppositeSide, rootInstanceSpec) || ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterOutputVariable(oppositeSide)) {
						System.out.println(instanceSpec.getName() + " : " + modelCenterModelUsage.getType().getName() + "(" + modelCenterModelUsage.getName() + ")." + ((NamedElement)nextElement).getName());
						ArrayList<String> connectedElements = findInputVariableLocations(oppositeSide, oppositeConnectorEnd.getPartWithPort(), rootInstanceSpec, instanceSpec, "");
						String modelCenterFullName = getFullNameInMCFileForGivenModelCenterModelUsage(modelCenterModelUsage, nextElement, instanceSpec);
						
						if(connectedElements != null && connectedElements.size() > 0) {
							try {
								Variable modelCenterVariable = ModelCenterPlugin.getModelCenterInstance().getVariable(modelCenterFullName);
								
								// Check type
								if(modelCenterVariable.getType().contains("[]")) {
									// ModelCenter variable is an n-dimensional array
									String addition = "";
									
									// TODO: Multi values
									for(int i=0; i<connectedElements.size(); i++) {
										addition = "[" + i + "]";
									
										// Value property values are vectors if and only if they have multiple values to offer!
										Variable valuePropertyVariable = ModelCenterPlugin.getModelCenterInstance().getVariable(connectedElements.get(i));
										
										if(valuePropertyVariable.getType().contains("[]")) {
											int curDimSize = (int)((Array)modelCenterVariable).getSize();
											
											if(((Array)valuePropertyVariable).getNumDimensions() < 2) {
												((Array)valuePropertyVariable).setNumDimensions(2);
											}
											
											if(((Array)valuePropertyVariable).getSize(0) < curDimSize) {
												((Array)valuePropertyVariable).setSize(curDimSize, 0);
											}
											
											// Column vectors for multi-values of variables
											if(((Array)valuePropertyVariable).getSize(1) < i) {
												((Array)valuePropertyVariable).setSize(i, 1);
											}
											
											for(int j=0; j<curDimSize; j++) {
												String varAddition = "[" + j + "," + i + "]";
												addition = "[" + j + "]";
												
												try {
													if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterInputVariable(nextElement)) {
														// Is an input, connect this input to our variable
														ModelCenterPlugin.getModelCenterInstance().createLink(modelCenterVariable.getFullName() + addition, valuePropertyVariable.getFullName() + varAddition);
													}
													else {
														ModelCenterPlugin.getModelCenterInstance().createLink(valuePropertyVariable.getFullName() + varAddition, modelCenterVariable.getFullName() + addition);
													}
												}
												catch(ModelCenterException e) {
													// ModelCenter variable does not exist or is already linked - ignore, maybe alert user in some
													// cases
													e.printStackTrace();
												}
											}
										}
										else {
											try {
												if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterInputVariable(nextElement)) {
													// Is an input, connect this input to our variable
													ModelCenterPlugin.getModelCenterInstance().createLink(modelCenterVariable.getFullName() + addition, valuePropertyVariable.getFullName());
												}
												else {
													ModelCenterPlugin.getModelCenterInstance().createLink(valuePropertyVariable.getFullName(), modelCenterVariable.getFullName() + addition);
												}
											}
											catch(ModelCenterException e) {
												// ModelCenter variable does not exist or is already linked - ignore, maybe alert user in some
												// cases
												e.printStackTrace();
											}
										}
									}
								}
								else {
									// Not an array - make sure that also connectors list is not an array, otherwise
									// Hook up element 0
									String addition = "";
									
									if(connectedElements.size() > 1) {
										addition = "[0]";
									}
									
									if(connectedElements.size() > 0) {
										Variable valuePropertyVariable = ModelCenterPlugin.getModelCenterInstance().getVariable(connectedElements.get(0));
										
										// Check to see whether the value property we are setting has a multiplicity
										if(valuePropertyVariable.getType().contains("[]") && !modelCenterVariable.getType().contains("[]")) {
											try {
												// TODO: this is experimental and potentially unsafe
												int numLinks = valuePropertyVariable.precedentLinks().getCount() + valuePropertyVariable.dependentLinks().getCount();
												
												// TODO: Right now, any size vectors / matrices are allowed - change this perhaps
												((Array)valuePropertyVariable).setSize(numLinks+1, 0);
												
												addition = "[" + numLinks + ",0]";
												
												if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterInputVariable(nextElement)) {
													// Is an input, connect this input to our variable
													ModelCenterPlugin.getModelCenterInstance().createLink(modelCenterVariable.getFullName(), valuePropertyVariable.getFullName() + addition);
												}
												else {
													ModelCenterPlugin.getModelCenterInstance().createLink(valuePropertyVariable.getFullName() + addition, modelCenterVariable.getFullName());
												}
											}
											catch(ModelCenterException e) {
												// ModelCenter variable does not exist or is already linked - ignore, maybe alert user in some
												// cases
												e.printStackTrace();
											}
										}
										else {
											try {
												if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterInputVariable(nextElement)) {
													// Is an input, connect this input to our variable
													ModelCenterPlugin.getModelCenterInstance().createLink(modelCenterVariable.getFullName() + addition, valuePropertyVariable.getFullName());
												}
												else {
													ModelCenterPlugin.getModelCenterInstance().createLink(valuePropertyVariable.getFullName(), modelCenterVariable.getFullName() + addition);
												}
											}
											catch(ModelCenterException e) {
												// ModelCenter variable does not exist or is already linked - ignore, maybe alert user in some
												// cases
												e.printStackTrace();
											}
										}
									}
								}
							}
							catch(ModelCenterException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param con
	 * @return
	 */
	private Property getModelCenterModelPartFromConnector(Connector con) {
		List <ConnectorEnd> list = con.getEnd();
		
		for(Iterator<ConnectorEnd> iter = list.iterator(); iter.hasNext(); ) {
			ConnectorEnd conEnd = iter.next();
			
			// Check whether currently looked at connector end is a modelcenter model
			if(conEnd.getPartWithPort() != null && ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(conEnd.getPartWithPort().getType())) {
				return conEnd.getPartWithPort();
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param con
	 * @return
	 */
	private Element getModelCenterModelVariableFromConnector(Connector con) {
		List <ConnectorEnd> list = con.getEnd();
		
		for(Iterator<ConnectorEnd> iter = list.iterator(); iter.hasNext(); ) {
			ConnectorEnd conEnd = iter.next();
			
			// Check whether currently looked at connector end is a modelcenter model
			if(conEnd.getRole() != null && ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterVariable(conEnd.getRole())) {
				return conEnd.getRole();
			}
		}
		
		return null;
	}
	
	/**
	 * Checks whether a certain element / classifier of an instance specification exists in the instance
	 * considered
	 * 
	 * @param element
	 * @param rootInstanceSpec
	 * @return
	 */
	private boolean isPartOfCurrentInstance(Element element, InstanceSpecification rootInstanceSpec) {
		for(Iterator<Slot> iter = rootInstanceSpec.getSlot().iterator(); iter.hasNext(); ) {
			Slot nextSlot = iter.next();
			
			// See whether we found the element
			if(nextSlot.getDefiningFeature() == element || nextSlot.getDefiningFeature().getRedefinedElement().contains(element))
				return true;

			// Find value, which could also be a reference to an instance
			if(nextSlot.hasValue()) {
				for(Iterator<ValueSpecification> valueIter = nextSlot.getValue().iterator(); valueIter.hasNext(); ) {
					ValueSpecification nextValueSpec = valueIter.next();
					
					if(nextValueSpec instanceof InstanceValue) {
						boolean found = isPartOfCurrentInstance(element, ((InstanceValue)nextValueSpec).getInstance());
						
						if(found == true)
							return true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param element
	 * @param rootInstanceSpec
	 * @return
	 */
	private String getFullNameInMCFileForGivenModelCenterModelUsage(Property modelCenterModel, Element subElement, InstanceSpecification currentParentInstance) {
		for(Iterator<ModelCenterModelInstance> iter = getModelCenterModelInstances().iterator(); iter.hasNext(); ) {
			ModelCenterModelInstance inst = iter.next();
			
			// Find the full name for the requested element of a modelcenter model
			if(inst.getCorrespondingProperty() == modelCenterModel) {
				if(inst.getCorrespondingInstanceSpecification() == currentParentInstance) {
					return inst.getFullNameInFinalModelCenterFile() + "." + ((NamedElement)subElement).getName();
				}
			}
		}
		
		// Return null if the elemtn could not be found
		return null;
	}

	/**
	 * 
	 * @param property
	 * @return
	 */
	private String toModelCenterType(Type type) {
		String mcType = "double";
		
		if(type != null) {
			String typeName = type.getName().toLowerCase();
		
			if(typeName.contains("string") || typeName.contains("char"))
				return "string";
			else if(typeName.contains("int") || typeName.contains("integer") || typeName.equals("long"))	// Covers int32, etc.
				return "long";
			else if(typeName.equals("boolean") || typeName.equals("$ocl_boolean"))
				return "boolean";
		}
		
		return mcType;
	}

	/**
	 * Returns the file manipulator object
	 * 
	 * @return
	 */
	private ModelCenterFileManipulator getModelCenterFileManipulator() {
		return modelCenterFileManipulator_;
	}

	/**
	 * Sets the file manipulator object
	 * 
	 * @param modelCenterFileManipulator
	 */
	private void setModelCenterFileManipulator(ModelCenterFileManipulator modelCenterFileManipulator) {
		this.modelCenterFileManipulator_ = modelCenterFileManipulator;
	}

	/**
	 * @return the connectorHandler
	 */
	private ConnectorHandler getConnectorHandler() {
		return connectorHandler_;
	}

	/**
	 * @param connectorHandler the connectorHandler to set
	 */
	private void setConnectorHandler(ConnectorHandler connectorHandler) {
		this.connectorHandler_ = connectorHandler;
	}

	/**
	 * @return the modelCenterModelInstances
	 */
	private ArrayList<ModelCenterModelInstance> getModelCenterModelInstances() {
		return modelCenterModelInstances_;
	}

	/**
	 * @param modelCenterModelInstances the modelCenterModelInstances to set
	 */
	private void setModelCenterModelInstances(
			ArrayList<ModelCenterModelInstance> modelCenterModelInstances) {
		this.modelCenterModelInstances_ = modelCenterModelInstances;
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

	/**
	 * 
	 * @param modelFilename
	 */
	public void setOutputFilename(String modelFilename) {
		this.outputFilename_ = modelFilename;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getOutputFilename() {
		return this.outputFilename_;
	}
	
}
