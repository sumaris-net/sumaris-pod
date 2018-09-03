package net.sumaris.core.dao.technical.model;

import java.io.Serializable;
import java.util.Date;

public interface IEntityBean<T extends Serializable>  {


    String PROPERTY_ID = "id";

    T getId();

    void setId(T id);
}
