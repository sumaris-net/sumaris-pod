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
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.referential.IReferentialVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class PodConfigurationVO implements IReferentialVO {

    public static final String PROPERTY_PROPERTIES = "properties";
    public static final String PROPERTY_PARTNERS = "partners";
    public static final String PROPERTY_BACKGROUND_IMAGES = "backgroundImages";
    public static final String PROPERTY_LOGO = "logo";

    private Integer id;

    private String label;

    private String name;

    private String logo;

    private String defaultProgram;

    private Date updateDate;

    private Map<String, String> properties;

    private List<String> backgroundImages;

    private List<DepartmentVO> partners;

}
