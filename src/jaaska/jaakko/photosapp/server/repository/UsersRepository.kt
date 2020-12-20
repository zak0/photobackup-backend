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