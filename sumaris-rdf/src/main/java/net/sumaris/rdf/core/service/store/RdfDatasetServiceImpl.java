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

package net.sumaris.rdf.core.service.store;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.service.crypto.CryptoService;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.util.file.FileContentReplacer;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.config.RdfConfigurationOption;
import net.sumaris.rdf.core.loader.INamedRdfLoader;
import net.sumaris.rdf.core.model.ModelVocabulary;
import net.sumaris.rdf.core.service.data.RdfIndividualFetchOptions;
import net.sumaris.rdf.core.service.data.RdfIndividualService;
import net.sumaris.rdf.core.service.schema.RdfSchemaService;
import net.sumaris.rdf.core.util.RdfFormat;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.jena.dboe.base.file.Location;
import org.apache.jena.fuseki.servlets.SPARQLProtocol;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.sparql.core.DatasetDescription;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.tdwg.rs.DWC;
import org.w3.W3NS;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component("rdfDatasetService")
@ConditionalOnBean({RdfConfiguration.class})
@Slf4j
public class RdfDatasetServiceImpl implements RdfDatasetService {

    @Resource
    private RdfSchemaService schemaService;

    @Resource
    private RdfIndividualService individualService;

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

    @Resource(name = "mnhnTaxonLoader")
    private INamedRdfLoader mnhnTaxonLoader;

    @Resource(name = "sandreTaxonLoader")
    private INamedRdfLoader sandreTaxonLoader;

    @Resource(name = "sandreDepartmentLoader")
    private INamedRdfLoader sandreDepartmentLoader;

    @Autowired(required = false)
    protected TaskExecutor taskExecutor;

    private Map<String, Callable<Model>> namedGraphFactories = Maps.newHashMap();


    @PostConstruct
    public void init() {
        // Register external loaders
        registerNameModel(mnhnTaxonLoader, 10000L); // FIXME MNHN endpoint has problem when offset >= 10000
        registerNameModel(sandreTaxonLoader, -1L);
        registerNameModel(sandreDepartmentLoader, -1L);

        // Init the query dataset
        this.dataset = createDataset();
    }

    @EventListener({ConfigurationReadyEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        // If auto import, load dataset
        if (config.isRdfImportEnable()) {
            if (!containsAllTypes(this.dataset, W3NS.Org.Organization, DWC.Voc.TaxonName)) {
                this.scheduleLoadDataset();
            }
            else {
                log.debug("Triple store already loaded. To force reload: remove directory {{}} then restart pod", config.getRdfTdb2Directory().getAbsoluteFile());
            }
        }
        else {
            log.debug("Triple store will not be loaded, because configuration option '{}' set to false.", RdfConfigurationOption.RDF_DATA_IMPORT_ENABLED.getKey());
        }
    }

    @PreDestroy
    public void destroy() {
        this.dataset.close();
    }

    public void registerNameModel(final INamedRdfLoader producer, final long maxStatements) {
        if (producer == null) return; // Skip if empty
        registerNamedModel(producer.getName(), () -> unionModel(producer.getName(), producer.streamAllByPages(maxStatements)));
    }

    public void registerNamedModel(final String name, final Callable<Model> producer) {
        namedGraphFactories.put(name, producer);
    }

    public void loadAllNamedModels(Dataset dataset, boolean replaceIfExists) {
        Set<String> modelNames = getModelNames();
        namedGraphFactories.forEach((name, producer) -> {

            boolean exists = modelNames.contains(name);

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
                                return FileManager.get().loadModel(graphUri, RdfFormat.fromUrlExtension(graphUri).orElse(RdfFormat.RDFXML).toJenaFormat());
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
                        namedGraph = FileManager.get().loadModel(graphUri, RdfFormat.fromUrlExtension(graphUri).orElse(RdfFormat.RDFXML).toJenaFormat());
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

    /**
     * Fill dataset
     * @return
     */
    public void loadDataset() {
        loadDataset(null);
    }

    /**
     * Fill dataset
     * @return
     */
    public Dataset getDataset() {
        return DatasetFactory.wrap(this.dataset.getUnionModel());
    }

    public Set<String> getModelNames() {
        Set<String> result;
        try (RDFConnection conn = RDFConnectionFactory.connect(dataset)) {
            conn.begin(ReadWrite.READ);
            result = Sets.newHashSet(dataset.listNames());
            conn.end();
        }
        return result;
    }

    /* -- protected methods -- */


    protected Dataset createDataset() {
        if (enableTdb2) {

            // Connect or create the TDB2 dataset
            File tdb2Dir = config.getRdfTdb2Directory();
            log.info("Starting {TDB2} triple store at {{}}...", tdb2Dir);

            // Check access write
            if (tdb2Dir.exists()) {
                if (!tdb2Dir.isDirectory() || !tdb2Dir.canRead() || !tdb2Dir.canWrite()) {
                    throw new SumarisTechnicalException("Missing write/read access to directory: " + tdb2Dir.getAbsolutePath());
                }
            }
            else {
                try {
                    FileUtils.forceMkdir(tdb2Dir);
                }
                catch (IOException e) {
                    throw new SumarisTechnicalException("Cannot create directory: " + tdb2Dir.getAbsolutePath());
                }
            }

            Location location = Location.create(tdb2Dir.getAbsolutePath());
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
     * @param ds can be null
     * @return
     */
    protected void loadDataset(@Nullable Dataset ds) {

        log.info("Load data into triple store...");

        final Dataset dataset = ds != null ? ds : this.dataset;

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

        // Load exported entities into dataset
        String[] entityNames = config.getDataImportDbEntities();
        if (ArrayUtils.isNotEmpty(entityNames)) {
            Arrays.asList(entityNames).forEach(entity -> loadDatabaseEntities(entity, dataset, true));
        }

        // Load other named models
        if (config.enableDataImportFromExternal()) {
            loadAllNamedModels(dataset, false);
        }
    }

    protected void loadDatabaseEntities(String entityName, Dataset dataset, boolean replaceIfExists) {
        // Store taxon entities into the dataset
        try (RDFConnection conn = RDFConnectionFactory.connect(dataset)) {
            Txn.executeWrite(conn, () -> {

                String graphName = config.getModelBaseUri() + "data/" + entityName;
                log.info("Loading {{}} into RDF dataset...", graphName);
                Model instances = individualService.getIndividuals(RdfIndividualFetchOptions.builder()
                        .maxDepth(1)
                        .domain(ModelVocabulary.REFERENTIAL)
                        .className(entityName)
                        .build());

                // Add if missing
                if (!dataset.containsNamedModel(graphName)) {
                    dataset.addNamedModel(graphName, instances);
                }

                // Or replace, if exists
                else if (replaceIfExists) {
                    dataset.replaceNamedModel(graphName, instances);
                }
            });
        }
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
                long startTime = System.currentTimeMillis();
                log.info("Downloading data from {{}}...", baseUri);

                // Write each model received
                try (FileOutputStream fos = new FileOutputStream(tempFile); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    stream.forEach(m -> m.write(bos, cacheFileFormat));
                    fos.flush();
                }

                log.info("Data from {{}} downloaded successfully, in {}", baseUri, TimeUtils.printDurationFrom(startTime));
            }
            else {
                log.debug("Data from {{}} already exists. Skip download", baseUri);
                // TODO download changes, using dcterms:modified ?
            }
        } catch (Exception e) {
            // Try to continue
            if (tempFile.length() > 0) {
                log.error(e.getMessage(), e);
            }
            else {
                tempFile.delete();
                throw new SumarisTechnicalException(String.format("Error while downloaded data from {{%s}}: %s", baseUri, e.getMessage()), e);
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
            long startTime = System.currentTimeMillis();
            log.info("Loading {{}} into RDF dataset...", name);

            try (RDFConnection conn = RDFConnectionFactory.connect(dataset)) {
                Txn.executeWrite(conn, () -> {
                    if (dataset.containsNamedModel(name)) {
                        if (replaceIfExists) dataset.replaceNamedModel(name, model);
                        log.info("Successfully update {{}} in dataset, in {}", name, TimeUtils.printDurationFrom(startTime));
                    } else {
                        dataset.addNamedModel(name, model);
                        log.info("Successfully store {{}} in dataset, in {}", name, TimeUtils.printDurationFrom(startTime));
                    }
                });
            }

        } catch (Exception e) {
            log.warn("Cannot load {{}} data", name, e);
        }
    }

    protected void scheduleLoadDataset(){
        // Fill dataset (async if possible)
        if (taskExecutor != null) {
            taskExecutor.execute(() -> {
                try {
                    Thread.sleep(10 * 1000); // Wait server starts

                    loadDataset();

                } catch (InterruptedException e) {
                }
            });
        } else {
            loadDataset();
        }
    }

    protected boolean containsAllTypes(Dataset dataset, org.apache.jena.rdf.model.Resource... types) {
        long existingTypeCount = Stream.of(types)
            .filter(type -> containsOneOfTypes(dataset, type))
            .count();

        return existingTypeCount == types.length;
    }

    protected boolean containsOneOfTypes(Dataset dataset, org.apache.jena.rdf.model.Resource... types) {
        String queryString = "PREFIX rdf: <" + RDF.getURI() + ">\n" +
                "SELECT (COUNT(?s) as ?count)\n" +
                "WHERE\n" +
                "{\n" +
                "  ?s rdf:type ?type .\n";

        if (ArrayUtils.isNotEmpty(types)) {
            queryString += "  FILTER(\n" +
                    " ?type = <" + Stream.of(types)
                    .map(org.apache.jena.rdf.model.Resource::getURI)
                    .collect(Collectors.joining("> || ?type = <")) + ">\n" +
                    ")\n";
        }

        queryString += "}\n" +
                "GROUP BY ?s";
        return countQuery(dataset, queryString) > 0;
    }

    protected long countQuery(Dataset dataset, String queryString) {

        log.debug("Executing SparQL count query: \n" + queryString);

        Query query = QueryFactory.create(queryString);
        dataset = (dataset != null && dataset != this.dataset) ? dataset : prepareDatasetForQuery(query);
        MutableLong result = new MutableLong(0);
        try (RDFConnection conn = RDFConnectionFactory.connect(dataset); QueryExecution qExec = conn.query(query)) {
            Txn.executeRead(conn, () -> {
                ResultSet rs = qExec.execSelect();
                if (rs.hasNext()) {
                    result.setValue(rs.next().get("count").asLiteral().getLong());
                }
            });
        }
        Preconditions.checkNotNull(result);
        return result.longValue();
    }
}
