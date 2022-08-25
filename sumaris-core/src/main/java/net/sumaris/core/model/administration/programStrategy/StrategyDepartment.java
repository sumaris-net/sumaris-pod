package net.sumaris.core.model.administration.programStrategy;

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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.location.Location;

import javax.persistence.*;
import java.util.Date;

@Data
@FieldNameConstants
@Entity
@Table(name = "strategy2department")
@NamedQueries({
    @NamedQuery(name = "StrategyDepartment.count", query = "SELECT\n" +
            "   count(distinct t.id)\n" +
            "      FROM\n" +
            "        StrategyDepartment t\n" +
            "      WHERE\n" +
            "        (:strategyId is null OR t.strategy.id = :strategyId)\n" +
            "        AND (:departmentId is null OR t.department.id = :departmentId)\n" +
            "        AND (:privilegeId is null OR  t.privilege.id = :privilegeId)"
    )
})
public class StrategyDepartment implements IUpdateDateEntity<Integer, Date> {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "STRATEGY2DEPARTMENT_SEQ")
    @SequenceGenerator(name = "STRATEGY2DEPARTMENT_SEQ", sequenceName="STRATEGY2DEPARTMENT_SEQ", allocationSize = IReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_fk", nullable = false)
    private Strategy strategy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_fk")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_privilege_fk", nullable = false)
    private ProgramPrivilege privilege;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_fk", nullable = false)
    private Department department;

}
