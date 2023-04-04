package net.sumaris.core.dao.data;

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
import net.sumaris.core.model.data.ImageAttachment;
import net.sumaris.core.model.referential.ObjectType;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.ImageAttachmentFilterVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;

/**
 * @author peck7 on 31/08/2020.
 */
@Slf4j
public class ImageAttachmentRepositoryImpl
    extends DataRepositoryImpl<ImageAttachment, ImageAttachmentVO, ImageAttachmentFilterVO, ImageAttachmentFetchOptions>
    implements ImageAttachmentSpecifications {

    protected ImageAttachmentRepositoryImpl(EntityManager entityManager) {
        super(ImageAttachment.class, ImageAttachmentVO.class, entityManager);
        setLockForUpdate(false);
        setCheckUpdateDate(false);
    }

    @Override
    public ImageAttachmentVO save(ImageAttachmentVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getContentType());
        Preconditions.checkNotNull(source.getDateTime());
        Preconditions.checkNotNull(source.getRecorderDepartment());
        Preconditions.checkNotNull(source.getRecorderDepartment().getId());
        Preconditions.checkArgument(StringUtils.isNotBlank(source.getPath()) || source.getContent() != null);
        Preconditions.checkArgument(
                (source.getObjectId() == null && source.getObjectTypeId() == null)
                || (source.getObjectId() != null && source.getObjectTypeId() != null));

        return super.save(source);
    }

    @Override
    public void toEntity(ImageAttachmentVO source, ImageAttachment target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);
        // Recorder person
        DataDaos.copyRecorderPerson(getEntityManager(), source, target, copyIfNull);

        // Object type
        if (source.getObjectTypeId() != null || copyIfNull) {
            if (source.getObjectTypeId() == null) {
                target.setObjectType(null);
            }
            else {
                target.setObjectType(getReference(ObjectType.class, source.getObjectTypeId()));
            }
        }
    }

    @Override
    public void toVO(ImageAttachment source, ImageAttachmentVO target, ImageAttachmentFetchOptions fetchOptions, boolean copyIfNull) {

        Beans.copyProperties(source, target, ImageAttachment.Fields.CONTENT /*Avoid to fetch Lob properties*/);

        // Quality flag
        target.setQualityFlagId(source.getQualityFlag().getId());

        // Recorder department
        if (fetchOptions == null || fetchOptions.isWithRecorderDepartment()) {
            DepartmentVO recorderDepartment = departmentRepository.toVO(source.getRecorderDepartment());
            target.setRecorderDepartment(recorderDepartment);
        }

        // Object type
        if (source.getObjectType() != null) {
            target.setObjectTypeId(source.getObjectType().getId());
        }
        else {
            target.setObjectTypeId(null);
        }

        // Fetch content only not a file image (no path) and when need to be fetched (e.g. from ImageRestController)
        if (target.getPath() == null && fetchOptions != null && fetchOptions.isWithContent()) {
            target.setContent(source.getContent());
        }
    }

    @Override
    protected void onBeforeSaveEntity(ImageAttachmentVO source, ImageAttachment target, boolean isNew) {
        super.onBeforeSaveEntity(source, target, isNew);

        // When new entity: set the creation date
        if (isNew || target.getCreationDate() == null) {
            target.setCreationDate(target.getUpdateDate());
        }
    }

    @Override
    protected void onAfterSaveEntity(ImageAttachmentVO vo, ImageAttachment savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        if (isNew) {
            vo.setCreationDate(savedEntity.getCreationDate());
        }
    }

    @Override
    protected Specification<ImageAttachment> toSpecification(ImageAttachmentFilterVO filter, ImageAttachmentFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
                .and(hasRecorderPersonId(filter.getRecorderPersonId()))
                .and(hasObjectId(filter.getObjectId()))
                .and(hasObjectTypeId(filter.getObjectTypeId()));
    }
}
