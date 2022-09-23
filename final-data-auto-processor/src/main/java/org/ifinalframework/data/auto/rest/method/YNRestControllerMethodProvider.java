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

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.ifinalframework.data.annotation.YN;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

/**
 * <pre class="code">
 * &#64;PutMapping("/{id}/yn")
 * public int yn(&#64;PathVariable Long id, &#64;RequestParam YN yn){
 *     Entity entity = new Entity();
 *     entity.setId(id);
 *     entity.setYn(yn);
 *     return entityService.update(entity);
 * }
 * </pre>
 */
public class YNRestControllerMethodProvider implements RestControllerMethodProvider {

    @Override
    public MethodSpec provide(Class<?> clazz, String service) {
        // @PostMapping
        AnnotationSpec putMapping = AnnotationSpec.builder(PutMapping.class)
                .addMember("value", "$S", "/{id}/yn")
                .build();

        // @PathVariable Long id
        ParameterSpec id = ParameterSpec.builder(ClassName.get(Long.class), "id")
                .addAnnotation(PathVariable.class)
                .build();
        // @RequestParam YN yn
        ParameterSpec yn = ParameterSpec.builder(ClassName.get(YN.class), "yn")
                .addAnnotation(RequestParam.class)
                .build();


        String entityName = clazz.getSimpleName();
        return MethodSpec.methodBuilder("update")
                .addAnnotation(putMapping)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addParameter(id)
                .addParameter(yn)
                .addCode("$L entity = new $L();\n", entityName, entityName)
                .addCode("entity.setId(id);\n")
                .addCode("entity.setYn(yn);\n")
                .addCode("return $L.update(entity);", service)
                .build();
    }
}


