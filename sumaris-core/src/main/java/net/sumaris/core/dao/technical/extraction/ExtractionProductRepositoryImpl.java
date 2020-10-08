package net.sumaris.core.dao.technical.extraction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.sumaris.core.dao.administration.user.DepartmentRepository;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.dao.data.DataDaos;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.*;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author peck7 on 21/08/2020.
 */
public class ExtractionProductRepositoryImpl
    extends ReferentialRepositoryImpl<ExtractionProduct, ExtractionProductVO, ExtractionProductFilterVO, ProductFetchOptions>
    implements ExtractionProductSpecifications {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PersonRepository personRepository;

    protected ExtractionProductRepositoryImpl(EntityManager entityManager) {
        super(ExtractionProduct.class, ExtractionProductVO.class, entityManager);
        setLockForUpdate(true);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PRODUCTS_BY_FILTER)
    public List<ExtractionProductVO> findAll(ExtractionProductFilterVO filter, ProductFetchOptions fetchOptions) {
        return super.findAll(filter, fetchOptions);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PRODUCT_BY_LABEL, key = "#label")
    public ExtractionProductVO getByLabel(String label, ProductFetchOptions fetchOption) {
        return super.getByLabel(label, fetchOption);
    }

    @Override
    protected Specification<ExtractionProduct> toSpecification(ExtractionProductFilterVO filter) {
        return super.toSpecification(filter)
            .and(withDepartmentId(filter.getDepartmentId()));
    }

    @Override
    protected void toVO(ExtractionProduct source, ExtractionProductVO target, ProductFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Tables
        if (fetchOptions == null || fetchOptions.isWithTables()) {
            if (CollectionUtils.isNotEmpty(source.getTables())) {
                List<ExtractionProductTableVO> tables = source.getTables().stream()
                    .map(t -> toProductTableVO(t, fetchOptions))
                    .collect(Collectors.toList());
                target.setTables(tables);
            }
        }

        // Stratum
        if (fetchOptions == null || fetchOptions.isWithStratum()) {
            if (CollectionUtils.isNotEmpty(source.getStratum())) {
                List<ExtractionProductStrataVO> stratum = source.getStratum().stream()
                    .map(this::toProductStrataVO)
                    .collect(Collectors.toList());
                target.setStratum(stratum);
            }
        }

        // Recorder department and person
        if (fetchOptions == null || fetchOptions.isWithRecorderDepartment()) {
            target.setRecorderDepartment(departmentRepository.toVO(source.getRecorderDepartment()));
        }
        if (fetchOptions == null || fetchOptions.isWithRecorderPerson()) {
            target.setRecorderPerson(personRepository.toVO(source.getRecorderPerson()));
        }
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

    protected ExtractionProductStrataVO toProductStrataVO(ExtractionProductStrata source) {
        ExtractionProductStrataVO target = new ExtractionProductStrataVO();
        Beans.copyProperties(source, target);

        // parent
        if (source.getProduct() != null) {
            target.setProductId(source.getProduct().getId());
        }

        // Status
        if (source.getStatus() != null) {
            target.setStatusId(source.getStatus().getId());
        }

        // table name
        if (source.getTable() != null) {
            target.setSheetName(source.getTable().getLabel());
        }

        // Column names
        if (source.getTimeColumn() != null) {
            target.setTimeColumnName(source.getTimeColumn().getColumnName());
        }
        if (source.getSpaceColumn() != null) {
            target.setSpaceColumnName(source.getSpaceColumn().getColumnName());
        }
        if (source.getTechColumn() != null) {
            target.setTechColumnName(source.getTechColumn().getColumnName());
        }
        if (source.getAggColumn() != null) {
            target.setAggColumnName(source.getAggColumn().getColumnName());
        }

        return target;
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_LABEL, key = "#vo.label", condition = "#vo != null && #vo.id != null"),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS_BY_FILTER, allEntries = true),
        },
        put = {
            @CachePut(cacheNames= CacheNames.PRODUCT_BY_LABEL, key="#vo.label", condition = "#vo != null && #vo.label != null")
        }
    )
    public ExtractionProductVO save(ExtractionProductVO vo) {
        return super.save(vo);
    }

    @Override
    public void toEntity(ExtractionProductVO source, ExtractionProduct target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Make sure label is uppercase
        if (source.getLabel() != null) {
            String label = source.getLabel().toUpperCase();
            source.setLabel(label);
            target.setLabel(label);
        }

        // Parent
        if (copyIfNull || source.getParentId() != null) {
            if (source.getParentId() == null) {
                target.setParent(null);
            } else {
                target.setParent(load(ExtractionProduct.class, source.getParentId()));
            }
        }

        // Recorder person
        DataDaos.copyRecorderPerson(getEntityManager(), source, target, copyIfNull);

        // Recorder department
        DataDaos.copyRecorderDepartment(getEntityManager(), source, target, copyIfNull);
        if (target.getRecorderDepartment() == null && target.getRecorderPerson() != null) {
            // If nul, use recorder person department
            target.setRecorderDepartment(target.getRecorderPerson().getDepartment());
        }
    }

    @Override
    protected void onAfterSaveEntity(ExtractionProductVO vo, ExtractionProduct savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        // Save tables
        saveProductTables(vo, savedEntity);

        getEntityManager().flush();
        getEntityManager().clear();

        // Save stratum
        saveProductStratum(vo, savedEntity);

        getEntityManager().flush();
        getEntityManager().clear();

        // Final merge
        getEntityManager().merge(savedEntity);

        getEntityManager().flush();
        getEntityManager().clear();

    }

    private void saveProductTables(ExtractionProductVO vo, ExtractionProduct entity) {
        List<ExtractionProductTableVO> sources = vo.getTables();
        Date updateDate = entity.getUpdateDate();

        final EntityManager em = getEntityManager();
        if (CollectionUtils.isEmpty(sources)) {
            if (entity.getTables() != null) {
                List<ExtractionProductTable> toRemove = ImmutableList.copyOf(entity.getTables());
                entity.getTables().clear();
                toRemove.forEach(em::remove);
            }
        } else {
            Map<String, ExtractionProductTable> existingItems = Beans.splitByProperty(
                Beans.getList(entity.getTables()),
                ExtractionProductTable.Fields.LABEL);
            final Status enableStatus = load(Status.class, StatusEnum.ENABLE.getId());
            if (entity.getTables() == null) {
                entity.setTables(Lists.newArrayList());
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
                    target.setProduct(entity);
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

                    if (isNew) entity.getTables().add(target);
                });

            em.flush();

            // Save each columns
            // Important: Skip if not columns, because UI editor not sent columns at all.
            boolean hasColumns = sources.stream().anyMatch(source -> source != null && source.getColumns() != null);
            if (hasColumns) {
                sources.stream()
                    .filter(Objects::nonNull)
                    .forEach(source -> saveProductTableColumns(source.getColumns(), source.getId(), updateDate));
                em.flush();
            }

            // Remove old tables
            if (MapUtils.isNotEmpty(existingItems)) {
                entity.getTables().removeAll(existingItems.values());
                existingItems.values().forEach(em::remove);
            }

        }
    }

    private void saveProductTableColumns(List<ExtractionProductColumnVO> sources, int tableId, Date updateDate) {
        final EntityManager em = getEntityManager();

        // Load parent
        ExtractionProductTable parent = find(ExtractionProductTable.class, tableId);

        if (CollectionUtils.isEmpty(sources)) {
            if (parent.getColumns() != null) {
                List<ExtractionProductColumn> toRemove = ImmutableList.copyOf(parent.getColumns());
                parent.getColumns().clear();
                toRemove.forEach(em::remove);
            }
        } else {
            Map<String, ExtractionProductColumn> existingItems = Beans.splitByProperty(
                Beans.getList(parent.getColumns()),
                ExtractionProductColumn.Fields.COLUMN_NAME);
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

                    if (isNew) parent.getColumns().add(target);
                });

            em.flush();

            // Save column values
            sources.stream()
                .filter(Objects::nonNull)
                .forEach(source -> saveProductTableValues(source.getValues(), source.getId()));

            em.flush();

            // Remove old tables
            if (MapUtils.isNotEmpty(existingItems)) {
                parent.getColumns().removeAll(existingItems.values());
                existingItems.values().forEach(em::remove);
            }

            em.flush();
        }
    }

    private void saveProductTableValues(List<String> sources, int columnId) {
        ExtractionProductColumn parent = find(ExtractionProductColumn.class, columnId);

        final EntityManager em = getEntityManager();
        if (CollectionUtils.isEmpty(sources)) {
            if (parent.getValues() != null) {
                List<ExtractionProductValue> toRemove = ImmutableList.copyOf(parent.getValues());
                parent.getValues().clear();
                toRemove.forEach(em::remove);
            }
        } else {
            Map<String, ExtractionProductValue> existingItems = Beans.splitByProperty(
                Beans.getList(parent.getValues()),
                ExtractionProductValue.Fields.LABEL);
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

                    if (isNew) parent.getValues().add(target);
                });

            em.flush();

            // Remove old values
            if (MapUtils.isNotEmpty(existingItems)) {
                parent.getValues().removeAll(existingItems.values());
                existingItems.values().forEach(em::remove);
            }

        }
    }

    private void saveProductStratum(ExtractionProductVO vo, ExtractionProduct entity) {
        List<ExtractionProductStrataVO> sources = vo.getStratum();
        Date updateDate = entity.getUpdateDate();

        final EntityManager em = getEntityManager();
        if (CollectionUtils.isEmpty(sources)) {
            if (entity.getStratum() != null) {
                List<ExtractionProductStrata> toRemove = ImmutableList.copyOf(entity.getStratum());
                entity.getStratum().clear();
                toRemove.forEach(em::remove);
            }
        } else {
            Map<String, ExtractionProductStrata> existingItems = Beans.splitByProperty(entity.getStratum(), ExtractionProductStrata.Fields.LABEL);
            Map<String, ExtractionProductTable> existingTables = Beans.splitByProperty(entity.getTables(), ExtractionProductTable.Fields.LABEL);
            final Status enableStatus = load(Status.class, StatusEnum.ENABLE.getId());
            if (entity.getStratum() == null) {
                entity.setStratum(Lists.newArrayList());
            }

            // Save each table
            sources.stream()
                .filter(Objects::nonNull)
                .forEach(source -> {
                    ExtractionProductStrata target = existingItems.remove(source.getLabel());
                    boolean isNew = (target == null);
                    if (isNew) {
                        target = new ExtractionProductStrata();
                        Beans.copyProperties(source, target);
                    }
                    else {
                        target.setName(source.getName());
                        target.setIsDefault(source.getIsDefault());
                    }
                    target.setProduct(entity);
                    target.setStatus(source.getStatusId() != null ? load(Status.class, source.getStatusId()) : enableStatus);
                    target.setUpdateDate(updateDate);

                    // Link to table (find by sheet anem, or find as singleton)
                    ExtractionProductTable table = StringUtils.isNotBlank(source.getSheetName())
                        ? existingTables.get(source.getSheetName())
                        : (existingTables.size() == 1 ? existingTables.values().iterator().next() : null);
                    if (table != null) {
                        target.setTable(table);
                        target.setTimeColumn(findColumnByName(table, source.getTimeColumnName()));
                        target.setSpaceColumn(findColumnByName(table, source.getSpaceColumnName()));
                        target.setAggColumn(findColumnByName(table, source.getAggColumnName()));
                        target.setTechColumn(findColumnByName(table, source.getTechColumnName()));
                    }
                    else {
                        target.setTable(null);
                        target.setTimeColumn(null);
                        target.setSpaceColumn(null);
                        target.setAggColumn(null);
                        target.setTechColumn(null);
                    }

                    if (isNew) {
                        target.setCreationDate(updateDate);
                        em.persist(target);
                        source.setId(target.getId());
                    } else {
                        em.merge(target);
                    }

                    source.setUpdateDate(updateDate);
                });

            em.flush();

            // Remove old tables
            if (MapUtils.isNotEmpty(existingItems)) {
                entity.getStratum().removeAll(existingItems.values());
                existingItems.values().forEach(em::remove);
            }

        }
    }

    protected ExtractionProductColumn findColumnByName(ExtractionProductTable table, String columnName) {
        if (StringUtils.isBlank(columnName)) return null;
        return table.getColumns().stream()
            .filter(c -> columnName.equalsIgnoreCase(c.getColumnName()))
            .findFirst().orElse(null);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_LABEL, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PRODUCTS_BY_FILTER, allEntries = true)
    })
    public void deleteById(Integer id) {
        super.deleteById(id);
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
                cb.equal(root.get(ExtractionProductColumn.Fields.TABLE)
                    .get(ExtractionProductTable.Fields.PRODUCT)
                    .get(ExtractionProduct.Fields.ID), productIdParam),
                cb.equal(root.get(ExtractionProductColumn.Fields.TABLE)
                    .get(ExtractionProductTable.Fields.LABEL), tableLabelParam)
            )
        );

        // Sort by rank order
        query.orderBy(cb.asc(root.get(ExtractionProductColumn.Fields.RANK_ORDER)));

        return getEntityManager().createQuery(query)
            .setParameter(productIdParam, id)
            .setParameter(tableLabelParam, tableLabel)
            .getResultStream()
            .map(c -> toColumnVO(c, null /*with values*/))
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

    protected List<String> toColumnValues(ExtractionProductColumn source) {
        if (source == null || CollectionUtils.isEmpty(source.getValues())) return null;
        return source.getValues().stream()
            .map(ExtractionProductValue::getLabel)
            .collect(Collectors.toList());
    }

}
