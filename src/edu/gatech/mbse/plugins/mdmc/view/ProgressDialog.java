package edu.gatech.mbse.plugins.mdmc.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JProgressBar;
import javax.swing.JLabel;

import edu.gatech.mbse.plugins.mdmc.util.ProgressMonitor;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Canvas;
import java.awt.Label;

public class ProgressDialog extends JDialog implements ChangeListener {

	private final JPanel contentPanel = new JPanel();
	private ProgressMonitor progressMonitor;
	private JLabel statusLabel;
	private JProgressBar progressBar;
	private JLabel subStatusLabel;
	private JButton cancelButton; 

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			ProgressDialog dialog = new ProgressDialog(null, new ProgressMonitor(0, false, 500));
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public ProgressDialog(Frame owner, ProgressMonitor monitor) {
		super(owner, "Progress", false); 
		
		this.progressMonitor = monitor; 
		
		setModal(true);
		setResizable(false);
		setTitle("ModelCenter Plugin Status");
		//setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 440, 146);
		setMinimumSize(new Dimension(440, 146));
		BorderLayout borderLayout = new BorderLayout();
		getContentPane().setLayout(borderLayout);
		getContentPane().setBounds(100, 100, 440, 146);
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		contentPanel.setBounds(100, 100, 440, 146);
		
		progressBar = new JProgressBar();
		progressBar.setBounds(10, 55, 414, 14);
		contentPanel.add(progressBar);
		
		statusLabel = new JLabel("Initializing ...");
		statusLabel.setBounds(54, 11, 370, 14);
		contentPanel.add(statusLabel);
		{
			subStatusLabel = new JLabel("Substatus");
			subStatusLabel.setBounds(54, 30, 370, 14);
			contentPanel.add(subStatusLabel);
		}
		
		JLabel lblNewLabel = new JLabel(new ImageIcon(getClass().getResource("bigger.gif")));
		lblNewLabel.setBounds(10, 11, 32, 32);
		contentPanel.add(lblNewLabel);
		{
			JPanel buttonPane = new JPanel();
			FlowLayout fl_buttonPane = new FlowLayout(FlowLayout.RIGHT);
			fl_buttonPane.setVgap(10);
			fl_buttonPane.setHgap(10);
			buttonPane.setLayout(fl_buttonPane);
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						cancelButton.setEnabled(false);
						
						progressMonitor.cancelOperation();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
			{
				JButton okButton = new JButton("Close");
				okButton.setEnabled(false);
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
		}
		
		init(monitor);
	}
	
	/**
	 * Initialize progress dialog
	 * 
	 * @param monitor
	 */
	private void init(ProgressMonitor monitor){ 
        progressBar.setMinimum(0);
        progressBar.setMaximum(monitor.getTotal()); 
        
        if(monitor.isIndeterminate()) 
            progressBar.setIndeterminate(true); 
        else 
            progressBar.setValue(monitor.getCurrent()); 
        
        statusLabel.setText(monitor.getStatus());
        subStatusLabel.setText(monitor.getSubStatus()); 
 
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); 
        monitor.addChangeListener(this); 
    }
	
	/**
	 * 
	 * @param ce
	 */
	public void stateChanged(final ChangeEvent ce){ 
        // to ensure EDT thread 
        if(!SwingUtilities.isEventDispatchThread()){ 
            SwingUtilities.invokeLater(new Runnable(){ 
                public void run(){ 
                    stateChanged(ce); 
                } 
            }); 
            return; 
        }
        
        if(progressBar != null && statusLabel != null) {
        	// Dispose of dialog is progress bar is at 100%
	        if(progressMonitor.getCurrent() != progressMonitor.getTotal()){ 
	            statusLabel.setText(progressMonitor.getStatus());
	            subStatusLabel.setText(progressMonitor.getSubStatus());
	            
	            if(!progressMonitor.isIndeterminate()) 
	                progressBar.setValue(progressMonitor.getCurrent()); 
	        }
	        else 
	            dispose();
        }
        
        if(progressMonitor.isCancelled() && !statusLabel.getText().equals("Cancelling ...")) {
        	statusLabel.setText("Cancelling ...");
            subStatusLabel.setText("Waiting for operation to finish ...");
        }
	}
	
}
