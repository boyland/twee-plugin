package edu.uwm.eclipse.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;

/**
 * A reconciler that assists in incrementality by
 * using {@link IReconcilingStrategyExtension2}
 * if available.
 */
public class ReconcilerFixed extends org.eclipse.jface.text.reconciler.Reconciler {
	private Set<IReconcilingStrategyExtension2> fStrategies;
	
	public ReconcilerFixed() {
		fStrategies = new HashSet<>();
	}

	@Override
	public void setReconcilingStrategy(IReconcilingStrategy strategy, String contentType) {
		super.setReconcilingStrategy(strategy, contentType);
		if (strategy instanceof IReconcilingStrategyExtension2) {
			fStrategies.add((IReconcilingStrategyExtension2)strategy);
		}
	}
	
	@Override
	protected void initialProcess() {
		ITypedRegion[] regions= computePartitioning(0, getDocument().getLength());
		List<String> contentTypes= new ArrayList<>(regions.length);
		for (ITypedRegion region : regions) {
			String contentType= region.getType();
			if( contentTypes.contains(contentType))
				continue;
			contentTypes.add(contentType);
			IReconcilingStrategy s= getReconcilingStrategy(contentType);
			if (s instanceof IReconcilingStrategyExtension2) {
				IReconcilingStrategyExtension2 e= (IReconcilingStrategyExtension2) s;
				e.initialReconcile(contentType);
			} else if (s instanceof IReconcilingStrategyExtension) {
				IReconcilingStrategyExtension e= (IReconcilingStrategyExtension) s;
				e.initialReconcile();
			}
		}
	}
	
	// should not have been declared "private"
	/**
	 * Computes and returns the partitioning for the given region of the input document
	 * of the reconciler's connected text viewer.
	 *
	 * @param offset the region offset
	 * @param length the region length
	 * @return the computed partitioning
	 * @since 3.0
	 */
	protected ITypedRegion[] computePartitioning(int offset, int length) {
		ITypedRegion[] regions= null;
		try {
			regions= TextUtilities.computePartitioning(getDocument(), getDocumentPartitioning(), offset, length, false);
		} catch (BadLocationException x) {
			regions= new TypedRegion[0];
		}
		return regions;
	}


	@Override
	protected void process(DirtyRegion dirtyRegion) {
		try {
			for (IReconcilingStrategyExtension2 s : fStrategies) {
				s.beforeReconcile(dirtyRegion);
			}
			super.process(dirtyRegion);
		} finally {
			for (IReconcilingStrategyExtension2 s : fStrategies) {
				s.afterReconcile(dirtyRegion);
			}
		}
	}

}
