package net.sumaris.server.service.technical.rdf;

import de.uni_stuttgart.vis.vowl.owl2vowl.Owl2Vowl;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@Transactional
public abstract class OpenDataController implements Synchro {

    private static final Logger LOG = LoggerFactory.getLogger(OpenDataController.class);


    public void initModule() {
        initConfig();
        new Reflections(BASE_SYNCHRONIZE_MODEL_PACKAGE, new SubTypesScanner(false))
                .getSubTypesOf(Object.class)
                .forEach(c -> URI_2_CLASS.putIfAbsent(c.getSimpleName(), c));
    }

    /*
     * ************* Service methods *************
     */
    @GetMapping(value = "/ntriple/{q}/{name}", produces = {"application/n-triples", "text/n-triples"})
    @ResponseBody
    public String getModelAsNTriples(@PathVariable String q, @PathVariable String name,
                                     @RequestParam(defaultValue = "false") String disjoints,
                                     @RequestParam(defaultValue = "false") String methods,
                                     @RequestParam(defaultValue = "false") String packages) {
        return Helpers.toString(execute(q, name, disjoints, methods, packages), "N-TRIPLE");
    }

    @GetMapping(value = "/ttl/{q}/{name}", produces = {"text/turtle"})
    @ResponseBody
    public String getModelAsTurtle(@PathVariable String q, @PathVariable String name,
                                   @RequestParam(defaultValue = "false") String disjoints,
                                   @RequestParam(defaultValue = "false") String methods,
                                   @RequestParam(defaultValue = "false") String packages) {
        return Helpers.toString(execute(q, name, disjoints, methods, packages), "TURTLE");
    }

    @GetMapping(value = "/n3/{q}/{name}", produces = {"text/n3", "application/text", "text/text"})
    @ResponseBody
    public String getModelAsN3(@PathVariable String q, @PathVariable String name,
                               @RequestParam(defaultValue = "false") String disjoints,
                               @RequestParam(defaultValue = "false") String methods,
                               @RequestParam(defaultValue = "false") String packages) {
        return Helpers.toString(execute(q, name, disjoints, methods, packages), "N3");
    }

    @GetMapping(value = "/json/{q}/{name}", produces = {"application/x-javascript", "application/json", "application/ld+json"})
    @ResponseBody
    public String getModelAsrdfjson(@PathVariable String q, @PathVariable String name,
                                    @RequestParam(defaultValue = "false") String disjoints,
                                    @RequestParam(defaultValue = "false") String methods,
                                    @RequestParam(defaultValue = "false") String packages) {
        return Helpers.toString(execute(q, name, disjoints, methods, packages), "RDF/JSON");
    }

    @GetMapping(value = "/trig/{q}/{name}", produces = {"text/trig"})
    @ResponseBody
    public String getModelAsTrig(@PathVariable String q, @PathVariable String name,
                                 @RequestParam(defaultValue = "false") String disjoints,
                                 @RequestParam(defaultValue = "false") String methods,
                                 @RequestParam(defaultValue = "false") String packages) {
        return Helpers.toString(execute(q, name, disjoints, methods, packages), "TriG");
    }

    @GetMapping(value = "/jsonld/{q}/{name}", produces = {"application/x-javascript", "application/json", "application/ld+json"})
    @ResponseBody
    public String getModelAsJson(@PathVariable String q, @PathVariable String name,
                                 @RequestParam(defaultValue = "false") String disjoints,
                                 @RequestParam(defaultValue = "false") String methods,
                                 @RequestParam(defaultValue = "false") String packages) {
        return Helpers.toString(execute(q, name, disjoints, methods, packages), "JSON-LD");
    }

    @GetMapping(value = "/rdf/{q}/{name}", produces = {"application/xml", "application/rdf+xml"})
    public String getModelAsXml(@PathVariable String q, @PathVariable String name,
                                @RequestParam(defaultValue = "false") String disjoints,
                                @RequestParam(defaultValue = "false") String methods,
                                @RequestParam(defaultValue = "false") String packages) {

        Model model = execute(q, name, disjoints, methods, packages);
        return Helpers.toString(model, "RDF/XML");
    }

    @GetMapping(value = "/trix/{q}/{name}", produces = {"text/trix"})
    @ResponseBody
    public String getModelAsTrix(@PathVariable String q, @PathVariable String name,
                                 @RequestParam(defaultValue = "false") String disjoints,
                                 @RequestParam(defaultValue = "false") String methods,
                                 @RequestParam(defaultValue = "false") String packages) {
        fillObjectWithStdAttribute(null,null,null);
        return Helpers.toString(execute(q, name, disjoints, methods, packages), "TriX");
    }

    @GetMapping(value = "/vowl/{q}/{name}", produces = {"application/x-javascript", "application/json", "application/ld+json"})
    @ResponseBody
    public String getModelAsVowl(@PathVariable String q, @PathVariable String name,
                                                @RequestParam(defaultValue = "false") String disjoints,
                                                @RequestParam(defaultValue = "false") String methods,
                                                @RequestParam(defaultValue = "false") String packages) {
        Model res =  execute(q, name, disjoints, methods, packages);
        try (
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                BufferedOutputStream bout = new BufferedOutputStream(bos);
        ) {
            res.write(bout, "N3");
            bout.flush();

            return new Owl2Vowl(new ByteArrayInputStream(bos.toByteArray())).getJsonAsString();
        } catch (Exception e) {
            LOG.warn("Error converting model into VOWL", e);
            throw new SumarisTechnicalException(e);
        }
    }

    // ===========  protected methods =============
    @Transactional
    protected Model execute(String q, String name, String disjoints, String methods, String packages) {
        LOG.info("executing /jena/{fmt}/{" + q + "}/{" + name + "}?disjoints=" + disjoints + "&methods=" + methods + "&packages=" + packages);
        Map<String, String> opts = new HashMap<>();
        opts.put("disjoints", disjoints);
        opts.put("methods", methods);
        opts.put("packages", packages);

        OntModel res = null;

        switch (q) {
            case "referentials":
                String query = NAMED_QUERIES.getOrDefault(name, "Status");
                res = ontOfData(MY_PREFIX + name,
                        getEntityManager().createQuery("from " + query)
                                .setMaxResults(20000)
                                .getResultList(),
                        opts);
                break;
            case "sync":
                res = overwriteFromRemote(
                        "http://localhost:8081/jena/rdf/referentials/" + name,
                        MY_PREFIX + "sync");
                break;
            case "ontologies":
            case "ontology":
                res = onto(name, opts);
        }
        return res;
    }

    protected abstract OntModel onto(String name, Map<String, String> opts);

    /**
     * Serialize model in requested format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a string representation of the model
     */
    protected StreamingResponseBody toStreamingResponseBody(Model model, String format) {

        return os -> {
            if (format == null) {
                model.write(os);
            } else {
                model.write(os, format);
            }
        };
    }

    /**
     * Serialize model in requested format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a string representation of the model
     */
    protected Callable<StreamingResponseBody> toCallableStreamingResponseBody(Model model, String format) {
        Callable<StreamingResponseBody> ret = () -> {
            return os -> {
                if (format == null) {
                    model.write(os);
                } else {
                    model.write(os, format);
                }
            };
        };


        return ret;
    }

}