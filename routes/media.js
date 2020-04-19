const app = module.exports = require("express")()
const fileUpload = require("express-fileupload")

const db = require("../db/db")
const config = require("../config")
const constants = require("../core/constants")

// For file uploads, parse requests with express-fileupload
app.use("/media/:id/file", fileUpload())

app.get("/media", (req, res) => {
    db.getAllMedia((err, rows) => {

        if (err) {
            console.log(err)
        }
        else {
            let files = []
            rows.forEach(row => {
                files.push(rowToFileMeta(row))
            })

            res.json({
                "files": files
            })
        }
    })
})

app.get("/media/:id", (req, res) => {
    let id = req.params.id

    db.getMediaForId(id, (err, row) => {
        if (err) {
            console.log(err)
            res.sendStatus(500)
        }
        else if (row) {
            res.json(rowToFileMeta(row))
        }
        else {
            res.sendStatus(404)
        }
    })
})

app.get("/media/:id/file", (req, res) => {
    let id = req.params.id
    let options = {
        root: config.mediaDir
    }

    db.getMediaForId(id, (err, row) => {
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
        "status": constants.MediaState.STATE_UPLOAD_PENDING
    }

    // First insert, then in callback get the id
    db.insertFileMetaToDb(newEntry, insertErr => {
        if (insertErr) {
            console.log(insertErr)
            res.sendStatus(500)
        }
        else {
            db.getMediaIdForMeta(newEntry, (selectErr, row) => {
                if (selectErr) {
                    console.log(selectErr)
                    res.sendStatus(500)
                }
                else {
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
    db.getMediaForId(req.params.id, (err, row) => {
        if (err) {
            console.log(err)
            res.sendStatus(500)
        }
        else {
            let media = rowToFileMeta(row)

            // Check that media entry is in "upload_pending" state
            if (media.status == constants.MediaState.STATE_UPLOAD_PENDING) {
                // Get file from the form-data (using express-fileupload)
                let newFile = req.files.newFile
                let filePath = config.mediaDir + "/" + media.fileName
                newFile.mv(filePath, err => {
                    // Error handler for mv()
                    if (err) {
                        return res.status(500).send(err)
                    }
                    else {
                        // TODO Validate file size and hash after upload
                        // TODO Trigger thumbnail and exif population
                        media.status = constants.MediaState.STATE_PROCESSING

                        db.updateMediaStatus(media, err => {
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

/**
 * Constructs a file metadata object from a row retrieved from the database.
 * 
 * @param {Object} row Row as read from the database
 */
function rowToFileMeta(row) {
    return {
        "id": row.id,
        "fileName": row.filename,
        "thumbnailName": row.thumbnailname,
        "fileSize": row.filesize,
        "hash": row.hash,
        "status": row.status
    }
}