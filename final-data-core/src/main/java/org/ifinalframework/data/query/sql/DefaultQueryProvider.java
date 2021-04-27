/*
 * Copyright 2020-2021 the original author or authors.
 *
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

package org.ifinalframework.data.query.sql;

import org.ifinalframework.data.query.criterion.VelocityCriterionValue;
import org.ifinalframework.query.AndOr;
import org.ifinalframework.query.Criteria;
import org.ifinalframework.query.Criterion;
import org.ifinalframework.query.CriterionAttributes;
import org.ifinalframework.query.Query;
import org.ifinalframework.velocity.Velocities;

/**
 * DefaultQueryProvider.
 *
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
public class DefaultQueryProvider extends AbsQueryProvider {

    private final Query query;

    public DefaultQueryProvider(final Query query) {
        this.query = query;
    }

    @Override
    public String where() {

        final StringBuilder sql = new StringBuilder();

        sql.append("<where>");

        appendCriteria(sql, query.getCriteria(), AndOr.AND, "query.criteria");

        sql.append("</where>");

        return sql.toString();

    }

    private void appendCriteria(StringBuilder sql, Criteria criteria, AndOr andOr, String expression) {
        for (int i = 0; i < criteria.size(); i++) {
            Criterion criterion = criteria.get(i);

            if (criterion instanceof CriterionAttributes) {
                CriterionAttributes attributes = ((CriterionAttributes) criterion);

                CriterionAttributes target = new CriterionAttributes();
                target.putAll(attributes);
                target.put("criterion", String.format("%s[%d]", expression, i));

                String column = target.getColumn();

                if (column.contains("${") || column.contains("#{")) {
                    column = Velocities.getValue(column, target);
                    target.setColumn(column);
                }

                target.put(CriterionAttributes.ATTRIBUTE_NAME_AND_OR, andOr);
                target.put(CriterionAttributes.ATTRIBUTE_NAME_VALUE, String.format("%s[%d].value", expression, i));

                String value = new VelocityCriterionValue(
                    attributes.getString(CriterionAttributes.ATTRIBUTE_NAME_EXPRESSION))
                    .value(target);
                sql.append(value);
            } else if (criterion instanceof Criteria) {

                sql.append("<trim prefix=\"").append(andOr).append(" (\" suffix=\")\" prefixOverrides=\"AND |OR\">");

                Criteria loopCriteria = (Criteria) criterion;
                appendCriteria(sql, loopCriteria, loopCriteria.getAndOr(), String.format("%s[%d]", expression, i));

                sql.append("</trim>");

            }

        }
    }

}
