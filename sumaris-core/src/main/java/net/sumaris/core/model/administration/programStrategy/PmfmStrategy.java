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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.pmfm.*;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;

import javax.persistence.*;
import java.util.Set;

@Data
@FieldNameConstants
@Entity
@Table(name = "pmfm_strategy")
public class PmfmStrategy implements IEntity<Integer> {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "PMFM_STRATEGY_SEQ")
    @SequenceGenerator(name = "PMFM_STRATEGY_SEQ", sequenceName="PMFM_STRATEGY_SEQ", allocationSize = IReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pmfm_fk")
    private Pmfm pmfm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parameter_fk")
    private Parameter parameter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matrix_fk")
    private Matrix matrix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fraction_fk")
    private Fraction fraction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "method_fk")
    private Method method;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_fk", nullable = false)
    private Strategy strategy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acquisition_level_fk", nullable = false)
    private AcquisitionLevel acquisitionLevel;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinTable(name = "pmfm_strategy2gear", joinColumns = {
            @JoinColumn(name = "pmfm_strategy_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "gear_fk", nullable = false, updatable = false) })
    private Set<Gear> gears = Sets.newHashSet();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinTable(name = "pmfm_strategy2taxon_group", joinColumns = {
            @JoinColumn(name = "pmfm_strategy_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "taxon_group_fk", nullable = false, updatable = false) })
    private Set<TaxonGroup> taxonGroups = Sets.newHashSet();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinTable(name = "pmfm_strategy2reference_taxon", joinColumns = {
            @JoinColumn(name = "pmfm_strategy_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "reference_taxon_fk", nullable = false, updatable = false) })
    private Set<ReferenceTaxon> referenceTaxons = Sets.newHashSet();

    public String toString() {
        return String.format("PmfmStrategy{id=%s, strategy=%s}",
                id,
                strategy);
    }
}
