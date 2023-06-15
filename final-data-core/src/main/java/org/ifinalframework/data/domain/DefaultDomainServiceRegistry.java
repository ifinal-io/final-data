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

package org.ifinalframework.data.domain;

import lombok.Setter;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.annotation.DomainResource;
import org.ifinalframework.data.domain.spi.LoggerAfterConsumer;
import org.ifinalframework.data.service.AbsService;
import org.ifinalframework.util.CompositeProxies;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DefaultDomainResourceServiceRegistry.
 *
 * @author ilikly
 * @version 1.5.0
 * @since 1.5.0
 */
@Component
public class DefaultDomainServiceRegistry implements DomainServiceRegistry, ApplicationContextAware, SmartInitializingSingleton {
    private final Map<String, DomainService<Long, IEntity<Long>, IUser<?>>> domainServiceMap = new LinkedHashMap<>();

    @Setter
    private ApplicationContext applicationContext;
    @Resource
    private List<LoggerAfterConsumer> loggerAfterConsumers;

    @Override
    public <ID extends Serializable, T extends IEntity<ID>, U extends IUser<?>> DomainService<ID, T, U> getDomainService(String resource) {
        return (DomainService<ID, T, U>) domainServiceMap.get(resource);
    }

    @Override
    public void afterSingletonsInstantiated() {

        String userClassName = applicationContext.getEnvironment().getRequiredProperty("final.security.user-class");

        Class<?> userClass = ClassUtils.resolveClassName(userClassName, getClass().getClassLoader());

        final DomainServiceFactory domainServiceFactory = new DefaultDomainServiceFactory(userClass, applicationContext, CompositeProxies.composite(LoggerAfterConsumer.class,loggerAfterConsumers));

        applicationContext.getBeanProvider(AbsService.class).stream()
                .forEach(service -> {
                    Class<?> entityClass = ResolvableType.forClass(AopUtils.getTargetClass(service)).as(AbsService.class).resolveGeneric(1);
                    final DomainResource domainResource = AnnotationUtils.findAnnotation(entityClass, DomainResource.class);

                    if (Objects.nonNull(domainResource)) {
                        DomainService domainService = domainServiceFactory.create(service);
                        for (final String resource : domainResource.value()) {
                            domainServiceMap.put(resource, domainService);
                        }
                    }


                });
    }
}
