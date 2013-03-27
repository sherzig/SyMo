/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.view;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;
import com.phoenix_int.ModelCenter.ModelCenterException;

import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;
import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterValuesImporter;
import edu.gatech.mbse.plugins.mdmc.controller.TransformationEngine;
import edu.gatech.mbse.plugins.mdmc.exceptions.ModelCenterProfileNotLoaded;
import edu.gatech.mbse.plugins.mdmc.util.ModelCenterFileDialogHandler;

/**
 * @author Sebastian
 *
 */
public class ImportValuesFromModelCenterModelAction extends MDAction {
	
	private ModelCenterFileDialogHandler fileDialogHandler_ = null;
	private InstanceSpecification selectedInstance_ = null;

	/**
	 * 
	 */
	public ImportValuesFromModelCenterModelAction() {
		super("", "Import Values from ModelCenter Model ...", null, null);
	}
	
	/**
	 * 
	 */
	public void actionPerformed(ActionEvent e) {
		// Show an open file dialog with a filter for pxc files
		String modelToImport = getModelCenterFileDialogHandler().showOpenDialog();
		
		// Parse the model and create elements as needed using the factory
		if(!modelToImport.equals("")) {
			// Create a new transformation engine object
			TransformationEngine trafoEngine = new TransformationEngine();
			
			ModelCenterPlugin.ensureMDSessionIsActive();
			
			try {
				// Import values from the modelcenter model
				trafoEngine.importValuesFromModelCenterModel(getSelectedInstance(), modelToImport);
			}
			catch(ModelCenterException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			ModelCenterPlugin.closeMDSession();
		}
	}
	
	/**
	 * 
	 */
	public void updateState() {
		
	}

	/**
	 * 
	 * @return
	 */
	private ModelCenterFileDialogHandler getModelCenterFileDialogHandler() {
		if(this.fileDialogHandler_ == null)
			this.fileDialogHandler_ = new ModelCenterFileDialogHandler();
		
		return this.fileDialogHandler_;
	}
	
	/**
	 * Returns the selected instance
	 * 
	 * @return the selectedInstance
	 */
	public InstanceSpecification getSelectedInstance() {
		return selectedInstance_;
	}

	/**
	 * Sets the selected instance
	 * 
	 * @param selectedInstance the selectedInstance to set
	 */
	public void setSelectedInstance(InstanceSpecification selectedInstance) {
		this.selectedInstance_ = selectedInstance;
	}
	
}
