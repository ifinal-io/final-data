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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ResolvableType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
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
import org.ifinalframework.core.IEnum;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IStatus;
import org.ifinalframework.core.IUser;
import org.ifinalframework.core.IView;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.domain.DomainService;
import org.ifinalframework.data.domain.DomainServiceRegistry;
import org.ifinalframework.data.rest.validation.NoValidationGroupsProvider;
import org.ifinalframework.data.rest.validation.ValidationGroupsProvider;
import org.ifinalframework.data.spi.QueryConsumer;
import org.ifinalframework.data.spi.composite.QueryConsumerComposite;
import org.ifinalframework.json.Json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResourceDomainController.
 *
 * @author ilikly
 * @version 1.4.2
 * @since 1.4.2
 */
@Transactional
@RestController
@RequestMapping("/api/{resource}")
public class DomainResourceController {
    private static final Logger logger = LoggerFactory.getLogger(DomainResourceController.class);

    @Resource
    private ValidationGroupsProvider validationGroupsProvider = new NoValidationGroupsProvider();
    @Resource
    private DomainServiceRegistry domainServiceRegistry;

    private final QueryConsumerComposite queryConsumerComposite;

    public DomainResourceController(ObjectProvider<QueryConsumer<?, ?>> queryConsumerProvider) {
        List consumers = queryConsumerProvider.orderedStream().collect(Collectors.toList());
        this.queryConsumerComposite = new QueryConsumerComposite(consumers);
    }


    @GetMapping
    public Object query(@PathVariable String resource, NativeWebRequest request, WebDataBinderFactory binderFactory, IUser<?> user) throws Exception {
        logger.info("==> GET /api/{}", resource);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<? extends IQuery> queryClass = domainService.domainQueryClass(IView.List.class);
        Class<IEntity<Long>> entityClass = domainService.entityClass();
        IQuery query = bindQuery(request, binderFactory, entityClass, queryClass);
        return domainService.list(query, user);
    }

    @GetMapping("/detail")
    public IEntity<Long> detail(@PathVariable String resource, NativeWebRequest request, WebDataBinderFactory binderFactory, IUser<?> user) throws Exception {
        logger.info("==> GET /api/{}/detail", resource);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<? extends IQuery> queryClass = domainService.domainQueryClass(IView.Detail.class);
        Class<IEntity<Long>> entityClass = domainService.entityClass();
        IQuery query = bindQuery(request, binderFactory, entityClass, queryClass);
        return domainService.detail(query, user);
    }

    @GetMapping("/{id}")
    public IEntity<Long> query(@PathVariable String resource, @PathVariable Long id, IUser<?> user) {
        logger.info("==> GET /api/{}/{}", resource, id);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        return domainService.detail(id, user);
    }

    // delete

    @DeleteMapping
    public Integer delete(@PathVariable String resource, @RequestBody String requestBody, IUser<?> user) {
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<? extends IQuery> queryClass = domainService.domainQueryClass(IView.Delete.class);
        IQuery query = Json.toObject(requestBody, queryClass);
        return domainService.delete(query, user);
    }

    @DeleteMapping("/delete")
    public Integer deleteFromParam(@PathVariable String resource, @RequestParam Long id, IUser<?> user) {
        return this.delete(resource, id, user);
    }

    @DeleteMapping("/{id}")
    public Integer delete(@PathVariable String resource, @PathVariable Long id, IUser<?> user) {
        logger.info("==> GET /api/{}/{}", resource, id);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        return domainService.delete(id, user);
    }


    @PostMapping
    public Object create(@PathVariable String resource, @RequestBody String requestBody, IUser<?> user, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        logger.info("==> POST /api/{}", resource);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<IEntity<Long>> entityClass = domainService.entityClass();

        Class<?> createEntityClass = domainService.domainEntityClass(IView.Create.class);
        if (Objects.nonNull(createEntityClass)) {
            Object createEntity = Json.toObject(requestBody, createEntityClass);
            WebDataBinder binder = binderFactory.createBinder(request, createEntity, "entity");
            Class<?>[] validationGroups = validationGroupsProvider.getEntityValidationGroups(entityClass);
            binder.validate(validationGroups);
            if (binder.getBindingResult().hasErrors()) {
                throw new BindException(binder.getBindingResult());
            }
            List<IEntity<Long>> entities = domainService.preInsertFunction().map(createEntity, user);
            return domainService.create(entities, user);
        } else if (requestBody.startsWith("{")) {
            IEntity<Long> entity = Json.toObject(requestBody, entityClass);
            WebDataBinder binder = binderFactory.createBinder(request, entity, "entity");
            Class<?>[] validationGroups = validationGroupsProvider.getEntityValidationGroups(entityClass);
            binder.validate(validationGroups);
            if (binder.getBindingResult().hasErrors()) {
                throw new BindException(binder.getBindingResult());
            }
            domainService.create(Collections.singletonList(entity), user);
            return entity.getId();
        } else if (requestBody.startsWith("[")) {
            List<IEntity<Long>> entities = Json.toList(requestBody, entityClass);
            WebDataBinder binder = binderFactory.createBinder(request, entities, "entities");
            Class<?>[] validationGroups = validationGroupsProvider.getEntityValidationGroups(entityClass);
            binder.validate(validationGroups);
            if (binder.getBindingResult().hasErrors()) {
                throw new BindException(binder.getBindingResult());
            }
            return domainService.create(entities, user);
        }

        throw new BadRequestException("unsupported requestBody format of " + requestBody);


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
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<IEntity<Long>> entityClass = domainService.entityClass();
        IEntity<Long> entity = Json.toObject(body, entityClass);
        if (Objects.nonNull(id)) {
            entity.setId(id);
        } else {
            Assert.notNull(entity.getId(), "update id is null");
        }
        WebDataBinder binder = binderFactory.createBinder(request, entity, "entity");
        binder.validate(entity);
        if (binder.getBindingResult().hasErrors()) {
            throw new BindException(binder.getBindingResult());
        }
        return domainService.update(entity, entity.getId(), true, user);
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
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<IEntity<Long>> entityClass = domainService.entityClass();

        if (!IStatus.class.isAssignableFrom(entityClass)) {
            throw new BadRequestException("resource is not supports status");
        }

        Class<? extends IEnum<?>> statusClass = (Class<? extends IEnum<?>>) ResolvableType.forClass(entityClass).as(IStatus.class).resolveGeneric();
        final IEnum<?> statusValue = Arrays.stream(statusClass.getEnumConstants())
                .filter(it -> it.getCode().toString().equals(status))
                .findFirst().orElse(null);


        if (Objects.isNull(statusValue)) {
            throw new BadRequestException("not status of " + status);
        }

        return domainService.status(id, statusValue, user);

    }

    // lock
    @PatchMapping("/{id}/lock")
    public Integer lock(@PathVariable String resource, @PathVariable Long id, IUser<?> user) {
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        return domainService.lock(id, true, user);
    }

    @PatchMapping("/{id}/unlock")
    public Integer unlock(@PathVariable String resource, @PathVariable Long id, IUser<?> user) {
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        return domainService.lock(id, false, user);
    }


    @PutMapping("/{id}/yn")
    public Integer update(@PathVariable String resource, @PathVariable Long id, @RequestParam YN yn, IUser<?> user) {
        logger.info("==> PUT /api/{}/{}/yn", resource, id);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        return domainService.yn(id, yn, user);
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
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<? extends IQuery> queryClass = domainService.domainQueryClass(IView.Count.class);
        Class<IEntity<Long>> entityClass = domainService.entityClass();
        IQuery query = bindQuery(request, binderFactory, entityClass, queryClass);
        return domainService.count(query, user);
    }

    @SuppressWarnings("unchecked")
    private IQuery bindQuery(NativeWebRequest request, WebDataBinderFactory binderFactory, Class<IEntity<Long>> entityClass, Class<? extends IQuery> queryClass) throws Exception {
        IQuery query = BeanUtils.instantiateClass(queryClass);
        WebDataBinder binder = binderFactory.createBinder(request, query, "query");
        if (binder instanceof WebRequestDataBinder) {
            ((WebRequestDataBinder) binder).bind(request);
        } else if (binder instanceof ExtendedServletRequestDataBinder && request.getNativeRequest() instanceof ServletRequest) {
            ((ExtendedServletRequestDataBinder) binder).bind((ServletRequest) request.getNativeRequest());
        }
        Class<?>[] validationGroups = validationGroupsProvider.getQueryValidationGroups(entityClass, queryClass);
        binder.validate(validationGroups);
        if (binder.getBindingResult().hasErrors()) {
            throw new BindException(binder.getBindingResult());
        }
        queryConsumerComposite.accept(query, entityClass);
        logger.info("query={}", Json.toJson(query));
        return query;
    }

    private DomainService<Long, IEntity<Long>> getDomainService(String resource) {
        DomainService<Long, IEntity<Long>> domainService = domainServiceRegistry.getDomainService(resource);

        if (Objects.isNull(domainService)) {
            throw new NotFoundException("not found resource for " + resource);
        }
        return domainService;
    }


}


