/*
 Wrapper for database access
*/

const sqlite3 = require("sqlite3")
const metaDir = require("../config").metaDir
const constants = require("../core/constants")
const dbFile = metaDir + "/db.db"

const db = new sqlite3.Database(dbFile, sqlite3.OPEN_READWRITE | sqlite3.OPEN_CREATE, err => {
    if (err) {
        console.log(err)
    }
})

function getAllMedia(offset, limit, callback) {
    db.all(`SELECT id, filename, filesize, datetimeoriginal, hash, status FROM media ORDER BY datetimeoriginal DESC LIMIT ${offset}, ${limit}`,
            callback)
}

function getAllMediaForProcessing(callback) {
    db.all("SELECT id, filename, dirpath FROM media WHERE status = ?", constants.MediaState.STATE_PROCESSING, callback)
}

function getMediaForId(id, callback) {
    db.get("SELECT id, filename, dirpath, filesize, datetimeoriginal, hash, status FROM media WHERE id = ?", id, callback)
}

function getMediaIdForMeta(fileMeta, callback) {
    db.get(`SELECT id FROM media WHERE filename = ? AND dirpath = ? AND filesize = ? AND hash = ?`,
    [fileMeta.fileName, fileMeta.dirPath, fileMeta.fileSize, fileMeta.hash], callback)
}

function updateMediaStatus(fileMeta, callback) {
    db.run(`UPDATE media SET status = ? WHERE id = ?`, [fileMeta.status, fileMeta.id], callback)
}

function updateMediaTime(id, dateTimeOriginal, callback) {
    db.run(`UPDATE media SET datetimeoriginal = ? WHERE id = ?`, [dateTimeOriginal, id], callback)
}

function insertFileMetaToDb(fileMeta, callback) {
    db.run(`INSERT INTO media (filename, dirpath, filesize, hash, status)
        VALUES (?, ?, ?, ?, ?)`,
        fileMeta.fileName, fileMeta.dirPath, fileMeta.fileSize, fileMeta.hash, fileMeta.status, callback)
}

/**
 * Inserts a file metadata row into the database if mathching one doesn't already exist.
 * Signature of the callback is function(recordAdded) {}.
 * 
 * @param {Object} fileMeta File metadata object.
 * @param {Function} callback Callback function with signature function(Boolean) {}.
 */
function insertFileMetaToDbIfNotExists(fileMeta, callback) {
    getMediaIdForMeta(fileMeta, (err, row) => {
        if (err) {
            console.log(err)
            callback(false)
        }
        else {
            if (!row) {
                insertFileMetaToDb(fileMeta, err => {
                    if (err) {
                        console.log(err)
                        callback(false)
                    }
                    else {
                        callback(true)
                    }
                })
            }
            else {
                callback(false)
            }
        }
    })
}

function getAllUsers(callback) {
    db.all(`SELECT id, username, password FROM user`, callback)
}

function getUserForName(username, callback) {
    db.get(`SELECT id, username FROM user WHERE username = ?`, username, callback)
}

function insertUser(username, passwordHash, callback) {
    db.run(`INSERT INTO user (username, password) VALUES (?, ?)`,
            username, passwordHash, callback)
}

function prepareDatabase(callback) {
    // Create / update schema
    let createMediaTable = `CREATE TABLE IF NOT EXISTS "media" (
        "id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
        "userid"    INTEGER DEFAULT 0,
        "filename"	TEXT,
        "dirpath"   TEXT,
        "filesize"	INTEGER,
        "hash"	TEXT,
        "status"	TEXT DEFAULT "unknown",
        "datetimeoriginal"	TEXT
    )`

    let createUserTable = `CREATE TABLE IF NOT EXISTS "user" (
        "id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
        "username"	TEXT NOT NULL UNIQUE,
        "password"	TEXT NOT NULL
    )`

    db.exec(createMediaTable, err => {
        if (err) {
            console.log(err)
        }
        else {
            db.exec(createUserTable, err => {
                if (err) {
                    console.log(err)
                }
                else {
                    callback()
                }
            })
        }
    })

    // TODO Remove this emptying of DB once there is sensible logic in place to detect existing files
    //db.exec("DELETE FROM media")

    console.log("INIT / DB - Database prepared")
}

module.exports = {
    prepareDatabase: prepareDatabase,
    getAllMedia: getAllMedia,
    getAllMediaForProcessing: getAllMediaForProcessing,
    insertFileMetaToDb: insertFileMetaToDb,
    updateMediaStatus: updateMediaStatus,
    updateMediaTime: updateMediaTime,
    getMediaForId: getMediaForId,
    getMediaIdForMeta: getMediaIdForMeta,
    insertFileMetaToDbIfNotExists: insertFileMetaToDbIfNotExists,
    getAllUsers: getAllUsers,
    getUserForName: getUserForName,
    insertUser: insertUser
}
