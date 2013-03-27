/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.view;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.phoenix_int.ModelCenter.ModelCenter;

import edu.gatech.mbse.plugins.mdmc.controller.InternalSolver;
import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;
import edu.gatech.mbse.plugins.mdmc.exceptions.DuplicateInstanceNameException;
import edu.gatech.mbse.plugins.mdmc.exceptions.DuplicateNamesException;
import edu.gatech.mbse.plugins.mdmc.exceptions.FailedToLaunchModelCenter;
import edu.gatech.mbse.plugins.mdmc.exceptions.IncompatibleNames;
import edu.gatech.mbse.plugins.mdmc.exceptions.ReadPermissionNotGrantedException;
import edu.gatech.mbse.plugins.mdmc.exceptions.WriteAccessDeniedException;
import edu.gatech.mbse.plugins.mdmc.util.ModelCenterFileDialogHandler;

/**
 * @author Sebastian
 *
 */
public class TransformInstanceToModelCenterModelAction extends MDAction {
	
	private InstanceSpecification selectedInstance_ = null;
	private ModelCenterFileDialogHandler fileDialogHandler_ = null;
	private String modelFilename;
	private int choice;

	/**
	 * 
	 */
	public TransformInstanceToModelCenterModelAction() {
		super("", "Transform Instance to ModelCenter Model", null, null);
		
		// Set icon
		this.setSmallIcon(new ImageIcon(getClass().getResource("transformation.gif")));
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// Check whether the selected instance was set (from the outside)
		if(getSelectedInstance() != null) {
			choice = JOptionPane.showConfirmDialog(MDDialogParentProvider.getProvider().getDialogParent(), "The transformation process may require the plugin to run the entire model once\nin order to size variables correctly.", "ModelCenter Plugin - Transformation", JOptionPane.OK_CANCEL_OPTION);
			
			if(choice == JOptionPane.OK_OPTION) {
				modelFilename = getModelCenterFileDialogHandler().showSaveAsDialog(getSelectedInstance().getName());
				
				// Parse the model and create elements as needed using the factory
				if(!modelFilename.equals("")) {
					// Thread group for runnable
					ThreadGroup tg = new ThreadGroup("ModelCenterGroup"){
						
						/*
						 * Catch uncaught exception during thread run 
						 */
						public void uncaughtException(Thread t, Throwable e){
							ModelCenterPlugin.closeMDSession();
							
							if(e instanceof Exception) {
								Application.getInstance().getGUILog().log("ModelCenter Plugin - Fatal Error: " + e.getMessage());
								
								for(StackTraceElement st : e.getStackTrace()){
									Application.getInstance().getGUILog().log(st.toString());
								}
							}
						}
						
					};
					
					// If yes, create a new solver and solve
					Runnable heavyRunnable = new Runnable() { 
			        	public void run() {
			        		InternalSolver solver = new InternalSolver();
			        		solver.setCreateModelCenterModelFlag(true, modelFilename);
			        		solver.showProgressWindow();
			        		
			        		try {
			        			// First, perform the pre-checks
								solver.performPreChecks(getSelectedInstance());
								
								// If that went well, run the model
								solver.runAllModelsInInstance(getSelectedInstance(), Application.getInstance().getMainFrame().getBrowser().getContainmentTree());
				        		
				        		// Check whether transformation was successful, i.e. whether file name exists
				        		File generatedFile = new File(modelFilename);
				        		
				        		if(generatedFile.exists()) {
				        			// If yes, ask user whether he wants to open the file in ModelCenter
				        			choice = JOptionPane.showConfirmDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Would you like to open the resulting model in ModelCenter now?", "ModelCenter Plugin - Transformation", JOptionPane.YES_NO_OPTION);
								
				        			if(choice == JOptionPane.YES_OPTION) {
										try {
											ModelCenterPlugin.getSynchronizationEngineInstance().launchModelCenter(modelFilename);
										}
										catch (FailedToLaunchModelCenter e1) {
											// TODO Auto-generated catch block
											e1.printStackTrace();
										}
										
										// TODO: Ask for importing values? Synchronization?
									}
				        		}
				        		else {
				        			// Show an error message
				        			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "An error occurred during the transformation. Make sure that connections\nare set up properly and that all ModelCenter models can be executed within ModelCenter", "ModelCenter Plugin - Transformation", JOptionPane.ERROR_MESSAGE);
				        		}
							}
			        		catch (IncompatibleNames e) {
								solver.hideProgressWindow();
								
								JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), e.getMessage(), "ModelCenter Plugin - Model Verification", JOptionPane.ERROR_MESSAGE);
			        			
								e.printStackTrace();
							}
			        		catch (DuplicateInstanceNameException e) {
			        			solver.hideProgressWindow();
			        			
			        			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), e.getMessage(), "ModelCenter Plugin - Model Verification", JOptionPane.ERROR_MESSAGE);
			        			
								e.printStackTrace();
							}
							catch (DuplicateNamesException e) {
								solver.hideProgressWindow();
								
								JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), e.getMessage(), "ModelCenter Plugin - Model Verification", JOptionPane.ERROR_MESSAGE);
			        			
								e.printStackTrace();
							}
							catch (FileNotFoundException e) {
								solver.hideProgressWindow();
								
								JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), e.getMessage(), "ModelCenter Plugin - Model Verification", JOptionPane.ERROR_MESSAGE);
			        			
								e.printStackTrace();
							}
							catch (WriteAccessDeniedException e) {
								solver.hideProgressWindow();
								
								JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), e.getMessage(), "ModelCenter Plugin - Model Verification", JOptionPane.ERROR_MESSAGE);
			        			
								e.printStackTrace();
							}
							catch (ReadPermissionNotGrantedException e) {
								solver.hideProgressWindow();
								
								JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), e.getMessage(), "ModelCenter Plugin - Model Verification", JOptionPane.ERROR_MESSAGE);
			        			
								e.printStackTrace();
							}
							catch(Exception e) {
								solver.hideProgressWindow();
								
								if(e.getMessage() != null && !e.getMessage().equals("") && !e.getMessage().equals("null"))
									JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Unhandled exception:\n\n" + e.getMessage(), "ModelCenter Plugin - Model Verification", JOptionPane.ERROR_MESSAGE);
			        			
			        			e.printStackTrace();
			        		}
			        	}
					};
					
					// Start the new thread
					new Thread(tg, heavyRunnable).start();
				}
			}
		}
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
