/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.phoenix_int.ModelCenter.Assembly;
import com.phoenix_int.ModelCenter.ModelCenterException;

import edu.gatech.mbse.plugins.mdmc.model.ModelCenterVariable;
import edu.gatech.mbse.plugins.mdmc.model.ScriptComponent;

/**
 * @author Sebastian
 *
 */
public class ModelCenterFileManipulator {
	
	private File pxcFile_ = null;
	private DocumentBuilderFactory docBuilderFactory_ = DocumentBuilderFactory.newInstance();
	private DocumentBuilder docBuilder_ = null;
	private Document document_ = null;
	private String tempAssemblyName_ = "___TEMP_ROOT_MCPLUGIN_TEMP___";
	private String modelCenterModelRootNodeName_ = "";
	private String tempScriptComponentName_ = "___TEMP_SCRIPT_COMPONENT_MCPLUGIN_TEMP___";

	/**
	 * Default constructor
	 */
	public ModelCenterFileManipulator() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Insert the root assembly (or "model") of a ModelCenter model into another ModelCenter model's
	 * hierarchy of elements
	 * 
	 * @param elementToInsert
	 * @param parentAssembly
	 * @throws ModelCenterException 
	 */
	public Assembly insertRootAssembly(Property elementToInsert, Assembly parentAssembly) throws ModelCenterException {
		String newModelFilename = Application.getInstance().getProject().getDirectory() + "\\" + ModelCenterPlugin.getModelCenterInstance().getModel().getName() + "__temp_0964_MCPLUGIN_Generated.pxc";
		String parentAssemblyFullName = parentAssembly.getFullName();
		
		// Insert a temporary assembly
		parentAssembly.addAssembly(tempAssemblyName_);
		
		// Save the current model
		ModelCenterPlugin.getModelCenterInstance().saveModelAs(newModelFilename);
		ModelCenterPlugin.getModelCenterInstance().closeModel();
		
		// Find the root assembly in the linked modelcenter file
		Node rootAssemblyNode = getRootAssemblyNode(ModelCenterPlugin.getMDModelHandlerInstance().getModelCenterDataModelFilename(elementToInsert.getType()));

		// Rename it if possible (i.e. if the name is not null)
		String newRootName = elementToInsert.getName();
		
		if(newRootName != null && !newRootName.equals("")) {
			rootAssemblyNode.getAttributes().getNamedItem("name").setNodeValue(newRootName);
			setModelCenterModelRootNodeName(newRootName);
		}
		
		// Insert this xml node into the original file
		insertAssemblyNodeIntoAssembly(rootAssemblyNode, newModelFilename);
		
		// Load old model
		ModelCenterPlugin.getModelCenterInstance().loadFile(newModelFilename);
		
		// Find the old assembly
		Assembly newAssembly = ModelCenterPlugin.getModelCenterInstance().getModel();
		
		if(!parentAssemblyFullName.equals(ModelCenterPlugin.getModelCenterInstance().getModel().getName()))
			newAssembly = findAssemblyRecursively(parentAssemblyFullName, ModelCenterPlugin.getModelCenterInstance().getModel());
		
		return newAssembly;
	}
	
	/**
	 * 
	 * @param script
	 * @param parentAssembly
	 * @throws ModelCenterException
	 */
	public Assembly insertScriptComponent(ScriptComponent script, Assembly parentAssembly) throws ModelCenterException {
		String newModelFilename = Application.getInstance().getProject().getDirectory() + "\\" + ModelCenterPlugin.getModelCenterInstance().getModel().getName() + "__temp_0964_MCPLUGIN_Generated.pxc";
		String parentAssemblyFullName = parentAssembly.getFullName();
		
		// Create an empty component inside the current ModelCenter model
		ModelCenterPlugin.getModelCenterInstance().createComponent("common:\\Functions\\Script", this.getTempScriptComponentName(), parentAssembly.getFullName());
		
		// Save the current model
		ModelCenterPlugin.getModelCenterInstance().saveModelAs(newModelFilename);
		ModelCenterPlugin.getModelCenterInstance().closeModel();
		
		// Check whether the script component to insert actually has a name (this shouldnt be allowed in the first place)
		if(script.getName().equals("") || script.getName() == null) {
			script.setName("Script");
		}
		
		createScriptComponent(script, newModelFilename);
		
		// Load old model
		ModelCenterPlugin.getModelCenterInstance().loadFile(newModelFilename);
		
		// Find the old assembly
		Assembly newAssembly = ModelCenterPlugin.getModelCenterInstance().getModel();
		
		if(!parentAssemblyFullName.equals(ModelCenterPlugin.getModelCenterInstance().getModel().getName()))
			newAssembly = findAssemblyRecursively(parentAssemblyFullName, ModelCenterPlugin.getModelCenterInstance().getModel());
		
		return newAssembly;
	}
	
	/**
	 * Creates an empty script component under a specific assembly
	 * 
	 * @param name The displayed name of the script component - needs to be ModelCenter safe
	 * @param assembly The assembly that owns this script component
	 * @throws ModelCenterException
	 */
	private void createScriptComponent(ScriptComponent script, String filename) throws ModelCenterException {
		// Load the XML file
		openAndParsePXCFile(filename);
		
		// Now create the script
		Node component = getFirstComponentByName(getTempScriptComponentName());
		
		// Set the script body
		setPropertyContent(component, "scriptSource", script.getScriptBody());
		
		// Now set the variables
		String scriptInputs = "";
		String scriptOutputs = "";
		
		// Build variable strings
		for(Iterator<ModelCenterVariable> iter = script.getVariables().iterator(); iter.hasNext(); ) {
			ModelCenterVariable nextVar = iter.next();
			
			// Create new variable element in XML document
			Element variable = getDocument().createElement("Variable");
			variable.setTextContent(nextVar.getValue().toString());		// TODO: double, string, etc.
			variable.setAttribute("name", nextVar.getName());
			
			// Set state and build string for component (property "scriptInputs/Outputs")
			if(nextVar.isInput()) {
				if(!scriptInputs.equals(""))
					scriptInputs += ",";
				
				scriptInputs += nextVar.getType() + " " + nextVar.getName();
				
				variable.setAttribute("state", "Input");
			}
			else {
				if(!scriptOutputs.equals(""))
					scriptOutputs += ",";
				
				scriptOutputs += nextVar.getType() + " " + nextVar.getName();
				
				variable.setAttribute("state", "Output");
			}
			
			// Set type
			variable.setAttribute("type", nextVar.getType());
			
			// Create properties
			Element properties = getDocument().createElement("Properties");
			createPropertiesFor(properties, nextVar);
			variable.appendChild(properties);
			
			// Add variable
			component.appendChild(variable);
		}
		
		// Now set the actual content
		setPropertyContent(component, "scriptInputs", scriptInputs);
		setPropertyContent(component, "scriptOutputs", scriptOutputs);
		
		component.getAttributes().getNamedItem("name").setNodeValue(script.getName());
		
		// Save the file
		savePXCFileAs(filename);
	}
	
	/**
	 * 
	 * @param properties
	 * @param nextVar
	 */
	private void createPropertiesFor(Node properties, ModelCenterVariable nextVar) {
		// TODO: if(nextVar.getType().equals("double")) etc
		addProperty(properties, "index", "-1");
		addProperty(properties, "description", "");
		addProperty(properties, "upperBound", "0");
		addProperty(properties, "hasUpperBound", "false");
		addProperty(properties, "lowerBound", "0");
		addProperty(properties, "hasLowerBound", "false");
		addProperty(properties, "units", "");
		addProperty(properties, "enumeratedValues", "");
		addProperty(properties, "enumeratedAliases", "");
		
		if(!nextVar.isInput())
			addProperty(properties, "validity", "false");
		else
			addProperty(properties, "validity", "true");
	}
	
	/**
	 * 
	 * @param properties
	 * @param nameAttributeValue
	 */
	private void addProperty(Node properties, String nameAttributeValue, String content) {
		Element property = getDocument().createElement("Property");
		property.setAttribute("name", nameAttributeValue);
		property.setTextContent(content);
		properties.appendChild(property);
	}
	
	/**
	 * 
	 * @param componentName
	 * @param assembly
	 * @return
	 */
	public Node getFirstComponentByName(String componentName) {
		if(getDocBuilder() != null && getDocument() != null) {
			NodeList components = getDocument().getElementsByTagName("Component");
			
			for(int i=0; i<components.getLength(); i++) {
				if(components.item(i).getAttributes().getNamedItem("name").getNodeValue().equals(componentName)) {
					return components.item(i);
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Set the content of a property. Note that a file should be open before calling this function
	 * 
	 * @param propertyName
	 * @param content
	 */
	public void setPropertyContent(Node topLevelNode, String propertyName, String content) {
		if(getDocBuilder() != null && getDocument() != null) {
			NodeList properties = getDocument().getElementsByTagName("Property");
			
			for(int i=0; i<properties.getLength(); i++) {
				if(properties.item(i).getParentNode().getParentNode() == topLevelNode) {
					if(properties.item(i).getAttributes().getNamedItem("name").getNodeValue().equals(propertyName)) {
						properties.item(i).setTextContent(content);
						
						return;
					}
				}
			}
		}
	}
	
	/**
	 * Find a specific assembly recursively by its relative full name
	 * 
	 * @param toSearchFor
	 * @param startingAssembly
	 * @return
	 * @throws ModelCenterException 
	 */
	private Assembly findAssemblyRecursively(String toSearchFor, Assembly startingAssembly) throws ModelCenterException {
		Assembly assembly = null;
		
		// Search for the assembly recursively
		for(int i=0; i<startingAssembly.getNumAssemblies(); i++) {
			if(startingAssembly.getAssembly(i).getFullName().equals(toSearchFor)) {
				// Found the assembly, return it
				return startingAssembly.getAssembly(i);
			}
			else if(toSearchFor.substring(0, toSearchFor.lastIndexOf(".")) != null
					&& !toSearchFor.substring(0, toSearchFor.lastIndexOf(".")).equals("") 
					&& toSearchFor.substring(0, toSearchFor.lastIndexOf(".")).startsWith(startingAssembly.getAssembly(i).getFullName())) {
				// We are on the right rack - explore this branch further
				return findAssemblyRecursively(toSearchFor, startingAssembly.getAssembly(i));
			}
		}
		
		return assembly;
	}
	
	/**
	 * 
	 * @param filename
	 * @return
	 */
	private Node getRootAssemblyNode(String filename) {
		openAndParsePXCFile(filename);
		
		if(getDocBuilder() != null && getDocument() != null) {
			// Sets the last used name for the model root name
			setModelCenterModelRootNodeName(getDocument().getElementsByTagName("Model").item(0).getFirstChild().getAttributes().getNamedItem("name").getNodeValue());
			
			return getDocument().getElementsByTagName("Model").item(0).getFirstChild();
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param filename
	 * @return
	 * @throws TransformerException 
	 * @throws FileNotFoundException 
	 */
	private void insertAssemblyNodeIntoAssembly(Node node, String filename) {
		openAndParsePXCFile(filename);
		
		if(getDocBuilder() != null && getDocument() != null) {
			// Get the pre-created assembly
			NodeList assemblies = getDocument().getElementsByTagName("Assembly");
			
			// Find the node with the "name" attribute ____TEMP_ROOT_...
			Node presetAssemblyNode = null;
			
			// Go through the assemblies and find the one we need
			for(int i=0; i<assemblies.getLength(); i++) {
				if(assemblies.item(i).getAttributes().getNamedItem("name").getNodeValue().equals(this.tempAssemblyName_)) {
					presetAssemblyNode = assemblies.item(i);
					
					break;
				}
			}
			
			// Make sure we found the node
			if(presetAssemblyNode != null) {
				// Insert the model after this element and remove the old element
				presetAssemblyNode.getParentNode().insertBefore(getDocument().importNode(node, true), presetAssemblyNode);
				presetAssemblyNode.getParentNode().removeChild(presetAssemblyNode);
				
				savePXCFileAs(filename);
			}
		}
	}
	
	/**
	 * 
	 * @param filename
	 */
	public void openAndParsePXCFile(String filename) {
		setPXCFile(new File(filename));
		
		try {
			setDocBuilder(getDocBuilderFactory().newDocumentBuilder());
			setDocument(getDocBuilder().parse(getPXCFile()));
		}
		catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// See if we were able to open and parse the given file
		if(getDocBuilder() != null && getDocument() != null) {
			getDocument().getDocumentElement().normalize();
		}
	}
	
	/**
	 * 
	 * @param filename
	 */
	public void savePXCFileAs(String filename) {
		// Save the changes
        StreamResult modifiedFile = null;
        
		try {
			modifiedFile = new StreamResult(new FileOutputStream(filename));
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		if(modifiedFile != null) {
            DOMSource source = new DOMSource(getDocument());
            
			try {
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.transform(source, modifiedFile);
			}
			catch (TransformerConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (TransformerFactoryConfigurationError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally {
				try {
					modifiedFile.getOutputStream().flush();
					modifiedFile.getOutputStream().close();
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * @return the pxcFile
	 */
	private File getPXCFile() {
		return pxcFile_;
	}

	/**
	 * @param pxcFile the pxcFile to set
	 */
	private void setPXCFile(File pxcFile) {
		this.pxcFile_ = pxcFile;
	}

	/**
	 * @return the docBuilderFactory
	 */
	private DocumentBuilderFactory getDocBuilderFactory() {
		setDocBuilderFactory(DocumentBuilderFactory.newInstance());
		
		return docBuilderFactory_;
	}

	/**
	 * @param docBuilderFactory the docBuilderFactory to set
	 */
	private void setDocBuilderFactory(DocumentBuilderFactory docBuilderFactory) {
		this.docBuilderFactory_ = docBuilderFactory;
	}

	/**
	 * @return the docBuilder
	 */
	private DocumentBuilder getDocBuilder() {
		return docBuilder_;
	}

	/**
	 * @param docBuilder the docBuilder to set
	 */
	private void setDocBuilder(DocumentBuilder docBuilder) {
		this.docBuilder_ = docBuilder;
	}

	/**
	 * @return the document
	 */
	private Document getDocument() {
		return document_;
	}

	/**
	 * @param document the document to set
	 */
	private void setDocument(Document document) {
		this.document_ = document;
	}

	/**
	 * @return the modelCenterModelRootNodeName
	 */
	public String getModelCenterModelRootNodeName() {
		return modelCenterModelRootNodeName_;
	}

	/**
	 * @param modelCenterModelRootNodeName the modelCenterModelRootNodeName to set
	 */
	public void setModelCenterModelRootNodeName(String modelCenterModelRootNodeName) {
		this.modelCenterModelRootNodeName_ = modelCenterModelRootNodeName;
	}

	/**
	 * @return the tempScriptComponentName
	 */
	public String getTempScriptComponentName() {
		return tempScriptComponentName_;
	}

	/**
	 * @param tempScriptComponentName the tempScriptComponentName to set
	 */
	public void setTempScriptComponentName(String tempScriptComponentName) {
		this.tempScriptComponentName_ = tempScriptComponentName;
	}

}
