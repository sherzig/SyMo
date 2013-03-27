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
public class CreateOutputVariableAction extends CreateVariableAction {

	/**
	 * 
	 */
	public CreateOutputVariableAction() {
		super("", "Output Variable", null, null);
		
		// Add an icon
		this.setSmallIcon(new ImageIcon(getClass().getResource("output.gif")));
	}
	

	@Override
	protected Port createVariable() throws ModelCenterProfileNotLoaded {
		return ModelCenterPlugin.getMDModelFactoryInstance().createOutputVariable("");
	}
	
	/**
	 * 
	 */
	public void updateState() {
		
	}

}
