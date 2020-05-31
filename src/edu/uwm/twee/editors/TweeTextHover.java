package edu.uwm.twee.editors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;

public class TweeTextHover extends DefaultTextHover {

	public TweeTextHover(ISourceViewer sourceViewer) {
		super(sourceViewer);
	}

	@Override
	protected boolean isIncluded(Annotation annotation) {
		String type = annotation.getType();
		return SugarCubeMacroAnnotation.TYPE.equals(type) ||
				SpellingAnnotation.TYPE.equals(type);
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		IDocument doc = textViewer.getDocument();
		try {
			return TextUtilities.getPartition(doc, IDocumentExtension3.DEFAULT_PARTITIONING, offset, false);
		} catch (BadLocationException e) {
			return null;
		}
	}

}
