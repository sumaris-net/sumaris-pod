package net.sumaris.server.config;

import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

public interface Jpa2OwlConfig {

    Logger LOG = LogManager.getLogger();


    int MAX_DEPTH = 4;

    String LABEL = "A first representation of the model";
    String TITLE = "SUMARiS";

    String SCHEMA_URL = "http://www.e-is.pro/2019/03/schema/";
    String ADAGIO_URL = "http://www.e-is.pro/2019/03/adagio/";

    List<Method> ALLOWED_MANY_TO_ONE = new ArrayList<>();

    List<Method> BLACKLIST = new ArrayList<>();
    Map<String, Class> URI_2_CLASS = new HashMap<>();
    Map<String, Function<OntResource, Object>> ARBITRARY_MAPPER = new HashMap<>();

    List<Function> PROCESSORS = new ArrayList<>();
    Map<String, Object> URI_2_OBJ_REF = new HashMap<>();

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");


    List getCacheStatus();
    EntityManager getEntityManager();

    List getCacheTL();

    @PostConstruct
    default void init() {
        ALLOWED_MANY_TO_ONE.addAll(Arrays.asList(
                getterOfField(TaxonName.class, TaxonName.PROPERTY_TAXONOMIC_LEVEL),
                getterOfField(TaxonName.class, TaxonName.PROPERTY_REFERENCE_TAXON),
                getterOfField(PmfmStrategy.class, PmfmStrategy.PROPERTY_STRATEGY),
                getterOfField(PmfmStrategy.class, PmfmStrategy.PROPERTY_ACQUISITION_LEVEL)
        ));

        BLACKLIST.addAll(Arrays.asList(
                getterOfField(Gear.class, Gear.PROPERTY_STRATEGIES),
                getterOfField(Gear.class, Gear.PROPERTY_CHILDREN)
        ));


        URI_2_CLASS.put("http://www.e-is.pro/2019/03/schema/net.sumaris.core.model.referential.gear.Gear", Gear.class);
        URI_2_CLASS.put("http://www.e-is.pro/2019/03/schema/net.sumaris.core.model.referential.taxon.ReferenceTaxon", ReferenceTaxon.class);
        URI_2_CLASS.put("http://www.e-is.pro/2019/03/schema/net.sumaris.core.model.referential.taxon.TaxonName", TaxonName.class);
        URI_2_CLASS.put("http://www.e-is.pro/2019/03/schema/net.sumaris.core.model.referential.taxon.TaxonomicLevel", TaxonomicLevel.class);
        URI_2_CLASS.put("http://www.e-is.pro/2019/03/schema/net.sumaris.core.model.referential.Status", Status.class);
        URI_2_CLASS.put("http://www.e-is.pro/2019/03/schema/net.sumaris.core.dao.technical.model.IUpdateDateEntityBean", IUpdateDateEntityBean.class);

        URI_2_CLASS.put("http://www.e-is.pro/2019/03/adagio/net.sumaris.core.model.referential.gear.Gear", Gear.class);
        URI_2_CLASS.put("http://www.e-is.pro/2019/03/adagio/net.sumaris.core.model.referential.taxon.ReferenceTaxon", ReferenceTaxon.class);
        URI_2_CLASS.put("http://www.e-is.pro/2019/03/adagio/net.sumaris.core.model.referential.taxon.TaxonName", TaxonName.class);
        URI_2_CLASS.put("http://www.e-is.pro/2019/03/adagio/net.sumaris.core.model.referential.taxon.TaxonomicLevel", TaxonomicLevel.class);
        URI_2_CLASS.put("http://www.e-is.pro/2019/03/adagio/net.sumaris.core.model.referential.Status", Status.class);
        URI_2_CLASS.put("http://www.e-is.pro/2019/03/adagio/net.sumaris.core.dao.technical.model.IUpdateDateEntityBean", IUpdateDateEntityBean.class);


        ARBITRARY_MAPPER.put(ADAGIO_URL + "net.sumaris.core.model.referential.taxon.TaxonomicLevel", ontResource -> {

            String clName = ADAGIO_URL + TaxonomicLevel.class.getTypeName();
            TaxonomicLevel tl = new TaxonomicLevel();

            try {
                // first try to get it from cache
                Property propCode = ontResource.getModel().getProperty(clName + "#Code");
                String label = ontResource.asIndividual().getPropertyValue(propCode).toString();

                int max = -1;
                for (Object ctl : getCacheTL()) {
                    if (((TaxonomicLevel) ctl).getLabel().equals(label)) {

                        return  URI_2_OBJ_REF.getOrDefault(ontResource.getURI(),ctl);
                    } else {
                        max = Math.max(max, ((TaxonomicLevel) ctl).getId());
                    }
                }


                // not in cache, create a new object

                Property name = ontResource.getModel().getProperty(clName + "#Name");
                tl.setName(ontResource
                        .asIndividual()
                        .getProperty(name)
                        .getObject()
                        .asLiteral()
                        .getString());

                Property cd = ontResource.getModel().getProperty(clName + "#CreationDate");
                tl.setCreationDate(sdf.parse(ontResource.asIndividual().getPropertyValue(cd).asLiteral().getString()));

                Property order = ontResource.getModel().getProperty(clName + "#RankOrder");
                tl.setRankOrder(ontResource.asIndividual().getPropertyValue(order).asLiteral().getInt());


                tl.setStatus(getEntityManager().getReference(Status.class,1));
                tl.setLabel(label);

                return getEntityManager().merge(tl);

            } catch (Exception e) {
                LOG.error("Arbitrary Mapper error " + ontResource + " - " + tl, e);
            }

            return tl;
        });

        ARBITRARY_MAPPER.put(ADAGIO_URL + "net.sumaris.core.model.referential.Status", ontResource -> {
            String clName = ADAGIO_URL + Status.class.getTypeName();
            Status st = new Status();

            try {

// first try to get it from cache
                Property propCode = ontResource.getModel().getProperty(clName + "#Code");
                Integer id = Integer.parseInt(ontResource.asIndividual().getPropertyValue(propCode).toString());

                int max = -1;
                for (Object ctl : getCacheStatus()) {
                    if (((Status) ctl).getId().equals(id)) {
                        return ctl;
                    } else {
                        max = Math.max(max, ((Status) ctl).getId());
                    }
                }

                Property name = ontResource.getModel().getProperty(clName + "#Name");
                st.setLabel(ontResource
                        .asIndividual()
                        .getProperty(name)
                        .getObject()
                        .asLiteral()
                        .getString());

                Property cd = ontResource.getModel().getProperty(clName + "#UpdateDate");
                st.setUpdateDate(sdf.parse(ontResource.asIndividual().getPropertyValue(cd).asLiteral().getString()));

                st.setId(max + 1);

                st.setName("DEFAULT GENERATED VALUE");
            } catch (Exception e) {
                LOG.error("Arbitrary Mapper error " + ontResource + " - " + st, e);
            }

            return st;
        });

        PROCESSORS.addAll(Arrays.asList(
                (x) -> "",
                (y) -> ""
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

    default OntModel ontModelWithMetadata() {
        OntModel ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
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
