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

import org.springframework.stereotype.Service;

import org.ifinalframework.data.auto.annotation.AutoService;
import org.ifinalframework.data.service.AbsService;
import org.ifinalframework.data.service.AbsServiceImpl;
import org.ifinalframework.javapoets.JavaPoets;
import org.ifinalframework.javapoets.JavaPoets.Javadoc;

import java.io.Writer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * AutoServiceGenerator.
 *
 * @author ilikly
 * @version 1.0.0
 * @since 1.0.0
 */
public class AutoServiceGenerator implements AutoGenerator<AutoService, TypeElement> {

    private static final String MAPPER_SUFFIX = "Mapper";

    private static final String SERVICE_SUFFIX = "Service";

    private static final String SERVICE_IMPL_SUFFIX = "ServiceImpl";

    private final ProcessingEnvironment processingEnv;

    public AutoServiceGenerator(final ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Override
    public void generate(final AutoService ann, final TypeElement entity) {

        final String servicePackageName = processingEnv.getElementUtils().getPackageOf(entity).getQualifiedName()
            .toString()
            .replace("." + ann.entity(), "." + ann.service());

        final String serviceImplPackageName = servicePackageName + ".impl";

        final String serviceName = entity.getSimpleName().toString() + SERVICE_SUFFIX;
        final String serviceImplName = entity.getSimpleName().toString() + SERVICE_IMPL_SUFFIX;

        final TypeElement serviceElement = processingEnv.getElementUtils()
            .getTypeElement(servicePackageName + "." + serviceName);
        final TypeElement serviceImplElement = processingEnv.getElementUtils()
            .getTypeElement(serviceImplPackageName + "." + serviceImplName);

        generateService(ann, entity, servicePackageName, serviceName, serviceElement);
        generateServiceImpl(ann, entity, servicePackageName, serviceImplPackageName, serviceName, serviceImplName,
            serviceImplElement);

    }

    private void generateService(final AutoService autoService, final TypeElement entity,
        final String servicePackageName, final String serviceName,
        final TypeElement serviceElement) {
        if (serviceElement == null) {

            try {
                final JavaFileObject sourceFile = processingEnv.getFiler()
                    .createSourceFile(servicePackageName + "." + serviceName);

                final String mapperPackageName = processingEnv.getElementUtils().getPackageOf(entity).getQualifiedName()
                    .toString()
                    .replace("." + autoService.entity(), "." + autoService.mapper());
                final String mapperName = entity.getSimpleName().toString() + MAPPER_SUFFIX;

                // AbsService<I,IEntity,EntityMapper>
                ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
                    ClassName.get(AbsService.class),
                    // 偷个小懒，先写死
                    TypeName.get(Long.class),
                    ClassName.get(entity),
                    ClassName.get(mapperPackageName, mapperName)
                );

                // public interface EntityService extends AbsService<I,IEntity,EntityMapper>
                TypeSpec service = TypeSpec.interfaceBuilder(serviceName)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(parameterizedTypeName)
                    .addAnnotation(JavaPoets.generated(AutoServiceGenerator.class))
                    .addJavadoc(Javadoc.author())
                    .addJavadoc(Javadoc.version())
                    .build();

                try (Writer writer = sourceFile.openWriter()) {
                    JavaFile javaFile = JavaFile.builder(servicePackageName, service)
                        .skipJavaLangImports(true).build();
                    javaFile.writeTo(writer);
                    writer.flush();
                }

            } catch (Exception e) {
                error(e.getMessage());
            }

        }
    }

    private void generateServiceImpl(final AutoService autoService, final TypeElement entity,
        final String servicePackageName,
        final String serviceImplPackageName, final String serviceName, final String serviceImplName,
        final TypeElement serviceImplElement) {
        if (serviceImplElement == null) {
            try {
                final JavaFileObject sourceFile = processingEnv.getFiler()
                    .createSourceFile(serviceImplPackageName + "." + serviceImplName);

                final String mapperPackageName = processingEnv.getElementUtils().getPackageOf(entity).getQualifiedName()
                    .toString()
                    .replace("." + autoService.entity(), "." + autoService.mapper());
                final String mapperName = entity.getSimpleName().toString() + MAPPER_SUFFIX;

                // AbsServiceImpl<I,IEntity,EntityMapper>
                ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
                    ClassName.get(AbsServiceImpl.class),
                    // 偷个小懒，先写死
                    TypeName.get(Long.class),
                    ClassName.get(entity),
                    ClassName.get(mapperPackageName, mapperName)
                );

                MethodSpec constructor = MethodSpec.constructorBuilder()
//                    .addAnnotation(JavaPoets.generated(AutoServiceGenerator.class))
                    .addParameter(
                        ParameterSpec.builder(ClassName.get(mapperPackageName, mapperName), "repository")
                            .addModifiers(Modifier.FINAL).build())
                    .addStatement("super(repository)")
                    .build();

                //  class EntityServiceImpl extends AbsServiceImpl<I,IEntity,EntityMapper> implements EntityService
                TypeSpec service = TypeSpec.classBuilder(serviceImplName)
                    .superclass(parameterizedTypeName)
                    .addSuperinterface(ClassName.get(servicePackageName, serviceName))
                    .addMethod(constructor)
                    .addAnnotation(Service.class)
                    .addAnnotation(JavaPoets.generated(AutoServiceGenerator.class))
                    .addJavadoc(Javadoc.author())
                    .addJavadoc(Javadoc.version())
                    .build();

                try (Writer writer = sourceFile.openWriter()) {
                    JavaFile javaFile = JavaFile.builder(serviceImplPackageName, service)
                        .skipJavaLangImports(true)
                        .indent("    ")
                        .build();
                    javaFile.writeTo(writer);
                    writer.flush();
                }

            } catch (Exception e) {
                error(e.getMessage());
            }
        }
    }

    private void error(final String msg) {

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

}
