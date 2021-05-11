package net.sumaris.core.vo.technical;

/*-
 * #%L
 * SUMARiS:: Core
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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@FieldNameConstants
@EqualsAndHashCode(callSuper = true)
public class ConfigurationVO extends SoftwareVO {

    private String smallLogo;
    private String largeLogo;

    public ConfigurationVO() {
    }

    public ConfigurationVO(SoftwareVO source) {
        Beans.copyProperties(source, this);
    }

    // TODO: add a map by resolution (e.g. hdpi, mdpi, ...)
    //private Map<String, String> logo;

    private Map<String, String> properties;

    private List<String> backgroundImages;

    private List<DepartmentVO> partners;

}
