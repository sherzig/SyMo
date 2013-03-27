/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.view;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;

import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;
import edu.gatech.mbse.plugins.mdmc.exceptions.ModelCenterProfileNotLoaded;

/**
 * @author Sebastian
 *
 */
public class CreateScriptAction extends CreateElementAction {


	/**
	 * Constructor
	 * 
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 */
	public CreateScriptAction() {
		super("", "Script", null, null);
		
		// Add an icon
		this.setSmallIcon(new ImageIcon(getClass().getResource("script.gif")));
	}

	/**
	 * 
	 */
	public void actionPerformed(ActionEvent e) {
		// Ensure that session is locked
		ModelCenterPlugin.ensureMDSessionIsActive();
		
		// Try to create a new variable
		Constraint newScript;
		
		try {
			newScript = ModelCenterPlugin.getMDModelFactoryInstance().createScript("");
		}
		catch(ModelCenterProfileNotLoaded e2) {
			e2.printStackTrace();
			
			// Don't forget to close the session
			ModelCenterPlugin.closeMDSession();
			
			// Warn the user if the ModelCenter profile has not yet been loaded
			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Please load ModelCenter profile first", "ModelCenter Plugin", JOptionPane.WARNING_MESSAGE);
			
			return;
		}
		
		try {
			// Set the constrained element
			newScript.getConstrainedElement().add(getSelectedElement());
			
			// Add the newly created data model element to the model
			ModelElementsManager.getInstance().addElement(newScript, getSelectedElement());
		}
		catch(ReadOnlyElementException e1) {
			// TODO: Handle if element is read only
			e1.printStackTrace();
		}
		
		// Done editing model, close session
		ModelCenterPlugin.closeMDSession();
		
		// Expand parent node
		expandSelectedNode();
		
		// Enable user to enter a name for the newly created element
		triggerRenameModeForElement(newScript);
	}
	
	/**
	 * 
	 */
	public void updateState() {
		
	}

}
