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

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IEnum;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.annotation.AbsEntity;
import org.ifinalframework.data.annotation.AbsRecord;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.spi.PreInsertFunction;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * DomainServiceTest.
 *
 * @author ilikly
 * @version 1.5.0
 * @since 1.5.0
 */
@Slf4j
class DomainServiceTest {



    @Test
    @SneakyThrows
    void test() {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        final DomainServiceInterfaceFactory domainServiceInterfaceFactory = new DomainServiceInterfaceFactory();

        final AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(domainServiceInterfaceFactory.create(Long.class,AbsEntity.class));
        beanDefinition.setBeanClass(DomainServiceFactoryBean.class);
        beanDefinition.getPropertyValues().addPropertyValue("entityClass", AbsEntity.class);
        context.registerBeanDefinition("a", beanDefinition);

        final AnnotatedGenericBeanDefinition bbeanDefinition = new AnnotatedGenericBeanDefinition(domainServiceInterfaceFactory.create(Long.class,AbsRecord.class));
        bbeanDefinition.setBeanClass(DomainServiceFactoryBean.class);
        bbeanDefinition.getPropertyValues().addPropertyValue("entityClass", AbsRecord.class);
        context.registerBeanDefinition("b", bbeanDefinition);

//        context.registerBeanDefinition("d",new AnnotatedGenericBeanDefinition(DomainServiceHolder.class));


        context.refresh();

        final Map<String, DomainService> beansOfType = context.getBeansOfType(DomainService.class);


//        final DomainServiceHolder domainServiceHolder = context.getBean(DomainServiceHolder.class);
//        Assertions.assertNotNull(domainServiceHolder.getDomainService());
    }

    @Getter
    @RequiredArgsConstructor
    protected static class DomainServiceHolder{

        private final DomainService<Long,AbsEntity> domainService;
    }

    protected static class DomainServiceFactoryBean implements FactoryBean<DomainService>, SmartInitializingSingleton {

        @Setter
        private Class<?> entityClass;

        @Nullable
        @Override
        public DomainService getObject() throws Exception {
            logger.info("CREATE DomainService:{}" + entityClass);
            return new DomainService() {
                @NonNull
                @Override
                public Class entityClass() {
                    return null;
                }

                @Nullable
                @Override
                public Class<?> domainEntityClass(Class prefix) {
                    return null;
                }

                @NonNull
                @Override
                public Class<? extends IQuery> domainQueryClass(Class prefix) {
                    return null;
                }

                @Nullable
                @Override
                public PreInsertFunction preInsertFunction() {
                    return null;
                }

                @Override
                public Object create(@NonNull List entities, @NonNull IUser user) {
                    return null;
                }

                @Override
                public Object list(@NonNull IQuery query, @NonNull IUser user) {
                    return null;
                }

                @Override
                public Object detail(@NonNull IQuery query, @NonNull IUser user) {
                    return null;
                }

                @Override
                public Object detail(@NonNull Serializable serializable, @NonNull IUser user) {
                    return null;
                }

                @Override
                public Long count(@NonNull IQuery query, @NonNull IUser user) {
                    return null;
                }

                @Override
                public Object delete(@NonNull IQuery query, @NonNull IUser user) {
                    return null;
                }

                @Override
                public Object delete(@NonNull Serializable serializable, @NonNull IUser user) {
                    return null;
                }

                @Override
                public int update(@NonNull IEntity entity, @NonNull Serializable serializable, boolean selective, @NonNull IUser user) {
                    return 0;
                }

                @Override
                public Object yn(@NonNull Serializable serializable, @NonNull YN yn, @NonNull IUser user) {
                    return null;
                }

                @Override
                public Object status(@NonNull Serializable serializable, @NonNull IEnum status, @NonNull IUser user) {
                    return null;
                }

                @Override
                public Object lock(@NonNull Serializable serializable, @NonNull Boolean locked, @NonNull IUser user) {
                    return null;
                }
            };
        }

        @Nullable
        @Override
        public Class<?> getObjectType() {
            return DomainService.class;
        }

        @Override
        public void afterSingletonsInstantiated() {
            logger.info("afterSingletonsInstantiated");
        }
    }

}