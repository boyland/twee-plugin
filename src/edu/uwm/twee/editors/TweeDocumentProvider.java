package edu.uwm.twee.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

public class TweeDocumentProvider extends FileDocumentProvider {

	@Override
	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner = new FastPartitioner(new TweePartitionScanner(),
					new String[] { 
							TweePartitionScanner.XML_TAG, 
							TweePartitionScanner.SC_TAG, 
							TweePartitionScanner.SC_LINK,
							TweePartitionScanner.SC_CODE,
							TweePartitionScanner.SC_HEADER,
							TweePartitionScanner.TW_PASSAGE,
							TweePartitionScanner.XML_COMMENT });
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
		return document;
	}
}