package net.sumaris.server.http.rest;

import com.google.common.base.Preconditions;
import org.springframework.http.MediaType;

public enum RdfFormat {

    RDF("RDF/XML", RdfMediaType.APPLICATION_RDF_XML),
    JSON("RDF/JSON", MediaType.APPLICATION_JSON),
    N3("N3", RdfMediaType.APPLICATION_N_TRIPLES),
    TRIG("TriG", RdfMediaType.TEXT_TRIG),
    TRIX("TriX", RdfMediaType.TEXT_TRIX),
    JSONLD("JSON-LD", RdfMediaType.APPLICATION_JSON_LD),
    TTL("TURTLE", RdfMediaType.TEXT_TURTLE),

    VOWL("VOWL", RdfMediaType.APPLICATION_WEBVOWL);

    private String jenaFormat;
    private MediaType contentType;

    RdfFormat(String jenaFormat, MediaType contentType) {
        this.jenaFormat = jenaFormat;
        this.contentType = contentType;
    }

    public String toJenaFormat() {
        return jenaFormat;
    }

    public MediaType mineType() {
        return contentType;
    }

    public static RdfFormat fromUserString(String userFormat) {
        Preconditions.checkNotNull(userFormat);

        switch(userFormat.toUpperCase()) {
            case "RDF":
                return RDF;
            case "JSON":
                return JSON;
            case "N3":
            case "NTRIPLE":
            case "N-TRIPLE":
                return N3;
            case "TRIG":
                return TRIG;
            case "TRIX":
                return TRIX;
            case "JSONLD":
            case "JSON-LD":
                return JSONLD;
            case "TTL":
                return TTL;
            case "VOWL":
                return VOWL;
            default:
                throw new IllegalArgumentException("Unknown format: " + userFormat);
        }
    }

    public static RdfFormat fromContentType(String contentType) {

        switch (contentType.toLowerCase()) {
            case RdfMediaType.APPLICATION_RDF_XML_VALUE:
            case RdfMediaType.APPLICATION_XML_VALUE:
            case RdfMediaType.TEXT_XML_VALUE:
                return RDF;
            case RdfMediaType.APPLICATION_JSON_LD_VALUE:
                return JSONLD;
            case RdfMediaType.TEXT_TURTLE_VALUE:
            case "application/ttl":
            case "text/ttl":
                return TTL;
            case RdfMediaType.TEXT_TRIX_VALUE:
            case "application/trix":
                return TRIX;
            case RdfMediaType.TEXT_TRIG_VALUE:
            case "application/trig":
                return TRIG;
            case RdfMediaType.APPLICATION_WEBVOWL_VALUE:
            case "application/vowl":
            case "text/vowl":
                return VOWL;
            case RdfMediaType.APPLICATION_N_TRIPLES_VALUE:
            case "text/n-triples":
            case "text/n3":
                return N3;
            default:
                throw new IllegalArgumentException("Unknown format: " + contentType);
        }

    }
}
