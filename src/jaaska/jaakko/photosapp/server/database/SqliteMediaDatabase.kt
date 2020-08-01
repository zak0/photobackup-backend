package jaaska.jaakko.photosapp.server.database

import jaaska.jaakko.photosapp.server.model.MediaMeta
import java.sql.Connection

class SqliteMediaDatabase(metaRoot: String) : MediaDatabase, SqliteDatabase("$metaRoot\\meta.db", 2) {

    override fun onCreate(connection: Connection) {
        val createMediaSql = QueryBuilder(QueryBuilder.QueryType.CREATE_TABLE, "media")
            .addField("id", QueryBuilder.FieldType.INTEGER, nullable = false, primaryKey = true)
            .addField("filename", QueryBuilder.FieldType.TEXT)
            .addField("dirpath", QueryBuilder.FieldType.TEXT)
            .addField("filesize", QueryBuilder.FieldType.INTEGER)
            .addField("checksum", QueryBuilder.FieldType.TEXT)
            .addField("status", QueryBuilder.FieldType.TEXT, nullable = false)
            .addField("datetimeoriginal", QueryBuilder.FieldType.TEXT)
            .build()

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

            while (result.isAfterLast) {
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
                result.next()
            }
        }

        return ret
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
        TODO("Not yet implemented")
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
        val lastInsertId = result.getInt("seq")

        return lastInsertId
    }
}