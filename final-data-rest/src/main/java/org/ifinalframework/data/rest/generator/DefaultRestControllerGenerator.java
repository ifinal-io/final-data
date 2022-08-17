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

package org.ifinalframework.data.rest.generator;

import javax.annotation.Resource;
import javax.lang.model.element.Modifier;

import java.beans.Introspector;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.data.rest.util.RestUtils;
import org.ifinalframework.data.service.util.ServiceUtil;
import org.ifinalframework.javapoets.JavaPoets;

import com.squareup.javapoet.*;
import lombok.SneakyThrows;

/**
 * DefaultRestControllerGenerator.
 *
 * @author ilikly
 * @version 1.4.0
 * @since 1.4.0
 */
public class DefaultRestControllerGenerator implements RestControllerGenerator {
    @Override
    @SneakyThrows
    public <T extends IEntity<?>> String generate(Class<T> clazz) {

        final String path = "path";
        final String packageName = RestUtils.packageName(clazz);
        final String controllerName = RestUtils.controllerName(clazz);

        // @RequestMapping("/api/${path}")
        AnnotationSpec resultMapping = AnnotationSpec.builder(RequestMapping.class)
                .addMember("value", "$S", "/api/" + path)
                .build();


        // public class EntityController
        TypeSpec controller = TypeSpec.classBuilder(controllerName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(RestController.class)
                .addAnnotation(resultMapping)
//                .addAnnotation(JavaPoets.generated(AutoMapperGeneratorProcessor.class))
                .addJavadoc(JavaPoets.Javadoc.author())
                .addJavadoc(JavaPoets.Javadoc.version())
                .addField(serviceField(clazz))

                .addMethod(postMethod(clazz))
                .addMethod(putMethod(clazz))
                .addMethod(deleteMethod(clazz))

                .build();

        JavaFile javaFile = JavaFile.builder(packageName, controller)
                .skipJavaLangImports(true)
                .build();

        StringBuilder out = new StringBuilder();
        javaFile.writeTo(out);
        return out.toString();

    }

    private FieldSpec serviceField(Class<?> clazz) {
        String packageName = ServiceUtil.packageName(clazz);
        String serviceName = ServiceUtil.serviceName(clazz);
        String fieldName = Introspector.decapitalize(serviceName);
        return FieldSpec.builder(ClassName.get(packageName,serviceName), fieldName)
                .addAnnotation(Resource.class)
                .addModifiers(Modifier.PRIVATE)
                .build();
    }

    private MethodSpec postMethod(Class<?> clazz) {

        ParameterSpec entity = ParameterSpec.builder(ClassName.get(clazz), "entity")
                .addAnnotation(RequestBody.class)
                .build();

        String serviceName = ServiceUtil.serviceName(clazz);
        String fieldName = Introspector.decapitalize(serviceName);

        return MethodSpec.methodBuilder("post")
                .addAnnotation(PostMapping.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addParameter(entity)
                .addCode("return $L.insert(entity);",fieldName)
                .build();
    }

    public MethodSpec putMethod(Class<?> clazz){
        AnnotationSpec putMapping = AnnotationSpec.builder(PutMapping.class)
                .addMember("value", "$S", "/{id}")
                .build();


        ParameterSpec entity = ParameterSpec.builder(ClassName.get(clazz), "entity")
                .addAnnotation(RequestBody.class)
                .build();

        ParameterSpec id = ParameterSpec.builder(ClassName.get(Long.class),"id")
                .addAnnotation(PathVariable.class)
                .build();

        String serviceName = ServiceUtil.serviceName(clazz);
        String fieldName = Introspector.decapitalize(serviceName);

        return MethodSpec.methodBuilder("put")
                .addAnnotation(putMapping)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addParameter(id)
                .addParameter(entity)
                .addCode("return $L.update(entity);",fieldName)
                .build();
    }

    public MethodSpec deleteById(Class<?> clazz){
        AnnotationSpec deleteMapping = AnnotationSpec.builder(DeleteMapping.class)
                .addMember("value", "$S", "/{id}")
                .build();

        ParameterSpec id = ParameterSpec.builder(ClassName.get(Long.class),"id")
                .addAnnotation(PathVariable.class)
                .build();

        String serviceName = ServiceUtil.serviceName(clazz);
        String fieldName = Introspector.decapitalize(serviceName);

        return MethodSpec.methodBuilder("delete")
                .addAnnotation(deleteMapping)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addParameter(id)
                .addCode("return $L.delete(entity);",fieldName)
                .build();
    }
    public MethodSpec deleteMethod(Class<?> clazz){

        ParameterizedTypeName listIds = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.get(Long.class)
        );

        ParameterSpec ids = ParameterSpec.builder(listIds,"ids")
                .addAnnotation(RequestBody.class)
                .build();

        String serviceName = ServiceUtil.serviceName(clazz);
        String fieldName = Introspector.decapitalize(serviceName);

        return MethodSpec.methodBuilder("delete")
                .addAnnotation(DeleteMapping.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addParameter(ids)
                .addCode("return $L.delete(ids);",fieldName)
                .build();
    }


}


