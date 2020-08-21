package net.sumaris.core.dao.referential.pmfm;

import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ParameterVO;
import net.sumaris.core.vo.referential.ParameterValueType;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author peck7 on 19/08/2020.
 */
public class ParameterRepositoryImpl
    extends ReferentialRepositoryImpl<Parameter, ParameterVO, ReferentialFilterVO> {

    @Autowired
    private ReferentialDao referentialDao;

    public ParameterRepositoryImpl(EntityManager entityManager) {
        super(Parameter.class, ParameterVO.class, entityManager);
    }

    @Override
    public void toEntity(ParameterVO source, Parameter target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Type
        if (copyIfNull || source.getType() != null) {

            ParameterValueType type = Optional.ofNullable(source.getType())
                .map(ParameterValueType::fromString)
                .orElse(ParameterValueType.DOUBLE);

            switch (type) {
                case STRING:
                    target.setIsAlphanumeric(true);
                    target.setIsBoolean(false);
                    target.setIsDate(false);
                    target.setIsQualitative(false);
                    break;
                case BOOLEAN:
                    target.setIsAlphanumeric(false);
                    target.setIsBoolean(true);
                    target.setIsDate(false);
                    target.setIsQualitative(false);
                    break;
                case DATE:
                    target.setIsAlphanumeric(false);
                    target.setIsBoolean(false);
                    target.setIsDate(true);
                    target.setIsQualitative(false);
                    break;
                case QUALITATIVE_VALUE:
                    target.setIsAlphanumeric(false);
                    target.setIsBoolean(false);
                    target.setIsDate(false);
                    target.setIsQualitative(true);
                    break;
                case DOUBLE:
                default:
                    target.setIsAlphanumeric(false);
                    target.setIsBoolean(false);
                    target.setIsDate(false);
                    target.setIsQualitative(false);
                    break;
            }
        }
    }

    @Override
    protected void toVO(Parameter source, ParameterVO target, ReferentialFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Type
        ParameterValueType type = ParameterValueType.fromParameter(source);
        target.setType(type.name().toLowerCase());

        // Qualitative values: from pmfm first, or (if empty) from parameter
        if (CollectionUtils.isNotEmpty(source.getQualitativeValues())) {
            List<ReferentialVO> qualitativeValues = source.getQualitativeValues()
                .stream()
                .map(referentialDao::toReferentialVO)
                .collect(Collectors.toList());
            target.setQualitativeValues(qualitativeValues);
        }

        // EntityName (as metadata - see ReferentialVO)
        target.setEntityName(Parameter.class.getSimpleName());

    }

    @Override
    protected void onAfterSaveEntity(ParameterVO vo, Parameter savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        List<ReferentialVO> savedQv = saveQualitativeValues(vo.getId(), vo.getQualitativeValues());
        vo.setQualitativeValues(savedQv);

    }

    private List<ReferentialVO> saveQualitativeValues(Integer parameterId, List<ReferentialVO> qualitativeValues) {
        Parameter parent = getOne(parameterId);
        Date newUpdateDate = parent.getUpdateDate();

        // Remember existing QV
        List<QualitativeValue> entities = Beans.getList(parent.getQualitativeValues());
        Map<Integer, QualitativeValue> entitiesToRemove = Beans.splitByProperty(entities, QualitativeValue.Fields.ID);

        Beans.getStream(qualitativeValues).forEach(qvSource -> {
            QualitativeValue qvTarget = null;
            if (qvSource.getId() != null) {
                qvTarget = entitiesToRemove.remove(qvSource.getId());
            }
            boolean isNew = qvTarget == null;
            if (qvTarget == null) {
                qvTarget = new QualitativeValue();
            }

            // Fill VO properties to entity
            Beans.copyProperties(qvSource, qvTarget);
            Daos.setEntityProperty(getEntityManager(), qvTarget, Pmfm.Fields.STATUS,
                Status.class, qvSource.getStatusId());
            qvTarget.setParameter(parent);
            qvTarget.setUpdateDate(newUpdateDate);

            // Save entity
            if (isNew) {
                parent.getQualitativeValues().add(qvTarget);
                qvTarget.setCreationDate(newUpdateDate);
                getEntityManager().persist(qvTarget);
            }
            else {
                getEntityManager().merge(qvTarget);
            }

            qvSource.setId(qvTarget.getId());
            qvSource.setCreationDate(qvTarget.getCreationDate());
            qvSource.setUpdateDate(qvTarget.getUpdateDate());
        });

        // Remove orphan
        if (!entitiesToRemove.isEmpty()) {
            entities.removeAll(entitiesToRemove.values());
            entitiesToRemove.values().forEach(getEntityManager()::remove);
        }

        parent.setQualitativeValues(entities);
        getEntityManager().merge(parent);

        return qualitativeValues;
    }
}
