/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.vo.social;


import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.IValueObject;
import net.sumaris.core.model.social.EventLevelEnum;
import net.sumaris.core.model.social.EventTypeEnum;

import java.util.Date;

@Data
@FieldNameConstants
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEventVO implements IValueObject<Integer>, IUpdateDateEntity<Integer, Date> {

    @EqualsAndHashCode.Exclude
    private Integer id;
    private Date creationDate;
    private Date updateDate;
    private String issuer;
    private String recipient;

    private EventTypeEnum type;
    private EventLevelEnum level;

    private String hash;
    private String content;
    private String signature;
    private Date readDate;
    private String readSignature;

    @Deprecated
    private String eventType;
}
