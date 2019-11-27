package net.sumaris.core.dao.technical;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import java.io.Serializable;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class Pageable implements Serializable {

    public static final Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Pageable pageable = new Pageable();

        public Builder setOffset(int offset) {
            pageable.setOffset(offset);
            return this;
        }
        public Builder setSize(int size) {
            pageable.setSize(size);
            return this;
        }
        public Builder setSortAttribute(String sortAttribute) {
            pageable.setSortAttribute(sortAttribute);
            return this;
        }
        public Builder setSortDirection(SortDirection sortDirection) {
            pageable.setSortDirection(sortDirection);
            return this;
        }

        public Pageable build() {
            return pageable;
        }
    }


    private int offset;

    private int size;

    private String sortAttribute;

    private SortDirection sortDirection;

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortAttribute() {
        return sortAttribute;
    }

    public void setSortAttribute(String sortAttribute) {
        this.sortAttribute = sortAttribute;
    }

    public SortDirection getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(SortDirection sortDirection) {
        this.sortDirection = sortDirection;
    }
}

