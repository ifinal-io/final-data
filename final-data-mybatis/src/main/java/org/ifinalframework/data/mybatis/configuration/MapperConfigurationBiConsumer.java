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

package org.ifinalframework.data.mybatis.configuration;

import org.apache.ibatis.session.Configuration;

import org.springframework.util.ClassUtils;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.data.mybatis.generator.DefaultMapperJavaSourceGenerator;
import org.ifinalframework.data.mybatis.generator.MapperJavaSourceGenerator;
import org.ifinalframework.data.mybatis.util.MapperUtils;
import org.ifinalframework.java.compiler.Compiler;
import org.ifinalframework.java.compiler.DynamicClassLoader;

import lombok.extern.slf4j.Slf4j;

/**
 * MapperConfigurationBiConsumer.
 *
 * @author ilikly
 * @version 1.4.0
 * @since 1.4.0
 */
@Slf4j
public class MapperConfigurationBiConsumer implements ConfigurationBiConsumer {

    private final MapperJavaSourceGenerator mapperJavaSourceGenerator = new DefaultMapperJavaSourceGenerator();

    @Override
    public void accept(Configuration configuration, Class<? extends IEntity<?>> clazz) {
        String mapperClassName = MapperUtils.mapperClassName(clazz);

        if (ClassUtils.isPresent(mapperClassName, clazz.getClassLoader())) {
            return;
        }

        String source = mapperJavaSourceGenerator.generate(clazz);


        Compiler compiler = new Compiler(clazz.getClassLoader());
        compiler.addSource(mapperClassName, source);
        DynamicClassLoader dynamicClassLoader = compiler.compile();
        try {
            Class<?> mapperClass = dynamicClassLoader.getClasses().get(mapperClassName);
            logger.info("==> addMapper: {}", mapperClass.getName());
            configuration.addMapper(mapperClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }


    }
}


