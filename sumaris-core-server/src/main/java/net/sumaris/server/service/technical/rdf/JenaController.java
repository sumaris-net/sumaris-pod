package net.sumaris.server.service.technical.rdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.taxon.*;
import net.sumaris.core.model.referential.transcribing.TranscribingItem;
import net.sumaris.core.model.referential.transcribing.TranscribingItemType;
import net.sumaris.core.model.referential.transcribing.TranscribingSystem;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfxml.xmlinput.JenaReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@RestController
@RequestMapping(value = "/jena")
public class JenaController implements Jpa2OwlConverter {
    private static final Logger LOG = LogManager.getLogger();


    private final static String IFREMER_URL = "http://ifremer.com/";


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaxonNameService refService;

    @Autowired
    private EntityManager entityManager;


    private OntModel ontology;

    @GetMapping(value = "/ontology", produces = {"application/x-javascript", "application/json", "application/ld+json"})
    @ResponseBody
    public String getOntology() {
        LOG.info("/ontology");
        return doWrite(ontology, "RDF/XML");
    }


    public EntityManager getEntityManager() {
        return entityManager;
    }

    @PostConstruct
    public void buildOntology() {

        Reflections reflections = Reflections.collect();

        ontology = ontModelWithMetadata();
        Map<OntClass, List<OntClass>> mutualyDisjoint = new HashMap<>();
        reflections.getTypesAnnotatedWith(Entity.class)
        //Stream.of(TaxonName.class, TaxonomicLevel.class, TaxonGroup.class, ReferenceTaxon.class, Status.class, TaxonGroupType.class)
                .forEach(ent -> {
                    buildOwlClass(ontology, ent, mutualyDisjoint);
                });

        // add mutually disjoint classes
        mutualyDisjoint.entrySet().stream()
                .filter(e -> e.getValue().size() > 1) // having more than one child
                .forEach(e -> {
                    List<OntClass> list = e.getValue();
                    for (int i = 0; i < list.size(); i++) {
                        OntClass r1 = list.get(i);
                        for (int j = i + 1; j < list.size(); j++) {
                            OntClass r2 = list.get(j);
                            LOG.info("setting disjoint " + i + " " + j + " " + r1 + " " + r2);
                            r1.addDisjointWith(r2);
                        }
                    }
                });


    }


    public OntModel loadDataModel() {
        return loadDataModel(null);
    }

    public OntModel loadDataModel(String query) {
        // load some data into the model
        OntModel model = ontModelWithMetadata();
        Map<OntClass, List<OntClass>> mutualyDisjoint = new HashMap<>();

        if (query == null) {
            query = "select tn from TaxonName as tn" + //  new net.sumaris.server.service.technical.Wrapper(tn, tl, rt)
                    " left join fetch tn.taxonomicLevel as tl" +
                    " left join fetch tn.referenceTaxon as rt"
            //        +" left join fetch tn.status as st"
            ;

            Stream.of(TaxonName.class, TaxonomicLevel.class, TaxonGroup.class, ReferenceTaxon.class, Status.class, TaxonGroupType.class,
                    TranscribingItem.class, TranscribingItemType.class, TranscribingSystem.class, Gear.class,
                    PmfmStrategy.class)
                    .forEach(ent -> {
                        buildOwlClass(model, ent, mutualyDisjoint);
                    });
            LOG.info("added sub ontology ");
        } else {
            // FIXME: parse queries, take Class Names and create ontology based on it

        }


        entityManager
                .createQuery("from Status")
                .getResultList()
                .forEach(status -> toModel(model, status, 2));


        entityManager
                .createQuery("from Gear")
                .getResultList()
                .forEach(ti -> {
                    //LOG.info("TI ============= ");
                    toModel(model, ti, 2);
                });


        entityManager
                .createQuery(query)
                .setMaxResults(100)
                .getResultList()
                .forEach(tn -> toModel(model, tn, 2));

        LOG.info("entityManager request Executed, finishing  ");
        return model;
    }


    String delta(long nanoStart) {
        long elapsedTime = System.nanoTime() - nanoStart;
        double seconds = (double) elapsedTime / 1_000_000_000.0;
        return " elapsed " + seconds;
    }

    @GetMapping(value = "/sync", produces = {"application/xml", "application/rdf+xml"})
    public @ResponseBody
    String kick() {

        long start = System.nanoTime();

        OntModel m = ModelFactory.createOntologyModel();
        new JenaReader().read(m, "http://localhost:8081/jena/rdf");
        LOG.info("Found " + m.size() + " triples remotely, reconstructing model now " + delta(start));

        List<? extends Object> recomposed = fromModel(m);
        LOG.info("Recomposed list of " + recomposed.size() + " Object, Making it OntClass again " + delta(start));

        Map<OntClass, List<OntClass>> mutualyDisjoint = new HashMap<>();

        OntModel m2 = ontModelWithMetadata();

        Stream.of(TaxonName.class, TaxonomicLevel.class, TaxonGroup.class, ReferenceTaxon.class, Status.class, TaxonGroupType.class,
                TranscribingItem.class, TranscribingItemType.class, TranscribingSystem.class, Gear.class,
                PmfmStrategy.class)
                .forEach(r -> {
                    buildOwlClass(m2, r, mutualyDisjoint);
                });
        recomposed.forEach(r -> toModel(m2, r, 2));

        LOG.info("Recomposed list of Object is " + m2.size() + " triple  " + delta(start) + " - " + (100.0 * m2.size() / m.size()) + "%");

        if (m.size() == m2.size()) {
            LOG.info(" QUANTITATIVE SUCCESSS   ");
            if (m.isIsomorphicWith(m2)) {
                LOG.info(" ISOMORPHIC SUCCESS " + delta(start));

            }
        } else {

            AtomicInteger ii = new AtomicInteger(0);
//            m.listStatements().toList().forEach(s -> {
//                if (!m2.contains(s)) {
//                    LOG.warn("statement not recomposed " +  ii.getAndIncrement() + " " + s);
//                }
//            });

        }


        return doWrite(m2, null);
    }

    /*
     * ************* Service Methods *************
     */
    @GetMapping(value = "/json", produces = {"application/x-javascript", "application/json", "application/ld+json"})
    public @ResponseBody
    String getModelAsJson() {
        return doWrite(loadDataModel(), "JSON-LD");
    }


    @GetMapping(value = {"/xml", "/rdf"}, produces = {"application/xml", "application/rdf+xml"})
    @ResponseBody
    public String getModelAsXml() {
        long start = System.nanoTime();

        OntModel m = loadDataModel();

        LOG.info("Recomposed list of Object is " + m.size() + " triple  " + delta(start));


        return doWrite(m, null);
    }

    @GetMapping(value = "/query/{q}",
            produces = {"application/x-javascript", "application/json", "application/ld+json"})
    @ResponseBody
    public String request(@PathVariable String q) {

        long start = System.nanoTime();
        LOG.info("GET /query/{q} : " + q);

        String query = null;

        switch (q) {
            case "taxonname":
                query = "select tn from TaxonName as tn" +
                        " left join fetch tn.taxonomicLevel as tl" +
                        " join fetch tn.referenceTaxon as rt";
                break;
            case "gears":
                query = "select g from Gears g";
                break;
            case "location":
                query = "select l from Location ";
                break;
            default:
                query = null;
        }

        OntModel m = loadDataModel(query);

        LOG.info("Recomposed list of Object is " + m.size() + " triple  " + delta(start));
        return doWrite(m, null);
    }

    @PostMapping(value = "/openquery2",
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String openquery(@RequestBody Map<String, String> request, HttpServletRequest rawRequest) {

        System.out.println("posted /opendata : " + request.size() + " " + rawRequest.getHeader("query"));
        request.forEach((k, v) -> {
            System.out.println("posted request : " + k + " => " + v);
        });


        return doWrite(loadDataModel(rawRequest.getHeader("query")), "JSONLD");
    }


    @GetMapping(value = "/ntriple", produces = {"application/n-triples", "text/n-triples"})
    @ResponseBody
    public String getModelAsNTriples() {
        return doWrite(loadDataModel(), "N-TRIPLE");
    }

    @GetMapping(value = "/ttl", produces = {"text/turtle"})
    @ResponseBody
    public String getModelAsTurtle() {
        return doWrite(loadDataModel(), "TURTLE");
    }


    @GetMapping(value = "/n3", produces = {"text/n3"})
    @ResponseBody
    public String getModelAsN3() {
        return doWrite(loadDataModel(), "N3");
    }


    @GetMapping(value = "/rdfjson", produces = {"application/x-javascript", "application/json", "application/ld+json"})
    @ResponseBody
    public String getModelAsrdfjson() {
        return doWrite(loadDataModel(), "RDF/JSON");
    }


    @GetMapping(value = "/thrift", produces = {"application/octet-stream"})
    @ResponseBody
    public String getModelAsBinary() {
        return doWrite(loadDataModel(), "RDF Binary");
    }

    @GetMapping(value = "/trig", produces = {"text/turtle"})
    @ResponseBody
    public String getModelAsTrig() {
        return doWrite(loadDataModel(), "TriG");
    }

    @GetMapping(value = "/trix", produces = {"text/turtle"})
    @ResponseBody
    public String getModelAsTrix() {
        return doWrite(loadDataModel(), "TriX");
    }
}