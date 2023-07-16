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
import org.ifinalframework.core.IAudit;
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
import org.ifinalframework.web.annotation.bind.RequestEntity;
import org.ifinalframework.web.annotation.bind.RequestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
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

import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.Collections;
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
@Validated
@RequestMapping("/api/{resource}")
@ConditionalOnWebApplication
public class DomainResourceDispatchController {
    private static final Logger logger = LoggerFactory.getLogger(DomainResourceDispatchController.class);

    @GetMapping
    public Object query(@PathVariable String resource, @Valid @RequestQuery(view = IView.List.class) IQuery query,
                        IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> query={}", Json.toJson(query));
        }
        return processResult(domainService.list(query, user));
    }

    @GetMapping("/detail")
    public Object detail(@PathVariable String resource, @Valid @RequestQuery(view = IView.Detail.class) IQuery query,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> query={}", Json.toJson(query));
        }
        final Object result = domainService.detail(query, user);
        return processResult(result);
    }

    @GetMapping("/{id}")
    public Object query(@PathVariable String resource, @PathVariable Long id,
                        IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return processResult(domainService.detail(id, user));
    }

    // delete
    @DeleteMapping
    public Object delete(@PathVariable String resource, @Valid @RequestQuery(view = IView.Delete.class) IQuery query,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> query={}", Json.toJson(query));
        }
        return processResult(domainService.delete(query, user));
    }

    @DeleteMapping("/{id}")
    public Object delete(@PathVariable String resource, @PathVariable Long id,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return processResult(domainService.delete(id, user));
    }


    @PostMapping
    public Object create(@PathVariable String resource, @Valid @RequestEntity(view = IView.Create.class) Object requestEntity,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("==> entity={}", Json.toJson(requestEntity));
        }
        Class<?> createEntityClass = domainService.domainEntityClass(IView.Create.class);
        if (Objects.nonNull(createEntityClass)) {
            List<IEntity<Long>> entities = domainService.preInsertFunction().map(requestEntity, user);
            return processResult(domainService.create(entities, user));
        } else if (requestEntity instanceof List<?>) {
            return processResult(domainService.create((List<IEntity<Long>>) requestEntity, user));
        } else if (requestEntity instanceof IEntity<?> entity) {
            final Object result = domainService.create(Collections.singletonList((IEntity<Long>) entity), user);
            if (result instanceof Number) {
                return processResult(entity.getId());
            }
            return processResult(result);
        }

        throw new BadRequestException("unsupported requestBody format of " + requestEntity);
    }

    @PutMapping("/{id}")
    public Integer update(@PathVariable String resource, @PathVariable Long id, @Valid @RequestEntity(view = IView.Update.class) Object requestEntity,
                          IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("==> entity={}", Json.toJson(requestEntity));
        }

        if (requestEntity instanceof IEntity<?> entity) {
            return processResult(domainService.update((IEntity<Long>) entity, id, true, user));
        }

        throw new BadRequestException("unsupported update requestEntity of " + requestEntity);

    }

    // status
    @PatchMapping("/{id}/status")
    public Object status(@PathVariable String resource, @PathVariable Long id, @RequestParam String status,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {

        if (logger.isDebugEnabled()) {
            logger.debug("==> status={}", status);
        }

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

        return processResult(domainService.status(id, statusValue, user));

    }

    // audit
    @PatchMapping("/{id}/audit")
    public Object audit(@PathVariable String resource, @PathVariable Long id, @Valid @RequestBody AuditValue auditValue,
                        IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> auditValue={}", Json.toJson(auditValue));
        }
        if (Objects.equals(auditValue.getStatus(), IAudit.AuditStatus.CANCELED)) {
            throw new BadRequestException("审核不能撤销");
        }
        return processResult(domainService.audit(id, auditValue, user));
    }

    @PatchMapping("/{id}/cancel")
    public Object cancel(@PathVariable String resource, @PathVariable Long id, @RequestParam String content,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> content={}", content);
        }
        final AuditValue auditValue = new AuditValue();
        auditValue.setStatus(IAudit.AuditStatus.CANCELED);
        auditValue.setContent(content);
        return processResult(domainService.audit(id, auditValue, user));
    }


    // lock
    @PatchMapping("/{id}/lock")
    public Object lock(@PathVariable String resource, @PathVariable Long id,
                       IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return processResult(domainService.lock(id, true, user));
    }

    @PatchMapping("/{id}/unlock")
    public Object unlock(@PathVariable String resource, @PathVariable Long id,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return processResult(domainService.lock(id, false, user));
    }


    // yn
    @PatchMapping("/{id}/yn")
    public Object update(@PathVariable String resource, @PathVariable Long id, @RequestParam YN yn,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> yn={}", yn);
        }
        return processResult(domainService.yn(id, yn, user));
    }

    @PutMapping("/{id}/disable")
    public Object disable(@PathVariable String resource, @PathVariable Long id,
                          IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return this.update(resource, id, YN.NO, user, domainService);
    }

    @PutMapping("/{id}/enable")
    public Object enable(@PathVariable String resource, @PathVariable Long id,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return this.update(resource, id, YN.YES, user, domainService);
    }

    // count
    @GetMapping("/count")
    public Long count(@PathVariable String resource, @RequestQuery(view = IView.Count.class) IQuery query,
                      IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> query={}", Json.toJson(query));
        }
        return processResult(domainService.count(query, user));
    }

    private <R> R processResult(R result) {
        if (logger.isDebugEnabled()) {
            logger.debug("<== {}", Json.toJson(result));
        }
        return result;
    }

}


