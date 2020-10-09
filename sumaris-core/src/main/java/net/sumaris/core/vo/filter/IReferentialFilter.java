package net.sumaris.core.vo.filter;

import java.io.Serializable;

/**
 * @author peck7 on 28/08/2020.
 */
public interface IReferentialFilter extends Serializable {

    String getLabel();

    void setLabel(String label);

    String getName();

    void setName(String name);

    Integer[] getStatusIds();

    void setStatusIds(Integer[] statusIds);

    Integer getLevelId();

    void setLevelId(Integer levelId);

    Integer[] getLevelIds();

    void setLevelIds(Integer[] levelIds);

    String getSearchJoin();

    void setSearchJoin(String searchJoin);

    String getSearchText();

    void setSearchText(String searchText);

    String getSearchAttribute();

    void setSearchAttribute(String searchAttribute);
}
