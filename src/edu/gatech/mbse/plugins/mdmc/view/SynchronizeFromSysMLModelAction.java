/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.view;

import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;

import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.phoenix_int.ModelCenter.ModelCenterException;

import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;

/**
 * @author Sebastian
 *
 */
public class SynchronizeFromSysMLModelAction extends DefaultBrowserAction {

	/**
	 * 
	 */
	public SynchronizeFromSysMLModelAction() {
		super("", "Synchronize ModelCenter Model with SysML Model", null, null);
		
	}
	
	/**
	 * 
	 */
	public void actionPerformed(ActionEvent e) {
		try {
			ModelCenterPlugin.getModelCenterInstance().closeModel();
			ModelCenterPlugin.getModelCenterInstance().loadModel(ModelCenterPlugin.getMDModelHandlerInstance().getModelCenterDataModelFilename((Element)getTree().getSelectedNode().getUserObject()));
			ModelCenterPlugin.getSynchronizationEngineInstance().updateModelCenterModelFromSysMLModel((Element)getTree().getSelectedNode().getUserObject());
		} catch (ModelCenterException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	public void updateState() {
		
	}

}
