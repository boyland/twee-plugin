package edu.uwm.twee.editors;

import org.eclipse.jface.text.source.Annotation;

/**
 * Problem annotations on sugarcube macro call.
 */
public class SugarCubeMacroAnnotation extends Annotation {
	public static final String TYPE = "edu.uwm.twee.macro";
	
	SugarCubeMacroAnnotation(String text) {
		super(SugarCubeMacroAnnotation.TYPE, false, text);
	}
	
	@Override
	public String toString() {
		return "SugarCubeMacroAnnotation[" +this.getText() + "]";
	}
}