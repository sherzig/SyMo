/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.ui.browser.ContainmentTree;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceValue;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.MultiplicityElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Slot;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;
import com.phoenix_int.ModelCenter.Array;
import com.phoenix_int.ModelCenter.BooleanArray;
import com.phoenix_int.ModelCenter.DoubleArray;
import com.phoenix_int.ModelCenter.IntegerArray;
import com.phoenix_int.ModelCenter.StringArray;
import com.phoenix_int.ModelCenter.Variable;
import com.phoenix_int.ModelCenter.Variant;

/**
 * @author Sebastian
 *
 */
public class InstanceHandler {

	private Tree tree_ = null;
	private ArrayList<Slot> resetSlots_ = null;
	
	/**
	 * 
	 */
	public InstanceHandler() {
		resetSlots_ = new ArrayList<Slot>();
	}
	
	/**
	 * 
	 */
	public void clearResetSlotsList() {
		resetSlots_.clear();
	}
	
	/**
	 * 
	 * @param e
	 * @return
	 */
	public void setInstanceValuesForElement(Element e, InstanceSpecification instanceSpec, InstanceSpecification rootInstanceSpec, ArrayList<ArrayList<Variant>> values) {
		goThroughInstanceSpecAndSetValuesForElement(e, instanceSpec, rootInstanceSpec, values.iterator());
	}
	
	/**
	 * 
	 * @param e
	 * @param selectedElement
	 * @return
	 */
	private void goThroughInstanceSpecAndSetValuesForElement(Element e, InstanceSpecification selectedElement, InstanceSpecification rootInstanceSpec, Iterator<ArrayList<Variant>> valueIterator) {
		InstanceSpecification selectedInstance = (InstanceSpecification)rootInstanceSpec;
		
		// Travers through the slots
		for(Iterator<Slot> slotIter = selectedInstance.getSlot().iterator(); slotIter.hasNext(); ) {
			// Slots can contain:
			// 1) Instances of value properties (usually a literal string)
			// 2) Instance values as reference to other instance specifications
			Slot nextSlot = slotIter.next();
			
			// Now go through values of slot
			if(nextSlot.hasValue()) {
				if(nextSlot.getDefiningFeature() == e) {
					if(valueIterator.hasNext()) {
						fillSlotWithValues(nextSlot, valueIterator.next());
				    }
				}
				else {
					List<ValueSpecification> value = nextSlot.getValue();
					
					boolean contained = false;
					
					for(int i=0; i<value.size(); i++)
						if(value.get(i) instanceof InstanceValue)
							if(((InstanceValue)value.get(i)).getInstance() == selectedElement)
								contained = true;
					
					if(!contained) {
						// Iterate through all values of a slot
						for(int i=0; i<value.size(); i++) {
					        ValueSpecification valueSpecification = value.get(i);
					        
					        // Check whether a value was found or a part
					        if(valueSpecification instanceof InstanceValue) {
					        	// Subsystem that can contain more elements
					        	InstanceValue instanceVal = (InstanceValue)valueSpecification;
					        	
					        	goThroughInstanceSpecAndSetValuesForElement(e, selectedElement, instanceVal.getInstance(), valueIterator); // Gives back instance specification
					        }
						}
					}
					else {
						goThroughInstanceSpecAndSetValuesForElement(e, selectedElement, selectedElement, valueIterator); // Gives back instance specification
					}
				}
			}
		}
	}
	
	/**
	 * Checks whether a specific slotäs values have already been reset
	 * 
	 * @param slot
	 * @return
	 */
	private boolean hasBeenReset(Slot slot) {
		if(resetSlots_.contains(slot))
			return true;
		
		return false;
	}
	
	/**
	 * Fills a specific slot with a list of values
	 * 
	 * @param slotToFill
	 * @param values
	 */
	public void fillSlotWithValues(Slot slotToFill, ArrayList<Variant> values) {
		List<ValueSpecification> value = slotToFill.getValue();
		
		ArrayList<Variant> newValues = values;
		
		if(!hasBeenReset(slotToFill))
			value.clear();
		
		resetSlots_.add(slotToFill);
		
		int valuesToAdd = newValues.size();
		// Get the amount of values of the slot previously to adding the new values
		int prevSize = value.size();
		int numSet = 0;
		
		// Get the number of values that were "set" already (i.e. non-empty values)
		for(int i=0; i<prevSize; i++)
			if(value.get(i) != null)
				if(!ModelHelper.getValueSpecificationValue(value.get(i)).equals(""))
					numSet++;
		
		// Check out the multiplicity of the variable that we are setting and set accordingly
		try {
			// Get the multiplicity
			String multiplicity = ModelHelper.getMultiplicity((MultiplicityElement)slotToFill.getDefiningFeature());
			int lowerBound = -1;
			int upperBound = -1;
			
			// Interpret the multiplicity and set values accordingly
    		if(multiplicity == null || multiplicity.equals("") || multiplicity.equals("1"))
    			valuesToAdd = 1;
    		else if(multiplicity.equals("0"))
    			valuesToAdd = 0;
    		else if(multiplicity.contains("..")) {
    			lowerBound = Integer.parseInt(multiplicity.substring(0, multiplicity.indexOf("..")));
    			upperBound = -1;
    			
    			// Get the upper bound
    			String upperBoundString = multiplicity.substring((multiplicity.lastIndexOf("..")) + 2);
    			
    			// Set upper bound
    			if(!upperBoundString.contains("*"))
    				upperBound = Integer.parseInt(upperBoundString);
    			else {
    				upperBound = newValues.size() + numSet;
    				
    				valuesToAdd = upperBound;
    			}
    			
    			// Adjust the number of values to add according to upper and lower bound
        		if(valuesToAdd < lowerBound)
        			valuesToAdd = lowerBound;
        		else if(!upperBoundString.contains("*") && (valuesToAdd + numSet) > upperBound)
        			valuesToAdd = upperBound;
        		else if(!upperBoundString.contains("*"))
        			valuesToAdd += numSet;
    		}
    		else if(!multiplicity.contains("*")) {
    			valuesToAdd = Integer.parseInt(multiplicity);
    		}
		}
		catch(Exception e1) {
			// Something went wrong with formatting, never mind
			valuesToAdd = newValues.size();
		}
	
		// Iterate through all values of a slot
		for(int i=0; i<(valuesToAdd - numSet); i++) {
			// Add a value specification to the current slot
			// TODO: Handle different types here
			if((i + prevSize) < valuesToAdd)
				value.add(Application.getInstance().getProject().getElementsFactory().createLiteralStringInstance());
			
			// Get the value specification instance
	        ValueSpecification valueSpecification = value.get(numSet + i);

	        if(i < newValues.size()) {
		        // Now get the value to set
        		Variant valueToSet = newValues.get(i);
	        	
        		// Update the value
	        	ModelHelper.setValueSpecificationValue(valueSpecification, valueToSet);
	        }
		}
	}
	
	/**
	 * Finds instance values within a given instance hierarchy for a given element
	 * 
	 * @param e
	 * @return
	 */
	public ArrayList<Variant> findInstanceValuesForElement(Element e, InstanceSpecification instanceSpec) {
		return goThroughInstanceSpecAndReturnValuesForElement(e, instanceSpec);
	}
	
	/**
	 * Iteratively search for instance values for a specific element within a given instance hierachy
	 * 
	 * @param e
	 * @param selectedElement
	 * @return
	 */
	private ArrayList<Variant> goThroughInstanceSpecAndReturnValuesForElement(Element e, InstanceSpecification selectedElement) {
		ArrayList<Variant> values = new ArrayList<Variant>();
		InstanceSpecification selectedInstance = (InstanceSpecification)selectedElement;
		
		// Travers through the slots
		for(Iterator<Slot> slotIter = selectedInstance.getSlot().iterator(); slotIter.hasNext(); ) {
			// Slots can contain:
			// 1) Instances of value properties (usually a literal string)
			// 2) Instance values as reference to other instance specifications
			// TODO: Array of literal strings
			Slot nextSlot = slotIter.next();
			
			// Now go through values of slot
			if(nextSlot.hasValue()) {
				List<ValueSpecification> value = nextSlot.getValue();
				
				// Get the next slot value for this defining feature - this supports composition
	        	// and aggregation!
				System.out.println("Searching for e: " + ((NamedElement)e).getQualifiedName() + " and found " + nextSlot.getDefiningFeature().getQualifiedName());
				
				// TODO: Search among redefined elements needs to be extended!!!
	        	if(nextSlot.getDefiningFeature() == e || nextSlot.getDefiningFeature().getRedefinedElement().contains(e)) {
	        		values.addAll(extractValuesFromSlotValues(e, value));
	        	}
	        	else {
					// Otheriwse just iterate through values and see whether we can find property values
					for(int i=0; i<value.size(); i++) {
				        ValueSpecification valueSpecification = value.get(i);
				        
				        // Check whether a value was found or a part
				        if(valueSpecification instanceof InstanceValue) {
				        	// Subsystem that can contain more elements
				        	InstanceValue instanceVal = (InstanceValue)valueSpecification;
				        	
				        	values.addAll(goThroughInstanceSpecAndReturnValuesForElement(e, instanceVal.getInstance())); // Gives back instance specification
				        }
				    }
	        	}
			}
		}
		
		return values;
	}
	
	/**
	 * 
	 * @param e The defining feature that corresponds to the slot
	 * @param value The values of the slot
	 * @return
	 */
	public ArrayList<Variant> extractValuesFromSlotValues(Element e, List<ValueSpecification> value) {
		ArrayList<Variant> values = new ArrayList<Variant>();
		
		// Assume single value
		ValueSpecification valueSpecification = value.get(0);
		
		// Now fetch all values connected to this slot feature
		String stringValue = ModelHelper.getValueString(valueSpecification);
		String multiplicity = ModelHelper.getMultiplicity((MultiplicityElement)e);
		String typeName = "";
		
		if(stringValue.equals("")) {
			stringValue = "0";
		}
		
		if(((Property)e).getType() != null && ((Property)e).getType().getName() != null)
			typeName = ((Property)e).getType().getName().toLowerCase();
		
		// Check whether we have a single value
		if(multiplicity == null || multiplicity.equals("") || multiplicity.equals("1")) {
			// Handle a single value
			if(typeName.contains("string") || typeName.contains("char"))
				values.add(new Variant(Variant.STRING, stringValue));
			else if(typeName.contains("int") || typeName.contains("integer") || typeName.equals("long"))	// Covers int32, etc.
				values.add(new Variant(Variant.LONG, new Long(stringValue)));
			else if(typeName.equals("boolean") || typeName.equals("$ocl_boolean"))
				values.add(new Variant(Variant.BOOLEAN, new Integer(stringValue)));
			else	// Float, Real, etc...
				values.add(new Variant(Variant.DOUBLE, new Double(stringValue)));
		}
		else if(multiplicity != "0") {
			Variant newValue = null;
			String[] stringArray = new String[value.size()];
			long[] longArray = new long[value.size()];
			boolean[] booleanArray = new boolean[value.size()];
			double[] doubleArray = new double[value.size()];
			
			// Handle multiplicity, i.e. arrays of values
			for(int i=0; i<value.size(); i++) {
		        valueSpecification = value.get(i);
		        stringValue = ModelHelper.getValueString(valueSpecification);
		        
		        if(stringValue.equals(""))
		        	stringValue = "0";
		        
		        // TODO: Catch number format exception and report to user
    			if(typeName.contains("string") || typeName.contains("char"))
    				stringArray[i] = new String(stringValue);
    			else if(typeName.contains("int") || typeName.contains("integer") || typeName.equals("long"))	// Covers int32, etc.
    				longArray[i] = new Long(stringValue).longValue();
    			else if(typeName.equals("boolean") || typeName.equals("$ocl_boolean"))
    				booleanArray[i] = new Boolean(stringValue).booleanValue();
    			else	// Float, Real, etc...
    				doubleArray[i] = new Double(stringValue).doubleValue();
			}
			
			if(typeName.contains("string") || typeName.contains("char"))
				newValue = new Variant(Variant.STRING_ARRAY, stringArray);
			else if(typeName.contains("int") || typeName.contains("integer") || typeName.equals("long"))	// Covers int32, etc.
				newValue = new Variant(Variant.LONG_ARRAY, longArray);
			else if(typeName.equals("boolean") || typeName.equals("$ocl_boolean"))
				newValue = new Variant(Variant.BOOLEAN_ARRAY, booleanArray);
			else	// Float, Real, etc...
				newValue = new Variant(Variant.DOUBLE_ARRAY, doubleArray);
			
			// Finally add the array of values to the list of values associated with this input
			// to a ModelCenter model
			// Note: individual values can have a multiplicity and if there are multiple
			// values going into a port that each have multiple values, then ModelCenter
			// should expect a matrix
			values.add(newValue);
		}
		
		return values;
	}
	
	/**
	 * Returns the containment tree
	 * 
	 * @return
	 */
	public Tree getTree() {
		return this.tree_;
	}
	
	/**
	 * Sets the containment tree
	 * 
	 * @return
	 */
	public void setTree(Tree tree) {
		this.tree_ = tree;
	}
	
}
