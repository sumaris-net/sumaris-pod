package net.sumaris.core.exception;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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


import java.util.List;

/**
 * Throw when a deletion is not allowed
 */
public class DenyDeletionException extends SumarisTechnicalException {

    private List<String> objectIds;

    /**
     * <p>Constructor for DeleteForbiddenException.</p>
     *
     * @param message a {@link String} object.
     */
    public DenyDeletionException(String message, List<String> objectIds) {
        super(message);
        this.objectIds = objectIds;
    }

    /**
     * <p>Constructor for DeleteForbiddenException.</p>
     *
     * @param message a {@link String} object.
     * @param cause a {@link Throwable} object.
     */
    public DenyDeletionException(String message, List<String> objectIds, Throwable cause) {
        super(message, cause);
        this.objectIds = objectIds;
    }

    /**
     * <p>Constructor for DeleteForbiddenException.</p>
     *
     * @param cause a {@link Throwable} object.
     */
    public DenyDeletionException(Throwable cause) {
        super(cause);
    }

    public List<String> getObjectIds() {
        return objectIds;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + (objectIds != null ? " : " + objectIds.toString() : "");
    }
}
