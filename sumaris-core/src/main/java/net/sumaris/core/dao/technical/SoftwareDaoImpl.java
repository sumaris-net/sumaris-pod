package net.sumaris.core.dao.technical;

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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.configuration.Software;
import net.sumaris.core.model.technical.configuration.SoftwareProperty;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.technical.SoftwareVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository("softwareDao")
@Slf4j
public class SoftwareDaoImpl extends HibernateDaoSupport implements SoftwareDao {

    @Autowired
    private SoftwareRepository softwareRepository;

    public SoftwareVO get(int id) {
        return toVO(softwareRepository.getById(id));
    }

    public SoftwareVO getByLabel(String label) {
        Software source = softwareRepository.getByLabel(label);
        if (source == null) {
            throw new DataRetrievalFailureException(String.format("Software with label '%s' not found", label));
        }
        return toVO(source);
    }

    public Optional<SoftwareVO> findByLabel(String label) {
        return softwareRepository.findOneByLabel(label)
                .map(this::toVO);
    }

    public SoftwareVO save(SoftwareVO source)  {

        EntityManager entityManager = getEntityManager();
        Software entity = null;
        if (source.getId() != null) {
            entity = find(Software.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Software();
        }

        else {
            // Check update date
            Daos.checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            lockForUpdate(entity);

            // Check same label (cannot be changed, because used as a PK, in the Pod configuration)
            Preconditions.checkArgument(Objects.equals(entity.getLabel(), source.getLabel()),
                    "Cannot change label of a software entity");
        }

        // VO -> Entity
        toEntity(source, entity, false);

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

        // Save properties
        saveProperties(source.getProperties(), entity, newUpdateDate);

        // Final merge
        entityManager.merge(entity);

        return source;
    }

    /* -- protected methods -- */

    protected void toEntity(SoftwareVO source, Software target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // status
        if (copyIfNull || source.getStatusId() != null) {
            if (source.getStatusId() == null) {
                target.setStatus(null);
            }
            else {
                target.setStatus(getReference(Status.class, source.getStatusId()));
            }
        }
    }

    protected void saveProperties(Map<String, String> source, Software parent, Timestamp updateDate) {
        final EntityManager em = getEntityManager();
        if (MapUtils.isEmpty(source)) {
            if (parent.getProperties() != null) {
                List<SoftwareProperty> toRemove = ImmutableList.copyOf(parent.getProperties());
                parent.getProperties().clear();
                toRemove.forEach(em::remove);
            }
        }
        else {
            // WARN: database can stored many values for the same keys.
            // Only the first existing instance will be reused. Duplicate properties will be removed
            ListMultimap<String, SoftwareProperty> existingPropertiesMap = Beans.splitByNotUniqueProperty(
                    Beans.getList(parent.getProperties()),
                    SoftwareProperty.Fields.LABEL);
            List<SoftwareProperty> existingValues = Beans.getList(existingPropertiesMap.values());
            final Status enableStatus = em.getReference(Status.class, StatusEnum.ENABLE.getId());
            if (parent.getProperties() == null) {
                parent.setProperties(Lists.newArrayList());
            }
            final List<SoftwareProperty> targetProperties = parent.getProperties();

            // Transform each entry into SoftwareProperty
            source.keySet().stream()
                    .map(key -> {
                        SoftwareProperty prop = existingPropertiesMap.containsKey(key) ? existingPropertiesMap.get(key).get(0) : null;
                        boolean isNew = (prop == null);
                        if (isNew) {
                            prop = new SoftwareProperty();
                            prop.setLabel(key);
                            prop.setSoftware(parent);
                            prop.setCreationDate(updateDate);
                        }
                        else {
                            existingValues.remove(prop);
                        }
                        prop.setName(source.get(key));
                        prop.setStatus(enableStatus);
                        prop.setUpdateDate(updateDate);
                        if (isNew) {
                            em.persist(prop);
                        }
                        else {
                            em.merge(prop);
                        }
                        return prop;
                    })
                    .forEach(targetProperties::add);

            // Remove old properties
            if (CollectionUtils.isNotEmpty(existingValues)) {
                parent.getProperties().removeAll(existingValues);
                existingValues.forEach(em::remove);
            }
        }
    }

    protected SoftwareVO toVO(Software source) {
        if (source == null) return null;

        SoftwareVO target = new SoftwareVO();

        Beans.copyProperties(source, target);

        // Status
        target.setStatusId(source.getStatus().getId());

        // properties
        Map<String, String> properties = Maps.newHashMap();
        Beans.getStream(source.getProperties())
                .filter(prop -> Objects.nonNull(prop)
                        && Objects.nonNull(prop.getLabel())
                        && Objects.nonNull(prop.getName())
                )
                .forEach(prop -> {
                    if (properties.containsKey(prop.getLabel())) {
                        log.warn(String.format("Duplicate software property with label {%s}. Overriding existing value with {%s}", prop.getLabel(), prop.getName()));
                    }
                    properties.put(prop.getLabel(), prop.getName());
                });
        target.setProperties(properties);


        return target;
    }

    protected Software toEntity(SoftwareVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLabel());

        Software target;
        if (source.getId() != null) {
            target = softwareRepository.getByLabel(source.getLabel());
        }
        else {
            target = new Software();
        }

        toEntity(source, target, true);

        return target;
    }

}
