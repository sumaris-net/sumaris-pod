/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package net.sumaris.core.model;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 * New progression model with long total and current *
 *
 * @author peck7 on 04/07/2019.
 */
@Data
@FieldNameConstants
public class ProgressionModel implements IProgressionModel {

    private static final double DEFAULT_RATE = 1;

    private long total;
    private long current;
    private double rate = DEFAULT_RATE;
    private String message;

    private Integer jobId;

    private transient PropertyChangeSupport pcs;

    public ProgressionModel() {
        this.pcs = new PropertyChangeSupport(this);
    }

    /**
     * get the progression total always as int *
     *
     * @return total as int
     */
    public int getTotal() {
        return (int) (total * rate);
    }

    /**
     * set the progression total *
     *
     * @param total as long
     */
    public void setTotal(long total) {
        // compute the long to int rate
        rate = (total > Integer.MAX_VALUE) ? ((double) Integer.MAX_VALUE / total) : DEFAULT_RATE;
        this.total = total;
        firePropertyChange(Fields.TOTAL, null, getTotal());
        setCurrent(0);
    }

    /**
     * adapt the progression total only if greater than actual *
     *
     * @param total as long
     */
    public void adaptTotal(long total) {
        if (total > this.total) {
            int current = getCurrent();
            setTotal(total);
            setCurrent(current);
        }
    }

    /**
     * get the current progression always as int *
     *
     * @return current as int
     */
    public int getCurrent() {
        return (int) (current * rate);
    }

    /**
     * set the current progression *
     *
     * @param current as long
     */
    public void setCurrent(long current) {
        this.current = Math.min(current, total);
        firePropertyChange(Fields.CURRENT, null, getCurrent());
    }

    public void increments(int nb) {
        setCurrent(current + nb);
    }

    public void increments(String message) {
        increments(1);
        setMessage(message);
    }

    public void setMessage(String message) {
        String oldMessage = getMessage();
        this.message = message;
        firePropertyChange(Fields.MESSAGE, oldMessage, message);
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public Integer getJobId() {
        return jobId;
    }

    /* PropertyChangeSupport methods */
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    public final PropertyChangeListener[] getPropertyChangeListeners() {
        return this.pcs.getPropertyChangeListeners();
    }

    public final void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(propertyName, listener);
    }

    public final void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(propertyName, listener);
    }

    public final PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return this.pcs.getPropertyChangeListeners(propertyName);
    }

    protected final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        this.pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected final void firePropertyChange(PropertyChangeEvent evt) {
        this.pcs.firePropertyChange(evt);
    }

    protected final void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue) {
        this.pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
    }

    protected final boolean hasPropertyChangeListeners(String propertyName) {
        return this.pcs.hasListeners(propertyName);
    }

}