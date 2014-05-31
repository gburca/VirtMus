/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.actions;

import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.options.Options;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.openide.awt.DropDownButtonFactory;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.actions.CallableSystemAction;


public class ScrollAmountAction extends CallableSystemAction {
    private static JButton button;
    private static ButtonGroup buttonGroup;
    private static JPopupMenu popup;
    private MyMenuItemListener menuItemListener;

    @Override
    public void performAction() {
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                throw new UnsupportedOperationException("ScrollAmountAction not supported yet.");
//            }
//        });
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(ScrollAmountAction.class, "CTL_ScrollAmountAction");
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    public Component getToolbarPresenter() {
        Image iconImage = ImageUtilities.loadImage(
            "com/ebixio/virtmus/resources/ScrollAmountAction24.png", false);
        ImageIcon icon = new ImageIcon(iconImage);

        popup = new JPopupMenu();
        button = DropDownButtonFactory.createDropDownButton(icon, popup);
        //button.setIcon(icon);
        button.setToolTipText(getName());

        menuItemListener = new MyMenuItemListener();

        String[] txt = new String[]{"50%", "100%"};

        buttonGroup = new ButtonGroup();

        for (String s : txt) {
            JRadioButtonMenuItem item =
                new JRadioButtonMenuItem(s);
            item.addActionListener(menuItemListener);
            buttonGroup.add(item);
            popup.add(item);
        }

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popup.show(button, 0, button.getHeight());
            }
        });

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                button.setSelected(false);
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                button.setSelected(false);
            }
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                button.setSelected(true);
            }
        });

        return button;
    }

    private class MyMenuItemListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ev) {
            JMenuItem item = (JMenuItem)ev.getSource();
            String selectedStr = item.getText();

            if (selectedStr.equals("50%")) {
                NbPreferences.forModule(MainApp.class).put(Options.OptPageScrollAmount, "50.0" );
            } else if (selectedStr.equals("100%")) {
                NbPreferences.forModule(MainApp.class).put(Options.OptPageScrollAmount, "100.0" );
            }
        }
    }

}