/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.finalframework.data.auto.generator;

import org.finalframework.data.auto.annotation.AutoQuery;
import org.finalframework.data.auto.entity.EntityFactory;
import org.finalframework.data.auto.query.QEntity;
import org.finalframework.data.auto.query.QEntityFactory;
import org.finalframework.data.query.AbsQEntity;
import org.finalframework.javapoets.JavaPoets;
import org.finalframework.javapoets.JavaPoets.Javadoc;
import org.finalframework.query.QProperty;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * AutoQueryGenerator.
 *
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
public class AutoQueryGenerator implements AutoGenerator<AutoQuery, TypeElement> {

    public AutoQueryGenerator(final ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    private final ProcessingEnvironment processingEnv;

    @Override
    public void generate(final AutoQuery ann, final TypeElement entity) {

        final String packageName = processingEnv.getElementUtils().getPackageOf(entity).getQualifiedName().toString()
            .replace("." + ann.entity(), "." + ann.query());

        generator(QEntityFactory.create(processingEnv, packageName, EntityFactory.create(processingEnv, entity)));
    }

    private void generator(final QEntity entity) {

        try {
            String name = entity.getName();
            info("try to generator entity of " + name);
            TypeElement mapperElement = processingEnv.getElementUtils().getTypeElement(name);
            if (mapperElement == null) {
                final JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(name);

                try (Writer writer = sourceFile.openWriter()) {
                    JavaFile javaFile = buildJavaFile(entity);
                    javaFile.writeTo(writer);
                    writer.flush();
                }

            }

        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
    }

    private void info(final String msg) {

        if (processingEnv.getOptions().containsKey("debug")) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
        }
    }

    private JavaFile buildJavaFile(final QEntity entity) {

        // AbsQEntity<I,IEntity>
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
            ClassName.get(AbsQEntity.class),
            TypeName.get(entity.getEntity().getRequiredIdProperty().getType()),
            ClassName.get(entity.getEntity().getElement())
        );

        MethodSpec defaultConstructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addStatement("super($T.class)", entity.getEntity().getElement())
            .build();

        MethodSpec tableConstructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            // final String table
            .addParameter(
                ParameterSpec.builder(ClassName.get(String.class), "table").addModifiers(Modifier.FINAL).build())
            // super(Entity.class, table)
            .addStatement("super($T.class, table)", entity.getEntity().getElement())
            .build();

        FieldSpec entityField = FieldSpec.builder(
            ClassName.get(entity.getPackageName(), entity.getSimpleName()),
            entity.getEntity().getSimpleName(),
            Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL
        ).initializer(String.format("new %s()", entity.getSimpleName()))
            .build();

        List<FieldSpec> fieldSpecs = entity.getProperties().stream()
            .map(property -> {
                Element element = property.getElement();
                TypeMirror type = element.asType();

                if (element instanceof ExecutableElement) {
                    type = ((ExecutableElement) element).getReturnType();
                }

                return FieldSpec.builder(
                    ParameterizedTypeName.get(
                        ClassName.get(QProperty.class), TypeName.get(type)), property.getName())
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    // Entity.getRequiredProperty('path')
                    .initializer("$L.getRequiredProperty($S)", entity.getEntity().getSimpleName(), property.getPath())
                    .build();
            })
            .collect(Collectors.toList());

        TypeSpec clazz = TypeSpec.classBuilder(entity.getSimpleName())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .superclass(parameterizedTypeName)
            .addMethod(defaultConstructor).addMethod(tableConstructor)
            .addAnnotation(JavaPoets.generated(AutoQueryGenerator.class))
            .addJavadoc(Javadoc.author())
            .addJavadoc(Javadoc.version())
            .addField(entityField)
            .addFields(fieldSpecs)
            .build();

        return JavaFile.builder(entity.getPackageName(), clazz)
            .skipJavaLangImports(true)
            .indent("    ").build();
    }

}
