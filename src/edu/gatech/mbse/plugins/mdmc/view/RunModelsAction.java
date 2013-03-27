/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.view;

import java.awt.event.ActionEvent;
import java.io.FileNotFoundException;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.phoenix_int.ModelCenter.ModelCenterException;

import edu.gatech.mbse.plugins.mdmc.controller.InternalSolver;
import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;
import edu.gatech.mbse.plugins.mdmc.exceptions.DuplicateInstanceNameException;
import edu.gatech.mbse.plugins.mdmc.exceptions.DuplicateNamesException;
import edu.gatech.mbse.plugins.mdmc.exceptions.IncompatibleNames;
import edu.gatech.mbse.plugins.mdmc.exceptions.ReadPermissionNotGrantedException;
import edu.gatech.mbse.plugins.mdmc.exceptions.WriteAccessDeniedException;

/**
 * @author Sebastian
 *
 */
public class RunModelsAction extends MDAction {
	
	private InstanceSpecification selectedInstance_ = null;

	/**
	 * 
	 */
	public RunModelsAction() {
		super("", "Run Associated PHX ModelCenter Model(s)", null, null);
		
		// Set icon
		this.setSmallIcon(new ImageIcon(getClass().getResource("run.gif")));
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// Check whether the selected instance was set (from the outside)
		if(getSelectedInstance() != null) {
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
			Runnable solverThread = new Runnable() { 
	        	public void run() {
					InternalSolver solver = new InternalSolver();
					solver.setCreateModelCenterModelFlag(false, "");
					solver.showProgressWindow();
					
					try {
						// First, perform some pre-checks
						solver.performPreChecks(getSelectedInstance());
						
						// Show any potential warnings (and stall until user selects to continue)
						solver.showWarnings();
						
						// Now run all of the models
						solver.runAllModelsInInstance(getSelectedInstance(), Application.getInstance().getMainFrame().getBrowser().getContainmentTree());
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
			
			// Start thread
			new Thread(tg, solverThread).start();
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

}
