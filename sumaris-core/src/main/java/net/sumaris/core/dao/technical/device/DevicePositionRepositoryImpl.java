package net.sumaris.core.dao.technical.device;

import lombok.RequiredArgsConstructor;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.data.DataRepositoryImpl;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.ObjectType;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.model.technical.device.DevicePosition;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.technical.device.DevicePositionFilterVO;
import net.sumaris.core.vo.technical.device.DevicePositionVO;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import java.util.Date;

@RequiredArgsConstructor
public class DevicePositionRepositoryImpl
        extends DataRepositoryImpl<DevicePosition, DevicePositionVO, DevicePositionFilterVO, DataFetchOptions>
        implements DevicePositionSpecifications {

    private final PersonRepository personRepository;

    public DevicePositionRepositoryImpl(EntityManager entityManager,
                                        PersonRepository personRepository) {
        super(DevicePosition.class, DevicePositionVO.class, entityManager);
        this.personRepository = personRepository;
    }

    @Override
    public void toEntity(DevicePositionVO source, DevicePosition target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        if (target.getId() == null || target.getCreationDate() == null) {
            target.setCreationDate(new Date());
        }

        // Object type
        Integer objectTypeId =  ObjectTypeEnum.byLabel(source.getObjectType().getLabel()).getId();
        if (copyIfNull || (objectTypeId != null)) {
            if (objectTypeId == null) {
                target.setObjectType(null);
            } else {
                target.setObjectType(getReference(ObjectType.class, objectTypeId));
            }
        }

        // Recorder department
        Integer recorderDepartmentId = source.getRecorderDepartmentId() != null ? source.getRecorderDepartmentId() : (source.getRecorderDepartment() != null ? source.getRecorderDepartment().getId() : null);
        if (copyIfNull || (recorderDepartmentId != null)) {
            if (recorderDepartmentId == null) {
                target.setRecorderDepartment(null);
            } else {
                target.setRecorderDepartment(getReference(Department.class, recorderDepartmentId));
            }
        }

        // Recorder person
        Integer recorderPersonId = source.getRecorderPersonId() != null ? source.getRecorderPersonId() : (source.getRecorderPerson() != null ? source.getRecorderPerson().getId() : null);
        if (copyIfNull || (recorderPersonId != null)) {
            if (recorderPersonId == null) {
                target.setRecorderPerson(null);
            } else {
                target.setRecorderPerson(getReference(Person.class, recorderPersonId));
            }
        }
    }

    @Override
    public void toVO(DevicePosition source,
                     DevicePositionVO target,
                     DataFetchOptions fetchOptions,
                     boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Object type
        ReferentialVO objectType = ReferentialVO.builder()
                .label(source.getObjectType().getLabel())
                .build();
        target.setObjectType(objectType);

        // Recorder person
        if ((fetchOptions == null || fetchOptions.isWithRecorderPerson()) && source.getRecorderPerson() != null) {
            PersonVO recorderPerson = personRepository.toVO(source.getRecorderPerson(), PERSON_FETCH_OPTIONS);
            target.setRecorderPerson(recorderPerson);
        }

    }

    @Override
    protected Specification<DevicePosition> toSpecification(DevicePositionFilterVO filter, DataFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(isBetweenDates(filter.getStartDate(), filter.getEndDate()))
            .and(hasRecorderPersonId(filter.getRecorderPersonId()))
            .and(hasObjectTypeLabel(filter.getObjectTypeLabel()))
            .and(hasObjectTypeId(filter.getObjectTypeId()))
            .and(hasObjectId(filter.getObjectTypeId()));
    }
}
