/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.view;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;
import com.phoenix_int.ModelCenter.ModelCenterException;

import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;
import edu.gatech.mbse.plugins.mdmc.exceptions.ModelCenterProfileNotLoaded;
import edu.gatech.mbse.plugins.mdmc.util.ModelCenterFileDialogHandler;

/**
 * @author Sebastian
 *
 */
public class ImportModelCenterModelAction extends DefaultBrowserAction {
	
	private ModelCenterFileDialogHandler fileDialogHandler_ = null;

	/**
	 * 
	 */
	public ImportModelCenterModelAction() {
		super("", "Import ModelCenter Model ...", null, null);
		
	}
	
	/**
	 * Import a ModelCenter model by creating a root element (representing the root model node)
	 * and synchronizing the rest
	 */
	public void actionPerformed(ActionEvent e) {
		// Show an open file dialog with a filter for pxc files
		String modelToImport = getModelCenterFileDialogHandler().showOpenDialog();
		
		// Parse the model and create elements as needed using the factory
		if(!modelToImport.equals("")) {
			try {
				Node currentNode = ((Node)getTree().getSelectedNode());
				
				// Load model
				ModelCenterPlugin.getModelCenterInstance().loadFile(modelToImport);
			
				// Create new session for changes
				ModelCenterPlugin.ensureMDSessionIsActive();
				
				// Create top level data model
				Class newDataModel = ModelCenterPlugin.getMDModelFactoryInstance().createDataModel(ModelCenterPlugin.getModelCenterInstance().getModel().getName());
				
				// Set the filename as property
				ModelCenterPlugin.getMDModelHandlerInstance().setModelCenterDataModelFilename(newDataModel, modelToImport);
				
				// Add to containment tree
				ModelElementsManager.getInstance().addElement(newDataModel, (Element)currentNode.getUserObject());
				
				// Synch elements so we can fetch the newly created node
				ModelCenterPlugin.closeMDSession();
				
				// Find the newly created element
				Node newNode = null;
				
				for(int i=0; i<currentNode.getChildCount(); i++)
					if(((Node)currentNode.getChildAt(i)).getUserObject() == newDataModel)
						newNode = ((Node)currentNode.getChildAt(i));
				
				if(newNode != null) {
					ModelCenterPlugin.ensureMDSessionIsActive();
					
					// Now synchronize data from ModelCenter file with SysML model
					ModelCenterPlugin.getSynchronizationEngineInstance().updateSysMLModelFromModelCenterModel(newNode);
				}
			}
			catch (ModelCenterException e1) {
				JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Failed to load ModelCenter model from file (file corrupt or version incompatible)", "ModelCenter Plugin - Critical Error", JOptionPane.ERROR_MESSAGE);
			}
			catch (ModelCenterProfileNotLoaded e1) {
				JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Please load the ModelCenter profile first", "ModelCenter Plugin", JOptionPane.WARNING_MESSAGE);
				e1.printStackTrace();
			}
			catch (ReadOnlyElementException e1) {
				// This should never happen
				e1.printStackTrace();
			}
			finally {
				// Close session even if exception was thrown
				ModelCenterPlugin.closeMDSession();
			}
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
	
}
