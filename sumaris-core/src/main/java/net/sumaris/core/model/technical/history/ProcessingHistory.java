package net.sumaris.core.model.technical.history;

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

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.annotation.Comment;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.ProcessingStatus;
import net.sumaris.core.model.referential.ProcessingType;

import javax.persistence.*;
import java.util.Date;

/**
 * Historique des traitements, qu’il s’agisse de flux (comme historiquement la table HIS_FLUX) ou non
 * (traitement d’agrégation, CQ auto, etc.).
 *
 * Permet donc de conserver l'historique des traitements qui se sont exécutés sur le système, notamment ceux qui ont
 * impactés la base de données brutes (Adagio).
 *
 *  L’exécution des traitements en erreur peuvent également être tracée.
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "processing_history")
@Comment("Liste des traitements lancés")
public class ProcessingHistory implements IEntity<Integer>, IUpdateDateEntity<Integer, Date> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PROCESSING_HISTORY_SEQ")
    @SequenceGenerator(name = "PROCESSING_HISTORY_SEQ", sequenceName="PROCESSING_HISTORY_SEQ", allocationSize = IReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
    private Integer id;

    /**
     * Nom du traitement, unique pour un type de traitement donné.
     * Par exemple, pour un traitement d'importation le nom du flux est le nom du fichier reçu par mail.
     * Ce fichier peut lui même référencer plusieurs fichiers qui composent le flux (Exemple : flux IDROLE).
     */
    @Column(length = 100)
    private String name;

    @Column(name = "processing_date", nullable = false)
    private Date date;

    /**
     * S'il s'agit d'un traitement manipulant des données (importation ou exportation) :
     * Type de transfert des données. valeurs possibles : MAIL, FTP, ETL
     */
    @Column(name="data_transfert_type")
    private String dataTransfertType;

    /**
     * S'il s'agit d'un traitement manipulant des données (importation ou exportation) : Information permettant de
     * retrouver l'origine de la donnée.
     *
     * Par exemple : l'email de l'émetteur, l'adresse FTP du fichier, etc.
     */
    @Column(name="data_transfert_address")
    private String dataTransfertAddress;

    /**
     * S'il s'agit d'un traitement manipulant des données (importation ou exportation) : Date du transfert des données
     * vers de destinataire (pour les flux en EXPORT) ou vers la base (pour les flux en IMPORT).
     */
    @Column(name="data_transfert_date")
    private Date dataTransfertDate;

    /**
     * Configuration du traitement, par exemple les paramètres utilisés dans la ligne de commande.
     */
    @Column(name="configuration")
    @Lob()
    private String configuration;

    /**
     * La configuration, sous forme XML (utilisé par les traitements CQ automatique)
     */
    @Column(name="xml_configuration", length = 3000)
    @Lob()
    private String xmlConfiguration;

    /**
     * Use to store execution reports
     */
    @Column(name="xml_report", length = 3000)
    @Lob()
    private String xmlReport;

    /* -- quality insurance -- */

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    /* -- type and status -- */

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ProcessingType.class)
    @JoinColumn(name = "processing_type_fk", nullable = false)
    private ProcessingType processingType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ProcessingStatus.class)
    @JoinColumn(name = "processing_status_fk", nullable = false)
    private ProcessingStatus processingStatus;

    /* -- child entities -- */


}
