package net.sumaris.core.dao.technical.schema;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

/**
 * Custom implementation of MetadataBuilderFactory that handles a configurable IdGeneratorStrategyInterpreter
 * set sumaris.persistence.sequence.increment option to set the allocation size for all sequences
 *
 * @author peck7 on 18/11/2019.
 */
@Component
public class CustomMetadataBuilderFactory implements MetadataBuilderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CustomMetadataBuilderFactory.class);

    // default no arg constructor needed for Java Service
    public CustomMetadataBuilderFactory() {
    }

    @Override
    public MetadataBuilderImplementor getMetadataBuilder(MetadataSources metadatasources, MetadataBuilderImplementor defaultBuilder) {

        // Read sumaris.persistence.sequence.increment option
        int allocationSize = SumarisConfiguration.getInstance().getSequenceIncrementValue();
        if (allocationSize <= 0) {
            LOG.debug(String.format("invalid allocationSize : %s, fallback to default MetadataBuilderImplementor", allocationSize));
            return null;
        }

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
                // Set allocationSize from configuration instead of annotation
                String.valueOf( allocationSize /*sequenceGeneratorAnnotation.allocationSize()*/ )
            );
            definitionBuilder.addParam(
                SequenceStyleGenerator.INITIAL_PARAM,
                String.valueOf( sequenceGeneratorAnnotation.initialValue() )
            );
        }
    }
}
