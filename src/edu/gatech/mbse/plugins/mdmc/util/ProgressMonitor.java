/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.util;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.gatech.mbse.plugins.mdmc.util.ProgressUtil.MonitorListener;

/**
 * Borrowed from http://www.javalobby.org/java/forums/t53926.html
 * Modified by Sebastian J. I. Herzig
 */
public class ProgressMonitor { 
	
    int total, current=-1; 
    boolean indeterminate; 
    int milliSecondsToWait = 500; // half second 
    String status;
    String subStatus; 
    boolean isCancelled = false;
 
    public ProgressMonitor(int total, boolean indeterminate, int milliSecondsToWait){ 
        this.total = total; 
        this.indeterminate = indeterminate; 
        this.milliSecondsToWait = milliSecondsToWait; 
    } 
 
    public ProgressMonitor(int total, boolean indeterminate){ 
        this.total = total; 
        this.indeterminate = indeterminate; 
    } 
 
    public int getTotal(){ 
        return total; 
    } 
 
    public void start(String status, String subStatus){ 
        if(current!=-1) 
            throw new IllegalStateException("not started yet"); 
        this.status = status;
        this.subStatus = subStatus; 
        current = 0; 
        isCancelled = false;
        fireChangeEvent(); 
    } 
 
    public int getMilliSecondsToWait(){ 
        return milliSecondsToWait; 
    } 
 
    public int getCurrent(){ 
        return current; 
    } 
 
    public String getStatus(){ 
        return status; 
    } 
    
    public String getSubStatus(){ 
        return subStatus; 
    } 
 
    public boolean isIndeterminate(){ 
        return indeterminate; 
    }
    
    public boolean isCancelled() {
    	return isCancelled;
    }
 
    public void setCurrent(String status, String subStatus, int current){ 
        if(current==-1) 
            throw new IllegalStateException("not started yet"); 
        this.current = current; 
        if(status!=null) 
            this.status = status; 
        if(subStatus!=null) 
            this.subStatus = subStatus; 
        fireChangeEvent(); 
    } 
    
    public void cancelOperation() {
    	isCancelled = true;
    	
    	fireChangeEvent();
    }
 
    private Vector listeners = new Vector(); 
    private ChangeEvent ce = new ChangeEvent(this); 
 
    public void addChangeListener(ChangeListener listener){ 
        listeners.add(listener); 
    } 
 
    public void removeChangeListener(ChangeListener listener){ 
        listeners.remove(listener); 
    } 
 
    private void fireChangeEvent(){ 
    	Object[] changeListeners = listeners.toArray(); 
        for(int i=0; i<changeListeners.length; i++){ 
            ((ChangeListener)changeListeners[i]).stateChanged(ce); 
        } 
    } 
    
    public void hideProgressDialog() {
    	Object[] changeListeners = listeners.toArray(); 
    	
    	for(int i=0; i<changeListeners.length; i++){
    		if(changeListeners[i] instanceof MonitorListener) {
    			((MonitorListener)changeListeners[i]).hideDialog();
    		}
        } 
    }
    
    public void showProgressDialog() {
    	Object[] changeListeners = listeners.toArray(); 
    	
    	for(int i=0; i<changeListeners.length; i++){
    		if(changeListeners[i] instanceof MonitorListener) {
    			((MonitorListener)changeListeners[i]).showDialog();
    		}
        } 
    }
}