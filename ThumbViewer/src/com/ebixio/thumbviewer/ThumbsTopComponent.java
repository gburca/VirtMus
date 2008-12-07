/*
 * ThumbsTopComponent.java
 * 
 * Copyright (C) 2006-2007  Gabriel Burca (gburca dash virtmus at ebixio dot com)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.ebixio.thumbviewer;

import com.ebixio.virtmus.DraggableThumbnail;
import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.Song;
import com.ebixio.virtmus.Thumbnail;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Vector;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.swingfx.jdraggable.DragPolicy;
import net.java.swingfx.jdraggable.DraggableManager;
import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Top component which displays something.
 */
final class ThumbsTopComponent extends TopComponent implements MouseListener {
    
    private static ThumbsTopComponent instance;
    /** path to the icon used by the component and its open action */
    static final String ICON_PATH = "com/ebixio/thumbviewer/image-x-generic.png";
    
    private static final String PREFERRED_ID = "ThumbsTopComponent";
    
    private Song loadedSong = null;
    private DraggableManager draggableManager;
    private int hgap = 25, vgap = 25;

    transient private final PropertyChangeListener eListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                //Node[] selectedNodes = (Node[]) evt.getNewValue();
                updateSelection();
            }
        }
    };
    
    private ChangeListener changeListener = new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
            if (e.getSource().getClass() == Song.class) loadSong((Song)e.getSource());
        }
    };
    
    private ThumbsTopComponent() {
        initComponents();
        setName(NbBundle.getMessage(ThumbsTopComponent.class, "CTL_ThumbsTopComponent"));
        setToolTipText(NbBundle.getMessage(ThumbsTopComponent.class, "HINT_ThumbsTopComponent"));
        setIcon(ImageUtilities.loadImage(ICON_PATH, true));
        
        draggableManager = new ThumbnailDraggableManager(jPanel);
        draggableManager.setDragPolicy(DragPolicy.STRICT);
        
        // 1 wheel scroll = 3 clicks on the scoll bar arrow
        jScrollPane.getVerticalScrollBar().setUnitIncrement((MusicPage.thumbH + vgap) / 3);
        layoutThumbs();
    }
  
    private void layoutThumbs() {
        jPanel.setLayout(new ModifiedFlowLayout(FlowLayout.CENTER, hgap, vgap));
        jPanel.validate(); // Forces the component to re-layout subcomponents.
    }

   
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane = new javax.swing.JScrollPane();
        jPanel = new javax.swing.JPanel();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jScrollPane.setBackground(new java.awt.Color(255, 255, 255));
        jScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane.setViewportBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        jPanel.setBackground(new java.awt.Color(255, 255, 255));

        org.jdesktop.layout.GroupLayout jPanelLayout = new org.jdesktop.layout.GroupLayout(jPanel);
        jPanel.setLayout(jPanelLayout);
        jPanelLayout.setHorizontalGroup(
            jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 398, Short.MAX_VALUE)
        );
        jPanelLayout.setVerticalGroup(
            jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 531, Short.MAX_VALUE)
        );

        jScrollPane.setViewportView(jPanel);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 410, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 553, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        layoutThumbs();
    }//GEN-LAST:event_formComponentResized
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel;
    private javax.swing.JScrollPane jScrollPane;
    // End of variables declaration//GEN-END:variables
    
    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link findInstance}.
     */
    public static synchronized ThumbsTopComponent getDefault() {
        if (instance == null) {
            instance = new ThumbsTopComponent();
        }
        return instance;
    }
    
    /**
     * Obtain the ThumbsTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized ThumbsTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            ErrorManager.getDefault().log(ErrorManager.WARNING,
                    "Cannot find MyWindow component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof ThumbsTopComponent) {
            return (ThumbsTopComponent)win;
        }
        ErrorManager.getDefault().log(ErrorManager.WARNING,
                "There seem to be multiple components with the '" + PREFERRED_ID +
                "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }
    
    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }
    
    public ExplorerManager getExplorerManager() {
        return MainApp.findInstance().getExplorerManager();
    }
    
    @Override
    public void addNotify() {
        getExplorerManager().addPropertyChangeListener(eListener);
        super.addNotify();
    }
    @Override
    public void removeNotify() {
        super.removeNotify();
        getExplorerManager().removePropertyChangeListener(eListener);
    }
    
    /** This function gets called every time the ExplorerManager selection changes */
    private void updateSelection() {
        Node[] nodes = getExplorerManager().getSelectedNodes();
        if (nodes.length > 0) {
            Lookup l = nodes[0].getLookup();

            Collection songs = l.lookupResult(Song.class).allInstances();
            if (!songs.isEmpty()) {
                Song s = (Song) songs.iterator().next();
                if (loadedSong != s) this.loadSong(s);
            }
            
            Collection pages = l.lookupResult(MusicPage.class).allInstances();
            if (!pages.isEmpty()) {
                MusicPage mp = (MusicPage) pages.iterator().next();
                for (MusicPage m: loadedSong.pageOrder) {
                    if (m == mp) {
                        m.getThumbnail().setSelected(true);
                    } else {
                        m.getThumbnail().setSelected(false);
                    }
                }                
            }
        }        
    }
    
    /** replaces this in object stream */
    @Override
    public Object writeReplace() {
        return new ResolvableHelper();
    }
    
    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }
    
    // <editor-fold defaultstate="collapsed" desc=" MouseListener interface ">
    public void mouseClicked(MouseEvent e) {
    }
    
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            Thumbnail t = (Thumbnail)e.getComponent();
            // Make this a key listener, and if ctrl/shift is not held down deselect all others
            for (Component c: jPanel.getComponents()) {
                ((Thumbnail)c).setSelected(false);
            }
            t.setSelected(true);
            ExplorerManager manager = com.ebixio.virtmus.MainApp.findInstance().getExplorerManager();
//            try {
//                manager.setSelectedNodes(new Node[] {t.getPage().node});
//            } catch (PropertyVetoException ex) {
//                //ex.printStackTrace();
//            }
            
            File f = t.getImgFile();
        }
    }
    
    public void mouseReleased(MouseEvent e) {
    }
    
    public void mouseEntered(MouseEvent e) {
    }
    
    public void mouseExited(MouseEvent e) {
    } // </editor-fold>
    
    final static class ResolvableHelper implements Serializable {
        private static final long serialVersionUID = 1L;
        public Object readResolve() {
            return ThumbsTopComponent.getDefault();
        }
    }

   
    public void loadSong(Song s) {
        if (loadedSong != null) loadedSong.removeChangeListener(changeListener);
        s.addChangeListener(changeListener);
        loadedSong = s;
        
        for (Component c: jPanel.getComponents()) {
            //c.removeMouseListener(this);    // OR
            for (MouseListener m: c.getMouseListeners()) {
                c.removeMouseListener(m);
            }
        }
        jPanel.removeAll();
        
        jPanel.setSize(this.getWidth(), this.getHeight());
        
        if (s == null) return;
        for (MusicPage p: loadedSong.pageOrder) {
            DraggableThumbnail t = p.getThumbnail();
            t.addMouseListener(this);
            t.setSelected(false);
            jPanel.add(t);
        }

    }

    /**
     * TODO: Only reorder if thumbnail was moved significantly
     * TODO: Make it work when in a single row or single column
     */
    void reorderThumbs() {
        Vector<MusicPage> newOrder = new Vector<MusicPage>();
        Thumbnail selectedThumb = null, otherThumb = null;
        int components = jPanel.getComponentCount();
        int selectedIdx = 0, insertBefore = components;
        boolean changed = false;
        
        // Find the thumbnail we want to move
        for (int i = 0; i < components; i++) {
            Thumbnail t = (Thumbnail)jPanel.getComponent(i);
            if ( t.isSelected() ) {
                selectedThumb = t;
                selectedIdx = i;
                break;
            }
        }
        if (selectedThumb == null) return;
        
        // Find where we want to move it
        Rectangle prevDrop = new Rectangle(0, 0, -1, -1);
        
        for (int i = 0; i < components; i++) {
            if (i == selectedIdx) continue;
            otherThumb = (Thumbnail)jPanel.getComponent(i);
            Rectangle rect = otherThumb.getBounds();
            Rectangle drop = new Rectangle(0, 0, rect.x, rect.y + (rect.height + vgap) / 2);
            
            // Check to see if it was dropped at the end of the previous row
            if (drop.height > prevDrop.height) {
                prevDrop.width = jPanel.getWidth();
                if (prevDrop.contains(selectedThumb.getLocation())) {
                    insertBefore = i;
                    break;
                }
            }
            
            if (drop.contains(selectedThumb.getLocation())) {
                insertBefore = i;
                break;
            } else {
                prevDrop = drop;
            }
        }

        // Move it to the new location and reload the thumbnails
        if (insertBefore != selectedIdx) {
            for (int i = 0; i < insertBefore; i++) {
                if (i != selectedIdx)
                    newOrder.add(loadedSong.pageOrder.get(i));
            }
            newOrder.add(loadedSong.pageOrder.get(selectedIdx));
            for (int i = insertBefore; i < loadedSong.pageOrder.size(); i++) {
                if (i != selectedIdx)
                    newOrder.add(loadedSong.pageOrder.get(i));
            }
            
            // The two better have the same number of pages
            if (loadedSong.pageOrder.size() != newOrder.size()) return;
            
            for (int i = 0; i < newOrder.size(); i++) {
                if (loadedSong.pageOrder.get(i) != newOrder.get(i)) {
                    // We have actually changed the order
                    changed = true;
                    break;
                }
            }
            
            if (changed) {
                loadedSong.pageOrder = newOrder;
                loadedSong.setDirty(true);
                loadedSong.notifyListeners();
                loadSong(loadedSong);
            }
        }
        
        layoutThumbs();
        //ExplorerManager manager = com.ebixio.virtmus.MainApp.findInstance().getExplorerManager();
        //manager.setRootContext(new AbstractNode(new PlayLists()));
    }
    
    // <editor-fold defaultstate="collapsed" desc=" ModifiedFlowLayout ">
    /**
     * A modified version of FlowLayout that allows containers using this
     * Layout to behave in a reasonable manner when placed inside a
     * JScrollPane
     * 
     * @author Babu Kalakrishnan
     */
    public class ModifiedFlowLayout extends FlowLayout {
        public ModifiedFlowLayout() {
            super();
        }
        
        public ModifiedFlowLayout(int align) {
            super(align);
        }
        
        public ModifiedFlowLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }
        
        public Dimension minimumLayoutSize(Container target) {
            return computeSize(target, true);
        }
        
        public Dimension preferredLayoutSize(Container target) {
            return computeSize(target, false);
        }
        
        private Dimension computeSize(Container target, boolean minimum) {
            synchronized (target.getTreeLock()) {
                int hgap = getHgap();
                int vgap = getVgap();
                int w = target.getWidth();
                
                // Let this behave like a regular FlowLayout (single row)
                // if the container hasn't been assigned any size yet
                if (w == 0)
                    w = Integer.MAX_VALUE;
                
                Insets insets = target.getInsets();
                if (insets == null)
                    insets = new Insets(0, 0, 0, 0);
                int reqdWidth = 0;
                
                int maxwidth = w - (insets.left + insets.right + hgap * 2);
                int n = target.getComponentCount();
                int x = 0;
                int y = insets.top;
                int rowHeight = 0;
                
                for (int i = 0; i < n; i++) {
                    Component c = target.getComponent(i);
                    if (c.isVisible()) {
                        Dimension d =
                                minimum ? c.getMinimumSize() :
                                    c.getPreferredSize();
                        if ((x == 0) || ((x + d.width) <= maxwidth)) {
                            if (x > 0) {
                                x += hgap;
                            }
                            x += d.width;
                            rowHeight = Math.max(rowHeight, d.height);
                        } else {
                            x = d.width;
                            y += vgap + rowHeight;
                            rowHeight = d.height;
                        }
                        reqdWidth = Math.max(reqdWidth, x);
                    }
                }
                y += rowHeight + vgap * 2;
                return new Dimension(reqdWidth+insets.left+insets.right, y+insets.bottom);
            }
        }
    }
    //</editor-fold>
}
