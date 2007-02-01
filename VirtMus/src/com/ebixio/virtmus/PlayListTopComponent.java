/*
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

package com.ebixio.virtmus;

import java.io.Serializable;
import javax.swing.ActionMap;
import javax.swing.text.DefaultEditorKit;
import org.openide.ErrorManager;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.openide.util.Utilities;
import org.openide.explorer.*;
import org.openide.nodes.*;

/**
 * Top component which displays something.
 */
final class PlayListTopComponent extends TopComponent
        implements ExplorerManager.Provider, Lookup.Provider {
    
    private static PlayListTopComponent instance;
    /** path to the icon used by the component and its open action */
    static final String ICON_PATH = "com/ebixio/virtmus/resources/PlayListTopComponent.png";
    
    private static final String PREFERRED_ID = "PlayListTopComponent";
    
    private PlayListTopComponent() {
        initComponents();
        setName(NbBundle.getMessage(PlayListTopComponent.class, "CTL_PlayListTopComponent"));
        setToolTipText(NbBundle.getMessage(PlayListTopComponent.class, "HINT_PlayListTopComponent"));
        setIcon(Utilities.loadImage(ICON_PATH, true));
        
        ExplorerManager manager = MainApp.findInstance().getExplorerManager();
        ActionMap map = this.getActionMap();
        map.put(DefaultEditorKit.copyAction, ExplorerUtils.actionCopy(manager));
        map.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(manager));
        map.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(manager));
        map.put("delete", ExplorerUtils.actionDelete(manager, true)); // or false
        
        // Place the Explorer Manager in the TopComponent's Lookup
        associateLookup(ExplorerUtils.createLookup(manager, map));
        
        //manager.setRootContext(new AbstractNode(new CategoryChildren()));
        MainApp.log("PlayListTopComponent::constructor before addAllPlayLists thread:" + Thread.currentThread().getName());
        //MainApp.findInstance().addAllPlayLists(NbPreferences.forModule(MainApp.class));
        MainApp.log("PlayListTopComponent::constructor before new PlayLists");
        manager.setRootContext(new AbstractNode(new PlayLists(MainApp.findInstance())));
        // OR
        //manager.setRootContext(new RootNode(new CategoryChildren()));
        manager.getRootContext().setDisplayName("Playlists");
        this.beanTreeView1.setRootVisible(false);
    }
        
//    // <editor-fold defaultstate="collapsed" desc=" RootNode ">
//    public class RootNode extends AbstractNode {
//        
//        /** Creates a new instance of RootNode */
//        public RootNode(Children children) {
//            super(children);
//        }
//        
//        public Image getIcon(int type) {
//            return Utilities.loadImage("org/netbeans/myfirstexplorer/right-rectangle.png");
//        }
//        
//        public Image getOpenedIcon(int type) {
//            return Utilities.loadImage("org/netbeans/myfirstexplorer/down-rectangle.png");
//        }
//        
//    }
//    // </editor-fold>
//    
//    // <editor-fold defaultstate="collapsed" desc=" Category ">
//    public class Category {
//        private String name;
//        public Category() {}
//        public String getName() {return name;}
//        public void setName(String name) {this.name = name;}
//    }
//    // </editor-fold>
//    
//    // <editor-fold defaultstate="collapsed" desc=" CategoryChildren ">
//    public class CategoryChildren extends Children.Keys {
//        
//        private String[] Categories = new String[]{
//            "Adventure",
//            "Drama",
//            "Comedy",
//            "Romance",
//            "Thriller"};
//        
//        public CategoryChildren() {}
//        
//        protected Node[] createNodes(Object key) {
//            Category obj = (Category) key;
//            return new Node[] { new CategoryNode( obj ) };
//        }
//        
//        protected void addNotify() {
//            super.addNotify();
//            Category[] objs = new Category[Categories.length];
//            for (int i = 0; i < objs.length; i++) {
//                Category cat = new Category();
//                cat.setName(Categories[i]);
//                objs[i] = cat;
//            }
//            setKeys(objs);
//        }
//        
//    }
//    // </editor-fold>
//    
//    // <editor-fold defaultstate="collapsed" desc=" CategoryNode ">
//    public class CategoryNode extends AbstractNode {
//        
//        /** Creates a new instance of CategoryNode */
//        public CategoryNode( Category category ) {
//            super( new MovieChildren(category), Lookups.singleton(category) );
//            setDisplayName(category.getName());
//            setIconBaseWithExtension("org/netbeans/myfirstexplorer/marilyn_category.gif");
//        }
//        
//        public PasteType getDropType(Transferable t, final int action, int index) {
//            final Node dropNode = NodeTransfer.node( t,
//                    DnDConstants.ACTION_COPY_OR_MOVE+NodeTransfer.CLIPBOARD_CUT );
//            if( null != dropNode ) {
//                final Movie movie = (Movie)dropNode.getLookup().lookup( Movie.class );
//                if( null != movie  && !this.equals( dropNode.getParentNode() )) {
//                    return new PasteType() {
//                        public Transferable paste() throws IOException {
//                            getChildren().add( new Node[] { new MovieNode(movie) } );
//                            if( (action & DnDConstants.ACTION_MOVE) != 0 ) {
//                                dropNode.getParentNode().getChildren().remove( new Node[] {dropNode} );
//                            }
//                            return null;
//                        }
//                    };
//                }
//            }
//            return null;
//        }
//        
//        public Cookie getCookie(Class clazz) {
//            Children ch = getChildren();
//            
//            if (clazz.isInstance(ch)) {
//                return (Cookie) ch;
//            }
//            
//            return super.getCookie(clazz);
//        }
//        
//        protected void createPasteTypes(Transferable t, List s) {
//            super.createPasteTypes(t, s);
//            PasteType paste = getDropType( t, DnDConstants.ACTION_COPY, -1 );
//            if( null != paste )
//                s.add( paste );
//        }
//        
//        public Action[] getActions(boolean context) {
//            return new Action[] {
//                SystemAction.get( NewAction.class ),
//                SystemAction.get( PasteAction.class ) };
//        }
//        
//        public boolean canDestroy() {
//            return true;
//        }
//        
//    }
//    // </editor-fold>
//    
//    // <editor-fold defaultstate="collapsed" desc=" Movie ">
//    public class Movie {
//        
//        private Integer number;
//        private String category;
//        private String title;
//        
//        /** Creates a new instance of Instrument */
//        public Movie() {
//        }
//        
//        public Integer getNumber() {
//            return number;
//        }
//        
//        public void setNumber(Integer number) {
//            this.number = number;
//        }
//        
//        public String getCategory() {
//            return category;
//        }
//        
//        public void setCategory(String category) {
//            this.category = category;
//        }
//        
//        public String getTitle() {
//            return title;
//        }
//        
//        public void setTitle(String title) {
//            this.title = title;
//        }
//        
//    }
//    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc=" MovieChildren ">
//    public class MovieChildren  extends Index.ArrayChildren {
//        
//        private Category category;
//        
//        private String[][] items = new String[][]{
//            {"0", "Adventure", "River of No Return"},
//            {"1", "Drama", "All About Eve"},
//            {"2", "Drama", "Home Town Story"},
//            {"3", "Comedy", "We're Not Married!"},
//            {"4", "Comedy", "Love Happy"},
//            {"5", "Romance", "Some Like It Hot"},
//            {"6", "Romance", "Let's Make Love"},
//            {"7", "Romance", "How to Marry a Millionaire"},
//            {"8", "Thriller", "Don't Bother to Knock"},
//            {"9", "Thriller", "Niagara"},
//        };
//        
//        public MovieChildren(Category Category) {
//            this.category = Category;
//        }
//        
//        protected java.util.List<Node> initCollection() {
//            ArrayList childrenNodes = new ArrayList( items.length );
//            for( int i=0; i<items.length; i++ ) {
//                if( category.getName().equals( items[i][1] ) ) {
//                    Movie item = new Movie();
//                    item.setNumber(new Integer(items[i][0]));
//                    item.setCategory(items[i][1]);
//                    item.setTitle(items[i][2]);
//                    childrenNodes.add( new MovieNode( item ) );
//                }
//            }
//            return childrenNodes;
//        }
//    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc=" MovieNode ">
//    public class MovieNode extends AbstractNode {
//        
//        private Movie movie;
//        
//        /** Creates a new instance of MovieNode */
//        public MovieNode(Movie key) {
//            super(Children.LEAF, Lookups.fixed( new Object[] {key} ) );
//            this.movie = key;
//            setDisplayName(key.getTitle());
//            setIconBaseWithExtension("org/netbeans/myfirstexplorer/marilyn.gif");
//        }
//        
//        public boolean canCut() {
//            
//            return true;
//        }
//        
//        public boolean canDestroy() {
//            return true;
//        }
//        
//        public Action[] getActions(boolean popup) {
//            return new Action[] {
//                SystemAction.get( CopyAction.class ),
//                SystemAction.get( CutAction.class ),
//                null,
//                SystemAction.get( DeleteAction.class ) };
//        }
//        
//    }
    // </editor-fold>

     
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        beanTreeView1 = new org.openide.explorer.view.BeanTreeView();

        beanTreeView1.setPreferredSize(new java.awt.Dimension(100, 400));
        jScrollPane1.setViewportView(beanTreeView1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.openide.explorer.view.BeanTreeView beanTreeView1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
    
    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link findInstance}.
     */
    public static synchronized PlayListTopComponent getDefault() {
        if (instance == null) {
            instance = new PlayListTopComponent();
        }
        return instance;
    }
    
    /**
     * Obtain the PlayListTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized PlayListTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            ErrorManager.getDefault().log(ErrorManager.WARNING,
                    "Cannot find MyWindow component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof PlayListTopComponent) {
            return (PlayListTopComponent)win;
        }
        ErrorManager.getDefault().log(ErrorManager.WARNING,
                "There seem to be multiple components with the '" + PREFERRED_ID +
                "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }
    
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }
    
    protected void componentActivated() {
        ExplorerManager manager = MainApp.findInstance().getExplorerManager();
        ExplorerUtils.activateActions(manager, true);
    }
    
    protected void componentDeactivated() {
        ExplorerManager manager = MainApp.findInstance().getExplorerManager();
        ExplorerUtils.activateActions(manager, false);
    }
    
    /** replaces this in object stream */
    public Object writeReplace() {
        return new ResolvableHelper();
    }
    
    protected String preferredID() {
        return PREFERRED_ID;
    }

    public ExplorerManager getExplorerManager() {
        return MainApp.findInstance().getExplorerManager();
    }
    
    final static class ResolvableHelper implements Serializable {
        private static final long serialVersionUID = 1L;
        public Object readResolve() {
            return PlayListTopComponent.getDefault();
        }
    }
    
}
