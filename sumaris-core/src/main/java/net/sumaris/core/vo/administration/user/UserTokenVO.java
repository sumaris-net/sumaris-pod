package net.sumaris.core.vo.administration.user;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2023 SUMARiS Consortium
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

import io.leangen.graphql.annotations.GraphQLIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.IValueObject;

import java.sql.Timestamp;
import java.util.Date;

@Data
@FieldNameConstants
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserTokenVO implements IUpdateDateEntity<Integer, Date>, IValueObject<Integer> {
    private Integer id;
    @GraphQLIgnore
    private Integer userId;
    private String pubkey; // Used only for compatibility with Sumaris API
    @EqualsAndHashCode.Include
    private String token;
    private String name;
    private Integer flags;
    private Timestamp expirationDate;
    private Timestamp lastUsedDate;

    private Date updateDate;
    private Date creationDate;
}
