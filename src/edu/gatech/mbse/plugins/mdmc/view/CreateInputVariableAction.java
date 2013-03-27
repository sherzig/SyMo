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
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;

import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;
import edu.gatech.mbse.plugins.mdmc.exceptions.ModelCenterProfileNotLoaded;

/**
 * @author Sebastian
 *
 */
public class CreateInputVariableAction extends CreateVariableAction {

	/**
	 * TODO: This has been changed temporarily to only represent a "variable" in general, not
	 * explicitly differentiating between input and output variables - is this a good idea? Only
	 * time will tell...
	 */
	public CreateInputVariableAction() {
		// Set the label as shown on the menu
		super("", "Variable", null, null);
		
		// Add an icon
		this.setSmallIcon(new ImageIcon(getClass().getResource("input.gif")));
	}

	@Override
	protected Port createVariable() throws ModelCenterProfileNotLoaded {
		return ModelCenterPlugin.getMDModelFactoryInstance().createInputVariable("");
	}
	
	/**
	 * 
	 */
	public void updateState() {
		
	}

}
