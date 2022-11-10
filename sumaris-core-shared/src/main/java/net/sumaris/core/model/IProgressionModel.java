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

import java.io.Serializable;

/**
 * Progression model with long total and current
 *
 * @author benoit.lavenier@e-is.pro on 04/07/2019.
 */
public interface IProgressionModel extends Serializable {

    interface Fields {
        String TOTAL = "total";
        String CURRENT = "current";
        String MESSAGE = "message";
        String JOB_ID = "jobId";
    }

    /**
     * get the progression total always as int
     *
     * @return total as int
     */
    int getTotal();
    void setTotal(long total);
    void adaptTotal(long total);

    /**
     * get the current progression, always as int
     *
     * @return current as int
     */
    int getCurrent();
    void setCurrent(long current);

    void increments(int nb);
    void increments(String message);

    void setMessage(String message);
    String getMessage();

    void setJobId(Integer jobId);

    Integer getJobId();
}