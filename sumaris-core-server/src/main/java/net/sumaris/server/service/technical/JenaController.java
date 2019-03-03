package net.sumaris.server.service.technical;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import net.sumaris.core.model.referential.taxon.TaxonomicLevelId;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.sumaris.server.service.technical.RDFHelpers.*;

@RestController
@RequestMapping(value = "/jena")
public class JenaController {
    private static final Logger LOG = LogManager.getLogger();

    private final static String SCHEMA_URL = "http://www.e-is.pro/2019/03/schema/";

    private static List<Class> ACCEPTED_LIST_CLASS = Arrays.asList(List.class, ArrayList.class);

    private static final List<Method> ALLOWED_MANY_TO_ONE = Arrays.asList(
            getterOfField(TaxonName.class, TaxonName.PROPERTY_TAXONOMIC_LEVEL),
            getterOfField(TaxonName.class, TaxonName.PROPERTY_REFERENCE_TAXON));


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaxonNameService refService;

    @Autowired
    private EntityManager entityManager;


    private static Resource toModel(Model model, Object clazz) {

        String namespace = SCHEMA_URL + clazz.getClass().getCanonicalName().replaceAll("\\.", "/");

        // try using the ID field if exists to represent the node
        AnonId node;
        try {
            node = new AnonId(clazz.getClass().getName() + "#" + clazz.getClass().getMethod("getId").invoke(clazz).toString());
            LOG.info("Created anonid " + node + " for object " + clazz.toString());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            node = new AnonId();
        }
        Resource resource = model.createResource(node);
        if (model.containsResource(resource)) {
            LOG.warn("Resource already exists, skipping " + resource.getId());
            return resource;
        }

        // Handle Methods
        Stream.of(clazz.getClass().getMethods())
                .filter(RDFHelpers::isGetter)
                .filter(met -> (!isManyToOne(met)) || ALLOWED_MANY_TO_ONE.contains(met))
                .forEach(met -> {

                    try {
                        Object invoked = met.invoke(clazz);
                        Property pred = model.createProperty(namespace, "#" + met.getName().replace("get", ""));

                        if (isId(met)) {
                            // ignore id field as we already used it to build the anonid
                        } else if ("getClass".equals(met.getName())) {
                            resource.addProperty(RDF.type, pred);
                        } else if (!isGenericJava(met)) {
                            LOG.info("not a generic object, recursing on " + invoked);
                            Resource recurse = toModel(model, invoked);
                            if (recurse != null)
                                resource.addProperty(pred, recurse);
                        } else if (met.getGenericReturnType() instanceof ParameterizedType) {

                            if (invoked != null) {
                                Resource anonId = model.createResource(new AnonId("params" + new Random().nextInt(1000000)));
                                //resource.addProperty( pred, anonId);

                                Optional<Resource> listNode = handleList(model, met.getGenericReturnType(), invoked, pred, anonId);
                                if (listNode.isPresent()) {
                                    LOG.info(" --and res  : " + listNode.get().getURI());
                                    resource.addProperty(pred, listNode.get());
                                }
                            }
                            //
                        } else {
                            if (invoked != null)
                                resource.addProperty(pred, invoked + "");
                        }

                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }

                });

        return resource;
    }


    static Optional<Resource> handleList(Model model, Type type, Object listObject, Property prop, Resource fieldId) {
        // handle ParameterizedType
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;// This would be Class<List>, say
            Type raw = parameterized.getRawType();
            Type own = parameterized.getOwnerType();
            Type[] typeArgs = parameterized.getActualTypeArguments();

            LOG.info("  - ParameterizedType  " +
                    raw.getTypeName() + " <" + typeArgs[0].getTypeName() + ">" +
                    // " - owner " +  own  +
                    " - going to cast  " + listObject);//+ Arrays.toString(typeArgs));

            if (ACCEPTED_LIST_CLASS.stream()
                    .anyMatch(x -> x.getCanonicalName().equals(raw.getTypeName()))) {

// Create a list containing the subjects of the role assignments in one go

                List<RDFNode> nodes = new ArrayList<>();
                List<? extends Object> asList = castListSafe((List<? extends Object>) listObject, Object.class);

                if (asList.isEmpty()) {
                    LOG.warn(" - empty list, ignoring ");
                    return Optional.empty();
                }
                for (Object x : asList) {
                    Resource listItem = toModel(model, x);
                    nodes.add(listItem);

                }

                RDFList list = model.createList(nodes.toArray(new RDFNode[nodes.size()]));

                LOG.info("  - rdflist " + list.size() + " : " + list);
//var tmp = model.createProperty("sdfsdfsdf"+new Random().nextInt(10000000));
                fieldId.addProperty(prop, list);
                //              fieldId.addProperty(tmp ,model.createList(list));
                return Optional.of(list);

            }

        }
        return Optional.empty();
    }

    public Model loadModel() {
        return loadModel(null);
    }

    public Model loadModel(String query) {
        // load some data into the model
        Model model = ModelFactory.createDefaultModel();

        model.add(
                model.createResource("urn:huhu.haha.subject.com"),
                model.createProperty("urn:huhu.haha.predicate.com"),
                "urn:huhu.haha.object.com"
        );

        if (query == null)
            query = "select tn from TaxonName as tn" +
                    " left join fetch tn.taxonomicLevel as tl" +
                    " join fetch tn.referenceTaxon as rt";


        entityManager
                .createQuery(query)
                .getResultList()
                .forEach(tn -> toModel(model, tn));


        return model;
    }


    private List<TaxonName> getAll(boolean withSynonyms) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TaxonName> query = builder.createQuery(TaxonName.class);
        Root<TaxonName> root = query.from(TaxonName.class);

        ParameterExpression<Boolean> withSynonymParam = builder.parameter(Boolean.class);

        query.select(root)
                .where(builder.and(
                        // Filter on taxonomic level (species+ subspecies)
                        builder.in(root.get(TaxonName.PROPERTY_TAXONOMIC_LEVEL).get(TaxonomicLevel.PROPERTY_ID))
                                .value(ImmutableList.of(TaxonomicLevelId.SPECIES.getId(), TaxonomicLevelId.SUBSPECIES.getId())),
                        // Filter on is_referent
                        builder.or(
                                builder.isNull(withSynonymParam),
                                builder.equal(root.get(TaxonName.PROPERTY_IS_REFERENT), Boolean.TRUE)
                        )
                ));

        TypedQuery<TaxonName> q = entityManager.createQuery(query)
                .setParameter(withSynonymParam, Boolean.valueOf(withSynonyms));
        return q.getResultList();
    }


    /*
     * ************* Service Methods *************
     */
    @GetMapping(value = "/json", produces = {"application/x-javascript", "application/json", "application/ld+json"})
    public @ResponseBody
    String getModelAsJson() {
        return doWrite(loadModel(), "JSON-LD");
    }


    @GetMapping(value = {"/xml", "/rdf"}, produces = {"application/xml", "application/rdf+xml"})
    @ResponseBody
    public String getModelAsXml() {
        return doWrite(loadModel(), null);
    }

    @GetMapping(value = "/query/{q}",
            produces = {"application/x-javascript", "application/json", "application/ld+json"})
    @ResponseBody
    public String request(@PathVariable String q) {

        LOG.info("GET /query/{q} : " + q);

        String query = null;

        switch (q) {
            case "taxonname":
                query = "select tn from TaxonName as tn" +
                        " left join fetch tn.taxonomicLevel as tl" +
                        " join fetch tn.referenceTaxon as rt";
                break;
            case "species":
                query = "select tn from TaxonName as tn" +
                        " left join fetch tn.taxonomicLevel as tl" +
                        " join fetch tn.referenceTaxon as rt";
                break;
            default:
                query = null;
        }

        return doWrite(loadModel(query), null);
    }


    @GetMapping(value = "/ntriple", produces = {"application/n-triples", "text/n-triples"})
    @ResponseBody
    public String getModelAsNTriples() {
        return doWrite(loadModel(), "N-TRIPLE");
    }


    @GetMapping(value = "/ttl", produces = {"text/turtle"})
    @ResponseBody
    public String getModelAsTurtle() {
        return doWrite(loadModel(), "TURTLE");
    }


    @GetMapping(value = "/n3", produces = {"text/n3"})
    @ResponseBody
    public String getModelAsN3() {
        return doWrite(loadModel(), "N3");
    }


    @GetMapping(value = "/rdfjson", produces = {"application/x-javascript", "application/json", "application/ld+json"})
    @ResponseBody
    public String getModelAsrdfjson() {
        return doWrite(loadModel(), "RDF/JSON");
    }


    @GetMapping(value = "/bin", produces = {"application/octet-stream"})
    @ResponseBody
    public String getModelAsBinary() {
        return doWrite(loadModel(), "RDF Binary");
    }

    @GetMapping(value = "/trig", produces = {"text/turtle"})
    @ResponseBody
    public String getModelAsTrig() {
        return doWrite(loadModel(), "TriG");
    }

    @GetMapping(value = "/trix", produces = {"text/turtle"})
    @ResponseBody
    public String getModelAsTrix() {
        return doWrite(loadModel(), "TriX");
    }
}