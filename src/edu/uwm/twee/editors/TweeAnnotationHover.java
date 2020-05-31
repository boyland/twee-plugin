package edu.uwm.twee.editors;

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;

public class TweeAnnotationHover extends DefaultAnnotationHover {

	public TweeAnnotationHover() {}

	@Override
	protected boolean isIncluded(Annotation annotation) {
		String type = annotation.getType();
		return SugarCubeMacroAnnotation.TYPE.equals(type) ||
				SpellingAnnotation.TYPE.equals(type);
	}

}
