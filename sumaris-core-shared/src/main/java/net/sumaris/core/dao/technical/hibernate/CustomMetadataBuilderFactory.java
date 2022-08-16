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

package net.sumaris.core.dao.technical.hibernate;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.spi.MetadataBuilderFactory;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

/**
 * Custom implementation of MetadataBuilderFactory that handles a configurable IdGeneratorStrategyInterpreter
 * set sumaris.persistence.sequence.increment option to set the allocation size for all sequences
 *
 * @author peck7 on 18/11/2019.
 */
@Slf4j
public class CustomMetadataBuilderFactory implements MetadataBuilderFactory {

    // default no arg constructor needed for Java Service
    public CustomMetadataBuilderFactory() {
    }

    @Override
    public MetadataBuilderImplementor getMetadataBuilder(MetadataSources metadatasources, MetadataBuilderImplementor defaultBuilder) {

        SumarisConfiguration config = SumarisConfiguration.getInstance();

        // Read sumaris.persistence.sequence.increment option
        int allocationSize = config != null ? config.getSequenceIncrementValue() : 0;
        if (allocationSize < 0) {
            log.debug("Invalid sequence allocationSize {}, fallback to default MetadataBuilderImplementor", allocationSize);
            return null;
        }
        if (allocationSize == 0) {
            log.debug("No sequence allocationSize defined in config. fallback to default MetadataBuilderImplementor");
            return null;
        }

        log.info("Using allocationSize {} to all sequences", allocationSize);

        MetadataBuilderImplementor implementor = new MetadataBuilderImpl(metadatasources, defaultBuilder.getBootstrapContext().getServiceRegistry());
        implementor.applyIdGenerationTypeInterpreter(new ConfigurableSequenceGenerator(allocationSize));
        return implementor;
    }

    static class ConfigurableSequenceGenerator implements IdGeneratorStrategyInterpreter {

        private final int allocationSize;

        ConfigurableSequenceGenerator(int allocationSize) {
            this.allocationSize = allocationSize;
        }

        @Override
        public String determineGeneratorName(GenerationType generationType, GeneratorNameDeterminationContext context) {
            if (generationType == GenerationType.SEQUENCE) {
                return SequenceStyleGenerator.class.getName();
            }
            throw new SumarisTechnicalException("Only SEQUENCE strategy is allowed !");
        }

        @Override
        public void interpretTableGenerator(TableGenerator tableGeneratorAnnotation, IdentifierGeneratorDefinition.Builder definitionBuilder) {
            throw new SumarisTechnicalException("Only SEQUENCE strategy is allowed !");
        }

        @Override
        public void interpretSequenceGenerator(
            SequenceGenerator sequenceGeneratorAnnotation,
            IdentifierGeneratorDefinition.Builder definitionBuilder) {
            definitionBuilder.setName( sequenceGeneratorAnnotation.name() );
            definitionBuilder.setStrategy( SequenceStyleGenerator.class.getName() );

            if ( !BinderHelper.isEmptyAnnotationValue( sequenceGeneratorAnnotation.catalog() ) ) {
                definitionBuilder.addParam(
                    PersistentIdentifierGenerator.CATALOG,
                    sequenceGeneratorAnnotation.catalog()
                );
            }
            if ( !BinderHelper.isEmptyAnnotationValue( sequenceGeneratorAnnotation.schema() ) ) {
                definitionBuilder.addParam(
                    PersistentIdentifierGenerator.SCHEMA,
                    sequenceGeneratorAnnotation.schema()
                );
            }
            if ( !BinderHelper.isEmptyAnnotationValue( sequenceGeneratorAnnotation.sequenceName() ) ) {
                definitionBuilder.addParam(
                    SequenceStyleGenerator.SEQUENCE_PARAM,
                    sequenceGeneratorAnnotation.sequenceName()
                );
            }

            definitionBuilder.addParam(
                SequenceStyleGenerator.INCREMENT_PARAM,
                String.valueOf(this.allocationSize) // Forced by configuration instead of annotation
            );
            definitionBuilder.addParam(
                SequenceStyleGenerator.INITIAL_PARAM,
                String.valueOf( sequenceGeneratorAnnotation.initialValue() )
            );
        }
    }
}
