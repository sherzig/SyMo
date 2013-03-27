/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.nomagic.magicdraw.ui.browser.ContainmentTree;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.OpaqueExpression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.impl.OpaqueExpressionImpl;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.Connector;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.ConnectorEnd;

/**
 * @author Sebastian
 *
 */
public class ConnectorHandler {

	private ArrayList<Connector> connectors_ = null;
	private Tree tree_ = null;
	
	/**
	 * 
	 */
	public ConnectorHandler() {
		this.connectors_ = new ArrayList<Connector>();
	}
	
	/**
	 * 
	 */
	public void rebuildConnectorsList() {
		// Remove all elements from the list
		clearConnectorList();
		
		// Find the connectors in the model and put them in a list
		findConnectors(((ContainmentTree)getTree()).getRootElement());
		
		System.out.println("Connectors list rebuilt: found " + getConnectors().size() + " items");
	}
	
	/**
	 * Clear the entire list of connectors - this function is commonly called before the list of
	 * connectors is re-built (before solve)
	 */
	public void clearConnectorList() {
		getConnectors().clear();
	}
	
	/**
	 * 
	 * @param node
	 */
	private void findConnectors(Element element) {
		// Go through sub nodes of current node
		for(Iterator<Element> elIter = element.getOwnedElement().iterator(); elIter.hasNext(); ) {
			Element subElement = elIter.next();
			
			// If the element connected to the current sub node is a classifier, check whether it has relations
			if(subElement instanceof Classifier) {
				Classifier currentClassifier = (Classifier)subElement;
				
				// Go through the relations connected to this classifier
				for(Iterator<NamedElement> iter=currentClassifier.getOwnedMember().iterator(); iter.hasNext(); ) {
					NamedElement el = iter.next();
					
					// Check whether the relation is a connector
					if(el instanceof Connector) {
						// Add it to the list
						getConnectors().add((Connector)el);
						
						System.out.println("Found a connector (1) at " + el.getQualifiedName());
					}
				}
			}
			
			// If the node has children, go through the children and call the function recursively
			if(subElement.getOwnedElement().size() > 0 && !(subElement instanceof Connector))
				findConnectors(subElement);
		}
	}
	
	/**
	 * 
	 * @param element
	 * @return
	 */
	public boolean elementIsConnectedToModelCenterVariable(Element element) {
		ArrayList<Connector> connectors = getConnectorsForElement(element);
		
		for(int i=0; i<connectors.size(); i++) {
			Connector con = connectors.get(i);
			List<ConnectorEnd> list = con.getEnd();
			ConnectorEnd connEnd1 = list.get(0);
			ConnectorEnd connEnd2 = list.get(1);
			
			if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterInputVariable(connEnd1.getRole())
					|| ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterInputVariable(connEnd2.getRole()))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Returns all connectors that have a given element at either side of the connector
	 * 
	 * @param element
	 * @return
	 */
	public ArrayList<Connector> getConnectorsForElement(Element element) {
		ArrayList<Connector> connectors = new ArrayList<Connector>();
		
		for(int i=0; i<getConnectors().size(); i++) {
			Connector con = getConnectors().get(i);
			List<ConnectorEnd> list = con.getEnd();
			ConnectorEnd connEnd1 = list.get(0);
			ConnectorEnd connEnd2 = list.get(1);
			
			if(connEnd1.getRole() == element || connEnd2.getRole() == element)
				connectors.add(con);
		}
		
		return connectors;
	}
	
	/**
	 * Returns all connectors that have a given part with port at either side of the connector
	 * 
	 * @param element
	 * @return
	 */
	public ArrayList<Connector> getConnectorsForElementAndPartWithPort(Element element, Property partWithPort) {
		ArrayList<Connector> connectors = new ArrayList<Connector>();
		
		for(int i=0; i<getConnectors().size(); i++) {
			Connector con = getConnectors().get(i);
			List<ConnectorEnd> list = con.getEnd();
			ConnectorEnd connEnd1 = list.get(0);
			ConnectorEnd connEnd2 = list.get(1);
			
			if((connEnd1.getPartWithPort() == partWithPort && connEnd1.getRole() == element) || (connEnd2.getPartWithPort() == partWithPort && connEnd2.getRole() == element))
				connectors.add(con);
		}
		
		return connectors;
	}
	
	/**
	 * Return the instance of the list of connectors
	 * 
	 * @return
	 */
	public ArrayList<Connector> getConnectors() {
		return this.connectors_;
	}

	/**
	 * 
	 * @return
	 */
	public Tree getTree() {
		return this.tree_;
	}
	
	/**
	 * 
	 * @return
	 */
	public void setTree(Tree tree) {
		this.tree_ = tree;
	}

}
