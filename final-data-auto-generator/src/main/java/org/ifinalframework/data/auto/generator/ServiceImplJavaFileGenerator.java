/*
 * Copyright 2020-2022 the original author or authors.
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

import javax.lang.model.element.Modifier;
import java.util.Objects;

import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.data.auto.annotation.AutoService;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.service.AbsServiceImpl;
import org.ifinalframework.javapoets.JavaPoets;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.extern.slf4j.Slf4j;

/**
 * ServiceJavaFileGenerator.
 * <pre class="code">
 * public interface EntityService extends AbsService&lt;Long,Entity&gt;{
 *
 * }
 * </pre>
 *
 * @author ilikly
 * @version 1.4.1
 * @since 1.4.1
 */
@Slf4j
public class ServiceImplJavaFileGenerator implements JavaFileGenerator<AutoService> {
    @Override
    public String getName(AutoService ann, Class<?> clazz) {
        return String.join(".", AutoNameHelper.servicePackage(clazz) + ".impl", AutoNameHelper.serviceName(clazz) + "Impl");
    }

    @NonNull
    @Override
    public JavaFile generate(@NonNull AutoService ann, @NonNull Class<?> clazz) {

        String servicePackage = AutoNameHelper.servicePackage(clazz);
        String serviceName = AutoNameHelper.serviceName(clazz);

        final String serviceImplPackage = servicePackage + ".impl";
        final String serviceImplName = serviceName + "Impl";

        logger.info("start generate service for entity of {}.{}", serviceImplPackage, serviceImplName);

        try {

            Class<?> id = ResolvableType.forClass(clazz).as(IEntity.class).getGeneric().resolve();

            if (Objects.isNull(id)) {
                throw new IllegalArgumentException("not found id for entity of " + clazz);
            }
            // AbsServiceImpl<I,IEntity>
            ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
                    ClassName.get(AbsServiceImpl.class),
                    ClassName.get(id),
                    ClassName.get(clazz)
            );

            ParameterizedTypeName repository = ParameterizedTypeName.get(
                    ClassName.get(Repository.class),
                    ClassName.get(id),
                    ClassName.get(clazz)
            );
//
//            MethodSpec constructor = MethodSpec.constructorBuilder()
////                    .addAnnotation(JavaPoets.generated(AutoServiceGenerator.class))
//                    .addParameter(
//                            ParameterSpec.builder(repository, "repository")
//                                    .addModifiers(Modifier.FINAL).build())
//                    .addStatement("super(repository)")
//                    .build();

            //  class EntityServiceImpl extends AbsServiceImpl<I, IEntity> implements EntityService
            TypeSpec service = TypeSpec.classBuilder(serviceImplName)
                    .superclass(parameterizedTypeName)
                    .addSuperinterface(ClassName.get(servicePackage, serviceName))
//                    .addMethod(constructor)
                    .addAnnotation(Service.class)
                    .addAnnotation(JavaPoets.generated(getClass()))
                    .addJavadoc(JavaPoets.Javadoc.author())
                    .addJavadoc(JavaPoets.Javadoc.version())
                    .build();

            return JavaFile.builder(servicePackage, service)
                    .skipJavaLangImports(true)
                    .indent("    ")
                    .build();

        } finally {
            logger.info("generated service: {}.{}", serviceImplPackage, serviceImplName);
        }
    }
}


