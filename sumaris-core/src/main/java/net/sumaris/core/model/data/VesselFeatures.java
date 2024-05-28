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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "vessel_features")
@NamedEntityGraph(
    name = VesselFeatures.GRAPH_SNAPSHOT,
    attributeNodes = {
        @NamedAttributeNode(VesselFeatures.Fields.VESSEL)
    }
)
public class VesselFeatures implements IDataEntity<Integer>,
        IWithRecorderPersonEntity<Integer, Person>,
        IWithRecorderDepartmentEntity<Integer, Department>,
        IWithVesselEntity<Integer, Vessel>{

    public static final String GRAPH_SNAPSHOT = "VesselFeatures.snapshot";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "VESSEL_FEATURES_SEQ")
    @SequenceGenerator(name = "VESSEL_FEATURES_SEQ", sequenceName="VESSEL_FEATURES_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_person_fk")
    private Person recorderPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_department_fk", nullable = false)
    private Department recorderDepartment;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    @Column(name="control_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date controlDate;

    @Column(name="validation_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date validationDate;

    @Column(name="qualification_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date qualificationDate;

    @Column(name="qualification_comments", length = LENGTH_COMMENTS)
    private String qualificationComments;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = QualityFlag.class)
    @JoinColumn(name = "quality_flag_fk", nullable = false)
    private QualityFlag qualityFlag;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Vessel.class)
    @JoinColumn(name = "vessel_fk", nullable = false)
    private Vessel vessel;

    @Column(name = "start_date", nullable = false)
    private Date startDate;

    @Column(name = "end_date")
    private Date endDate;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "exterior_marking", length = 100)
    private String exteriorMarking;

    @Column(name = "length_over_all")
    private Integer lengthOverAll;

    @Column(name = "administrative_power")
    private Integer administrativePower;

    @Column(name = "auxiliary_power")
    private Integer auxiliaryPower;

    /**
     * Tonnage du navire, ou jauge Brute, exprimé en TJB (Tonnage de Jauge Brute), et multiplié par 100.
     *
     * Tonnage (GRT) 'Gross Registered Tonnage' en accord avec la convention d'Oslo (1947) (en emploi en France jusqu'en 1995, toujours utilisé dans certains pays).
     *
     * (GRT represent the total cubic measured content of the permanently enclosed spaces of a vessel, with some allowances or deductions for exempt spaces such as living quarters [1 gross register ton = 100 cubic feet = 2.83 cubic metres]).
     */
    @Column(name = "gross_tonnage_grt")
    private Integer grossTonnageGrt;

    /**
     * Tonnage GT (Gross Tonnage), exprimé en UMS (Universal Measurement System), et multiplié par 100.
     * Il s'agit d'un tonnage reconnu au niveau international.
     * La France s'est engagé à fournir un tonnage GT pour tous les navires de plus de 24m, par application du Décret N°725 du 10 août 1982.
     *
     * Jauge GT ou Gross Tonnage en accord avec la Convention internationale de 1969 (règlement de Londres, International Convention on Tonnage Measurement of Ships, London, 1969 (in use since 1996) for vessels >= 15m"
     * Par décret n° 82-725 du 10 août 1982, publié au Journal officiel du 20 août 1982, la Convention internationale de 1969 sur le jaugeage des navires est entrée en vigueur en France le 18 juillet 1982. Le règlement de jaugeage annexé à cette convention conclue à Londres le 23 juin 1969 devient règlement international. La jauge, de Londres comprend la jauge brute et la jauge nette et doit être calculée selon les règles énoncées par la convention internationale de 1969 sur le jaugeage des navires. La jauge brute est obtenue à partir des mesures pratiquées sur tous les espaces fermés d'un navire, à l'exception des espaces exclus par les règles de la convention précitée. La jauge nette est déterminée par les dimensions des espaces réservés au fret et par le nombre des passagers.
     * La jauge de Londres est reprise sur le "certificat international de jaugeage des navires (1969)".
     */
    @Column(name = "gross_tonnage_gt")
    private Integer grossTonnageGt;

    /**
     * Année de construction du navire
     */
    @Column(name = "construction_year")
    private Integer constructionYear;

    /**
     * Indicatif radio international du navire (IRCS)
     */
    @Column(name = "ircs", length = 10)
    private String ircs;

    /**
     * Matériaux de la coque (ex : Bois, Métal, Plastique, etc). cf PMFM "HULL_MATERIAL".
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="hull_material_qv_fk")
    private QualitativeValue hullMaterial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="base_port_location_fk", nullable = false)
    private Location basePortLocation;

    /* -- measurements -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = VesselPhysicalMeasurement.class, mappedBy = VesselPhysicalMeasurement.Fields.VESSEL_FEATURES)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<VesselPhysicalMeasurement> measurements = new ArrayList<>();

}
