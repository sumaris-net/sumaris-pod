package net.sumaris.core.model.data;

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

import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.SaleType;
import net.sumaris.core.model.referential.location.Location;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@FieldNameConstants
@Entity
@Table(name = "expected_sale")
public class ExpectedSale implements IEntity<Integer>,
    IWithProductsEntity<Integer, Product>,
    IWithRecorderDepartmentEntity<Integer, Department> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EXPECTED_SALE_SEQ")
    @SequenceGenerator(name = "EXPECTED_SALE_SEQ", sequenceName = "EXPECTED_SALE_SEQ", allocationSize = IDataEntity.SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "sale_date")
    private Date saleDate;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = Location.class)
    @JoinColumn(name = "sale_location_fk", nullable = false)
    private Location saleLocation;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = SaleType.class)
    @JoinColumn(name = "sale_type_fk", nullable = false)
    private SaleType saleType;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Product.class, mappedBy = Product.Fields.EXPECTED_SALE)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Product> products = new ArrayList<>();

    /* -- measurements -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = SaleMeasurement.class, mappedBy = SaleMeasurement.Fields.EXPECTED_SALE)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<SaleMeasurement> measurements = new ArrayList<>();

    /* -- parent -- */

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Trip.class)
    @JoinColumn(name = "trip_fk")
    @ToString.Exclude
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Landing.class)
    @JoinColumn(name = "landing_fk")
    @ToString.Exclude
    private Landing landing;

    @Override
    public Department getRecorderDepartment() {
        if (landing != null) return landing.getRecorderDepartment();
        if (trip != null) return trip.getRecorderDepartment();
        return null;
    }

    @Override
    public void setRecorderDepartment(Department recorderDepartment) {
        throw new IllegalArgumentException("Cannot set recorder department on ExpectedSale");
    }
}
