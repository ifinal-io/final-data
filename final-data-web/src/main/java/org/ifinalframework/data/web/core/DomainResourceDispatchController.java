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

package org.ifinalframework.data.web.core;

import org.ifinalframework.context.exception.BadRequestException;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IEnum;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IStatus;
import org.ifinalframework.core.IUser;
import org.ifinalframework.core.IView;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.domain.DomainService;
import org.ifinalframework.data.domain.model.AuditValue;
import org.ifinalframework.json.Json;
import org.ifinalframework.validation.GlobalValidationGroupsProvider;
import org.ifinalframework.web.annotation.bind.RequestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * ResourceDomainController.
 *
 * @author ilikly
 * @version 1.4.2
 * @since 1.4.2
 */
@Configuration
@Transactional
@RestController
@RequestMapping("/api/{resource}")
@ConditionalOnWebApplication
public class DomainResourceDispatchController {
    private static final Logger logger = LoggerFactory.getLogger(DomainResourceDispatchController.class);

    @Resource
    private GlobalValidationGroupsProvider globalValidationGroupsProvider;


    @GetMapping
    public Object query(@PathVariable String resource, @RequestQuery(view = IView.List.class) IQuery query, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        logger.info("==> GET /api/{}", resource);
        return domainService.list(query, user);
    }

    @GetMapping("/detail")
    public Object detail(@PathVariable String resource, @RequestQuery(view = IView.Detail.class) IQuery query, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        logger.info("==> GET /api/{}/detail", resource);
        return domainService.detail(query, user);
    }

    @GetMapping("/{id}")
    public Object query(@PathVariable String resource, @PathVariable Long id, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        logger.info("==> GET /api/{}/{}", resource, id);
        return domainService.detail(id, user);
    }

    // delete

    @DeleteMapping
    public Object delete(@PathVariable String resource, @RequestQuery(view = IView.Delete.class) IQuery query, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return domainService.delete(query, user);
    }

    @DeleteMapping("/delete")
    public Object deleteFromParam(@PathVariable String resource, @RequestParam Long id, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return this.delete(resource, id, user, domainService);
    }

    @DeleteMapping("/{id}")
    public Object delete(@PathVariable String resource, @PathVariable Long id, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        logger.info("==> GET /api/{}/{}", resource, id);
        return domainService.delete(id, user);
    }


    @PostMapping
    public Object create(@PathVariable String resource, @RequestBody String requestBody, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService,
                         NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        logger.info("==> POST /api/{}", resource);
        Class<IEntity<Long>> entityClass = domainService.entityClass();

        List<Class<?>> validationGroups = new LinkedList<>(globalValidationGroupsProvider.getValidationGroups());
        Class<?> createEntityClass = domainService.domainEntityClass(IView.Create.class);
        if (Objects.nonNull(createEntityClass)) {
            Object createEntity = Json.toObject(requestBody, createEntityClass);
            WebDataBinder binder = binderFactory.createBinder(request, createEntity, "entity");
            binder.validate(ClassUtils.toClassArray(validationGroups));
            if (binder.getBindingResult().hasErrors()) {
                throw new BindException(binder.getBindingResult());
            }
            List<IEntity<Long>> entities = domainService.preInsertFunction().map(createEntity, user);
            return domainService.create(entities, user);
        } else if (requestBody.startsWith("{")) {
            IEntity<Long> entity = Json.toObject(requestBody, entityClass);
            WebDataBinder binder = binderFactory.createBinder(request, entity, "entity");
            binder.validate(ClassUtils.toClassArray(validationGroups));
            if (binder.getBindingResult().hasErrors()) {
                throw new BindException(binder.getBindingResult());
            }
            domainService.create(Collections.singletonList(entity), user);
            return entity.getId();
        } else if (requestBody.startsWith("[")) {
            List<IEntity<Long>> entities = Json.toList(requestBody, entityClass);
            WebDataBinder binder = binderFactory.createBinder(request, entities, "entities");
            final List<Validator> validators = binder.getValidators();
            final BindingResult bindingResult = binder.getBindingResult();
            final Class<?>[] validationHints = ClassUtils.toClassArray(validationGroups);
            for (IEntity<Long> entity : entities) {
                for (Validator validator : validators) {
                    if (!ObjectUtils.isEmpty(validationGroups) && validator instanceof SmartValidator smartValidator) {
                        smartValidator.validate(entity, bindingResult, validationHints);
                    } else if (validator != null) {
                        validator.validate(entity, bindingResult);
                    }

                    if (bindingResult.hasErrors()) {
                        throw new BindException(binder.getBindingResult());
                    }
                }
            }


            return domainService.create(entities, user);
        }

        throw new BadRequestException("unsupported requestBody format of " + requestBody);


    }

    @PutMapping
    public Integer update(@PathVariable String resource, @RequestBody String requestBody, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService,
                          NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        return doUpdate(resource, null, requestBody, user, domainService, request, binderFactory);
    }

    @PutMapping("/{id}")
    public Integer update(@PathVariable String resource, @PathVariable Long id, @RequestBody String requestBody, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService,
                          NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        return doUpdate(resource, id, requestBody, user, domainService, request, binderFactory);
    }

    private Integer doUpdate(String resource, Long id, String body, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService,
                             NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        logger.info("==> PUT /api/{}", resource);
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

    // status

    @PatchMapping("/{id}/status")
    public Object status(@PathVariable String resource, @PathVariable Long id, @RequestParam String status, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return doUpdateStatus(resource, id, status, user, domainService);
    }

    @PatchMapping("/status")
    public Object status2(@PathVariable String resource, @RequestParam Long id, @RequestParam String status, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return doUpdateStatus(resource, id, status, user, domainService);
    }


    @SuppressWarnings("unchecked")
    private Object doUpdateStatus(String resource, Long id, String status, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
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

    // audit

    @PatchMapping("/{id}/audit")
    public Object audit(@PathVariable String resource, @PathVariable Long id, @Valid @RequestBody AuditValue auditValue, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return domainService.audit(id, auditValue, user);
    }


    // lock
    @PatchMapping("/{id}/lock")
    public Object lock(@PathVariable String resource, @PathVariable Long id, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return domainService.lock(id, true, user);
    }

    @PatchMapping("/{id}/unlock")
    public Object unlock(@PathVariable String resource, @PathVariable Long id, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return domainService.lock(id, false, user);
    }


    // yn
    @RequestMapping(value = "/{id}/yn", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public Object update(@PathVariable String resource, @PathVariable Long id, @RequestParam YN yn, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        logger.info("==> PUT /api/{}/{}/yn", resource, id);
        return domainService.yn(id, yn, user);
    }

    @PutMapping("/{id}/disable")
    public Object disable(@PathVariable String resource, @PathVariable Long id, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return this.update(resource, id, YN.NO, user, domainService);
    }

    @PutMapping("/{id}/enable")
    public Object enable(@PathVariable String resource, @PathVariable Long id, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return this.update(resource, id, YN.YES, user, domainService);
    }

    @PutMapping("/yn")
    public Object yn(@PathVariable String resource, @RequestParam Long id, @RequestParam YN yn, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return this.update(resource, id, yn, user, domainService);
    }

    // count
    @GetMapping("/count")
    public Long count(@PathVariable String resource, @RequestQuery(view = IView.Count.class) IQuery query, IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        logger.info("==> GET /api/{}", resource);
        return domainService.count(query, user);
    }

}


