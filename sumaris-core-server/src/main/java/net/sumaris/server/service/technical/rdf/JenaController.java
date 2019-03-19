package net.sumaris.server.service.technical.rdf;

import de.uni_stuttgart.vis.vowl.owl2vowl.Owl2Vowl;
import net.sumaris.core.dao.administration.programStrategy.ProgramDaoImpl;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import static net.sumaris.server.config.O2BConfig.NAMED_QUERIES;
import static net.sumaris.server.service.technical.rdf.Helpers.doWrite;

@RestController
@RequestMapping(value = "/jena")
public class JenaController {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(JenaController.class);


    private final static String IFREMER_URL = "http://ifremer.com/";

    @Autowired
    private EntityManager entityManager;

    private Synchro sync = new Synchro();


    private OntModel ontologyOfJPAEntities;

    private OntModel ontologyOfVOPackages;

    private OntModel ontologyOfModule;

    @PostConstruct
    public void buildOntologies() {
        sync.init();
        Map<String, String> opts = new HashMap<>();
        opts.put("disjoints", "false");
        opts.put("methods", "false");


        ontologyOfJPAEntities = sync.ontOfCapturedClasses(sync.MY_PREFIX + "entities",
                Reflections.collect().getTypesAnnotatedWith(Entity.class).stream(),
                opts);

        ontologyOfVOPackages = sync.ontOfPackage(sync.MY_PREFIX + "vo",
                "net.sumaris.core.vo",
                opts);


        opts.put("methods", "true");
        ontologyOfModule = sync.ontOfPackage(sync.MY_PREFIX + "module",
                "net.sumaris.server.service.technical.rdf",
                opts);


        LOG.info("Loaded Ontoglogies : " +
                " JPA => " + ontologyOfJPAEntities.size() +
                " VO => " + ontologyOfVOPackages.size()+
                " O2B => " + ontologyOfModule.size());

    }


    public OntModel onto(String name, Map<String, String> opts) {

        switch (name) {
            case "vo":
                return ontologyOfVOPackages;
            case "entities":
                return ontologyOfJPAEntities;
            case "module":
                return ontologyOfModule;
        }

        return sync.ontOfPackage(sync.MY_PREFIX + name,
                opts.getOrDefault("package", "net.sumaris.server.service.technical.rdf"),
                opts);
    }



    /*
     * ************* Service methods *************
     */


    @GetMapping(value = "/ntriple/{q}/{name}", produces = {"application/n-triples", "text/n-triples"})
    @ResponseBody
    public String getModelAsNTriples(@PathVariable String q, @PathVariable String name,
                                     @RequestParam(defaultValue = "false") String disjoints,
                                     @RequestParam(defaultValue = "false") String methods,
                                     @RequestParam(defaultValue = "false") String packaze) {
        return doWrite(execute(q, name, disjoints, methods, packaze), "N-TRIPLE");
    }

    @GetMapping(value = "/ttl/{q}/{name}", produces = {"text/turtle"})
    @ResponseBody
    public String getModelAsTurtle(@PathVariable String q, @PathVariable String name,
                                   @RequestParam(defaultValue = "false") String disjoints,
                                   @RequestParam(defaultValue = "false") String methods,
                                   @RequestParam(defaultValue = "false") String packaze) {
        return doWrite(execute(q, name, disjoints, methods, packaze), "TURTLE");
    }

    @GetMapping(value = "/n3/{q}/{name}", produces = {"text/n3", "application/text", "text/text"})
    @ResponseBody
    public String getModelAsN3(@PathVariable String q, @PathVariable String name,
                               @RequestParam(defaultValue = "false") String disjoints,
                               @RequestParam(defaultValue = "false") String methods,
                               @RequestParam(defaultValue = "false") String packaze) {
        return doWrite(execute(q, name, disjoints, methods, packaze), "N3");
    }

    @GetMapping(value = "/json/{q}/{name}", produces = {"application/x-javascript", "application/json", "application/ld+json"})
    @ResponseBody
    public String getModelAsrdfjson(@PathVariable String q, @PathVariable String name,
                                    @RequestParam(defaultValue = "false") String disjoints,
                                    @RequestParam(defaultValue = "false") String methods,
                                    @RequestParam(defaultValue = "false") String packaze) {
        return doWrite(execute(q, name, disjoints, methods, packaze), "RDF/JSON");
    }

    @GetMapping(value = "/trig/{q}/{name}", produces = {"text/trig"})
    @ResponseBody
    public String getModelAsTrig(@PathVariable String q, @PathVariable String name,
                                 @RequestParam(defaultValue = "false") String disjoints,
                                 @RequestParam(defaultValue = "false") String methods,
                                 @RequestParam(defaultValue = "false") String packaze) {
        return doWrite(execute(q, name, disjoints, methods, packaze), "TriG");
    }

    @GetMapping(value = "/jsonld/{q}/{name}", produces = {"application/x-javascript", "application/json", "application/ld+json"})
    public @ResponseBody
    String getModelAsJson(@PathVariable String q, @PathVariable String name,
                          @RequestParam(defaultValue = "false") String disjoints,
                          @RequestParam(defaultValue = "false") String methods,
                          @RequestParam(defaultValue = "false") String packaze) {
        return doWrite(execute(q, name, disjoints, methods, packaze), "JSON-LD");
    }

    @GetMapping(value = "/rdf/{q}/{name}", produces = {"application/xml", "application/rdf+xml"})
    @ResponseBody
    public String getModelAsXml(@PathVariable String q, @PathVariable String name,
                                @RequestParam(defaultValue = "false") String disjoints,
                                @RequestParam(defaultValue = "false") String methods,
                                @RequestParam(defaultValue = "false") String packaze) {
        return doWrite(execute(q, name, disjoints, methods, packaze), "RDF/XML");
    }

    @GetMapping(value = "/trix/{q}/{name}", produces = {"text/trix"})
    @ResponseBody
    public String getModelAsTrix(@PathVariable String q, @PathVariable String name,
                                 @RequestParam(defaultValue = "false") String disjoints,
                                 @RequestParam(defaultValue = "false") String methods,
                                 @RequestParam(defaultValue = "false") String packaze) {
        return doWrite(execute(q, name, disjoints, methods, packaze), "TriX");
    }


    // ===========  private methods =============

    private Model execute(String q, String name, String disjoints, String methods, String packaze) {
        LOG.info("executing /jena/{fmt}/{" + q + "}/{" + name + "}?disjoints=" + disjoints + "&methods=" + methods + "&package=" + packaze);
        Map<String, String> opts = new HashMap<>();
        opts.put("disjoints", disjoints);
        opts.put("methods", methods);

        OntModel res = null;

        switch (q) {
            case "referentials":
                String query = NAMED_QUERIES.getOrDefault(name, "from Status");
                res = sync.ontOfData(sync.MY_PREFIX + name,
                        entityManager.createQuery(query)
                                .setMaxResults(1000)
                                .getResultList(),
                        opts);
                break;
            case "sync":
                res = sync.overwriteFromRemote("http://localhost:8081" + "/jena/rdf/referentials/" + name, sync.MY_PREFIX + "sync");
                break;
            case "ontologies":
            case "ontology":
                res = onto(name, opts);
        }

        if (res == null)
            // report error to user
            return null;

        try {
            res.write(new FileOutputStream("/home/bbertran/ws/WebVOWL/deploy/data/" + name + ".owl"), "N3");
            Owl2Vowl o2v = new Owl2Vowl(new FileInputStream("/home/bbertran/ws/WebVOWL/deploy/data/" + name + ".owl"));
            o2v.writeToFile(new File("/home/bbertran/ws/WebVOWL/deploy/data/" + name + ".json"));

        } catch (Exception e) {
            LOG.warn ("error saving OWL and VOWL", e);
        }


        return res;
    }


}