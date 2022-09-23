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
import javax.validation.Valid;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.data.auto.annotation.RestResource;
import org.ifinalframework.data.auto.util.RestUtils;
import org.ifinalframework.data.service.util.ServiceUtil;
import org.ifinalframework.javapoets.JavaPoets;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.SneakyThrows;

/**
 * DefaultRestControllerGenerator.
 *
 * <pre class="code">
 * &#64;RestController
 * &#64;RequestMapping("/{prefix}/{resource}")
 * public class EntityController{
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
        providers.add(new QueryRestControllerMethodProvider());
        providers.add(new CreateRestControllerMethodProvider());
        providers.add(new UpdateRestControllerMethodProvider());
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
                .addAnnotation(RestController.class)
                .addAnnotation(resultMapping)
//                .addAnnotation(JavaPoets.generated(AutoMapperGeneratorProcessor.class))
                .addJavadoc(JavaPoets.Javadoc.author())
                .addJavadoc(JavaPoets.Javadoc.version())
                .addField(serviceField(clazz))

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
        return FieldSpec.builder(ClassName.get(packageName, serviceName), fieldName)
                .addAnnotation(Resource.class)
                .addModifiers(Modifier.PRIVATE)
                .build();
    }


    /**
     * UpdateRestControllerMethodProvider.
     *
     * @author ilikly
     * @version 1.4.1
     * @since 1.4.1
     */
    public static class UpdateRestControllerMethodProvider implements RestControllerMethodProvider {
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
                    .addCode("entity.setId(id);")
                    .addCode("return $L.update(entity);", service)
                    .build();
        }
    }

    /**
     * CreateRestControllerMethodProvider.
     *
     * @author ilikly
     * @version 1.4.1
     * @since 1.4.1
     */
    public static class CreateRestControllerMethodProvider implements RestControllerMethodProvider {
        @Override
        public MethodSpec provide(Class<?> clazz, String service) {
            ParameterSpec entity = ParameterSpec.builder(ClassName.get(clazz), "entity")
                    .addAnnotation(Valid.class)
                    .addAnnotation(RequestBody.class)
                    .build();

            return MethodSpec.methodBuilder("create")
                    .addAnnotation(PostMapping.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ClassName.get(Long.class))
                    .addParameter(entity)
                    .addCode("$L.insert(entity);", service)
                    .addCode("return entity.getId();")
                    .build();
        }
    }

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
    public static class QueryRestControllerMethodProvider implements RestControllerMethodProvider {
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

    /**
     * DeleteRestControllerMethodProvider.
     *
     * @author ilikly
     * @version 1.4.1
     * @since 1.4.1
     */
    public static class DeleteRestControllerMethodProvider implements RestControllerMethodProvider {
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
                    .addCode("return $L.delete(entity);", service)
                    .build();
        }
    }
}


