/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.view;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.magicdraw.uml.ModelElementFactory;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;

import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;
import edu.gatech.mbse.plugins.mdmc.exceptions.ModelCenterProfileNotLoaded;

/**
 * @author Sebastian
 *
 */
public class CreateDataModelAction extends CreateElementAction {

	/**
	 * Constructor
	 */
	public CreateDataModelAction() {
		super("", "Data Model", null, null);
		
		// Add an icon
		this.setSmallIcon(new ImageIcon(getClass().getResource("model.gif")));
	}
	
	/**
	 * Called whenever the creation of a new ModelCenter data model is requested
	 */
	public void actionPerformed(ActionEvent e) {
		// Ensure that session is locked
		ModelCenterPlugin.ensureMDSessionIsActive();
		
		// Create a new data model
		Class newDataModel;
		
		try {
			newDataModel = ModelCenterPlugin.getMDModelFactoryInstance().createDataModel("");
		}
		catch(ModelCenterProfileNotLoaded e2) {
			e2.printStackTrace();
			
			// Don't forget to close the session
			ModelCenterPlugin.closeMDSession();
			
			// Warn the user if the ModelCenter profile has not yet been loaded
			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Please load the ModelCenter profile first", "ModelCenter Plugin", JOptionPane.WARNING_MESSAGE);
			
			return;
		}
		
		try {
			// Add the newly created data model element to the model
			ModelElementsManager.getInstance().addElement(newDataModel, getSelectedElement());
		}
		catch(ReadOnlyElementException e1) {
			// TODO: Handle if element is read only
			e1.printStackTrace();
		}
		
		// Expand parent node
		expandSelectedNode();
		
		// Done editing model, close session
		ModelCenterPlugin.closeMDSession();
		
		// Enable user to enter a name for the newly created element
		triggerRenameModeForElement(newDataModel);
	}
	
	/**
	 * Called whenever the menu is re-created so that individual items can be disabled or updated
	 */
	public void updateState() {
		
	}

}
