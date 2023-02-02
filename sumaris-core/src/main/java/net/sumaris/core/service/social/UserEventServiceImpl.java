package net.sumaris.core.service.social;

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


import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.social.UserEventRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.social.UserEvent;
import net.sumaris.core.vo.social.UserEventFetchOptions;
import net.sumaris.core.vo.social.UserEventFilterVO;
import net.sumaris.core.vo.social.UserEventVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

/**
 * @author <benoit.lavenier@e-is.pro> on 08/07/2020.
 */
@Service("userEventService")
@Slf4j
public class UserEventServiceImpl implements UserEventService {

    private final UserEventRepository userEventRepository;

    @Autowired
    public UserEventServiceImpl(UserEventRepository userEventRepository) {
        this.userEventRepository = userEventRepository;
    }

    @Override
    public Long count(UserEventFilterVO filter) {
        return userEventRepository.count(filter);
    }


    @Override
    public List<UserEventVO> findAll(UserEventFilterVO filter) {
        return userEventRepository.findAllVO(filter, null);
    }

    @Override
    public List<UserEventVO> findAll(UserEventFilterVO filter, Page page) {
        return userEventRepository.findAllVO(filter, page);
    }

    @Override
    public List<UserEventVO> findAll(UserEventFilterVO filter, Page page, UserEventFetchOptions fetchOptions) {
        return userEventRepository.findAllVO(filter, page, fetchOptions);
    }

    @Override
    public UserEventVO save(UserEventVO event) {
        Preconditions.checkNotNull(event);
        Preconditions.checkNotNull(event.getIssuer());
        Preconditions.checkNotNull(event.getRecipient());
        Preconditions.checkNotNull(event.getType());
        Preconditions.checkNotNull(event.getLevel());

        // Special case if link to a job: retrieve the existing event
        if (event.getId() == null && event.getSource() != null) {
            UserEvent existingEvent = userEventRepository.getBySource(event.getSource());
            if (existingEvent != null) {
                event.setId(existingEvent.getId());
                event.setUpdateDate(existingEvent.getUpdateDate());
            }
        }

        // Mark as unread
        event.setReadDate(null);
        event.setReadSignature(null);

        return userEventRepository.save(event);
    }

    @Override
    public void delete(int id) {
        userEventRepository.deleteById(id);
    }

    @Override
    public void delete(List<Integer> ids) {
        Preconditions.checkNotNull(ids);
        ids.stream()
                .filter(Objects::nonNull)
                .forEach(this::delete);
    }

    @Override
    public Timestamp getLastCreationDate(String... recipients) {
        return userEventRepository.getMaxCreationDateByRecipient(Arrays.asList(recipients));
    }

    @Override
    public Timestamp getLastReadDate(String... recipients) {
        return userEventRepository.getMaxReadDateByRecipient(Arrays.asList(recipients));
    }

    @Override
    public void markAsRead(List<Integer> userEventIds) {
        if (CollectionUtils.isEmpty(userEventIds)) return;

        Timestamp readDate = userEventRepository.getDatabaseCurrentTimestamp();
        userEventIds.stream()
            .map(userEventRepository::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(userEventVO -> {
                userEventVO.setReadDate(readDate);
                userEventRepository.save(userEventVO);
            });
    }
}
