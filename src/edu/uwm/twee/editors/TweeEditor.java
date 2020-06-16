package edu.uwm.twee.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import edu.uwm.eclipse.util.ColorManager;

public class TweeEditor extends TextEditor {

	private ColorManager colorManager;
  private TweeOutline fOutlinePage;

	public TweeEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new TweeConfiguration(colorManager));
		setDocumentProvider(new TweeDocumentProvider());
	}
	

  public TweeOutline getOutline() {
    if (fOutlinePage == null) {
      ISourceViewer sourceViewer = getSourceViewer();
      if (sourceViewer != null) {
        fOutlinePage= new TweeOutline(getDocumentProvider(), this, sourceViewer);
        if (getEditorInput() != null)
          fOutlinePage.setInput(getEditorInput());
      } 
    }
    return fOutlinePage;    
  }

  @Override
  public <T> T getAdapter(Class<T> adapter) {
	  if (adapter.equals(IContentOutlinePage.class)) {
	    @SuppressWarnings("unchecked")
	    T outline = (T)getOutline();
      return outline;
	  }
    return super.getAdapter(adapter);
  }

  @Override
  protected void doSetInput(IEditorInput input) throws CoreException {
    super.doSetInput(input);
    if (fOutlinePage != null) fOutlinePage.setInput(input);
  }

  /**
   * Return the cursor position as an offset within the document.
   * (Why isn't this standard?)
   * @return offset with the document of the editor "caret".
   * @see #getCursorPosition()
   */
  protected int getCursorOffset() {
    final ISourceViewer sourceViewer = getSourceViewer();
    StyledText styledText= sourceViewer.getTextWidget();
    return widgetOffset2ModelOffset(sourceViewer, styledText.getCaretOffset());
  }
  
  @Override
  protected void handleCursorPositionChanged() {
    super.handleCursorPositionChanged();
    if (fOutlinePage != null) {
      fOutlinePage.cursorPositionChanged(this.getCursorOffset());
    }
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
		if (fOutlinePage != null) {
		  fOutlinePage.dispose();
		  fOutlinePage = null;
		}
		super.dispose();
	}

}
