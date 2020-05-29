package edu.uwm.twee.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.Reconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.spelling.SpellingReconcileStrategy;
import org.eclipse.ui.texteditor.spelling.SpellingService;
import edu.uwm.eclipse.util.ColorManager;
import edu.uwm.eclipse.util.NonRuleBasedDamagerRepairer;

public class TweeConfiguration extends SourceViewerConfiguration {
	private XMLDoubleClickStrategy doubleClickStrategy;
	private XMLTagScanner tagScanner;
	private SCTagScanner sctagScanner;
	private XMLScanner scanner;
	private ColorManager colorManager;
	private TWPassageScanner twPassageScanner;

	public TweeConfiguration(ColorManager colorManager) {
		this.colorManager = colorManager;
	}

	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			TweePartitionScanner.XML_COMMENT,
			TweePartitionScanner.JS_COMMENT,
			TweePartitionScanner.TW_PASSAGE,
			TweePartitionScanner.SC_HEADER,
			TweePartitionScanner.SC_CODE,
			TweePartitionScanner.SC_LINK,
			TweePartitionScanner.SC_MACRO,
			TweePartitionScanner.XML_TAG};
	}

	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,
		String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new XMLDoubleClickStrategy();
		return doubleClickStrategy;
	}

	protected XMLScanner getXMLScanner() {
		if (scanner == null) {
			scanner = new XMLScanner(colorManager);
			scanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(ITweeColorConstants.DEFAULT))));
		}
		return scanner;
	}
	protected XMLTagScanner getXMLTagScanner() {
		if (tagScanner == null) {
			tagScanner = new XMLTagScanner(colorManager);
			tagScanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(ITweeColorConstants.XML_TAG))));
		}
		return tagScanner;
	}
	protected SCTagScanner getSCTagScanner() {
		if (sctagScanner == null) {
			sctagScanner = new SCTagScanner(colorManager);
			sctagScanner.setDefaultReturnToken(
					new Token(
							new TextAttribute(
									colorManager.getColor(ITweeColorConstants.SC_MACRO))));
		}
		return sctagScanner;
	}
	protected TWPassageScanner getTWPassageScanner() {
		if (twPassageScanner == null) {
			twPassageScanner = new TWPassageScanner(colorManager);
			twPassageScanner.setDefaultReturnToken(
					new Token(
							new TextAttribute(colorManager.getColor(ITweeColorConstants.SC_LINK), null, SWT.BOLD)));
		}
		return twPassageScanner;
	}

	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		SpellingService spellingService= EditorsUI.getSpellingService();
		IReconcilingStrategy strategy= new SpellingReconcileStrategy(sourceViewer, spellingService);
		Reconciler reconciler = new Reconciler();
		reconciler.setDocumentPartitioning(this.getConfiguredDocumentPartitioning(sourceViewer));
		reconciler.setReconcilingStrategy(strategy, TweePartitionScanner.SC_HEADER);
		reconciler.setReconcilingStrategy(strategy, IDocument.DEFAULT_CONTENT_TYPE);
		return reconciler;
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getXMLTagScanner());
		reconciler.setDamager(dr, TweePartitionScanner.XML_TAG);
		reconciler.setRepairer(dr, TweePartitionScanner.XML_TAG);

		dr = new DefaultDamagerRepairer(getSCTagScanner());
		reconciler.setDamager(dr, TweePartitionScanner.SC_MACRO);
		reconciler.setRepairer(dr, TweePartitionScanner.SC_MACRO);
		
		dr = new DefaultDamagerRepairer(getTWPassageScanner());
		reconciler.setDamager(dr, TweePartitionScanner.TW_PASSAGE);
		reconciler.setRepairer(dr, TweePartitionScanner.TW_PASSAGE);
		
		dr = new DefaultDamagerRepairer(getXMLScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		NonRuleBasedDamagerRepairer ndr = new NonRuleBasedDamagerRepairer(
				new TextAttribute(colorManager.getColor(ITweeColorConstants.XML_COMMENT)));
		reconciler.setDamager(ndr, TweePartitionScanner.XML_COMMENT);
		reconciler.setRepairer(ndr, TweePartitionScanner.XML_COMMENT);

		ndr = new NonRuleBasedDamagerRepairer(
				new TextAttribute(colorManager.getColor(ITweeColorConstants.JS_COMMENT)));
		reconciler.setDamager(ndr, TweePartitionScanner.JS_COMMENT);
		reconciler.setRepairer(ndr, TweePartitionScanner.JS_COMMENT);

		ndr = new NonRuleBasedDamagerRepairer(
				new TextAttribute(colorManager.getColor(ITweeColorConstants.SC_CODE)));
		reconciler.setDamager(ndr, TweePartitionScanner.SC_CODE);
		reconciler.setRepairer(ndr, TweePartitionScanner.SC_CODE);

		ndr = new NonRuleBasedDamagerRepairer(
				new TextAttribute(colorManager.getColor(ITweeColorConstants.SC_LINK)));
		reconciler.setDamager(ndr, TweePartitionScanner.SC_LINK);
		reconciler.setRepairer(ndr, TweePartitionScanner.SC_LINK);

		ndr = new NonRuleBasedDamagerRepairer(
				new TextAttribute(null, null, SWT.BOLD));
		reconciler.setDamager(ndr, TweePartitionScanner.SC_HEADER);
		reconciler.setRepairer(ndr, TweePartitionScanner.SC_HEADER);
		
		return reconciler;
	}

}