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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IUser;
import org.ifinalframework.core.IView;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.core.AutoNameHelper;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.AfterReturnQueryConsumer;
import org.ifinalframework.data.spi.AfterReturningConsumer;
import org.ifinalframework.data.spi.AfterThrowingConsumer;
import org.ifinalframework.data.spi.AfterThrowingQueryConsumer;
import org.ifinalframework.data.spi.Consumer;
import org.ifinalframework.data.spi.Filter;
import org.ifinalframework.data.spi.PostQueryConsumer;
import org.ifinalframework.data.spi.PostUpdateYNConsumer;
import org.ifinalframework.data.spi.PreInsertFunction;
import org.ifinalframework.data.spi.PreInsertValidator;
import org.ifinalframework.data.spi.PreQueryConsumer;
import org.ifinalframework.data.spi.PreQueryPredicate;
import org.ifinalframework.data.spi.PreUpdateYnValidator;
import org.ifinalframework.data.spi.SpiAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DefaultDomainServiceFactory.
 *
 * @author ilikly
 * @version 1.4.3
 * @since 1.4.3
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultDomainResourceServiceFactory implements DomainResourceServiceFactory {

    private final Class<? extends IUser<?>> userClass;
    private final ApplicationContext applicationContext;

    @Override
    @SuppressWarnings("unchecked,rawtypes")
    public <ID extends Serializable, T extends IEntity<ID>> DomainResourceService<ID, T> create(Repository<ID, T> repository) {
        DefaultDomainResourceService.DefaultDomainResourceServiceBuilder<ID, T> builder = DefaultDomainResourceService.builder();
        builder.repository(repository);
        ResolvableType repositoryResolvableType = ResolvableType.forClass(AopUtils.getTargetClass(repository)).as(Repository.class);
        Class<?> entityClass = Objects.requireNonNull(repositoryResolvableType.resolveGeneric(1));
        builder.entityClass((Class<T>) entityClass);
        ClassLoader classLoader = entityClass.getClassLoader();

        // entity
        final Map<Class<?>, Class<?>> domainEntityClassMap = new LinkedHashMap<>();
        builder.domainClassMap(domainEntityClassMap);

        // query
        final Map<Class<?>, Class<? extends IQuery>> queryClassMap = new LinkedHashMap<>();
        builder.queryClassMap(queryClassMap);
        final String queryPackage = AutoNameHelper.queryPackage(entityClass);
        final String defaultQueryName = AutoNameHelper.queryName(entityClass);
        final String defaultQueryClassName = String.join(".", queryPackage, defaultQueryName);
        final Class<?> defaultqueryClass = ClassUtils.resolveClassName(defaultQueryClassName, classLoader);

        // create

        String dtoClassName = AutoNameHelper.dtoClassName(entityClass, IView.Create.class.getSimpleName());

        if (ClassUtils.isPresent(dtoClassName, entityClass.getClassLoader())) {
            Class<?> dtoClass = ClassUtils.resolveClassName(dtoClassName, entityClass.getClassLoader());
            domainEntityClassMap.put(IView.Create.class, dtoClass);
            PreInsertFunction preInsertFunction = (PreInsertFunction) applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(PreInsertFunction.class, dtoClass, userClass, entityClass)).getObject();
            builder.preInsertFunction(preInsertFunction);

        }

        builder.preInsertFilter(new FilterComposite<>(getBeansOf(Filter.class, entityClass, userClass)));
        builder.preInsertConsumer(new ConsumerComposite<>(getBeansOf(SpiAction.PRE_CREATE, Consumer.class, entityClass, userClass)));
        builder.postInsertConsumer(new ConsumerComposite<>(getBeansOf(SpiAction.POST_CREATE, Consumer.class, entityClass, userClass)));
        builder.afterThrowingInsertConsumer(new AfterThrowingConsumerComposite<>(getBeansOf(SpiAction.CREATE, AfterThrowingConsumer.class, entityClass, userClass)));
        builder.afterReturningInsertConsumer(new AfterReturningConsumerComposite<>(getBeansOf(SpiAction.CREATE, AfterReturningConsumer.class, entityClass, Integer.class, userClass)));

        // delete
        final Class<?> deleteQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.Delete.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.Delete.class, (Class<? extends IQuery>) deleteQueryClass);
        builder.preDeleteQueryConsumer(new PreQueryConsumerComposite<>(getBeansOf(SpiAction.PRE_DELETE, PreQueryConsumer.class, deleteQueryClass, userClass)));
        builder.preDeleteConsumer(new ConsumerComposite<>(getBeansOf(SpiAction.PRE_DELETE, Consumer.class, entityClass, userClass)));
        builder.postDeleteQueryConsumer(new PostQueryConsumerComposite<>(getBeansOf(SpiAction.POST_DETAIL, PostQueryConsumer.class, entityClass, deleteQueryClass, userClass)));
        builder.postDeleteConsumer(new ConsumerComposite<>(getBeansOf(SpiAction.POST_DELETE, Consumer.class, entityClass, userClass)));

        // update
        builder.preUpdateConsumer(new ConsumerComposite<>(getBeansOf(SpiAction.PRE_UPDATE, Consumer.class, entityClass, userClass)));
        builder.postUpdateConsumer(new ConsumerComposite<>(getBeansOf(SpiAction.POST_UPDATE, Consumer.class, entityClass, userClass)));


        // list
        final Class<?> listQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.List.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.List.class, (Class<? extends IQuery>) listQueryClass);
        builder.preQueryConsumer(new PreQueryConsumerComposite(getBeansOf(SpiAction.PRE_LIST, PreQueryConsumer.class, listQueryClass, userClass)));
        builder.postQueryConsumer(new PostQueryConsumerComposite<>(getBeansOf(SpiAction.POST_LIST, PostQueryConsumer.class, entityClass, listQueryClass, userClass)));
        builder.afterThrowingQueryConsumer(new AfterThrowingQueryConsumerComposite<>(getBeansOf(SpiAction.POST_LIST, AfterThrowingQueryConsumer.class, entityClass, listQueryClass, userClass)));
        builder.afterReturnQueryConsumer(new AfterReturnQueryConsumerComposite<>(getBeansOf(SpiAction.POST_LIST, AfterReturnQueryConsumer.class, entityClass, listQueryClass, userClass)));

        // detail
        final Class<?> detailQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.Detail.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.Detail.class, (Class<? extends IQuery>) detailQueryClass);
        builder.preDetailQueryConsumer(new PreQueryConsumerComposite(getBeansOf(SpiAction.PRE_DETAIL, PreQueryConsumer.class, detailQueryClass, userClass)));
        builder.postDetailQueryConsumer(new PostQueryConsumerComposite<>(getBeansOf(SpiAction.POST_DETAIL, PostQueryConsumer.class, entityClass, detailQueryClass, userClass)));
        builder.postDetailConsumer(new ConsumerComposite<>(getBeansOf(SpiAction.POST_DETAIL, Consumer.class, entityClass, userClass)));

        // count
        final Class<?> countQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.Count.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.Count.class, (Class<? extends IQuery>) countQueryClass);
        builder.preCountQueryConsumer(new PreQueryConsumerComposite<>(getBeansOf(SpiAction.PRE_COUNT, PreQueryConsumer.class, countQueryClass, userClass)));

        // yn
        builder.preUpdateYnValidator(new PreUpdateYnValidatorComposite<>(getBeansOf(SpiAction.PRE_YN, PreUpdateYnValidator.class, entityClass, userClass)));
        builder.postUpdateYNConsumer(new PostUpdateYnConsumerComposite<>(getBeansOf(SpiAction.POST_YN, PostUpdateYNConsumer.class, entityClass, userClass)));
        return builder.build();
    }

    private static String buildClassName(String packageName, Class<?> prefix, String className) {
        return packageName + "." + prefix.getSimpleName() + className;
    }

    private static Class<?> resolveClass(ClassLoader classLoader, String className, Class<?> defaultClass) {
        if (ClassUtils.isPresent(className, classLoader)) {
            return ClassUtils.resolveClassName(className, classLoader);
        }
        return defaultClass;
    }

    private List getBeansOf(Class<?> type, Class<?>... generics) {
        return getBeansOf(null, type, generics);
    }

    private List getBeansOf(SpiAction action, Class<?> type, Class<?>... generics) {
        return applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(type, generics))
                .orderedStream()
                .filter(it -> {
                    final Class<?> targetClass = AopUtils.getTargetClass(it);
                    for (final String name : action.getValues()) {
                        if (targetClass.getSimpleName().contains(name)) {
                            logger.info("found type={} for action={}", type, action);
                            return true;
                        }
                    }
                    return false;

                })
                .collect(Collectors.toList());
    }

    /**
     * PostQueryConsumerComposite.
     *
     * @author ilikly
     * @version 1.4.2
     * @since 1.4.2
     */
    private static final class PostQueryConsumerComposite<T, Q, U> implements PostQueryConsumer<T, Q, U> {

        private final List<PostQueryConsumer<T, Q, U>> consumers;

        public PostQueryConsumerComposite(List<PostQueryConsumer<T, Q, U>> consumers) {
            this.consumers = consumers;
        }

        @Override
        public void accept(@NonNull SpiAction action, @NonNull List<T> entities, @NonNull Q query, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            for (PostQueryConsumer<T, Q, U> consumer : consumers) {
                consumer.accept(action, entities, query, user);
            }
        }

        @Override
        public void accept(@NonNull SpiAction action, @NonNull T entity, @NonNull Q query, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            for (PostQueryConsumer<T, Q, U> consumer : consumers) {
                consumer.accept(action, entity, query, user);
            }

        }
    }

    @RequiredArgsConstructor
    private static final class AfterThrowingQueryConsumerComposite<T, Q, U> implements AfterThrowingQueryConsumer<T, Q, U> {
        private final List<AfterThrowingQueryConsumer<T, Q, U>> consumers;

        @Override
        public void accept(@NonNull SpiAction action, @Nullable List<T> entities, @NonNull Q query, @NonNull U user, @NonNull Throwable e) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(action, entities, query, user, e));
        }

        @Override
        public void accept(@NonNull SpiAction action, @Nullable T entity, @NonNull Q query, @NonNull U user, @NonNull Throwable e) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(action, entity, query, user, e));

        }
    }

    @RequiredArgsConstructor
    private static final class AfterReturnQueryConsumerComposite<T, Q, U> implements AfterReturnQueryConsumer<T, Q, U> {
        private final List<AfterReturnQueryConsumer<T, Q, U>> consumers;

        @Override
        public void accept(@NonNull SpiAction action, @Nullable List<T> entities, @NonNull Q query, @NonNull U user, @Nullable Throwable e) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }

            consumers.forEach(it -> it.accept(action, entities, query, user, e));
        }

        @Override
        public void accept(@NonNull SpiAction action, @Nullable T entity, @NonNull Q query, @NonNull U user, @Nullable Throwable e) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(action, entity, query, user, e));
        }
    }


    /**
     * PreDeleteConsumerComposite.
     *
     * @author ilikly
     * @version 1.4.2
     * @see PreQueryConsumerComposite
     * @since 1.4.2
     */
    private static class ConsumerComposite<T, U> implements Consumer<T, U> {
        private final List<Consumer<T, U>> consumers;

        public ConsumerComposite(List<Consumer<T, U>> consumers) {
            this.consumers = consumers;
        }

        @Override
        public void accept(@NonNull SpiAction action, @NonNull List<T> entities, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(action, entities, user));
        }

        @Override
        public void accept(@NonNull SpiAction action, @NonNull T entity, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }

            for (Consumer<T, U> consumer : consumers) {
                consumer.accept(action, entity, user);
            }
        }
    }

    @RequiredArgsConstructor
    private static final class AfterThrowingConsumerComposite<T, U> implements AfterThrowingConsumer<T, U> {
        private final List<AfterThrowingConsumer<T, U>> consumers;

        @Override
        public void accept(@NonNull SpiAction action, @NonNull List<T> entities, @NonNull U user, @NonNull Throwable throwable) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(action, entities, user, throwable));
        }

        @Override
        public void accept(@NonNull SpiAction action, @NonNull T entity, @NonNull U user, @NonNull Throwable throwable) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(action, entity, user, throwable));
        }
    }

    @RequiredArgsConstructor
    private static final class AfterReturningConsumerComposite<T, R, U> implements AfterReturningConsumer<T, R, U> {
        private final List<AfterReturningConsumer<T, R, U>> consumers;

        @Override
        public void accept(@NonNull SpiAction action, @NonNull List<T> entities, @Nullable R result, @NonNull U user, @Nullable Throwable throwable) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(action, entities, result, user, throwable));
        }

        @Override
        public void accept(@NonNull SpiAction action, @NonNull T entity, @Nullable R result, @NonNull U user, @Nullable Throwable throwable) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(action, entity, result, user, throwable));
        }
    }

    @RequiredArgsConstructor
    private static class FilterComposite<T, U> implements Filter<T, U> {
        private final List<Filter<T, U>> filters;

        @Override
        public boolean test(@NonNull SpiAction action, @NonNull T entity, @NonNull U user) {

            if (CollectionUtils.isEmpty(filters)) {
                return true;
            }

            for (Filter<T, U> filter : filters) {
                boolean test = filter.test(action, entity, user);

                if (test) {
                    return true;
                }

            }


            return false;
        }
    }


    /**
     * PreInsertValidatorComposite.
     *
     * @author ilikly
     * @version 1.4.2
     * @since 1.4.2
     */
    private static class PreInsertValidatorComposite<T, U> implements PreInsertValidator<T, U> {

        private final List<PreInsertValidator<T, U>> validators;

        public PreInsertValidatorComposite(List<PreInsertValidator<T, U>> validators) {
            this.validators = validators;
        }

        @Override
        public void validate(@NonNull T entity, @NonNull U user) {
            if (CollectionUtils.isEmpty(validators)) {
                return;
            }

            for (PreInsertValidator<T, U> validator : validators) {
                validator.validate(entity, user);
            }
        }
    }

    /**
     * PreQueryConsumerComposite.
     *
     * @author ilikly
     * @version 1.4.2
     * @see PostQueryConsumerComposite
     * @since 1.4.2
     */
    @RequiredArgsConstructor
    private static class PreQueryConsumerComposite<Q, U> implements PreQueryConsumer<Q, U> {
        private final List<PreQueryConsumer<Q, U>> consumers;

        @Override
        @SuppressWarnings("unchecked")
        public void accept(@NonNull SpiAction action, @NonNull Q query, @NonNull U iUser) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }

            for (PreQueryConsumer<Q, U> consumer : consumers) {
                if (consumer instanceof PreQueryPredicate) {
                    if (((PreQueryPredicate<Q, U>) consumer).test(query, iUser)) {
                        consumer.accept(action, query, iUser);
                    }
                } else {
                    consumer.accept(action, query, iUser);
                }
            }

        }
    }


    /**
     * PreUpdateYnValidatorComposite.
     *
     * @author ilikly
     * @version 1.4.2
     * @since 1.4.2
     */
    private static class PreUpdateYnValidatorComposite<T, U> implements PreUpdateYnValidator<T, U> {
        private final List<PreUpdateYnValidator<T, U>> validators;

        public PreUpdateYnValidatorComposite(List<PreUpdateYnValidator<T, U>> validators) {
            this.validators = validators;
        }

        @Override
        public void validate(@NonNull T entity, @NonNull YN yn, @NonNull U user) {
            if (CollectionUtils.isEmpty(validators)) {
                return;
            }

            for (PreUpdateYnValidator<T, U> validator : validators) {
                validator.validate(entity, yn, user);
            }

        }
    }

    @RequiredArgsConstructor
    private static class PostUpdateYnConsumerComposite<T, U> implements PostUpdateYNConsumer<T, U> {

        private final List<PostUpdateYNConsumer<T, U>> consumers;

        @Override
        public void accept(@NonNull List<T> entities, @NonNull YN yn, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }

            consumers.forEach(it -> it.accept(entities, yn, user));
        }

        @Override
        public void accept(@NonNull T entity, @NonNull YN yn, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }

            consumers.forEach(it -> it.accept(entity, yn, user));
        }
    }
}
