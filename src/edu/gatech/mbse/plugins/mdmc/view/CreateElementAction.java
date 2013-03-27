/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.view;

import java.awt.event.ActionEvent;

import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.TreePath;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.magicdraw.ui.browser.ContainmentTree;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.impl.ElementsFactory;

import edu.gatech.mbse.plugins.mdmc.controller.MDModelFactory;
import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;

/**
 * @author Sebastian
 *
 */
public abstract class CreateElementAction extends DefaultBrowserAction {
	
	/**
	 * Constructor
	 * 
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 */
	public CreateElementAction(String arg0, String arg1, KeyStroke arg2, String arg3) {
		super(arg0, arg1, arg2, arg3);
	}
	
	/**
	 * Return the first of the selected node from the containment tree
	 * 
	 * @return
	 */
	public int getSelectedNodeCount() {
		return getTree().getSelectedNodes().length;
	}

	/**
	 * Return the first of the selected node from the containment tree
	 * 
	 * @return
	 */
	public Node getSelectedNode() {
		return getTree().getSelectedNode();
	}
	
	/**
	 * Return the selected nodes from the containment tree
	 * 
	 * @return
	 */
	public Node[] getSelectedNodes() {
		return getTree().getSelectedNodes();
	}
	
	/**
	 * Get the selected element
	 * 
	 * @return
	 */
	public Element getSelectedElement() {
		return (Element)getSelectedNode().getUserObject();
	}
	
	/**
	 * Retrieve the user object for each of the selected nodes and return these
	 * as an array of elements
	 * 
	 * @return
	 */
	public Element[] getSelectedElements() {
		Element[] selectedElements = new Element[getSelectedNodeCount()];
		
		// Convert each node
		for(int i=0; i<selectedElements.length; i++)
			selectedElements[i] = (Element)getSelectedNodes()[i].getUserObject();
		
		return selectedElements;
	}
	
	/**
	 * Return the containment tree
	 * 
	 * @return
	 */
	public ContainmentTree getContainmentTree() {
		return ((ContainmentTree)getTree());
	}
	
	/**
	 * Expand the currently selected node
	 * 
	 */
	public void expandSelectedNode() {
		if(getSelectedNode() != null)
			getSelectedNode().expand();
	}
	
	/**
	 * 
	 * @param e
	 */
	public void triggerRenameModeForElement(Element e) {
		TreePath treePathToElement = getContainmentTree().findTreePathFor(e);
		
		// Find the newly created node
		getContainmentTree().getTree().scrollPathToVisible(treePathToElement);
		getContainmentTree().getTree().setSelectionPath(treePathToElement);
		
		// Now force the "rename" mode so that the user can conveniently enter the new name
		getContainmentTree().getTree().startEditingAtPath(treePathToElement);
		
		// TODO: Check whether name given collides with existing element, also upon rename!! (add a listener)
	}

}
