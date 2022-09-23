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
import java.beans.Introspector;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;

/**
 * CreateRestControllerMethodProvider.
 * <pre class="code">
 *      &#64;PostMapping
 *      public Long create(&#64;Valid &#64;RequestBody Entity entity){
 *          entityService.insert(entity);
 *          return entity.getId();
 *      }
 * </pre>
 *
 * @author ilikly
 * @version 1.4.1
 * @since 1.4.1
 */
public class CreateRestControllerMethodProvider implements RestControllerMethodProvider {
    @Override
    public MethodSpec provide(Class<?> clazz, String service) {

        String entityName = Introspector.decapitalize(clazz.getSimpleName());

        ParameterSpec entity = ParameterSpec.builder(ClassName.get(clazz), entityName)
                .addAnnotation(Valid.class)
                .addAnnotation(RequestBody.class)
                .build();

        return MethodSpec.methodBuilder("create")
                .addAnnotation(PostMapping.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(Long.class))
                .addParameter(entity)
                .addCode("$L.insert($L);\n", service, entityName)
                .addCode("return $L.getId();", entityName)
                .build();
    }
}


