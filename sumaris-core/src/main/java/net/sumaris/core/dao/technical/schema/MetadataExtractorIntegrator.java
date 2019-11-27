package net.sumaris.core.dao.technical.schema;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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
