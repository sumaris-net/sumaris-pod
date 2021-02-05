package net.sumaris.core.vo.filter;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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
 * @author peck7 on 28/08/2020.
 */
public interface IReferentialFilter extends Serializable {

    Integer getId();

    void setId(Integer id);

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

    String getLevelLabel();

    void setLevelLabel(String LevelLabel);

    String[] getLevelLabels();

    void setLevelLabels(String[] LevelLabels);

    String getSearchJoin();

    void setSearchJoin(String searchJoin);

    String getSearchText();

    void setSearchText(String searchText);

    String getSearchAttribute();

    void setSearchAttribute(String searchAttribute);


    Integer[] getExcludedIds();

    void setExcludedIds(Integer[] excludedIds);
}
