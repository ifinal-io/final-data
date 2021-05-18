/*
 * Copyright 2020-2021 the original author or authors.
 *
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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ReflectionUtils;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.lang.Transient;
import org.ifinalframework.data.mybatis.handler.EnumTypeHandler;
import org.ifinalframework.data.mybatis.mapper.AbsMapper;
import org.ifinalframework.data.mybatis.resumtmap.ResultMapFactory;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;

/**
 * @author likly
 * @version 1.0.0
 * @see org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration
 * @since 1.0.0
 */
@Slf4j
@org.springframework.context.annotation.Configuration
//@ConditionalOnMissingBean(FinalMybatisConfigurationCustomizer.class)
public class FinalMybatisConfigurationCustomizer implements ConfigurationCustomizer, BeanFactoryAware,
    InitializingBean {

    private static final Field composites = Objects
        .requireNonNull(ReflectionUtils.findField(ResultMapping.class, "composites"));

    static {
        ReflectionUtils.makeAccessible(composites);
    }

    @Setter
    private BeanFactory beanFactory;

    @Setter
    private List<String> packages;

    @Override
    public void customize(final Configuration configuration) {

        logger.info("setDefaultEnumTypeHandler:{}", EnumTypeHandler.class.getCanonicalName());
        configuration.addMapper(AbsMapper.class);
        configuration.getTypeHandlerRegistry().setDefaultEnumTypeHandler(EnumTypeHandler.class);

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
            false);

        scanner.addIncludeFilter(new AssignableTypeFilter(IEntity.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(Transient.class));

        Set<BeanDefinition> entities = new LinkedHashSet<>();
        packages.forEach(it -> entities.addAll(scanner.findCandidateComponents(it)));

        entities.stream()
            .map(BeanDefinition::getBeanClassName)
            .map(className -> {
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(className);
                }
            })
            .forEach(clazz -> {
                ResultMap resultMap = ResultMapFactory.from(configuration, clazz);

                if (logger.isInfoEnabled()) {
                    logger.info("==> addResultMap:[{}],class={}", resultMap.getId(), clazz);
                }

                configuration.addResultMap(resultMap);

                resultMap.getResultMappings()
                    .stream()
                    .filter(ResultMapping::isCompositeResult)
                    .forEach(resultMapping -> {

                        ResultMap map = new ResultMap.Builder(configuration, resultMapping.getNestedResultMapId(),
                            resultMap.getType(),
                            resultMapping.getComposites()).build();
                        configuration.addResultMap(map);

                        // mybatis not support composites result mapping
                        ReflectionUtils.setField(composites, resultMapping, null);

                    });
            });

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.packages = AutoConfigurationPackages.get(this.beanFactory);
    }

}

