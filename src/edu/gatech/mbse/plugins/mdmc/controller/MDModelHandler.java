/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Generalization;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceValue;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Slot;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;

/**
 * @author Sebastian
 *
 */
public class MDModelHandler {

	/**
	 * 
	 * @param element
	 * @param stereotype
	 * @param parameter
	 * @return
	 */
	private Object getFirstStereotypeParameterValue(Element element, String stereotype, String parameter) {
		Stereotype stereotypeOfInterest = StereotypesHelper.getStereotype(Application.getInstance().getProject(), stereotype);
		
		return StereotypesHelper.getStereotypePropertyFirst(element, stereotypeOfInterest, parameter);
	}
	
	/**
	 * 
	 * @param element
	 * @param stereotype
	 * @param parameter
	 * @return
	 */
	private void setStereotypeParameterValue(Element element, String stereotype, String parameter, Object value) {
		Stereotype stereotypeOfInterest = StereotypesHelper.getStereotype(Application.getInstance().getProject(), stereotype);
		
		StereotypesHelper.setStereotypePropertyValue(element, stereotypeOfInterest, parameter, value);
	}
	
	/**
	 * Returns true if the specified element is a ModelCenter data model
	 * 
	 * @param element
	 * @return
	 */
	public boolean isModelCenterDataModel(Element element) {
		if(element == null)
			return false;
		
		// If any of the stereotypes applied to this element is of type modelcenter data model, return true
		for(int i=0; i<StereotypesHelper.getStereotypes(element).size(); i++)
			if(StereotypesHelper.getStereotypes(element).get(i).getName().equals("ModelCenterDataModel"))
				return true;
		
		return false;
	}
	
	
	/**
	 * Returns the data model's filename
	 * 
	 * @param element
	 * @return
	 */
	public String getModelCenterDataModelFilename(Element element) {
		String filename = (String)getFirstStereotypeParameterValue(element, "ModelCenterDataModel", "filename");
		
		// If this property value has not yet been set, null will be returned by the above function
		if(filename == null || filename.equals(""))
			return "";
		
		// If the filename is relative to the current MagicDraw project, add the path
		if(!filename.contains(":")) {
			String projectDir = Application.getInstance().getProject().getDirectory();
			
			if(!projectDir.endsWith("\\"))
				projectDir = projectDir + "\\";
			
			// Add project path to filename
			filename = projectDir + filename;
		}
		
		return filename;
	}
	
	
	/**
	 * Sets the data model's filename
	 * 
	 * @param element
	 * @param filename
	 * @return
	 */
	public void setModelCenterDataModelFilename(Element element, String filename) {
		// If the filename is relative to the current MagicDraw project, just set the filename without a directory
		String projectDir = Application.getInstance().getProject().getDirectory();
		
		if(!projectDir.endsWith("\\"))
			projectDir = projectDir + "\\";
		
		// Remove project path from filename
		if(filename.startsWith(projectDir)) {
			filename = filename.replace(projectDir, "");
		}
		
		setStereotypeParameterValue(element, "ModelCenterDataModel", "filename", filename);
	}
	
	/**
	 * Returns true if the specified element is a ModelCenter variable
	 * 
	 * @param element
	 * @return
	 */
	public boolean isModelCenterVariable(Element element) {
		if(element == null)
			return false;
		
		// If any of the stereotypes applied to this element is of type modelcenter data model, return true
		for(int i=0; i<StereotypesHelper.getStereotypes(element).size(); i++)
			if(StereotypesHelper.getStereotypes(element).get(i).getName().equals("ModelCenterVariable"))
				return true;
		
		return false;
	}
	
	/**
	 * Returns true if the specified element is a ModelCenter input variable
	 * 
	 * @param element
	 * @return
	 */
	public boolean isModelCenterInputVariable(Element element) {
		// If any of the stereotypes applied to this element is of type modelcenter data model, return true
		if(isModelCenterVariable(element) && (Boolean)getFirstStereotypeParameterValue(element, "ModelCenterVariable", "isInput"))
			return true;
		
		return false;
	}
	
	/**
	 * Returns true if the specified element is a ModelCenter input variable
	 * 
	 * @param element
	 * @return
	 */
	public boolean isModelCenterOutputVariable(Element element) {
		// If any of the stereotypes applied to this element is of type modelcenter data model, return true
		if(isModelCenterVariable(element) && (Boolean)getFirstStereotypeParameterValue(element, "ModelCenterVariable", "isInput") == false)
			return true;
		
		return false;
	}
	
	/**
	 * 
	 * @param element
	 * @return
	 */
	public boolean containsVariable(Element modelCenterModel, Element element) {
		if(!isModelCenterVariable(element) || !isModelCenterDataModel(modelCenterModel))
			return false;
		
		if(modelCenterModel.getOwnedElement().contains(element))
			return true;
		
		return false;
	}
	
	/**
	 * 
	 * @param element
	 */
	public void setVariableDirectionToOutput(Element element) {
		setStereotypeParameterValue(element, "ModelCenterVariable", "isInput", false);
	}
	
	/**
	 * 
	 * @param element
	 */
	public void setVariableDirectionToInput(Element element) {
		setStereotypeParameterValue(element, "ModelCenterVariable", "isInput", true);
	}
	
	/**
	 * Returns true if the specified element is a ModelCenter data model
	 * 
	 * @param element
	 * @return
	 */
	public boolean isModelCenterScript(Element element) {
		if(element == null)
			return false;
		
		// If any of the stereotypes applied to this element is of type modelcenter data model, return true
		for(int i=0; i<StereotypesHelper.getStereotypes(element).size(); i++)
			if(StereotypesHelper.getStereotypes(element).get(i).getName().equals("ModelCenterScript"))
				return true;
		
		return false;
	}
	
	/**
	 * 
	 * @param instanceSpec
	 * @return
	 */
	public static HashSet<NamedElement> collectModelCenterModels(InstanceSpecification instanceSpec) {
		HashSet<NamedElement> modelCenterModels = new HashSet<NamedElement>();

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
						if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(((Property)nextElement).getType())) {
							modelCenterModels.add(((Property)nextElement).getType());
							
							modelCenterModels.addAll(collectSubModelCenterModels(((Property)nextElement).getType(), 0));
						}
					}
				}
			}
		}
		
		// Check for other instance values and extract any ModelCenter models that these may contain
		for(Iterator<Slot> iter = instanceSpec.getSlot().iterator(); iter.hasNext(); ) {
			Slot nextSlot = iter.next();
			
			for(Iterator<ValueSpecification> valueIter = nextSlot.getValue().iterator(); valueIter.hasNext(); ) {
				ValueSpecification nextValueSpec = valueIter.next();
				
				if(nextValueSpec instanceof InstanceValue) {
					modelCenterModels.addAll(collectModelCenterModels(((InstanceValue)nextValueSpec).getInstance()));
				}
			}
		}
		
		return modelCenterModels;
	}
	
	/**
	 * 
	 * @param instanceSpec
	 * @return
	 */
	public static ArrayList<Class> collectBlocksWithModelCenterModelUsages(InstanceSpecification instanceSpec) {
		ArrayList<Class> blocks = new ArrayList<Class>();

		// Go through element and see whether the sub-elements / properties are ModelCenter models
		List<Classifier> classifiers = findGeneralizations(instanceSpec.getClassifier());
			
		// Go through all classifiers
		for(int j=0; j<classifiers.size(); j++) {
			Classifier classifier = classifiers.get(j);
				
			// Search recursively
			if(classifier instanceof Class) {
				Element toParse = (Element)classifier;
				
				if(containsModelCenterModelTypedProperties(toParse)) {
					blocks.add((Class)classifier);
				}
			}
		}
		
		// Check for other instance values and extract any blocks with modelcenter model usages that these may contain
		for(Iterator<Slot> iter = instanceSpec.getSlot().iterator(); iter.hasNext(); ) {
			Slot nextSlot = iter.next();
			
			for(Iterator<ValueSpecification> valueIter = nextSlot.getValue().iterator(); valueIter.hasNext(); ) {
				ValueSpecification nextValueSpec = valueIter.next();
				
				if(nextValueSpec instanceof InstanceValue) {
					blocks.addAll(collectBlocksWithModelCenterModelUsages(((InstanceValue)nextValueSpec).getInstance()));
				}
			}
		}
		
		return blocks;
	}
	
	/**
	 * 
	 * @param toParse
	 * @return
	 */
	public static boolean containsModelCenterModelTypedProperties(Element toParse) {
		if(getModelCenterModelTypedProperties(toParse).size() > 0)
			return true;
		
		return false;
	}

	/**
	 * 
	 * @param rootElement
	 * @return
	 */
	public static ArrayList<Property> getModelCenterModelTypedProperties(Element rootElement) {
		ArrayList<Property> mcProperties = new ArrayList<Property>();
		
		for(Iterator<Element> elementIterator = rootElement.getOwnedElement().iterator(); elementIterator.hasNext(); ) {
			Element nextElement = elementIterator.next();
			
			// TODO: Structural references - need to search as well??
			if(nextElement instanceof Property) {
				if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(((Property)nextElement).getType())) {
					// Class contains a modelcenter model reference
					mcProperties.add((Property)nextElement);
				}
			}
		}
		
		return mcProperties;
	}
	
	/**
	 * Returns "usages" / nested modelcenter models within other modelcenter models
	 * 
	 * @param instanceSpec
	 * @return
	 */
	public static HashSet<NamedElement> collectSubModelCenterModels(NamedElement modelCenterModel, int level) {
		HashSet<NamedElement> modelCenterModels = new HashSet<NamedElement>();
		
		// Avoid deadlock due to cyclic definitions
		if(level > 20)
			return modelCenterModels;
		
		// Go through all classifiers
		for(Iterator<Element> iter = modelCenterModel.getOwnedElement().iterator(); iter.hasNext(); ) {
			Element subElement = iter.next();
				
			// Search recursively
			if(subElement != null && subElement instanceof Property) {
				if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(((Property)subElement).getType())) {
					modelCenterModels.add(((Property)subElement).getType());
					
					modelCenterModels.addAll(collectSubModelCenterModels(((Property)subElement).getType(), level+1));
				}
			}
		}
		
		return modelCenterModels;
	}
	
	/**
	 * Find generalizing elements by going through the generalization hierarchy of a given
	 * classifier
	 * 
	 * @param classifier
	 * @return
	 */
	public static List<Classifier> findGeneralizations(List<Classifier> classifiers) {
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
	public static List<Classifier> extractGeneralizationElements(Classifier classifier) {
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
	
}
