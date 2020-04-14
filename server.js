const express = require("express")
const fileUpload = require("express-fileupload")
const fs = require("fs")
const sqlite3 = require("sqlite3")

const app = express()

const mediaDir = "devmedia"
const metaDir = "devmetadata"
const dbFile = metaDir + "/db.db"
const db = new sqlite3.Database(dbFile, sqlite3.OPEN_READWRITE | sqlite3.OPEN_CREATE, err => {
    if (err) {
        console.log(err)
    }
})

const STATE_UPLOAD_PENDING = "upload_pending"
const STATE_PROCESSING = "processing"
const STATE_READY = "ready"

prepareDatabase()

// By default, parse request bodies as JSON
app.use(express.json())

// For file uploads, parse requests with express-fileupload
app.use("/media/:id/file", fileUpload())

const port = 3000

app.post("/", (req, res, next) => {
    let name = req.body.name
    let message = `Hello ${name}!`
    res.json({
        "message": message
    })
})

app.get("/media", (req, res) => {
    db.all("SELECT id, filename, filesize, hash, status FROM media", (err, rows) => {

        if (err) {
            console.log(err)
        }
        else {
            let files = []
            rows.forEach(row => {
                files.push({
                    "id": row.id,
                    "fileName": row.filename,
                    "fileSize": row.filesize,
                    "hash": row.hash,
                    "status": row.status
                })
            })

            res.json({
                "files": files
            })
        }
    })
})

app.get("/media/:id", (req, res) => {
    let id = req.params.id

    getMediaForId(id, (err, row) => {
        if (err) {
            console.log(err)
            res.sendStatus(500)
        }
        else if (row) {
            res.json({
                "id": row.id,
                "fileName": row.filename,
                "fileSize": row.filesize,
                "hash": row.hash,
                "status": row.status
            })
        }
        else {
            res.sendStatus(404)
        }
    })
})

app.get("/media/:id/file", (req, res) => {
    let id = req.params.id
    let options = {
        root: mediaDir
    }

    getMediaForId(id, (err, row) => {
        if (err) {
            console.log(err)
            res.sendStatus(500)
        }
        else if (row) {
            res.sendFile(row.filename, options)
        }
        else {
            res.sendStatus(404)
        }
    })

})

app.post("/media", (req, res) => {
    let fileName = req.body.fileName
    let fileSize = req.body.fileSize
    let hash = req.body.hash

    // TODO Validate request

    // Insert new "upload_pending" entry with filename, size and hash
    let newEntry = {
        "id": -1,
        "fileName": fileName,
        "fileSize": fileSize,
        "hash": hash,
        "status": STATE_UPLOAD_PENDING
    }

    // First insert, then in callback get the id
    insertFileMetaToDb(newEntry, insertErr => {
        if (insertErr) {
            console.log(insertErr)
            res.sendStatus(500)
        }
        else {
            getMediaIdForMeta(newEntry, (selectErr, row) => {
                if (selectErr) {
                    console.log(selectErr)
                    res.sendStatus(500)
                }
                else {
                    console.log("yolo")
                    // Respond with the new entry
                    // (client uses id of this for actual file upload later)
                    newEntry.id = row.id
                    res.status(201).json(newEntry)
                }
            })
        }
    })
})

app.post("/media/:id/file", (req, res) => {
    getMediaForId(req.params.id, (err, row) => {
        if (err) {
            console.log(err)
            res.sendStatus(500)
        }
        else {
            let media = {
                "id": row.id,
                "fileName": row.filename,
                "fileSize": row.filesize,
                "hash": row.hash,
                "status": row.status
            }

            // Check that media entry is in "upload_pending" state
            if (media.status == STATE_UPLOAD_PENDING) {
                // Get file from the form-data (using express-fileupload)
                let newFile = req.files.newFile
                let filePath = mediaDir + "/" + media.fileName
                newFile.mv(filePath, err => {
                    // Error handler for mv()
                    if (err) {
                        return res.status(500).send(err)
                    }
                    else {
                        // TODO Validate file size and hash after upload
                        // TODO Trigger thumbnail and exif population
                        media.status = STATE_PROCESSING

                        updateMediaStatus(media, err => {
                            if (err) {
                                console.log(err)
                                res.sendStatus(500)
                            }
                            else {
                                res.status(201).json(media)
                            }
                        })
                    }
                })   
            }
            else {
                // Media was not in correct state
                res.sendStatus(409)
            }
        }
    })
})

app.delete("/media/:id", (req, res) => {
    // TODO Check if media with given ID exists
    // TODO Change its state to "in_trash"
    // TODO Store the media into list of media in trash can with date
})

app.listen(port, () => {
    console.log(`INIT - Server listening on port ${port}...`)
})

function readMediaFilesSync() {
    fs.readdirSync(mediaDir).forEach(fileName => {
        fs.stat(mediaDir + "/" + fileName, (err, stat) => {
            let file = {
                "id": -1,
                "fileName": fileName,
                "fileSize": stat.size,
                "hash": null,
                "status": STATE_READY
            }
            insertFileMetaToDb(file, err => {
                if (err) {
                    console.log(err)
                }
            })
        })
    })
}

function scanLibrary() {
    readMediaFilesSync()
    console.log("INIT - Library scan complete")
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

function prepareDatabase() {
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
            scanLibrary()
        }
    })

    // TODO Remove this emptying of DB once there is sensible logic in place to detect existing files
    db.exec("DELETE FROM media")

    console.log("INIT - Database prepared")
}
