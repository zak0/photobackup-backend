package jaaska.jaakko.photosapp.server.repository

import jaaska.jaakko.photosapp.server.Logger
import jaaska.jaakko.photosapp.server.configuration.Config
import jaaska.jaakko.photosapp.server.database.UsersDatabase
import jaaska.jaakko.photosapp.server.extension.md5
import jaaska.jaakko.photosapp.server.extension.contains
import jaaska.jaakko.photosapp.server.model.User
import jaaska.jaakko.photosapp.server.model.UserType

class UsersRepository(
    private val db: UsersDatabase,
    private val config: Config
) {

    /**
     * Initializes user accounts by adding an admin account with default admin password from configuration.
     *
     * This is done if there are no admin accounts defined to ensure not losing access to the system.
     */
    fun initWithAdminUserIfNeeded() {
        if (!db.getAllUsers().contains { it.type == UserType.Admin }) {
            Logger.i("No admin account found. Creating account 'admin' with password from configuration.")

            val defaultAdminUser = User("admin", config.initialAdminPassword.md5(), UserType.Admin)
            db.persistUser(defaultAdminUser)
        }
    }

    fun getAll(): List<User> = db.getAllUsers()

    fun getUser(id: Int): User? = db.getUser(id)

    /**
     * Creates given [user] into the system.
     *
     * @return [User] with filled in changes made by the server, null if creation fails.
     */
    fun createUser(user: User): User? {
        return if (!getAll().contains { it.name == user.name }) {
            db.persistUser(user)
            user.takeIf { it.id >= 0 }
        } else {
            null
        }
    }

    /**
     * Updates given [user]'s record in the system. Use this to change password or user type.
     *
     * @return [User] with done changes applied.
     */
    fun updateUser(user: User): User {
        db.persistUser(user)

        // Check if the changes were applied. Return user if they were, null if not (this means update failed).
        return getAll().first { it.id == user.id }
    }

    fun deleteUser(user: User) = db.deleteUser(user)

    /**
     * @return [User] matching given username and password, if also its type is [UserType.Admin]. Otherwise null.
     */
    fun validateAdmin(username: String, password: String): User? =
        validateUser(username, password)?.takeIf { it.type == UserType.Admin }

    /**
     * @return [User] matching given username and password. If no match is found, null.
     */
    fun validateUser(username: String, password: String): User? =
        db.getAllUsers().firstOrNull { it.name == username }?.takeIf { it.passwordHash == password.md5() }

}
