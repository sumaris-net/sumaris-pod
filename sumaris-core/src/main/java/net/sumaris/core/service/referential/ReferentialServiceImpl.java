package net.sumaris.core.service.referential;

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
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ReferentialEntities;
import net.sumaris.core.dao.referential.ReferentialRepository;
import net.sumaris.core.dao.referential.gradient.DepthGradientRepository;
import net.sumaris.core.dao.referential.gradient.DistanceToCoastGradientRepository;
import net.sumaris.core.dao.referential.gradient.NearbySpecificAreaRepository;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.model.referential.gradient.DepthGradient;
import net.sumaris.core.model.referential.gradient.DistanceToCoastGradient;
import net.sumaris.core.model.referential.gradient.NearbySpecificArea;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.filter.LocationFilterVO;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("referentialService")
@Slf4j
public class ReferentialServiceImpl implements ReferentialService {

	protected final ReferentialDao referentialDao;

	private final MetierRepository metierRepository;
	private final LocationRepository locationRepository;
	private final DistanceToCoastGradientRepository distanceToCoastGradientRepository;
	private final DepthGradientRepository depthGradientRepository;
	private final NearbySpecificAreaRepository nearbySpecificAreaRepository;

	private final Map<String, ReferentialRepository<Integer,
		? extends IItemReferentialEntity<Integer>,
		? extends ReferentialVO, ? extends IReferentialFilter,
		ReferentialFetchOptions>> repositoriesMap = Maps.newHashMap();

	private final ApplicationEventPublisher publisher;

	private boolean enableTrash = false;


	@Autowired
	public ReferentialServiceImpl(ReferentialDao referentialDao,
                                  MetierRepository metierRepository,
								  LocationRepository locationRepository,
                                  DistanceToCoastGradientRepository distanceToCoastGradientRepository,
                                  DepthGradientRepository depthGradientRepository,
                                  NearbySpecificAreaRepository nearbySpecificAreaRepository,
                                  GenericConversionService conversionService,
                                  ApplicationEventPublisher publisher) {
		this.referentialDao = referentialDao;
		this.metierRepository = metierRepository;
        this.locationRepository = locationRepository;
        this.distanceToCoastGradientRepository = distanceToCoastGradientRepository;
        this.depthGradientRepository = depthGradientRepository;
        this.nearbySpecificAreaRepository = nearbySpecificAreaRepository;
        this.publisher = publisher;

		// Register repositories, by entity name
		repositoriesMap.put(Metier.ENTITY_NAME, metierRepository);
		repositoriesMap.put(DistanceToCoastGradient.ENTITY_NAME, distanceToCoastGradientRepository);
		repositoriesMap.put(DepthGradient.ENTITY_NAME, depthGradientRepository);
		repositoriesMap.put(NearbySpecificArea.ENTITY_NAME, nearbySpecificAreaRepository);

		// Entity->ReferentialVO converters
		ReferentialEntities.ROOT_CLASSES.forEach(entityClass -> {
			conversionService.addConverter(entityClass, ReferentialVO.class, this.referentialDao::toVO);
		});
	}

	@EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
	public void onConfigurationReady(ConfigurationEvent event) {
		this.enableTrash = event.getConfiguration().enableEntityTrash();
	}

	@Override
	public Date getLastUpdateDate() {
		return referentialDao.getLastUpdateDate();
	}

	@Override
	public List<ReferentialTypeVO> getAllTypes() {
		return referentialDao.getAllTypes();
	}

	@Override
	public ReferentialVO get(String entityName, int id) {
		return referentialDao.get(entityName, id);
	}

	@Override
	public ReferentialVO get(String entityName, int id, ReferentialFetchOptions fetchOptions) {
		return referentialDao.get(entityName, id, fetchOptions);
	}

	@Override
	public ReferentialVO get(Class<? extends IReferentialWithStatusEntity<Integer>> entityClass, int id) {
		return referentialDao.get(entityClass, id);
	}

	@Override
	public ReferentialVO get(Class<? extends IReferentialWithStatusEntity<Integer>> entityClass, int id, ReferentialFetchOptions fetchOptions) {
		return referentialDao.get(entityClass, id, fetchOptions);
	}

	@Override
	public List<ReferentialVO> getAllLevels(final String entityName) {
		return referentialDao.getAllLevels(entityName);
	}

	@Override
	public ReferentialVO getLevelById(String entityName, int levelId) {
		return referentialDao.getLevelById(entityName, levelId);
	}

	@Override
	@Cacheable(cacheNames = CacheConfiguration.Names.REFERENTIAL_ITEMS_BY_FILTER)
	public List<ReferentialVO> findByFilter(String entityName,
											IReferentialFilter filter, int offset, int size,
											String sortAttribute, SortDirection sortDirection,
											ReferentialFetchOptions fetchOptions) {

		// Location: use locationIds to find descendant locations
		if (entityName.equalsIgnoreCase(Location.ENTITY_NAME)) {
			return locationRepository.findAll(
					toLocationFilterVO(filter),
					Page.create(offset, size, sortAttribute, sortDirection),
					fetchOptions)
				.stream().map(item -> (ReferentialVO)item).toList();
		}

		// Métier: special case to be able to sort on join attribute (e.g. taxonGroup)
		if (entityName.equalsIgnoreCase(Metier.ENTITY_NAME)) {
			return metierRepository.findAll(
					IReferentialFilter.nullToEmpty(filter),
					Page.create(offset, size, sortAttribute, sortDirection), fetchOptions)
				.stream().map(item -> (ReferentialVO)item).toList();
		}

		// Distance To Coast Gradient
		if (entityName.equalsIgnoreCase(DistanceToCoastGradient.ENTITY_NAME)) {
			return distanceToCoastGradientRepository.findAll(
				(ReferentialFilterVO) IReferentialFilter.nullToEmpty(filter),
				Page.create(offset, size, sortAttribute, sortDirection), fetchOptions);
		}

		// Depth Gradient
		if (entityName.equalsIgnoreCase(DepthGradient.ENTITY_NAME)) {
			return depthGradientRepository.findAll(
				(ReferentialFilterVO) IReferentialFilter.nullToEmpty(filter),
				Page.create(offset, size, sortAttribute, sortDirection), fetchOptions);
		}

		// Nearby Specific Area
		if (entityName.equalsIgnoreCase(NearbySpecificArea.ENTITY_NAME)) {
			return nearbySpecificAreaRepository.findAll(
				(ReferentialFilterVO) IReferentialFilter.nullToEmpty(filter),
				Page.create(offset, size, sortAttribute, sortDirection), fetchOptions);
		}

		return referentialDao.findByFilter(entityName, IReferentialFilter.nullToEmpty(filter), offset, size, sortAttribute,
				sortDirection,
				fetchOptions);
	}

	@Override
	public List<ReferentialVO> findByFilter(String entityName, IReferentialFilter filter, int offset, int size) {
		return findByFilter(entityName, filter != null ? filter : new ReferentialFilterVO(), offset, size,
				IItemReferentialEntity.Fields.LABEL,
				SortDirection.ASC, null);
	}

	@Override
	@Cacheable(cacheNames = CacheConfiguration.Names.REFERENTIAL_COUNT_BY_FILTER)
	public Long countByFilter(String entityName, IReferentialFilter filter) {
		Preconditions.checkNotNull(entityName);
		if (filter == null) {
			return count(entityName);
		}

		// Location: use locationIds to find descendant locations
		if (entityName.equalsIgnoreCase(Location.ENTITY_NAME)) {
			return locationRepository.count(toLocationFilterVO(filter));
		}

		// Métier: special case to be able to sort on join attribute (e.g. taxonGroup)
		if (entityName.equalsIgnoreCase(Metier.ENTITY_NAME)) {
			return metierRepository.count(
					IReferentialFilter.nullToEmpty(filter));
		}

		// Distance To Coast Gradient
		if (entityName.equalsIgnoreCase(DistanceToCoastGradient.ENTITY_NAME)) {
			return distanceToCoastGradientRepository.count(
				(ReferentialFilterVO) IReferentialFilter.nullToEmpty(filter));
		}

		// Depth Gradient
		if (entityName.equalsIgnoreCase(DepthGradient.ENTITY_NAME)) {
			return depthGradientRepository.count(
				(ReferentialFilterVO) IReferentialFilter.nullToEmpty(filter));
		}

		// Nearby Specific Area
		if (entityName.equalsIgnoreCase(NearbySpecificArea.ENTITY_NAME)) {
			return nearbySpecificAreaRepository.count(
				(ReferentialFilterVO) IReferentialFilter.nullToEmpty(filter));
		}

		return referentialDao.countByFilter(entityName, filter);
	}

	@Override
	public ReferentialVO findByUniqueLabel(String entityName, String label) {
		Preconditions.checkNotNull(entityName);
		Preconditions.checkNotNull(label);
		return referentialDao.findByUniqueLabel(entityName, label)
			.orElseThrow(() -> new DataNotFoundException(I18n.t("sumaris.error.entity.notFoundByLabel", entityName, label)));
	}

	@Override
	public void delete(final String entityName, int id) {
		log.info("Delete {}}#{} {trash: {}}", entityName, id, enableTrash);

		// Create events (before deletion, to be able to join VO)
		ReferentialVO eventData = enableTrash ? get(entityName, id) : null;

		referentialDao.delete(entityName, id);

		// Emit event
		// TODO: move this into the HibernateDaoSupport ? See SumarisJpaRepositoryImpl
		publisher.publishEvent(new EntityDeleteEvent(id, entityName, eventData));
	}

	@Override
	public void delete(final String entityName, List<Integer> ids) {
		Preconditions.checkNotNull(entityName);
		Preconditions.checkNotNull(ids);

		ids.forEach(id -> delete(entityName, id));
	}

	@Override
	public Long count(String entityName) {
		Preconditions.checkNotNull(entityName);
		return referentialDao.count(entityName);
	}

	@Override
	public Long countByLevelId(String entityName, Integer... levelIds) {
		Preconditions.checkNotNull(entityName);
		return referentialDao.countByLevelId(entityName, levelIds);
	}

	@Override
	public ReferentialVO save(ReferentialVO source) {
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(source.getStatusId(), "Missing statusId");
		Preconditions.checkNotNull(source.getEntityName(), "Missing entityName");

		boolean isNew = source.getId() == null;

		ReferentialVO target = referentialDao.save(source);

		// Emit event
		// TODO: move this into the HibernateDaoSupport ? See SumarisJpaRepositoryImpl
		if (isNew) {
			publisher.publishEvent(new EntityInsertEvent(target.getId(), source.getEntityName(), target));
		} else {
			publisher.publishEvent(new EntityUpdateEvent(target.getId(), source.getEntityName(), target));
		}

		return target;
	}

	@Override
	public List<ReferentialVO> save(List<ReferentialVO> beans) {
		Preconditions.checkNotNull(beans);

		return beans.stream()
				.map(this::save)
				.collect(Collectors.toList());
	}

	private LocationFilterVO toLocationFilterVO(IReferentialFilter filter) {
		LocationFilterVO locationFilter = new LocationFilterVO();
		Beans.copyProperties(filter, locationFilter);
		locationFilter.setAncestorIds(((ReferentialFilterVO) filter).getLocationIds());
		return locationFilter;
	}
}
