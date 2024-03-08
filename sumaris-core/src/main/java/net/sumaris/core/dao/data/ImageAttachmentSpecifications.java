package net.sumaris.core.dao.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2022 SUMARiS Consortium
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

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.ImageAttachment;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;

public interface ImageAttachmentSpecifications extends IDataSpecifications<Integer, ImageAttachment> {

    default Specification<ImageAttachment> hasRecorderPersonId(Integer recorderPersonId) {
        if (recorderPersonId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, ImageAttachment.Fields.RECORDER_PERSON);
            return cb.equal(root.get(ImageAttachment.Fields.RECORDER_PERSON).get(IEntity.Fields.ID), param);
        }).addBind(ImageAttachment.Fields.RECORDER_PERSON, recorderPersonId);
    }

    default Specification<ImageAttachment> hasObjectId(Integer objectId) {
        if (objectId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, ImageAttachment.Fields.OBJECT_ID);
            return cb.equal(root.get(ImageAttachment.Fields.OBJECT_ID), param);
        }).addBind(ImageAttachment.Fields.OBJECT_ID, objectId);
    }

    default Specification<ImageAttachment> hasObjectTypeId(Integer objectTypeId) {
        if (objectTypeId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, ImageAttachment.Fields.OBJECT_TYPE);
            return cb.equal(root.get(ImageAttachment.Fields.OBJECT_TYPE).get(IEntity.Fields.ID), param);
        }).addBind(ImageAttachment.Fields.OBJECT_TYPE, objectTypeId);
    }
}
