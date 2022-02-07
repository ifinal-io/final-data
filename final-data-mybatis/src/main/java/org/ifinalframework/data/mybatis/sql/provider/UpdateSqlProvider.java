/*
 * Copyright 2020-2021 the original author or authors.
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

package org.ifinalframework.data.mybatis.sql.provider;

import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.type.TypeHandler;
import org.ifinalframework.context.user.UserContextHolder;
import org.ifinalframework.core.IRecord;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.annotation.Metadata;
import org.ifinalframework.data.mybatis.sql.AbsMapperSqlProvider;
import org.ifinalframework.data.mybatis.sql.ScriptMapperHelper;
import org.ifinalframework.data.query.DefaultQEntityFactory;
import org.ifinalframework.query.*;
import org.ifinalframework.util.Asserts;
import org.ifinalframework.velocity.Velocities;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.Objects;

/**
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
public class UpdateSqlProvider implements AbsMapperSqlProvider, ScriptSqlProvider {

    private static final String DEFAULT_WRITER = "#{${value}#if($javaType),javaType=$!{javaType.canonicalName}#end"
            + "#if($typeHandler),typeHandler=$!{typeHandler.canonicalName}#end}";

    private static final String PROPERTIES_PARAMETER_NAME = "properties";

    private static final String SELECTIVE_PARAMETER_NAME = "selective";

    private static final String ENTITY_PARAMETER_NAME = "entity";

    private static final String UPDATE_PARAMETER_NAME = "update";

    private static final String IDS_PARAMETER_NAME = "ids";

    private static final String QUERY_PARAMETER_NAME = "query";

    /**
     * @param context    context
     * @param parameters parameters
     * @return sql
     */
    public String update(final ProviderContext context, final Map<String, Object> parameters) {

        return provide(context, parameters);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void doProvide(final StringBuilder sql, final ProviderContext context,
                          final Map<String, Object> parameters) {

        final Object query = parameters.get(QUERY_PARAMETER_NAME);

        Class<?> entity = getEntityClass(context.getMapperType());

        final QEntity<?, ?> properties = DefaultQEntityFactory.INSTANCE.create(entity);
        parameters.put(PROPERTIES_PARAMETER_NAME, properties);

        injectLastModifier(entity, properties, parameters);

        sql.append("<trim prefix=\"UPDATE\">").append("${table}").append("</trim>");

        sql.append("<set>");

        if (parameters.containsKey(UPDATE_PARAMETER_NAME) && parameters.get(UPDATE_PARAMETER_NAME) != null) {
            final Update updates = (Update) parameters.get(UPDATE_PARAMETER_NAME);
            for (int i = 0; i < updates.size(); i++) {
                Criterion criterion = updates.get(i);

                if (criterion instanceof CriterionAttributes) {
                    CriterionAttributes fragment = new CriterionAttributes();
                    fragment.putAll((CriterionAttributes) criterion);
                    fragment.setValue(String.format("update[%d]", i));
                    String value = Velocities.getValue(fragment.getExpression(), fragment);
                    sql.append(value);
                } else {
                    throw new IllegalArgumentException("update not support criterion of " + criterion.getClass());
                }

            }

        } else {
            appendEntitySet(sql, properties);
        }

        appendVersionProperty(sql, properties);

        sql.append("</set>");

        if (parameters.containsKey(IDS_PARAMETER_NAME) && parameters.get(IDS_PARAMETER_NAME) != null) {
            sql.append(whereIdsNotNull());
        } else {
            appendQuery(sql, entity, query);
        }

    }

    /**
     * Inject LastModifier into update sql from {@link UserContextHolder}
     *
     * @param entity     entity
     * @param properties properties
     * @param parameters parameters
     * @since 1.2.2
     */
    private void injectLastModifier(Class<?> entity, final QEntity<?, ?> properties, final Map<String, Object> parameters) {
        final IUser<?> user = UserContextHolder.getUser();
        if (IRecord.class.isAssignableFrom(entity) && Objects.nonNull(user)) {
            if (parameters.containsKey(ENTITY_PARAMETER_NAME) && Objects.nonNull(parameters.get(ENTITY_PARAMETER_NAME))) {
                final IRecord record = (IRecord) parameters.get(ENTITY_PARAMETER_NAME);
                record.setLastModifier(UserContextHolder.getUser());
            } else if (parameters.containsKey(UPDATE_PARAMETER_NAME) && Objects.nonNull(parameters.get(UPDATE_PARAMETER_NAME))) {
                final Update update = (Update) parameters.get(UPDATE_PARAMETER_NAME);
                update.set(properties.getRequiredProperty("creator.id"), user.getId());
                update.set(properties.getRequiredProperty("creator.name"), user.getName());
            }
        }

    }

    /**
     * @param sql    sql
     * @param entity entity
     */
    private void appendEntitySet(final @NonNull StringBuilder sql, final @NonNull QEntity<?, ?> entity) {

        entity.stream()
                .filter(QProperty::isModifiable)
                .forEach(property -> {
                    // <if test="properties.property.hasView(view)>"
                    sql.append("<if test=\"properties.getRequiredProperty('")
                            .append(property.getPath())
                            .append("').hasView(view)\">");

                    final Metadata metadata = new Metadata();

                    metadata.setProperty(property.getName());
                    metadata.setColumn(property.getColumn());
                    metadata.setValue("entity." + property.getPath());
                    metadata.setJavaType(property.getType());
                    if (Objects.nonNull(property.getTypeHandler())) {
                        metadata.setTypeHandler((Class<? extends TypeHandler>) property.getTypeHandler());
                    }

                    final String writer = Asserts.isBlank(property.getWriter()) ? DEFAULT_WRITER : property.getWriter();
                    final String value = Velocities.getValue(writer, metadata);

                    // <choose>
                    sql.append("<choose>");

                    final String testWithSelective = ScriptMapperHelper
                            .formatTest(ENTITY_PARAMETER_NAME, property.getPath(), true);

                    final String selectiveTest =
                            testWithSelective == null ? SELECTIVE_PARAMETER_NAME : "selective and " + testWithSelective;

                    // <when test="selective and entity.path != null">
                    sql.append("<when test=\"").append(selectiveTest).append("\">")
                            // property.column = entity.path
                            .append(property.getColumn()).append(" = ").append(value).append(",")
                            // </when>
                            .append("</when>");

                    final String testNotWithSelective = ScriptMapperHelper
                            .formatTest(ENTITY_PARAMETER_NAME, property.getPath(), false);
                    final String notSelectiveTest =
                            testNotWithSelective == null ? "!selective" : "!selective and " + testNotWithSelective;

                    sql.append("<when test=\"").append(notSelectiveTest).append("\">")
                            .append(property.getColumn()).append(" = ").append(value).append(",")
                            .append("</when>");

                    if (testNotWithSelective != null) {
                        sql.append("<otherwise>")
                                .append(property.getColumn()).append(" = ").append("null").append(",")
                                .append("</otherwise>");
                    }

                    sql.append("</choose>");

                    sql.append("</if>");
                });
    }

    private void appendVersionProperty(final StringBuilder sql, final QEntity<?, ?> entity) {

        if (!entity.hasVersionProperty()) {
            return;
        }

        sql.append("<if test=\"properties.hasVersionProperty()\">");
        String version = entity.getVersionProperty().getColumn();
        sql.append(version)
                .append(" = ")
                .append(version)
                .append(" + 1,");

        sql.append("</if>");
    }

}

