package net.sumaris.core.dao.social;

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


import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.social.EventLevelEnum;
import net.sumaris.core.model.social.EventTypeEnum;
import net.sumaris.core.model.social.UserEvent;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.social.UserEventFetchOptions;
import net.sumaris.core.vo.social.UserEventFilterVO;
import net.sumaris.core.vo.social.UserEventVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <benoit.lavenier@e-is.pro> on 08/07/2020.
 */
@Slf4j
public class UserEventRepositoryImpl
    extends SumarisJpaRepositoryImpl<UserEvent, Integer, UserEventVO>
    implements UserEventSpecifications {

    @Autowired
    public UserEventRepositoryImpl(EntityManager entityManager) {
        super(UserEvent.class, UserEventVO.class, entityManager);
    }

    @Override
    public long count(@NonNull UserEventFilterVO filter) {
        return count(toSpecification(filter));
    }

    @Override
    public Page<UserEventVO> findAllVO(@Nullable Specification<UserEvent> spec, net.sumaris.core.dao.technical.Page page) {
        return findAllVO(spec, page.asPageable());
    }

    @Override
    public Page<UserEventVO> findAllVO(@Nullable Specification<UserEvent> spec, Pageable pageable) {
        return super.findAll(spec, pageable).map(this::toVO);
    }

    @Override
    public List<UserEventVO> findAllVO(@NonNull UserEventFilterVO filter, @Nullable net.sumaris.core.dao.technical.Page page,
                                       @Nullable UserEventFetchOptions fetchOptions) {
        return super.findAll(toSpecification(filter), page != null ? page.asPageable(): Pageable.unpaged())
            .map(entity -> this.toVO(entity, fetchOptions))
            .stream().collect(Collectors.toList());
    }

    @Override
    public void toEntity(UserEventVO source, UserEvent target, boolean copyIfNull) {
        Beans.copyProperties(source, target,
            // Exclude computed field
            UserEventVO.Fields.HAS_CONTENT);

        target.setLevel(source.getLevel().name());
        target.setType(source.getType().name());
    }

    @Override
    public void toVO(UserEvent source, UserEventVO target, boolean copyIfNull) {
        this.toVO(source, target, null, copyIfNull);
    }


    @Override
    public Timestamp getDatabaseCurrentTimestamp() {
        return super.getDatabaseCurrentTimestamp();
    }

    /* -- protected methods -- */

    @Override
    protected void onBeforeSaveEntity(UserEventVO source, UserEvent target, boolean isNew) {
        super.onBeforeSaveEntity(source, target, isNew);

        // When new entity: set the creation date
        if (isNew || target.getCreationDate() == null) {
            target.setCreationDate(target.getUpdateDate());
        }
    }

    protected void onAfterSaveEntity(UserEventVO vo, UserEvent savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        if (isNew) {
            vo.setCreationDate(savedEntity.getCreationDate());
        }
        // Update computed property
        vo.setHasContent(savedEntity.getContent() != null);
    }

    protected UserEventVO toVO(UserEvent source, UserEventFetchOptions fetchOptions) {
        UserEventVO target = new UserEventVO();
        this.toVO(source, target, fetchOptions, true);
        return target;
    }

    protected void toVO(UserEvent source, UserEventVO target, UserEventFetchOptions fetchOptions, boolean copyIfNull) {
        Beans.copyProperties(source, target,
            // Do not fetch 'content' (should be lazy loaded, only if fetched)
            UserEventVO.Fields.CONTENT);

        target.setLevel(EventLevelEnum.valueOfOrNull(source.getLevel()));
        target.setType(EventTypeEnum.valueOfOrNull(source.getType()));

        // Content
        if (fetchOptions != null && fetchOptions.isWithContent()) {
            target.setContent(source.getContent());
        }
    }
}
