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

package net.sumaris.rdf.service;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.service.crypto.CryptoService;
import net.sumaris.core.util.file.FileContentReplacer;
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.dao.NamedModelProducer;
import net.sumaris.rdf.model.ModelVocabulary;
import net.sumaris.rdf.service.data.RdfDataExportOptions;
import net.sumaris.rdf.service.data.RdfDataExportService;
import net.sumaris.rdf.service.schema.RdfSchemaService;
import net.sumaris.server.http.rest.RdfFormat;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.jena.dboe.base.file.Location;
import org.apache.jena.fuseki.servlets.SPARQLProtocol;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.sparql.core.DatasetDescription;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Component("datasetService")
@ConditionalOnBean({RdfConfiguration.class})
public class DatasetService {

    private static final Logger log = LoggerFactory.getLogger(DatasetService.class);

    @Resource
    private RdfSchemaService schemaService;

    @Resource
    private RdfDataExportService dataExportService;

    @Resource
    private RdfConfiguration config;

    @Resource
    private CryptoService cryptoService;

    @Value("${rdf.tdb2.enabled:true}")
    private boolean enableTdb2;

    @Value("${rdf.sparql.maxLimit:10000}")
    private long maxLimit;

    private Model defaultModel;

    private Dataset dataset;

    @Resource(name = "taxrefRdfTaxonDao")
    private NamedModelProducer taxrefRdfTaxonDao;

    @Resource(name = "sandreRdfTaxonDao")
    private NamedModelProducer sandreRdfTaxonDao;

    @Autowired(required = false)
    protected TaskExecutor taskExecutor;

    private Map<String, Callable<Model>> namedGraphFactories = Maps.newHashMap();


    @PostConstruct
    public void start() {
        // Register taxon daos
        registerNameModel(taxrefRdfTaxonDao,10000L);
        registerNameModel(sandreRdfTaxonDao, -1L);

        // Init the query dataset
        this.dataset = createDataset();

        // Fill dataset (async if possible)
        if (taskExecutor != null) {
            taskExecutor.execute(() -> {
                try {
                    Thread.sleep(10 * 1000); // Wait server starts

                    loadDataset(this.dataset);

                } catch (InterruptedException e) { }
            });
        }
        else {
            loadDataset(this.dataset);
        }
    }

    @PreDestroy
    public void destroy() {
        this.dataset.close();
    }

    public void registerNameModel(final NamedModelProducer producer, final long maxStatements) {
        registerNamedModel(producer.getName(), () -> unionModel(producer.getName(), producer.streamAllByPages(maxStatements)));
    }

    public void registerNamedModel(final String name, final Callable<Model> producer) {
        namedGraphFactories.put(name, producer);
    }

    public void loadAllNamedModels(Dataset dataset, boolean replaceIfExists) {
        Collection<String> existingNames = listNames();
        namedGraphFactories.forEach((name, producer) -> {

            boolean exists = existingNames.contains(name);

            if (!exists || replaceIfExists) {
                try {
                    // Fetch the model
                    Model model = producer.call();

                    // Store into dataset
                    saveNamedModel(dataset, name, model, replaceIfExists);
                } catch (Exception e) {
                    log.warn("Cannot load {{}}: {}", name, e.getMessage(), e);
                }
            }
        });
    }

    /**
     * Construct a dataset for a query
     * @param query
     * @return a dataset
     */
    public Dataset prepareDatasetForQuery(Query query) {
        Dataset dataset;
        DatasetDescription datasetDescription = SPARQLProtocol.getQueryDatasetDescription(query);
        if (datasetDescription == null) {
            dataset = DatasetFactory.wrap(this.dataset.getUnionModel());
        } else {
            dataset = DatasetFactory.create();
            this.dataset.begin(ReadWrite.READ);

            // Load default graph
            if (CollectionUtils.isNotEmpty(datasetDescription.getDefaultGraphURIs())) {
                dataset.setDefaultModel(
                        datasetDescription.getDefaultGraphURIs().stream()
                        .map(graphUri -> {
                            if (this.dataset.containsNamedModel(graphUri)) {
                                return this.dataset.getNamedModel(graphUri);
                            } else {
                                return FileManager.get().loadModel(graphUri, RdfFormat.fromUrlExtension(graphUri).orElse(RdfFormat.RDF).toJenaFormat());
                            }
                        })
                        .reduce(ModelFactory::createUnion)
                        .orElse(this.defaultModel));
            }
            else {
                dataset.setDefaultModel(this.defaultModel);
            }

            // Load named model, if need
            if (CollectionUtils.isNotEmpty(datasetDescription.getNamedGraphURIs())) {
                for (String graphUri : datasetDescription.getNamedGraphURIs()) {
                    Model namedGraph;
                    if (!this.dataset.containsNamedModel(graphUri)) {
                        namedGraph = FileManager.get().loadModel(graphUri, RdfFormat.fromUrlExtension(graphUri).orElse(RdfFormat.RDF).toJenaFormat());
                    } else {
                        namedGraph = this.dataset.getNamedModel(graphUri);
                    }
                    dataset.addNamedModel(graphUri, namedGraph);
                }
                ;
            }
            this.dataset.end();
        }

        // These will have been taken care of by the "getDatasetDescription"
        if (query.hasDatasetDescription()) {
            // Don't modify input.
            query = query.cloneQuery();
            query.getNamedGraphURIs().clear();
            query.getGraphURIs().clear();
        }

        return dataset;
    }

    /* -- protected methods -- */

    protected Collection<String> listNames() {
        Collection<String> result;
        try (RDFConnection conn = RDFConnectionFactory.connect(dataset)) {
            conn.begin(ReadWrite.READ);
            result = Lists.newArrayList(dataset.listNames());
            conn.end();
        }
        return result;
    }


    protected Dataset createDataset() {
        if (enableTdb2) {

            // Connect or create the TDB2 dataset
            File tdbDir = new File(config.getRdfDirectory(), "tdb");
            log.info("Starting {TDB2} triple store at {{}}...", tdbDir);

            Location location = Location.create(tdbDir.getAbsolutePath());
            this.dataset = TDB2Factory.connectDataset(location);
        }
        else {
            log.info("Starting {memory} triple store...");
            this.dataset = DatasetFactory.createTxnMem();
        }

        return this.dataset;
    }

    /**
     * Fill dataset
     * @param dataset
     * @return
     */
    protected void loadDataset(Dataset dataset) {

        // Generate schema, and store it into the dataset
        this.defaultModel = getFullSchemaOntology();
        FileManager.get().addCacheModel(schemaService.getNamespace(), this.defaultModel);
        try (RDFConnection conn = RDFConnectionFactory.connect(dataset)) {
            Txn.executeWrite(conn, () -> {

                log.info("Loading {{}} into RDF dataset...", schemaService.getNamespace());
                if (dataset.containsNamedModel(schemaService.getNamespace())) {
                    dataset.replaceNamedModel(schemaService.getNamespace(), this.defaultModel);
                } else {
                    dataset.addNamedModel(schemaService.getNamespace(), this.defaultModel);
                }
            });
        }

        // Store taxon entities into the dataset
        // TODO: move this in TaxonDao + registerProducer() ?
        try (RDFConnection conn = RDFConnectionFactory.connect(dataset)) {
            Txn.executeWrite(conn, () -> {

                String graphName = config.getModelBaseUri() + "data/TaxonName";
                log.info("Loading {{}} into RDF dataset...", graphName);
                Model instances = dataExportService.getIndividuals(RdfDataExportOptions.builder()
                        .maxDepth(1)
                        .className("TaxonName")
                        .build());

                if (dataset.containsNamedModel(graphName)) {
                    dataset.replaceNamedModel(graphName, instances);
                } else {
                    dataset.addNamedModel(graphName, instances);
                }
            });
        }

        // Load other named models
        loadAllNamedModels(dataset, false);
    }

    protected Model getFullSchemaOntology() {
        return ModelFactory.createDefaultModel().add(getReferentialSchemaOntology()).add(getDataSchemaOntology());
    }

    protected Model getReferentialSchemaOntology() {
        return schemaService.getOntology(ModelVocabulary.REFERENTIAL);
    }

    protected Model getDataSchemaOntology() {
        return schemaService.getOntology(ModelVocabulary.DATA);
    }

    public Model unionModel(String baseUri, Stream<Model> stream) throws Exception {

        // Init the temp directory
        FileUtils.forceMkdir(config.getTempDirectory());

        final String cacheFileFormat = RdfFormat.TURTLE.toJenaFormat();
        String baseFilename = cryptoService.hash(baseUri) + ".ttl";
        File tempFile = new File(config.getTempDirectory(), baseFilename + ".tmp");
        File cacheFile = new File(config.getRdfDirectory(), baseFilename);

        try {
            if (!cacheFile.exists() && !tempFile.exists()) {
                long now = System.currentTimeMillis();
                log.info("Downloading {}...", baseUri);

                // Write each model received
                try (FileOutputStream fos = new FileOutputStream(tempFile); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    stream.forEach(m -> m.write(bos, cacheFileFormat));
                    fos.flush();
                }

                log.info("Successfully downloaded {{}} in {}ms", baseUri, System.currentTimeMillis() - now);
            }
            else {
                log.debug("Already downloaded {}.", baseUri);
            }
        } catch (Exception e) {
            // Try to continue
            if (tempFile.length() > 0) {
                log.error(e.getMessage(), e);
            }
            else {
                tempFile.delete();
                throw new SumarisTechnicalException(String.format("Error while downloaded {%s}: %s", baseUri, e.getMessage()), e);
            }
        }

        // Patch downloaded file
        if (!cacheFile.exists() && tempFile.exists() && tempFile.length() > 0) {
            // Bad URI in Sandre data (sameAs)
            FileContentReplacer replacer = new FileContentReplacer("/ ?([0-9]+) ?[>]", "/$1>");
            replacer.matchAndReplace(tempFile, cacheFile);

            tempFile.delete();
        }



        // Read model from file
        try {
            return FileManager.get().loadModel("file:" + cacheFile.getAbsolutePath(), cacheFileFormat);
        } catch (Exception e) {
            throw new SumarisTechnicalException(String.format("Error while loading model {%s} from file {%s}: %s", baseUri, tempFile.getPath(), e.getMessage()), e);
        }
    }

    public void saveNamedModel(Dataset dataset, String name, Model model, boolean replaceIfExists) {

        try {
            long now = System.currentTimeMillis();
            log.info("Loading {{}} into RDF dataset...", name);

            try (RDFConnection conn = RDFConnectionFactory.connect(dataset)) {
                Txn.executeWrite(conn, () -> {
                    if (dataset.containsNamedModel(name)) {
                        if (replaceIfExists) dataset.replaceNamedModel(name, model);
                        log.info("Successfully update {{}} in dataset, in {}ms", name, System.currentTimeMillis() - now);
                    } else {
                        dataset.addNamedModel(name, model);
                        log.info("Successfully store {{}} in dataset, in {}ms", name, System.currentTimeMillis() - now);
                    }
                });
            }

        } catch (Exception e) {
            log.warn("Cannot load {{}} data", name, e);
        }
    }

}
