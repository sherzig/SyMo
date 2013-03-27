/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.controller;

import com.phoenix_int.ModelCenter.Component;

/**
 * @author Sebastian
 *
 */
public class ScriptComponentHandler {
	
	private ModelCenterFileManipulator mcFileManipulator_ = null;
	
	/**
	 * 
	 */
	public ScriptComponentHandler() {
		setMCFileManipulator(new ModelCenterFileManipulator());
	}
	
	/**
	 * 
	 */
	public void updateModelCenterModel() {
		
	}

	/**
	 * @return the mcFileManipulator
	 */
	public ModelCenterFileManipulator getMCFileManipulator() {
		return mcFileManipulator_;
	}

	/**
	 * @param mcFileManipulator the mcFileManipulator to set
	 */
	public void setMCFileManipulator(ModelCenterFileManipulator mcFileManipulator) {
		this.mcFileManipulator_ = mcFileManipulator;
	}

}
