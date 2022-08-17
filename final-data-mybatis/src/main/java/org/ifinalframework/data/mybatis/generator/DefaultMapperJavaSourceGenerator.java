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

package org.ifinalframework.data.mybatis.generator;

import javax.lang.model.element.Modifier;

import org.apache.ibatis.annotations.Mapper;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.data.mapping.Entity;
import org.ifinalframework.data.mybatis.mapper.AbsMapper;
import org.ifinalframework.data.mybatis.util.MapperUtils;
import org.ifinalframework.javapoets.JavaPoets;

import com.squareup.javapoet.*;
import lombok.SneakyThrows;

/**
 * DefaultMapperGenerator.
 *
 * @author ilikly
 * @version 1.4.0
 * @since 1.4.0
 */
public class DefaultMapperJavaSourceGenerator implements MapperJavaSourceGenerator {

    @Override
    @SneakyThrows
    public <T extends IEntity<?>> String generate(Class<T> clazz) {

        final String packageName = MapperUtils.packageName(clazz);
        final String mapperName = MapperUtils.mapperName(clazz);


        Entity<T> entity = Entity.from(clazz);

        // AbsMapper<I,IEntity>
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
                ClassName.get(AbsMapper.class),
                TypeName.get(entity.getRequiredIdProperty().getType()),
                ClassName.get(clazz)
        );

        // public interface EntityMapper extends AbsMapper<I,IEntity>
        TypeSpec myMapper = TypeSpec.interfaceBuilder(mapperName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(parameterizedTypeName)
                .addAnnotation(Mapper.class)
//                .addAnnotation(JavaPoets.generated(AutoMapperGeneratorProcessor.class))
                .addJavadoc(JavaPoets.Javadoc.author())
                .addJavadoc(JavaPoets.Javadoc.version())
                .build();

        JavaFile javaFile = JavaFile.builder(packageName, myMapper)
                .skipJavaLangImports(true)
                .build();

        StringBuilder out = new StringBuilder();
        javaFile.writeTo(out);
        return out.toString();
    }
}


