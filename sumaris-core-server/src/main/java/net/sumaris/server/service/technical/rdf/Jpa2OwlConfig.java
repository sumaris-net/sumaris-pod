package net.sumaris.server.service.technical.rdf;

import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.taxon.TaxonName;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.apache.jena.vocabulary.RDF;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public interface Jpa2OwlConfig {

    int MAX_DEPTH = 4;

    String LABEL = "A first representation of the model";
    String TITLE = "SUMARiS";

    String SCHEMA_URL = "http://www.e-is.pro/2019/03/schema/";

    List<Method> ALLOWED_MANY_TO_ONE = new ArrayList<>();

    List<Method> BLACKLIST = new ArrayList<>();


    List<Function> PROCESSORS = new ArrayList<>();


    @PostConstruct
    default void init(){
        ALLOWED_MANY_TO_ONE .addAll(Arrays.asList(
                getterOfField(TaxonName.class, TaxonName.PROPERTY_TAXONOMIC_LEVEL),
                getterOfField(TaxonName.class, TaxonName.PROPERTY_REFERENCE_TAXON),
                getterOfField(PmfmStrategy.class, PmfmStrategy.PROPERTY_STRATEGY),
                getterOfField(PmfmStrategy.class, PmfmStrategy.PROPERTY_ACQUISITION_LEVEL)
        ));

         BLACKLIST.addAll(Arrays.asList(
                 getterOfField(Gear.class, Gear.PROPERTY_STRATEGIES),
                 getterOfField(Gear.class, Gear.PROPERTY_CHILDREN)
         ));

        PROCESSORS.addAll(Arrays.asList(
                (x)-> "",
                (y)-> ""
        ));
    }

    Method getterOfField(Class t, String field);

//    default Model dataModelWithMetadata(){
//        Model model = ModelFactory.createDefaultModel();
//        model.setNsPrefix("this", SCHEMA_URL);
//        model.setNsPrefix("foaf", FOAF.getURI());
//
//        Resource benj = model.createResource(FOAF.Person);
//        Resource beno = model.createResource(FOAF.Person);
//        Resource peck = model.createResource(FOAF.Person);
//
//        Resource org = model.createResource(FOAF.Organization)
//                .addProperty(FOAF.homepage, SCHEMA_URL)
//                .addProperty(FOAF.name, "#E-IS");
//
//        benj.addProperty(FOAF.lastName, "BERTRAND")
//                .addProperty(FOAF.firstName, "Benjamin")
//                .addProperty(FOAF.knows, beno)
//                .addProperty(FOAF.knows, peck)
//                .addProperty(FOAF.member, org);
//
//        beno.addProperty(FOAF.lastName, "Lavenier")
//                .addProperty(FOAF.firstName, "Benoit")
//                .addProperty(FOAF.knows, benj)
//                .addProperty(FOAF.knows, peck)
//                .addProperty(FOAF.member, org);
//
//        beno.addProperty(FOAF.lastName, "Peck")
//                .addProperty(FOAF.firstName, "Ludovic")
//                .addProperty(FOAF.knows, benj)
//                .addProperty(FOAF.knows, beno)
//                .addProperty(FOAF.member, org);
//
//
//        return model;
//    }

    default OntModel ontModelWithMetadata(){
        OntModel  ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        ontology.setNsPrefix("this", SCHEMA_URL);
        ontology.setNsPrefix("foaf", FOAF.getURI());

        Resource author = ontology.createResource(FOAF.Person)
                .addProperty(FOAF.lastName, "BERTRAND")
                .addProperty(FOAF.firstName, "Benjamin");

        Resource schema = ontology.createResource(SCHEMA_URL)
                .addProperty(RDF.type, OWL.Ontology.asResource())
                .addProperty(OWL2.versionInfo, "1.1.0")
                .addProperty(OWL2.versionIRI, SCHEMA_URL)
                .addProperty(RDFS.label, TITLE)
                .addProperty(RDFS.comment, LABEL)
                .addProperty(DC.creator, "BERTRAND Benjamin")
                .addProperty(DC.creator, "LAVENIER Benoit")
                .addProperty(DC.creator, "PECQUOT Ludovic")
                .addProperty(DC.title, TITLE)
                .addProperty(DC.date, "2019-03-05")
                .addProperty(DC.rights, "http://www.gnu.org/licenses/gpl-3.0.html")
                .addProperty(DC.description, LABEL);
        return ontology;

    }
}
