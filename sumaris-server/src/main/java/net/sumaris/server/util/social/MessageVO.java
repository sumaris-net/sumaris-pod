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

package net.sumaris.server.util.social;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.filter.PersonFilterVO;

import java.io.Serializable;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@FieldNameConstants
@ToString(onlyExplicitlyIncluded = true)
public class MessageVO implements Serializable {

    String subject;
    String body;

    // Recipient
    Integer recipientId;
    PersonVO recipient;
    PersonVO[] recipients;
    PersonFilterVO recipientFilter;

    // Issuer
    Integer issuerId;
    PersonVO issuer;

    // Message type
    @ToString.Include(rank = 1)
    MessageTypeEnum type;

}
