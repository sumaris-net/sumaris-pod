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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.VesselPosition;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.VesselPositionVO;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository("vesselPositionDao")
@RequiredArgsConstructor
@Slf4j
public class VesselPositionDaoImpl extends HibernateDaoSupport implements VesselPositionDao {

    static {
        I18n.n("sumaris.persistence.table.vesselPosition");
    }

    private final ReferentialDao referentialDao;

    @Override
    public List<VesselPositionVO> getAllByOperationId(int operationId) {
        return getAllByOperationId(operationId, 0, 1000, VesselPositionVO.Fields.DATE_TIME, SortDirection.ASC);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VesselPositionVO> getAllByOperationId(int operationId, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        // Fetch locations
        //getEntityManager().enableFetchProfile("with-location");

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<VesselPosition> query = builder.createQuery(VesselPosition.class);
        Root<VesselPosition> root = query.from(VesselPosition.class);

        ParameterExpression<Integer> operationIdParam = builder.parameter(Integer.class);

        query.select(root)
            .where(builder.equal(root.get(VesselPosition.Fields.OPERATION).get(Operation.Fields.ID), operationIdParam));

        // Add sorting
        if (StringUtils.isNotBlank(sortAttribute)) {
            Expression<?> sortExpression = root.get(sortAttribute);
            query.orderBy(SortDirection.DESC.equals(sortDirection) ?
                    builder.desc(sortExpression) :
                    builder.asc(sortExpression)
            );
        }

        TypedQuery<VesselPosition> q = getEntityManager().createQuery(query)
                .setParameter(operationIdParam, operationId)
                .setFirstResult(offset)
                .setMaxResults(size);
        return toVesselPositionVOs(q.getResultList());
    }

    @Override
    public VesselPositionVO get(int id) {
        VesselPosition entity = getEntityManager().find(VesselPosition.class, id);
        return toMeasurementVO(entity);
    }

    @Override
    public List<VesselPositionVO> saveByOperationId(int operationId, List<VesselPositionVO> sources) {

        // Load parent entity
        Operation parent = getById(Operation.class, operationId);

        // Remember existing entities
        final Map<Integer, VesselPosition> sourcesToRemove = Beans.splitById(Beans.getList(parent.getPositions()));

        // Save each gears
        List<VesselPositionVO> result = sources.stream().map(gear -> {
            gear.setOperationId(operationId);
            if (gear.getId() != null) {
                sourcesToRemove.remove(gear.getId());
            }
            return save(gear);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (MapUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.values().forEach(this::delete);
        }

        return result;
    }

    @Override
    public VesselPositionVO save(VesselPositionVO source) {
        Preconditions.checkNotNull(source);

        EntityManager entityManager = getEntityManager();
        VesselPosition entity = null;
        if (source.getId() != null) {
            entity = entityManager.find(VesselPosition.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new VesselPosition();
        }

        if (!isNew) {
            // Check update date
            Daos.checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            lockForUpdate(entity);
        }

        // VO -> Entity
        vesselPositionVOToEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        if (isNew) {
            entityManager.persist(entity);
            source.setId(entity.getId());
        } else {
            entityManager.merge(entity);
        }

        source.setUpdateDate(newUpdateDate);

        //session.flush();
        //session.clear();

        return source;
    }

    @Override
    public void delete(int id) {

        log.debug(String.format("Deleting vesselPosition {id=%s}...", id));
        delete(VesselPosition.class, id);
    }

    @Override
    public VesselPositionVO toMeasurementVO(VesselPosition source) {
        if (source == null) return null;

        VesselPositionVO target = new VesselPositionVO();

        Beans.copyProperties(source, target);

        target.setQualityFlagId(source.getQualityFlag().getId());

        // Recorder department
        DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
        target.setRecorderDepartment(recorderDepartment);

        return target;
    }

    /* -- protected methods -- */

    protected List<VesselPositionVO> toVesselPositionVOs(List<VesselPosition> source) {
        return source.stream()
                .map(this::toMeasurementVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void vesselPositionVOToEntity(VesselPositionVO source, VesselPosition target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Operation
        Integer operationId = source.getOperationId() != null ? source.getOperationId() : (source.getOperation() != null ? source.getOperation().getId() : null);
        if (copyIfNull || (operationId != null)) {
            if (operationId == null) {
                target.setOperation(null);
            }
            else {
                target.setOperation(getReference(Operation.class, operationId));
            }
        }

        // Recorder department
        if (copyIfNull || source.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null || source.getRecorderDepartment().getId() == null) {
                target.setRecorderDepartment(null);
            }
            else {
                target.setRecorderDepartment(getReference(Department.class, source.getRecorderDepartment().getId()));
            }
        }

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(getReference(QualityFlag.class, getConfig().getDefaultQualityFlagId()));
            }
            else {
                target.setQualityFlag(getReference(QualityFlag.class, source.getQualityFlagId()));
            }
        }

    }
}
