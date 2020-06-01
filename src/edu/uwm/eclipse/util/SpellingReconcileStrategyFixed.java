package edu.uwm.eclipse.util;

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
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingService;

/**
 * A spelling reconciler that can work in a single partition.
 * It also handled incrementality correctly.
 * This fix was not able to be accomplished with sub-classing because
 * the original class uses some final methods and most fields are private.
 */
public class SpellingReconcileStrategyFixed 
	implements IReconcilingStrategy, IReconcilingStrategyExtension, IReconcilingStrategyExtension2
{

	/**
	 * Spelling problem collector.
	 */
	protected class SpellingProblemCollector implements ISpellingProblemCollector {

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
		public SpellingProblemCollector(IAnnotationModel annotationModel) {
			Assert.isLegal(annotationModel != null);
			fAnnotationModel= annotationModel;
			if (fAnnotationModel instanceof ISynchronizable)
				fLockObject= ((ISynchronizable)fAnnotationModel).getLockObject();
			else
				fLockObject= fAnnotationModel;
		}

		@Override
		public void accept(SpellingProblem problem) {
			fAddAnnotations.put(new SpellingAnnotation(problem), new Position(problem.getOffset(), problem.getLength()));
		}

		@Override
		public void beginCollecting() {
			if (fAddAnnotations == null) beforeCollecting(null);
		}

		@Override
		public void endCollecting() {
			// do nothing
		}
		
		public void beforeCollecting(DirtyRegion reg) {
			fAddAnnotations= new HashMap<>();
		}
		
		public void afterCollecting(DirtyRegion reg) {

			List<Annotation> toRemove= new ArrayList<>();

			synchronized (fLockObject) {
				Iterator<Annotation> iter= fAnnotationModel.getAnnotationIterator();
				while (iter.hasNext()) {
					Annotation annotation= iter.next();
					if (SpellingAnnotation.TYPE.equals(annotation.getType()) &&
							(reg == null || fAnnotationModel.getPosition(annotation).overlapsWith(reg.getOffset(), reg.getLength())))
						toRemove.add(annotation);
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

	private SpellingService fSpellingService;

	private SpellingProblemCollector fCollector;

	/** The spelling context containing the Java source content type. */
	private SpellingContext fSpellingContext;

	/**
	 * Region array, used to prevent us from creating a new array on each reconcile pass.
	 * @since 3.4
	 */
	private IRegion[] fRegions= new IRegion[1];
	
	public SpellingReconcileStrategyFixed(ISourceViewer viewer, SpellingService spellingService) {
		Assert.isNotNull(viewer);
		Assert.isNotNull(spellingService);
		fViewer= viewer;
		fSpellingService= spellingService;
		fSpellingContext= new SpellingContext();
		fSpellingContext.setContentType(getContentType());
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
		fCollector= createSpellingProblemCollector();
	}
	
	protected SpellingProblemCollector createSpellingProblemCollector() {
		IAnnotationModel model= getAnnotationModel();
		if (model == null)
			return null;
		return new SpellingProblemCollector(model);
	}
	
	@Override
	public void setProgressMonitor(IProgressMonitor pm) {
		fProgressMonitor = pm;
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

	@Override
	public void initialReconcile() {
		System.out.println("Calling bad initialReconcile");
		try {
			if (fCollector != null) fCollector.beginCollecting();
			reconcile(new Region(0,getDocument().getLength()));
		} finally {
			if (fCollector != null) fCollector.afterCollecting(null);
		}
	}
	
	@Override
	public void initialReconcile(String contentType) {
		System.out.println("Calling spelling initialReconcile(" + contentType + ")");
		if (fProgressMonitor != null) {
			fProgressMonitor.beginTask("Checking Spelling", 12);
		}
		try {
			ITypedRegion[] regions;
			try {
				regions = TextUtilities.computePartitioning(fDocument, IDocumentExtension3.DEFAULT_PARTITIONING, 0, fDocument.getLength(), false);
			} catch (BadLocationException e) {
				return;
			}
			if (fProgressMonitor != null) {
				fProgressMonitor.worked(1);
			}
			fCollector.beforeCollecting(null);

			int segment = regions.length / 10;
			int count = 0;
			for (ITypedRegion r : regions) {
				if (fProgressMonitor != null && fProgressMonitor.isCanceled()) return;
				if (count++ == segment) {
					if (fProgressMonitor != null) {
						fProgressMonitor.worked(1);
					}
				}
				if (contentType.equals(r.getType())) {
					reconcile(r);
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
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		// problem: region may include less than a whole word
		// So we either use the typed region around the sub region, or else
		// use entire lines.
		try {
			IRegion startLineInfo= fDocument.getLineInformationOfOffset(subRegion.getOffset());
			IRegion endLineInfo= fDocument.getLineInformationOfOffset(subRegion.getOffset() + Math.max(0, subRegion.getLength() - 1));
			ITypedRegion tr = TextUtilities.getPartition(fDocument, IDocumentExtension3.DEFAULT_PARTITIONING, subRegion.getOffset(), false);
			IRegion lineRegion;
			if (startLineInfo.getOffset() == endLineInfo.getOffset())
				lineRegion= startLineInfo;
			else
				lineRegion= new Region(startLineInfo.getOffset(), endLineInfo.getOffset() + Math.max(0, endLineInfo.getLength() - 1) - startLineInfo.getOffset());
			// choose the smaller of tr and lineRegion
			// or rather, choose tr unless it includes the entire lineRegion
			if (tr.getOffset() <= lineRegion.getOffset() && 
				tr.getOffset() + tr.getLength() >= lineRegion.getOffset() + lineRegion.getLength()) {
				subRegion = lineRegion;
			} else {
				subRegion = tr;
			}
		} catch (BadLocationException e) {
			// shouldn't happen
			subRegion = dirtyRegion;
		}

		reconcile(subRegion);
	}

	@Override
	public void reconcile(IRegion region) {
		if (getAnnotationModel() == null || fCollector == null)
			return;

		fRegions[0]= region;
		fSpellingService.check(fDocument, fRegions, fSpellingContext, fCollector, fProgressMonitor);
	}

	@Override
	public void beforeReconcile(DirtyRegion reg) {
		fCollector.beforeCollecting(reg);	
	}

	@Override
	public void afterReconcile(DirtyRegion reg) {
		fCollector.afterCollecting(reg);
	}

}
