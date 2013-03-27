/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JOptionPane;

import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.plugins.*;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.magicdraw.uml.DiagramType;
import com.phoenix_int.ModelCenter.Assembly;
import com.phoenix_int.ModelCenter.ModelCenter;
import com.phoenix_int.ModelCenter.ModelCenterException;
import com.phoenix_int.ModelCenter.DoubleArray;
import com.phoenix_int.ModelCenter.Variant;

import edu.gatech.mbse.plugins.mdmc.model.ModelCenterVariable;
import edu.gatech.mbse.plugins.mdmc.model.ScriptComponent;
import edu.gatech.mbse.plugins.mdmc.util.WindowsRegistry;
import edu.gatech.mbse.plugins.mdmc.view.ContainmentTreeContextPop;
import edu.gatech.mbse.plugins.mdmc.view.InstanceDiagramContextPop;

/**
 * @author Sebastian
 *
 */
public class ModelCenterPlugin extends Plugin {

	private static ModelCenter modelCenter_ = null;					// ModelCenter instance
	private static MDModelFactory modelFactory_ = null;				// Produces representations of a ModelCenter model in UML
	private static MDModelHandler modelHandler_ = null;				// Handles existing models
	private static SynchronizationEngine synchronizer_ = null;		// Synchronizes models
	
	@Override
	public boolean close() {
		// Just to make sure - try to close any open sessions
		ModelCenterPlugin.closeMDSession();
		
		return true;
	}

	@Override
	public void init() {
		// Check to see whether the required ModelCenter .dll file has already been copied to the
		// Java virtual machine's "bin" directory and do so if not. Note that this function will
		// detect whether or not a 32 bit or a 64 bit version of MagicDraw is used
		if(!checkJNIInstallation()) {
			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Failed to automatically install JNIModelCenter.dll at the correct location. Please do the following:\n\n" +
					"1) Go to your MagicDraw installation directory\n" + 
					"2) Navigate to the folder titled \"plugins\", then \"edu.gatech.mbse.plugins.mdmc\"\n" +
					"3) Now, copy the file \"JNIModelCenter.dll\" located in the sub-folder \"win" + System.getProperty("sun.arch.data.model") + "\" over to\n" +
					"" + System.getProperty("java.home") + "\\bin\\\n\n" +
					"After you have completed the above steps, please restart MagicDraw", "ModelCenter Plug-In", JOptionPane.ERROR_MESSAGE);
		}
		else {
			// Add the context pop manager for the containment tree
			ActionsConfiguratorsManager.getInstance().addContainmentBrowserContextConfigurator(new ContainmentTreeContextPop());
			
			// Add the context pop manager in the diagram section
			ActionsConfiguratorsManager.getInstance().addDiagramContextConfigurator(DiagramType.UML_ANY_DIAGRAM, new InstanceDiagramContextPop());
		}
	}

	@Override
	public boolean isSupported() {
		return true;
	}
	
	/**
	 * Converts a given string into a string that is safe to use in ModelCenter as a name for variables
	 * and/or assemblies by removing all characters other than A-Z, a-z, 0-9 and underscores.
	 * 
	 * @param name
	 * @return The modified string
	 */
	public static String toModelCenterSafeName(String name) {
		return name.replaceAll("[^A-Za-z0-9_]", "");
	}
	
	/**
	 * Creates (if necessary) and returns the static, plugin wide ModelCenter instance
	 * 
	 * @return
	 * @throws ModelCenterException
	 */
	public static ModelCenter getModelCenterInstance() throws ModelCenterException {
		if(ModelCenterPlugin.modelCenter_ == null)
			resetModelCenterInstance();
		
		return ModelCenterPlugin.modelCenter_;
	}
	
	/**
	 * Creates (if necessary) and returns the static, plugin wide ModelCenter instance
	 * 
	 * @return
	 * @throws ModelCenterException
	 */
	public static void resetModelCenterInstance() throws ModelCenterException {
		ModelCenterPlugin.modelCenter_ = new ModelCenter();
	}
	
	/**
	 * Returns the static (hence sort of global) model factory instance
	 * 
	 * @return
	 */
	public static MDModelFactory getMDModelFactoryInstance() {
		if(ModelCenterPlugin.modelFactory_ == null)
			ModelCenterPlugin.modelFactory_ = new MDModelFactory();
		
		return ModelCenterPlugin.modelFactory_;
	}
	
	/**
	 * Returns the static (hence sort of global) model handler instance
	 * 
	 * @return
	 */
	public static MDModelHandler getMDModelHandlerInstance() {
		if(ModelCenterPlugin.modelHandler_ == null)
			ModelCenterPlugin.modelHandler_ = new MDModelHandler();
		
		return ModelCenterPlugin.modelHandler_;
	}
	
	/**
	 * Returns the static (hence sort of global) sychronization engine
	 * 
	 * @return
	 */
	public static SynchronizationEngine getSynchronizationEngineInstance() {
		if(ModelCenterPlugin.synchronizer_ == null)
			ModelCenterPlugin.synchronizer_ = new SynchronizationEngine();
		
		return ModelCenterPlugin.synchronizer_;
	}
	
	/**
	 * Ensures that a session is created and active within the plugin environment
	 */
	public static void ensureMDSessionIsActive() {
		if(!SessionManager.getInstance().isSessionCreated()) {
			SessionManager.getInstance().createSession("ModelCenter Plugin");
		}
	}
	
	/**
	 * Closes the session if it is active
	 */
	public static void closeMDSession() {
		if(SessionManager.getInstance().isSessionCreated()) {
			SessionManager.getInstance().closeSession();
		}
	}
	
	/**
	 * Checks whether the JNIModelCenter.dll was copied over to the correct directory and copies it
	 * to the java.home directory if possible
	 */
	private boolean checkJNIInstallation() {
		String destinationFilePath = System.getProperty("java.home") + "\\bin\\JNIModelCenter.dll";
		
		// Check to see whether the JNIModelCenter.dll file is where it should be
		try {
			File f = new File(destinationFilePath);
			
			if(!f.exists()) {
				// It appears as though it does not exist, therefore, try copying it over to the new directory
				File src = new File(System.getProperty("user.dir") + "\\plugins\\edu.gatech.mbse.plugins.mdmc\\win" + System.getProperty("sun.arch.data.model") + "\\JNIModelCenter.dll");
				
				try {
					// Copy the file
					InputStream in = new FileInputStream(src);
					OutputStream out = new FileOutputStream(f);
					
					byte[] buf = new byte[1024];
					int len;
					
					// Perform the actual copying
					while((len = in.read(buf)) > 0)
						  out.write(buf, 0, len);
					
					// Close streams
					in.close();
					out.close();
				}
				catch(Exception e) {
					return false;
				}
			}
		}
		catch(Exception e) {
			// General read error
			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Error while trying to install JNIModelCenter.dll:\n" + e.getMessage(), "ModelCenter Plug-In", JOptionPane.ERROR_MESSAGE);
			
			return false;
		}
			
		return true;
	}

}
