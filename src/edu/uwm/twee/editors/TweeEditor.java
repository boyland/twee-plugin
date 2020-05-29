package edu.uwm.twee.editors;

import org.eclipse.ui.editors.text.TextEditor;

import edu.uwm.eclipse.util.ColorManager;

public class TweeEditor extends TextEditor {

	private ColorManager colorManager;

	public TweeEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new TweeConfiguration(colorManager));
		setDocumentProvider(new TweeDocumentProvider());
	}
	
	@Override
	protected boolean getInitialWordWrapStatus() {
		return true;
	}

	@Override
	protected boolean isLineNumberRulerVisible() {
		return true;
	}

	@Override
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

}
