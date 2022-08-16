package net.sumaris.core.dao.technical.extraction;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.administration.user.DepartmentRepository;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.data.DataDaos;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.*;
import net.sumaris.core.model.technical.history.ProcessingFrequency;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 21/08/2020.
 */
@Slf4j
public class ExtractionProductRepositoryImpl
    extends ReferentialRepositoryImpl<Integer, ExtractionProduct, ExtractionProductVO, ExtractionTypeFilterVO, ExtractionProductFetchOptions>
    implements ExtractionProductSpecifications {

    private final DepartmentRepository departmentRepository;
    private final PersonRepository personRepository;

    private final String dropTableQuery;

    protected ExtractionProductRepositoryImpl(EntityManager entityManager, DepartmentRepository departmentRepository, PersonRepository personRepository, SumarisDatabaseMetadata databaseMetadata) {
        super(ExtractionProduct.class, ExtractionProductVO.class, entityManager);
        this.departmentRepository = departmentRepository;
        this.personRepository = personRepository;
        this.dropTableQuery = databaseMetadata.getDialect().getDropTableString("%s");
        setLockForUpdate(true);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PRODUCTS_BY_FILTER)
    public List<ExtractionProductVO> findAll(ExtractionTypeFilterVO filter, ExtractionProductFetchOptions fetchOptions) {
        return super.findAll(filter, fetchOptions);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PRODUCT_BY_LABEL_AND_OPTIONS)
    public ExtractionProductVO getByLabel(String label, ExtractionProductFetchOptions fetchOption) {
        return super.getByLabel(label, fetchOption);
    }

    @Override
    protected Specification<ExtractionProduct> toSpecification(ExtractionTypeFilterVO filter, ExtractionProductFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(withPropertyValue(ExtractionProduct.Fields.FORMAT, String.class, filter.getFormat()))
            .and(withPropertyValue(ExtractionProduct.Fields.VERSION, String.class, filter.getVersion()))
            .and(isSpatial(filter.getIsSpatial()))
            .and(withParentId(filter.getParentId()))
            .and(withRecorderPersonIdOrPublic(filter.getRecorderPersonId()))
            .and(withRecorderDepartmentId(filter.getRecorderDepartmentId()));
    }

    @Override
    public boolean shouldQueryDistinct(String joinProperty) {
        // When searching on 'processingFrequency.label' do NOT apply distinct
        if (ExtractionProduct.Fields.PROCESSING_FREQUENCY.equalsIgnoreCase(joinProperty)) {
            return false;
        }
        return super.shouldQueryDistinct(joinProperty);
    }

    @Override
    public void dropTable(String tableName) {
        Preconditions.checkNotNull(tableName);

        log.debug(String.format("Dropping extraction table {%s}...", tableName));
        try {
            String sql = String.format(dropTableQuery, tableName);
            getSession().createSQLQuery(sql).executeUpdate();

        } catch (Exception e) {
            throw new SumarisTechnicalException(String.format("Cannot drop extraction table {%s}...", tableName), e);
        }
    }

    @Override
    protected void toVO(ExtractionProduct source, ExtractionProductVO target, ExtractionProductFetchOptions fetchOptions, boolean copyIfNull) {
        // Status
        target.setStatusId(source.getStatus().getId());


        // Copy without/with documentation (can be very long)
        List<String> excludedProperties = Lists.newArrayList();
        if (fetchOptions == null || !fetchOptions.isWithDocumentation()) {
            excludedProperties.add(ExtractionProduct.Fields.DOCUMENTATION);
        }
        if (fetchOptions == null || !fetchOptions.isWithFilter()) {
            excludedProperties.add(ExtractionProduct.Fields.FILTER_CONTENT);
        }
        if (excludedProperties.size() > 0) {
            Beans.copyProperties(source, target, excludedProperties.toArray(new String[0]));
        }
        else {
            Beans.copyProperties(source, target);
        }

        // Parent
        target.setParentId(source.getParent() != null ? source.getParent().getId() : null);

        // Processing frequency
        if (copyIfNull || source.getProcessingFrequency() != null) {
            if (source.getProcessingFrequency() == null) {
                target.setProcessingFrequencyId(null);
            }
            else {
                target.setProcessingFrequencyId(source.getProcessingFrequency().getId());
            }
        }

        // Tables
        if (fetchOptions == null || fetchOptions.isWithTables()) {
            if (CollectionUtils.isNotEmpty(source.getTables())) {
                List<ExtractionTableVO> tables = source.getTables().stream()
                    .map(t -> toProductTableVO(t, fetchOptions))
                    .collect(Collectors.toList());
                target.setTables(tables);
            }
        }

        // Stratum
        if (fetchOptions == null || fetchOptions.isWithStratum()) {
            if (CollectionUtils.isNotEmpty(source.getStratum())) {
                List<AggregationStrataVO> stratum = source.getStratum().stream()
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

    protected ExtractionTableVO toProductTableVO(ExtractionProductTable source, ExtractionProductFetchOptions fetchOptions) {
        ExtractionTableVO target = new ExtractionTableVO();
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

    protected AggregationStrataVO toProductStrataVO(ExtractionProductStrata source) {
        AggregationStrataVO target = new AggregationStrataVO();
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
            target.setSpatialColumnName(source.getSpaceColumn().getColumnName());
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
            @CacheEvict(cacheNames = CacheConfiguration.Names.PRODUCT_BY_LABEL_AND_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PRODUCTS_BY_FILTER, allEntries = true),
        }
    )
    public ExtractionProductVO save(@NonNull ExtractionProductVO source, @NonNull ExtractionProductSaveOptions saveOptions) {


        // Workaround to force tables to be saved (or not)
        // See onAfterSaveEntity() for details
        List<ExtractionTableVO> tables = source.getTables();
        if (!saveOptions.isWithTables()) source.setTables(null);
        else source.setTables(Beans.getList(tables));

        // Workaround to force stratum to be saved (or not)
        // See onAfterSaveEntity() for details
        List<AggregationStrataVO> stratum = source.getStratum();
        if (!saveOptions.isWithStratum()) source.setStratum(null);
        else source.setStratum(Beans.getList(stratum));

        // Call the inherited method
        ExtractionProductVO result = super.save(source);

        // Restore original tables or stratum, if not saved
        if (!saveOptions.isWithTables()) result.setTables(tables);
        if (!saveOptions.isWithStratum()) result.setStratum(stratum);

        return result;
    }

    @Override
    public void toEntity(ExtractionProductVO source, ExtractionProduct target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        EntityManager em = getEntityManager();

        // Make sure label is uppercase
        if (source.getLabel() != null) {
            String label = source.getLabel().toUpperCase();
            source.setLabel(label);
            target.setLabel(label);
        }

        // Parent
        Integer parentId = source.getParent() != null ? source.getParent().getId() : source.getParentId();
        parentId = parentId != null && parentId >= 0 ? parentId : null; // Avoid to ave negative ID (= from ExtractionType enum)
        if (copyIfNull || parentId != null) {
            if (parentId == null) {
                target.setParent(null);
            } else {
                target.setParent(getReference(ExtractionProduct.class, parentId));
            }
        }
        source.setParentId(parentId);

        // Processing frequency
        if (copyIfNull || source.getProcessingFrequencyId() != null) {
            if (source.getProcessingFrequencyId() == null) {
                target.setProcessingFrequency(null);
            }
            else {
                target.setProcessingFrequency(getReference(ProcessingFrequency.class, source.getProcessingFrequencyId()));
            }
        }

        // Recorder person
        DataDaos.copyRecorderPerson(em, source, target, copyIfNull);

        // Recorder department
        DataDaos.copyRecorderDepartment(em, source, target, copyIfNull);
        if (target.getRecorderDepartment() == null && target.getRecorderPerson() != null) {
            // If nul, use recorder person department
            target.setRecorderDepartment(target.getRecorderPerson().getDepartment());
        }
    }

    @Override
    protected void onAfterSaveEntity(ExtractionProductVO vo, ExtractionProduct savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);
        EntityManager em = getEntityManager();

        // Save tables
        if (vo.getTables() != null) {
            saveProductTables(vo, savedEntity);
            em.flush();
        }

        // Save stratum
        if (vo.getStratum() != null) {
            saveProductStratum(vo, savedEntity);
            em.flush();
        }

        // Final merge
        em.merge(savedEntity);

        em.flush();
        em.clear();
    }

    private void saveProductTables(ExtractionProductVO vo, ExtractionProduct entity) {
        List<ExtractionTableVO> sources = vo.getTables();
        Date updateDate = entity.getUpdateDate();

        final EntityManager em = getEntityManager();
        if (CollectionUtils.isEmpty(sources)) {
            if (entity.getTables() != null) {
                List<ExtractionProductTable> tableEntitiesToRemove = ImmutableList.copyOf(entity.getTables());
                entity.getTables().clear();
                tableEntitiesToRemove.forEach(em::remove);
            }
        } else {
            Map<String, ExtractionProductTable> tableEntitiesToRemove = Beans.splitByProperty(
                Beans.getList(entity.getTables()),
                ExtractionProductTable.Fields.LABEL);
            final Status enableStatus = getReference(Status.class, StatusEnum.ENABLE.getId());
            if (entity.getTables() == null) {
                entity.setTables(Lists.newArrayList());
            }

            // Save each table
            sources.stream()
                .filter(Objects::nonNull)
                .forEach(source -> {
                    ExtractionProductTable target = tableEntitiesToRemove.remove(source.getLabel());
                    boolean isNew = (target == null);
                    if (isNew) {
                        target = new ExtractionProductTable();
                    }
                    Beans.copyProperties(source, target,
                        // Keep immutable properties, from the existing entity
                        ExtractionProductTable.Fields.ID,
                        ExtractionProductTable.Fields.CREATION_DATE);
                    target.setProduct(entity);
                    target.setStatus(enableStatus);
                    target.setUpdateDate(updateDate);
                    if (isNew) {
                        target.setCreationDate(updateDate);
                        em.persist(target);
                    } else {
                        em.merge(target);
                    }

                    source.setId(target.getId());
                    source.setUpdateDate(target.getUpdateDate());
                    source.setCreationDate(target.getCreationDate());
                    source.setProductId(entity.getId());

                    if (isNew) entity.getTables().add(target);
                });

            em.flush();

            // Save each column
            // Important: Skip if not columns, because UI editor not sent columns at all.
            boolean hasColumns = sources.stream().anyMatch(source -> source != null && source.getColumns() != null);
            if (hasColumns) {
                sources.stream()
                    .filter(Objects::nonNull)
                    .forEach(source -> saveProductTableColumns(source.getColumns(), source.getId()));
                em.flush();
            }

            // Remove old tables
            if (MapUtils.isNotEmpty(tableEntitiesToRemove)) {
                entity.getTables().removeAll(tableEntitiesToRemove.values());
                tableEntitiesToRemove.values().forEach(em::remove);
            }

        }
    }

    private void saveProductTableColumns(List<ExtractionTableColumnVO> sources, int tableId) {
        final EntityManager em = getEntityManager();

        // Load parent
        ExtractionProductTable parent = getById(ExtractionProductTable.class, tableId);

        if (CollectionUtils.isEmpty(sources)) {
            if (parent.getColumns() != null) {
                List<ExtractionProductColumn> toRemove = ImmutableList.copyOf(parent.getColumns());
                parent.getColumns().clear();
                toRemove.forEach(em::remove);
            }
        } else {
            Map<String, ExtractionProductColumn> columnEntitiesToRemove = Beans.splitByProperty(
                Beans.getList(parent.getColumns()),
                ExtractionProductColumn.Fields.COLUMN_NAME);
            if (parent.getColumns() == null) {
                parent.setColumns(Lists.newArrayList());
            }

            // Save each column
            sources.stream()
                .filter(Objects::nonNull)
                .forEach(source -> {
                    ExtractionProductColumn target = columnEntitiesToRemove.remove(source.getColumnName());
                    boolean isNew = (target == null);
                    if (isNew) {
                        target = new ExtractionProductColumn();
                    }
                    Beans.copyProperties(source, target,
                        // Keep immutable properties, from the existing entity
                        ExtractionProductColumn.Fields.ID);

                    target.setTable(parent);
                    target.setLabel(StringUtils.underscoreToChangeCase(source.getColumnName()));
                    source.setLabel(target.getLabel());

                    if (isNew) {
                        em.persist(target);
                    } else {
                        em.merge(target);
                    }

                    source.setId(target.getId());

                    if (isNew) parent.getColumns().add(target);
                });

            em.flush();

            // Save column values
            sources.stream()
                .filter(Objects::nonNull)
                .forEach(source -> saveProductTableValues(source.getValues(), source.getId()));

            em.flush();

            // Remove old tables
            if (MapUtils.isNotEmpty(columnEntitiesToRemove)) {
                parent.getColumns().removeAll(columnEntitiesToRemove.values());
                columnEntitiesToRemove.values().forEach(em::remove);
            }

            em.flush();
        }
    }

    private void saveProductTableValues(List<String> sources, int columnId) {
        ExtractionProductColumn parent = getById(ExtractionProductColumn.class, columnId);

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
        List<AggregationStrataVO> sources = vo.getStratum();
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
            final Status enableStatus = getReference(Status.class, StatusEnum.ENABLE.getId());
            if (entity.getStratum() == null) {
                entity.setStratum(Lists.newArrayList());
            }

            // Save each table
            sources.stream().filter(Objects::nonNull)
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
                    target.setStatus(source.getStatusId() != null ? getReference(Status.class, source.getStatusId()) : enableStatus);
                    target.setUpdateDate(updateDate);

                    // Link to table (find by sheet anem, or find as singleton)
                    String sheetName = source.getSheetName();
                    ExtractionProductTable table = StringUtils.isNotBlank(sheetName)
                        ? existingTables.get(sheetName)
                        : (existingTables.size() == 1 ? existingTables.values().iterator().next() : null);
                    if (table != null) {
                        target.setTable(table);
                        target.setTimeColumn(findColumnByName(table, source.getTimeColumnName()));
                        target.setSpaceColumn(findColumnByName(table, source.getSpatialColumnName()));
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
                        entity.getStratum().add(target);
                    } else {
                        em.merge(target);
                    }

                    source.setUpdateDate(updateDate);
                });

            // Remove old tables
            List<Integer> strataIdsToRemove = existingItems.values().stream()
                    .map(ExtractionProductStrata::getId)
                    .filter(Objects::nonNull).collect(Collectors.toList());
            entity.getStratum().removeAll(existingItems.values());


            em.merge(entity);

            em.flush();
            em.clear();

            // Remove old stratum
            if (CollectionUtils.isNotEmpty(strataIdsToRemove)) {
                strataIdsToRemove.forEach(id -> this.deleteById(id, ExtractionProductStrata.class));
            }
        }
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfiguration.Names.PRODUCT_BY_LABEL_AND_OPTIONS, allEntries = true),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PRODUCTS_BY_FILTER, allEntries = true)
    })
    public void deleteById(Integer id) {
        super.deleteById(id);
    }

    @Override
    public List<ExtractionTableColumnVO> getColumnsByIdAndTableLabel(Integer id, String tableLabel) {
        EntityManager em  = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
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

        return em.createQuery(query)
            .setParameter(productIdParam, id)
            .setParameter(tableLabelParam, tableLabel)
            .getResultStream()
            .map(c -> toColumnVO(c, null /*with values*/))
            .collect(Collectors.toList());
    }


    protected ExtractionProductColumn findColumnByName(ExtractionProductTable table, String columnName) {
        if (StringUtils.isBlank(columnName)) return null;
        final String columnNameLowerCase = columnName.toLowerCase();
        return table.getColumns().stream()
                .filter(c -> columnNameLowerCase.equalsIgnoreCase(c.getColumnName()))
                .findFirst().orElse(null);
    }

    protected int deleteById(int id, Class<?> entityClass) {
        return getEntityManager().createQuery(String.format("delete from %s where id=:id", entityClass.getSimpleName()))
                .setParameter("id", id)
                .executeUpdate();
    }

    protected ExtractionTableColumnVO toColumnVO(ExtractionProductColumn source, ExtractionProductFetchOptions fetchOptions) {
        ExtractionTableColumnVO target = new ExtractionTableColumnVO();
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
