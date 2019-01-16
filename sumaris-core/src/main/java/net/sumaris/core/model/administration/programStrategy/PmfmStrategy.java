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

import com.google.common.collect.Sets;
import lombok.Data;
import net.sumaris.core.dao.technical.model.IEntityBean;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.pmfm.Pmfm;

import javax.persistence.*;
import java.util.Set;

@Data
@Entity
@Table(name = "pmfm_strategy")
public class PmfmStrategy implements IEntityBean<Integer> {

    public static final String PROPERTY_STRATEGY = "strategy";
    public static final String PROPERTY_ACQUISITION_LEVEL = "acquisitionLevel";
    public static final String PROPERTY_RANK_ORDER = "rankOrder";

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer id;

    @Column(name = "acquisition_number", nullable = false)
    private Integer acquisitionNumber;

    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @Column(name = "default_value")
    private Double defaultValue;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pmfm_fk", nullable = false)
    private Pmfm pmfm;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_fk", nullable = false)
    private Strategy strategy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acquisition_level_fk", nullable = false)
    private AcquisitionLevel acquisitionLevel;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "pmfm_strategy2gear", joinColumns = {
            @JoinColumn(name = "pmfm_strategy_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "gear_fk", nullable = false, updatable = false) })
    private Set<Gear> gears = Sets.newHashSet();

}
