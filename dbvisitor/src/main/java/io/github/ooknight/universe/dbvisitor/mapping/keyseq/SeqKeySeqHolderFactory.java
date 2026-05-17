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
package io.github.ooknight.universe.dbvisitor.mapping.keyseq;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.reflect.Annotation;
import net.hasor.cobble.reflect.Annotations;
import io.github.ooknight.universe.dbvisitor.dialect.SqlDialect;
import io.github.ooknight.universe.dbvisitor.dialect.SqlDialectRegister;
import io.github.ooknight.universe.dbvisitor.dialect.features.SeqSqlDialect;
import io.github.ooknight.universe.dbvisitor.error.RuntimeSQLException;
import io.github.ooknight.universe.dbvisitor.mapping.GeneratedKeyHandler;
import io.github.ooknight.universe.dbvisitor.mapping.GeneratedKeyHandlerContext;
import io.github.ooknight.universe.dbvisitor.mapping.GeneratedKeyHandlerFactory;
import io.github.ooknight.universe.dbvisitor.mapping.KeySeq;
import io.github.ooknight.universe.dbvisitor.mapping.def.ColumnMapping;
import io.github.ooknight.universe.dbvisitor.types.TypeHandler;

/**
 * 支持 @KeySequence 注解方式
 * @author 赵永春 (zyc@hasor.net)
 * @version 2022-12-01
 */
public class SeqKeySeqHolderFactory implements GeneratedKeyHandlerFactory {
    @Override
    public GeneratedKeyHandler createHolder(GeneratedKeyHandlerContext context) {
        Annotations annotations = context.getAnnotations();
        if (annotations == null) {
            return null;
        }
        Annotation keySeq = annotations.getAnnotation(KeySeq.class);
        if (keySeq == null) {
            return null;
        }

        String seqName = keySeq.getString("value", null);
        if (StringUtils.isBlank(seqName)) {
            throw new IllegalArgumentException("@KeySeq config failed, no name specified.");
        }

        boolean useDelimited = context.useDelimited();
        SqlDialect dialect = SqlDialectRegister.findOrDefault(context.getRegistry().getGlobalOptions());
        if (!(dialect instanceof SeqSqlDialect)) {
            throw new ClassCastException(dialect.getClass().getName() + " is not SeqSqlDialect.");
        }
        final String seqQuery = ((SeqSqlDialect) dialect).selectSeq(useDelimited, context.getCatalog(), context.getSchema(), seqName);

        TypeHandler<?> typeHandler = context.getTypeHandler();
        if (typeHandler == null) {
            Class<?> javaType = context.getJavaType();
            Integer jdbcType = context.getJdbcType();
            if (jdbcType != null) {
                typeHandler = context.getRegistry().getTypeRegistry().getTypeHandler(javaType, jdbcType);
            } else {
                typeHandler = context.getRegistry().getTypeRegistry().getTypeHandler(javaType);
            }
        }
        if (typeHandler == null) {
            typeHandler = context.getRegistry().getTypeRegistry().getDefaultTypeHandler();
        }

        TypeHandler<?> finalTypeHandler = typeHandler;
        return new GeneratedKeyHandler() {
            @Override
            public boolean onBefore() {
                return true;
            }

            @Override
            public Object beforeApply(Connection conn, Object entity, ColumnMapping mapping) {
                Object var = selectSeq(seqQuery, conn, finalTypeHandler);
                mapping.getHandler().set(entity, var);
                return var;
            }

            @Override
            public String toString() {
                return "Sequence@" + this.hashCode();
            }
        };
    }

    protected Object selectSeq(String queryStr, Connection conn, TypeHandler<?> typeHandler) {
        try (Statement s = conn.createStatement()) {
            try (ResultSet res = s.executeQuery(queryStr)) {
                if (res.next()) {
                    return typeHandler.getResult(res, 1);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }
}
