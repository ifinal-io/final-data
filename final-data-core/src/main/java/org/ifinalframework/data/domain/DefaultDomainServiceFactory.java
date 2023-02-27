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
import org.ifinalframework.data.spi.AfterThrowingQueryConsumer;
import org.ifinalframework.data.spi.PostDeleteConsumer;
import org.ifinalframework.data.spi.PostDetailConsumer;
import org.ifinalframework.data.spi.PostInsertConsumer;
import org.ifinalframework.data.spi.PostQueryConsumer;
import org.ifinalframework.data.spi.PostUpdateConsumer;
import org.ifinalframework.data.spi.PostUpdateYNConsumer;
import org.ifinalframework.data.spi.PreCountQueryConsumer;
import org.ifinalframework.data.spi.PreDeleteConsumer;
import org.ifinalframework.data.spi.PreInsertConsumer;
import org.ifinalframework.data.spi.PreInsertFilter;
import org.ifinalframework.data.spi.PreInsertFunction;
import org.ifinalframework.data.spi.PreInsertValidator;
import org.ifinalframework.data.spi.PreQueryConsumer;
import org.ifinalframework.data.spi.PreQueryPredicate;
import org.ifinalframework.data.spi.PreUpdateConsumer;
import org.ifinalframework.data.spi.PreUpdateYnValidator;

import lombok.RequiredArgsConstructor;

/**
 * DefaultDomainServiceFactory.
 *
 * @author ilikly
 * @version 1.4.3
 * @since 1.4.3
 */
@RequiredArgsConstructor
public class DefaultDomainServiceFactory implements DomainServiceFactory {

    private final Class<? extends IUser<?>> userClass;
    private final ApplicationContext applicationContext;

    @Override
    @SuppressWarnings("unchecked,rawtypes")
    public <ID extends Serializable, T extends IEntity<ID>> DomainService<ID, T> create(Repository<ID, T> repository) {
        DefaultDomainService.DefaultDomainServiceBuilder<ID, T> builder = DefaultDomainService.builder();
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

        builder.preInsertFilter(new PreInsertFilterComposite<>(getBeansOf(PreInsertFilter.class, entityClass, userClass)));
        builder.preInsertConsumer(new PreInsertConsumerComposite<>(getBeansOf(PreInsertConsumer.class, entityClass, userClass)));
        builder.postInsertConsumer(new PostInsertConsumerComposite<>(getBeansOf(PostInsertConsumer.class, entityClass, userClass)));

        // delete
        final Class<?> deleteQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.Delete.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.Delete.class, (Class<? extends IQuery>) deleteQueryClass);
        builder.preDeleteQueryConsumer(new PreQueryConsumerComposite(getBeansOf(PreQueryConsumer.class, deleteQueryClass, userClass)));
        builder.preDeleteConsumer(new PreDeleteConsumerComposite<>(getBeansOf(PreDeleteConsumer.class, entityClass, userClass)));
        builder.postDeleteQueryConsumer(new PostQueryConsumerComposite<>(getBeansOf(PostQueryConsumer.class, entityClass, deleteQueryClass, userClass)));
        builder.postDeleteConsumer(new PostDeleteConsumerComposite<>(getBeansOf(PostDeleteConsumer.class, entityClass, userClass)));

        // update
        builder.preUpdateConsumer(new PreUpdateConsumerComposite<>(getBeansOf(PreUpdateConsumer.class, entityClass, userClass)));
        builder.postUpdateConsumer(new PostUpdateConsumerComposite<>(getBeansOf(PostUpdateConsumer.class, entityClass, userClass)));


        // list
        final Class<?> listQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.List.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.List.class, (Class<? extends IQuery>) listQueryClass);
        builder.preQueryConsumer(new PreQueryConsumerComposite(getBeansOf(PreQueryConsumer.class, listQueryClass, userClass)));
        builder.postQueryConsumer(new PostQueryConsumerComposite<>(getBeansOf(PostQueryConsumer.class, entityClass, listQueryClass, userClass)));
        builder.afterThrowingQueryConsumer(new AfterThrowingQueryConsumerComposite<>(getBeansOf(AfterThrowingQueryConsumer.class, entityClass, listQueryClass, userClass)));
        builder.afterReturnQueryConsumer(new AfterReturnQueryConsumerComposite<>(getBeansOf(AfterReturnQueryConsumer.class, entityClass, listQueryClass, userClass)));

        // detail
        final Class<?> detailQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.Detail.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.Detail.class, (Class<? extends IQuery>) detailQueryClass);
        builder.preDetailQueryConsumer(new PreQueryConsumerComposite(getBeansOf(PreQueryConsumer.class, detailQueryClass, userClass)));
        builder.postDetailQueryConsumer(new PostQueryConsumerComposite<>(getBeansOf(PostQueryConsumer.class, entityClass, detailQueryClass, userClass)));
        builder.postDetailConsumer(new PostDetailConsumerComposite<>(getBeansOf(PostDetailConsumer.class, entityClass, userClass)));

        // count
        final Class<?> countQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.Count.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.Count.class, (Class<? extends IQuery>) countQueryClass);
        builder.preCountQueryConsumer(new PreCountQueryConsumerComposite<>(getBeansOf(PreCountQueryConsumer.class, countQueryClass, userClass)));

        // yn
        builder.preUpdateYnValidator(new PreUpdateYnValidatorComposite<>(getBeansOf(PreUpdateYnValidator.class, entityClass, userClass)));
        builder.postUpdateYNConsumer(new PostUpdateYnConsumerComposite<>(getBeansOf(PostUpdateYNConsumer.class, entityClass, userClass)));
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
        return applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(type, generics))
                .orderedStream()
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
        public void accept(@NonNull List<T> entities, @NonNull Q query, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            for (PostQueryConsumer<T, Q, U> consumer : consumers) {
                consumer.accept(entities, query, user);
            }
        }

        @Override
        public void accept(@NonNull T entity, @NonNull Q query, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            for (PostQueryConsumer<T, Q, U> consumer : consumers) {
                consumer.accept(entity, query, user);
            }

        }
    }

    @RequiredArgsConstructor
    private static final class AfterThrowingQueryConsumerComposite<T, Q, U> implements AfterThrowingQueryConsumer<T, Q, U> {
        private final List<AfterThrowingQueryConsumer<T, Q, U>> consumers;

        @Override
        public void accept(@Nullable List<T> entities, @NonNull Q query, @NonNull U user, @NonNull Throwable e) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(entities, query, user, e));
        }

        @Override
        public void accept(@Nullable T entity, @NonNull Q query, @NonNull U user, @NonNull Throwable e) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(entity, query, user, e));

        }
    }

    @RequiredArgsConstructor
    private static final class AfterReturnQueryConsumerComposite<T, Q, U> implements AfterReturnQueryConsumer<T, Q, U> {
        private final List<AfterReturnQueryConsumer<T, Q, U>> consumers;

        @Override
        public void accept(@Nullable List<T> entities, @NonNull Q query, @NonNull U user, @Nullable Throwable e) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }

            consumers.forEach(it -> it.accept(entities, query, user, e));
        }

        @Override
        public void accept(@Nullable T entity, @NonNull Q query, @NonNull U user, @Nullable Throwable e) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(entity, query, user, e));
        }
    }

    @RequiredArgsConstructor
    private static class PreCountQueryConsumerComposite<Q, U> implements PreCountQueryConsumer<Q, U> {
        private final List<PreCountQueryConsumer<Q, U>> consumers;

        @Override
        public void accept(@NonNull Q query, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(query, user));

        }
    }

    /**
     * PostDeleteConsumerComposite.
     *
     * @author ilikly
     * @version 1.4.2
     * @since 1.4.2
     */
    private static class PostDeleteConsumerComposite<T, U> implements PostDeleteConsumer<T, U> {

        private final List<PostDeleteConsumer<T, U>> consumers;

        public PostDeleteConsumerComposite(List<PostDeleteConsumer<T, U>> consumers) {
            this.consumers = consumers;
        }

        @Override
        public void accept(@NonNull T entity, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }

            for (PostDeleteConsumer<T, U> consumer : consumers) {
                consumer.accept(entity, user);
            }
        }
    }

    /**
     * PostInsertConsumerComposite.
     *
     * @author ilikly
     * @version 1.4.2
     * @since 1.4.2
     */
    private static class PostInsertConsumerComposite<T, U> implements PostInsertConsumer<T, U> {
        private final List<PostInsertConsumer<T, U>> consumers;

        public PostInsertConsumerComposite(List<PostInsertConsumer<T, U>> consumers) {
            this.consumers = consumers;
        }

        @Override
        public void accept(@NonNull List<T> entities, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(entities, user));
        }

        @Override
        public void accept(@NonNull T entity, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }

            for (PostInsertConsumer<T, U> consumer : consumers) {
                consumer.accept(entity, user);
            }
        }
    }

    /**
     * PostUpdateConsumerComposite.
     *
     * @author ilikly
     * @version 1.4.2
     * @since 1.4.2
     */
    private static class PostUpdateConsumerComposite<T, U> implements PostUpdateConsumer<T, U> {

        private final List<PostUpdateConsumer<T, U>> consumers;

        public PostUpdateConsumerComposite(List<PostUpdateConsumer<T, U>> consumers) {
            this.consumers = consumers;
        }

        @Override
        public void accept(@NonNull T entity, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(entity, user));

        }
    }

    /**
     * PreDeleteConsumerComposite.
     *
     * @author ilikly
     * @version 1.4.2
     * @since 1.4.2
     */
    private static class PreDeleteConsumerComposite<T, U> implements PreDeleteConsumer<T, U> {
        private final List<PreDeleteConsumer<T, U>> consumers;

        public PreDeleteConsumerComposite(List<PreDeleteConsumer<T, U>> consumers) {
            this.consumers = consumers;
        }

        @Override
        public void accept(@NonNull T entity, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }

            for (PreDeleteConsumer<T, U> consumer : consumers) {
                consumer.accept(entity, user);
            }
        }
    }

    @RequiredArgsConstructor
    private static class PreInsertFilterComposite<T, U> implements PreInsertFilter<T, U> {
        private final List<PreInsertFilter<T, U>> filters;

        @Override
        public boolean test(@NonNull T entity, @NonNull U user) {

            if (CollectionUtils.isEmpty(filters)) {
                return true;
            }

            for (PreInsertFilter<T, U> filter : filters) {
                boolean test = filter.test(entity, user);

                if (test) {
                    return true;
                }

            }


            return false;
        }
    }

    /**
     * PreInsertConsumerComposite.
     *
     * @author ilikly
     * @version 1.4.2
     * @since 1.4.2
     */
    private static class PreInsertConsumerComposite<T, U> implements PreInsertConsumer<T, U> {

        private final List<PreInsertConsumer<T, U>> consumers;

        public PreInsertConsumerComposite(List<PreInsertConsumer<T, U>> consumers) {
            this.consumers = consumers;
        }

        @Override
        public void accept(@NonNull List<T> entities, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }
            consumers.forEach(it -> it.accept(entities, user));
        }

        @Override
        public void accept(@NonNull T entity, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }

            for (PreInsertConsumer<T, U> consumer : consumers) {
                consumer.accept(entity, user);
            }

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
     * @since 1.4.2
     */
    private static class PreQueryConsumerComposite implements PreQueryConsumer<IQuery, IUser<?>> {
        private final List<PreQueryConsumer<IQuery, IUser<?>>> consumers;

        public PreQueryConsumerComposite(List<PreQueryConsumer<IQuery, IUser<?>>> consumers) {
            this.consumers = consumers;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void accept(@NonNull IQuery query, @NonNull IUser<?> iUser) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }

            for (PreQueryConsumer<IQuery, IUser<?>> consumer : consumers) {
                if (consumer instanceof PreQueryPredicate) {
                    if (((PreQueryPredicate<IQuery, IUser<?>>) consumer).test(query, iUser)) {
                        consumer.accept(query, iUser);
                    }
                } else {
                    consumer.accept(query, iUser);
                }
            }

        }
    }

    @RequiredArgsConstructor
    private static class PostDetailConsumerComposite<T, U> implements PostDetailConsumer<T, U> {
        private final List<PostDetailConsumer<T, U>> consumers;

        @Override
        public void accept(@NonNull T entity, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }

            consumers.forEach(it -> it.accept(entity, user));
        }
    }

    /**
     * PreUpdateConsumerComposite.
     *
     * @author ilikly
     * @version 1.4.2
     * @since 1.4.2
     */
    private static class PreUpdateConsumerComposite<T, U> implements PreUpdateConsumer<T, U> {
        private final List<PreUpdateConsumer<T, U>> consumers;

        public PreUpdateConsumerComposite(List<PreUpdateConsumer<T, U>> consumers) {
            this.consumers = consumers;
        }

        @Override
        public void accept(@NonNull T entity, @NonNull U user) {
            if (CollectionUtils.isEmpty(consumers)) {
                return;
            }

            consumers.forEach(it -> it.accept(entity, user));
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
