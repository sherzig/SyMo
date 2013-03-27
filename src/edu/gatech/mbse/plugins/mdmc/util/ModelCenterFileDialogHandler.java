/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.util;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;

/**
 * @author Sebastian
 *
 */
public class ModelCenterFileDialogHandler {
	
	/**
	 * 
	 * @param presetFilename
	 * @return
	 */
	public String showSaveAsDialog(String presetFilename) {
		final JFileChooser fc = createSaveAsFileChooser();
		
		fc.setCurrentDirectory(new File(Application.getInstance().getProject().getDirectory()));
		
		if(!presetFilename.equals("")) {
			fc.setSelectedFile(new File(presetFilename + ".pxc"));
		}
		
		fc.setDialogTitle("Save ModelCenter Model As ...");
		int opt = fc.showSaveDialog(MDDialogParentProvider.getProvider().getDialogParent());
		
		if(opt==JFileChooser.APPROVE_OPTION) {
			String selectedFile = fc.getSelectedFile().toString();
			
			if(selectedFile.endsWith(".pxc"))
				return selectedFile;
			
			return selectedFile + ".pxc";
		}
		
		return "";
	}
	
	/**
	 * 
	 * @param presetFilename
	 * @return
	 */
	public String showOpenDialog() {
		final JFileChooser fc = createOpenFileChooser();
		
		fc.setCurrentDirectory(new File(Application.getInstance().getProject().getDirectory()));
		fc.setDialogTitle("Select ModelCenter Model File");
		
		int opt = fc.showOpenDialog(MDDialogParentProvider.getProvider().getDialogParent());
		
		if(opt==JFileChooser.APPROVE_OPTION) {
			String selectedFile = fc.getSelectedFile().toString();
			
			if(selectedFile.endsWith(".pxc"))
				return selectedFile;
			
			return selectedFile + ".pxc";
		}
		
		return "";
	}
	
	/**
	 * 
	 * @return
	 */
	private JFileChooser createSaveAsFileChooser() {
		final JFileChooser fc = new JFileChooser() {
			
			/**
			 * Ask user whether to overwrite file if file already exists
			 */
			public void approveSelection() {
				File f = getSelectedFile();
				
				// TODO: Overwrite when extension not specified!!!!!
				
			    if(f.exists() && getDialogType() == SAVE_DIALOG){
			        int result = JOptionPane.showConfirmDialog(this, "Overwrite existing file?", "File Exists", JOptionPane.YES_NO_OPTION);
			        
			        switch(result) {
			            case JOptionPane.YES_OPTION:
			                super.approveSelection();
			                return;
			                
			            case JOptionPane.NO_OPTION:
			                return;
			        }
			    }
		    
			    super.approveSelection();
			}
		    
		};
	
		fc.addChoosableFileFilter(createFileFilter());
		
		return fc;
	}
	
	/**
	 * 
	 * @return
	 */
	private JFileChooser createOpenFileChooser() {
		final JFileChooser fc = new JFileChooser();
	
		fc.addChoosableFileFilter(createFileFilter());
		
		return fc;
	}
	
	/**
	 * 
	 * @return
	 */
	private FileFilter createFileFilter() {
		// Only accept ModelCenter models
		FileFilter filter = new FileFilter() {

			@Override
			public boolean accept(File f) {
				if(!f.isFile())
					return true;
				
				if(f.getName().endsWith(".pxc"))
					return true;
				
				return false;
			}

			@Override
			public String getDescription() {
				return "ModelCenter Model (*.pxc)";
			}
			
		};
		
		return filter;
	}
	
}
