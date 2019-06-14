package net.sumaris.core.dao.technical.extraction;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.sumaris.core.dao.administration.user.DepartmentDao;
import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.dao.data.DataDaos;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.model.technical.extraction.ExtractionProductColumn;
import net.sumaris.core.model.technical.extraction.ExtractionProductTable;
import net.sumaris.core.model.technical.extraction.ExtractionProductValue;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.technical.extraction.ExtractionProductTableVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Repository("extractionProductDao")
public class ExtractionProductDaoImpl extends HibernateDaoSupport implements ExtractionProductDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(ExtractionProductDaoImpl.class);

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private PersonDao personDao;

    @Override
    public List<ExtractionProductVO> findAllByStatus(List<Integer> statusIds) {
        Preconditions.checkNotNull(statusIds);
        Preconditions.checkArgument(statusIds.size() > 0);
        return getEntityManager().createQuery("from ExtractionProduct p where p.status.id IN (:statusIds)", ExtractionProduct.class)
                .setParameter("statusIds", statusIds)
                .getResultStream()
                .map(this::toProductVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExtractionProductVO> getAll() {
        return getEntityManager().createQuery("from ExtractionProduct p where p.status.id!=0", ExtractionProduct.class)
                .getResultStream()
                .map(this::toProductVO)
                .collect(Collectors.toList());
    }

    @Override
    public ExtractionProductVO getByLabel(String label) {
        Preconditions.checkNotNull(label);
        return toProductVO(getEntityManager().createQuery("from ExtractionProduct p where p.label=:label", ExtractionProduct.class)
                .setParameter("label", label.toUpperCase())
                .getSingleResult()
        );
    }

    @Override
    public Optional<ExtractionProductVO> get(Integer id) {
        try {
            return Optional.ofNullable(toProductVO(get(ExtractionProduct.class, id)));
        }
        catch(Exception e){
            return Optional.empty();
        }
    }

    @Override
    public ExtractionProductVO save(ExtractionProductVO source) {
        EntityManager entityManager = getEntityManager();
        ExtractionProduct entity = null;
        if (source.getId() != null) {
            entity = get(ExtractionProduct.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new ExtractionProduct();
        }

        else {
            // Check update date
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            lockForUpdate(entity);
        }

        // VO -> Entity
        productVOToEntity(source, entity, true);

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
        }

        source.setUpdateDate(newUpdateDate);
        source.setLabel(entity.getLabel());

        // Save tables
        saveProductTables(source.getTables(), entity, newUpdateDate);

        // Final merge
        entityManager.merge(entity);

        return source;
    }

    @Override
    public void delete(int id) {
        log.debug(String.format("Deleting product {id=%s}...", id));
        delete(ExtractionProduct.class, id);
    }

    /* -- protected method -- */

    protected void productVOToEntity(ExtractionProductVO source, ExtractionProduct target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Make sure label is uppercase
        if (source.getLabel() != null) {
            String label = source.getLabel().toUpperCase();
            target.setLabel(label);
        }

        // status
        if (copyIfNull || source.getStatusId() != null) {
            if (source.getStatusId() == null) {
                target.setStatus(null);
            }
            else {
                target.setStatus(load(Status.class, source.getStatusId()));
            }
        }

        // Parent
        if (copyIfNull || source.getParentId() != null) {
            if (source.getParentId() == null) {
                target.setParent(null);
            }
            else {
                target.setParent(load(ExtractionProduct.class, source.getParentId()));
            }
        }

        // Recorder department
        DataDaos.copyRecorderDepartment(getEntityManager(), source, target, copyIfNull);
        // Recorder person
        DataDaos.copyRecorderPerson(getEntityManager(), source, target, copyIfNull);

    }

    protected void saveProductTables(List<ExtractionProductTableVO> sources, ExtractionProduct parent, Timestamp updateDate) {
        final EntityManager em = getEntityManager();
        if (CollectionUtils.isEmpty(sources)) {
            if (parent.getTables() != null) {
                List<ExtractionProductTable> toRemove = ImmutableList.copyOf(parent.getTables());
                parent.getTables().clear();
                toRemove.stream().forEach(em::remove);
            }
        }
        else {
            Map<String, ExtractionProductTable> existingItems = Beans.splitByProperty(
                    Beans.getList(parent.getTables()),
                    ExtractionProductTable.PROPERTY_LABEL);
            final Status enableStatus = load(Status.class, StatusEnum.ENABLE.getId());
            if (parent.getTables() == null) {
                parent.setTables(Lists.newArrayList());
            }

            // Transform each entry
            sources.stream()
                    .filter(Objects::nonNull)
                    .forEach(source -> {
                        ExtractionProductTable target = existingItems.remove(source.getLabel());
                        boolean isNew = (target == null);
                        if (isNew) {
                            target = new ExtractionProductTable();
                        }
                        Beans.copyProperties(source, target);
                        target.setProduct(parent);
                        target.setStatus(enableStatus);
                        target.setUpdateDate(updateDate);
                        if (isNew) {
                            target.setCreationDate(updateDate);
                            em.persist(target);
                        }
                        else {
                            em.merge(target);
                        }

                        // Save column
                        saveProductTableColumns(source.getColumnValues(), target, updateDate);
                    });

            // Remove old tables
            if (MapUtils.isNotEmpty(existingItems)) {
                parent.getTables().removeAll(existingItems.values());
                existingItems.values().stream().forEach(em::remove);
            }

        }
    }

    protected void saveProductTableColumns(Map<String, List<Object>> sources, ExtractionProductTable parent, Timestamp updateDate) {
        final EntityManager em = getEntityManager();
        if (MapUtils.isEmpty(sources)) {
            if (parent.getColumns() != null) {
                List<ExtractionProductColumn> toRemove = ImmutableList.copyOf(parent.getColumns());
                parent.getColumns().clear();
                toRemove.stream().forEach(em::remove);
            }
        }
        else {
            Map<String, ExtractionProductColumn> existingItems = Beans.splitByProperty(
                    Beans.getList(parent.getColumns()),
                    ExtractionProductColumn.PROPERTY_NAME);
            if (parent.getColumns() == null) {
                parent.setColumns(Lists.newArrayList());
            }

            // Transform each entry into entity
            sources.keySet().stream()
                    .filter(Objects::nonNull)
                    .forEach(columnName -> {
                        ExtractionProductColumn target = existingItems.remove(columnName);
                        boolean isNew = (target == null);
                        if (isNew) {
                            target = new ExtractionProductColumn();
                            target.setName(columnName);
                            target.setTable(parent);
                        }
                        if (isNew) {
                            em.persist(target);
                        }
                        else {
                            em.merge(target);
                        }

                        // Save values
                        saveProductTableValues(sources.get(columnName), target, updateDate);
                    });

            // Remove old tables
            if (MapUtils.isNotEmpty(existingItems)) {
                parent.getColumns().removeAll(existingItems.values());
                existingItems.values().stream().forEach(em::remove);
            }

        }
    }

    protected void saveProductTableValues(List<Object> sources, ExtractionProductColumn parent, Timestamp updateDate) {
        final EntityManager em = getEntityManager();
        if (CollectionUtils.isEmpty(sources)) {
            if (parent.getValues() != null) {
                List<ExtractionProductValue> toRemove = ImmutableList.copyOf(parent.getValues());
                parent.getValues().clear();
                toRemove.stream().forEach(em::remove);
            }
        }
        else {
            Map<String, ExtractionProductValue> existingItems = Beans.splitByProperty(
                    Beans.getList(parent.getValues()),
                    ExtractionProductValue.PROPERTY_LABEL);
            if (parent.getValues() == null) {
                parent.setValues(Lists.newArrayList());
            }

            // Transform each entry into entity
            sources.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .forEach(valueLabel -> {
                        ExtractionProductValue target = existingItems.remove(valueLabel);
                        boolean isNew = (target == null);
                        if (isNew) {
                            target = new ExtractionProductValue();
                            target.setColumn(parent);
                        }
                        target.setLabel(valueLabel);
                        if (isNew) {
                            em.persist(target);
                        }
                        else {
                            em.merge(target);
                        }
                    });

            // Remove old tables
            if (MapUtils.isNotEmpty(existingItems)) {
                parent.getValues().removeAll(existingItems.values());
                existingItems.values().stream().forEach(em::remove);
            }

        }
    }

    protected ExtractionProductVO toProductVO(ExtractionProduct source) {
        ExtractionProductVO target = new ExtractionProductVO();
        Beans.copyProperties(source, target);

        // tables
        if (CollectionUtils.isNotEmpty(source.getTables())) {
            List<ExtractionProductTableVO> tables = source.getTables().stream()
                    .map(this::toProductTableVO)
                    .collect(Collectors.toList());
            target.setTables(tables);
        }

        // Status
        if (source.getStatus() != null) {
            target.setStatusId(source.getId());
        }

        // Recorder department and person
        target.setRecorderDepartment(departmentDao.toDepartmentVO(source.getRecorderDepartment()));
        target.setRecorderPerson(personDao.toPersonVO(source.getRecorderPerson()));

        return target;

    }

    protected ExtractionProductTableVO toProductTableVO(ExtractionProductTable source) {
        ExtractionProductTableVO target = new ExtractionProductTableVO();
        Beans.copyProperties(source, target);

        // parent
        if (source.getProduct() != null) {
            target.setProductId(source.getProduct().getId());
        }

        // Status
        if (source.getStatus() != null) {
            target.setStatusId(source.getId());
        }

        // Column values
        if (CollectionUtils.isNotEmpty(source.getColumns())) {
            target.setColumnValues(
                    source.getColumns().stream()
                    .collect(Collectors.toMap(
                            ExtractionProductColumn::getName,
                            this::toColumnValues
                    ))
            );
        }

        return target;

    }

    protected List<Object> toColumnValues(ExtractionProductColumn source) {
        if (source == null || CollectionUtils.isEmpty(source.getValues())) return null;
        return source.getValues().stream()
                .map(v -> v.getLabel())
                .collect(Collectors.toList());
    }
}
