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

package org.ifinalframework.data.auto.rest.method;

import javax.lang.model.element.Modifier;
import javax.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

/**
 * UpdateRestControllerMethodProvider.
 *
 * @author ilikly
 * @version 1.4.1
 * @since 1.4.1
 */
public class UpdateRestControllerMethodProvider implements RestControllerMethodProvider {
    @Override
    public MethodSpec provide(Class<?> clazz, String service) {
        AnnotationSpec putMapping = AnnotationSpec.builder(PutMapping.class)
                .addMember("value", "$S", "/{id}")
                .build();


        ParameterSpec entity = ParameterSpec.builder(ClassName.get(clazz), "entity")
                .addAnnotation(Valid.class)
                .addAnnotation(RequestBody.class)
                .build();

        ParameterSpec id = ParameterSpec.builder(ClassName.get(Long.class), "id")
                .addAnnotation(PathVariable.class)
                .build();


        return MethodSpec.methodBuilder("update")
                .addAnnotation(putMapping)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addParameter(id)
                .addParameter(entity)
                .addCode("entity.setId(id);\n")
                .addCode("return $L.update(entity);", service)
                .build();
    }
}


