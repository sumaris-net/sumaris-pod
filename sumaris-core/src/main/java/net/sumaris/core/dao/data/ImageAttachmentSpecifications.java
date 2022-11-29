package net.sumaris.core.dao.data;

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.ImageAttachment;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;

public interface ImageAttachmentSpecifications extends DataSpecifications<Integer, ImageAttachment> {

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
