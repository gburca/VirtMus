package com.ebixio.util;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.openide.NotifyDescriptor;
import org.openide.util.ImageUtilities;

/**
 *
 * @author qbeukes.blogspot.com, used by metalklesk
 */
public enum MessageType {
    PLAIN   (NotifyDescriptor.PLAIN_MESSAGE,       null),
    INFO    (NotifyDescriptor.INFORMATION_MESSAGE, "NotifyInfo.png"),
    QUESTION(NotifyDescriptor.QUESTION_MESSAGE,    "NotifyQestion.png"),
    ERROR   (NotifyDescriptor.ERROR_MESSAGE,       "NotifyError.png"),
    WARNING (NotifyDescriptor.WARNING_MESSAGE,     "NotifyWarning.png");

    private int notifyDescriptorType;

    private Icon icon;

    private MessageType(int notifyDescriptorType, String resourceName) {
        this.notifyDescriptorType = notifyDescriptorType;
        if (resourceName == null) {
            icon = new ImageIcon();
        } else {
            icon = loadIcon(resourceName);
        }
    }

    private static Icon loadIcon(String resourceName) {
        Icon icon = ImageUtilities.loadImageIcon("com/ebixio/virtmus/resources/" + resourceName, false);
        return icon != null ? icon : new ImageIcon();
        
//        URL resource = MessageType.class.getResource("images/" + resourceName);
//        System.out.println(resource);
//        if (resource == null) {
//            return new ImageIcon();
//        }
//        
//        return new ImageIcon(resource);
    }

    int getNotifyDescriptorType() {
        return notifyDescriptorType;
    }

    Icon getIcon() {
        return icon;
    }
}