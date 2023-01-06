/*
 * Copyright 2020-2023 the original author or authors.
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

package org.ifinalframework.data.core;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import org.ifinalframework.data.annotation.Tenant;
import org.ifinalframework.data.util.TableUtils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * DefaultTenantTableService.
 *
 * @author ilikly
 * @version 1.4.3
 * @since 1.4.3
 */
@Slf4j
@Component
public class DefaultTenantTableService implements TenantTableService, BeanFactoryAware, InitializingBean {

    private final Set<String> tenantTables = new LinkedHashSet<>();

    @Setter
    private BeanFactory beanFactory;

    @Override
    public boolean isTenantTable(String table) {
        return tenantTables.contains(table);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<String> packages = AutoConfigurationPackages.get(beanFactory);

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        ClassLoader classLoader = beanFactory.getClass().getClassLoader();
        scanner.setResourceLoader(new PathMatchingResourcePatternResolver(classLoader));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Tenant.class));

        Set<String> tenantEntityTables = packages.stream()
                .flatMap(it -> scanner.findCandidateComponents(it).stream())
                .map(BeanDefinition::getBeanClassName)
                .filter(Objects::nonNull)
                .map(it -> ClassUtils.resolveClassName(it, classLoader))
                .flatMap(it -> TableUtils.getTables(it).stream())
                .collect(Collectors.toSet());

        this.tenantTables.addAll(tenantEntityTables);

        logger.info("found tenantTables: {}", tenantTables);

    }
}
