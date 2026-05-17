/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.ooknight.universe.dbvisitor.dynamic.rule;
import java.util.UUID;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.function.EConsumer;
import io.github.ooknight.universe.dbvisitor.dynamic.DynamicParsed;
import io.github.ooknight.universe.dbvisitor.dynamic.QueryContext;
import io.github.ooknight.universe.dbvisitor.dynamic.SqlArgSource;
import io.github.ooknight.universe.dbvisitor.dynamic.SqlBuilder;
import io.github.ooknight.universe.dbvisitor.dynamic.segment.PlanDynamicSql;
import io.github.ooknight.universe.dbvisitor.dynamic.segment.RuleSqlSegment;
import io.github.ooknight.universe.dbvisitor.dynamic.segment.SqlSegment;
import io.github.ooknight.universe.dbvisitor.error.RuntimeSQLException;
import io.github.ooknight.universe.dbvisitor.internal.OgnlUtils;

public class CaseRule extends AbstractCaseRule {
    public static final SqlRule INSTANCE = new CaseRule();

    @Override
    public boolean test(SqlArgSource data, QueryContext context, String activeExpr) {
        return true;
    }

    private void runInCaseScope(SqlArgSource data, EConsumer<String, RuntimeSQLException> action) {
        // Generate a unique key for tracking state within this specific CASE block
        String caseId = CASE_KEY_PREFIX + UUID.randomUUID();
        Object parentCaseId = null;

        try {
            // Initialize state: NOT_MATCHED
            data.putValue(caseId, Boolean.FALSE);

            if (data.hasValue(CURRENT_CASE_ID_KEY)) {
                parentCaseId = data.getValue(CURRENT_CASE_ID_KEY);
            }
            data.putValue(CURRENT_CASE_ID_KEY, caseId);

            action.eAccept(caseId);
        } finally {
            data.putValue(caseId, null);
            data.putValue(caseId + TEST_EXPR_SUFFIX, null);
            data.putValue(caseId + HAS_TEST_EXPR_KEY, null);
            data.putValue(CURRENT_CASE_ID_KEY, parentCaseId);
        }
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder, String activeExpr, String ruleValue) {
        this.runInCaseScope(data, caseId -> {
            if (StringUtils.isNotBlank(activeExpr)) {
                Object val = OgnlUtils.evalOgnl(activeExpr, data);
                data.putValue(caseId + TEST_EXPR_SUFFIX, val);
                data.putValue(caseId + HAS_TEST_EXPR_KEY, Boolean.TRUE);
            }

            if (StringUtils.isBlank(ruleValue)) {
                return;
            }

            PlanDynamicSql parser = DynamicParsed.getParsedSql(ruleValue);
            for (SqlSegment sqlSegment : parser.getSqlSegments()) {
                if (!(sqlSegment instanceof RuleSqlSegment)) {
                    continue;
                }

                String ruleName = ((RuleSqlSegment) sqlSegment).getRule();
                boolean testResult = StringUtils.equalsIgnoreCase(ruleName, "when") || StringUtils.equalsIgnoreCase(ruleName, "else");
                if (!testResult) {
                    continue;
                }

                // 先尝试开枪（buildQuery 判断条件），如果打中了（getValue 为 true），就收工回家（break）
                //  -- when 规则在执行时还会再次判断 getValue 以确保不会出问题。
                sqlSegment.buildQuery(data, context, sqlBuilder);
                if (Boolean.TRUE.equals(data.getValue(caseId))) {
                    break;
                }
            }
        });
    }
}
