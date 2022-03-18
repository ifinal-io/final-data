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

package org.ifinalframework.data.auto.generator;

import org.ifinalframework.data.auto.annotation.AutoMapper;
import org.ifinalframework.data.auto.entity.Entity;
import org.ifinalframework.data.auto.entity.EntityFactory;
import org.ifinalframework.data.auto.processor.AutoMapperGeneratorProcessor;
import org.ifinalframework.data.mybatis.mapper.AbsMapper;
import org.ifinalframework.javapoets.JavaPoets;
import org.ifinalframework.javapoets.JavaPoets.Javadoc;

import java.io.IOException;
import java.io.Writer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.apache.ibatis.annotations.Mapper;

/**
 * AutoMapperGenerator.
 *
 * <pre class="code">
 *      package xxx.dao.mapper;
 *
 *      &#64;Mapper
 *      public interface XxxMapper extends AbsMapper&lt;Long,XxxEntity&gt;{
 *
 *      }
 * </pre>
 *
 * @author ilikly
 * @version 1.0.0
 * @since 1.0.0
 */
public class AutoMapperGenerator implements AutoGenerator<AutoMapper, TypeElement> {

    private static final String MAPPER_SUFFIX = "Mapper";

    private final ProcessingEnvironment processingEnv;

    public AutoMapperGenerator(final ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Override
    public void generate(final AutoMapper ann, final TypeElement entity) {

        final String packageName = processingEnv.getElementUtils().getPackageOf(entity).getQualifiedName().toString()
            .replace("." + ann.entity(), "." + ann.mapper());
        String mapperName = entity.getSimpleName().toString() + MAPPER_SUFFIX;

        final String elementName = packageName + "." + mapperName;

        try {
            final TypeElement mapperElement = processingEnv.getElementUtils().getTypeElement(elementName);

            if (mapperElement == null) {
                final JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(elementName);

                try (Writer writer = sourceFile.openWriter()) {
                    JavaFile javaFile = buildJavaFile(packageName, mapperName, entity);
                    javaFile.writeTo(writer);
                    writer.flush();
                }

            }

        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private JavaFile buildJavaFile(final String packageName, final String mapperName, final TypeElement typeElement) {

        Entity entity = EntityFactory.create(processingEnv, typeElement);

        // AbsMapper<I,IEntity>
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
            ClassName.get(AbsMapper.class),
            TypeName.get(entity.getRequiredIdProperty().getType()),
            ClassName.get(entity.getElement())
        );

        // public interface EntityMapper extends AbsMapper<I,IEntity>
        TypeSpec myMapper = TypeSpec.interfaceBuilder(mapperName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(parameterizedTypeName)
            .addAnnotation(Mapper.class)
            .addAnnotation(JavaPoets.generated(AutoMapperGeneratorProcessor.class))
            .addJavadoc(Javadoc.author())
            .addJavadoc(Javadoc.version())
            .build();

        return JavaFile.builder(packageName, myMapper)
            .skipJavaLangImports(true).build();

    }

    private void error(final String msg) {

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

}
