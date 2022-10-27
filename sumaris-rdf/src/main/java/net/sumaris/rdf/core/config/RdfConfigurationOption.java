/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.rdf.core.config;

import net.sumaris.rdf.core.util.RdfFormat;
import org.nuiton.config.ConfigOptionDef;

import java.io.File;

import static org.nuiton.i18n.I18n.n;

public enum RdfConfigurationOption implements ConfigOptionDef {

    RDF_ENABLED(
        "rdf.enabled",
        n("sumaris.config.option.rdf.enabled.description"),
        "true",
        Boolean.class,
        false),

    RDF_DIRECTORY(
        "rdf.directory",
        n("sumaris.config.option.rdf.directory.description"),
        "${sumaris.data.directory}/rdf",
        File.class,
        false),

    RDF_MODEL_DATE(
        "rdf.model.date",
        n("sumaris.config.option.rdf.model.date.description"),
        "2021-10-01",
        String.class,
        false),

    RDF_MODEL_PREFIX(
        "rdf.model.prefix",
        n("sumaris.config.option.rdf.model.prefix.description"),
        "${spring.application.name}",
        String.class,
        false),

    RDF_MODEL_VERSION(
        "rdf.model.version",
        n("sumaris.config.option.rdf.model.version.description"),
        "0.1",
        String.class,
        false),

    RDF_MODEL_BASE_URI(
        "rdf.model.baseUri",
        n("sumaris.config.option.rdf.model.baseUri.description"),
        "${server.url}",
        String.class,
        false),

    RDF_MODEL_LANGUAGE(
        "rdf.model.language",
        n("sumaris.config.option.rdf.model.language.description"),
        "fr",
        String.class,
        false),

    RDF_MODEL_DESCRIPTION(
        "rdf.model.description",
        n("sumaris.config.option.rdf.model.description.description"),
        "Modèle de données du ${spring.application.name}",
        String.class,
        false),

    RDF_MODEL_LICENSE(
        "rdf.model.license",
        n("sumaris.config.option.rdf.model.license.description"),
        "http://www.gnu.org/licenses/gpl-3.0.html",
        String.class,
        false),

    RDF_MODEL_AUTHORS(
        "rdf.model.authors",
        n("sumaris.config.option.rdf.model.authors.description"),
        "${sumaris.organization.name}",
        String.class,
        false),

    RDF_MODEL_COMMENT(
        "rdf.model.comment",
        n("sumaris.config.option.rdf.model.comment.description"),
        "${rdf.model.comment.fr}",
        String.class,
        false),

    RDF_MODEL_COMMENT_FR(
        "rdf.model.comment.fr",
        n("sumaris.config.option.rdf.model.comment.fr.description"),
        "Consulter la documentation complète sur ${rdf.model.doc.url}",
        String.class,
        false),

    RDF_MODEL_COMMENT_EN(
        "rdf.model.comment.en",
        n("sumaris.config.option.rdf.model.comment.en.description"),
        "Please see full documentation at ${rdf.model.doc.url}",
        String.class,
        false),

    RDF_MODEL_DOC_URL(
        "rdf.model.doc.url",
        n("sumaris.config.option.rdf.model.comment.en.description"),
        "${sumaris.site.url}",
        String.class,
        false),

    RDF_MODEL_PUBLISHER(
        "rdf.model.publisher",
        n("sumaris.config.option.rdf.model.publisher.description"),
        "${sumaris.site.url}",
        String.class,
        false),

    RDF_DEFAULT_PAGE_SIZE(
        "rdf.data.pageSize.default",
        n("sumaris.config.option.rdf.data.pageSize.default.description"),
        "100",
        Integer.class,
        false),

    RDF_MAX_PAGE_SIZE(
        "rdf.data.pageSize.max",
        n("sumaris.config.option.rdf.data.pageSize.max.description"),
        "10000",
        Integer.class,
        false),

    RDF_TDB2_ENABLED(
        "rdf.tdb2.enabled",
        n("sumaris.config.option.rdf.tdb2.enabled.description"),
        Boolean.TRUE.toString(),
        String.class,
        false),

    RDF_TDB2_DIRECTORY(
        "rdf.tdb2.directory",
        n("sumaris.config.option.rdf.tdb2.directory.description"),
        "${rdf.directory}/tdb2",
        File.class,
        false),

    RDF_DATA_IMPORT_ENABLED(
        "rdf.data.import.enabled",
        n("sumaris.config.option.rdf.data.import.enabled.description"),
        Boolean.TRUE.toString(),
        Boolean.class,
        false),

    RDF_DATA_IMPORT_DB_ENABLED(
        "rdf.data.import.database.enabled",
        n("sumaris.config.option.rdf.data.import.database.enabled.description"),
        "${rdf.data.import.enabled}",
        Boolean.class,
        false),

    RDF_DATA_IMPORT_EXTERNAL_ENABLED(
        "rdf.data.import.external.enabled",
        n("sumaris.config.option.rdf.data.import.external.enabled.description"),
        "${rdf.data.import.enabled}",
        Boolean.class,
        false),

    RDF_OUTPUT_FORMAT(
        "rdf.cli.output.format",
        n("sumaris.config.option.rdf.cli.output.format.description"),
        RdfFormat.TRIG.getName(),
        String.class,
        false);

    /**
     * Configuration key.
     */
    private final String key;

    /**
     * I18n key of option description
     */
    private final String description;

    /**
     * Type of option
     */
    private final Class<?> type;

    /**
     * Default value of option.
     */
    private String defaultValue;

    /**
     * Flag to not keep option value on disk
     */
    private boolean isTransient;

    /**
     * Flag to not allow option value modification
     */
    private boolean isFinal;

    RdfConfigurationOption(String key,
                           String description,
                           String defaultValue,
                           Class<?> type,
                           boolean isTransient) {
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
        this.type = type;
        this.isTransient = isTransient;
        this.isFinal = isTransient;
    }

    RdfConfigurationOption(String key,
                           String description,
                           String defaultValue,
                           Class<?> type) {
        this(key, description, defaultValue, type, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getKey() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTransient() {
        return isTransient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFinal() {
        return isFinal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTransient(boolean newValue) {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFinal(boolean newValue) {
        // not used
    }

}
