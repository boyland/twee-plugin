package edu.uwm.twee.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.reconciler.Reconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import edu.uwm.eclipse.util.IReconcilingStrategyExtension2;
import edu.uwm.eclipse.util.PositionComparator;
import edu.uwm.eclipse.util.ReconcilerFixed;
import edu.uwm.twee.Activator;

/**
 * Outline page for Twee code.
 * Currently we just list passages.
 * Eventually I'd like to distinguish script and style passages
 * and other special passages.
 */
public class TweeOutline extends ContentOutlinePage {

  public static final String[] SPECIAL_PASSAGE_NAMES = {
      "StoryTitle", "StoryData"
  };
  
  public static enum PassageType {
    NORMAL, SCRIPT, STYLE, SPECIAL;
  }
  
  private static class PassageOutlineElement implements Comparable<PassageOutlineElement> {
    private PassageType fType;
    private String fName;
    private Position fPosition;
    
    PassageOutlineElement(int offset) {
      this(null,null,new Position(offset,0));
    }
    
    PassageOutlineElement(PassageType type, String name, IRegion location) {
      this(type,name,new Position(location.getOffset(),location.getLength()));
    }
    
    PassageOutlineElement(PassageType type, String name, Position position) {
      fType = type;
      fName = name;
      fPosition = position;
    }
    
    public PassageType getType() {
      return fType;
    }
    
    public String getName() {
      return fName;
    }
    
    public Position getPosition() {
      return fPosition;
    }

    @Override
    public int compareTo(PassageOutlineElement arg0) {
      return PositionComparator.getDefault().compare(fPosition,arg0.fPosition);
    }
    
    @Override
    public String toString() {
      return "Passage(" + fName + ")";
    }
  }
  
  private static class MyViewerComparator extends ViewerComparator {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      PassageOutlineElement poe1 = (PassageOutlineElement)e1;
      PassageOutlineElement poe2 = (PassageOutlineElement)e2;
      return poe1.compareTo(poe2);
    }
    
  }
  
  private class MyContentProvider implements ITreeContentProvider {
    protected final static String PASSAGES= "__tw_passages"; //$NON-NLS-1$
    protected IPositionUpdater fPositionUpdater= new DefaultPositionUpdater(PASSAGES);

    private NavigableSet<PassageOutlineElement> passages = new TreeSet<>();
    private IDocument fDocument;
    
    @Override
    public Object[] getElements(Object inputElement) {
      return passages.toArray();
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      return null;
    }

    @Override
    public Object getParent(Object element) {
      return null;
    }

    @Override
    public boolean hasChildren(Object element) {
      return false;
    }
    
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      if (oldInput != null) {
        IDocument document= fDocumentProvider.getDocument(oldInput);
        if (document != null) {
          try {
            document.removePositionCategory(PASSAGES);
          } catch (BadPositionCategoryException x) {
          }
          document.removePositionUpdater(fPositionUpdater);
        }
      }

      passages.clear();

      if (newInput != null) {
        IDocument document= fDocumentProvider.getDocument(newInput);
        if (document != null) {
          document.addPositionCategory(PASSAGES);
          document.addPositionUpdater(fPositionUpdater);
        }
        fDocument = document;
      }
    }
    
    /**
     * Return a region that includes r and everything in the array,
     * assuming that the array is in order.
     * @param r region to possible extend (if null, refers to whole document)
     * @param a array of passage outline elements being added
     * @return region (possibly null)
     */
    private IRegion stretchToInclude(IRegion r, PassageOutlineElement[] a) {
      if (r == null || a.length == 0) return r;
      final int firstOff = a[0].getPosition().getOffset();
      int diffFirst = r.getOffset() - firstOff;
      if (diffFirst > 0) {
        r = new Region(firstOff,r.getLength()+diffFirst);
      }
      PassageOutlineElement poe = a[a.length-1];
      Position pos = poe.getPosition();
      final int lastOff = pos.getOffset() + pos.getLength();
      if (r.getOffset()+r.getLength() < lastOff) {
        return new Region(r.getOffset(),lastOff - r.getOffset());
      }
      return r;
    }
    
    /**
     * Remove all passages from the view that are in the existing region,
     * and replace them with the new passages given. This method
     * can be called from any thread.
     * @param reg region to remove passages from, if null, this means remove all
     * @param add new passages to add
     */
    public void replace(final IRegion reginit, final PassageOutlineElement[] add) {
      final IRegion reg = stretchToInclude(reginit,add);
      Display.getDefault().asyncExec(() -> {
        Set<PassageOutlineElement> toRemove;
        //System.out.println("Replacing " + (reg== null ? "everything" : "[" + reg.getOffset() + ":" + reg.getLength() +"]"));
        //System.out.println("  adding " + Arrays.toString(add));
        if (reg == null) {
          toRemove = passages;
        } else {
          PassageOutlineElement mark = new PassageOutlineElement(reg.getOffset());
          PassageOutlineElement prev = passages.floor(mark);
          if (prev.getPosition().overlapsWith(reg.getOffset(), reg.getLength())) mark = prev;
          toRemove = passages.subSet(mark, new PassageOutlineElement(reg.getOffset()+reg.getLength()));
        }
        Object[] removeArray = toRemove.toArray();
        //System.out.println("  removing " + Arrays.toString(removeArray));
        toRemove.clear();
        passages.addAll(Arrays.asList(add));
        final TreeViewer treeViewer = getTreeViewer();
        treeViewer.remove(removeArray);
        treeViewer.add(treeViewer.getInput(), add);
        if (fDocument != null) {
          try {
            for (Object o : removeArray) {
              PassageOutlineElement poe = (PassageOutlineElement)o;
              fDocument.removePosition(PASSAGES,poe.getPosition());
            }
            for (PassageOutlineElement poe : add) {
              try {
                fDocument.addPosition(PASSAGES, poe.getPosition());
              } catch (BadLocationException e) {
                // muffle
              }
            }
          } catch (BadPositionCategoryException e) {
            e.printStackTrace();
          }
        }
        if (fTextEditor != null) {
          cursorPositionChanged(fTextEditor.getCursorOffset());
        }
      });
    }
    
    /**
     * Return the passage that includes this position 
     * @param offset offset in the document
     * @param inBodyOK whether being in the "body" of a passage counts,
     * or (if false) only if it's in the header.
     * @return passage element for the given position
     */
    public PassageOutlineElement get(int offset, boolean inBodyOK) {
      PassageOutlineElement mark = new PassageOutlineElement(offset);
      PassageOutlineElement poe;
      poe = passages.ceiling(mark);
      if (poe.getPosition().overlapsWith(offset, 0)) return poe;
      poe = passages.floor(mark);
      if (inBodyOK || poe.getPosition().overlapsWith(offset, 0)) return poe;
      return null;
    }
  }
  
  private class MyLabelProvider extends LabelProvider {
    private final Map<PassageType,Image> kindImages = new HashMap<>();

    private void ensureImages() {
      if (kindImages.size() == 0) {
        Activator activator = Activator.getDefault();
        kindImages.put(PassageType.NORMAL,activator.getImage("icons/fourdots-black.png"));
        kindImages.put(PassageType.SCRIPT, activator.getImage("icons/fourdots-blue.png"));
        kindImages.put(PassageType.STYLE, activator.getImage("icons/fourdots-green.png"));
        kindImages.put(PassageType.SPECIAL, activator.getImage("icons/fourdots-red.png"));
      }
    }

    @Override
    public Image getImage(Object element) {
      if (!(element instanceof PassageOutlineElement)) return super.getImage(element);
      ensureImages();
      PassageOutlineElement poe = (PassageOutlineElement)element;
      return kindImages.get(poe.getType());
   }

    @Override
    public String getText(Object element) {
      if (!(element instanceof PassageOutlineElement)) return super.getText(element);
      PassageOutlineElement poe = (PassageOutlineElement)element;
      return poe.getName();
    }
    
  }
  
  private class MyReconcilingStrategy implements 
      IReconcilingStrategy, 
      IReconcilingStrategyExtension, 
      IReconcilingStrategyExtension2 
  {
    private IDocument fDocument;
    private IProgressMonitor fProgressMonitor;
    private MyContentProvider fContentProvider;
    private List<PassageOutlineElement> newElements = new ArrayList<>();

    /// setup
    
    @Override
    public void setProgressMonitor(IProgressMonitor monitor) {
      fProgressMonitor = monitor;
    }
    
    @Override
    public void setDocument(IDocument document) {
      fDocument = document;
    }
    
    public void setContentProvider(MyContentProvider provider) {
      fContentProvider = provider;
    }

    
    /// 
    @Override
    public void initialReconcile() {
      initialReconcile(TweePartitionScanner.TW_PASSAGE);
    }
    
    @Override
    public void initialReconcile(String contentType) {
      if (fContentProvider == null) return;
      if (fProgressMonitor != null) {
        fProgressMonitor.beginTask("Building outline", 12);
      }
      beforeReconcile(null);
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

        int segment = regions.length / 10;
        int count = 0;
        for (ITypedRegion r : regions) {
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
        afterReconcile(null);
        if (fProgressMonitor != null) fProgressMonitor.done();
      }
   }

    @Override
    public void beforeReconcile(DirtyRegion reg) {
      newElements.clear();
    }
    
    @Override
    public void afterReconcile(DirtyRegion reg) {
      if (fContentProvider == null) return;
      // special case because passage header conceptually includes the newline
      // that occurs right before passage but we didn't declare it that way.
      if (reg != null && reg.getLength() > 0) {
        try {
          int lastPos = reg.getOffset() + reg.getLength() - 1;
          if (lastPos+2 < fDocument.getLength() && fDocument.get(lastPos,3).equals("\n::")) {
            ITypedRegion tr = TextUtilities.getPartition(fDocument, IDocumentExtension3.DEFAULT_PARTITIONING, lastPos+1, false);
            if (tr.getType() == TweePartitionScanner.TW_PASSAGE) {
              // System.out.println("going forwards a bit");
              reconcile(tr);
            }
          }
        } catch (BadLocationException e) {
          // ignore
        }
      }
      fContentProvider.replace(reg,newElements.toArray(new PassageOutlineElement[newElements.size()]));
    }

    @Override
    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
      try {
        ITypedRegion tr = TextUtilities.getPartition(fDocument, IDocumentExtension3.DEFAULT_PARTITIONING, subRegion.getOffset(), false);
        // System.out.println("  in reconcile, changed subregion from '" +fDocument.get(subRegion.getOffset(),subRegion.getLength()) + "' to '" + 
        //                    fDocument.get(tr.getOffset(),tr.getLength()) + "'");
        subRegion = tr;
      } catch (BadLocationException e) {
        // muffle
      }
      reconcile(subRegion);
    }
    
    @Override
    public void reconcile(IRegion partition) {
      if (fContentProvider == null) return;
      try {
        String text = fDocument.get(partition.getOffset(),partition.getLength());
        if (text.startsWith("::")) {
          int t = text.indexOf('[');
          int p = text.indexOf('{');
          int e = text.length();
          if (t > 0) e = t;
          if (p > 0 && p < e) e = p;
          String name = text.substring(2, e).trim();
          PassageType type = PassageType.NORMAL;
          if (Arrays.asList(SPECIAL_PASSAGE_NAMES).contains(name)) type = PassageType.SPECIAL;
          if (e < text.length() && text.charAt(e) == '[') {
            while (true) {
              t = e+1;
              while (t < text.length() && text.charAt(t) == ' ') ++t;
              e = t;
              loop: while (e < text.length()) {
                switch (text.charAt(e)) {
                case ' ': case ']': break loop;
                case '\\': ++e; break;
                default: break;
                }
                ++e;
              }
              if (e < text.length()) {
                switch (text.substring(t, e)) {
                case "script": type = PassageType.SCRIPT; break;
                case "stylesheet": type = PassageType.STYLE; break;
                default:
                  break;
                }
                if (text.charAt(e) == ']') break;
              } else break;
            }
          }
          newElements.add(new PassageOutlineElement(type, name, partition));
        }
      } catch (BadLocationException e) {
        
      }
    }
    
  }
  
  protected IEditorInput fInput;
  protected IDocumentProvider fDocumentProvider;
  protected TweeEditor fTextEditor;
  protected ISourceViewer fSourceViewer;
  protected MyReconcilingStrategy fReconcilingStrategy;
  protected IReconciler fReconciler;

  /**
   * Creates a content outline page using the given provider and the given editor.
   * @param provider the document provider
   * @param editor the editor
   */
  public TweeOutline(IDocumentProvider provider, TweeEditor editor, ISourceViewer viewer) {
    super();
    this.fDocumentProvider = provider;
    this.fTextEditor = editor;
    this.fSourceViewer = viewer;
    this.fReconcilingStrategy = new MyReconcilingStrategy();
  }

  @Override
  public void dispose() {
    if (fReconciler != null) {
      fReconciler.uninstall();
      fReconciler = null;
    }
    super.dispose();
  }
  
  /* (non-Javadoc)
   * Method declared on ContentOutlinePage
   */
  @Override
  public void createControl(Composite parent) {

    super.createControl(parent);

    TreeViewer viewer= getTreeViewer();
    MyContentProvider provider = new MyContentProvider();
    viewer.setContentProvider(provider);
    viewer.setLabelProvider(new MyLabelProvider());
    viewer.setComparator(new MyViewerComparator()); 
    viewer.addSelectionChangedListener(this);
    
    fReconcilingStrategy.setContentProvider(provider);

    if (fInput != null)
      viewer.setInput(fInput);
  }

  /**
   * Sets the input of the outline page
   * 
   * @param input the input of this outline page
   */
  public void setInput(IEditorInput input) {
    fInput= input;
    if (fReconciler != null) fReconciler.uninstall();
    Reconciler reconciler = new ReconcilerFixed();
    reconciler.setReconcilingStrategy(fReconcilingStrategy, TweePartitionScanner.TW_PASSAGE);
    reconciler.install(fSourceViewer);
    fReconciler = reconciler;
  }

  private transient boolean reentering = false; // set to true to avoid reacting to changes we generate
  
  /* (non-Javadoc)
   * Method declared on ContentOutlinePage
   */
  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    super.selectionChanged(event);
    if (reentering) return;
    
    ISelection selection= event.getSelection();
    if (selection.isEmpty())
      fTextEditor.resetHighlightRange();
    else {
      PassageOutlineElement element = (PassageOutlineElement) ((IStructuredSelection) selection).getFirstElement();
      int start= element.getPosition().getOffset();
      int length= element.getPosition().getLength();
      try {
        fTextEditor.setHighlightRange(start, length, true);
      } catch (IllegalArgumentException x) {
        fTextEditor.resetHighlightRange();
      }
    }
  }

  /**
   * The cursor position in the editor changed.  Move the selection in the outline.
   * @param cursorOffset offset with the document of the cursor.
   */
  public void cursorPositionChanged(int cursorOffset) {
    if (reentering) return;
    MyContentProvider provider = (MyContentProvider)getTreeViewer().getContentProvider();
    PassageOutlineElement poe = provider.get(cursorOffset,true);
    if (poe != null) {
      reentering = true;
      try {
        getTreeViewer().setSelection(new StructuredSelection(poe));
      } finally {
        reentering = false;
      }
    }
  }
  
  /**
   * Return the passage whose header overlaps the given document position 
   * @param offset position in document
   * @return passage outline element overlapping the given position, or null if no such passage
   */
  public PassageOutlineElement get(int offset) {
    if (getTreeViewer() == null) return null;
    MyContentProvider p = (MyContentProvider)getTreeViewer().getContentProvider();
    return p.get(offset,false);
  }
}
