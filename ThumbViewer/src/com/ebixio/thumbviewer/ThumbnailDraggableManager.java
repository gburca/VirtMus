/*
 * ThumbnailDraggableManager.java
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ContainerEvent;
import java.util.HashSet;
import java.util.Set;
import net.java.swingfx.jdraggable.DragPolicy;
import net.java.swingfx.jdraggable.Draggable;
import net.java.swingfx.jdraggable.DraggableListener;
import net.java.swingfx.jdraggable.DraggableManager;
import net.java.swingfx.jdraggable.DraggableMask;

/**
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 * Needed to implement DraggableManager (instead of just extending DefaultDraggableManager)
 * because the default manager did not remove the mouse listeners from the component when
 * the component was removed from the container.
 */
public class ThumbnailDraggableManager implements DraggableManager {
    /**
     * the {@link Container} which contains {@link Component}'s, which may
     * or may not, implement the {@link Draggable} interface
     */
    private Container draggableContainer;
    /**
     * maintains whether a "Draggable Container" has been registered or not
     */
    private boolean draggableContainerRegistered;
    /**
     * the component which was chosen to be dragged
     */
    private Draggable hitDraggable;
    /**
     * maintains the state of <code>hitDraggable</cdoe>
     */
    private byte draggableState;
    /**
     * the {@link DragPolicy} to abide by
     */
    private DragPolicy dragPolicy;
    /**
     * the listener which provides the real ability for a component to
     * change its location during a "drag"
     */
    private DraggableListener dragListener;
    /**
     * maintains a {@link java.util.Set} of the components
     * which have had a {@link DraggableListener} added to them.
     * This is only used for "cleanup".
     * This implementation stores the hash code of each component.
     */
    private Set<Integer> hearingComponents;
    /**
     * determines whether the "draggable container" layout manager should be
     * set to <code>null</code> once a component is dragged (this allows the components
     * to maintain their position even if the container is resized), or not.
     */
    private boolean nullifyLayout = true;

    /** The starting location of the draggable component */
    private Point startLocation;
    
    /**
     * Creates a new {@link DraggableManager} with no "Draggable Container"
     * registered
     *
     * @see net.java.swingfx.jdraggable.DefaultDraggableManager#DefaultDraggableManager(Container)
     * @see net.java.swingfx.jdraggable.DefaultDraggableManager#registerDraggableContainer(Container)
     */
    public ThumbnailDraggableManager() {
    }
    
    /**
     * Creates a new {@link DraggableManager} and registers
     * <code>draggableContainer</code> as the "Draggable Container"
     *
     * @param draggableContainer	the "Draggable Container" to register
     *
     * @throws IllegalArgumentException	if <code>draggableContainer</code> is
     * 									<code>null</code>
     *
     * @see net.java.swingfx.jdraggable.DefaultDraggableManager#DefaultDraggableManager()
     * @see net.java.swingfx.jdraggable.DefaultDraggableManager#registerDraggableContainer(Container)
     */
    public ThumbnailDraggableManager(Container draggableContainer) {
        if (draggableContainer == null) {
            throw new IllegalArgumentException("Can not register a null Draggable Container");
        }
        registerDraggableContainer(draggableContainer);
    }
    
    /* (non-Javadoc)
     * @see com.codecraig.jdraggable.DraggableManager#setNullifyLayout(boolean)
     */
    @Override
    public void setNullifyLayout(boolean nullifyLayout) {
        this.nullifyLayout = nullifyLayout;
    }
    
    /* (non-Javadoc)
     * @see com.codecraig.jdraggable.DraggableManager#shouldNullifyLayout()
     */
    @Override
    public boolean shouldNullifyLayout() {
        return nullifyLayout;
    }
    
    /* (non-Javadoc)
     * @see com.codecraig.jdraggable.DraggableManager#startDrag(java.awt.Component)
     */
    @Override
    public boolean startDrag(Component componentToDrag) {
        if (isDraggableContainerRegistered()) {
            if (getDragPolicy().isDraggable(componentToDrag)) {
                hitDraggable = (componentToDrag instanceof Draggable ? (Draggable) componentToDrag : new DraggableMask(componentToDrag));
                setState(STATE_STILL);
                startLocation = componentToDrag.getLocation();
                return true;
            }
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see com.codecraig.jdraggable.DraggableManager#dragging()
     */
    @Override
    public boolean dragging() {
        if (isDraggableContainerRegistered()) {
            if (hitDraggable != null) {
                setState(STATE_DRAGGING);
                return true;
            }
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see com.codecraig.jdraggable.DraggableManager#stopDrag()
     */
    @Override
    public boolean stopDrag() {
        if (isDraggableContainerRegistered()) {
            // Only reorder thumbs if they've been moved by a significant amount
            Component comp = hitDraggable.getComponent();
            hitDraggable = null;
            setState(STATE_UNKNOWN);

            double distance = comp.getLocation().distance(startLocation);
            if (distance < Math.min(comp.getWidth()/2, comp.getHeight()/2)) {
                comp.setLocation(startLocation);
            } else {
                ThumbsTopComponent.findInstance().reorderThumbs();
            }
            return true;
        }
        return false;
    }
    
    /**
     * Returns the {@link Container} which registered itself as the
     * "Draggable Container" with this {@link DraggableManager}
     *
     * @return	the "Draggable Container" or <code>null</code> if not
     * 			<code>Container</code> has been registered as the
     * 			"Draggable Container"
     */
    @Override
    public Container getDraggableContainer() {
        return draggableContainer;
    }
    
    private boolean isDraggableContainerRegistered() {
        return draggableContainerRegistered;
    }
    
    /**
     * Sets the state of <code>hitDraggable</code>
     *
     * @param state	the state of <code>hitDraggable</code>
     */
    private void setState(byte state) {
        draggableState = state;
    }
    
    /**
     * Returns the state of the current {@link Draggable} component which this
     * manager is handling
     *
     * @param draggableComponent The draggable component.
     * @return the state of the current <code>Draggable</code> component
     *
     * @see net.java.swingfx.jdraggable.DraggableManager#getState(net.java.swingfx.jdraggable.Draggable)
     */
    @Override
    public byte getState(Draggable draggableComponent) {
        return draggableState;
    }
    
    /**
     * Registers the given {@link Container} as the "Draggable Container"
     *
     * @param draggableContainer	the <code>Container</code> whose <code>Draggable</code>
     * 								components should be able to be dragged
     *
     * @see DraggableManager#registerDraggableContainer(java.awt.Container)
     *
     * @throws IllegalArgumentException	if a <code>Container</code> has already
     * 									been registered
     */
    @Override
    public void registerDraggableContainer(Container draggableContainer) {
        if (this.draggableContainer == null) {
            this.draggableContainer = draggableContainer;
            draggableContainer.addContainerListener(this);
            dragListener = new DraggableListener(this);
            hearingComponents = new HashSet<Integer>();
            draggableContainerRegistered = true;
        } else {
            throw new IllegalArgumentException("A Draggable Container has already been registered");
        }
    }
    
    /**
     * Un-Registers the given {@link Container} from being the "Draggable Container"
     *
     * @param draggableContainer	the <code>Container</code> to unregister
     *
     * @see DraggableManager#unregisterDraggableContainer(Container)
     *
     * @throws IllegalArgumentException	if the given container is not the same
     * 									as the already registered container
     * @throws IllegalStateException	if no container is currently registered
     */
    @Override
    public void unregisterDraggableContainer(Container draggableContainer) {
        if (this.draggableContainer == null) {
            throw new IllegalStateException("Failed to unregister draggable container," +
                    " since no draggable container was registered");
        }
        if (this.draggableContainer.equals(draggableContainer)) {
            this.draggableContainer.removeContainerListener(this);
            cleanupHearingComponents();
            this.dragListener = null;
            this.draggableContainer = null;
            draggableContainerRegistered = false;
        } else {
            throw new IllegalArgumentException("Failed to unregister draggable container," +
                    " the given Container is not the same as the" +
                    " register draggable container");
        }
    }
    
    /**
     * Removes the listeners from "hearing components"
     */
    private void cleanupHearingComponents() {
        int count = draggableContainer.getComponentCount();
        for (int i = count - 1; i >= 0 && hearingComponents.size() > 0; i--) {
            Component c = draggableContainer.getComponent(i);
            Integer code = new Integer(c.hashCode());
            if (c != null && hearingComponents.contains(code)) {
                hearingComponents.remove(code);
            }
        }
    }
    
    /**
     * Returns the {@link DragPolicy} which this manager obides by
     *
     * @return the <code>DragPolicy</code> for this manager.  If no
     * 		   policy has been set the default policy is used.
     *
     * @see net.java.swingfx.jdraggable.DraggableManager#getDragPolicy()
     * @see #setDragPolicy(DragPolicy)
     * @see DragPolicy#DEFAULT
     */
    @Override
    public DragPolicy getDragPolicy() {
        if (dragPolicy == null) {
            setDragPolicy(DragPolicy.DEFAULT);
        }
        return dragPolicy;
    }
    
    /* (non-Javadoc)
     * @see com.codecraig.jdraggable.DraggableManager#setDragPolicy(com.codecraig.jdraggable.DragPolicy)
     */
    @Override
    public void setDragPolicy(DragPolicy dragPolicy) {
        this.dragPolicy = dragPolicy;
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ContainerListener#componentAdded(java.awt.event.ContainerEvent)
     */
    @Override
    public void componentAdded(ContainerEvent e) {
        if (dragListener == null || isDraggableContainerRegistered() == false) {
            // this should not occur, since we listening to a container in the first place
            throw new IllegalStateException("Draggable Container must be registered prior to adding components");
        }
        Component c = e.getChild();
        Integer code = new Integer(c.hashCode());
        if (hearingComponents.contains(code) == false) {
            hearingComponents.add(code);
            c.addMouseListener(dragListener);
            c.addMouseMotionListener(dragListener);
        }
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ContainerListener#componentRemoved(java.awt.event.ContainerEvent)
     */
    @Override
    public void componentRemoved(ContainerEvent e) {
        Component c = e.getChild();
        Integer code = new Integer(c.hashCode());
        if (hearingComponents.contains(code)) {
            c.removeMouseListener(dragListener);
            c.removeMouseMotionListener(dragListener);
            hearingComponents.remove(code);
        }
    }
}
