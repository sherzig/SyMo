/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.view;

import java.util.Iterator;
import java.util.List;

import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.actions.ActionsID;
import com.nomagic.magicdraw.actions.BrowserContextAMConfigurator;
import com.nomagic.magicdraw.actions.MDActionsCategory;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;

import edu.gatech.mbse.plugins.mdmc.controller.ModelCenterPlugin;

/**
 * @author Sebastian
 *
 */
public class ContainmentTreeContextPop implements BrowserContextAMConfigurator {

	@Override
	public int getPriority() {
		return AMConfigurator.MEDIUM_PRIORITY;
	}

	/**
	 * Configure the pop up menu depending on the element selected
	 */
	@Override
	public void configure(ActionsManager actionsManager, Tree tree) {
		// Don't even display menu if profile not loaded?
		System.out.println("Creating it");
		
		// Build the tree depending on what was selected
		if(tree.getSelectedNode() != null && Application.getInstance().getMainFrame().getBrowser().getActiveTree() == Application.getInstance().getMainFrame().getBrowser().getContainmentTree()) {
			// Get the element that was selected - only care about the first one
			Element selectedElement = (Element)tree.getSelectedNode().getUserObject();
			
			// Make sure the element is not null
			if(selectedElement != null) {
				// Create sub category for element creation
				ActionsCategory createElementsCat = new ActionsCategory("ModelCenterCreateElements", "ModelCenter");
				createElementsCat.setNested(true);
				
				// Category for other actions, such as import, synchronize, solve, etc.
				ActionsCategory modelCenterCat = new ActionsCategory("ModelCenter", "ModelCenter");
				modelCenterCat.setNested(true);
				
				if(selectedElement instanceof Package && selectedElement.isEditable()) {
					// If we have a package, allow for ModelCenter models to be created and to be imported
					CreateDataModelAction createDataModelAction = new CreateDataModelAction();
					ImportModelCenterModelAction importMCModelAction = new ImportModelCenterModelAction();
					
					// New Element -> ModelCenter -> ...
					createElementsCat.addAction(createDataModelAction);
					
					// ModelCenter -> ...
					modelCenterCat.addAction(new DisabledDummyBrowserAction("Edit in ModelCenter"));
					modelCenterCat.addAction(importMCModelAction);
					
					ActionsCategory synchroSection = new ActionsCategory("ModelCenterSynchroSection", "ModelCenterSynchroSection");
					synchroSection.setNested(false);
					
					ActionsCategory synchroCat = new ActionsCategory("ModelCenterSynchronization", "Model Synchronization");
					synchroCat.setNested(true);
					synchroCat.addAction(new DisabledDummyBrowserAction("Synchronize SysML Model with ModelCenter Model"));
					synchroCat.addAction(new DisabledDummyBrowserAction("Synchronize ModelCenter Model with SysML Model"));
					
					synchroSection.addAction(synchroCat);
					
					modelCenterCat.addAction(synchroSection);
					
					// Element creation
					List<NMAction> actions = actionsManager.getCategories().get(0).getActions().get(0).getActions();
					actions.add(0, createElementsCat);
					actionsManager.getCategories().get(0).getActions().get(0).setActions(actions);
					
					// Other ModelCenter actions
					actionsManager.addCategory(modelCenterCat);
				}
				else if(ModelCenterPlugin.getMDModelHandlerInstance().isModelCenterDataModel(selectedElement)) {
					// If we have a ModelCenter model selected, allow for variables to be created, the
					// models be updated in ModelCenter, and synchronized
					CreateInputVariableAction createVariableAction = new CreateInputVariableAction();
					CreateScriptAction createScriptAction = new CreateScriptAction();
					OpenModelInModelCenterAction openModelInModelCenterAction = new OpenModelInModelCenterAction();
					SynchronizeFromModelCenterModelAction synchronizeFromModelCenterModelAction = new SynchronizeFromModelCenterModelAction();
					SynchronizeFromSysMLModelAction synchronizeFromSysMLModelAction = new SynchronizeFromSysMLModelAction();
					
					// New Element -> ModelCenter -> ...
					createElementsCat.addAction(createVariableAction);
					createElementsCat.addAction(createScriptAction);
					
					// ModelCenter -> ...
					modelCenterCat.addAction(openModelInModelCenterAction);
					modelCenterCat.addAction(new DisabledDummyBrowserAction("Import ModelCenter Model ..."));
					
					// Separator, then synchronization menu
					ActionsCategory synchroSection = new ActionsCategory("ModelCenterSynchroSection", "ModelCenterSynchroSection");
					synchroSection.setNested(false);
					
					ActionsCategory synchroCat = new ActionsCategory("ModelCenterSynchronization", "Model Synchronization");
					synchroCat.setNested(true);
					synchroCat.addAction(synchronizeFromModelCenterModelAction);
					synchroCat.addAction(synchronizeFromSysMLModelAction);
					
					synchroSection.addAction(synchroCat);
					
					modelCenterCat.addAction(synchroSection);
					
					// Element creation
					List<NMAction> actions = actionsManager.getCategories().get(0).getActions().get(0).getActions();
					actions.add(0, createElementsCat);
					actionsManager.getCategories().get(0).getActions().get(0).setActions(actions);
					
					// Other ModelCenter actions
					actionsManager.getLastActionsCategory().addAction(modelCenterCat);
				}
				else if(selectedElement instanceof InstanceSpecification && StereotypesHelper.getProfile(Application.getInstance().getProject(), "ModelCenter") != null) {
					// If an instance was selected, allow the user to try and solve for instance values using
					// ModelCenter
					RunModelsAction solveInternally = new RunModelsAction();
					solveInternally.setSelectedInstance((InstanceSpecification)tree.getSelectedNode().getUserObject());
					
					TransformInstanceToModelCenterModelAction transformation = new TransformInstanceToModelCenterModelAction();
					transformation.setSelectedInstance((InstanceSpecification)tree.getSelectedNode().getUserObject());
					
					ImportValuesFromModelCenterModelAction importMCValuesAction = new ImportValuesFromModelCenterModelAction();
					importMCValuesAction.setSelectedInstance((InstanceSpecification)tree.getSelectedNode().getUserObject());
					
					ActionsCategory runSection = new ActionsCategory("ModelCenterRunSection", "ModelCenterRunSection");
					runSection.setNested(false);
					runSection.addAction(solveInternally);
					
					ActionsCategory trafoSection = new ActionsCategory("ModelCenterTrafoSection", "ModelCenterTrafoSection");
					trafoSection.setNested(false);
					trafoSection.addAction(transformation);
					trafoSection.addAction(importMCValuesAction);
					
					actionsManager.addCategory(runSection);
					actionsManager.addCategory(trafoSection);
				}
			}
		}
	}

}
