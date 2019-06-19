package net.sumaris.core.dao.technical.extraction;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.sumaris.core.dao.administration.user.DepartmentDao;
import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.dao.data.DataDaos;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.schema.SumarisColumnMetadata;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.model.technical.extraction.ExtractionProductColumn;
import net.sumaris.core.model.technical.extraction.ExtractionProductTable;
import net.sumaris.core.model.technical.extraction.ExtractionProductValue;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.ExtractionBeans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductColumnVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductTableVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ProductFetchOptions;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.sql.Types;
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

    /**
     * Logger.
     */
    private static final Logger log =
            LoggerFactory.getLogger(ExtractionProductDaoImpl.class);

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private SumarisDatabaseMetadata databaseMetaData;

    @Override
    public List<ExtractionProductVO> findAllByStatus(List<Integer> statusIds, ProductFetchOptions fetchOptions) {
        Preconditions.checkNotNull(statusIds);
        Preconditions.checkArgument(statusIds.size() > 0);
        return getEntityManager().createQuery("from ExtractionProduct p where p.status.id IN (:statusIds)", ExtractionProduct.class)
                .setParameter("statusIds", statusIds)
                .getResultStream()
                .map(p -> toProductVO(p, fetchOptions))
                .collect(Collectors.toList());
    }

    @Override
    public List<ExtractionProductVO> getAll(ProductFetchOptions fetchOptions) {
        return getEntityManager().createQuery("from ExtractionProduct p where p.status.id!=0", ExtractionProduct.class)
                .getResultStream()
                .map(p -> toProductVO(p, fetchOptions))
                .collect(Collectors.toList());
    }

    @Override
    public ExtractionProductVO getByLabel(String label, ProductFetchOptions fetchOptions) {
        Preconditions.checkNotNull(label);
        return toProductVO(getEntityManager().createQuery("from ExtractionProduct p where p.label=:label", ExtractionProduct.class)
                        .setParameter("label", label.toUpperCase())
                        .getSingleResult(),
                fetchOptions);
    }

    @Override
    public Optional<ExtractionProductVO> get(int id, ProductFetchOptions fetchOptions) {
        try {
            return Optional.ofNullable(toProductVO(get(ExtractionProduct.class, id), fetchOptions));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<ExtractionProductColumnVO> getColumnsByIdAndTableLabel(int id, String tableLabel) {

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ExtractionProductColumn> query = cb.createQuery(ExtractionProductColumn.class);
        Root<ExtractionProductColumn> root = query.from(ExtractionProductColumn.class);

        query.select(root);

        ParameterExpression<Integer> productIdParam = cb.parameter(Integer.class);
        ParameterExpression<String> tableLabelParam = cb.parameter(String.class);

        query.where(
                cb.and(
                        cb.equal(root.get(ExtractionProductColumn.PROPERTY_TABLE)
                                .get(ExtractionProductTable.PROPERTY_PRODUCT)
                                .get(ExtractionProduct.PROPERTY_ID), productIdParam),
                        cb.equal(root.get(ExtractionProductColumn.PROPERTY_TABLE)
                                .get(ExtractionProductTable.PROPERTY_LABEL), tableLabelParam)
                )
        );

        // Sort by rank order
        query.orderBy(cb.asc(root.get(ExtractionProductColumn.PROPERTY_RANK_ORDER)));

        return getEntityManager().createQuery(query)
                .setParameter(productIdParam, id)
                .setParameter(tableLabelParam, tableLabel)
                .getResultStream()
                .map(c -> toColumnVO(c, null))
                .collect(Collectors.toList());
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
        } else {
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

        getSession().flush();
        getSession().clear();

        // Save tables
        saveProductTables(source.getTables(), source.getId(), newUpdateDate);

        // Final merge
        //entityManager.merge(entity);

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
            } else {
                target.setStatus(load(Status.class, source.getStatusId()));
            }
        }

        // Parent
        if (copyIfNull || source.getParentId() != null) {
            if (source.getParentId() == null) {
                target.setParent(null);
            } else {
                target.setParent(load(ExtractionProduct.class, source.getParentId()));
            }
        }

        // Recorder department
        DataDaos.copyRecorderDepartment(getEntityManager(), source, target, copyIfNull);
        // Recorder person
        DataDaos.copyRecorderPerson(getEntityManager(), source, target, copyIfNull);

    }

    protected void saveProductTables(List<ExtractionProductTableVO> sources, int productId, Timestamp updateDate) {
        ExtractionProduct parent = get(ExtractionProduct.class, productId);

        final EntityManager em = getEntityManager();
        if (CollectionUtils.isEmpty(sources)) {
            if (parent.getTables() != null) {
                List<ExtractionProductTable> toRemove = ImmutableList.copyOf(parent.getTables());
                parent.getTables().clear();
                toRemove.stream().forEach(em::remove);
            }
        } else {
            Map<String, ExtractionProductTable> existingItems = Beans.splitByProperty(
                    Beans.getList(parent.getTables()),
                    ExtractionProductTable.PROPERTY_LABEL);
            final Status enableStatus = load(Status.class, StatusEnum.ENABLE.getId());
            if (parent.getTables() == null) {
                parent.setTables(Lists.newArrayList());
            }

            // Save each table
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
                            source.setId(target.getId());
                        } else {
                            em.merge(target);
                        }

                        source.setUpdateDate(updateDate);
                    });

            getSession().flush();

            // Save each columns
            sources.stream()
                    .filter(Objects::nonNull)
                    .forEach(source -> saveProductTableColumns(source.getColumns(), source.getId(), updateDate));

            getSession().flush();

            // Remove old tables
            if (MapUtils.isNotEmpty(existingItems)) {
                parent.getTables().removeAll(existingItems.values());
                existingItems.values().stream().forEach(em::remove);
            }

        }
    }

    protected void saveProductTableColumns(List<ExtractionProductColumnVO> sources, int tableId, Timestamp updateDate) {
        final EntityManager em = getEntityManager();

        // Load parent
        ExtractionProductTable parent = get(ExtractionProductTable.class, tableId);

        if (CollectionUtils.isEmpty(sources)) {
            if (parent.getColumns() != null) {
                List<ExtractionProductColumn> toRemove = ImmutableList.copyOf(parent.getColumns());
                parent.getColumns().clear();
                toRemove.stream().forEach(em::remove);
            }
        } else {
            Map<String, ExtractionProductColumn> existingItems = Beans.splitByProperty(
                    Beans.getList(parent.getColumns()),
                    ExtractionProductColumn.PROPERTY_COLUMN_NAME);
            if (parent.getColumns() == null) {
                parent.setColumns(Lists.newArrayList());
            }

            // Save each column
            sources.stream()
                    .filter(Objects::nonNull)
                    .forEach(source -> {
                        ExtractionProductColumn target = existingItems.remove(source.getColumnName());
                        boolean isNew = (target == null);
                        if (isNew) {
                            target = new ExtractionProductColumn();
                        }
                        target.setTable(parent);
                        Beans.copyProperties(source, target);
                        target.setLabel(StringUtils.underscoreToChangeCase(source.getColumnName()));

                        if (isNew) {
                            em.persist(target);
                            source.setId(target.getId());
                        } else {
                            em.merge(target);
                        }
                    });

            getSession().flush();
            getSession().clear();

            // Save column values
            sources.stream()
                    .filter(Objects::nonNull)
                    .forEach(source -> saveProductTableValues(source.getValues(), source.getId()));

            getSession().flush();
            getSession().clear();

            // Remove old tables
            if (MapUtils.isNotEmpty(existingItems)) {
                parent.getColumns().removeAll(existingItems.values());
                existingItems.values().stream().forEach(em::remove);
            }

            getSession().flush();
            getSession().clear();
        }
    }

    protected void saveProductTableValues(List<String> sources, int columnId) {
        ExtractionProductColumn parent = get(ExtractionProductColumn.class, columnId);

        final EntityManager em = getEntityManager();
        if (CollectionUtils.isEmpty(sources)) {
            if (parent.getValues() != null) {
                List<ExtractionProductValue> toRemove = ImmutableList.copyOf(parent.getValues());
                parent.getValues().clear();
                toRemove.stream().forEach(em::remove);
            }
        } else {
            Map<String, ExtractionProductValue> existingItems = Beans.splitByProperty(
                    Beans.getList(parent.getValues()),
                    ExtractionProductValue.PROPERTY_LABEL);
            if (parent.getValues() == null) {
                parent.setValues(Lists.newArrayList());
            }

            // Transform each entry into entity
            sources.stream()
                    .filter(StringUtils::isNotBlank)
                    .forEach(valueLabel -> {
                        ExtractionProductValue target = existingItems.remove(valueLabel);
                        boolean isNew = (target == null);
                        if (isNew) {
                            target = new ExtractionProductValue();
                        }
                        target.setColumn(parent);
                        target.setLabel(valueLabel);
                        if (isNew) {
                            em.persist(target);
                        } else {
                            em.merge(target);
                        }
                    });

            getSession().flush();
            getSession().clear();

            // Remove old values
            if (MapUtils.isNotEmpty(existingItems)) {
                parent.getValues().removeAll(existingItems.values());
                existingItems.values().stream().forEach(em::remove);
            }

        }
    }

    protected ExtractionProductVO toProductVO(ExtractionProduct source, ProductFetchOptions fetchOptions) {
        ExtractionProductVO target = new ExtractionProductVO();
        Beans.copyProperties(source, target);


        // Status
        if (source.getStatus() != null) {
            target.setStatusId(source.getStatus().getId());
        }

        // Tables
        if (fetchOptions == null || fetchOptions.isWithTables()) {
            if (CollectionUtils.isNotEmpty(source.getTables())) {
                List<ExtractionProductTableVO> tables = source.getTables().stream()
                        .map(t -> toProductTableVO(t, fetchOptions))
                        .collect(Collectors.toList());
                target.setTables(tables);
            }
        }

        // Recorder department and person
        if (fetchOptions == null || fetchOptions.isWithRecorderDepartment()) {
            target.setRecorderDepartment(departmentDao.toDepartmentVO(source.getRecorderDepartment()));
        }

        if (fetchOptions == null || fetchOptions.isWithRecorderPerson()) {
            target.setRecorderPerson(personDao.toPersonVO(source.getRecorderPerson()));
        }

        return target;

    }

    protected ExtractionProductTableVO toProductTableVO(ExtractionProductTable source, ProductFetchOptions fetchOptions) {
        ExtractionProductTableVO target = new ExtractionProductTableVO();
        Beans.copyProperties(source, target);

        // parent
        if (source.getProduct() != null) {
            target.setProductId(source.getProduct().getId());
        }

        // Status
        if (source.getStatus() != null) {
            target.setStatusId(source.getStatus().getId());
        }

        // Columns
        if (fetchOptions == null || fetchOptions.isWithColumns()) {
            if (CollectionUtils.isNotEmpty(source.getColumns())) {
                target.setColumns(source.getColumns().stream()
                .map(c -> toColumnVO(c, fetchOptions))
                .collect(Collectors.toList()));
            }
        }

        return target;

    }

    protected List<String> toColumnValues(ExtractionProductColumn source) {
        if (source == null || CollectionUtils.isEmpty(source.getValues())) return null;
        return source.getValues().stream()
                .map(ExtractionProductValue::getLabel)
                .collect(Collectors.toList());
    }

    protected ExtractionProductColumnVO toColumnVO(ExtractionProductColumn source, ProductFetchOptions fetchOptions) {
        ExtractionProductColumnVO target = new ExtractionProductColumnVO();
        Beans.copyProperties(source, target);

        if (fetchOptions == null || fetchOptions.isWithColumnValues()) {
            target.setValues(toColumnValues(source));
        }
        return target;
    }


}
