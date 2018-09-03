package net.sumaris.core.dao.technical.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

public interface IUpdateDateEntityBean<T extends Serializable, D extends Date> extends IEntityBean<T> {

    String PROPERTY_UPDATE_DATE = "updateDate";

    D getUpdateDate();

    void setUpdateDate(D updateDate);
}
