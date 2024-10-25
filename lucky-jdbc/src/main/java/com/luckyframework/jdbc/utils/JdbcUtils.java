package com.luckyframework.jdbc.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/9/30 11:06 上午
 */
public abstract class JdbcUtils {

    private final static Logger logger = LoggerFactory.getLogger(JdbcUtils.class);

    public static void close(ResultSet rs,Statement st,Connection conn){
        closeResultSet(rs);
        closeStatement(st);
        closeConnect(conn);
    }

    public static void close(ResultSet rs,PreparedStatement ps,Connection conn){
        closeResultSet(rs);
        closePreparedStatement(ps);
        closeConnect(conn);
    }

    public static void closeResultSet(ResultSet rs){
        if(rs != null){
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("An exception occurred while closing the ResultSet.",e);
            }
        }
    }

    public static void closeStatement(Statement statement){
        if(statement != null){
            try {
                statement.close();
            } catch (SQLException e) {
                logger.error("An exception occurred while closing the Statement.",e);
            }
        }
    }

    public static void closePreparedStatement(PreparedStatement preparedStatement){
        if(preparedStatement != null){
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                logger.error("An exception occurred while closing the PreparedStatement.",e);
            }
        }
    }

    public static void closeConnect(Connection connection){
        if(connection != null){
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("An exception occurred while closing the Connection.",e);
            }
        }
    }

    public static String lookupColumnName(ResultSetMetaData resultSetMetaData,int columnIndex) throws SQLException {
        String name = resultSetMetaData.getColumnLabel(columnIndex);
        if(!StringUtils.hasLength(name)){
            name = resultSetMetaData.getColumnName(columnIndex);
        }
        return name;
    }

    @Nullable
    public static Object getResultSetValue(ResultSet rs, int index, @Nullable Class<?> requiredType) throws SQLException {
        if (requiredType == null) {
            return getResultSetValue(rs, index);
        }

        Object value;

        // Explicitly extract typed value, as far as possible.
        if (String.class == requiredType) {
            return rs.getString(index);
        }
        else if (boolean.class == requiredType || Boolean.class == requiredType) {
            value = rs.getBoolean(index);
        }
        else if (byte.class == requiredType || Byte.class == requiredType) {
            value = rs.getByte(index);
        }
        else if (short.class == requiredType || Short.class == requiredType) {
            value = rs.getShort(index);
        }
        else if (int.class == requiredType || Integer.class == requiredType) {
            value = rs.getInt(index);
        }
        else if (long.class == requiredType || Long.class == requiredType) {
            value = rs.getLong(index);
        }
        else if (float.class == requiredType || Float.class == requiredType) {
            value = rs.getFloat(index);
        }
        else if (double.class == requiredType || Double.class == requiredType ||
                Number.class == requiredType) {
            value = rs.getDouble(index);
        }
        else if (BigDecimal.class == requiredType) {
            return rs.getBigDecimal(index);
        }
        else if (java.sql.Date.class == requiredType) {
            return rs.getDate(index);
        }
        else if (java.sql.Time.class == requiredType) {
            return rs.getTime(index);
        }
        else if (java.sql.Timestamp.class == requiredType || java.util.Date.class == requiredType) {
            return rs.getTimestamp(index);
        }
        else if (byte[].class == requiredType) {
            return rs.getBytes(index);
        }
        else if (Blob.class == requiredType) {
            return rs.getBlob(index);
        }
        else if (Clob.class == requiredType) {
            return rs.getClob(index);
        }
        else if (requiredType.isEnum()) {
            // Enums can either be represented through a String or an enum index value:
            // leave enum type conversion up to the caller (e.g. a ConversionService)
            // but make sure that we return nothing other than a String or an Integer.
            Object obj = rs.getObject(index);
            if (obj instanceof String) {
                return obj;
            }
            else if (obj instanceof Number) {
                // Defensively convert any Number to an Integer (as needed by our
                // ConversionService's IntegerToEnumConverterFactory) for use as index
                return NumberUtils.convertNumberToTargetClass((Number) obj, Integer.class);
            }
            else {
                // e.g. on Postgres: getObject returns a PGObject but we need a String
                return rs.getString(index);
            }
        }

        else {
            // Some unknown type desired -> rely on getObject.
            try {
                return rs.getObject(index, requiredType);
            }
            catch (AbstractMethodError err) {
                logger.debug("JDBC driver does not implement JDBC 4.1 'getObject(int, Class)' method", err);
            }
            catch (SQLFeatureNotSupportedException ex) {
                logger.debug("JDBC driver does not support JDBC 4.1 'getObject(int, Class)' method", ex);
            }
            catch (SQLException ex) {
                logger.debug("JDBC driver has limited support for JDBC 4.1 'getObject(int, Class)' method", ex);
            }

            // Corresponding SQL types for JSR-310 / Joda-Time types, left up
            // to the caller to convert them (e.g. through a ConversionService).
            String typeName = requiredType.getSimpleName();
            if ("LocalDate".equals(typeName)) {
                return rs.getDate(index);
            }
            else if ("LocalTime".equals(typeName)) {
                return rs.getTime(index);
            }
            else if ("LocalDateTime".equals(typeName)) {
                return rs.getTimestamp(index);
            }

            // Fall back to getObject without type specification, again
            // left up to the caller to convert the value if necessary.
            return getResultSetValue(rs, index);
        }

        // Perform was-null check if necessary (for results that the JDBC driver returns as primitives).
        return (rs.wasNull() ? null : value);
    }

    @Nullable
    public static Object getResultSetValue(ResultSet rs, int index) throws SQLException {
        Object obj = rs.getObject(index);
        String className = null;
        if (obj != null) {
            className = obj.getClass().getName();
        }
        if (obj instanceof Blob) {
            Blob blob = (Blob) obj;
            obj = blob.getBytes(1, (int) blob.length());
        }
        else if (obj instanceof Clob) {
            Clob clob = (Clob) obj;
            obj = clob.getSubString(1, (int) clob.length());
        }
        else if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
            obj = rs.getTimestamp(index);
        }
        else if (className != null && className.startsWith("oracle.sql.DATE")) {
            String metaDataClassName = rs.getMetaData().getColumnClassName(index);
            if ("java.sql.Timestamp".equals(metaDataClassName) || "oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
                obj = rs.getTimestamp(index);
            }
            else {
                obj = rs.getDate(index);
            }
        }
        else if (obj instanceof java.sql.Date) {
            if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(index))) {
                obj = rs.getTimestamp(index);
            }
        }
        return obj;
    }

    public static String convertUnderscoreNameToPropertyName(@Nullable String name) {
        StringBuilder result = new StringBuilder();
        boolean nextIsUpper = false;
        if (name != null && name.length() > 0) {
            if (name.length() > 1 && name.charAt(1) == '_') {
                result.append(Character.toUpperCase(name.charAt(0)));
            }
            else {
                result.append(Character.toLowerCase(name.charAt(0)));
            }
            for (int i = 1; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c == '_') {
                    nextIsUpper = true;
                }
                else {
                    if (nextIsUpper) {
                        result.append(Character.toUpperCase(c));
                        nextIsUpper = false;
                    }
                    else {
                        result.append(Character.toLowerCase(c));
                    }
                }
            }
        }
        return result.toString();
    }

}
