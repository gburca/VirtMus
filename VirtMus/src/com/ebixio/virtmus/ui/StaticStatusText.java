/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
//import org.netbeans.core.windows.view.ui.slides.SlideBar;
import org.openide.awt.StatusDisplayer;

/**
 * Status line text that shows at the bottom of the main IDE window only when
 * there's any status text available and auto-hides when the status text is empty.
 *
 * @author gburca
 */
final class StaticStatusText implements ChangeListener, Runnable {

    private final JPanel panel = new JPanel( new BorderLayout() );
    private final JLabel lblStatus = new JLabel();
    private String text;
    private final JPanel statusContainer;

    private StaticStatusText( JFrame frame, JPanel statusContainer  ) {
        this.statusContainer = statusContainer;
        Border outerBorder = UIManager.getBorder( "Nb.ScrollPane.border" ); //NOI18N
        if( null == outerBorder ) {
            outerBorder = BorderFactory.createEtchedBorder();
        }
        panel.setBorder( BorderFactory.createCompoundBorder( outerBorder,
                BorderFactory.createEmptyBorder(3,3,3,3) ) );
        lblStatus.setName("StaticStatusTextLabel"); //NOI18N
        panel.add( lblStatus, BorderLayout.CENTER );
        frame.getLayeredPane().add( panel, Integer.valueOf( 101 ) );
        StatusDisplayer.getDefault().addChangeListener( this );

        frame.addComponentListener( new ComponentAdapter() {
            @Override
            public void componentResized( ComponentEvent e ) {
                run();
            }
        });
    }

    static void install( JFrame frame, JPanel statusContainer ) {
        new StaticStatusText( frame, statusContainer );
    }

    @Override
    public void stateChanged( ChangeEvent e ) {
        text = StatusDisplayer.getDefault().getStatusText();
        String oldValue = lblStatus.getText();
        if( text == null ? oldValue == null : text.equals( oldValue ) ) {
            // no change needed
            return;
        }
        if( SwingUtilities.isEventDispatchThread() ) {
            run();
        } else {
            SwingUtilities.invokeLater( this );
        }
    }

    @Override
    public void run() {
        lblStatus.setText( text );

        panel.setVisible( true );
        Container parent = panel.getParent();
        Dimension dim = panel.getPreferredSize();
        Rectangle rect = parent.getBounds();
        Component slideBar = findSlideBar();
        if( null != slideBar ) {
            int slideWidth = slideBar.getWidth();
            if( slideWidth > 0 ) {
                rect.x += slideWidth + 10;
            }
        }
        panel.setBounds( rect.x-1, rect.y+rect.height-dim.height+1, dim.width, dim.height+1 );
        if( parent instanceof JLayeredPane ) {
            JLayeredPane pane = (JLayeredPane) parent;
            if( pane.getComponentZOrder(panel) >= 0 ) { //#241059
                pane.moveToFront( panel );
            }
        }
    }

    private Component findSlideBar() {
        if( null == statusContainer )
            return null;
        for( Component c : statusContainer.getComponents() ) {
//            if( c instanceof SlideBar ) {
//                return c;
//            }
        }
        return null;
    }
}
