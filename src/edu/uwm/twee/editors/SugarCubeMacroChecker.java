package edu.uwm.twee.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;

import edu.uwm.eclipse.util.IReconcilingStrategyExtension2;
import edu.uwm.twee.Activator;
import edu.uwm.twee.macro.MacroDictionary;
import edu.uwm.twee.preferences.PreferenceConstants;

/**
 * Reconcile strategy used for spell checking.
 *
 * @since 3.3
 */
public class SugarCubeMacroChecker implements IReconcilingStrategy, IReconcilingStrategyExtension, IReconcilingStrategyExtension2 {

	/**
	 * Problem collector.
	 */
	private class SugarCubeProblemCollector {

		/** Annotation model. */
		private IAnnotationModel fAnnotationModel;

		/** Annotations to add. */
		private Map<Annotation, Position> fAddAnnotations;

		/** Lock object for modifying the annotations. */
		private Object fLockObject;

		/**
		 * Initializes this collector with the given annotation model.
		 *
		 * @param annotationModel the annotation model
		 */
		public SugarCubeProblemCollector(IAnnotationModel annotationModel) {
			Assert.isLegal(annotationModel != null);
			fAnnotationModel= annotationModel;
			if (fAnnotationModel instanceof ISynchronizable)
				fLockObject= ((ISynchronizable)fAnnotationModel).getLockObject();
			else
				fLockObject= fAnnotationModel;
		}

		public void accept(IRegion location, String problem) {
			System.out.println("annotating error: " + problem);
			fAddAnnotations.put(new SugarCubeMacroAnnotation(problem), new Position(location.getOffset(), location.getLength()));
		}

		public void beforeCollecting() {
			fAddAnnotations= new HashMap<>();
		}

		public void afterCollecting(IRegion handled) {
			if (fAddAnnotations == null) return;
			List<Annotation> toRemove= new ArrayList<>();

			synchronized (fLockObject) {
				Iterator<Annotation> iter= fAnnotationModel.getAnnotationIterator();
				while (iter.hasNext()) {
					Annotation annotation= iter.next();
					if (SugarCubeMacroAnnotation.TYPE.equals(annotation.getType())) {
						Position p = fAnnotationModel.getPosition(annotation);
						if (handled == null || p.overlapsWith(handled.getOffset(), handled.getLength())) {
							toRemove.add(annotation);
						}
					}
				}
				Annotation[] annotationsToRemove= toRemove.toArray(new Annotation[toRemove.size()]);

				if (fAnnotationModel instanceof IAnnotationModelExtension)
					((IAnnotationModelExtension)fAnnotationModel).replaceAnnotations(annotationsToRemove, fAddAnnotations);
				else {
					for (Annotation element : annotationsToRemove) {
						fAnnotationModel.removeAnnotation(element);
					}
					for (Entry<Annotation, Position> entry : fAddAnnotations.entrySet()) {
						fAnnotationModel.addAnnotation(entry.getKey(), entry.getValue());
					}
				}
			}

			fAddAnnotations= null;
		}
		
	}


	/** Text content type */
	private static final IContentType TEXT_CONTENT_TYPE= Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);

	/** The text editor to operate on. */
	private ISourceViewer fViewer;

	/** The document to operate on. */
	private IDocument fDocument;

	/** The progress monitor. */
	private IProgressMonitor fProgressMonitor;

	private SugarCubeProblemCollector fCollector;

	/**
	 * Creates a new comment reconcile strategy.
	 *
	 * @param viewer the source viewer
	 */
	public SugarCubeMacroChecker(ISourceViewer viewer) {
		Assert.isNotNull(viewer);
		fViewer= viewer;
	}

	@Override
	public void initialReconcile() {
		initialReconcile(TweePartitionScanner.SC_MACRO);
	}

	@Override
	public void initialReconcile(String contentType) {
		if (!Activator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.P_MACROCHECK))
			return;
		if (fProgressMonitor != null) {
			fProgressMonitor.beginTask("Checking Macro Calls", 13);
		}
		try {
			MacroDictionary.getInstance();
			if (fProgressMonitor != null) {
				fProgressMonitor.worked(1);
			}

			ITypedRegion[] regions;
			try {
				regions = TextUtilities.computePartitioning(fDocument, IDocumentExtension3.DEFAULT_PARTITIONING, 0, fDocument.getLength(), false);
			} catch (BadLocationException e) {
				return;
			}
			if (fProgressMonitor != null) {
				fProgressMonitor.worked(1);
			}
			fCollector.beforeCollecting();

			int segment = regions.length / 10;
			int count = 0;
			for (ITypedRegion r : regions) {
				if (count++ == segment) {
					if (fProgressMonitor != null) {
						fProgressMonitor.worked(1);
					}
				}
				if (contentType.equals(r.getType())) {
					check(r);
				}
			}
			if (fProgressMonitor != null) {
				fProgressMonitor.worked(1);
			}
		} finally {
			fCollector.afterCollecting(null);
			if (fProgressMonitor != null) fProgressMonitor.done();
		}
	}

	@Override
	public void beforeReconcile(DirtyRegion reg) {
		fCollector.beforeCollecting();	
	}

	@Override
	public void afterReconcile(DirtyRegion reg) {
		fCollector.afterCollecting(reg);
	}

	@Override
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {		
		try {
			ITypedRegion tr = TextUtilities.getPartition(fDocument, IDocumentExtension3.DEFAULT_PARTITIONING, subRegion.getOffset(), false);
			subRegion = tr;
		} catch (BadLocationException e) {
			// muffle
		}
		reconcile(subRegion);
	}

	@Override
	public void reconcile(IRegion region) {
		if (!Activator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.P_MACROCHECK) 
				|| getAnnotationModel() == null || fCollector == null) {
			return;
		}
		if (fProgressMonitor != null && fProgressMonitor.isCanceled()) return;
		/*
		try {
			ITypedRegion tr = TextUtilities.getPartition(fDocument, IDocumentExtension3.DEFAULT_PARTITIONING, region.getOffset(), false);
			check(tr);
		} catch (BadLocationException e) {
			// shouldn't happen
			System.err.println("bad region: " + e);
		}*/
		check(region);
	}

	protected void check(IRegion tr) {
		try {
			int length = tr.getLength();
			if (length >= 4) {
				MacroDictionary md = MacroDictionary.getInstance();
				String problem = md.check(fDocument.get(tr.getOffset()+2,length-4));
				if (problem != null) fCollector.accept(tr, problem);
			}
		} catch (BadLocationException ex) {
			// shouldn't happen:
			System.err.println("bad region: " + tr);
		}
	}
	
	/**
	 * Returns the content type of the underlying editor input.
	 *
	 * @return the content type of the underlying editor input or
	 *         <code>null</code> if none could be determined
	 */
	protected IContentType getContentType() {
		return TEXT_CONTENT_TYPE;
	}

	/**
	 * Returns the document which is spell checked.
	 *
	 * @return the document
	 */
	protected final IDocument getDocument() {
		return fDocument;
	}

	@Override
	public void setDocument(IDocument document) {
		fDocument= document;
		fCollector= createSugarCubeProblemCollector();
	}

	/**
	 * Creates a problem collector for problems with sugar cube macros.
	 *
	 * @return the collector or <code>null</code> if none is available
	 */
	protected SugarCubeProblemCollector createSugarCubeProblemCollector() {
		IAnnotationModel model= getAnnotationModel();
		if (model == null)
			return null;
		return new SugarCubeProblemCollector(model);
	}

	@Override
	public final void setProgressMonitor(IProgressMonitor monitor) {
		fProgressMonitor= monitor;
	}

	/**
	 * Returns the annotation model to be used by this reconcile strategy.
	 *
	 * @return the annotation model of the underlying editor input or
	 *         <code>null</code> if none could be determined
	 */
	protected IAnnotationModel getAnnotationModel() {
		return fViewer.getAnnotationModel();
	}

}
