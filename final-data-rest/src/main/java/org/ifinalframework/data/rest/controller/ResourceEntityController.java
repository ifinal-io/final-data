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

package org.ifinalframework.data.rest.controller;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;

import org.ifinalframework.context.exception.BadRequestException;
import org.ifinalframework.context.exception.NotFoundException;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IStatus;
import org.ifinalframework.core.IUser;
import org.ifinalframework.core.IView;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.auto.annotation.RestResource;
import org.ifinalframework.data.auto.generator.AutoNameHelper;
import org.ifinalframework.data.rest.model.ResourceEntity;
import org.ifinalframework.data.rest.validation.NoValidationGroupsProvider;
import org.ifinalframework.data.rest.validation.ValidationGroupsProvider;
import org.ifinalframework.data.security.ResourceSecurity;
import org.ifinalframework.data.service.AbsService;
import org.ifinalframework.data.spi.PostDeleteConsumer;
import org.ifinalframework.data.spi.PostInsertConsumer;
import org.ifinalframework.data.spi.PostQueryConsumer;
import org.ifinalframework.data.spi.PostUpdateConsumer;
import org.ifinalframework.data.spi.PreDeleteConsumer;
import org.ifinalframework.data.spi.PreInsertConsumer;
import org.ifinalframework.data.spi.PreInsertFunction;
import org.ifinalframework.data.spi.PreQueryConsumer;
import org.ifinalframework.data.spi.PreResourceAuthorize;
import org.ifinalframework.data.spi.PreUpdateConsumer;
import org.ifinalframework.data.spi.PreUpdateYnValidator;
import org.ifinalframework.data.spi.QueryConsumer;
import org.ifinalframework.data.spi.composite.PostDeleteConsumerComposite;
import org.ifinalframework.data.spi.composite.PostInsertConsumerComposite;
import org.ifinalframework.data.spi.composite.PostQueryConsumerComposite;
import org.ifinalframework.data.spi.composite.PostUpdateConsumerComposite;
import org.ifinalframework.data.spi.composite.PreDeleteConsumerComposite;
import org.ifinalframework.data.spi.composite.PreInsertConsumerComposite;
import org.ifinalframework.data.spi.composite.PreQueryConsumerComposite;
import org.ifinalframework.data.spi.composite.PreUpdateConsumerComposite;
import org.ifinalframework.data.spi.composite.PreUpdateYnValidatorComposite;
import org.ifinalframework.data.spi.composite.QueryConsumerComposite;
import org.ifinalframework.json.Json;
import org.ifinalframework.query.Update;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResourceEntityController.
 *
 * @author ilikly
 * @version 1.4.2
 * @since 1.4.2
 */
@RestController
@RequestMapping("/api/{resource}")
public class ResourceEntityController implements ApplicationContextAware, SmartInitializingSingleton {
    private static final Logger logger = LoggerFactory.getLogger(ResourceEntityController.class);


    @Resource
    private ValidationGroupsProvider validationGroupsProvider = new NoValidationGroupsProvider();

    private Map<String, ResourceEntity> resourceEntityMap = new LinkedHashMap<>();

    private PreResourceAuthorize<IUser<?>> preResourceAuthorize;

    private final QueryConsumerComposite queryConsumerComposite;

    public ResourceEntityController(ObjectProvider<QueryConsumer<?, ?>> queryConsumerProvider) {
        List consumers = queryConsumerProvider.orderedStream().collect(Collectors.toList());
        this.queryConsumerComposite = new QueryConsumerComposite(consumers);
    }

    @Setter
    private ApplicationContext applicationContext;

    private void applyPreResourceAuthorize(ResourceSecurity resourceSecurity, String resource, IUser<?> user) {
        if (Objects.nonNull(preResourceAuthorize)) {
            preResourceAuthorize.auth(resourceSecurity.format(resource), user);
        }
    }


    @GetMapping
    public List<? extends IEntity<Long>> query(@PathVariable String resource, NativeWebRequest request, WebDataBinderFactory binderFactory, IUser<?> user) throws Exception {
        logger.info("==> GET /api/{}", resource);
        applyPreResourceAuthorize(ResourceSecurity.QUERY, resource, user);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        IQuery query = bindQuery(request, binderFactory, resourceEntity);
        resourceEntity.getPreQueryConsumer().accept(query, user);
        AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
        List<IEntity<Long>> entities = service.select(query);
        if (CollectionUtils.isEmpty(entities)) return entities;
        entities.forEach(it -> resourceEntity.getPostQueryConsumer().accept(it, query, user));
        return entities;
    }

    @GetMapping("/{id}")
    public IEntity<Long> query(@PathVariable String resource, @PathVariable Long id, IUser<?> user) {
        logger.info("==> GET /api/{}/{}", resource, id);
        applyPreResourceAuthorize(ResourceSecurity.QUERY, resource, user);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
        return service.selectOne(id);
    }

    @DeleteMapping("/delete")
    public Integer deleteFromParam(@PathVariable String resource, @RequestParam Long id, IUser<?> user) {
        return this.delete(resource, id, user);
    }

    @DeleteMapping("/{id}")
    public Integer delete(@PathVariable String resource, @PathVariable Long id, IUser<?> user) {
        logger.info("==> GET /api/{}/{}", resource, id);
        applyPreResourceAuthorize(ResourceSecurity.DELETE, resource, user);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        AbsService<Long, IEntity<Long>> service = resourceEntity.getService();

        IEntity<Long> entity = service.selectOne(id);

        if (Objects.isNull(entity)) {
            throw new NotFoundException("not found delete target. id=" + id);
        }

        resourceEntity.getPreDeleteConsumer().accept(entity, user);
        int delete = service.delete(id);
        resourceEntity.getPostDeleteConsumer().accept(entity, user);
        return delete;
    }


    @PostMapping
    public Object create(@PathVariable String resource, @RequestBody String requestBody, IUser<?> user, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        logger.info("==> POST /api/{}", resource);
        applyPreResourceAuthorize(ResourceSecurity.INSERT, resource, user);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        PreInsertConsumer<IEntity<Long>, IUser<?>> preInsertConsumer = resourceEntity.getPreInsertConsumer();

        Class<?> createEntityClass = resourceEntity.getCreateEntityClass();
        if (Objects.nonNull(createEntityClass)) {
            Object createEntity = Json.toObject(requestBody, createEntityClass);
            WebDataBinder binder = binderFactory.createBinder(request, createEntity, "entity");
            Class<?>[] validationGroups = validationGroupsProvider.getEntityValidationGroups(resourceEntity.getEntityClass());
            validate(resourceEntity.getEntityClass(), createEntity, binder, validationGroups);
            AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
            List<IEntity<Long>> entities = resourceEntity.getPreInsertFunction().map(createEntity, user);
            entities.forEach(it -> preInsertConsumer.accept(it, user));
            int result = service.insert(entities);
            entities.forEach(it -> resourceEntity.getPostInsertConsumer().accept(it, user));
            return result;
        } else if (requestBody.startsWith("{")) {
            IEntity<Long> entity = Json.toObject(requestBody, resourceEntity.getEntityClass());
            WebDataBinder binder = binderFactory.createBinder(request, entity, "entity");
            Class<?>[] validationGroups = validationGroupsProvider.getEntityValidationGroups(resourceEntity.getEntityClass());
            validate(resourceEntity.getEntityClass(), entity, binder, validationGroups);
            AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
            preInsertConsumer.accept(entity, user);
            service.insert(entity);
            resourceEntity.getPostInsertConsumer().accept(entity, user);
            return entity.getId();
        } else if (requestBody.startsWith("[")) {
            List<? extends IEntity<Long>> entities = Json.toList(requestBody, resourceEntity.getEntityClass());
            WebDataBinder binder = binderFactory.createBinder(request, entities, "entities");
            Class<?>[] validationGroups = validationGroupsProvider.getEntityValidationGroups(resourceEntity.getEntityClass());
            validate(resourceEntity.getEntityClass(), entities, binder, validationGroups);
            AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
            for (IEntity<Long> entity : entities) {
                preInsertConsumer.accept(entity, user);
            }
            int result = service.insert((Collection<IEntity<Long>>) entities);
            entities.forEach(it -> resourceEntity.getPostInsertConsumer().accept(it, user));
            return result;
        }

        throw new BadRequestException("unsupported requestBody format of " + requestBody);


    }

    @PostMapping("/copy")
    public Long copy2(@PathVariable String resource, @RequestParam Long id) {
        return copy(resource, id);
    }


    @PostMapping("/{id}/copy")
    public Long copy(@PathVariable String resource, @PathVariable Long id) {
        logger.info("==> POST /api/{}/{}/copy", resource, id);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
        IEntity<Long> entity = service.selectOne(id);
        if (Objects.isNull(entity)) {
            throw new NotFoundException("not found entity for " + id);
        }
        entity.setId(null);
        service.insert(entity);
        return entity.getId();
    }

    @PutMapping
    public Integer update(@PathVariable String resource, @RequestBody String requestBody, IUser<?> user, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        return doUpdate(resource, null, requestBody, user, request, binderFactory);
    }

    @PutMapping("/{id}")
    public Integer update(@PathVariable String resource, @PathVariable Long id, @RequestBody String requestBody, IUser<?> user, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        return doUpdate(resource, id, requestBody, user, request, binderFactory);
    }

    private Integer doUpdate(String resource, Long id, String body, IUser<?> user, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        logger.info("==> PUT /api/{}", resource);
        applyPreResourceAuthorize(ResourceSecurity.UPDATE, resource, user);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        IEntity<Long> entity = Json.toObject(body, resourceEntity.getEntityClass());
        if (Objects.nonNull(id)) {
            entity.setId(id);
        } else {
            Assert.notNull(entity.getId(), "update id is null");
        }
        WebDataBinder binder = binderFactory.createBinder(request, entity, "entity");

        validate(resourceEntity.getEntityClass(), entity, binder);
        resourceEntity.getPreUpdateConsumer().accept(entity, user);
        AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
        resourceEntity.getPostUpdateConsumer().accept(entity, user);
        return service.update(entity);
    }

    @PatchMapping("/{id}/status")
    public Integer status(@PathVariable String resource, @PathVariable Long id, @RequestParam String status, IUser<?> user) {
        return doUpdateStatus(resource, id, status, user);
    }

    @PatchMapping("/status")
    public Integer status2(@PathVariable String resource, @RequestParam Long id, @RequestParam String status, IUser<?> user) {
        return doUpdateStatus(resource, id, status, user);
    }


    @SuppressWarnings("unchecked")
    private Integer doUpdateStatus(String resource, Long id, String status, IUser<?> user) {
        applyPreResourceAuthorize(ResourceSecurity.UPDATE, resource, user);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        Class<? extends IEntity<Long>> entityClass = resourceEntity.getEntityClass();

        if (!IStatus.class.isAssignableFrom(entityClass)) {
            throw new BadRequestException("resource is not supports status");
        }

        Class<?> statusClass = ResolvableType.forClass(entityClass).as(IStatus.class).resolveGeneric();
        Object statusValue = Json.toObject(status, statusClass);

        if (Objects.isNull(statusValue)) {
            throw new BadRequestException("not status of " + status);
        }


        Update update = Update.update().set("status", statusValue);
        return resourceEntity.getService().update(update, id);

    }


    @PutMapping("/{id}/yn")
    public Integer update(@PathVariable String resource, @PathVariable Long id, @RequestParam YN yn, IUser<?> user) {
        logger.info("==> PUT /api/{}/{}/yn", resource, id);
        applyPreResourceAuthorize(ResourceSecurity.UPDATE, resource, user);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        Update update = Update.update().set("yn", yn);
        AbsService<Long, IEntity<Long>> service = resourceEntity.getService();

        IEntity<Long> entity = service.selectOne(id);

        if (Objects.isNull(entity)) {
            throw new NotFoundException("not found entity for resource: " + resource + " by id= " + id);
        }

        resourceEntity.getPreUpdateYnValidator().validate(entity, yn, user);

        return service.update(update, id);
    }

    @PutMapping("/{id}/disable")
    public Integer disable(@PathVariable String resource, @PathVariable Long id, IUser<?> user) {
        return this.update(resource, id, YN.NO, user);
    }

    @PutMapping("/{id}/enable")
    public Integer enable(@PathVariable String resource, @PathVariable Long id, IUser<?> user) {
        return this.update(resource, id, YN.YES, user);
    }

    @PutMapping("/yn")
    public Integer yn(@PathVariable String resource, @RequestParam Long id, @RequestParam YN yn, IUser<?> user) {
        return this.update(resource, id, yn, user);
    }


    @GetMapping("/count")
    public Long count(@PathVariable String resource, IUser<?> user, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        logger.info("==> GET /api/{}", resource);
        applyPreResourceAuthorize(ResourceSecurity.QUERY, resource, user);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        IQuery query = bindQuery(request, binderFactory, resourceEntity);
        AbsService<Long, ? extends IEntity<Long>> service = resourceEntity.getService();
        return service.selectCount(query);
    }

    @SuppressWarnings("unchecked")
    private IQuery bindQuery(NativeWebRequest request, WebDataBinderFactory binderFactory, ResourceEntity resourceEntity) throws Exception {
        IQuery query = BeanUtils.instantiateClass(resourceEntity.getQueryClass());
        WebDataBinder binder = binderFactory.createBinder(request, query, "query");
        if (binder instanceof WebRequestDataBinder) {
            ((WebRequestDataBinder) binder).bind(request);
        } else if (binder instanceof ExtendedServletRequestDataBinder && request.getNativeRequest() instanceof ServletRequest) {
            ((ExtendedServletRequestDataBinder) binder).bind((ServletRequest) request.getNativeRequest());
        }
        Class entityClass = resourceEntity.getEntityClass();
        Class<?>[] validationGroups = validationGroupsProvider.getQueryValidationGroups(entityClass, resourceEntity.getQueryClass());
        validate(resourceEntity.getQueryClass(), query, binder, validationGroups);

        queryConsumerComposite.accept(query, entityClass);
        logger.info("query={}", Json.toJson(query));
        return query;
    }

    private static void validate(Class<?> clazz, Object value, WebDataBinder binder, Class<?>... groups) throws BindException {
        BindingResult bindingResult = binder.getBindingResult();
        for (Validator validator : binder.getValidators()) {
            logger.info("validator:{},groups={}", validator.getClass(), groups);
            ValidationUtils.invokeValidator(validator, value, bindingResult, groups);
            if (bindingResult.hasErrors()) {
                break;
            }

        }
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }
    }

    private ResourceEntity getResourceEntity(String resource) {
        ResourceEntity resourceEntity = resourceEntityMap.get(resource);

        if (Objects.isNull(resourceEntity)) {
            throw new NotFoundException("not found resource for " + resource);
        }
        return resourceEntity;
    }


    @Override
    @SuppressWarnings("unchecked")
    public void afterSingletonsInstantiated() {

        this.preResourceAuthorize = applicationContext.getBeanProvider(PreResourceAuthorize.class).getIfAvailable();


        String userClassName = applicationContext.getEnvironment().getRequiredProperty("final.data.spi.user-class");

        Class<?> userClass = ClassUtils.resolveClassName(userClassName, getClass().getClassLoader());

        logger.info("userClass:{}", userClass);

        resourceEntityMap = applicationContext.getBeanProvider(AbsService.class).stream()
                .map(service -> {
                    Class<?> entityClass = ResolvableType.forClass(AopUtils.getTargetClass(service)).as(AbsService.class).resolveGeneric(1);
                    RestResource restResource = entityClass.getAnnotation(RestResource.class);

                    if (Objects.isNull(restResource)) {
                        return null;
                    }

                    ResourceEntity.ResourceEntityBuilder builder = ResourceEntity.builder();

                    String queryClassName = String.join(".", AutoNameHelper.queryPackage(entityClass), AutoNameHelper.queryName(entityClass));

                    Class<?> queryClass = null;
                    try {
                        queryClass = ClassUtils.forName(queryClassName, entityClass.getClassLoader());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                    builder.resource(restResource.value().trim())
                            .entityClass((Class<? extends IEntity<Long>>) entityClass)
                            .queryClass((Class<? extends IQuery>) queryClass);

                    String dtoClassName = AutoNameHelper.dtoClassName(entityClass, IView.Create.class.getSimpleName());

                    if (ClassUtils.isPresent(dtoClassName, entityClass.getClassLoader())) {
                        Class<?> dtoClass = null;
                        try {
                            dtoClass = ClassUtils.forName(dtoClassName, entityClass.getClassLoader());
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                        PreInsertFunction preInsertFunction = (PreInsertFunction) applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(PreInsertFunction.class, dtoClass, userClass, entityClass)).getObject();
                        builder.createEntityClass(dtoClass).preInsertFunction(preInsertFunction);
                    }

                    List collect = applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(PostQueryConsumer.class, entityClass, queryClass, userClass))
                            .orderedStream()
                            .collect(Collectors.toList());

                    logger.info("found PostQueryConsumer:");
                    logger.info("entityClass:{}", entityClass);
                    logger.info("queryClass:{}", queryClass);
                    logger.info("userClass:{}", userClass);
                    collect.forEach(it -> logger.info("postQueryConsumer:{}", AopUtils.getTargetClass(it)));

                    List preInsertConsumers = applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(PreInsertConsumer.class, entityClass, userClass))
                            .orderedStream()
                            .collect(Collectors.toList());

                    List postInsertConsumers = applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(PostInsertConsumer.class, entityClass, userClass))
                            .orderedStream()
                            .collect(Collectors.toList());

                    List preUpdateYnValidators = applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(PreUpdateYnValidator.class, entityClass, userClass))
                            .orderedStream()
                            .collect(Collectors.toList());

                    List preDeleteConsumers = applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(PreDeleteConsumer.class, entityClass, userClass))
                            .orderedStream()
                            .collect(Collectors.toList());

                    List postDeleteConsumers = applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(PostDeleteConsumer.class, entityClass, userClass))
                            .orderedStream()
                            .collect(Collectors.toList());

                    List preUpdateConsumers = applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(PreUpdateConsumer.class, entityClass, userClass))
                            .orderedStream()
                            .collect(Collectors.toList());

                    List postUpdateConsumers = applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(PostUpdateConsumer.class, entityClass, userClass))
                            .orderedStream()
                            .collect(Collectors.toList());


                    final PostQueryConsumer consumerComposite = new PostQueryConsumerComposite(collect);
                    final PreInsertConsumer preInsertConsumerComposite = new PreInsertConsumerComposite(preInsertConsumers);
                    final PostInsertConsumer postInsertConsumer = new PostInsertConsumerComposite(postInsertConsumers);
                    final PreUpdateYnValidator preUpdateYnValidator = new PreUpdateYnValidatorComposite(preUpdateYnValidators);

                    final PreUpdateConsumer preUpdateConsumer = new PreUpdateConsumerComposite(preUpdateConsumers);
                    final PostUpdateConsumer postUpdateConsumer = new PostUpdateConsumerComposite(postUpdateConsumers);

                    final PreDeleteConsumer preDeleteConsumer = new PreDeleteConsumerComposite(preDeleteConsumers);
                    final PostDeleteConsumer postDeleteConsumer = new PostDeleteConsumerComposite(postDeleteConsumers);

                    List preQueryConsumers = applicationContext.getBeanProvider(
                                    ResolvableType.forClassWithGenerics(PreQueryConsumer.class, queryClass, userClass)
                            )
                            .orderedStream()
                            .collect(Collectors.toList());

                    builder.preQueryConsumer(new PreQueryConsumerComposite(preQueryConsumers));
                    builder.postQueryConsumer(consumerComposite);

                    builder.preInsertConsumer(preInsertConsumerComposite);
                    builder.postInsertConsumer(postInsertConsumer);

                    builder.preUpdateConsumer(preUpdateConsumer);
                    builder.postUpdateConsumer(postUpdateConsumer);

                    builder.preDeleteConsumer(preDeleteConsumer);
                    builder.postDeleteConsumer(postDeleteConsumer);

                    builder.preUpdateYnValidator(preUpdateYnValidator);

                    return builder.service(service).build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ResourceEntity::getResource, Function.identity()));
    }

}


