package net.sumaris.core.dao.data.sale;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2021 SUMARiS Consortium
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

import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.data.ExpectedSale;
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.SaleType;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.ExpectedSaleVO;
import org.springframework.context.ApplicationContext;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ExpectedSaleRepositoryImpl
    extends SumarisJpaRepositoryImpl<ExpectedSale, Integer, ExpectedSaleVO>
    implements ExpectedSaleSpecifications {

    private final LocationRepository locationRepository;
    private final ReferentialDao referentialDao;

    private final ApplicationContext applicationContext;

    protected ExpectedSaleRepositoryImpl(EntityManager entityManager,
                                         LocationRepository locationRepository,
                                         ReferentialDao referentialDao,
                                         ApplicationContext applicationContext) {
        super(ExpectedSale.class, ExpectedSaleVO.class, entityManager);
        this.locationRepository = locationRepository;
        this.referentialDao = referentialDao;
        this.applicationContext = applicationContext;
    }

    @Override
    public void toVO(ExpectedSale source, ExpectedSaleVO target, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        // Sale location
        target.setSaleLocation(locationRepository.toVO(source.getSaleLocation()));

        // Sale type
        target.setSaleType(referentialDao.toVO(source.getSaleType()));

    }

    @Override
    public void toEntity(ExpectedSaleVO source, ExpectedSale target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // TODO : entity should be owned by a trip or a landing, not both -> validation to do in service

        // Trip
        Integer tripId = source.getTripId() != null ? source.getTripId() : (source.getTrip() != null ? source.getTrip().getId() : null);
        if (copyIfNull || (tripId != null)) {
            if (tripId == null) {
                target.setTrip(null);
            }
            else {
                target.setTrip(getReference(Trip.class, tripId));
            }
        }

        // Landing
        Integer landingId = source.getLandingId() != null ? source.getLandingId() : (source.getLanding() != null ? source.getLanding().getId() : null);
        if (copyIfNull || (landingId != null)) {
            if (landingId == null) {
                target.setLanding(null);
            }
            else {
                target.setLanding(getReference(Landing.class, landingId));
            }
        }

        // Sale location
        if (copyIfNull || source.getSaleLocation() != null) {
            if (source.getSaleLocation() == null || source.getSaleLocation().getId() == null) {
                target.setSaleLocation(null);
            }
            else {
                target.setSaleLocation(getReference(Location.class, source.getSaleLocation().getId()));
            }
        }

        // Sale type
        if (copyIfNull || source.getSaleType() != null) {
            if (source.getSaleType() == null || source.getSaleType().getId() == null) {
                target.setSaleType(null);
            }
            else {
                target.setSaleType(getReference(SaleType.class, source.getSaleType().getId()));
            }
        }

    }

    @Override
    public List<ExpectedSaleVO> getAllByTripId(int tripId) {
        return getRepository().getExpectedSaleByTripId(tripId).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ExpectedSaleVO> saveAllByTripId(int tripId, List<ExpectedSaleVO> sales) {

        // Filter on non null objects
        sales = sales.stream().filter(Objects::nonNull).collect(Collectors.toList());

        // Get existing fishing areas
        Trip parent = getById(Trip.class, tripId);
        final Set<Integer> existingIds = Beans.collectIdsAsSet(parent.getExpectedSales());

        // Save
        sales.forEach(sale -> {
            // Set parent link
            sale.setTripId(tripId);
            save(sale);
            existingIds.remove(sale.getId());
        });

        // Delete remaining objects
        existingIds.forEach(this::deleteById);

        return sales;

    }

    protected ExpectedSaleRepository repository;
    protected ExpectedSaleRepository getRepository() {
        if (repository == null) {
            repository = applicationContext.getBean(ExpectedSaleRepository.class);
        }
        return repository;
    }
}
