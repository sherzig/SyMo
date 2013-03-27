/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.view;

import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.magicdraw.actions.DiagramContextAMConfigurator;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.ui.browser.ContainmentTree;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.magicdraw.uml.symbols.PresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.magicdraw.ui.browser.Node;

/**
 * @author Sebastian
 *
 */
public class InstanceDiagramContextPop implements DiagramContextAMConfigurator {

	/**
	 * 
	 */
	public InstanceDiagramContextPop() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.nomagic.magicdraw.actions.ConfiguratorWithPriority#getPriority()
	 */
	@Override
	public int getPriority() {
		return this.MEDIUM_PRIORITY;
	}

	/* (non-Javadoc)
	 * @see com.nomagic.magicdraw.actions.DiagramContextAMConfigurator#configure(com.nomagic.actions.ActionsManager, com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement, com.nomagic.magicdraw.uml.symbols.PresentationElement[], com.nomagic.magicdraw.uml.symbols.PresentationElement)
	 */
	@Override
	public void configure(ActionsManager actionsManager, DiagramPresentationElement diagramPresentationElement, PresentationElement[] presentationElements, PresentationElement presentationElement) {
		// presentationElement.getElement() is null if a right click is performed in the background of the
		// diagram - hence, check first whether the element is null
		if(presentationElement != null && presentationElement.getElement() != null && presentationElement.getElement() instanceof InstanceSpecification && StereotypesHelper.getProfile(Application.getInstance().getProject(), "ModelCenter") != null) {
			// If an instance was selected, allow the user to try and solve for instance values using
			// ModelCenter
			// TODO: This needs to be a diagram action!!!
			// TODO: Also add: open in modelcenter
			RunModelsAction solveInternally = new RunModelsAction();
			solveInternally.setSelectedInstance((InstanceSpecification)presentationElement.getElement());
			
			TransformInstanceToModelCenterModelAction transformation = new TransformInstanceToModelCenterModelAction();
			transformation.setSelectedInstance((InstanceSpecification)presentationElement.getElement());
			
			ImportValuesFromModelCenterModelAction importMCValuesAction = new ImportValuesFromModelCenterModelAction();
			importMCValuesAction.setSelectedInstance((InstanceSpecification)presentationElement.getElement());
			
			ActionsCategory trafoSection = new ActionsCategory("ModelCenterTrafoSection", "ModelCenterTrafoSection");
			trafoSection.setNested(false);
			trafoSection.addAction(transformation);
			trafoSection.addAction(importMCValuesAction);
			
			actionsManager.getLastActionsCategory().addAction(solveInternally);
			actionsManager.getLastActionsCategory().addAction(trafoSection);
		}
	}

}
