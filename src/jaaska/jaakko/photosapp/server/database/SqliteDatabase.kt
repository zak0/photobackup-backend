package jaaska.jaakko.photosapp.server.database

import jaaska.jaakko.photosapp.server.Logger
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

abstract class SqliteDatabase(private val dbFilePath: String, private val dbVersion: Int) {

    private var initialized = false

    /**
     * Called when the database is created for the first time. Use this to create initial database schema.
     */
    abstract fun onCreate(connection: Connection)

    /**
     * Called when [dbVersion] is greater than the version of already existing database. Use this to perform schema
     * migrations.
     */
    abstract fun onUpgrade(connection: Connection, fromVersion: Int, toVersion: Int)

    /**
     * Executes operations within [block] with [Connection] to the database. Takes care of opening and closing the
     * database after the operations are executed.
     *
     * @throws SQLException if things go wrong
     */
    fun <T> dbIo(block: (Connection) -> T): T {
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbFilePath")

        if (!initialized) {
            initialize(connection)
        }

        val ret = block(connection)
        connection.close()
        return ret
    }

    /**
     * Executes an arbitrary SQL statement in the database.
     *
     * @throws SQLException if things go wrong
     */
    fun execSql(connection: Connection, sql: String) {
        val statement = connection.createStatement()
        statement.executeUpdate(sql)
    }

    /**
     * Executes an SQL statement resulting in a [ResultSet] in the database.
     *
     * @return [ResultSet] of the given query
     * @throws SQLException if things go wrong
     */
    fun execQuery(connection: Connection, sql: String): ResultSet {
        val statement = connection.createStatement()
        return statement.executeQuery(sql)
    }

    /**
     * Initializes the database. This should be called before the first use of the DB. This only needs to be done once
     * per [SqliteDatabase] lifecycle.
     *
     * The init process:
     * 1. Check whether the database is already created. Create the DB by calling [onCreate], if not.
     * 2. If database was created already, check if the existing (schema) version is less than current. Run [onUpgrade],
     *    if yes.
     */
    private fun initialize(connection: Connection) {
        // Check if meta table exists.
        val sql = QueryBuilder(QueryBuilder.QueryType.SELECT_ALL, "sqlite_master")
                .addTextCondition("name", "databasemeta")
                .build()
        val dbWasCreated = rowCount(execQuery(connection, sql)) > 0

        if (!dbWasCreated) {
            Logger.i("No existing database found, creating a new one.")
            // If meta table did not exist, it means database had not been created. So let's create it.
            createDatabase(connection)
            Logger.i("Database creation complete.")
        } else {
            // If meta table existed, check what the previous database version was.
            val versionSql = QueryBuilder(QueryBuilder.QueryType.SELECT, "databasemeta")
                    .addField("version")
                    .build()
            val previousVersion = execQuery(connection, versionSql).getInt("version")

            if (previousVersion < dbVersion) {
                Logger.i("Upgrading database...")
                // Run DB upgrade if version is older than current
                onUpgrade(connection, previousVersion, dbVersion)

                // ... and update version to current in the database meta table
                QueryBuilder(QueryBuilder.QueryType.UPDATE, "databasemeta")
                        .addIntegerValue("version", dbVersion)
                        .build()
                        .also { execSql(connection, it) }
                Logger.i("Database upgrade complete.")
            }
        }

        initialized = true
        Logger.i("Database initialization complete.")
    }

    /**
     * Creates initial database.
     */
    private fun createDatabase(connection: Connection) {
        // Create DB metadata table
        QueryBuilder(QueryBuilder.QueryType.CREATE_TABLE, "databasemeta")
                .addField("version", QueryBuilder.FieldType.INTEGER, nullable = false)
                .addField("id", QueryBuilder.FieldType.TEXT, nullable = false)
                .build()
                .also { execSql(connection, it) }

        // Insert DB version into this new table
        QueryBuilder(QueryBuilder.QueryType.INSERT, "databasemeta")
                .addIntegerValue("version", dbVersion)
                .build()
                .also { execSql(connection, it) }

        // Finally run onCreate to create the schema for implementation
        onCreate(connection)
    }

    private fun rowCount(resultSet: ResultSet): Int {
        var count = 0
        try {
            while (resultSet.next()) {
                count++
            }
        } catch (e: Exception) {
        }

        return count
    }
}
