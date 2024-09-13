/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package net.sumaris.xml.query.annotation;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.StringMemberValue;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.xml.query.XMLQuery;
import net.sumaris.xml.query.XMLQueryImpl;
import org.springframework.stereotype.Component;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

@SupportedAnnotationTypes("net.sumaris.xml.query.annotation.XmlQuery")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class XmlQueryProcessor extends AbstractProcessor {

    public XmlQueryProcessor() {
        super();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Processing...");

        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : annotatedElements) {
                if (element.getKind() == ElementKind.INTERFACE) {
                    // Process the annotated interface
                    TypeElement annotatedInterface = (TypeElement) element;
                    XmlQuery xmlQuery = annotatedInterface.getAnnotation(XmlQuery.class);

                    processAnnotatedInterface(annotatedInterface, xmlQuery);
                }
            }
        }
        return true;
    }

    private void processAnnotatedInterface(TypeElement annotatedInterface, XmlQuery xmlQueryAnnotation) {
        String interfaceName = annotatedInterface.getQualifiedName().toString();
        String implClassName = interfaceName + "Impl";
        String implConstructorName = annotatedInterface.getSimpleName() + "Impl";
        String beanName = annotatedInterface.getSimpleName().toString().substring(0,1).toLowerCase() + annotatedInterface.getSimpleName().toString().substring(1);

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating class " + implClassName);

        String xmlResourcePath = xmlQueryAnnotation.value();
        DatabaseType dbms = xmlQueryAnnotation.dbms();
        if (dbms == null) dbms = DatabaseType.hsqldb;

        // Load the XML file and create an instance of the XmlQuery class
        XMLQueryImpl xmlQuery = new XMLQueryImpl(dbms);
        try {
            if (xmlResourcePath.startsWith("classpath:")) {
                xmlResourcePath = xmlResourcePath.substring("classpath:".length());
                FileObject resource = processingEnv.getFiler().getResource(StandardLocation.CLASS_PATH, "", xmlResourcePath);
                InputStream xmlInputStream = resource.openInputStream();
                xmlQuery.setQuery(xmlInputStream);
            }
            else {
                xmlQuery.setQuery(xmlResourcePath);
            }
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to load XML resource: " + xmlResourcePath);
            return;
        }

        String outputDirectory = processingEnv.getOptions().get("generatedSourcesDirectory");
        if (outputDirectory == null) {
            outputDirectory = "target/classes";
        }

        // Extract groups from the XML file
        Set<String> groups = xmlQuery.extractGroups();

        try {
            ClassPool classPool = ClassPool.getDefault();
            classPool.appendClassPath(new ClassClassPath(XMLQuery.class));

            CtClass ctInterface = classPool.get(XMLQuery.class.getName());
            CtClass generatedClass = classPool.makeClass(implClassName);
            generatedClass.addInterface(ctInterface);

            // Faire en sorte que la classe générée hérite de XmlQuery
            CtClass ctXmlQuery = classPool.get(XMLQueryImpl.class.getName());
            generatedClass.setSuperclass(ctXmlQuery);

            for (String group : groups) {
                // Generate enableGroupXxx and disableGroupXxx methods for each group
                CtMethod enableMethod = CtNewMethod.make(createEnableGroupMethod(group), generatedClass);
                CtMethod disableMethod = CtNewMethod.make(createDisableGroupMethod(group), generatedClass);

                generatedClass.addMethod(enableMethod);
                generatedClass.addMethod(disableMethod);
            }

            // Generate constructor
            CtConstructor constructor = CtNewConstructor.make("public "+ implConstructorName + "() {" +
                "    super(\"" + dbms.name() + "\");" +
                "    this.setQuery(\"" + xmlResourcePath + "\");" +
                "}", generatedClass);
            generatedClass.addConstructor(constructor);

            // Add annotation @Component
            CtClass ctComponent = classPool.get(Component.class.getName());
            ConstPool constPool = generatedClass.getClassFile().getConstPool();
            AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            javassist.bytecode.annotation.Annotation componentAnnotation = new javassist.bytecode.annotation.Annotation(constPool, ctComponent);
            componentAnnotation.addMemberValue("value", new StringMemberValue(beanName, generatedClass.getClassFile().getConstPool()));
            annotationsAttribute.addAnnotation(componentAnnotation);
            generatedClass.getClassFile().addAttribute(annotationsAttribute);

            // Save the generated class
            generatedClass.writeFile(outputDirectory);
        } catch (CannotCompileException | NotFoundException | IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("Failed to generate class '%s' into '%s': %s",
                implClassName,
                outputDirectory,
                stackTrace
                ));
        }
    }

    private String createEnableGroupMethod(String groupName) {
        String methodName = "enableGroup" + groupName.substring(0, 1).toUpperCase() + groupName.substring(1);
        return "public void " + methodName + "() { setGroup(\"" + groupName + "\", true); }";
    }

    private String createDisableGroupMethod(String groupName) {
        String methodName = "disableGroup" + groupName.substring(0, 1).toUpperCase() + groupName.substring(1);
        return "public void " + methodName + "() { setGroup(\"" + groupName + "\", false); }";
    }
}
