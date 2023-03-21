package net.sumaris.core.dao.technical.device;

import net.sumaris.core.dao.administration.user.DepartmentRepository;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.ObjectType;
import net.sumaris.core.model.technical.device.DevicePosition;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.technical.device.DevicePositionVO;

import javax.annotation.Resource;
import javax.persistence.EntityManager;

public class DevicePositionRepositoryImpl
        extends SumarisJpaRepositoryImpl<DevicePosition, Integer, DevicePositionVO>
        implements DevicePositionSpecifications {

    @Resource
    private DepartmentRepository departmentRepository;

    @Resource
    private PersonRepository personRepository;

    @Resource
    private ReferentialDao referentialDao;

    public DevicePositionRepositoryImpl(EntityManager entityManager) {
        super(DevicePosition.class, DevicePositionVO.class, entityManager);
    }

    @Override
    public void toEntity(DevicePositionVO source, DevicePosition target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Object type
        Integer objectTypeId = source.getObjectTypeId() != null ? source.getObjectTypeId() : (source.getObjectType() != null ? source.getObjectType().getId() : null);
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
    public void toVO(DevicePosition source, DevicePositionVO target, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        // Object type
        if (copyIfNull || source.getObjectType() != null) {
            if (source.getObjectType() == null) {
                target.setObjectType(null);
            } else {
                target.setObjectType(referentialDao.toVO(source.getObjectType()));
            }
        }

        // Recorder department
        if (copyIfNull || source.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null) {
                target.setRecorderDepartment(null);
            } else {
                DepartmentVO departmentVO = departmentRepository.toVO(source.getRecorderDepartment());               ;
                target.setRecorderDepartment(departmentVO);
            }
        }

        // Recorder person
        if (copyIfNull || source.getRecorderPerson() != null) {
            if (source.getRecorderPerson() == null) {
                target.setRecorderPerson(null);
            } else {
                PersonVO personVO = personRepository.toVO(source.getRecorderPerson());
                target.setRecorderPerson(personVO);
            }
        }

    }
}
