package net.sumaris.core.dao.technical.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public interface ITreeNodeEntityBean<T extends Serializable, E extends IEntityBean<T>> extends IEntityBean<T> {

    String PROPERTY_PARENT = "parent";
    String PROPERTY_CHILDREN = "children";

    E getParent();

    void setParent(E parent);

    List<E> getChildren();

    void setChildren(List<E> children);
}
