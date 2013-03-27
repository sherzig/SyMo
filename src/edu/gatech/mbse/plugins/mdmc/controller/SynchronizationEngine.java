/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Type;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.Connector;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.ConnectorEnd;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;
import com.phoenix_int.ModelCenter.AddToModel;
import com.phoenix_int.ModelCenter.Assembly;
import com.phoenix_int.ModelCenter.Component;
import com.phoenix_int.ModelCenter.ModelCenterException;
import com.phoenix_int.ModelCenter.Variable;
import com.phoenix_int.ModelCenter.Variant;

import edu.gatech.mbse.plugins.mdmc.exceptions.ElementIsNotAModelCenterModel;
import edu.gatech.mbse.plugins.mdmc.exceptions.FailedToLaunchModelCenter;
import edu.gatech.mbse.plugins.mdmc.exceptions.ModelCenterProfileNotLoaded;
import edu.gatech.mbse.plugins.mdmc.exceptions.UserCanceledOperation;
import edu.gatech.mbse.plugins.mdmc.model.ModelCenterVariable;
import edu.gatech.mbse.plugins.mdmc.model.ScriptComponent;
import edu.gatech.mbse.plugins.mdmc.util.ModelCenterFileDialogHandler;
import edu.gatech.mbse.plugins.mdmc.util.WindowsRegistry;

/**
 * @author Sebastian
 *
 */
public class SynchronizationEngine {
	
	private ArrayList<String> filesToRestore_ = new ArrayList<String>();
	private ConnectorHandler connectorHandler_ = new ConnectorHandler();
	private String alternativeModelCenterLaunchPath = "";
	private ModelCenterFileDialogHandler fileDialogHandler_ = null;
	
	/**
	 * Synchronizes a pre-loaded ModelCenter model with the SysML Model
	 * 
	 * @param rootModelCenterModelElement
	 * @throws ModelCenterException
	 */
	public void updateModelCenterModelFromSysMLModel(Element rootModelCenterModelElement, boolean fullSync) throws ModelCenterException {
		// Prerequisite is that model is already loaded
		if(!ModelCenterPlugin.getModelCenterInstance().getModelFileName().equals("")) {
			// Go through SysML model and compare with ModelCenter model
			Assembly rootModel = ModelCenterPlugin.getModelCenterInstance().getModel();
			boolean hadSubModels = false;
			
			// Rename model
			rootModel.rename(ModelCenterPlugin.toModelCenterSafeName(((NamedElement)rootModelCenterModelElement).getName()));
			
			for(Iterator<Element> elIter = rootModelCenterModelElement.getOwnedElement().iterator(); elIter.hasNext(); ) {
				Element el = elIter.next();
				
				// Handle variables
				if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterVariable(el)) {
					if(!findVariable((Port)el, rootModel)) {
						// Variable was not found, create it
						rootModel.addVariable(((Port)el).getName(), getModelCenterTypeForSysMLType((Port)el));
					}
				}
				
				// Handle scripts
				if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterScript(el)) {
					// Check whether it exists already
					if(findScriptComponent((Constraint)el, rootModel)) {
						// Remove it
						removeGeneratedScriptComponent((Constraint)el, rootModel);
					}
						
					// Add a fresh version of it
					ScriptComponent script = new ScriptComponent(((NamedElement)el).getName(), rootModelCenterModelElement);
					script.setScriptBody((String)ModelHelper.getValueSpecificationValue(((Constraint)el).getSpecification()));
					script.extractVariablesFromScriptBody();
					
					// Add the model to the modelcenter file
					ModelCenterFileManipulator m = new ModelCenterFileManipulator();
					m.insertScriptComponent(script, rootModel);
					
					// Re-save the model to its original filename
					ModelCenterPlugin.getModelCenterInstance().saveModelAs(ModelCenterPlugin.getMDModelHandlerInstance().getModelCenterDataModelFilename(rootModelCenterModelElement));
					
					rootModel = ModelCenterPlugin.getModelCenterInstance().getModel();
					
					try {
						// Now set the links, first for the input variables...
						for(Iterator<ModelCenterVariable> iter = script.getInputVariables().iterator(); iter.hasNext(); ) {
							ModelCenterVariable var = iter.next();
							
							ModelCenterPlugin.getModelCenterInstance().createLink(ModelCenterPlugin.getModelCenterInstance().getModel().getName() + "." + script.getName() + "." + var.getName(), ModelCenterPlugin.getModelCenterInstance().getModel().getName() + "." + var.getName());
						}
						
						// Now for the outputs
						for(Iterator<ModelCenterVariable> iter = script.getOutputVariables().iterator(); iter.hasNext(); ) {
							ModelCenterVariable var = iter.next();
							
							ModelCenterPlugin.getModelCenterInstance().createLink(ModelCenterPlugin.getModelCenterInstance().getModel().getName() + "." + var.getName(), ModelCenterPlugin.getModelCenterInstance().getModel().getName() + "." + script.getName() + "." + var.getName());
						}
					}
					catch(ModelCenterException e) {
						// Error setting links
						System.out.println(e.getStackTrace());
					}
				}
				
				// Handle models inside models
				if(fullSync && el instanceof Property && ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(((Property)el).getType())) {
					hadSubModels = true;
					
					// We can safely add all assemblies anew, since a "full sync" will restore the data previous to the solving process at its end
					// Travers down the tree so that all sub models get updated as well
					// To do so, first get the filename of the currently loaded model, then the filename of the model to be analyzed
					String oldFilename = ModelCenterPlugin.getModelCenterInstance().getModelFileName();
					String filename = ModelCenterPlugin.getMDModelHandlerInstance().getModelCenterDataModelFilename(((Property)el).getType());
					
					// Save the current model in a new file, hence backing up its original state (we will revert the file to its original
					// state at the end of the solving process)
					// TODO: Recover from crash
					ModelCenterPlugin.getModelCenterInstance().saveModel();
					
					if(!this.filesToRestore_.contains(oldFilename + ".orig.pxc")) {
						ModelCenterPlugin.getModelCenterInstance().saveModelAs(oldFilename + ".orig.pxc");
						this.filesToRestore_.add(oldFilename + ".orig.pxc");
					}
					
					// Load the model to be analyzed / inserted
					System.out.println("Attempting to load ModelCenter model from " + filename);
					
					// TODO: Potentially, sub-model has not yet been automatically generated!!
					try {
						ModelCenterPlugin.getModelCenterInstance().loadModel(filename);
					}
					catch(ModelCenterException e) {
						if(e.getMessage().contains("File does not")) {
							ModelCenterPlugin.getModelCenterInstance().newModel();
							ModelCenterPlugin.getModelCenterInstance().saveModelAs(filename);
						}
					}
					
					// Synchronize it (this will create any child assemblies / insert any child data models
					updateModelCenterModelFromSysMLModel(((Property)el).getType(), fullSync);
					
					// Save the edited model, close it and load the old model
					ModelCenterPlugin.getModelCenterInstance().saveModel();
					ModelCenterPlugin.getModelCenterInstance().closeModel();
					ModelCenterPlugin.getModelCenterInstance().loadModel(oldFilename);
					
					rootModel = ModelCenterPlugin.getModelCenterInstance().getModel();
					
					// Now insert the last modified model into our original model
					ModelCenterFileManipulator m = new ModelCenterFileManipulator();
					m.insertRootAssembly((Property)el, rootModel);
					
					// Re-save the model to its original filename
					ModelCenterPlugin.getModelCenterInstance().saveModelAs(oldFilename);
					
					rootModel = ModelCenterPlugin.getModelCenterInstance().getModel();
				}
			}
			
			if(fullSync && hadSubModels) {
				updateLinksToAndBetweenSubModels(rootModelCenterModelElement);
				ModelCenterPlugin.getModelCenterInstance().saveModel();
			}
		}
		
		// Following was commented out: behavior still the same?
		// removeTemporaryFiles()
	}
	
	/**
	 * 
	 */
	public void removeTemporaryFiles() {
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
	 * @param rootModelCenterModelElement
	 * @throws ModelCenterException
	 */
	public void updateModelCenterModelFromSysMLModel(Element rootModelCenterModelElement) throws ModelCenterException {
		updateModelCenterModelFromSysMLModel(rootModelCenterModelElement, false);
	}
	
	/**
	 * Synchronizes a pre-loaded ModelCenter model with the SysML Model
	 * 
	 * @param rootModelCenterModelNode
	 * @throws ModelCenterException
	 * @throws ModelCenterProfileNotLoaded 
	 */
	public void updateSysMLModelFromModelCenterModel(Node rootModelCenterModelNode) throws ModelCenterException, ModelCenterProfileNotLoaded {
		// Prerequisite is that model is already loaded
		if(!ModelCenterPlugin.getModelCenterInstance().getModelFileName().equals("")) {
			// Go through ModelCenter model and compare with SysML model
			Assembly rootModel = ModelCenterPlugin.getModelCenterInstance().getModel();
			
			// Make sure we can edit the model
			ModelCenterPlugin.ensureMDSessionIsActive();
			
			// Rename SysML representation
			((NamedElement)rootModelCenterModelNode.getUserObject()).setName(rootModel.getName());
			
			try {
				for(int i=0; i<rootModel.getNumVariables(); i++) {
					Variable nextVarFromMC = rootModel.getVariable(i);
					
					// TODO: Update properties of already found variables (e.g. multiplicity)

					// Handle variables
					if(!findVariable(nextVarFromMC, rootModelCenterModelNode)) {
						// Variable was not found, create it
						// TODO: Input / output vars? How do we handle the differentiation?
						Port newVariable = ModelCenterPlugin.getMDModelFactoryInstance().createInputVariable(nextVarFromMC.getName());
						
						// TODO: Somehow convert type double to SysML equivalent, e.g. Real or so? Is this necessary?
						
						ModelElementsManager.getInstance().addElement(newVariable, (Element)rootModelCenterModelNode.getUserObject());
					}
				}
			}
			catch(ModelCenterException e) {
				// Something went wrong while traversing through the ModelCenter model
				throw e;
			}
			catch (ReadOnlyElementException e) {
				// Tried to add an element to a read only element
				e.printStackTrace();
			}
			finally {
				// Close the session, even if an exception was thrown
				ModelCenterPlugin.closeMDSession();
			}
			
			// Go through assemblies
			
			// Go through components and find variables & assemblies
			
			//// Add any elements that are missing (up to a specific level)
			//// If there is stuff in the modelcenter model that cannot be found in the sysml model, delete it, but ask user first!!!
		}
	}
	
	/**
	 * 
	 */
	public void restoreOriginalData() {
		if(filesToRestore_ == null)
			return;
		
		for(Iterator<String> iter = filesToRestore_.iterator(); iter.hasNext(); ) {
			String origFile = iter.next();
			
			// Delete the modified file
			File toDel = new File(origFile.replace(".orig.pxc", ""));
			toDel.delete();
			
			// Now rename the original file
			File toRename = new File(origFile);
			toRename.renameTo(new File(origFile.replace(".orig.pxc", "")));
		}
		
		filesToRestore_.clear();
	}
	
	/**
	 * Create links between a specific top level model and its sub models. Note that links from the "outside"
	 * are created later on
	 * 
	 * @param topLevelModel
	 * @throws ModelCenterException
	 */
	private void updateLinksToAndBetweenSubModels(Element topLevelModel) throws ModelCenterException {
		// Go through owned elements
		for(Iterator<Element> iter = topLevelModel.getOwnedElement().iterator(); iter.hasNext(); ) {
			Element nextElement = iter.next();
			
			// Check whether we found a port
			if(nextElement instanceof Port && ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterVariable(nextElement)) {
				// Get the connectors for our ModelCenter model for which partWithPort is null (this is the case for internal connections!)
				ArrayList<Connector> connectors = getConnectorHandler().getConnectorsForElementAndPartWithPort(nextElement, null);
				
				// Iterate through all connectors of this port
				for(Iterator<Connector> conIter = connectors.iterator(); conIter.hasNext(); ) {
					Connector nextConnector = conIter.next();
					
					ConnectorEnd rootModelPort = nextConnector.getEnd().get(0);
					ConnectorEnd subModelPort = nextConnector.getEnd().get(1);
					
					// Make sure we have the right elements in the right variables
					if(rootModelPort.getPartWithPort() != null) {
						rootModelPort = subModelPort;
						subModelPort = nextConnector.getEnd().get(0);
					}
					
					// Construct the path as in the ModelCenter model from the top level model to the sub model's
					// variable
					String modelCenterPathToInnerVariable = constructSubPath(topLevelModel, subModelPort.getPartWithPort());
					String subModelPortName = subModelPort.getRole().getName();
					
					if(subModelPortName.equals(""))
						subModelPortName = subModelPort.getRole().getType().getName();
					
					modelCenterPathToInnerVariable += "." + ModelCenterPlugin.toModelCenterSafeName(subModelPortName);
					
					while(!modelCenterPathToInnerVariable.equals("") && modelCenterPathToInnerVariable.startsWith("."))
						modelCenterPathToInnerVariable = modelCenterPathToInnerVariable.substring(modelCenterPathToInnerVariable.indexOf(".") + 1);
					
					System.out.println("Found path to be: " + modelCenterPathToInnerVariable);
					
					String modelPrefix = ModelCenterPlugin.getModelCenterInstance().getModel().getName() + ".";
					
					// Now connect the two variables using a link
					try {
						if(ModelCenterPlugin.getModelCenterInstance().getVariable(modelPrefix + modelCenterPathToInnerVariable).isInputToModel())
							ModelCenterPlugin.getModelCenterInstance().createLink(modelPrefix + modelCenterPathToInnerVariable, modelPrefix + ModelCenterPlugin.toModelCenterSafeName(rootModelPort.getRole().getName()));
						else
							ModelCenterPlugin.getModelCenterInstance().createLink(modelPrefix + ModelCenterPlugin.toModelCenterSafeName(rootModelPort.getRole().getName()), modelPrefix + modelCenterPathToInnerVariable);
					}
					catch(ModelCenterException e) {
						// Don't worry if we end up here: it may be a double connector
						e.printStackTrace();
					}
				}
			}
			else if(nextElement instanceof Property && ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(((Property)nextElement).getType())) {
				// Update links between sub models (where partWithPort is no longer null) - recursively
				updateLinksBetweenSubModels(((Property)nextElement).getType(), topLevelModel);
			}
		}
	}
	
	/**
	 * 
	 * @param element
	 * @throws ModelCenterException 
	 */
	private void updateLinksBetweenSubModels(Element element, Element topLevelModel) throws ModelCenterException {
		// Go through owned elements
		for(Iterator<Element> iter = element.getOwnedElement().iterator(); iter.hasNext(); ) {
			Element nextElement = iter.next();
			
			// Check whether we found a port
			if(nextElement instanceof Port && ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterVariable(nextElement)) {
				// Get the connectors for our ModelCenter model for which partWithPort is null (this is the case for internal connections!)
				ArrayList<Connector> connectors = getConnectorHandler().getConnectorsForElement(nextElement);
				
				// Iterate through all connectors of this port
				for(Iterator<Connector> conIter = connectors.iterator(); conIter.hasNext(); ) {
					Connector nextConnector = conIter.next();
					
					ConnectorEnd rootModelPort = nextConnector.getEnd().get(0);
					ConnectorEnd subModelPort = nextConnector.getEnd().get(1);
					
					if(rootModelPort.getPartWithPort() != null && subModelPort.getPartWithPort() != null) {
						// Construct the path as in the ModelCenter model from the top level model to the sub model's
						// variable
						String pathToFirstVariable = constructSubPath(topLevelModel, subModelPort.getPartWithPort());
						String pathToSecondVariable = constructSubPath(topLevelModel, rootModelPort.getPartWithPort());
						System.out.println("Initial paths: " + pathToFirstVariable + " and " + pathToSecondVariable);
						
						if(!pathToFirstVariable.equals("") && !pathToSecondVariable.equals("")) {
							pathToFirstVariable += "." + ModelCenterPlugin.toModelCenterSafeName(subModelPort.getRole().getName());
							System.out.println("Found first path to be: " + pathToFirstVariable);
							
							pathToSecondVariable += "." + ModelCenterPlugin.toModelCenterSafeName(rootModelPort.getRole().getName());
							System.out.println("Found second path to be: " + pathToSecondVariable);
							
							// If a path from the outermost model to the target variable was not found, dont create the link
							// (this prevents links to be created that go outside the bounds of the top level model of the
							// nested assembly)
							
							String modelPrefix = ModelCenterPlugin.getModelCenterInstance().getModel().getName() + ".";
							
							// Now connect the two variables using a link
							try {
								if(ModelCenterPlugin.getModelCenterInstance().getVariable(modelPrefix + pathToFirstVariable).isInputToModel())
									ModelCenterPlugin.getModelCenterInstance().createLink(modelPrefix + pathToFirstVariable, modelPrefix + pathToSecondVariable);
								else
									ModelCenterPlugin.getModelCenterInstance().createLink(modelPrefix + pathToSecondVariable, modelPrefix + pathToFirstVariable);
							}
							catch(ModelCenterException e) {
								// Linking error - most likely a duplicate link
								if(!e.getMessage().toLowerCase().contains("is already linked")) {
									// If not, print the stack trace
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
			else if(nextElement instanceof Property && ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(((Property)nextElement).getType())) {
				// Update links between sub models (where partWithPort is no longer null) - recursively
				updateLinksBetweenSubModels(((Property)nextElement).getType(), topLevelModel);
			}
		}
	}
	
	/**
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	private String constructSubPath(Element from, Property to) {
		String path = "";
		
		for(Iterator<Element> iter = from.getOwnedElement().iterator(); iter.hasNext(); ) {
			Element nextElement = iter.next();
			
			// Check whether we found a reference to a ModelCenter model
			if(nextElement instanceof Property && ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(((Property)nextElement).getType()) && elementIsWithinReach(nextElement, to)) {
				// Get the name of the element as it would appear in ModelCenter
				String name = ((Property)nextElement).getName();
				
				if(name.equals(""))
					name = ((Property)nextElement).getType().getName();
				
				// Make sure we have a modelcenter safe name (i.e. does not contain special characters)
				name = ModelCenterPlugin.toModelCenterSafeName(name);
				
				// Check whether we have reached our target element
				if((Property)nextElement == to) {
					return path + name;			// Already has a "." at the end
				}
				else {
					// If not, construct the sub path recursively
					path += name + "." + constructSubPath(((Property)nextElement).getType(), to);
				}
			}
		}
		
		return path;
	}
	
	/**
	 * Check whether a given element (targetElement) is within reach from the currentElement
	 * 
	 * @param currentElement
	 * @param target
	 * @return
	 */
	private boolean elementIsWithinReach(Element currentElement, Property target) {
		boolean withinReach = false;
		
		if(currentElement == target)
			withinReach = true;
		else {
			for(Iterator<Element> iter = currentElement.getOwnedElement().iterator(); iter.hasNext(); ) {
				Element nextElement = iter.next();
				
				if(nextElement instanceof Property && ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(((Property)nextElement).getType())) {
					withinReach = elementIsWithinReach(nextElement, target);
					
					if(withinReach == true) {
						break;
					}
				}
			}
		}
		
		return withinReach;
	}
	
	/**
	 * Check whether a specified variable as defined in SysML has a corresponding variable in ModelCenter.
	 * The decision whether or not the two elements are equal is based on the name and, of
	 * course, the hierachy level (since only one level is considered). Note that direction is
	 * not included in these criteria, since variables only become output variables once they are
	 * linked that way!
	 * 
	 * @param variable
	 * @param currentAssembly
	 * @return
	 * @throws ModelCenterException
	 */
	private boolean findVariable(Port variable, Assembly currentAssembly) throws ModelCenterException {
		// Check whether this variable exists in the ModelCenter model
		for(int i=0; i<currentAssembly.getNumVariables(); i++) {
			Variable nextVarFromMC = currentAssembly.getVariable(i);
			
			if(nextVarFromMC.getName().equals(variable.getName())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Check whether a given ModelCenter variable exists as a child of a specific SysML block.
	 * The decision whether or not the two elements are equal is based on the name and, of
	 * course, the hierachy level (since only one level is considered). Note that direction is
	 * not included in these criteria, since variables only become output variables once they are
	 * linked that way!
	 * 
	 * @param var
	 * @param nodeToConsider
	 * @return
	 * @throws ModelCenterException
	 */
	private boolean findVariable(Variable var, Node nodeToConsider) throws ModelCenterException {
		for(int j=0; j<nodeToConsider.getChildCount(); j++) {
			Element el = (Element)((Node)nodeToConsider.getChildAt(j)).getUserObject();
			
			if(el instanceof Port && var.getName().equals(((Port)el).getName())) {
				return true;
			}
		}
	
		// Couldn't find it
		return false;
	}
	
	/**
	 * 
	 * @param script
	 * @param currentAssembly
	 * @return
	 * @throws ModelCenterException
	 */
	private boolean findScriptComponent(Constraint script, Assembly currentAssembly) throws ModelCenterException {
		// Check whether this variable exists in the ModelCenter model
		for(int i=0; i<currentAssembly.getNumComponents(); i++) {
			Component nextVarFromMC = currentAssembly.getComponent(i);
			
			if(nextVarFromMC.getName().equals(script.getName())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param script
	 * @param currentAssembly
	 * @return
	 * @throws ModelCenterException
	 */
	private void removeGeneratedScriptComponent(Constraint script, Assembly currentAssembly) throws ModelCenterException {
		// TODO: if the name changes, this wont work anymore - what to do then?
		// One possible solution: add a tag "generated_" to the script to identify it, since we cannot access
		// the script body
		// Another: access the script body from the XML file
		ModelCenterPlugin.getModelCenterInstance().removeComponent(currentAssembly.getFullName() + "." + script.getName());
	}
	
	/**
	 * Returns the variable type (e.g. double, string or double[]) as used in ModelCenter for a given
	 * SysML port
	 * 
	 * @param variable
	 * @return
	 */
	public static String getModelCenterTypeForSysMLType(Port variable) {
		String multiplicity = ModelHelper.getMultiplicity(variable);
		String type = "";
		String typeModifier = "";
		
		if(variable.getType() != null)
			type = variable.getType().getName();

		// Check whether there is multiplicity involved
		if(multiplicity != null && !multiplicity.equals("")) {
			// If multiplicity ends with a *, i.e. 0..* or 1..* or even 4..* then mark as array
			if(multiplicity.endsWith("*")) {
				typeModifier = "[]";
			}
			else {
				// If this was not the case, it may still be that a number was specified for the
				// multiplicity, e.g. "4" or "3" - hence check whether the string contains only numbers
				if(!multiplicity.matches("^[a-zA-Z]+$")) {
					// Then parse the string as integer
					int fixedMultiplicity = Integer.parseInt(multiplicity);
					
					// Check how large the numerical value is
					if(fixedMultiplicity > 1)
						typeModifier = "[]";
				}
			}
		}
		
		// Now consider the type
		if(type.equals("Real") || type.equals("double") || type.equals("$OCL_Real") || type.equals("float"))
			return "double" + typeModifier;
		else if(type.equals("Integer") || type.equals("integer") || type.equals("$OCL_Integer") || type.equals("int"))
			return "integer" + typeModifier;
		else if(type.equals("String") || type.equals("string") || type.equals("$OCL_String"))
			return "string" + typeModifier;
		else if(type.equals("Boolean") || type.equals("boolean") || type.equals("$OCL_Boolean") || type.equals("bool"))
			return "boolean" + typeModifier;
		
		// Default: assume double (e.g. if physical units are used)
		return "double" + typeModifier;
	}
	
	/**
	 * Returns the variable type (e.g. double, string or double[]) as used in ModelCenter for a given
	 * SysML port
	 * 
	 * @param variable
	 * @return
	 * @throws ModelCenterException 
	 */
	public static Type getSysMLTypeForModelCenterType(Variable variable, Type currentType) throws ModelCenterException {
		// Name of type
		String modelCenterTypeName = variable.getType().replace("[]", "");
		
		// [] indicates whether multiple or not - read dimensions? Only if not yet set
		boolean isMultiple = variable.getType().endsWith("[]");
		
		// First handle type itself
		if(modelCenterTypeName.equals("int")) {
			// Will become SysML's Integer
		}
		else if(modelCenterTypeName.equals("double")) {
			// Will become SysML's Real
		}
		else if(modelCenterTypeName.equals("string")) {
			// Will become SysML's / UML's String
		}
		else if(modelCenterTypeName.equals("boolean")) {
			// Will become SysML's Boolean
		}
		
		// Return modified type
		return currentType;
	}
	
	/**
	 * 
	 * @param variableType
	 * @return
	 */
	public static Variant getInitialValueForModelCenterType(String variableType) {
		Variant value = null;
		
		if(variableType.equals("string"))
			value = new Variant(Variant.STRING, "");
		else if(variableType.equals("string[]"))
			value = new Variant(Variant.STRING_ARRAY, new String[1]);
		else if(variableType.equals("double"))
			value = new Variant(Variant.DOUBLE, 0);
		else if(variableType.equals("double[]"))
			value = new Variant(Variant.DOUBLE_ARRAY, new Double[1]);
		else if(variableType.equals("boolean"))
			value = new Variant(Variant.BOOLEAN, false);
		else if(variableType.equals("boolean[]"))
			value = new Variant(Variant.BOOLEAN_ARRAY, new Boolean[1]);
		else if(variableType.equals("int"))
			value = new Variant(Variant.INT, 0);
		else if(variableType.equals("int[]"))
			value = new Variant(Variant.INT_ARRAY, new Integer[1]);
		else if(variableType.equals("long"))
			value = new Variant(Variant.LONG, 0);
		else if(variableType.equals("long[]"))
			value = new Variant(Variant.LONG_ARRAY, new Long[1]);
		
		return value;
	}
	
	/**
	 * 
	 * @param fileToOpen
	 * @throws FailedToLaunchModelCenter 
	 */
	public void launchModelCenter(String fileToOpen) throws FailedToLaunchModelCenter {
		String modelCenterExecutable = WindowsRegistry.readRegistry("HKLM\\SOFTWARE\\Wow6432Node\\Phoenix Integration\\ModelCenter", "CurrentInstallLocation");
		
		// 32 bit location
		if(modelCenterExecutable == null || modelCenterExecutable.equals("")) {
			modelCenterExecutable = WindowsRegistry.readRegistry("HKLM\\SOFTWARE\\Phoenix Integration\\ModelCenter", "CurrentInstallLocation");
		}
		
		if(modelCenterExecutable != null && !modelCenterExecutable.endsWith("\\")) {
			modelCenterExecutable += "\\";
		}
		
		// Works, at least for version 9 and 10
		modelCenterExecutable += "ModelCenter.exe";
		
		// Launch ModelCenter
		Runtime rt = Runtime.getRuntime();
		String cmd = "\"" + alternativeModelCenterLaunchPath + "\" \"" + fileToOpen + "\"";
		
		if(alternativeModelCenterLaunchPath.equals(""))
			cmd = "\"" + modelCenterExecutable + "\" \"" + fileToOpen + "\""; 
		
		try {
			String s = "";
			
			Process p = rt.exec(cmd);
			
			// Read output streams (OS buffer may otherwise not be able to handle the output directly
			// since the buffer may be too small)
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			
			while((s = stdInput.readLine()) != null);
			while((s = stdError.readLine()) != null);
		}
		catch (IOException e1) {
			e1.printStackTrace();
			
			// Warn user
			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Failed to launch ModelCenter. Please select the ModelCenter executable in\nthe following dialog.", "ModelCenter Plug-In", JOptionPane.ERROR_MESSAGE);
			
			final JFileChooser fc = new JFileChooser();
			
			fc.setCurrentDirectory(new File(Application.getInstance().getProject().getDirectory()));
			fc.setDialogTitle("Select ModelCenter.exe");
			
			int opt = fc.showOpenDialog(MDDialogParentProvider.getProvider().getDialogParent());
			
			alternativeModelCenterLaunchPath = "";
			
			if(opt==JFileChooser.APPROVE_OPTION) {
				String selectedFile = fc.getSelectedFile().toString();
				
				if(!selectedFile.endsWith(".exe"))
					selectedFile = selectedFile + ".exe";
				
				alternativeModelCenterLaunchPath = selectedFile;
			}
			
			if(!alternativeModelCenterLaunchPath.equals("")) {	// User pressed cancel
				// Try again
				launchModelCenter(fileToOpen);
			}
			else {
				throw new FailedToLaunchModelCenter();
			}
		}
	}

	/**
	 * @return the connectorHandler
	 */
	public ConnectorHandler getConnectorHandler() {
		return connectorHandler_;
	}

	/**
	 * @param connectorHandler the connectorHandler to set
	 */
	public void setConnectorHandler(ConnectorHandler connectorHandler) {
		this.connectorHandler_ = connectorHandler;
	}
	
}
