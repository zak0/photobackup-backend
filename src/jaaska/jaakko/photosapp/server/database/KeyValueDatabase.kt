package jaaska.jaakko.photosapp.server.database

interface KeyValueDatabase {
    fun contains(key: String): Boolean
    fun delete(key: String)

    fun getString(key: String): String?
    fun getInt(key: String): Int?
    fun getLong(key: String): Long?
    fun getDouble(key: String): Double?

    fun putString(key: String, value: String)
    fun putInt(key: String, value: Int)
    fun putLong(key: String, value: Long)
    fun putDouble(key: String, value: Double)
}