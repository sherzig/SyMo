/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.view;

import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;

/**
 * @author Sebastian
 *
 */
public class DisabledDummyBrowserAction extends DefaultBrowserAction {

	/**
	 * 
	 */
	public DisabledDummyBrowserAction(String actionName) {
		super("", actionName, null, null);
		
		this.setEnabled(false);
	}
	
	/**
	 * 
	 */
	public void updateState() {
		this.setEnabled(false);
	}

}
