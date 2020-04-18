const sqlite3 = require("sqlite3")
const metaDir = require("../config").metaDir
const dbFile = metaDir + "/db.db"

const db = new sqlite3.Database(dbFile, sqlite3.OPEN_READWRITE | sqlite3.OPEN_CREATE, err => {
    if (err) {
        console.log(err)
    }
})

function getAllMedia(callback) {
    db.all("SELECT id, filename, filesize, hash, status FROM media", callback)
}

function getMediaForId(id, callback) {
    db.get("SELECT id, filename, filesize, hash, status FROM media WHERE id = ?", id, callback)
}

function getMediaIdForMeta(fileMeta, callback) {
    db.get(`SELECT id FROM media WHERE filename = ? AND filesize = ? AND hash = ?`,
    [fileMeta.fileName, fileMeta.fileSize, fileMeta.hash], callback)
}

function updateMediaStatus(fileMeta, callback) {
    db.run(`UPDATE media SET status = ? WHERE id = ?`, [fileMeta.status, fileMeta.id], callback)
}

function insertFileMetaToDb(fileMeta, callback) {
    db.run(`INSERT INTO media (filename, filesize, hash, status)
        VALUES (?, ?, ?, ?)`,
        fileMeta.fileName, fileMeta.fileSize, fileMeta.hash, fileMeta.status, callback)
}

function prepareDatabase(callback) {
    // Create / update schema
    let createMediaTable = `CREATE TABLE IF NOT EXISTS "media" (
        "id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
        "filename"	TEXT,
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
    db.exec("DELETE FROM media")

    console.log("INIT - Database prepared")
}

module.exports = {
    prepareDatabase: prepareDatabase,
    getAllMedia: getAllMedia,
    insertFileMetaToDb: insertFileMetaToDb,
    updateMediaStatus: updateMediaStatus,
    getMediaForId: getMediaForId,
    getMediaIdForMeta: getMediaIdForMeta
}