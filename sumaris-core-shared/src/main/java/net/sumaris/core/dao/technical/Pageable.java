package net.sumaris.core.dao.technical;

import javafx.util.Builder;

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

