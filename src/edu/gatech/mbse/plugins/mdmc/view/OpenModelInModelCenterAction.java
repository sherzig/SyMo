/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.view;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.phoenix_int.ModelCenter.ModelCenterException;

import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;
import edu.gatech.mbse.plugins.mdmc.exceptions.ElementIsNotAModelCenterModel;
import edu.gatech.mbse.plugins.mdmc.exceptions.FailedToLaunchModelCenter;
import edu.gatech.mbse.plugins.mdmc.exceptions.ModelCenterProfileNotLoaded;
import edu.gatech.mbse.plugins.mdmc.exceptions.UserCanceledOperation;
import edu.gatech.mbse.plugins.mdmc.util.ModelCenterFileDialogHandler;
import edu.gatech.mbse.plugins.mdmc.util.WindowsRegistry;

/**
 * @author Sebastian
 *
 */
public class OpenModelInModelCenterAction extends DefaultBrowserAction {
	
	private ModelCenterFileDialogHandler fileDialogHandler_ = null;

	/**
	 * 
	 */
	public OpenModelInModelCenterAction() {
		super("", "Edit in ModelCenter", null, null);
		
	}
	
	/**
	 * 
	 */
	public void actionPerformed(ActionEvent e) {
		// Launch ModelCenter with the filename provided
		try {
			openModelInModelCenter();
		}
		catch(ElementIsNotAModelCenterModel e1) {
			// Do nothing - this should not happen anyway
		}
		catch(UserCanceledOperation e1) {
			// Do nothing - user simply canceled when creating a new file
		}
		catch(ModelCenterException e1) {
			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Error communicating with ModelCenter", "ModelCenter Plugin - Critical Error", JOptionPane.ERROR_MESSAGE);
		}
		catch(FailedToLaunchModelCenter e1) {
			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Failed to launch ModelCenter - please ensure that ModelCenter is installed", "ModelCenter Plugin", JOptionPane.WARNING_MESSAGE);
		}
		catch (ModelCenterProfileNotLoaded e1) {
			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "The ModelCenter profile no longer seems to be loaded - failed to synchronize changes\nfrom ModelCenter model. Load the profile and synchronize manually", "ModelCenter Plugin - Critical Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * 
	 * @throws ElementIsNotAModelCenterModel
	 * @throws UserCanceledOperation
	 * @throws ModelCenterException
	 * @throws FailedToLaunchModelCenter
	 * @throws ModelCenterProfileNotLoaded 
	 */
	private void openModelInModelCenter() throws ElementIsNotAModelCenterModel, UserCanceledOperation, ModelCenterException, FailedToLaunchModelCenter, ModelCenterProfileNotLoaded {
		// Get the selected element
		Node modelNode = (Node)getTree().getSelectedNode();
		Element model = (Element)modelNode.getUserObject();
		
		// Check whether it truly is a model center data model
		if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(model)) {
			// Check whether element selected has a filename specified
			String filename = ModelCenterPlugin.getMDModelHandlerInstance().getModelCenterDataModelFilename(model);
			
			if(filename.equals("")) {
				// If not, either ask user or default to a name (in which case you have to check whether file exists)
				filename = createNewModelCenterFile(((Class)model).getName());
				
				ModelCenterPlugin.ensureMDSessionIsActive();
				
				ModelCenterPlugin.getMDModelHandlerInstance().setModelCenterDataModelFilename(model, filename);
				
				ModelCenterPlugin.closeMDSession();
			}
			else {
				// Check to see whether ModelCenter file exists on file system
				File mcFile = new File(filename);
				
				if(!mcFile.exists())
					initializeNewModelCenterFile(filename, ((Class)model).getName());
			}
			
			// Load model
			try {
				ModelCenterPlugin.getModelCenterInstance().loadModel(filename);
			}
			catch (ModelCenterException e) {
				e.printStackTrace();
				
				JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Failed to open associated ModelCenter model at specified filename", "ModelCenter Plugin", JOptionPane.ERROR_MESSAGE);
				
				Application.getInstance().getGUILog().log(e.getMessage());
				
				throw new UserCanceledOperation();
			}
			
			// Synchronize model
			ModelCenterPlugin.getSynchronizationEngineInstance().updateModelCenterModelFromSysMLModel(model);
			
			// Save any changes made to the model
			ModelCenterPlugin.getModelCenterInstance().saveModel();
			
			// Launch ModelCenter with new file as argument
			// Note that MagicDraw will "hand" for as long as the user is updating the model - not sure whether this is good!
			ModelCenterPlugin.getSynchronizationEngineInstance().launchModelCenter(filename);
			
			// Reload model internally to see changes
			ModelCenterPlugin.getModelCenterInstance().loadModel(filename);
			
			// Synchronize changes made in model with SysML model
			ModelCenterPlugin.getSynchronizationEngineInstance().updateSysMLModelFromModelCenterModel(modelNode);
		}
		else {
			throw new ElementIsNotAModelCenterModel();
		}
	}
	
	/**
	 * 
	 * @return
	 * @throws UserCanceledOperation 
	 */
	private String createNewModelCenterFile(String suggestedName) throws UserCanceledOperation, ModelCenterException {
		// Ask user where he wants to save the new ModelCenter file
		String filename = getModelCenterFileDialogHandler().showSaveAsDialog(suggestedName);
		
		// User must have cancelled
		if(filename.equals(""))
			throw new UserCanceledOperation();
		
		initializeNewModelCenterFile(filename, suggestedName);
		
		return filename;
	}
	
	/**
	 * 
	 * @param filename
	 * @param suggestedName
	 * @throws ModelCenterException
	 */
	private void initializeNewModelCenterFile(String filename, String suggestedName) throws ModelCenterException {
		// Create new ModelCenter model
		ModelCenterPlugin.getModelCenterInstance().newModel();
		ModelCenterPlugin.getModelCenterInstance().getModel().rename(suggestedName);
		
		// Save the newly created model
		ModelCenterPlugin.getModelCenterInstance().saveModelAs(filename);
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
