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

package org.ifinalframework.data.auto.generator.method;

import javax.lang.model.element.Modifier;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.ifinalframework.data.auto.generator.RestControllerMethodProvider;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

/**
 * DeleteRestControllerMethodProvider.
 *
 * @author ilikly
 * @version 1.4.1
 * @since 1.4.1
 */
public class DeleteRestControllerMethodProvider implements RestControllerMethodProvider {
    @Override
    public MethodSpec provide(Class<?> clazz, String service) {
        AnnotationSpec deleteMapping = AnnotationSpec.builder(DeleteMapping.class)
                .addMember("value", "$S", "/{id}")
                .build();

        ParameterSpec id = ParameterSpec.builder(ClassName.get(Long.class), "id")
                .addAnnotation(PathVariable.class)
                .build();


        return MethodSpec.methodBuilder("delete")
                .addAnnotation(deleteMapping)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addParameter(id)
                .addCode("return $L.delete(id);", service)
                .build();
    }
}


