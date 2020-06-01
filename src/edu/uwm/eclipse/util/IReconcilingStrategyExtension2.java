package edu.uwm.eclipse.util;

import org.eclipse.jface.text.reconciler.DirtyRegion;

/**
 * A reconciling strategy that wants to be
 * informed before and after the reconcile calls occur
 * for a particular dirty region.
 * For example, if it generates annotations, then all old annotations
 * for this region can be purged, and all new ones added.
 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy
 * @author boyland
 */
public interface IReconcilingStrategyExtension2 {

	/**
	 * Perform the initial reconcile for this content type.
	 * @param contentType content type to check in the partition
	 * of the document.
	 */
	public void initialReconcile(String contentType);
	
	/**
	 * Called before the first call to {@link org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile} 
	 * @param reg dirty region being reconciled, if null this refers to the entire document
	 */
	public void beforeReconcile(DirtyRegion reg);
	
	/**
	 * Called before the last call to {@link org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile} 
	 * @param reg dirty region being reconciled, if null this refers to the entire document
	 */
	public void afterReconcile(DirtyRegion reg);
}
