package jaaska.jaakko.photosapp.server.database

import jaaska.jaakko.photosapp.server.model.User

interface UsersDatabase {

    fun getAllUsers(): List<User>
    fun getUser(id: Int): User?
    fun persistUser(user: User)
    fun deleteUser(user: User)

}