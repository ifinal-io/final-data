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
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;

import org.ifinalframework.data.service.util.ServiceUtil;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;

/**
 * QueryRestControllerMethodProvider.
 *
 * <pre class="code">
 *     &#64;GetMapping
 *     public List&lt;Entity&gt; query(&#64;Valid EntityQuery query){
 *         return entityService.select(query);
 *     }
 * </pre>
 *
 * @author ilikly
 * @version 1.4.1
 * @since 1.4.1
 */
public class QueryRestControllerMethodProvider implements RestControllerMethodProvider {
    @Override
    public MethodSpec provide(Class<?> clazz, String service) {

        ParameterizedTypeName returnValue = ParameterizedTypeName.get(List.class, clazz);

        ParameterSpec query = ParameterSpec.builder(ClassName.get(ServiceUtil.queryPackageName(clazz), ServiceUtil.queryName(clazz)), "query")
                .addAnnotation(Valid.class)
                .build();


        return MethodSpec.methodBuilder("query")
                .addAnnotation(GetMapping.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(returnValue)
                .addParameter(query)
                .addCode("return $L.select(query);", service)
                .build();
    }
}


