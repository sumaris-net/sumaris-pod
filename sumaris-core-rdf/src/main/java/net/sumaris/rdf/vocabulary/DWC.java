package net.sumaris.rdf.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class DWC {
    private static final Model m_model = ModelFactory.createDefaultModel();
    public static final String NS = "http://rs.tdwg.org/dwc/terms/";
    public static final Resource NAMESPACE;
    public static final Property title;
    public static final Property creator;
    public static final Property subject;
    public static final Property description;
    public static final Property publisher;
    public static final Property contributor;
    public static final Property date;
    public static final Property type;
    public static final Property format;
    public static final Property identifier;
    public static final Property source;
    public static final Property language;
    public static final Property relation;
    public static final Property coverage;
    public static final Property rights;

    public DWC() {
    }

    public static String getURI() {
        return "http://purl.org/dc/elements/1.1/";
    }

    static {
        NAMESPACE = m_model.createResource("http://purl.org/dc/elements/1.1/");
        title = m_model.createProperty("http://purl.org/dc/elements/1.1/title");
        creator = m_model.createProperty("http://purl.org/dc/elements/1.1/creator");
        subject = m_model.createProperty("http://purl.org/dc/elements/1.1/subject");
        description = m_model.createProperty("http://purl.org/dc/elements/1.1/description");
        publisher = m_model.createProperty("http://purl.org/dc/elements/1.1/publisher");
        contributor = m_model.createProperty("http://purl.org/dc/elements/1.1/contributor");
        date = m_model.createProperty("http://purl.org/dc/elements/1.1/date");
        type = m_model.createProperty("http://purl.org/dc/elements/1.1/type");
        format = m_model.createProperty("http://purl.org/dc/elements/1.1/format");
        identifier = m_model.createProperty("http://purl.org/dc/elements/1.1/identifier");
        source = m_model.createProperty("http://purl.org/dc/elements/1.1/source");
        language = m_model.createProperty("http://purl.org/dc/elements/1.1/language");
        relation = m_model.createProperty("http://purl.org/dc/elements/1.1/relation");
        coverage = m_model.createProperty("http://purl.org/dc/elements/1.1/coverage");
        rights = m_model.createProperty("http://purl.org/dc/elements/1.1/rights");
    }
}
