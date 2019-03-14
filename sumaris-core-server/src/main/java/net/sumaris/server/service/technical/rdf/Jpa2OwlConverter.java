package net.sumaris.server.service.technical.rdf;

import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import net.sumaris.server.config.Jpa2OwlConfig;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper functions to convert Java Bean / Spring Jpa objects  to Jena  model
 * <p>
 * the methods described here are mostly generic, business code and objects are being passed as parameters
 */
public interface Jpa2OwlConverter extends Jpa2OwlConfig {

    Logger LOG = LogManager.getLogger();

    List<Class> ACCEPTED_LIST_CLASS = Arrays.asList(List.class, ArrayList.class);
    Map<String, String> FORMATS = initFormats();
    Map<Class, Resource> TYPE_CONVERTER = initConverters();
    Map<Resource, Class> TYPE_CONVERTER2 = initConverters2();
    AtomicInteger ai = new AtomicInteger(0);


    static Map<Resource, Class> initConverters2() {
        return TYPE_CONVERTER.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey, (x, y) -> x));
    }

    static Map<String, String> initFormats() {
        Map<String, String> res = new HashMap<>();
        res.put("json", "JSON-LD");
        res.put("ttl", "TURTLE");
        res.put("ntriple", "N-TRIPLE");
        res.put("n3", "N3");
        res.put("trig", "TriG");
        res.put("trix", "TriX");
        // res.put("thrift", "RDFTHRIFT");
        return res;
    }


    static Map<Class, Resource> initConverters() {
        Map<Class, Resource> res = new HashMap<>();
        res.put(Date.class, XSD.date);
        res.put(LocalDateTime.class, XSD.dateTime);
        res.put(Timestamp.class, XSD.dateTimeStamp);
        res.put(Integer.class, XSD.integer);
        res.put(Short.class, XSD.xshort);
        res.put(Long.class, XSD.xlong);
        res.put(Double.class, XSD.xdouble);
        res.put(Float.class, XSD.xfloat);
        res.put(Boolean.class, XSD.xboolean);
        res.put(long.class, XSD.xlong);
        res.put(int.class, XSD.integer);
        res.put(float.class, XSD.xfloat);
        res.put(double.class, XSD.xdouble);
        res.put(short.class, XSD.xshort);
        res.put(boolean.class, XSD.xboolean);
        res.put(String.class, XSD.xstring);
        return res;
    }

    default Stream<Annotation> annotsOfField(Optional<Field> field) {
        return field.map(field1 -> Stream.of(field1.getAnnotations())).orElseGet(Stream::empty);
    }

    /**
     * check the getter and its corresponding field's annotations
     *
     * @param met the getter method to test
     * @return true if it is a technical id to exclude from the model
     */
    default boolean isId(Method met) {
        return "getId".equals(met.getName())
                && Stream.concat(annotsOfField(getFieldOfGetter(met)), Stream.of(met.getAnnotations()))
                .anyMatch(annot -> annot instanceof Id || annot instanceof org.springframework.data.annotation.Id);
    }

    default boolean isManyToOne(Method met) {
        return annotsOfField(getFieldOfGetter(met)).anyMatch(annot -> annot instanceof ManyToOne) // check the corresponding field's annotations
                ||
                Stream.of(met.getAnnotations()).anyMatch(annot -> annot instanceof ManyToOne)  // check the method's annotations
                ;
    }


    default boolean isGetter(Method met) {
        return met.getName().startsWith("get") // only getters
                && !"getBytes".equals(met.getName()) // ignore ugly
                && met.getParameterCount() == 0 // ignore getters that are not getters
                && getFieldOfGetter(met).isPresent()
                ;
    }


    default Method getterOfField(Class t, String field) {
        try {
            Method res = t.getMethod("get" + field.substring(0, 1).toUpperCase() + field.substring(1));
            return res;
        } catch (NoSuchMethodException e) {
            LOG.error("error in the declaration of allowed ManyToOne " + e.getMessage());
        }
        return null;
    }

    static String classToURI(Class c) {
        String uri = SCHEMA_URL + c.getTypeName();
        if (uri.substring(1).contains("<")) {
            uri = uri.substring(0, uri.indexOf("<"));
        }
        if (uri.endsWith("<java.lang.Integer, java.util.Date>")) {
            uri = uri.replace("<java.lang.Integer, java.util.Date>", "");
        }

        if(uri.contains("$")){
            LOG.error("Inner classes not handled " + uri);
        }

        return uri;
    }


    default Optional<Method> setterOfField(Class t, String field) {
        try {
            Field f = fieldOf(t, field);
            Method met = t.getMethod("set" + f.getName().substring(0, 1).toUpperCase() + f.getName().substring(1), f.getType());
            return Optional.of(met);
        } catch (NoSuchMethodException e) {
            LOG.error("NoSuchMethodException setterOfField " + field + " - ", e);
        } catch (NullPointerException e) {
            LOG.error("NullPointerException setterOfField " + field + " - ", e);
        }
        return Optional.empty();
    }

    default Field fieldOf(Class t, String name) {
        try {
            return URI_2_CLASS.get(classToURI(t)).getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            LOG.error("error fieldOf " + t.getSimpleName() + " " + name + " - " + e.getMessage());
        }
        return null;
    }


    static void main(String[] args) {
        net.sumaris.core.model.referential.Status s = new Status();

    }

    /**
     * Serialize model in requested format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a string representation of the model
     */
    default String doWrite(Model model, String format) {

        try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            if (format == null) {
                model.write(os);
            } else {
                model.write(os, format);
            }
            return os.toString();
        } catch (IOException e) {
            LOG.error("doWrite ", e);
        }
        return "there was an error writing the model ";
    }

    default Optional<Field> getFieldOfGetter(Method getter) {

        String fieldName = getter.getName().substring(3, 4).toLowerCase() + getter.getName().substring(4);
        //LOG.info("searching field : " + fieldName);
        try {
            return Optional.of(getter.getDeclaringClass().getDeclaredField(fieldName));
        } catch (Exception e) {
            //LOG.error("field not found : " + fieldName + " for class " + getter.getDeclaringClass() + "  " + e.getMessage());
            return Optional.empty();
        }
    }

    default Optional<Field> getFieldOfSetter(Method setter) {
        if (!setter.getName().startsWith("set"))
            return Optional.empty();
        String fieldName = setter.getName().substring(3, 4).toLowerCase() + setter.getName().substring(4);
        //LOG.info("searching field : " + fieldName);
        try {
            return Optional.of(setter.getDeclaringClass().getDeclaredField(fieldName));
        } catch (Exception e) {
            //LOG.error("field not found : " + fieldName + " for class " + getter.getDeclaringClass() + "  " + e.getMessage());
            return Optional.empty();
        }
    }

    default boolean isJavaType(Type type) {
        return TYPE_CONVERTER.keySet().stream().anyMatch(type::equals);
    }

    default boolean isJavaType(Method getter) {
        return isJavaType(getter.getGenericReturnType());
    }

    default boolean isJavaType(Field field) {
        return isJavaType(field.getType());
    }


    default Resource getType(Field f) {
        return TYPE_CONVERTER.entrySet().stream()
                .filter((entry) -> entry.getKey().getTypeName().equals(f.getType().getName()))
                .map(Map.Entry::getValue)
                .findAny()
                .orElse(RDFS.Literal);
    }

    default Resource getType(Type type) {
        return TYPE_CONVERTER.entrySet().stream()
                .filter((entry) -> entry.getKey().getTypeName().equals(type.getTypeName()))
                .map(Map.Entry::getValue)
                .findAny()
                .orElse(RDFS.Literal);
    }


    /**
     * Performs a forced cast.
     * Returns null if the collection type does not match the items in the list.
     *
     * @param data     The list to cast.
     * @param listType The type of list to cast to.
     */
    default <T> List<? super T> castListSafe(List<?> data, Class<T> listType) {
        List<T> retval = null;
        //This test could be skipped if you trust the callers, but it wouldn't be safe then.
        if (data != null && !data.isEmpty() && listType.isInstance(data.iterator().next().getClass())) {
            LOG.info("  - castListSafe passed check ");

            @SuppressWarnings("unchecked")//It's OK, we know List<T> contains the expected type.
                    List<T> foo = (List<T>) data;
            return foo;
        }

        LOG.info("  - castListSafe failed check  forcing it though");
        return (List<T>) data;
    }

    default Optional<Resource> fillDataList(OntModel model, Type type, Object listObject, Property prop, Resource fieldId, int depth) {

        if (isListType(type)) {

// Create a list containing the subjects of the role assignments in one go

            List<RDFNode> nodes = new ArrayList<>();
            List<? extends Object> asList = castListSafe((List<? extends Object>) listObject, Object.class);

            if (asList.isEmpty()) {
                LOG.warn(" - empty list, ignoring ");
                return Optional.empty();
            }
            for (Object x : asList) {
                Resource listItem = toModel(model, x, (depth - 1));
                nodes.add(listItem);

            }

            RDFList list = model.createList(nodes.toArray(new RDFNode[nodes.size()]));

            LOG.info("  - rdflist " + list.size() + " : " + list);
//var tmp = model.createProperty("sdfsdfsdf"+new Random().nextInt(10000000));
            fieldId.addProperty(prop, list);
            //              fieldId.addProperty(tmp ,model.createList(list));
            return Optional.of(list);

        }

//        }
        return Optional.empty();
    }


    //default <T> List<? super T> castListSafe(List<?> data, Class<T> listType) {
//    default  <T> Class<T> classFromURI(String uri, Class<T> cast ) {
//
//        return (cast)
//        if (uri != null) {
//            if (uri.startsWith("http://www.e-is.pro/2019/03/schema/net.sumaris.core.model.referential.gear.Gear"))
//                return Gear.class;
//            if (uri.startsWith("http://www.e-is.pro/2019/03/schema/net.sumaris.core.model.referential.taxon.ReferenceTaxon"))
//                return ReferenceTaxon.class;
//            if (uri.startsWith("http://www.e-is.pro/2019/03/schema/net.sumaris.core.model.referential.taxon.TaxonName"))
//                return TaxonName.class;
//            if (uri.startsWith("http://www.e-is.pro/2019/03/schema/net.sumaris.core.model.referential.Status"))
//                return Status.class;
//            if (uri.startsWith("http://www.e-is.pro/2019/03/schema/net.sumaris.core.model.referential.taxon.TaxonomicLevel"))
//                return TaxonomicLevel.class;
//        }
//        return Object.class;
//    }

    default boolean isManyTo(AccessibleObject met) {
        List<Class> manys = Arrays.asList(ManyToOne.class, ManyToMany.class);

        return Arrays.stream(met.getDeclaredAnnotations())
                .anyMatch(m -> manys.stream().anyMatch(m1 -> m1.equals(m)));

    }

    default Object safeCastRDFNode(RDFNode node, String fieldName, Class<? extends Object> clazz) {
        try {
            LOG.info("safeCastRDFNode " + node + " - " + fieldName + " - " + clazz + "  ");

            Class returnType = clazz.getDeclaredField(fieldName).getClass();
            LOG.info(" -- expecting return Type " + returnType);

            if (returnType.equals(Integer.class)) {
                return node.asLiteral().getInt();
            }
            if (returnType.equals(Date.class)) {
                return node.asLiteral().getString();
            }
            if (returnType.equals(Long.class)) {
                return node.asLiteral().getLong();
            }

        } catch (NoSuchFieldException e) {
            LOG.error(clazz + "#" + e.getMessage(), e);
        }
        return null;
    }

    default boolean classEquals(Class c, Class<?> d) {
        return Objects.equals(d.getTypeName(), c.getTypeName());
    }

    default Optional<Class> ontToJavaClass(OntClass ontClass) {
        String uri = ontClass.getURI();
        if (uri != null) {
            if (uri.contains("#")) {
                uri = uri.substring(0, uri.indexOf("#"));
                LOG.warn(" tail '#'  " + uri);
            }
            if (uri.contains("<")) {
                uri = uri.substring(0, uri.indexOf("<"));
                LOG.warn(" tail <parametrized> " + uri);
            }
        }

        if (uri == null) {
            LOG.error(" uri null for OntClass " + ontClass);
            return Optional.empty();
        }


        Class clazz = URI_2_CLASS.get(uri);

        if (clazz == null) {
            LOG.warn(" clazz not mapped for uri " + uri);
            return Optional.empty();
        }

        if (clazz.isInterface()) {
            LOG.warn(" corresponding Type is interface, skip instances " + clazz);
            return Optional.empty();
        }
        return Optional.of(clazz);

    }

    default Optional<Object> individualToInstance(OntResource ontResource, Class clazz) {
        LOG.info("processing ont Instance " + ontResource + " - " +
                ontResource
                        .asIndividual()
                        .listProperties().toList().size());

        try {
            Object obj = clazz.newInstance();

            ontResource
                    .asIndividual()
                    .listProperties()
                    .toList()
                    .forEach(stmt -> {
                        String pred = stmt.getPredicate().getURI();
                        RDFNode val = stmt.getObject();
                        if ((pred.startsWith(SCHEMA_URL) || pred.startsWith(ADAGIO_URL)) && pred.contains("#")) {
                            String fName = pred.substring(pred.indexOf("#") + 1);
                            fName = fName.substring(0, 1).toLowerCase() + fName.substring(1);
                            try {
                                Method setter;
                                if (fName.equals("id")) {
                                    LOG.warn("searching id field ");
                                    setter = findSetterAnnotatedID(clazz);
                                } else {
                                    setter = setterOfField(clazz, fName).get();
                                }
                                if (setter == null) {
                                    LOG.warn("no setter found for field " + fName + " on class " + clazz);
                                    return;
                                }
                                Class<?> setterParam = setter.getParameterTypes()[0];
                                // LOG.info("trying to insert  " + fName + " => " + val + " using method " + me + " !!  " + huhu);

                                if (classEquals(setterParam, String.class)) {
                                    setter.invoke(obj, val.asLiteral().getString());
                                } else if (classEquals(setterParam, Long.class) || classEquals(setterParam, long.class)) {
                                    setter.invoke(obj, val.asLiteral().getLong());
                                } else if (classEquals(setterParam, Integer.class) || classEquals(setterParam, int.class)) {
                                    setter.invoke(obj, Integer.parseInt(val.toString()));
                                } else if (classEquals(setterParam, Date.class)) {
                                    setter.invoke(obj, sdf.parse(val.asLiteral().getString()));
                                } else if (classEquals(setterParam, Boolean.class) || classEquals(setterParam, boolean.class)) {
                                    setter.invoke(obj, val.asLiteral().getBoolean());
                                }
//                              else if (val.isURIResource()) {
//                                  setter.invoke(obj, val.asLiteral().getBoolean());
//                              }
                                else {
                                    setter.invoke(obj, getTranslatedReference(val, setterParam, obj));
                                }
                                //myAccessor.setPropertyValue(fName,safeCastRDFNode(val, fName,clazz));
                            } catch (Exception e) {
                                LOG.error(e.getClass().getSimpleName() + " on field " + fName + " => " + val + " using class " + clazz + " using method " + setterOfField(clazz, fName) + " " + e.getMessage(), e);
                            }

                            //values.put(fName, safeCastRDFNode(val, fName, clazz));
                        }

                    });
            //getEntityManager().merge(obj);
            LOG.info("  - created object " + ontResource + " - " + " of class " + ontResource.getClass() + "  - " + ai.get());
            return Optional.of(obj);
        } catch (Exception e) {
            LOG.error(" processing individual " + ontResource + " - " + clazz, e);
        }
        return Optional.empty();
    }


    default Object getTranslatedReference(RDFNode val, Class<?> setterParam, Object obj) {
        ai.getAndIncrement();

        String identifier = val.toString();
        if (identifier.contains("#"))
            identifier = identifier.substring(identifier.indexOf("#") + 1);

        if (setterParam == TaxonomicLevel.class) {

            LOG.warn("TaxonomicLevel " + getCacheTL().size());
            for (Object ctl : getCacheTL()) {
                String lab = ((TaxonomicLevel) ctl).getLabel();
                if (identifier.endsWith(lab)) {
                    return ctl;
                }
            }

            // if none were cached, create a new TaxonLevel
            TaxonomicLevel tl = new TaxonomicLevel();
            tl.setLabel(identifier);
            getEntityManager().persist(tl);

            return URI_2_OBJ_REF.putIfAbsent(val.toString(), tl);
        }


        // Default case, try to fetch reference (@Id) as Integer or String
        LOG.warn("mapping getEntityManager().getReference " + identifier + " - " + val + " - " + obj);
        Object ref;
        try {
            Integer asInt = Integer.parseInt(identifier);
            ref = getEntityManager().getReference(setterParam, asInt);
        } catch (NumberFormatException e) {
            ref = getEntityManager().getReference(setterParam, identifier);
        }
        return ref;
    }

    default List<Object> fromModel(OntModel m) {

        List<Object> ret = new ArrayList<>();

        for (OntClass ontClass : m.listClasses().toList()) {


            ontToJavaClass(ontClass).ifPresent(clazz -> {
                for (OntResource ontResource : ontClass.listInstances().toList()) {

                    Function<OntResource, Object> f = ARBITRARY_MAPPER.get(ontClass.getURI());
                    if (f != null) {
                        ret.add(f.apply(ontResource));
                    } else {
                        individualToInstance(ontResource, clazz).ifPresent(ret::add);
                    }

                }
            });


        }
        return ret;
    }


    default Method findGetterAnnotatedID(Class clazz) {
        for (Field f : clazz.getDeclaredFields())
            for (Annotation an : f.getDeclaredAnnotations())
                if (an instanceof Id)
                    return getterOfField(clazz, f.getName());

        return null;
    }


    default Method findSetterAnnotatedID(Class clazz) {
        for (Field f : clazz.getDeclaredFields())
            for (Annotation an : f.getDeclaredAnnotations())
                if (an instanceof Id)
                    return setterOfField(clazz, f.getName()).get();

        return null;
    }

    default Resource toModel(OntModel model, Object obj, int depth) {

        String classURI = classToURI(obj.getClass());
        OntClass ontClazz = model.getOntClass(classURI);
        if (ontClazz == null) {
            LOG.warn("ontClazz " + ontClazz + ", not found in model, making one at " + classURI);
            ontClazz = buildOwlClass(model, obj.getClass(), null);
        }

        // try using the ID field if exists to represent the node
        String objectIdentifier;
        try {
            Method m = findGetterAnnotatedID(obj.getClass());
            objectIdentifier = classURI + "#" + m.invoke(obj);
            LOG.info("Created objectIdentifier " + objectIdentifier);
        } catch (Exception e) {
            objectIdentifier = "";
            LOG.error(e.getClass().getName() + " toModel " + classURI + " - ");
        }
        Resource individual = ontClazz.createIndividual(objectIdentifier); // model.createResource(node);
        if (depth < 0) {
            LOG.error("Max depth reached " + depth);
            return individual;
        } else {
            // LOG.warn("depth reached " + depth);

        }


        // Handle Methods
        Stream.of(obj.getClass().getMethods())
                .filter(this::isGetter)
                .filter(met -> BLACKLIST.stream().noneMatch(x -> x.equals(met)))
                .filter(met -> (!isManyToOne(met)) || ALLOWED_MANY_TO_ONE.contains(met))
                .forEach(met -> {

                    //LOG.info("processing method " + met.getDeclaringClass().getSimpleName()+"."+ met.getName()+" "+met.getGenericReturnType());
                    try {
                        Object invoked = met.invoke(obj);
                        if (invoked == null) {
                            //LOG.warn("invoked function null "+ met.getName() + " skipping... " );
                            return;
                        }
                        Property pred = model.createProperty(classURI, "#" + met.getName().replace("get", ""));

                        if (isId(met)) {
                            individual.addProperty(pred, invoked + "");
                        } else if ("getClass".equals(met.getName())) {
                            individual.addProperty(RDF.type, pred);
                        } else if ( invoked.getClass().getCanonicalName().contains("$")) {
                            //skip inner classes. mostly handles generated code issues
                        } else if (!isJavaType(met)) {

                            //LOG.info("not java generic, recurse on node..." + invoked);
                            Resource recurse = toModel(model, invoked, (depth - 1));
                            LOG.warn("recurse null for " + met.getName() + "  " + invoked.getClass() + "  ");
                            if (recurse != null)
                                individual.addProperty(pred, recurse);
                        } else if (met.getGenericReturnType() instanceof ParameterizedType) {

                            Resource anonId = model.createResource(new AnonId("params" + new Random().nextInt(1000000)));
                            //individual.addProperty( pred, anonId);

                            Optional<Resource> listNode = fillDataList(model, met.getGenericReturnType(), invoked, pred, anonId, depth - 1);
                            if (listNode.isPresent()) {
                                LOG.info(" --and res  : " + listNode.get().getURI());
                                individual.addProperty(pred, listNode.get());
                            }
                            //
                        } else {
                            if (met.getName().toLowerCase().contains("date")) {
                                String d = sdf.format((Date) invoked);
                                individual.addProperty(pred, d);

                            } else {
                                individual.addProperty(pred, invoked + "");
                            }
                        }

                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }

                });

        return individual;
    }

    default Type getListType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;// This would be Class<List>, say
            Type raw = parameterized.getRawType();
            Type own = parameterized.getOwnerType();
            Type[] typeArgs = parameterized.getActualTypeArguments();

            if (ACCEPTED_LIST_CLASS.stream()
                    .anyMatch(x -> x.getCanonicalName().equals(raw.getTypeName()))) {
                return typeArgs[0];
            }
        }
        long x;
        return null;
    }

    default boolean isListType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;// This would be Class<List>, say
            Type raw = parameterized.getRawType();

            return (ACCEPTED_LIST_CLASS.stream()
                    .anyMatch(x -> x.getCanonicalName().equals(raw.getTypeName())));
        }

        return false;

    }


    default void createOneToMany(OntModel ontoModel, OntClass ontoClass, OntProperty prop, Resource resource) {
//        OntClass allValuesFromRestriction = ontoModel.createAllValuesFromRestriction(null, prop, resource);
//        ontoClass.addSuperClass(allValuesFromRestriction);

        OntClass minCardinalityRestriction = ontoModel.createMinCardinalityRestriction(null, prop, 1);
        ontoClass.addSuperClass(minCardinalityRestriction);
    }

    default void createZeroToMany(OntModel ontoModel, OntClass ontoClass, OntProperty prop, Resource resource) {
//        OntClass allValuesFromRestriction = ontoModel.createAllValuesFromRestriction(null, prop, resource);
//        ontoClass.addSuperClass(allValuesFromRestriction);

        OntClass minCardinalityRestriction = ontoModel.createMinCardinalityRestriction(null, prop, 0);
        ontoClass.addSuperClass(minCardinalityRestriction);
    }

    default void createZeroToOne(OntModel ontoModel, OntClass ontoClass1, OntProperty prop, OntClass ontoClass2) {

//        OntClass minCardinalityRestriction = ontoModel.createMinCardinalityRestriction(null, prop, 0);
//        ontoClass1.addSuperClass(minCardinalityRestriction);

        OntClass maxCardinalityRestriction = ontoModel.createMaxCardinalityRestriction(null, prop, 1);
        ontoClass1.addSuperClass(maxCardinalityRestriction);
    }

    default void createOneToOne(OntModel ontoModel, OntClass ontoClass1, OntProperty prop, OntClass ontoClass2) {
//        OntClass minCardinalityRestriction = ontoModel.createMinCardinalityRestriction(null, prop, 1);
//        ontoClass1.addSuperClass(minCardinalityRestriction);

        OntClass maxCardinalityRestriction = ontoModel.createMaxCardinalityRestriction(null, prop, 1);
        ontoClass1.addSuperClass(maxCardinalityRestriction);
    }


    /**
     * Returns and adds OntClass to the model
     *
     * @param ontology
     * @param ent
     * @return
     */
    default OntClass buildOwlClass(OntModel ontology, Class ent, Map<OntClass, List<OntClass>> mutualyDisjoint) {
        Resource schema = ontology.getResource(SCHEMA_URL);

        try {
            //OntResource r = m.createOntResource(SCHEMA_URL+ ent.getName().replaceAll("\\.", "/"));
            //OntProperty pred = m.createOntProperty(SCHEMA_URL+ ent.getName().replaceAll("\\.", "/"));
            //String classBase = ent.getName().replaceAll("\\.", "/");
//                        LOG.info("Building Ont of " + classBase);

            OntClass aClass = ontology.createClass(classToURI(ent));
            aClass.setIsDefinedBy(schema);

            aClass.addSameAs(ontology.createResource(classToURI(ent)));

            String label = ent.getName().substring(ent.getName().lastIndexOf(".") + 1);
            //aClass.addLabel(label + "e", "fr");
            aClass.addLabel(label, "en");

            if (mutualyDisjoint != null) {
                Stream.of(ent.getGenericInterfaces())
                        .forEach(parent -> {
                            OntClass r = ontology.createClass(SCHEMA_URL + parent.getTypeName());
                            if (!mutualyDisjoint.containsKey(r)) {
                                mutualyDisjoint.put(r, new ArrayList<>());
                            }
                            mutualyDisjoint.get(r).add(aClass);
                            aClass.addSuperClass(r);
                        });
            }


            Stream.of(ent.getGenericSuperclass()).filter(c -> !c.equals(Object.class))
                    .forEach(i -> aClass.addSuperClass(ontology.createResource(SCHEMA_URL + i.getTypeName())));

            String entityName = ent.getName();
            if (!"".equals(entityName))
                aClass.addComment(entityName, "en");

            Stream.of(ent.getMethods())
                    .filter(this::isGetter)
                    .map(this::getFieldOfGetter)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(field -> {
                        //LOG.info("processing Field : " + field + " - type?" + isJavaType(field) + " - list?" + isListType(field.getGenericType()));
                        String fieldName = SCHEMA_URL + ent.getTypeName() + "#" + field.getName();
                        String fileTypeURI = SCHEMA_URL + field.getType().getName();

                        if (isJavaType(field)) {
                            Resource type = getType(field);

                            OntProperty link = ontology.createDatatypeProperty(fieldName, true);

                            link.setDomain(aClass.asResource());
                            link.setRange(type);
                            link.setIsDefinedBy(schema);


                            //link.addRDFType(ontology.createResource(type));
                            link.addLabel(field.getName(), "en");
                            //LOG.info("Simple property of type " + type + " for " + fieldName + "\n" + link);
                        } else if (isListType(field.getGenericType())) {
                            Type contained = getListType(field.getGenericType());
                            OntProperty link = null;
                            Resource resou = null;
                            LOG.info("List property x " + contained.getTypeName() + " for " + fieldName + "\n" + fileTypeURI);

                            if (isJavaType(contained)) {
                                link = ontology.createDatatypeProperty(fieldName, true);
                                resou = getType(contained);
                            } else {
                                link = ontology.createObjectProperty(fieldName, false);
                                resou = ontology.createOntResource(SCHEMA_URL + contained.getTypeName());
                            }

                            link.addRange(resou);
                            link.addDomain(aClass.asResource());
                            link.setIsDefinedBy(schema);

                            createZeroToMany(ontology, aClass, link, resou);

                        } else {

                            // var type = ontology.createObjectProperty();
                            OntProperty link = ontology.createObjectProperty(fieldName, true);
                            link.addDomain(aClass.asResource());
                            link.addRange(ontology.createOntResource(fileTypeURI));
                            link.setIsDefinedBy(schema);

                            // LOG.info("Default Object property x " + link);

                        }
                    });
            return aClass;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }
}
