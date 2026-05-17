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
package io.github.ooknight.universe.dbvisitor.dialect.features;
import io.github.ooknight.universe.dbvisitor.dialect.SqlCommandBuilder.ConditionLogic;
import io.github.ooknight.universe.dbvisitor.dialect.SqlDialect;
import io.github.ooknight.universe.dbvisitor.lambda.core.MetricType;

/**
 * SQL 分页方言
 * @author 赵永春 (zyc@hasor.net)
 * @version 2020-10-31
 */
public interface VectorSqlDialect extends SqlDialect {
    /** 添加向量范围查询条件 (col <-> vector) < threshold */
    void addConditionForVectorRange(ConditionLogic logic, String col, String colTerm, //
            Object vector, String vectorTerm, Object threshold, String thresholdTerm, MetricType metricType);

    /** 添加向量排序 */
    void addOrderByVector(String col, String colTerm, Object vector, String vectorTerm, MetricType metricType);
}
