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
import net.sumaris.core.model.social.EventTypeEnum;
import net.sumaris.core.vo.social.UserEventFilterVO;
import net.sumaris.core.vo.social.UserEventVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author <benoit.lavenier@e-is.pro> on 08/07/2020.
 */
@Service("userEventService")
@Slf4j
public class UserEventServiceImpl implements UserEventService {

    private final UserEventRepository repository;

    @Autowired
    public UserEventServiceImpl(UserEventRepository userEventRepository) {
        this.repository = userEventRepository;
    }

    @Override
    public List<UserEventVO> findAll(UserEventFilterVO filter, Page page) {
        return repository.findAllVO(repository.toSpecification(filter), page)
                .stream().collect(Collectors.toList());
    }

    @Override
    public List<UserEventVO> findAll(UserEventFilterVO filter, Pageable page) {
        return repository.findAllVO(repository.toSpecification(filter), page)
            .stream().collect(Collectors.toList());
    }

    @Override
    public UserEventVO save(UserEventVO event) {
        Preconditions.checkNotNull(event);
        Preconditions.checkNotNull(event.getIssuer());
        Preconditions.checkNotNull(event.getRecipient());
        Preconditions.checkNotNull(event.getEventType());

        // Check event type exists
        EventTypeEnum.byLabel(event.getEventType());

        return repository.save(event);
    }

    @Override
    public void delete(int id) {
        repository.deleteById(id);
    }

    @Override
    public void delete(List<Integer> ids) {
        Preconditions.checkNotNull(ids);
        ids.stream()
                .filter(Objects::nonNull)
                .forEach(this::delete);
    }


}
