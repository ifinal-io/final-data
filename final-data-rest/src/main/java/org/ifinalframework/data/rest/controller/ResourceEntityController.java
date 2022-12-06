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
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.auto.annotation.RestResource;
import org.ifinalframework.data.auto.generator.AutoNameHelper;
import org.ifinalframework.data.rest.function.QueryConsumer;
import org.ifinalframework.data.rest.function.QueryConsumerComposite;
import org.ifinalframework.data.rest.model.ResourceEntity;
import org.ifinalframework.data.rest.validation.NoValidationGroupsProvider;
import org.ifinalframework.data.rest.validation.ValidationGroupsProvider;
import org.ifinalframework.data.service.AbsService;
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

    private final QueryConsumerComposite queryConsumerComposite;

    public ResourceEntityController(ObjectProvider<QueryConsumer> queryConsumerProvider) {
        this.queryConsumerComposite = new QueryConsumerComposite(queryConsumerProvider.orderedStream().collect(Collectors.toList()));
    }

    @Setter
    private ApplicationContext applicationContext;

    @GetMapping
    public List<? extends IEntity<Long>> query(@PathVariable String resource, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        logger.info("==> GET /api/{}", resource);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        IQuery query = bindQuery(request, binderFactory, resourceEntity);
        AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
        return service.select(query);
    }

    @GetMapping("/{id}")
    public IEntity<Long> query(@PathVariable String resource, @PathVariable Long id) {
        logger.info("==> GET /api/{}/{}", resource, id);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
        return service.selectOne(id);
    }

    @DeleteMapping("/{id}")
    public Integer delete(@PathVariable String resource, @PathVariable Long id) {
        logger.info("==> GET /api/{}/{}", resource, id);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
        return service.delete(id);
    }


    @PostMapping
    public Long create(@PathVariable String resource, @RequestBody String requestBody, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        logger.info("==> POST /api/{}", resource);
        ResourceEntity resourceEntity = getResourceEntity(resource);

        if(requestBody.startsWith("{")){
            IEntity<Long> entity = Json.toObject(requestBody, resourceEntity.getEntityClass());
            WebDataBinder binder = binderFactory.createBinder(request, entity, "entity");
            Class<?>[] validationGroups = validationGroupsProvider.getEntityValidationGroups(resourceEntity.getEntityClass());
            validate(resourceEntity.getEntityClass(), entity, binder, validationGroups);
            AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
            service.insert(entity);
            return entity.getId();
        }else if(requestBody.startsWith("[")){
            List<? extends IEntity<Long>> entities = Json.toList(requestBody, resourceEntity.getEntityClass());
            WebDataBinder binder = binderFactory.createBinder(request, entities, "entities");
            Class<?>[] validationGroups = validationGroupsProvider.getEntityValidationGroups(resourceEntity.getEntityClass());
            validate(resourceEntity.getEntityClass(), entities, binder, validationGroups);
            AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
            return service.insert((Collection<IEntity<Long>>) entities) * 1L;
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
    public Integer update(@PathVariable String resource, @RequestBody String requestBody, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        logger.info("==> PUT /api/{}", resource);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        IEntity<Long> entity = Json.toObject(requestBody, resourceEntity.getEntityClass());
        Assert.notNull(entity.getId(), "update id is null");
        WebDataBinder binder = binderFactory.createBinder(request, entity, "entity");
        validate(resourceEntity.getEntityClass(), entity, binder);
        AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
        return service.update(entity);
    }

    @PutMapping("/{id}")
    public Integer update(@PathVariable String resource, @PathVariable Long id, @RequestBody String requestBody) {
        logger.info("==> PUT /api/{}/{}", resource, id);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        IEntity<Long> entity = Json.toObject(requestBody, resourceEntity.getEntityClass());
        entity.setId(id);
        AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
        return service.update(entity);
    }

    @PutMapping("/{id}/yn")
    public Integer update(@PathVariable String resource, @PathVariable Long id, @RequestParam YN yn) {
        logger.info("==> PUT /api/{}/{}/yn", resource, id);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        Update update = Update.update().set("yn", yn);
        AbsService<Long, IEntity<Long>> service = resourceEntity.getService();
        return service.update(update, id);
    }

    @PutMapping("/{id}/disable")
    public Integer disable(@PathVariable String resource, @PathVariable Long id) {
        return this.update(resource, id, YN.NO);
    }

    @PutMapping("/{id}/enable")
    public Integer enable(@PathVariable String resource, @PathVariable Long id) {
        return this.update(resource, id, YN.YES);
    }

    @PutMapping("/yn")
    public Integer yn(@PathVariable String resource, @RequestParam Long id, @RequestParam YN yn) {
        return this.update(resource, id, yn);
    }


    @GetMapping("/count")
    public Long count(@PathVariable String resource, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        logger.info("==> GET /api/{}", resource);
        ResourceEntity resourceEntity = getResourceEntity(resource);
        IQuery query = bindQuery(request, binderFactory, resourceEntity);
        AbsService<Long, ? extends IEntity<Long>> service = resourceEntity.getService();
        return service.selectCount(query);
    }

    private IQuery bindQuery(NativeWebRequest request, WebDataBinderFactory binderFactory, ResourceEntity resourceEntity) throws Exception {
        IQuery query = BeanUtils.instantiateClass(resourceEntity.getQueryClass());
        WebDataBinder binder = binderFactory.createBinder(request, query, "query");
        if (binder instanceof WebRequestDataBinder) {
            ((WebRequestDataBinder) binder).bind(request);
        } else if (binder instanceof ExtendedServletRequestDataBinder && request.getNativeRequest() instanceof ServletRequest) {
            ((ExtendedServletRequestDataBinder) binder).bind((ServletRequest) request.getNativeRequest());
        }
        Class<?>[] validationGroups = validationGroupsProvider.getQueryValidationGroups(resourceEntity.getEntityClass(), resourceEntity.getQueryClass());
        validate(resourceEntity.getQueryClass(), query, binder, validationGroups);
        queryConsumerComposite.accept(query);
        logger.info("query={}", Json.toJson(query));
        return query;
    }

    private static void validate(Class<?> clazz, Object value, WebDataBinder binder, Class<?>... groups) throws BindException {
        BindingResult bindingResult = binder.getBindingResult();
        for (Validator validator : binder.getValidators()) {
            logger.info("validator:{},groups={}",validator.getClass(),groups);
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



        resourceEntityMap = applicationContext.getBeanProvider(AbsService.class).stream()
                .map(service -> {
                    Class<?> entityClass = ResolvableType.forClass(AopUtils.getTargetClass(service)).as(AbsService.class).resolveGeneric(1);
                    RestResource restResource = entityClass.getAnnotation(RestResource.class);

                    if (Objects.isNull(restResource)) {
                        return null;
                    }

                    String queryClassName = String.join(".", AutoNameHelper.queryPackage(entityClass), AutoNameHelper.queryName(entityClass));

                    Class<?> queryClass = null;
                    try {
                        queryClass = ClassUtils.forName(queryClassName, entityClass.getClassLoader());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                    return new ResourceEntity(restResource.value(), (Class<? extends IQuery>) queryClass, service, (Class<? extends IEntity<Long>>) entityClass);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ResourceEntity::getResource, Function.identity()));
    }
}


