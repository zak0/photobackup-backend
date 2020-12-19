package jaaska.jaakko.photosapp.server.database

class KeyValueStore(
    val db: KeyValueDatabase
) {

    inline fun <reified T> get(key: String): T? {
        return when (T::class) {
            String::class -> db.getString(key)
            Int::class -> db.getInt(key)
            Long::class -> db.getLong(key)
            Double::class -> db.getDouble(key)
            else -> error("KeyValueStore doesn't support type ${T::class}")
        } as T?
    }

    inline fun <reified T> get(key: String, defaultValue: T): T {
        return get(key) ?: defaultValue
    }

    inline fun <reified T> put(key: String, value: T) {
        when (T::class) {
            String::class -> db.putString(key, value as String)
            Int::class -> db.putInt(key, value as Int)
            Long::class -> db.putLong(key, value as Long)
            Double::class -> db.putDouble(key, value as Double)
            else -> error("KeyValueStore doesn't support type ${T::class}")
        }
    }

    fun contains(key: String): Boolean = db.contains(key)

    fun delete(key: String) = db.delete(key)
}