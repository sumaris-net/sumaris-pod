package net.sumaris.core.dao.technical.schema;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import java.util.List;

public class MetadataExtractorIntegrator implements Integrator, IntegratorProvider {

    public static final MetadataExtractorIntegrator INSTANCE = new MetadataExtractorIntegrator();

    private Database database;

    private Metadata metadata;

    private SessionFactoryImplementor sessionFactory;

    public Database getDatabase() {
        return database;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public SessionFactoryImplementor getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public void integrate(
            Metadata metadata,
            SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {

        this.database = metadata.getDatabase();
        this.metadata = metadata;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void disintegrate(
        SessionFactoryImplementor sessionFactory,
        SessionFactoryServiceRegistry serviceRegistry) {

    }

    @Override
    public List<Integrator> getIntegrators() {
        return null;
    }


}