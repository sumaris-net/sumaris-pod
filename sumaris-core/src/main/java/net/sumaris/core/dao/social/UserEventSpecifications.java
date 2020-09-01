package net.sumaris.core.dao.social;

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

import net.sumaris.core.model.social.UserEvent;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.social.UserEventFilterVO;
import net.sumaris.core.vo.social.UserEventVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

public interface UserEventSpecifications {


    default Specification<UserEvent> toSpecification(UserEventFilterVO filter) {
        if (filter == null) return null;
        return Specification.where(isIssuer(filter.getIssuer()))
                .and(isRecipient(filter.getRecipient()));
    }

    default Specification<UserEvent> isIssuer(String issuer) {
        if (StringUtils.isBlank(issuer)) return null;
        return (root, query, cb) -> cb.equal(root.get(UserEvent.Fields.ISSUER), issuer);
    }

    default Specification<UserEvent> isRecipient(String recipient) {
        if (StringUtils.isBlank(recipient)) return null;
        return (root, query, cb) -> cb.equal(root.get(UserEvent.Fields.RECIPIENT), recipient);
    }

    Page<UserEventVO> findAllVO(@Nullable Specification<UserEvent> spec, Pageable pageable);
    Page<UserEventVO> findAllVO(@Nullable Specification<UserEvent> spec, net.sumaris.core.dao.technical.Page page);

}
