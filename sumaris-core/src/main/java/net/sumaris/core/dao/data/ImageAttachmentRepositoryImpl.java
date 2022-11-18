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
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.ImageAttachmentFilterVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;

/**
 * @author peck7 on 31/08/2020.
 */
@Slf4j
public class ImageAttachmentRepositoryImpl
    extends DataRepositoryImpl<ImageAttachment, ImageAttachmentVO, ImageAttachmentFilterVO, DataFetchOptions>
    implements ImageAttachmentSpecifications {

    protected ImageAttachmentRepositoryImpl(EntityManager entityManager) {
        super(ImageAttachment.class, ImageAttachmentVO.class, entityManager);
        setLockForUpdate(false);
        setCheckUpdateDate(false);
    }

    @Override
    public ImageAttachmentVO save(ImageAttachmentVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getContent());
        Preconditions.checkNotNull(source.getContentType());
        Preconditions.checkNotNull(source.getDateTime());
        Preconditions.checkNotNull(source.getRecorderDepartment());
        Preconditions.checkNotNull(source.getRecorderDepartment().getId());
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
    public void toVO(ImageAttachment source, ImageAttachmentVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        if (source.getObjectType() != null) {
            target.setObjectTypeId(source.getObjectType().getId());
        }
        else {
            target.setObjectTypeId(null);
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
    protected Specification<ImageAttachment> toSpecification(ImageAttachmentFilterVO filter, DataFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
                .and(hasObjectId(filter.getObjectId()))
                .and(hasObjectTypeId(filter.getObjectTypeId()));
    }
}
