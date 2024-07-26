package net.sumaris.core.service;

/*-
 * #%L
 * SUMARiS :: Sumaris Core Shared
 * $Id:$
 * $HeadURL:$
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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.service.technical.schema.DatabaseSchemaService;
import org.springframework.context.ApplicationContext;

import java.io.Closeable;


/**
 * Locates and provides all available application services.
 */
@Slf4j
public class ServiceLocator implements Closeable {

    /**
     * The core instance of this ServiceLocator.
     */
    private static ServiceLocator INSTANCE = new ServiceLocator();

    /**
     * Indicates if the spring context is open or not.
     */
    private boolean open = false;

    /**
     * <p>Constructor for ServiceLocator.</p>
     */
    protected ServiceLocator() {
    }

    /**
     * replace the default core instance of this Class
     *
     * @param newInstance the new core service locator instance.
     */
    public static void setInstance(ServiceLocator newInstance) {
        INSTANCE = newInstance;
    }

    /**
     * Gets the core instance of this Class
     *
     * @return the core service locator instance.
     */
    public static ServiceLocator instance() {
        return INSTANCE;
    }

    /**
     * The bean factory reference instance.
     */
    private ApplicationContext applicationContext;

    /**
     * <p>initDefault.</p>
     */
    public static void init(ApplicationContext applicationContext) {
        INSTANCE.setApplicationContext(applicationContext);
        ServiceLocator.setInstance(INSTANCE);
    }

    /**
     * Shuts down the ServiceLocator and releases any used resources.
     */
    public synchronized void shutdown() {
        // Do not try to close if not already opened
        if (!open) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Close Spring application context");
        }

        // applicationContext.close()

        open = false;
    }

    /**
     * Get a service.
     *
     * @param name        name of the service (i.e name of the spring bean)
     * @param serviceType type of service
     * @param <S>         type of the service
     * @return the instantiated service
     */
    public <S> S getService(String name, Class<S> serviceType) {

        return applicationContext.getBean(name, serviceType);
    }

    /**
     * <p>isOpen.</p>
     *
     * @return {@code true} if spring context is open, {@code false} otherwise.
     * @since 3.5.2
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Gets the Spring ApplicationContext.
     *
     * @return a {@link ApplicationContext} object.
     */
    protected synchronized ApplicationContext getContext() {
        return this.applicationContext;
    }


    protected synchronized void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        shutdown();
    }

    /**
     * <p>getDatabaseSchemaService.</p>
     *
     * @return a {@link DatabaseSchemaService} object.
     */
    public DatabaseSchemaService getDatabaseSchemaService() {
        return getService("databaseSchemaService", DatabaseSchemaService.class);
    }
}
