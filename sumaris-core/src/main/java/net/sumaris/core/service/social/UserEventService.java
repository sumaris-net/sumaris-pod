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

package net.sumaris.core.service.social;

import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.vo.social.UserEventFetchOptions;
import net.sumaris.core.vo.social.UserEventFilterVO;
import net.sumaris.core.vo.social.UserEventVO;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Transactional
public interface UserEventService {

    @Transactional(readOnly = true)
    Long count(UserEventFilterVO filter);

    @Transactional(readOnly = true)
    List<UserEventVO> findAll(UserEventFilterVO filter);

    @Transactional(readOnly = true)
    List<UserEventVO> findAll(UserEventFilterVO filter, Page page);

    @Transactional(readOnly = true)
    List<UserEventVO> findAll(UserEventFilterVO filter, Page page, UserEventFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    Timestamp getLastCreationDate(String ...recipients);

    @Transactional(readOnly = true)
    Timestamp getLastReadDate(String ...recipients);

    UserEventVO save(UserEventVO event);

    void delete(int id);

    void delete(List<Integer> ids);

    void markAsRead(List<Integer> userEventIds);
}
