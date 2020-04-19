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

function getAllMedia(callback) {
    db.all("SELECT id, filename, thumbnailname, filesize, hash, status FROM media", callback)
}

function getAllMediaForProcessing(callback) {
    db.all("SELECT id, filename FROM media WHERE status = ?", constants.MediaState.STATE_PROCESSING, callback)
}

function getMediaForId(id, callback) {
    db.get("SELECT id, filename, thumbnailname, filesize, hash, status FROM media WHERE id = ?", id, callback)
}

function getMediaIdForMeta(fileMeta, callback) {
    db.get(`SELECT id FROM media WHERE filename = ? AND filesize = ? AND hash = ?`,
    [fileMeta.fileName, fileMeta.fileSize, fileMeta.hash], callback)
}

function updateMediaStatus(fileMeta, callback) {
    db.run(`UPDATE media SET status = ? WHERE id = ?`, [fileMeta.status, fileMeta.id], callback)
}

function updateThumbnail(id, thumbnailName, callback) {
    db.run(`UPDATE media SET thumbnailname = ? WHERE id = ?`, [thumbnailName, id], callback)
}

function insertFileMetaToDb(fileMeta, callback) {
    db.run(`INSERT INTO media (filename, filesize, hash, status)
        VALUES (?, ?, ?, ?)`,
        fileMeta.fileName, fileMeta.fileSize, fileMeta.hash, fileMeta.status, callback)
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

function prepareDatabase(callback) {
    // Create / update schema
    let createMediaTable = `CREATE TABLE IF NOT EXISTS "media" (
        "id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
        "filename"	TEXT,
        "thumbnailname" TEXT,
        "filesize"	INTEGER,
        "hash"	TEXT,
        "status"	TEXT DEFAULT "unknown",
        "timestamp"	INTEGER
    )`

    db.exec(createMediaTable, err => {
        if (err) {
            console.log(err)
        }
        else {
            callback()
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
    updateThumbnail: updateThumbnail,
    getMediaForId: getMediaForId,
    getMediaIdForMeta: getMediaIdForMeta,
    insertFileMetaToDbIfNotExists: insertFileMetaToDbIfNotExists
}
