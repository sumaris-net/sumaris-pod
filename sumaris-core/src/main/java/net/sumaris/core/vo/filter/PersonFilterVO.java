package net.sumaris.core.vo.filter;

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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Data
@FieldNameConstants
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonFilterVO implements IReferentialFilter {

    private String label;
    private String name;

    private Integer[] statusIds;

    private Integer levelId;
    private Integer[] levelIds;

    private String searchJoin;
    private String searchText;
    private String searchAttribute;

    private Integer userProfileId;
    private Integer[] userProfileIds;
    private String[] userProfiles;

    private String email;
    private String pubkey;
    private String firstName;
    private String lastName;

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
