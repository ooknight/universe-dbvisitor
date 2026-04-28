package net.hasor.dbvisitor.dynamic.dto;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.hasor.dbvisitor.types.handler.AbstractTypeHandler;

public class MyTypeHandler extends AbstractTypeHandler<String> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, Integer jdbcType) {

    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) {
        return null;
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) {
        return null;
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) {
        return null;
    }
}
