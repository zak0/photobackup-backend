package jaaska.jaakko.photosapp.server.database

import jaaska.jaakko.photosapp.server.extension.appendWithSafeSpace

/**
 * Utility for constructing consistent SQL query strings.
 *
 * Purpose of this is to prevent having to write (complex) SQL queries by hand and thus reducing
 * the chances of silly syntactical errors.
 */
internal class QueryBuilder(private val queryType: QueryType, private var tableName: String = "") {

    // region SUBCLASSES
    enum class QueryType {
        CREATE_TABLE,
        SELECT,
        SELECT_ALL,
        INSERT,
        UPDATE,
        DELETE
    }

    enum class FieldType(val sql: String) {
        TEXT("TEXT"),
        INTEGER("INTEGER"),
        REAL("REAL")
    }

    private class FieldSpec(
        val name: String,
        val type: FieldType,
        val nullable: Boolean,
        val primaryKeyAutoIncrement: Boolean
    )

    private abstract class ColumnValue(
        val columnName: String
    ) {
        abstract val value: String?
    }

    private class TextColumnValue(
        columnName: String,
        columnValue: String?
    ) : ColumnValue(columnName) {
        // Text value is surrounded in single quotes in the resulting query instead of usual double
        // quotes. This is to be more compatible with SQL syntax (for example, escaping double
        // quotes is not a thing in SQLite).
        override val value: String? = columnValue?.let { "'$it'" }
    }

    private class IntegerColumnValue(
        columnName: String,
        columnValue: Int?
    ) : ColumnValue(columnName) {
        override val value: String? = columnValue?.let { "$it" }
    }

    private class LongColumnValue(
        columnName: String,
        columnValue: Long?
    ) : ColumnValue(columnName) {
        override val value: String? = columnValue?.let { "$it" }
    }

    private class DoubleColumnValue(
        columnName: String,
        columnValue: Double?
    ) : ColumnValue(columnName) {
        override val value: String? = columnValue?.let { "$it" }
    }
    // endregion

    // region MEMBER VARIABLES
    /**
     * Array containing all field specifications. This is only used for [QueryType.CREATE_TABLE].
     */
    private val createTableFields = ArrayList<FieldSpec>()

    /**
     * Array containing fields to SELECT. This is only used for [QueryType.SELECT]
     */
    private val selectFields = ArrayList<String>()

    /**
     * Array containing all column values. This is only used for [QueryType.INSERT].
     */
    private val columnValues = ArrayList<ColumnValue>()

    /**
     * Array containing all column values. Used for [QueryType.INSERT], [QueryType.UPDATE],
     * [QueryType.DELETE], [QueryType.SELECT_ALL] and [QueryType.SELECT].
     */
    private val conditionValues = ArrayList<ColumnValue>()

    /**
     * Array containing fields to add as ORDER BY in [QueryType.SELECT] and
     * [QueryType.SELECT_ALL] queries. When ordering is desired to be descending, DESC is appended
     * to the field name before it's inserted into this array.
     */
    private val orderByFields = ArrayList<String>()
    // endregion

    // region PUBLIC METHODS
    fun setTable(table: String): QueryBuilder {
        tableName = table
        return this
    }

    /**
     * Adds a field for table modification. For SELECT, use the override with only one [String]
     * parameter.
     */
    fun addField(
        fieldName: String,
        fieldType: FieldType,
        nullable: Boolean = true,
        primaryKey: Boolean = false
    ): QueryBuilder {
        createTableFields.add(FieldSpec(fieldName, fieldType, nullable, primaryKey))
        return this
    }

    /**
     * Adds a field for SELECT. For table modification, use the override with detailed field
     * specification parameters.
     *
     * Use [QueryType.SELECT_ALL] for "SELECT * FROM ..." type query.
     */
    fun addField(fieldName: String): QueryBuilder {
        selectFields.add(fieldName)
        return this
    }

    /**
     * Adds an [Int] value into a data modification query.
     */
    fun addIntegerValue(
        fieldName: String,
        value: Int?
    ): QueryBuilder {
        columnValues.add(IntegerColumnValue(fieldName, value))
        return this
    }

    /**
     * Adds a [Long] value into a data modification query.
     */
    fun addLongValue(
        fieldName: String,
        value: Long?
    ): QueryBuilder {
        columnValues.add(LongColumnValue(fieldName, value))
        return this
    }

    /**
     * Adds a [Double] value into a data modification query.
     */
    fun addDoubleValue(
        fieldName: String,
        value: Double?
    ): QueryBuilder {
        columnValues.add(DoubleColumnValue(fieldName, value))
        return this
    }

    /**
     * Adds a textual string value into a data modification query.
     */
    fun addTextValue(
        fieldName: String,
        value: String?
    ): QueryBuilder {
        // Quotes need to be escaped also in the query, so let's replace quotes with escaped quotes
        val quoteSafeValue = value?.replace("'", "''")
        columnValues.add(TextColumnValue(fieldName, quoteSafeValue))
        return this
    }

    /**
     * Adds an integer "WHERE" condition into the query.
     */
    fun addIntegerCondition(
        fieldName: String,
        value: Int?
    ): QueryBuilder {
        conditionValues.add(IntegerColumnValue(fieldName, value))
        return this
    }

    /**
     * Adds a textual "WHERE" condition into the query.
     */
    fun addTextCondition(
        fieldName: String,
        value: String?
    ): QueryBuilder {
        conditionValues.add(TextColumnValue(fieldName, value))
        return this
    }

    /**
     * Adds a field to be used as an ORDER BY setting. If ordering should be descending, set
     * [descending] to true. This will append "DESC" after the field name.
     */
    fun addOrderByField(fieldName: String, descending: Boolean = false): QueryBuilder {
        orderByFields.add("`${fieldName}`${if (descending) " DESC" else ""}")
        return this
    }

    /**
     * Builds the SQL query into a [String].
     */
    fun build(): String {
        val sb = when (queryType) {
            QueryType.CREATE_TABLE -> buildCreateTable()
            QueryType.INSERT -> buildInsert()
            QueryType.UPDATE -> buildUpdate()
            QueryType.SELECT_ALL -> buildSelectAll()
            QueryType.SELECT -> buildSelect()
            QueryType.DELETE -> buildDelete()
        }

        // Trim away excess spaces at the beginning and the end
        // Replace any occurrences of two or more spaces with one space
        return sb.trim().replace(Regex(" {2,}"), " ")
    }
    // endregion

    // region PRIVATE METHODS
    private fun buildCreateTable(): StringBuilder {
        val sb = StringBuilder()
        sb.append("CREATE TABLE")
            .appendWithSafeSpace("`$tableName`")
            .append("(")
        createTableFields.forEachIndexed { index, field ->
            val isLast = index == createTableFields.size - 1
            sb.appendWithSafeSpace("`${field.name}`")
                .appendWithSafeSpace(field.type.sql)
            if (!field.nullable) {
                sb.appendWithSafeSpace("NOT NULL")
            }
            if (field.primaryKeyAutoIncrement) {
                sb.appendWithSafeSpace("PRIMARY KEY AUTOINCREMENT")
            }
            if (!isLast) {
                sb.append(",")
            }
        }
        sb.append(")")

        return sb
    }

    private fun buildInsert(): StringBuilder {
        val sb = StringBuilder()
        sb.append("INSERT INTO")
            .appendWithSafeSpace("`$tableName`")
            .append("(")
        columnValues.forEachIndexed { index, columnValue ->
            val isLast = index == columnValues.size - 1
            sb.appendWithSafeSpace("`${columnValue.columnName}`")
            if (!isLast) {
                sb.append(",")
            }
        }
        sb.append(") VALUES (")
        columnValues.forEachIndexed { index, columnValue ->
            val isLast = index == columnValues.size - 1
            sb.appendWithSafeSpace(columnValue.value ?: "null")
            if (!isLast) {
                sb.append(",")
            }
        }
        sb.append(")")

        return sb
    }

    private fun buildUpdate(): StringBuilder {
        val sb = StringBuilder()
        sb.append("UPDATE")
            .appendWithSafeSpace("`$tableName`")
            .appendWithSafeSpace("SET")
        columnValues.forEachIndexed { index, columnValue ->
            val isLast = index == columnValues.size - 1
            sb.appendWithSafeSpace("`${columnValue.columnName}`")
                .append("=")
                .appendWithSafeSpace(columnValue.value ?: "null")

            if (!isLast) {
                sb.append(",")
            }
        }
        appendConditions(sb)
        return sb
    }

    private fun buildSelectAll(): StringBuilder {
        val sb = StringBuilder()
        sb.append("SELECT * FROM")
            .appendWithSafeSpace("`$tableName`")

        appendConditions(sb)
        appendOrdering(sb)
        return sb
    }

    private fun buildSelect(): StringBuilder {
        val sb = StringBuilder()
        sb.append("SELECT")

        selectFields.forEachIndexed { index, selectField ->
            val isLast = index == selectFields.size - 1
            sb.appendWithSafeSpace("`$selectField`")
            if (!isLast) {
                sb.append(",")
            }
        }

        sb.appendWithSafeSpace("FROM")
            .appendWithSafeSpace("`$tableName`")

        appendConditions(sb)
        appendOrdering(sb)
        return sb
    }

    private fun buildDelete(): StringBuilder {
        val sb = StringBuilder()
            .append("DELETE FROM")
            .appendWithSafeSpace("`$tableName`")

        appendConditions(sb)
        return sb
    }

    private fun appendConditions(sb: StringBuilder): StringBuilder {
        // Only append conditions, if at least one is set
        if (conditionValues.size > 0) {
            sb.appendWithSafeSpace("WHERE")
            conditionValues.forEachIndexed { index, conditionValue ->
                val isLast = index == conditionValues.size - 1
                sb.appendWithSafeSpace("`${conditionValue.columnName}`")
                    .append("=")
                    .appendWithSafeSpace(conditionValue.value ?: "null")
                if (!isLast) {
                    sb.append("AND")
                }
            }
        }
        return sb
    }

    private fun appendOrdering(sb: StringBuilder): StringBuilder {
        if (orderByFields.size > 0) {
            sb.appendWithSafeSpace("ORDER BY")
            orderByFields.forEachIndexed { index, field ->
                val isLast = index == orderByFields.size - 1
                sb.appendWithSafeSpace(field)
                if (!isLast) {
                    sb.append(",")
                }
            }
        }
        return sb
    }
    // endregion
}
