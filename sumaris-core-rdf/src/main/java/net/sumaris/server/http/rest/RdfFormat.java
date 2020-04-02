package net.sumaris.server.http.rest;


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


    public static RdfFormat RDF = new RdfFormat(Lang.RDFXML, RdfMediaType.APPLICATION_RDF_XML);
    public static RdfFormat JSON = new RdfFormat(Lang.RDFJSON,  RdfMediaType.APPLICATION_RDF_JSON);
    public static RdfFormat N3 = new RdfFormat(Lang.N3, RdfMediaType.TEXT_N3);
    public static RdfFormat NTRIPLES = new RdfFormat(Lang.NTRIPLES, RdfMediaType.APPLICATION_N_TRIPLES);
    public static RdfFormat NQUADS = new RdfFormat(Lang.NQUADS, RdfMediaType.APPLICATION_N_QUADS);
    public static RdfFormat TRIG = new RdfFormat(Lang.TRIG, RdfMediaType.TEXT_TRIG);
    public static RdfFormat TRIX = new RdfFormat(Lang.TRIX, RdfMediaType.TEXT_TRIX);
    public static RdfFormat JSONLD = new RdfFormat(Lang.JSONLD, RdfMediaType.APPLICATION_JSON_LD);
    public static RdfFormat TURTLE = new RdfFormat( Lang.TURTLE, RdfMediaType.TEXT_TURTLE);

    // RDF binary - see https://jena.apache.org/documentation/io/rdf-binary.html
    public static RdfFormat RDFTHRIFT = new RdfFormat( Lang.RDFTHRIFT, RdfMediaType.TEXT_TURTLE);

    public static RdfFormat VOWL = new RdfFormat("VOWL", RdfMediaType.APPLICATION_WEBVOWL, ImmutableList.of("json", "vowl"));

    public static Collection<RdfFormat> allValues = ImmutableList.of(RDF, JSON, N3, NTRIPLES, NQUADS, TRIG, TRIX, JSONLD, TURTLE, RDFTHRIFT, VOWL);

    private MediaType contentType;

    protected RdfFormat(String langlabel, String mainContentType, List<String> fileExt) {
        super(langlabel, mainContentType, null, null, fileExt);
    }

    protected RdfFormat(Lang jenaLang, MediaType contentType) {
        super(jenaLang.getLabel(), contentType.getType(), jenaLang.getAltContentTypes(), jenaLang.getAltContentTypes(), jenaLang.getFileExtensions());
        this.contentType = contentType;
    }

    protected RdfFormat(String langlabel, MediaType contentType, List<String> fileExt) {
        super(langlabel, contentType.getType(), ImmutableList.of(), ImmutableList.of(), fileExt);
    }

    public String toJenaFormat() {
        return getLabel();
    }

    public Lang toJenaLang() {
        return (Lang)this;
    }

    public MediaType mineType() {
        return contentType;
    }

    public static Optional<RdfFormat> fromUserString(@Nullable String userFormat) {
        if (StringUtils.isBlank(userFormat)) return Optional.empty();

        switch(userFormat.toUpperCase()) {
            case "RDF/XML":
            case "RDF":
                return Optional.of(RDF);
            case "RDF/JSON":
            case "JSON":
                return Optional.of(JSON);
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
            case "THRIFT":
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
                return Optional.of(RDF);
            case RdfMediaType.APPLICATION_RDF_JSON_VALUE:
            case RdfMediaType.APPLICATION_JSON_VALUE:
            case RdfMediaType.APPLICATION_JSON_UTF8_VALUE:
                return Optional.of(JSON);
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
            case RdfMediaType.APPLICATION_WEBVOWL_VALUE:
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
            case "application/rdf+thrift":
            case "application/rdf+x-thrift":
            case "application/vnd.apache.thrift.binary": // See https://stackoverflow.com/questions/4844482/is-there-a-commonly-used-mime-type-for-thrift
                return Optional.of(RDFTHRIFT);
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
            switch (extension) {
                case "rdf":
                case "xml":
                    return Optional.of(RDF);
                case "json":
                case "yml":
                    return Optional.of(JSON);
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
                case "vowl":
                    return Optional.of(VOWL);
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
            }
        }

        return Optional.empty();
    }

    public static RdfFormat[] values() {
        return allValues.toArray(new RdfFormat[allValues.size()]);
    }
}
