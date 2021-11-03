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

package net.sumaris.rdf.core.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.model.ModelURIs;
import net.sumaris.rdf.core.service.data.RdfIndividualFetchOptions;
import net.sumaris.rdf.core.service.data.RdfIndividualService;
import net.sumaris.rdf.core.service.schema.RdfSchemaFetchOptions;
import net.sumaris.rdf.core.service.schema.RdfSchemaService;
import net.sumaris.rdf.core.util.ModelUtils;
import net.sumaris.rdf.core.util.RdfFormat;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component("rdfModelService")
@ConditionalOnBean({RdfConfiguration.class})
@Slf4j
public class RdfModelServiceImpl implements RdfModelService {

    @Resource
    private RdfSchemaService schemaExportService;

    @Resource
    private RdfIndividualService dataExportService;

    @Resource
    private RdfConfiguration config;

    @Override
    public boolean isLocalIri(String iri) {
        Preconditions.checkNotNull(iri);
        return iri.startsWith(config.getModelBaseUri());
    }

    @Override
    public Model get(String iri, @Nullable RdfFormat format) {
        Preconditions.checkArgument(StringUtils.isNotBlank(iri), "Invalid IRI: " + iri);

        if (format == null) {
            log.info("Reading model {{}}...", iri);
        } else {
            log.info("Reading {} model {{}}...", format.toJenaFormat(), iri);
        }

        // If URI is NOT local (not in current pod)
        if (!isLocalIri(iri)) {

            // Try to resolve IRI, from known namespace/prefix
            return ModelURIs.getModelUrlByNamespace(iri)
                    // IRI is mapped: parse the corresponding URL, without format (will detect it)
                    .map(mappedUrl -> ModelUtils.read(mappedUrl, (RdfFormat)null))
                    // IRI not mapped, read using the given format (if any)
                    .orElseGet(() -> ModelUtils.read(iri, format));
        }

        // Path must match /ontology/{schema|data}/{class}/{id}
        URI relativeUri = URI.create(iri.substring(config.getModelBaseUri().length()));
        List<String> pathParams = Splitter.on('/').omitEmptyStrings().trimResults().splitToList(relativeUri.getPath());
        if (pathParams.size() < 1 || pathParams.size() > 3) {
            throw new IllegalArgumentException("Invalid URI: " + iri);
        }

        String modelType = pathParams.get(0);
        String className = pathParams.size() > 1 ? pathParams.get(1) : null;

        switch (modelType) {
            case "schema":
                RdfSchemaFetchOptions schemaOptions = RdfSchemaFetchOptions.builder()
                        .withEquivalences(true)
                        .className(className)
                        .build();
                fillSchemaOptionsByUri(schemaOptions, relativeUri);
                return schemaExportService.getOntology(schemaOptions);
            case "data":
                String objectId = pathParams.size() > 2 ? pathParams.get(2) : null;
                RdfIndividualFetchOptions dataOptions = RdfIndividualFetchOptions.builder()
                        .className(className)
                        .id(objectId)
                        .build();
                fillDataOptionsByUri(dataOptions, relativeUri);
                return dataExportService.getIndividuals(dataOptions);
            default:
                throw new IllegalArgumentException("Invalid URI: " + iri);
        }
    }

    @Override
    public Model union(String[] iris, @Nullable RdfFormat sourceFormat) {
        return Arrays.stream(iris)
                .map(partIri -> {
                    if (StringUtils.isBlank(partIri)) throw new IllegalArgumentException("Invalid 'uri': " + partIri);

                    // Retrieve the source format
                    RdfFormat partFormat = sourceFormat;
                    if (partFormat == null) {
                        partFormat = RdfFormat.fromUrlExtension(partIri)
                                .orElse(RdfFormat.RDFXML);
                    };

                    // Read model from uri
                    return get(partIri.trim(), partFormat);
                })
                // Union on all models
                .reduce(ModelFactory::createUnion).orElse(null);
    }

    @Override
    public byte[] convert(Model model, @Nullable RdfFormat targetFormat) {
        Preconditions.checkNotNull(model);

        log.info("Converting model into {}...", targetFormat.toJenaFormat());
        return ModelUtils.toBytes(model, targetFormat);
    }

    @Override
    public byte[] convert(String iri, RdfFormat sourceFormat, RdfFormat targetFormat) {
        if (StringUtils.isBlank(iri)) throw new IllegalArgumentException("Invalid 'iri': " + iri);

        // Load the model
        Model model = get(iri, sourceFormat);

        // Apply conversion
        return convert(model, targetFormat);
    }

    @Override
    public byte[] unionThenConvert(String[] iris, RdfFormat sourceFormat, RdfFormat targetFormat) {

        // IF only one iri to load: use simple conversion method
        if (iris.length == 1) {
            return convert(iris[0], sourceFormat, targetFormat);
        }

        // Create an union model
        Model model = union(iris, sourceFormat);

        // Apply conversion
        return convert(model, targetFormat);
    }

    /* -- protected functions -- */

    protected RdfSchemaFetchOptions fillSchemaOptionsByUri(RdfSchemaFetchOptions options, URI uri) {
        Preconditions.checkNotNull(options);
        Preconditions.checkNotNull(uri);
        Map<String, String> requestParams = parseQueryParams(uri);

        // With disjoint ?
        String disjoints = requestParams.get("disjoints");
        if (StringUtils.isNotBlank(disjoints)) options.setWithDisjoints(! "false".equalsIgnoreCase(disjoints)); // true by default

        // With equivalences ?
        String equivalences = requestParams.get("equivalences");
        if (StringUtils.isNotBlank(equivalences)) options.setWithEquivalences(!"false".equalsIgnoreCase(equivalences)); // true by default

        // packages ? empty by default
        String packages = requestParams.get("packages");
        if (StringUtils.isNotBlank(packages)) {
            options.setPackages(Splitter.on(',').omitEmptyStrings().trimResults().splitToList(packages));
        }

        return options;
    }

    protected RdfIndividualFetchOptions fillDataOptionsByUri(RdfIndividualFetchOptions options, URI uri) {
        Preconditions.checkNotNull(options);
        Preconditions.checkNotNull(uri);
        Map<String, String> requestParams = parseQueryParams(uri);

        // Page offset = 'from' query param
        String pageOffset = requestParams.get("from");
        if (StringUtils.isNotBlank(pageOffset)) options.getPage().setOffset(Integer.parseInt(pageOffset));

        // Page size = 'size' query param
        String pageSize = requestParams.get("size");
        if (StringUtils.isNotBlank(pageSize)) options.getPage().setSize(Integer.parseInt(pageSize));

        return options;
    }

    protected Map<String, String> parseQueryParams(URI uri) {
        Preconditions.checkNotNull(uri);

        Map<String, String> result = Maps.newHashMap();

        String query = uri.getQuery();
        if (StringUtils.isNotBlank(query)) {
            for (String paramStr : Splitter.on('&').omitEmptyStrings().trimResults().split(query)) {
                String[] paramParts = paramStr.split("=");
                if (paramParts.length == 1) {
                    result.put(paramParts[0], "true");
                }
                else if (paramParts.length == 2) {
                    result.put(paramParts[0], paramParts[1]);
                }
                else {
                    // Ignore
                    if (log.isInfoEnabled()) log.info("Skipping invalid IRI's query parameter: " + paramStr);
                }
            }
        }
        return result;
    }
}
