package net.sumaris.core.dao.data;

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

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.administration.user.DepartmentDao;
import net.sumaris.core.util.Beans;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.ImageAttachment;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository("imageAttachmentDao")
public class ImageAttachmentDaoImpl extends HibernateDaoSupport implements ImageAttachmentDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(ImageAttachmentDaoImpl.class);

    @Autowired
    private DepartmentDao departmentDao;

    @Override
    public ImageAttachmentVO get(int id) {
        return toImageAttachmentVO(get(ImageAttachment.class, id));
    }

    @Override
    public ImageAttachmentVO save(ImageAttachmentVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getContent());
        Preconditions.checkNotNull(source.getContentType());
        Preconditions.checkNotNull(source.getDateTime());
        Preconditions.checkNotNull(source.getRecorderDepartment());
        Preconditions.checkNotNull(source.getRecorderDepartment().getId());

        EntityManager entityManager = getEntityManager();
        ImageAttachment entity = null;
        if (source.getId() != null) {
            entity = get(ImageAttachment.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new ImageAttachment();
        }

        if (!isNew) {
            // Check update date
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            lockForUpdate(entity);
        }

        imageAttachmentVOToEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        if (isNew) {
            // Force creation date
            entity.setCreationDate(newUpdateDate);
            source.setCreationDate(newUpdateDate);

            entityManager.persist(entity);
            source.setId(entity.getId());
        } else {
            entityManager.merge(entity);
        }

        source.setUpdateDate(newUpdateDate);

        getEntityManager().flush();
        getEntityManager().clear();

        return source;
    }

    @Override
    public void delete(int id) {

        log.debug(String.format("Deleting image attachment {id=%s}...", id));
        delete(ImageAttachment.class, id);
    }

    @Override
    public ImageAttachmentVO toImageAttachmentVO(ImageAttachment source) {
        if (source == null) return null;
        ImageAttachmentVO target = new ImageAttachmentVO();

        Beans.copyProperties(source, target);

        // Department
        DepartmentVO department = departmentDao.toDepartmentVO(source.getRecorderDepartment());
        target.setRecorderDepartment(department);

        // Status
        target.setQualityFlagId(source.getQualityFlag().getId());

        return target;
    }

    /* -- protected methods -- */

    protected List<ImageAttachmentVO> toImageAttachmentVOs(List<ImageAttachment> source) {
        return source.stream()
                .map(this::toImageAttachmentVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void imageAttachmentVOToEntity(ImageAttachmentVO source, ImageAttachment target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Department
        if (copyIfNull || source.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null) {
                target.setRecorderDepartment(null);
            }
            else {
                target.setRecorderDepartment(load(Department.class, source.getRecorderDepartment().getId()));
            }
        }

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(null);
            }
            else {
                target.setQualityFlag(load(QualityFlag.class, source.getQualityFlagId()));
            }
        }

    }
}
