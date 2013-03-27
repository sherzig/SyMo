/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.controller;

import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.phoenix_int.ModelCenter.ModelCenterException;

/**
 * @author Sebastian
 *
 */
public class TransformationEngine {

	private InstanceToModelCenterTransformation instanceToModelCenterTransformation_ = null;
	private ModelCenterValuesImporter modelCenterValuesImporter_ = null;
	
	/**
	 * 
	 */
	public TransformationEngine() {
		this.setInstanceToModelCenterTransformation(new InstanceToModelCenterTransformation());
		this.setModelCenterValuesImporter(new ModelCenterValuesImporter());
	}
	
	/**
	 * 
	 * @param instanceSpec
	 * @param tree
	 * @throws ModelCenterException 
	 */
	public void toModelCenterModel(InstanceSpecification instanceSpec, Tree tree) throws ModelCenterException {
		// First transform the instance to a ModelCenter model
		getInstanceToModelCenterTransformation().createModelCenterModelFromInstance(instanceSpec, tree);
	}
	
	/**
	 * 
	 * @param instanceSpec
	 * @throws ModelCenterException 
	 */
	public void importValuesFromModelCenterModel(InstanceSpecification instanceSpec, String filename) throws ModelCenterException {
		// First transform the instance to a ModelCenter model
		getModelCenterValuesImporter().updateInstanceValues(instanceSpec, filename);
	}

	/**
	 * @return the instanceToModelCenterTransformation_
	 */
	public InstanceToModelCenterTransformation getInstanceToModelCenterTransformation() {
		return instanceToModelCenterTransformation_;
	}

	/**
	 * @param instanceToModelCenterTransformation_ the instanceToModelCenterTransformation to set
	 */
	private void setInstanceToModelCenterTransformation(
			InstanceToModelCenterTransformation instanceToModelCenterTransformation) {
		this.instanceToModelCenterTransformation_ = instanceToModelCenterTransformation;
	}

	/**
	 * @return the modelCenterValuesImporter
	 */
	public ModelCenterValuesImporter getModelCenterValuesImporter() {
		return modelCenterValuesImporter_;
	}

	/**
	 * @param modelCenterValuesImporter the modelCenterValuesImporter to set
	 */
	public void setModelCenterValuesImporter(ModelCenterValuesImporter modelCenterValuesImporter) {
		this.modelCenterValuesImporter_ = modelCenterValuesImporter;
	}

}
