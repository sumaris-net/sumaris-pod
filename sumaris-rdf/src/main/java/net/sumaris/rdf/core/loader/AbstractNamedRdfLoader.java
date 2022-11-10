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

package net.sumaris.rdf.core.loader;


import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.ModelVocabularyEnum;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.model.ModelType;
import net.sumaris.rdf.core.util.ModelUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public abstract class AbstractNamedRdfLoader implements INamedRdfLoader, ResourceLoaderAware {

    private ResourceLoader resourceLoader;


    @Autowired(required = false)
    private RdfConfiguration config;

    @Override
    public boolean enable() {
        return config != null;
    }

    @Override
    public Stream<Model> streamAllByPages(long maxLimit) {
        return Daos.streamByPageIteration(
            this::loadOnePage,
            this::hasNextPage,
            getFetchSize(),
            maxLimit);
    }

    @Override
    public Model loadOnePage(Page page) {

        String queryString = getConstructQuery(page);

        log.debug("Downloading data {{}} - Executing SparQL query on endpoint {{}}...\n{}", getName(), getEndpointUrl(), queryString);

        Query query = QueryFactory.create(queryString);
        try (RDFConnection conn = connect(); QueryExecution execution = conn.query(query)) {
            return execution.execConstruct();
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /* -- protected methods  -- */

    protected abstract String getEndpointUrl();

    protected int getFetchSize() {
        return 10000;
    }

    protected RDFConnection connect() {
        return RDFConnectionFactory.connect(getEndpointUrl());
    }

    protected String getQuery() {
        String queryPath = getQueryPath();
        if (queryPath == null) {
            throw new SumarisTechnicalException("Not implemented. Please provide a queryFile, or override getQuery()");
        }
        try {
            // use resource loader, if enable
            if (resourceLoader != null) {
                Resource resource = resourceLoader.getResource(queryPath);
                try (InputStream is = resource.getInputStream()) {
                    return net.sumaris.core.util.Files.readContent(is, Charsets.UTF_8);
                }
            }
            else {
                // Read File Content
                File file = ResourceUtils.getFile(queryPath);
                return new String(Files.readAllBytes(file.toPath()), Charsets.UTF_8);
            }

        } catch (IOException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    protected String getQueryPath() {
        return null;
    }

    protected String getConstructQuery(Page page) {
        // Read query (e.g. from query file)
        String query = getQuery();

        // Bind (replace some parameters)
        query = bindQuery(query);

        return SparqlQueries.asPageableQuery(query, page);
    }

    protected String bindQuery(String queryString) {
        Map<String, String> parameters = getQueryParameters();
        if (parameters == null) return queryString;

        return SparqlQueries.bindParameters(queryString, parameters);
    }

    protected Map<String, String> getQueryParameters(){
        // Can be overwritten by subclasses
        return getUriByVocabulary();
    }

    protected Map<String, String> getUriByVocabulary() {
        Map<String, String> uriByVocab = Maps.newHashMap();

        for (ModelVocabularyEnum vocab: ModelVocabularyEnum.values()) {
            String vocabUri = config.getModelVocabularyUri(ModelType.SCHEMA, vocab.getLabel());
            uriByVocab.put(vocab.getLabel(), vocabUri);
        }
        return uriByVocab;
    }

    /**
     * From a fetched model, return if there is a next page or not.
     * E.g. when model is empty, then it should not have more page.
     * @param model
     * @return
     */
    protected boolean hasNextPage(Model model) {
        return ModelUtils.isNotEmpty(model);
    }
}
