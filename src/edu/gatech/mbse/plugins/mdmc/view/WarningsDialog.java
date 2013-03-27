package edu.gatech.mbse.plugins.mdmc.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.AbstractListModel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.JLabel;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.TypedElement;

import edu.gatech.mbse.plugins.mdmc.controller.WarningItem;
import edu.gatech.mbse.plugins.mdmc.controller.Severity;

import java.awt.Dialog.ModalExclusionType;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.Dialog.ModalityType;

public class WarningsDialog extends JDialog {

	private JPanel contentPane;
	private JTable table;
	private JButton btnStop;
	private JScrollPane scrollPane;
	private ArrayList<WarningItem> warnings;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WarningsDialog frame = new WarningsDialog();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public WarningsDialog() {
		setModal(true);
		setResizable(false);
		setIconImage(Toolkit.getDefaultToolkit().getImage(WarningsDialog.class.getResource("/edu/gatech/mbse/plugins/mdmc/view/transformation.gif")));
		setTitle("ModelCenter Plugin - Warnings");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 580, 275);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JButton btnClose = new JButton("Continue");
		btnClose.setBounds(466, 205, 89, 23);
		contentPane.add(btnClose);
		
		table = new JTable() {
			public boolean isCellEditable(int rowIndex, int colIndex) {
				return false; //Disallow the editing of any cell
			}
		};
		table.setModel(new DefaultTableModel(
			new Object[][] {
			},
			new String[] {
				"", "Severity", "Description", "Element"
			}
		));
		table.getColumnModel().getColumn(0).setPreferredWidth(25);
		table.getColumnModel().getColumn(1).setPreferredWidth(100);
		table.getColumnModel().getColumn(2).setPreferredWidth(250);
		table.getColumnModel().getColumn(3).setPreferredWidth(150);
		table.setShowVerticalLines(false);
		table.setShowHorizontalLines(false);
		table.setShowGrid(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setBounds(0, 0, 540, 170);
		
		scrollPane = new JScrollPane(table);
		scrollPane.setViewportBorder(null);
		scrollPane.setBackground(Color.white);
		scrollPane.setBounds(15, 15, 540, 170);
		scrollPane.getViewport().setOpaque(false);
		
		contentPane.add(scrollPane);
		
		JLabel lblDoubleclickOnA = new JLabel("Double-click on a warning message for more details");
		lblDoubleclickOnA.setBounds(15, 209, 289, 14);
		contentPane.add(lblDoubleclickOnA);
		
		btnStop = new JButton("Abort Run");
		btnStop.setBounds(367, 205, 89, 23);
		contentPane.add(btnStop);
	}
	
	/**
	 * Fill the warnings table with values from the warnings generated during the pre checking process
	 */
	public void fillWarningsTable() {
		// Table model will be a DefaultTableModel instance (see initialization in constructor) 
		DefaultTableModel dataModel = (DefaultTableModel)table.getModel();
		
		// Go through each of the warnings
		for(Iterator<WarningItem> iter = getWarnings().iterator(); iter.hasNext(); ) {
			WarningItem nextWarning = iter.next();
			
			String severity = "";
			String description = nextWarning.getDescription();
			String elementName = "";
			
			// Generate severity string
			switch(nextWarning.getSeverity()) {
				case MC_SEVERITY_CRITICAL:
					severity = "Critical";
					break;
					
				case MC_SEVERITY_WARNING:
					severity = "Warning";
					break;
					
				case MC_SEVERITY_INFO:
				default:
					severity = "Info";
					break;
			}
			
			// Generate element name field
			if(nextWarning.getAffectedModelCenterModel() != null) {
				elementName = nextWarning.getAffectedModelCenterModel().getName();
			}
			
			String subElementNames = "";
			
			// Append any specific elements
			if(!nextWarning.getAffectedElements().isEmpty()) {
				for(int i=0; i<nextWarning.getAffectedElements().size(); i++) {
					if(nextWarning.getAffectedElements().get(i) != null && nextWarning.getAffectedElements().get(i) instanceof NamedElement) {
						String name = ((NamedElement)nextWarning.getAffectedElements().get(i)).getName();
						
						if(name.equals("") && nextWarning.getAffectedElements().get(i) instanceof TypedElement) {
							name = "Unnamed property of type " + ((TypedElement)nextWarning.getAffectedElements().get(i)).getType().getName();
						}
						
						if(i > 0)
							subElementNames += ", ";
						
						subElementNames += name;
					}
				}
			}
			
			if(elementName.equals(""))
				elementName = subElementNames;
			else if(!elementName.equals("") && !subElementNames.equals(""))
				elementName += " / " + subElementNames;
		
			// Generate a data structure
			Object[] data = { "", severity, description, elementName };
			
			// Pass it to the table model
			dataModel.addRow(data);
		}
	}

	/**
	 * @return the warnings
	 */
	public ArrayList<WarningItem> getWarnings() {
		return warnings;
	}

	/**
	 * @param warnings the warnings to set
	 */
	public void setWarnings(ArrayList<WarningItem> warnings) {
		this.warnings = warnings;
	}
}
