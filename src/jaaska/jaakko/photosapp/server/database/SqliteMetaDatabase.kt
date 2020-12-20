package jaaska.jaakko.photosapp.server.database

import jaaska.jaakko.photosapp.server.configuration.Config
import jaaska.jaakko.photosapp.server.extension.OS_PATH_SEPARATOR
import jaaska.jaakko.photosapp.server.model.MediaMeta
import jaaska.jaakko.photosapp.server.model.User
import jaaska.jaakko.photosapp.server.model.UserType
import java.sql.Connection
import java.sql.ResultSet

class SqliteMetaDatabase(config: Config) : MediaDatabase, KeyValueDatabase, UsersDatabase,
    SqliteDatabase("${config.metaRootPath}${OS_PATH_SEPARATOR}meta.db", 2) {

    override fun onCreate(connection: Connection) {
        val createKeyValueSql = QueryBuilder(QueryBuilder.QueryType.CREATE_TABLE, "keyvalue")
            .addField("id", QueryBuilder.FieldType.INTEGER, nullable = false, primaryKey = true)
            .addField("key", QueryBuilder.FieldType.TEXT, nullable = false)
            .addField("textvalue", QueryBuilder.FieldType.TEXT)
            .addField("integervalue", QueryBuilder.FieldType.INTEGER)
            .addField("realvalue", QueryBuilder.FieldType.REAL)
            .addField("lastchanged", QueryBuilder.FieldType.INTEGER, nullable = false)
            .build()

        val createUserSql = QueryBuilder(QueryBuilder.QueryType.CREATE_TABLE, "user")
            .addField("id", QueryBuilder.FieldType.INTEGER, nullable = false, primaryKey = true)
            .addField("name", QueryBuilder.FieldType.TEXT, nullable = false)
            .addField("passwordhash", QueryBuilder.FieldType.TEXT, nullable = false)
            .addField("type", QueryBuilder.FieldType.TEXT, nullable = false)
            .build()

        val createMediaSql = QueryBuilder(QueryBuilder.QueryType.CREATE_TABLE, "media")
            .addField("id", QueryBuilder.FieldType.INTEGER, nullable = false, primaryKey = true)
            .addField("filename", QueryBuilder.FieldType.TEXT)
            .addField("dirpath", QueryBuilder.FieldType.TEXT)
            .addField("filesize", QueryBuilder.FieldType.INTEGER)
            .addField("checksum", QueryBuilder.FieldType.TEXT)
            .addField("status", QueryBuilder.FieldType.TEXT, nullable = false)
            .addField("datetimeoriginal", QueryBuilder.FieldType.TEXT)
            .build()

        execSql(connection, createKeyValueSql)
        execSql(connection, createUserSql)
        execSql(connection, createMediaSql)
    }

    override fun onUpgrade(connection: Connection, fromVersion: Int, toVersion: Int) {
        // TODO Implement upgrade paths here
    }

    override fun getMediaMetas(): List<MediaMeta> {
        val ret = ArrayList<MediaMeta>()

        dbIo {
            val sql = QueryBuilder(QueryBuilder.QueryType.SELECT_ALL, "media").build()
            val result = execQuery(it, sql)

            while (result.next()) {
                ret.add(
                    MediaMeta(
                        result.getInt("id"),
                        result.getString("filename"),
                        result.getLong("filesize"),
                        result.getString("dirpath"),
                        result.getString("checksum"),
                        result.getString("datetimeoriginal"),
                        result.getString("status")
                    )
                )
            }
        }

        return ret
    }

    override fun getMediaMeta(id: Int): MediaMeta? {
        var mediaMeta: MediaMeta? = null

        dbIo {
            val sql = QueryBuilder(QueryBuilder.QueryType.SELECT_ALL, "media")
                .addIntegerCondition("id", id)
                .build()
            val result = execQuery(it, sql)

            if (result.next()) {
                mediaMeta = MediaMeta(
                    result.getInt("id"),
                    result.getString("filename"),
                    result.getLong("filesize"),
                    result.getString("dirpath"),
                    result.getString("checksum"),
                    result.getString("datetimeoriginal"),
                    result.getString("status")
                )
            }
        }

        return mediaMeta
    }

    override fun persistMediaMeta(mediaMeta: MediaMeta) {
        dbIo {
            if (mediaMeta.id >= 0) {
                // UPDATE existing
                val sql = QueryBuilder(QueryBuilder.QueryType.UPDATE, "media")
                    .addTextValue("filename", mediaMeta.fileName)
                    .addTextValue("dirpath", mediaMeta.dirPath)
                    .addLongValue("filesize", mediaMeta.fileSize)
                    .addTextValue("checksum", mediaMeta.checksum)
                    .addTextValue("status", mediaMeta.status)
                    .addTextValue("datetimeoriginal", mediaMeta.dateTimeOriginal)
                    .addIntegerCondition("id", mediaMeta.id)
                    .build()

                execSql(it, sql)
            } else {
                // INSERT new
                val sql = QueryBuilder(QueryBuilder.QueryType.INSERT, "media")
                    .addTextValue("filename", mediaMeta.fileName)
                    .addTextValue("dirpath", mediaMeta.dirPath)
                    .addLongValue("filesize", mediaMeta.fileSize)
                    .addTextValue("checksum", mediaMeta.checksum)
                    .addTextValue("status", mediaMeta.status)
                    .addTextValue("datetimeoriginal", mediaMeta.dateTimeOriginal)
                    .build()

                execSql(it, sql)
                mediaMeta.id = getLastInsertId(it, "media")
            }
        }

    }

    override fun deleteMediaMeta(mediaMeta: MediaMeta) {
        dbIo { connection ->
            QueryBuilder(QueryBuilder.QueryType.DELETE, "media")
                .addIntegerCondition("id", mediaMeta.id)
                .build().also { execSql(connection, it) }
        }
    }

    /**
     * Returns the ID of the entry that was added the last time into table [tableName].
     * <b>Note!</b> This requires that the table in question has an auto-incremented integer primary
     * key.
     */
    private fun getLastInsertId(connection: Connection, tableName: String): Int {
        val sql = QueryBuilder(QueryBuilder.QueryType.SELECT, "sqlite_sequence")
            .addField("seq")
            .addTextCondition("name", tableName)
            .build()

        val result = execQuery(connection, sql)
        return result.getInt("seq")
    }

    override fun contains(key: String): Boolean {
        var contains = false

        dbIo {
            val sql = QueryBuilder(QueryBuilder.QueryType.SELECT_ALL, "keyvalue")
                .addTextCondition("key", key)
                .build()
            val result = execQuery(it, sql)

            if (result.next()) {
                contains = true
            }
        }

        return contains
    }

    private fun <T> getValue(forKey: String, processResult: (row: ResultSet?) -> T): T? {
        return dbIo {
            val sql = QueryBuilder(QueryBuilder.QueryType.SELECT_ALL, "keyvalue")
                .addTextCondition("key", forKey)
                .build()

            val result = execQuery(it, sql)

            if (result.next()) {
                processResult(result)
            } else {
                processResult(null)
            }
        }
    }

    override fun getString(key: String): String? = getValue(key) { it?.getString("textvalue") }

    override fun getInt(key: String): Int? = getValue(key) { it?.getInt("integervalue") }

    override fun getLong(key: String): Long? = getValue(key) { it?.getLong("integervalue") }

    override fun getDouble(key: String): Double? = getValue(key) { it?.getDouble("realvalue") }

    private fun <T> putValue(key: String, value: T) {
        val exists = contains(key)

        dbIo {
            val queryType = if (exists) QueryBuilder.QueryType.UPDATE else QueryBuilder.QueryType.INSERT
            val queryBuilder = QueryBuilder(queryType, "keyvalue")
                .addLongValue("lastchanged", System.currentTimeMillis())

            when (value) {
                is String -> queryBuilder.addTextValue("textvalue", value)
                is Int -> queryBuilder.addIntegerValue("integervalue", value)
                is Long -> queryBuilder.addLongValue("integervalue", value)
                is Double -> queryBuilder.addDoubleValue("realvalue", value)
                else -> error("Unsupported value for key-value store: $value")
            }

            if (exists) {
                queryBuilder.addTextCondition("key", key)
            } else {
                queryBuilder.addTextValue("key", key)
            }

            execSql(it, queryBuilder.build())
        }
    }

    override fun putString(key: String, value: String) = putValue(key, value)

    override fun putInt(key: String, value: Int) = putValue(key, value)

    override fun putLong(key: String, value: Long) = putValue(key, value)

    override fun putDouble(key: String, value: Double) = putValue(key, value)

    override fun delete(key: String) {
        dbIo { connection ->
            QueryBuilder(QueryBuilder.QueryType.DELETE, "keyvalue")
                .addTextCondition("key", key)
                .build().also { execSql(connection, it) }
        }
    }

    override fun getAllUsers(): List<User> {
        val ret = ArrayList<User>()

        dbIo {
            val sql = QueryBuilder(QueryBuilder.QueryType.SELECT_ALL, "user").build()
            val result = execQuery(it, sql)

            while (result.next()) {
                ret.add(
                    User(
                        result.getString("name"),
                        result.getString("passwordhash"),
                        UserType.fromString(result.getString("type")),
                        result.getInt("id")
                    )
                )
            }
        }

        return ret
    }

    override fun getUser(id: Int): User? {
        return dbIo {
            val sql = QueryBuilder(QueryBuilder.QueryType.SELECT_ALL, "user")
                .addIntegerCondition("id", id)
                .build()

            val result = execQuery(it, sql)

            if (result.next()) {
                User(
                    result.getString("name"),
                    result.getString("passwordhash"),
                    UserType.fromString(result.getString("type")),
                    result.getInt("id")
                )
            } else {
                null
            }
        }
    }

    override fun persistUser(user: User) {
        dbIo { connection ->
            if (user.id >= 0) {
                // UPDATE existing
                // Only password and account type are changeable!
                val sql = QueryBuilder(QueryBuilder.QueryType.UPDATE, "user")
                    .addTextValue("passwordhash", user.passwordHash)
                    .addTextValue("type", user.type.name)
                    .addIntegerCondition("id", user.id)
                    .build()

                execSql(connection, sql)
            } else {
                // INSERT new
                val sql = QueryBuilder(QueryBuilder.QueryType.INSERT, "user")
                    .addTextValue("name", user.name)
                    .addTextValue("passwordhash", user.passwordHash)
                    .addTextValue("type", user.type.name)
                    .build()

                execSql(connection, sql)
                user.id = getLastInsertId(connection, "user")
            }
        }
    }

    override fun deleteUser(user: User) {
        dbIo { connection ->
            QueryBuilder(QueryBuilder.QueryType.DELETE, "user")
                .addIntegerCondition("id", user.id)
                .build().also { execSql(connection, it) }
        }
    }
}