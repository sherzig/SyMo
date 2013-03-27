/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.view;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import sun.awt.SunHints.Value;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Generalization;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceValue;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.MultiplicityElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Slot;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.ConnectableElement;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.Connector;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.ConnectorEnd;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;
import com.phoenix_int.ModelCenter.Array;
import com.phoenix_int.ModelCenter.Assembly;
import com.phoenix_int.ModelCenter.Component;
import com.phoenix_int.ModelCenter.DoubleArray;
import com.phoenix_int.ModelCenter.Group;
import com.phoenix_int.ModelCenter.ModelCenterException;
import com.phoenix_int.ModelCenter.Variable;
import com.phoenix_int.ModelCenter.Variant;
import com.sun.xml.internal.bind.v2.model.core.ReferencePropertyInfo;

import edu.gatech.mbse.plugins.mdmc.controller.ConnectorHandler;
import edu.gatech.mbse.plugins.mdmc.controller.InstanceHandler;
import edu.gatech.mbse.plugins.mdmc.controller.InstanceToModelCenterTransformation;
import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;
import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterValuesImporter;
import edu.gatech.mbse.plugins.mdmc.controller.TransformationEngine;
import edu.gatech.mbse.plugins.mdmc.exceptions.ErrorInModelCenterModelException;
import edu.gatech.mbse.plugins.mdmc.exceptions.InstanceValuesNotDefinedException;
import edu.gatech.mbse.plugins.mdmc.model.ModelCenterModelInstance;

/**
 * @author Sebastian
 *
 */
public class SolveAction_PRIOR_TO_CHANGE extends DefaultBrowserAction {
	
	private ArrayList<ModelCenterModelInstance> modelCenterModels_ = null;
	private ConnectorHandler connectorHandler_ = null;
	private InstanceHandler instanceHandler_ = null;
	private TransformationEngine transformationEngine_ = null;

	/**
	 * 
	 */
	public SolveAction_PRIOR_TO_CHANGE() {
		super("", "Run Associated PHX ModelCenter Model(s)", null, null);
		
		// Set icon
		this.setSmallIcon(new ImageIcon(getClass().getResource("run.gif")));
		
		// Allocate memory
		this.modelCenterModels_ = new ArrayList<ModelCenterModelInstance>();
		this.connectorHandler_ = new ConnectorHandler();
		this.instanceHandler_ = new InstanceHandler();
		this.transformationEngine_ = new TransformationEngine();
	}
	
	/**
	 * Solve one instance
	 */
	public void actionPerformed(ActionEvent e) {
		// First, check whether model is in sync and offer to continue if only some things are omitted in
		// the SysML model. If, however, there are elements (e.g. variables) that have connections, offer
		// to synchronize, else abort
		
		// Hand the tree object to the handler classes
		getConnectorHandler().setTree(this.getTree());
		getInstanceHandler().setTree(this.getTree());
		
		// First rebuild the list of connectors
		getConnectorHandler().rebuildConnectorsList();
		
		// Also clear the list of slot values that were previously contained in the list of already cleared slots
		// (Note: this was added to allow for multiple values from multiple ModelCenter models to be set for a variable)
		getInstanceHandler().clearResetSlotsList();
		
		// Update all of the ModelCenter models by checking which of their ports are inputs and which
		// ones are outputs
		ModelCenterPlugin.ensureMDSessionIsActive();
		
		try {
			updateVariableDirectionsOfModelCenterModels(getTree().getRootNode());
		}
		catch (ModelCenterException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		finally {
			ModelCenterPlugin.closeMDSession();
		}
		
		// Now go through model and search for property entries that represent ModelCenter models and
		// solve them
		findModelCenterModelsAndSolve();
	}
	
	/**
	 * Go through the tree and open ALL of the ModelCenter models and check, for each variable, which
	 * one of them is not an input
	 * 
	 * @param node
	 * @throws ModelCenterException
	 */
	private void updateVariableDirectionsOfModelCenterModels(Node node) throws ModelCenterException {
		// Go through sub nodes of current node
		for(int i=0; i<node.getChildCount(); i++) {
			Node subNode = (Node)node.getChildAt(i);
			
			// If the element connected to the current sub node is a classifier, check whether it has relations
			if(subNode.getUserObject() instanceof Class) {
				Classifier currentClassifier = (Classifier)subNode.getUserObject();
				
				// Check whether the relation is a connector
				if(currentClassifier instanceof Element) {
					// Add it to the list
					if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel((Element)currentClassifier)) {
						// Update directions of variables
						Element el = (Element)currentClassifier;
						String filename = ModelCenterPlugin.getMDModelHandlerInstance().getModelCenterDataModelFilename(el);
						
						// TODO: Throw an exception here: if this model cannot be converted automatically and is not attached
						// to an external model, it cannot be solved!
						if(filename == null || filename.equals(""))
							return;
						
						// First load the ModelCenter model
						ModelCenterPlugin.getModelCenterInstance().loadFile(filename);
						
						// Then get the model as an assembly of variables, components and subassemblies
						Assembly currentAssembly = ModelCenterPlugin.getModelCenterInstance().getModel();

						// Now transfer the data from the solved model over into the instance
						findAndUpdateVariablesRecursively(el, currentAssembly);
					}
				}
			}
			
			// If the node has children, go through the children and call the function recursively
			if(subNode.getChildCount() > 0)
				updateVariableDirectionsOfModelCenterModels(subNode);
		}
	}
	
	/**
	 * Travers through the ModelCenter model recursively
	 * 
	 * @param el
	 * @param currentAssembly
	 * @throws ModelCenterException 
	 */
	private void findAndUpdateVariablesRecursively(Element el, Assembly currentAssembly) throws ModelCenterException {
		// Go through children (e.g. assemblies, variables, etc.)
		for(Iterator<Element> iter=el.getOwnedElement().iterator(); iter.hasNext(); ) {
			Element subElement = iter.next();
			
			// TODO: Components need to be treated somehow
			if(subElement instanceof Port) {
				if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterVariable(subElement)) {
					if(!currentAssembly.getVariable(((Port)subElement).getName()).isInputToModel()) {
						// Update variable to be an output
						ModelCenterPlugin.getMDModelHandlerInstance().setVariableDirectionToOutput(subElement);
					}
					else {
						ModelCenterPlugin.getMDModelHandlerInstance().setVariableDirectionToInput(subElement);
					}
				}
			}
		}
	}
	
	/**
	 * Go through model based on a root instance specification and search for constraint properties
	 * that represent ModelCenter models. For each one that was found, check whether all of the inputs
	 * are available (including already solved values from other models that are available either as
	 * outputs on ports or as property values). If yes, solve, and add to list of solved models. If not,
	 * continue with next.<br>
	 * <br>
	 * The goal is to solve only the relevant ModelCenter models!
	 */
	private void findModelCenterModelsAndSolve() {
		// TODO: Also search generalizations!
		// First, clear the list of "solved" models - resolve all at this point
		getModelCenterModels().clear();
		
		// Get the selected instance
		InstanceSpecification instanceSpec = (InstanceSpecification)getTree().getSelectedNode().getUserObject();
		
		// Create the base model of the full modelcenter model (basically transform all SysML elements except for
		// the ModelCenter model usages to the ModelCenter file - insert the ModelCenter models that are referenced
		// later (as soon as we have their input and output values defined - that way we can size vectors and matrices more easily)
		try {
			this.getTransformationEngine().getInstanceToModelCenterTransformation().createBaseModel(instanceSpec, this.getTree());
		}
		catch (ModelCenterException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Below is a first draft of a really simple scheduler - it is similar to the one used by ModelCenter
		// by default and does not work if cyclic dependencies are present.
		// The reason we need to include this scheduler is that we may need to size input arrays correctly.
		// For instance, if a calculation depends on the number of input values (determined by e.g. a
		// block with a composition relation to another block with a value, i.e. n values), it is important
		// to know the size of the vector - hence execute the model once using the simple scheduler of the plugin,
		// then generate a full modelcenter file that can be used for optimization purposes
		int numModelCenterModelUsages = getNumberOfModelCenterModelUsages(instanceSpec);
		int maxSearches = 2*numModelCenterModelUsages;
		
		System.out.println("There are a total of " + numModelCenterModelUsages + " usages of ModelCenter models");
		
		if(numModelCenterModelUsages > 0) {
			for(int i=0; i<maxSearches; i++) {
				System.out.println("Current depth of precedence tree: " + i);
				
				try {
					searchForModelCenterModelsAndSolve(instanceSpec);
				}
				catch (ErrorInModelCenterModelException e) {
					// No use in trying to execute the model numerous times - exit the function
					return;
				}
	
				if(getModelCenterModels().size() >= numModelCenterModelUsages)
					break;
			}
			
			// Check whether we were able to successfully solve all models
			if(getModelCenterModels().size() != numModelCenterModelUsages) {
				// TODO: Potentially try and fill in dummy values
				JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Unable to resolve all input values. Please check for any possible\ncyclic dependencies in your model.", "ModelCenter Plugin - Cyclic Redundancy Found", JOptionPane.ERROR_MESSAGE);
			}
			else {
				try {
					this.getTransformationEngine().getInstanceToModelCenterTransformation().createLinks(instanceSpec);
				}
				catch (ModelCenterException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else {
			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "No nested usages of ModelCenter models were found for this particular instance specification", "ModelCenter Plugin - Nothing To Solve", JOptionPane.INFORMATION_MESSAGE);
		}
		
		// Delete any temporary files
		File dir = new File(Application.getInstance().getProject().getDirectory());
		
		// Get a list of temporary files (perhaps from earlier sessions)
		File[] toDelete = dir.listFiles(new FilenameFilter() {
			
			public boolean accept(File dir, String filename) {
				return filename.endsWith("__temp_0964_MCPLUGIN_Generated.pxc");
			}
			
		});
		
		// Delete all temporary files
		if(toDelete != null) {
			for(int i=0; i<toDelete.length; i++) {
				toDelete[i].delete();
			}
		}
	}
	
	/**
	 * 
	 * @return
	 */
	private int getNumberOfModelCenterModelUsages(InstanceSpecification instanceSpec) {
		int numberOfModelCenterModelUsages = 0;
		
		// Go through element and see whether the sub-elements / properties are ModelCenter models
		List<Classifier> classifiers = findGeneralizations(instanceSpec.getClassifier());
			
		// Go through all classifiers
		for(int j=0; j<classifiers.size(); j++) {
			Classifier classifier = classifiers.get(j);
				
			// Search recursively
			if(classifier instanceof Element) {
				Element toParse = (Element)classifier;
				
				for(Iterator<Element> elementIterator = toParse.getOwnedElement().iterator(); elementIterator.hasNext(); ) {
					Element nextElement = elementIterator.next();
					
					if(nextElement instanceof Property) {
						if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(((Property)nextElement).getType()))
							numberOfModelCenterModelUsages++;
					}
				}
			}
		}
		
		for(Iterator<Slot> iter = instanceSpec.getSlot().iterator(); iter.hasNext(); ) {
			Slot nextSlot = iter.next();
			
			for(Iterator<ValueSpecification> valueIter = nextSlot.getValue().iterator(); valueIter.hasNext(); ) {
				ValueSpecification nextValueSpec = valueIter.next();
				
				if(nextValueSpec instanceof InstanceValue) {
					numberOfModelCenterModelUsages += getNumberOfModelCenterModelUsages(((InstanceValue)nextValueSpec).getInstance());
				}
			}
		}
		
		return numberOfModelCenterModelUsages;
	}
	
	/**
	 * Search recursively for ModelCenter models that are attached
	 * 
	 * @param nextElement
	 * @throws ErrorInModelCenterModelException 
	 */
	private void searchForModelCenterModelsAndSolve(InstanceSpecification instanceSpec) throws ErrorInModelCenterModelException {
		// Go through element and see whether the sub-elements / properties are ModelCenter models
		List<Classifier> classifiers = findGeneralizations(instanceSpec.getClassifier());
			
		// Go through all classifiers
		for(int j=0; j<classifiers.size(); j++) {
			Classifier classifier = classifiers.get(j);
				
			// Search recursively
			if(classifier instanceof Element) {
				Element toParse = (Element)classifier;
				
				for(Iterator<Element> elementIterator = toParse.getOwnedElement().iterator(); elementIterator.hasNext(); ) {
					Element nextSubElement = elementIterator.next();
					
					if(nextSubElement instanceof Property) {
						if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(((Property)nextSubElement).getType())) {
							// If all of the inputs are available
							if(!hasBeenWorkedWith((Property)nextSubElement, instanceSpec) && hasAllInputsAvailable((Property)nextSubElement, instanceSpec)) {
								// Then solve the model and, after doing so, automatically add it to the list of solved models
								solveModel((Property)nextSubElement, instanceSpec);
							}
						}
					}
				}
			}
		}
		
		for(Iterator<Slot> iter = instanceSpec.getSlot().iterator(); iter.hasNext(); ) {
			Slot nextSlot = iter.next();
			
			for(Iterator<ValueSpecification> valueIter = nextSlot.getValue().iterator(); valueIter.hasNext(); ) {
				ValueSpecification nextValueSpec = valueIter.next();
				
				if(nextValueSpec instanceof InstanceValue) {
					searchForModelCenterModelsAndSolve(((InstanceValue)nextValueSpec).getInstance());
				}
			}
		}
	}
	
	/**
	 * Run a particular model, given a property that is of a type representing a ModelCenter model
	 * 
	 * @param modelCenterModel
	 * @throws ErrorInModelCenterModelException 
	 */
	private void solveModel(Property modelCenterModel, InstanceSpecification instanceSpec) throws ErrorInModelCenterModelException {
		String filename = ModelCenterPlugin.getMDModelHandlerInstance().getModelCenterDataModelFilename(modelCenterModel.getType());
		
		// Check whether a filename is given
		if(filename == null || filename.equals(""))
			return;		// TODO: Throw exception
		
		int n = 3;
		boolean loaded = false;
		
		// Try to load a model - if an exception occurs, try creating a new instance of the ModelCenter
		// class and use that new instance to load the model (up to n times)
		while(loaded == false && n > 0) {
			n--;
			
			try {
				// Close any old models
				ModelCenterPlugin.getModelCenterInstance().closeModel();
				
				// Try to load model
				ModelCenterPlugin.getModelCenterInstance().loadModel(filename);
				
				loaded = true;
			}
			catch (ModelCenterException e) {
				// If that didn't work, try creating a new instance and then try loading the file again
				if(n <= 0)
					e.printStackTrace();
				else {
					try {
						ModelCenterPlugin.resetModelCenterInstance();
					}
					catch (ModelCenterException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}
		
		try {
			System.out.println("Name: " + modelCenterModel.getName() + " : and qualified path: " + modelCenterModel.getQualifiedName());
		
			// Update inputs to model - do this only once per model "instance"!!
			updateInputsToModel(modelCenterModel, instanceSpec);
			
			// Run model in ModelCenter
			System.out.println("Running model");
			// TODO: Do not run if we are just creating the full model center file
			ModelCenterPlugin.getModelCenterInstance().run(null);
			
			// Save the model - this ensures that parts of the model will not be solved if minor changes
			// are made to the instance, which will speed up the solving process tremenduously
			System.out.println("Last was " + ModelCenterPlugin.getModelCenterInstance().getModelFileName());
			ModelCenterPlugin.getModelCenterInstance().saveModel();
			
			// Ensure we have an active session so that we can edit the model
			ModelCenterPlugin.ensureMDSessionIsActive();
			
			// Get all output values and update value properties in instance accordingly
			updateLinkedOutputs(modelCenterModel, instanceSpec);
			
			// Add entry to solved models and record outputs
			getModelCenterModels().add(new ModelCenterModelInstance(modelCenterModel, instanceSpec));
		}
		catch(ModelCenterException e) {
			e.printStackTrace();
			
			if(e.getMessage().contains("Failed Component")) {
				JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Corresponding SysML Model Element: " + modelCenterModel.getQualifiedName() + "\n" + e.getMessage(), "ModelCenter Plugin - Error in ModelCenter Model", JOptionPane.ERROR_MESSAGE);
				
				throw new ErrorInModelCenterModelException();
			}
		}
		catch(InstanceValuesNotDefinedException e) {
			// Some instance values were not defined - not a problem, just add the model to the list of
			// "solved" models (this exception will be thrown if e.g. the instance does not use values
			// that are required for a model to execute, hence no calculations can be done
			System.out.println("Ignoring model from computation");
			
			getModelCenterModels().add(new ModelCenterModelInstance(modelCenterModel, instanceSpec, false));
		}
		finally {
			// Close the session once done
			ModelCenterPlugin.closeMDSession();
		}
		
		// Close any old models
		try {
			ModelCenterPlugin.getModelCenterInstance().closeModel();
		}
		catch (ModelCenterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Now insert this solved model into the large model that represents the entire system model
		try {
			this.getTransformationEngine().getInstanceToModelCenterTransformation().insertSpecificModelCenterUsage(modelCenterModel, instanceSpec, (InstanceSpecification)getTree().getSelectedNode().getUserObject());
		}
		catch (ModelCenterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Update instance values with the calculated values from the ModelCenter models
	 * 
	 * @param modelCenterModel
	 */
	private void updateLinkedOutputs(Property modelCenterModel, InstanceSpecification instanceSpec) {
		for(Iterator<Element> iter = modelCenterModel.getType().getOwnedElement().iterator(); iter.hasNext(); ) {
			Element el = iter.next();
			
			if(el instanceof Port) {
				if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterOutputVariable(el)) {
					ArrayList<Connector> connectors = getConnectorHandler().getConnectorsForElement(el);
					
					for(int i=0; i<connectors.size(); i++) {
						Connector curConnector = connectors.get(i);
						List <ConnectorEnd> list = curConnector.getEnd();
						ConnectorEnd connEnd1 = list.get(0);
						ConnectorEnd connEnd2 = list.get(1);
						Property propModel1 = connEnd1.getPartWithPort();
						Property propModel2 = connEnd2.getPartWithPort();
						
						ConnectableElement outputValueElement = connEnd2.getRole();
						
						// Check whether we have a relevant connector
						if((propModel1 == modelCenterModel && connEnd1.getRole() == el) || (propModel2 == modelCenterModel && connEnd2.getRole() == el)) {
							if(propModel2 == modelCenterModel)
								outputValueElement = connEnd1.getRole();
							
							try {
								// Retrieve value in ModelCenter file
								Variable outputVar = ModelCenterPlugin.getModelCenterInstance().getModel().getVariable(((Port)el).getName());
								ArrayList<ArrayList<Variant>> outputValues = new ArrayList<ArrayList<Variant>>();
								ArrayList<Variant> valuesToAdd = new ArrayList<Variant>();
								
								if(outputVar instanceof Array) {
									Array arrayValue = (Array)outputVar;
									
									int secondDimLength = 0;
									
									if(arrayValue.getNumDimensions() > 1) {
										secondDimLength = (int)arrayValue.getLength(1);
										
										for(int x=0; x<secondDimLength; x++) {
											for(int j=0; j<arrayValue.getLength(0); j++) {
												valuesToAdd.add(ModelCenterPlugin.getModelCenterInstance().getValue(outputVar.getFullName() + "[" + j + "," + x + "]"));
											}
											
											// Copy the created list of output values onto a new list using its copy constructor
											outputValues.add(new ArrayList<Variant>(valuesToAdd));
											
											// Clear the list of values and continue with the next
											valuesToAdd.clear();
										}
									}
									else {
										// Get the multiplicity
										String multiplicity = ModelHelper.getMultiplicity((MultiplicityElement)outputValueElement);
										
										// This is a little trick to enable arrays to be filled if no matrix is present
										// This is highly experimental and probably not a good idea since it is inconsistent
										// with the specified behavior for vectors and matrices - but a good way to read vectors
										// from ModelCenter files
										// TODO: Watch out for unexpected behavior
							    		if(multiplicity == null || multiplicity.equals("") || multiplicity.equals("1") || multiplicity.equals("0")) {
											for(int j=0; j<arrayValue.getLength(); j++) {
												valuesToAdd.add(ModelCenterPlugin.getModelCenterInstance().getValue(outputVar.getFullName() + "[" + j + "]"));
												
												// Copy the created list of output values onto a new list using its copy constructor
												outputValues.add(new ArrayList<Variant>(valuesToAdd));
												
												// Clear the list of values and continue with the next
												valuesToAdd.clear();
											}
										}
										else {
											for(int j=0; j<arrayValue.getLength(); j++) {
												valuesToAdd.add(ModelCenterPlugin.getModelCenterInstance().getValue(outputVar.getFullName() + "[" + j + "]"));
											}
											
											// Copy the created list of output values onto a new list using its copy constructor
											outputValues.add(new ArrayList<Variant>(valuesToAdd));
											
											// Clear the list of values and continue with the next
											valuesToAdd.clear();
										}
									}
								}
								else {
									valuesToAdd.add(ModelCenterPlugin.getModelCenterInstance().getValue(outputVar.getFullName()));
									
									outputValues.add(valuesToAdd);
								}
								
								// Update instance
								getInstanceHandler().setInstanceValuesForElement(outputValueElement, instanceSpec, (InstanceSpecification)getTree().getSelectedNode().getUserObject(), outputValues);
								System.out.println(outputValues.toString());
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
	 * Resets the size and dimensions of all ModelCenter array variables to 0, resp. 1. This is done
	 * so that the plugin can automatically size the arrays during the solving process
	 * 
	 * @param rootModelCenterModel
	 * @throws ModelCenterException
	 */
	private void resetArraysInCurrentModelCenterModel() throws ModelCenterException {
		resetArraysInCurrentModelCenterModel(ModelCenterPlugin.getModelCenterInstance().getModel());
	}
	
	/**
	 * Resets the size and dimensions of all ModelCenter array variables to 0, resp. 1. This is done
	 * so that the plugin can automatically size the arrays during the solving process
	 * 
	 * @param rootModelCenterModel
	 * @throws ModelCenterException
	 */
	private void resetArraysInCurrentModelCenterModel(Assembly curAssembly) throws ModelCenterException {
		for(int i=0; i<curAssembly.getNumVariables(); i++) {
			if(curAssembly.getVariable(i) instanceof Array) {
				((Array)curAssembly.getVariable(i)).setDimensions(1);
				((Array)curAssembly.getVariable(i)).setSize(0);
			}
		}
		
		// Also search sub assemblies
		for(int i=0; i<curAssembly.getNumAssemblies(); i++) {
			resetArraysInCurrentModelCenterModel(curAssembly.getAssembly(i));
		}
		
		// And subcomponents
		for(int i=0; i<curAssembly.getNumComponents(); i++) {
			resetArraysInCurrentModelCenterModel(curAssembly.getComponent(i));
		}
		
		// And groups
		for(int i=0; i<curAssembly.getNumComponents(); i++) {
			resetArraysInCurrentModelCenterModel(curAssembly.getComponent(i));
		}
	}
	
	/**
	 * Resets the size and dimensions of all ModelCenter array variables to 0, resp. 1. This is done
	 * so that the plugin can automatically size the arrays during the solving process
	 * 
	 * @param rootModelCenterModel
	 * @throws ModelCenterException
	 */
	private void resetArraysInCurrentModelCenterModel(Component curComponent) throws ModelCenterException {
		for(int i=0; i<curComponent.getNumVariables(); i++) {
			if(curComponent.getVariable(i) instanceof Array) {
				((Array)curComponent.getVariable(i)).setDimensions(1);
				((Array)curComponent.getVariable(i)).setSize(0);
			}
		}
		
		// Also search sub assemblies
		for(int i=0; i<curComponent.getNumGroups(); i++) {
			resetArraysInCurrentModelCenterModel(curComponent.getGroup(i));
		}
	}
	
	/**
	 * Resets the size and dimensions of all ModelCenter array variables to 0, resp. 1. This is done
	 * so that the plugin can automatically size the arrays during the solving process
	 * 
	 * @param rootModelCenterModel
	 * @throws ModelCenterException
	 */
	private void resetArraysInCurrentModelCenterModel(Group curGroup) throws ModelCenterException {
		for(int i=0; i<curGroup.getNumVariables(); i++) {
			if(curGroup.getVariable(i) instanceof Array) {
				((Array)curGroup.getVariable(i)).setDimensions(1);
				((Array)curGroup.getVariable(i)).setSize(0);
			}
		}
		
		// Also search sub assemblies
		for(int i=0; i<curGroup.getNumGroups(); i++) {
			resetArraysInCurrentModelCenterModel(curGroup.getGroup(i));
		}
	}
	
	/**
	 * Fetch instance values from the SysML model and input the ModelCenter model with these inputs
	 * 
	 * @param rootModelCenterModel
	 * @throws InstanceValuesNotDefinedException 
	 */
	private void updateInputsToModel(Property rootModelCenterModel, InstanceSpecification instanceSpec) throws InstanceValuesNotDefinedException {
		// First reset the arrays in the current ModelCenter model so that we can size them automatically
		// TODO: is this a good idea?
		try {
			resetArraysInCurrentModelCenterModel();
		} catch (ModelCenterException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Iterate through the elements of the type of the property, i.e. the definition of the <<ModelCenterDataModel>> block
		for(Iterator<Element> iter = rootModelCenterModel.getType().getOwnedElement().iterator(); iter.hasNext(); ) {
			// Get the next element
			Element el = iter.next();
			
			// Check whether we have a port
			if(el instanceof Port) {
				// Now check whether this port is in fact a ModelCenter input variable
				if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterInputVariable(el)) {
					// Retrieve the connectors relevant to this port
					ArrayList<Connector> connectors = getConnectorHandler().getConnectorsForElement(el);
					
					// Go through the list of connectors
					for(int i=0; i<connectors.size(); i++) {
						// Get the current connector
						Connector curConnector = connectors.get(i);
						
						// Retrieve the list of ends
						List <ConnectorEnd> list = curConnector.getEnd();
						
						// Grab the first two ends
						ConnectorEnd connEnd1 = list.get(0);
						ConnectorEnd connEnd2 = list.get(1);
						
						// Get the parts with ports
						Property propModel1 = connEnd1.getPartWithPort();
						Property propModel2 = connEnd2.getPartWithPort();
						
						ConnectableElement inputValue = connEnd2.getRole();
						Property otherModel = propModel2;
						
						// Check whether we have a relevant connector
						if((propModel1 == rootModelCenterModel && connEnd1.getRole() == el) || (propModel2 == rootModelCenterModel && connEnd2.getRole() == el)) {
							if(propModel2 == rootModelCenterModel) {
								inputValue = connEnd1.getRole();
								otherModel = propModel1;
							}
							
							System.out.println("Input value used: " + inputValue.getName());
							
							try {
								// Update value in ModelCenter file
								// TODO: Multiple values: move this out of for loop, build string, then set
								Variable varToSet = ModelCenterPlugin.getModelCenterInstance().getModel().getVariable(((Port)el).getName());
								ArrayList<Variant> values = new ArrayList<Variant>();
								
								if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterInputVariable(inputValue)) {
									// TODO: throw an exception: input connected to input!
								}
								else if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterOutputVariable(inputValue)) {
									// At this point, the output SHOULD be available from the solved part-with-port model
									// representing the modelcenter model
									if(otherModel != null && hasBeenSolvedAcrossAllInstances(otherModel, (InstanceSpecification)getTree().getSelectedNode().getUserObject())) {
										for(int x=0; x<getModelCenterModels().size(); x++) {
											// Get the values from all instantiations of the blocks linked to the output model
											if(getModelCenterModels().get(x).getCorrespondingProperty() == otherModel) {
												if(isNestedInInstance(getModelCenterModels().get(x).getCorrespondingInstanceSpecification(), instanceSpec)) {
													if(getModelCenterModels().get(x).getOutputVariableInstanceForVariable((Port)inputValue) != null)
														values.add(getModelCenterModels().get(x).getOutputVariableInstanceForVariable((Port)inputValue).getValue());
												}
											}
										}
									}
								}
								else if(inputValue instanceof Property) {
									// TODO: There could be multiple values for this element
									values.addAll(getInstanceHandler().findInstanceValuesForElement(inputValue, instanceSpec));
								}
							
								System.out.println("Values are apparently: " + values);
								
								// Default to "0" as a value if none was retrieved - this avoids an exception to
								// being thrown by ModelCenter when it tries parsing the value string
								if(values == null || values.size() <= 0) {
									// This case can happen when there are specific parts of the object that are not
									// instantiated, e.g. a specific value is not instantiated
									System.out.println("Some inputs have not been defined properly!!!");
									
									throw new InstanceValuesNotDefinedException();
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
											
											// Set the number of dimensions required
											if(((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).getNumDimensions() < 2) {
												((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).setNumDimensions(2);
												
												// Set size for dimension 0 (1 row)
												((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).setSize(1, 0);
												
												// Set size for dimension 1 (n columns)
												((Array)(ModelCenterPlugin.getModelCenterInstance().getVariable(varToSet.getFullName()))).setSize(length, 1);
											}
											
											for(int x=0; x<length; x++) {
												// Set the vector length - in this case 1 
												if(values.get(0).getType() == Variant.BOOLEAN_ARRAY)
													ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[0," + x + "]", Boolean.toString(values.get(0).booleanArrayValue()[x]));
												else if(values.get(0).getType() == Variant.DOUBLE_ARRAY)
													ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[0," + x + "]", Double.toString(values.get(0).doubleArrayValue()[x]));
												else if(values.get(0).getType() == Variant.INT_ARRAY)
													ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[0," + x + "]", Integer.toString(values.get(0).intArrayValue()[x]));
												else if(values.get(0).getType() == Variant.LONG_ARRAY)
													ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[0," + x + "]", Long.toString(values.get(0).longArrayValue()[x]));
												else if(values.get(0).getType() == Variant.STRING_ARRAY)
													ModelCenterPlugin.getModelCenterInstance().setValue(varToSet.getFullName() + "[0," + x + "]", values.get(0).stringArrayValue()[x]);
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
							catch(ModelCenterException e) {
								// TODO: Handle
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
	 * @param nestedElement
	 * @param baseElement
	 * @return
	 */
	private boolean isNestedInInstance(InstanceSpecification nestedElement, InstanceSpecification baseElement) {
		if(nestedElement == baseElement)
			return true;
		
		for(Iterator<Slot> slotIter = baseElement.getSlot().iterator(); slotIter.hasNext(); ) {
			Slot nextSlot = slotIter.next();
			
			for(Iterator<ValueSpecification> valueIter = nextSlot.getValue().iterator(); valueIter.hasNext(); ) {
				ValueSpecification nextValueSpec = valueIter.next();
				
				if(nextValueSpec instanceof InstanceValue) {
					InstanceSpecification nextInstanceSpec = ((InstanceValue)nextValueSpec).getInstance();
					
					if(nextInstanceSpec == nestedElement) {
						return true;
					}
					else {
						isNestedInInstance(nestedElement, nextInstanceSpec);
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Determines whether or not all input parameter values are available to a given ModelCenter model
	 * 
	 * @param modelCenterModel
	 * @return true if all inputs are available, false otherwise
	 */
	private boolean hasAllInputsAvailable(Property modelCenterModel, InstanceSpecification instanceSpec) {
		// All inputs are available if and only if:
		// 1) All inputs of the modelcenter model are value properties that:
		// 1.1) are not connected to any output ports of modelcenter models
		// 1.2) are not connected to output ports of any modelcenter models that have not yet been solved
		// 2) Any input ports are connected to ModelCenter models that have already been executed
		// 3) If the ModelCenter model is part of e.g. a subassembly that contains components, make sure
		//    that the values for these components are available
		// (NOTE: That way one could start with one model in a cyclic representation and then cycle until
		// point is reached where outputs no longer change?)
		// Iterate through the elements of the type of the property, i.e. the definition of the <<ModelCenterDataModel>> block
		for(Iterator<Element> iter = modelCenterModel.getType().getOwnedElement().iterator(); iter.hasNext(); ) {
			// Get the next element
			Element el = iter.next();
			
			// Check whether we have a port
			if(el instanceof Port) {
				// Now check whether this port is in fact a ModelCenter input variable
				if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterInputVariable(el)) {
					// Retrieve the connectors relevant to this port
					ArrayList<Connector> connectors = getConnectorHandler().getConnectorsForElement(el);
					
					// Go through the list of connectors
					for(int i=0; i<connectors.size(); i++) {
						// Get the current connector
						Connector curConnector = connectors.get(i);
						
						// Retrieve the list of ends
						List <ConnectorEnd> list = curConnector.getEnd();
						
						// Grab the first two ends
						ConnectorEnd connEnd1 = list.get(0);
						ConnectorEnd connEnd2 = list.get(1);
						
						// Get the parts with ports
						Property propModel1 = connEnd1.getPartWithPort();
						Property propModel2 = connEnd2.getPartWithPort();
						
						// Guess that our ModelCenter model is at end "1"
						ConnectableElement elementToCheck = connEnd2.getRole();
						Property partWithPortToCheck = connEnd2.getPartWithPort();
						
						// Check whether we have a relevant connector
						if((propModel1 == modelCenterModel && connEnd1.getRole() == el) || (propModel2 == modelCenterModel && connEnd2.getRole() == el)) {
							// Check whether our ModelCenter model truly is at end "1" and change otherwise
							if(propModel2 == modelCenterModel) {
								elementToCheck = connEnd1.getRole();
								partWithPortToCheck = connEnd1.getPartWithPort();
							}
							
							// If element to check is a modelcenter input variable, throw exception
							// (inputs are not allowed to be connected to inputs!)
							if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterInputVariable(elementToCheck)) {
								// TODO: Throw exception
								System.out.println("Error: Input connected to input!");
							}
							else if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterOutputVariable(elementToCheck)) {
								// Element on the other end of the ModelCenter port is an output of another modelcenter model
								// Hence, check whether its associated part with port (the ModelCenter model) has been solved
								// Therefore check whether all models are solved in the corresponding instance level
								// i.e. search for model within instance and check all of the ones that it appears in
								if(!hasBeenSolvedAcrossAllInstances(partWithPortToCheck, (InstanceSpecification)getTree().getSelectedNode().getUserObject())) {
									System.out.println("Found a ModelCenter model that requires other models to execute first");
									
									return false;
								}
							}
							else if(elementToCheck instanceof Property) {
								// Check whether this property is connected to an output port of another modelcenter
								// model and, if yes, whether this model has already been solved
								ArrayList<Connector> secondLevelConnectors = getConnectorHandler().getConnectorsForElement(elementToCheck);
								
								// Go through the list of connectors
								for(int j=0; j<secondLevelConnectors.size(); j++) {
									// Get the current connector
									Connector con = secondLevelConnectors.get(j);
									
									// Retrieve the list of ends
									List <ConnectorEnd> ends = con.getEnd();
									
									// Check whether role is output and partwithport is unsolved modelcenter model
									// TODO: If models are nested, this may not work
									for(int k=0; k<ends.size(); k++)
										if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterOutputVariable(ends.get(k).getRole()))
											if(ends.get(k).getPartWithPort() != null && !hasBeenSolvedAcrossAllInstances(ends.get(k).getPartWithPort(), (InstanceSpecification)getTree().getSelectedNode().getUserObject())) {
												System.out.println("Found a ModelCenter model that requires other models to execute first");
												
												return false;
											}
								}
							}
						}
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Determine whether a given ModelCenter model has been solved in this run
	 * 
	 * @param modelCenterModel
	 * @return
	 */
	private boolean hasBeenWorkedWith(Property modelCenterModel, InstanceSpecification instanceSpec) {
		// Go through the list in which all models that have already been solved during this run are saved
		for(int i=0; i<this.getModelCenterModels().size(); i++)
			if(this.getModelCenterModels().get(i).isUsage(modelCenterModel, instanceSpec))
				return true;
		
		return false;
	}
	
	/**
	 * 
	 * @param modelCenterModel
	 * @param instanceSpec
	 * @return
	 */
	private boolean hasBeenSolvedAcrossAllInstances(Property modelCenterModel, InstanceSpecification instanceSpec) {
		// Go through element and see whether the sub-elements / properties are ModelCenter models
		List<Classifier> classifiers = findGeneralizations(instanceSpec.getClassifier());
			
		// Go through all classifiers
		for(int j=0; j<classifiers.size(); j++) {
			Classifier classifier = classifiers.get(j);
				
			// Search recursively
			if(classifier instanceof Element) {
				Element toParse = (Element)classifier;
				
				for(Iterator<Element> elementIterator = toParse.getOwnedElement().iterator(); elementIterator.hasNext(); ) {
					Element nextElement = elementIterator.next();
					
					if(nextElement instanceof Property) {
						Property curProperty = (Property)nextElement;
						
						if(curProperty == modelCenterModel) {
							if(!hasBeenWorkedWith(curProperty, instanceSpec))
								return false;
						}
					}
				}
			}
		}
		
		for(Iterator<Slot> iter = instanceSpec.getSlot().iterator(); iter.hasNext(); ) {
			Slot nextSlot = iter.next();
			
			for(Iterator<ValueSpecification> valueIter = nextSlot.getValue().iterator(); valueIter.hasNext(); ) {
				ValueSpecification nextValueSpec = valueIter.next();
				
				if(nextValueSpec instanceof InstanceValue) {
					if(!hasBeenSolvedAcrossAllInstances(modelCenterModel, ((InstanceValue)nextValueSpec).getInstance()))
						return false;
				}
			}
		}
		
		return true;
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
	 */
	public void updateState() {
		// TODO: Implement (if necessary)
	}
	
	/**
	 * Return the instance of the list of connectors
	 * 
	 * @return
	 */
	private ArrayList<ModelCenterModelInstance> getModelCenterModels() {
		return this.modelCenterModels_;
	}
	
	/**
	 * Returns the connection handler object
	 * 
	 * @return
	 */
	public ConnectorHandler getConnectorHandler() {
		return this.connectorHandler_;
	}
	
	/**
	 * Returns the instance handler object
	 * 
	 * @return
	 */
	public InstanceHandler getInstanceHandler() {
		return this.instanceHandler_;
	}

	/**
	 * @return the transformationEngine
	 */
	public TransformationEngine getTransformationEngine() {
		return transformationEngine_;
	}

	/**
	 * @param transformationEngine the transformationEngine to set
	 */
	public void setTransformationEngine(TransformationEngine transformationEngine) {
		this.transformationEngine_ = transformationEngine;
	}

}
