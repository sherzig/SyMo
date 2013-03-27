/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Generalization;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceValue;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Slot;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.TypedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;

import edu.gatech.mbse.plugins.mdmc.exceptions.DuplicateInstanceNameException;
import edu.gatech.mbse.plugins.mdmc.exceptions.DuplicateNamesException;
import edu.gatech.mbse.plugins.mdmc.exceptions.IncompatibleNames;
import edu.gatech.mbse.plugins.mdmc.exceptions.ReadPermissionNotGrantedException;
import edu.gatech.mbse.plugins.mdmc.exceptions.WriteAccessDeniedException;

/**
 * Class that checks the model prior to the transformation
 * 
 * @author Sebastian
 *
 */
public class PreTransformationChecker {
	
	private ArrayList<WarningItem> warnings_ = null;
	
	/**
	 * Default constructor
	 */
	public PreTransformationChecker() {
		this.setWarnings(new ArrayList<WarningItem>());
	}

	/**
	 * Check the instance for compatibility with ModelCenter
	 * 
	 * @throws IncompatibleNames 
	 * @throws DuplicateInstanceNameException 
	 */
	public void checkInstanceModel(InstanceSpecification selectedInstance) throws IncompatibleNames, DuplicateInstanceNameException {
		// Check whether instance names are compatible for the transformation
		checkInstanceNames(selectedInstance);
	}
	
	/**
	 * Check the modelcenter models that are being used
	 * 
	 * @param selectedInstance
	 * @throws ReadPermissionNotGrantedException 
	 * @throws FileNotFoundException 
	 */
	public void checkModelCenterModels(InstanceSpecification selectedInstance) throws IncompatibleNames, DuplicateNamesException, FileNotFoundException, ReadPermissionNotGrantedException {
		// Collect all ModelCenter models used in solving process
		Set<NamedElement> modelCenterModels = MDModelHandler.collectModelCenterModels(selectedInstance);
		
		// Go through the individual modelcenter models
		for(Iterator<NamedElement> iter = modelCenterModels.iterator(); iter.hasNext(); ) {
			NamedElement nextModel = iter.next();
			System.out.println("Checking " + nextModel.getQualifiedName());
			
			// Check name of model and names of owned elements
			checkNames(nextModel);
			
			// Check filename - does the file exist?
			checkFilename(nextModel);
			
			// TODO: Check connector ends - are these connected at all? Connected properly?
		}
		
		// Check any properties that use one of the modelcenter models as types
		ArrayList<Class> blocksWithModelCenterModelUsages = MDModelHandler.collectBlocksWithModelCenterModelUsages(selectedInstance);
		
		// Go through the individual modelcenter models
		for(Iterator<Class> iter = blocksWithModelCenterModelUsages.iterator(); iter.hasNext(); ) {
			NamedElement nextModel = iter.next();
			System.out.println("Checking block " + nextModel.getQualifiedName());
			
			for(Iterator<Property> propIter = MDModelHandler.getModelCenterModelTypedProperties(nextModel).iterator(); propIter.hasNext(); ) {
				Property nextMCModelUsage = propIter.next();
				
				System.out.println("-> Checking contained usage named \"" + nextMCModelUsage.getQualifiedName() + "\"");
				
				// Check name of model and names of owned elements
				checkNames(nextMCModelUsage);
			}
		}
	}
	
	/**
	 * Check whether we can create files in the project directory
	 * @throws WriteAccessDeniedException 
	 */
	public void checkWriteAccessInProjectDirectory() throws WriteAccessDeniedException {
		File projectDir = new File(Application.getInstance().getProject().getDirectory());
		
		// Can we write files?
		if(!projectDir.canWrite()) {
			// If not, currently just fail
			// TODO: Be more specific about directory
			throw new WriteAccessDeniedException("Write access to project directory is denied:\n" + projectDir.getAbsolutePath());
			
			// TODO: Mitigation strategy: change working directory internally to temp directory?
		}
	}
	
	/**
	 * Checks names of a modelcenter model and its owned elements
	 * 
	 * @param modelCenterModel
	 * @throws IncompatibleNames
	 */
	private void checkNames(NamedElement modelCenterModel) throws IncompatibleNames {
		// Check the name of the modelcenter model itself
		checkName(modelCenterModel);
		
		// Now check the names of the owned elements
		for(Iterator<Element> iter = modelCenterModel.getOwnedElement().iterator(); iter.hasNext(); ) {
			Element nextElement = iter.next();
			
			if(nextElement instanceof NamedElement) {
				// Hard failure - if variable names or either empty or 
				if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterVariable(nextElement)) {
					checkName((NamedElement)nextElement);
				}
				// "Soft" failure - can give script default name of "constraint" - assumes one script per model
				else if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterScript(nextElement)) {
					if(((NamedElement)nextElement).getName().equals("")) {
						ModelCenterPlugin.ensureMDSessionIsActive();
						
						// Automatic remedy - give the script a name
						((NamedElement)nextElement).setName("constraint");
						
						ModelCenterPlugin.closeMDSession();
						
						// Add warning to list of warnings
						this.getWarnings().add(new WarningItem(Severity.MC_SEVERITY_WARNING,
								"Scripts (constraints) must have a name",
								"The element in question was renamed to \"constraint\" automatically. However, this " +
								"can lead to potential errors if two or more scripts with the label \"constraint\" " +
								"are owned by the same ModelCenter constraint block.",
								(Class)modelCenterModel,	// Affected ModelCenter model / constraint block
								nextElement,				// Affected element 1
								null));						// Affected element 2
					}
					else
						checkName((NamedElement)nextElement);
				}
				// "Soft" failure: referenced modelcenter model is not named or ill-named - could lead to errors when
				// multiple models of the same type are used in the same context
				else if(nextElement instanceof TypedElement && ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(((TypedElement)nextElement).getType())) {
					try {
						checkName((NamedElement)nextElement);
					}
					catch(IncompatibleNames e) {
						if(e.getMessage().contains("must have a name")) {
							throw new IncompatibleNames(e.getMessage());
						}
						else {
							// Add warning
							this.getWarnings().add(new WarningItem(Severity.MC_SEVERITY_WARNING,
									"Element has no name",
									"Constraint properties in ModelCenter constraint blocks should have a name that " +
									"consists of characters A-Z, a-z, 0-9 and / or _ (underscore). While " +
									"not providing a name at all may not lead to any erroneous results / errors, it is " +
									"a potential source of error in cases where multiple unnamed constraint properties with " +
									"the same type are contained within the same ModelCenter constraint block.",
									(Class)modelCenterModel,	// Affected ModelCenter model / constraint block
									nextElement,				// Affected element 1
									null));						// Affected element 2
						}
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param modelCenterModel
	 * @throws IncompatibleNames 
	 */
	private void checkName(NamedElement element) throws IncompatibleNames {
		String name = element.getName();
		
		// Check whether the name would need adjusting
		if((name == null || name.equals("")) && element instanceof TypedElement && !(element instanceof Property))
			throw new IncompatibleNames("Element " + element.getQualifiedName() + " of type " + ((TypedElement)element).getType().getQualifiedName() + " must have a name!");
		
		// Properties typed with a modelcenter model contained in e.g. blocks
		if((name == null || name.equals("")) && element instanceof Property && ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(((TypedElement)element).getType())) {
			// Add warning
			this.getWarnings().add(new WarningItem(Severity.MC_SEVERITY_WARNING,
					"Element has no name",
					"Constraint properties contained in blocks should have a name that " +
					"consists of characters A-Z, a-z, 0-9 and / or _ (underscore). While " +
					"not providing a name at all may not lead to any erroneous results / errors, it is " +
					"a potential source of error in cases where multiple unnamed constraint properties with " +
					"the same type are contained within the same ModelCenter constraint block.",
					null,	// Affected ModelCenter model / constraint block
					element,				// Affected element 1
					null));						// Affected element 2
		}
		
		if(!ModelCenterPlugin.toModelCenterSafeName(name).equals(name))
			throw new IncompatibleNames("The name \"" + name + "\" for " + element.getQualifiedName() + "\nis incompatible with ModelCenter! Please use only characters A-Z, a-z, 0-9 and/or _");
	}
	
	/**
	 * 
	 * @param modelCenterModel
	 * @throws FileNotFoundException
	 * @throws ReadPermissionNotGrantedException 
	 */
	private void checkFilename(NamedElement modelCenterModel) throws FileNotFoundException, ReadPermissionNotGrantedException {
		String filename = ModelCenterPlugin.getMDModelHandlerInstance().getModelCenterDataModelFilename(modelCenterModel);
		
		// If no filename is specified, then return
		if(filename.equals(""))
			return;		// TODO: Check whether auto-generatable, i.e. contains script, if not throw warning
		else {
			// Check whether file exists
			File correspondingFile = new File(filename);
			
			// Check whether file exists
			if(!correspondingFile.exists()) {
				throw new FileNotFoundException("Could not find the .pxc file associated with " + modelCenterModel.getQualifiedName() + "! Please make sure that\nthe path specified in the element specification is correct.");
			}
			
			// Check whether we can access the file, i.e. check permissions
			if(!correspondingFile.canRead()) {
				throw new ReadPermissionNotGrantedException("Unable to read ModelCenter file associated with " + modelCenterModel.getQualifiedName() + ".\nThis is most likely a file permission error. Please restart MagicDraw as an administrator.");
			}
		}
	}
	
	/**
	 * 
	 * @throws IncompatibleNames
	 * @throws DuplicateInstanceNameException 
	 */
	private void checkInstanceNames(InstanceSpecification instanceSpec) throws IncompatibleNames, DuplicateInstanceNameException {
		// Check whether current instance name is compatible
		if(!ModelCenterPlugin.toModelCenterSafeName(instanceSpec.getName()).equals(instanceSpec.getName()))
			throw new IncompatibleNames("Instance named \"" + instanceSpec.getName() + "\" is not compatible with ModelCenter.\nPlease use only characters A-Z, a-z, 0-9 and _.");

		// Array that holds all instance names and that is used to check for double occurrences of names
		ArrayList<String> instanceNames = new ArrayList<String>();
		
		// Go through sub-slots to check whether they are compatible
		for(Iterator<Slot> iter = instanceSpec.getSlot().iterator(); iter.hasNext(); ) {
			Slot currentSlot = iter.next();
			
			// Go through the values of the slot
			for(Iterator<ValueSpecification> valIter = currentSlot.getValue().iterator(); valIter.hasNext(); ) {
				ValueSpecification currentVal = valIter.next();
				
				if(currentVal instanceof InstanceValue) {
					checkInstanceNames(((InstanceValue)currentVal).getInstance());
					
					String currentInstanceName = ((InstanceValue)currentVal).getInstance().getName();
					
					// Check whether we have a double naming - note that this is only done for slots currently
					// TODO: Expand to properties
					if(instanceNames.contains(currentInstanceName)) {
						throw new DuplicateInstanceNameException("Constructs with the same name at the same level are not supported\nby ModelCenter. Please rename the duplicates of \"" + currentInstanceName + "\".");
					}
					
					instanceNames.add(currentInstanceName);
				}
				else if(currentVal instanceof LiteralSpecification) {
					// In this case the defining feature of the slot is most likely a potential culprit
					String definingFeatureName = currentSlot.getDefiningFeature().getName();
					
					if(!ModelCenterPlugin.toModelCenterSafeName(definingFeatureName).equals(definingFeatureName))
						throw new IncompatibleNames("Property \"" + instanceSpec.getName() + "\" has a name that is incompatible with ModelCenter. Please use only characters A-Z, a-z, 0-9 and _.");
					
					// Don't worry about the other values
					break;
				}
			}
			
		}
	}

	/**
	 * @return the warnings
	 */
	public ArrayList<WarningItem> getWarnings() {
		return warnings_;
	}

	/**
	 * @param warnings the warnings to set
	 */
	public void setWarnings(ArrayList<WarningItem> warnings) {
		this.warnings_ = warnings;
	}
	
	// TODO: Check analysis model integrity: e.g. connectors and ports
	
}
