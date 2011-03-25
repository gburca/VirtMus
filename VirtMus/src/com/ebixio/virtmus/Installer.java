/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus;

import com.ebixio.util.Log;
import java.awt.EventQueue;
import org.openide.awt.ToolbarPool;
import org.openide.modules.ModuleInstall;
import org.openide.windows.WindowManager;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {

            @Override
            public void run() {
                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        Log.log("VirtMus module: restored");
                        ToolbarPool.getDefault().setConfiguration("StandardToolbar");
                    }
                });
            }
        });
    }
}
