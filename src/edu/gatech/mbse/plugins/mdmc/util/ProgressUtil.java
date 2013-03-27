/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.util;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.gatech.mbse.plugins.mdmc.view.ProgressDialog;

/**
 * Borrowed from http://www.javalobby.org/java/forums/t53926.html
 *
 */
public class ProgressUtil{ 
    static class MonitorListener implements ChangeListener, ActionListener{ 
        ProgressMonitor monitor; 
        Window owner; 
        Timer timer;
        ProgressDialog dlg = null;
 
        public MonitorListener(Window owner, ProgressMonitor monitor){ 
            this.owner = owner; 
            this.monitor = monitor; 
        } 
 
        public void stateChanged(ChangeEvent ce){ 
            ProgressMonitor monitor = (ProgressMonitor)ce.getSource(); 
            if(monitor.getCurrent()!=monitor.getTotal()){ 
                if(timer==null){ 
                    timer = new Timer(monitor.getMilliSecondsToWait(), this); 
                    timer.setRepeats(false); 
                    timer.start(); 
                } 
            }else{ 
                if(timer!=null && timer.isRunning()) 
                    timer.stop(); 
                monitor.removeChangeListener(this); 
            } 
        } 
 
        public void actionPerformed(ActionEvent e){ 
            monitor.removeChangeListener(this); 
            dlg = new ProgressDialog((Frame)owner, monitor); 
            dlg.pack(); 
            dlg.setLocationRelativeTo(owner);
            this.showDialog(); 
        } 
        
        public void hideDialog() {
        	if(timer != null)
        		timer.stop();
        	
        	if(dlg != null)
        		dlg.setVisible(false);
        }
        
        public void showDialog() {
        	if(timer != null && !timer.isRunning())
        		timer.start();
        	
        	if(dlg != null)
        		dlg.setVisible(true);
        }
    } 
 
    public static ProgressMonitor createModalProgressMonitor(Component owner, int total, boolean indeterminate, int milliSecondsToWait){ 
        ProgressMonitor monitor = new ProgressMonitor(total, indeterminate, milliSecondsToWait); 
        Window window = owner instanceof Window 
                ? (Window)owner 
                : SwingUtilities.getWindowAncestor(owner); 
        monitor.addChangeListener(new MonitorListener(window, monitor)); 
        return monitor; 
    } 
}