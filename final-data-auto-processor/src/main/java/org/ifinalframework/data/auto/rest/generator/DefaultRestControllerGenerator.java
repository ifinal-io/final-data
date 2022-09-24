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

package org.ifinalframework.data.auto.rest.generator;

import javax.annotation.Resource;
import javax.lang.model.element.Modifier;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.data.auto.annotation.RestResource;
import org.ifinalframework.data.auto.rest.method.CountRestControllerMethodProvider;
import org.ifinalframework.data.auto.rest.method.CreateRestControllerMethodProvider;
import org.ifinalframework.data.auto.rest.method.DeleteRestControllerMethodProvider;
import org.ifinalframework.data.auto.rest.method.QueryDetailRestControllerMethodProvider;
import org.ifinalframework.data.auto.rest.method.QueryRestControllerMethodProvider;
import org.ifinalframework.data.auto.rest.method.RestControllerMethodProvider;
import org.ifinalframework.data.auto.rest.method.UpdateRestControllerMethodProvider;
import org.ifinalframework.data.auto.rest.method.YNRestControllerMethodProvider;
import org.ifinalframework.data.auto.util.RestUtils;
import org.ifinalframework.data.service.util.ServiceUtil;
import org.ifinalframework.javapoets.JavaPoets;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import lombok.SneakyThrows;

/**
 * DefaultRestControllerGenerator.
 *
 * <pre class="code">
 * &#64;RestController
 * &#64;RequestMapping("/{prefix}/{resource}")
 * public class EntityRestController{
 *
 *      &#64;Resource
 *      private EntityService entityService;
 *
 *      &#64;GetMapping
 *      public List&lt;Entity&gt; query(&#64;Valid EntityQuery query){
 *          return entityService.select(query);
 *      }
 *
 *      &#64;PostMapping
 *      public Long create(&#64;Valid &#64;RequestBody Entity entity){
 *          entityService.insert(entity);
 *          return entity.getId();
 *      }
 *
 *      &#64;PutMapping("/{id})
 *      public int update(&#64;PathVariable Long id, &#64;RequestBody Entity entity){
 *          entity.setId(id);
 *          return entityService.update(entity);
 *      }
 *
 *      &#64;DeleteMapping("/{id}")
 *      public int delete(&#64;PathVariable Long id){
 *          return entityService.delete(id);
 *      }
 *
 * }
 * </pre>
 *
 * @author ilikly
 * @version 1.4.0
 * @since 1.4.0
 */
public class DefaultRestControllerGenerator implements RestControllerGenerator {

    private final List<RestControllerMethodProvider> providers = new ArrayList<>();

    public DefaultRestControllerGenerator() {
        // select
        providers.add(new QueryRestControllerMethodProvider());
        providers.add(new CountRestControllerMethodProvider());
        providers.add(new QueryDetailRestControllerMethodProvider());
        // create
        providers.add(new CreateRestControllerMethodProvider());
        // update
        providers.add(new UpdateRestControllerMethodProvider());
        providers.add(new YNRestControllerMethodProvider());
        // delete
        providers.add(new DeleteRestControllerMethodProvider());
    }

    @Override
    @SneakyThrows
    public <T extends IEntity<?>> String generate(Class<T> clazz) {
        final String path = Optional.ofNullable(clazz.getAnnotation(RestResource.class)).map(RestResource::value).orElse(clazz.getSimpleName());
        final String packageName = RestUtils.packageName(clazz);
        final String controllerName = RestUtils.controllerName(clazz);

        // @RequestMapping("/api/${path}")
        AnnotationSpec resultMapping = AnnotationSpec.builder(RequestMapping.class)
                .addMember("value", "$S", "/api/" + path)
                .build();


        // public class EntityController
        TypeSpec.Builder builder = TypeSpec.classBuilder(controllerName);

        String serviceName = Introspector.decapitalize(ServiceUtil.serviceName(clazz));

        for (RestControllerMethodProvider provider : providers) {
            builder.addMethod(provider.provide(clazz, serviceName));
        }


        TypeSpec controller = builder
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Validated.class)
                .addAnnotation(RestController.class)
                .addAnnotation(resultMapping)
//                .addAnnotation(JavaPoets.generated(AutoMapperGeneratorProcessor.class))
                .addJavadoc(JavaPoets.Javadoc.author())
                .addJavadoc(JavaPoets.Javadoc.version())
                .addField(serviceField(clazz))

                .build();

        JavaFile javaFile = JavaFile.builder(packageName, controller)
                .skipJavaLangImports(true)
                .indent("    ")
                .build();

        StringBuilder out = new StringBuilder();
        javaFile.writeTo(out);
        return out.toString();

    }

    private FieldSpec serviceField(Class<?> clazz) {
        String packageName = ServiceUtil.packageName(clazz);
        String serviceName = ServiceUtil.serviceName(clazz);
        String fieldName = Introspector.decapitalize(serviceName);
        return FieldSpec.builder(ClassName.get(packageName, serviceName), fieldName)
                .addAnnotation(Resource.class)
                .addModifiers(Modifier.PRIVATE)
                .build();
    }


}


