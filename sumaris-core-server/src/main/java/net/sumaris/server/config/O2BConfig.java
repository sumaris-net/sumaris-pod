package net.sumaris.server.config;

import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import net.sumaris.server.service.technical.rdf.Bean2Owl;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

public interface O2BConfig {

    Logger LOG = LoggerFactory.getLogger(O2BConfig.class);


    int MAX_DEPTH = 4;

    String LABEL = "A first representation of the model";
    String TITLE = "SUMARiS";
    String[] AUTHORS = new String[]{
            "BERTRAND Benjamin",
            "LAVENIER Benoit",
            "PECQUOT Ludovic"};

    String LICENCE = "http://www.gnu.org/licenses/gpl-3.0.html";


    //String MY_PREFIX = "http://"+whatsMyIp().get()+"/jena/referentials/taxons/";
    String MY_PREFIX = "http://www.sumaris.net/2019/03/ontologies/";


    //String SCHEMA_URL = "http://www.e-is.pro/2019/03/schema/";
    String ADAGIO_PREFIX = "http://www.e-is.pro/2019/03/adagio/";

    List<Method> ALLOWED_MANY_TO_ONE = new ArrayList<>();

    List<Method> BLACKLIST = new ArrayList<>();
    Map<String, Class> URI_2_CLASS = new HashMap<>();
    Map<String, Function<OntResource, Object>> B2O_ARBITRARY_MAPPER = new HashMap<>();
    Map<String, Function<Object, OntResource>> O2B_ARBITRARY_MAPPER = new HashMap<>();

    Map<String, String> NAMED_QUERIES = new HashMap<>();

    Map<String, Object> URI_2_OBJ_REF = new HashMap<>();

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");


    EntityManager getEntityManager();

    default List getCacheStatus() {
        return getEntityManager()
                .createQuery("from Status")
                .getResultList();
    }

    default List getCacheTL() {
        return getEntityManager()
                .createQuery("from TaxonomicLevel")
                .getResultList();
    }

    static Optional<String> whatsMyIp() {

        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            return Optional.of(in.readLine());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @PostConstruct
    default void init() {
        try {
            ALLOWED_MANY_TO_ONE.addAll(Arrays.asList(
                    Bean2Owl.getterOfField(TaxonName.class, TaxonName.PROPERTY_TAXONOMIC_LEVEL),
                    Bean2Owl.getterOfField(TaxonName.class, TaxonName.PROPERTY_REFERENCE_TAXON),
                    Bean2Owl.getterOfField(TaxonName.class, TaxonName.PROPERTY_STATUS),
                    Bean2Owl.getterOfField(Location.class, Location.PROPERTY_STATUS),
                    Bean2Owl.getterOfField(Location.class, Location.PROPERTY_LOCATION_LEVEL),
                    Bean2Owl.getterOfField(PmfmStrategy.class, PmfmStrategy.PROPERTY_STRATEGY),
                    Bean2Owl.getterOfField(PmfmStrategy.class, PmfmStrategy.PROPERTY_ACQUISITION_LEVEL)
            ));
        } catch (Exception e) {
            LOG.error("Exception ", e);
        }

        BLACKLIST.addAll(Arrays.asList(
                Bean2Owl.getterOfField(Gear.class, Gear.PROPERTY_STRATEGIES),
                Bean2Owl.getterOfField(Gear.class, Gear.PROPERTY_CHILDREN)
        ));


        // ============== NAMED_QUERIES ==============
        NAMED_QUERIES.put("taxons", "select tn from TaxonName as tn" +
                " left join fetch tn.taxonomicLevel as tl" +
                " join fetch tn.referenceTaxon as rt" +
                " join fetch tn.status st" +
                " where tn.updateDate > '2015-01-01 23:59:50'");
        NAMED_QUERIES.put("transcriptions", "select ti from TranscribingItem ti" +
                " join fetch ti.type tit " +
                " join fetch ti.status st");
        NAMED_QUERIES.put("locations", "select l from Location l " +
                " join fetch l.locationLevel ll " +
                " join fetch l.validityStatus vs " +
                " join fetch l.status st");
        NAMED_QUERIES.put("gears", "select g from Gear g " +
                " join fetch g.gearLevel gl " +
                " join fetch g.strategies s " +
                " join fetch g.status st");


        URI_2_CLASS.put("Gear", Gear.class);
        URI_2_CLASS.put("ReferenceTaxon", ReferenceTaxon.class);
        URI_2_CLASS.put("TaxonName", TaxonName.class);
        URI_2_CLASS.put("TaxonomicLevel", TaxonomicLevel.class);
        URI_2_CLASS.put("Status", Status.class);
        URI_2_CLASS.put("IUpdateDateEntityBean", IUpdateDateEntityBean.class);

        O2B_ARBITRARY_MAPPER.put("uri", obj-> {




            return (OntResource)null;
        });

        B2O_ARBITRARY_MAPPER.put(ADAGIO_PREFIX + "net.sumaris.core.model.referential.taxon.TaxonomicLevel", ontResource -> {

            String clName = ADAGIO_PREFIX + TaxonomicLevel.class.getTypeName();
            TaxonomicLevel tl = (TaxonomicLevel) URI_2_OBJ_REF.get(ontResource.getURI());

            try {
                // first try to get it from cache
                Property propCode = ontResource.getModel().getProperty(clName + "#Code");
                String label = ontResource.asIndividual().getPropertyValue(propCode).toString();


                for (Object ctl : getCacheTL()) {
                    if (((TaxonomicLevel) ctl).getLabel().equals(label)) {
                        return URI_2_OBJ_REF.putIfAbsent(ontResource.getURI(), ctl);
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


                tl.setStatus(getEntityManager().getReference(Status.class, 1));
                tl.setLabel(label);

                getEntityManager().persist(tl);
                return tl;

            } catch (Exception e) {
                LOG.error("Arbitrary Mapper error " + ontResource + " - " + tl, e);
            }

            return tl;
        });

        B2O_ARBITRARY_MAPPER.put(ADAGIO_PREFIX + "net.sumaris.core.model.referential.Status", ontResource -> {
            String clName = ADAGIO_PREFIX + Status.class.getTypeName();
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


    }


}
