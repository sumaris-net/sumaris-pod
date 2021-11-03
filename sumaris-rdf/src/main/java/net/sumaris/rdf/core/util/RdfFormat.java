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

package net.sumaris.rdf.core.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.sumaris.core.util.StringUtils;
import org.apache.jena.riot.Lang;
import org.springframework.http.MediaType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Extend Jena ang, to add more utility functions, and a new VOWL format
 */
public class RdfFormat extends Lang {


    public static RdfFormat RDFXML = new RdfFormat(Lang.RDFXML, RdfMediaType.APPLICATION_RDF_XML);

    public static RdfFormat RDFJSON = new RdfFormat(Lang.RDFJSON,  RdfMediaType.APPLICATION_RDF_JSON);
    public static RdfFormat N3 = new RdfFormat(Lang.N3, RdfMediaType.TEXT_N3);
    public static RdfFormat NTRIPLES = new RdfFormat(Lang.NTRIPLES, RdfMediaType.APPLICATION_N_TRIPLES);
    public static RdfFormat NQUADS = new RdfFormat(Lang.NQUADS, RdfMediaType.APPLICATION_N_QUADS);
    public static RdfFormat TRIG = new RdfFormat(Lang.TRIG, RdfMediaType.TEXT_TRIG);
    public static RdfFormat TRIX = new RdfFormat(Lang.TRIX, RdfMediaType.TEXT_TRIX);
    public static RdfFormat JSONLD = new RdfFormat(Lang.JSONLD, RdfMediaType.APPLICATION_JSON_LD);
    public static RdfFormat TURTLE = new RdfFormat(Lang.TURTLE, RdfMediaType.TEXT_TURTLE);
    public static RdfFormat TTL = new RdfFormat(Lang.TTL, RdfMediaType.TEXT_TURTLE);

    // RDF binary - see https://jena.apache.org/documentation/io/rdf-binary.html
    public static RdfFormat RDFTHRIFT = new RdfFormat(Lang.RDFTHRIFT,  RdfMediaType.APPLICATION_THRIFT);

    public static RdfFormat OWL = new RdfFormat("OWL", RdfMediaType.APPLICATION_XML, ImmutableList.of("owl"));
    public static RdfFormat VOWL = new RdfFormat("VOWL", RdfMediaType.APPLICATION_WEBVOWL, ImmutableList.of("json", "vowl"));

    public static Collection<RdfFormat> allValues = ImmutableList.of(RDFXML, RDFJSON, N3, NTRIPLES, NQUADS, TRIG, TRIX, JSONLD, TURTLE, RDFTHRIFT, VOWL);

    private MediaType contentType;
    private Lang jenaLang;

    protected RdfFormat(String langlabel, String mainContentType, List<String> fileExt) {
        super(langlabel, mainContentType, null, null, fileExt);
        this.jenaLang = (Lang)this;
    }

    protected RdfFormat(Lang jenaLang, MediaType contentType) {
        super(jenaLang.getLabel(), contentType.getType(), jenaLang.getAltContentTypes(), jenaLang.getAltContentTypes(), jenaLang.getFileExtensions());
        this.contentType = contentType;
        this.jenaLang = jenaLang;
    }

    protected RdfFormat(String langlabel, MediaType contentType, List<String> fileExt) {
        super(langlabel, contentType.getType(), ImmutableList.of(), ImmutableList.of(), fileExt);
    }

    public String toJenaFormat() {
        return getLabel();
    }

    public Lang toJenaLang() {
        return this.jenaLang;
    }

    public MediaType mineType() {
        return contentType;
    }

    public static Optional<RdfFormat> fromUserString(@Nullable String userFormat) {
        if (StringUtils.isBlank(userFormat)) return Optional.empty();

        switch(userFormat.toUpperCase()) {
            case "RDF/XML":
            case "RDF":
                return Optional.of(RDFXML);
            case "RDF/JSON":
            case "JSON":
                return Optional.of(RDFJSON);
            case "RDF/N3":
            case "N3":
                return Optional.of(N3);
            case "NT":
            case "NTRIPLES":
            case "N-TRIPLES":
                return Optional.of(NTRIPLES);
            case "NQ":
            case "NQUADS":
            case "N-QUADS":
                return Optional.of(NQUADS);
            case "TRIG":
                return Optional.of(TRIG);
            case "TRIX":
                return Optional.of(TRIX);
            case "JSONLD":
            case "JSON-LD":
                return Optional.of(JSONLD);
            case "TTL":
            case "TURTLE":
                return Optional.of(TURTLE);
            case "OWL":
                return Optional.of(OWL);
            case "THRIFT":
            case "RT":
            case "TRDF":
                return Optional.of(RDFTHRIFT);
            case "VOWL":
                return Optional.of(VOWL);
        }
        return Optional.empty();
    }

    public static Optional<RdfFormat> fromContentType(String contentType) throws IllegalArgumentException{

        switch (contentType.toLowerCase()) {
            case RdfMediaType.APPLICATION_RDF_XML_VALUE:
            case RdfMediaType.APPLICATION_XML_VALUE:
            case RdfMediaType.TEXT_XML_VALUE:
                return Optional.of(RDFXML);
            case RdfMediaType.APPLICATION_RDF_JSON_VALUE:
            case RdfMediaType.APPLICATION_JSON_VALUE:
            case RdfMediaType.APPLICATION_JSON_UTF8_VALUE:
                return Optional.of(RDFJSON);
            case RdfMediaType.APPLICATION_JSON_LD_VALUE:
                return Optional.of(JSONLD);
            case RdfMediaType.APPLICATION_TURTLE_VALUE:
            case RdfMediaType.TEXT_TURTLE_VALUE:
            case "text/ttl":
                return Optional.of(TURTLE);
            case RdfMediaType.APPLICATION_TRIX_VALUE:
            case RdfMediaType.TEXT_TRIX_VALUE:
                return Optional.of(TRIX);
            case RdfMediaType.APPLICATION_TRIG_VALUE:
            case RdfMediaType.TEXT_TRIG_VALUE:
                return Optional.of(TRIG);
            case RdfMediaType.TEXT_N3_VALUE:
            case "text/n3":
                return Optional.of(N3);
            case RdfMediaType.APPLICATION_N_TRIPLES_VALUE:
            case "text/n-triples":
                return Optional.of(NTRIPLES);
            case RdfMediaType.APPLICATION_N_QUADS_VALUE:
            case "text/n-quads":
                return Optional.of(NQUADS);
            case RdfMediaType.APPLICATION_THRIFT_VALUE:
            case RdfMediaType.APPLICATION_RDF_THRIFT_VALUE:
            case "application/rdf+x-thrift":
            case "application/vnd.apache.thrift.binary": // See https://stackoverflow.com/questions/4844482/is-there-a-commonly-used-mime-type-for-thrift
                return Optional.of(RDFTHRIFT);
            case "application/xml+owl":
                return Optional.of(OWL);
            case RdfMediaType.APPLICATION_WEBVOWL_VALUE:
            case "application/vowl":
            case "text/vowl":
                return Optional.of(VOWL);
        }

        return Optional.empty();

    }

    public static Optional<RdfFormat> fromUrlExtension(String url) {
        int extIndex = url.lastIndexOf('.');
        if (extIndex > url.lastIndexOf('/') && extIndex < url.length() - 1) {
            String extension = url.substring(extIndex+1).toLowerCase();
            return fromExtension(extension);
        }

        return Optional.empty();
    }

    /**
     * See https://jena.apache.org/documentation/io/index.html
     */
    public static Optional<RdfFormat> fromExtension(String extension) {
        Preconditions.checkNotNull(extension);
        switch (extension.toLowerCase()) {
            case "rdf":
            case "xml":
                return Optional.of(RDFXML);
            case "json":
            case "yml":
            case "rj":
                return Optional.of(RDFJSON);
            case "jsonld":
            case "json-ld":
                return Optional.of(JSONLD);
            case "ttl":
            case "turtle":
                return Optional.of(TURTLE);
            case "trix":
                return Optional.of(TRIX);
            case "trig":
                return Optional.of(TRIG);
            case "n3":
                return Optional.of(N3);
            case "nt":
            case "ntriples":
            case "n-triples":
                return Optional.of(NTRIPLES);
            case "nq":
            case "nquad":
            case "nquads":
            case "n-quads":
                return Optional.of(NQUADS);
            case "owl":
                return Optional.of(OWL);
            case "thrift":
            case "trdf":
            case "rt":
                return Optional.of(RDFTHRIFT);
            case "vowl":
                return Optional.of(VOWL);
        }

        return Optional.empty();
    }

    public static RdfFormat[] values() {
        return allValues.toArray(new RdfFormat[allValues.size()]);
    }
}
