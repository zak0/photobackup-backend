const express = require("express")
const fileUpload = require("express-fileupload")
const fs = require("fs")

const app = express()

const mediaDir = "devmedia"
//const uploadsDir = "uploads" // will be under mediaDir

const STATE_UPLOAD_PENDING = "upload_pending"
const STATE_PROCESSING = "processing"
const STATE_READY = "ready"

var mediaFiles = readMediaFilesSync()

// By default, parse request bodies as JSON
app.use(express.json())

// For file uploads, parse requests with express-fileupload
app.use("/media/:id/file", fileUpload())

const port = 3000

app.get("/test", (req, res) => {
    res.json({
        "key": "value",
        "anotherKey": 12.34
    })
})

app.post("/", (req, res, next) => {
    let name = req.body.name
    let message = `Hello ${name}!`
    res.json({
        "message": message
    })
})

app.get("/media", (req, res) => {
    res.json({
        "files": mediaFiles
    })
})

app.get("/media/:id", (req, res) => {
    let id = req.params.id

    if (id < mediaFiles.length) {
        res.json(mediaFiles[id])
    }
    else {
        res.sendStatus(404)
    }
})

app.get("/media/:id/file", (req, res) => {
    let id = req.params.id
    let options = {
        root: mediaDir
    }

    if (id < mediaFiles.length) {
        res.sendFile(mediaFiles[id].fileName, options)
    }
    else {
        res.sendStatus(404)
    }
})

app.post("/media", (req, res) => {
    let fileName = req.body.fileName
    let fileSize = req.body.fileSize
    let hash = req.body.hash

    // TODO Validate request

    // Insert new "upload_pending" entry with filename, size and hash
    let newEntry = {
        "id": mediaFiles.length,
        "fileName": fileName,
        "fileSize": fileSize,
        "hash": hash,
        "status": STATE_UPLOAD_PENDING
    }

    mediaFiles.push(newEntry)

    // Respond with the new entry (client uses id of this for actual file upload later)
    res.status(201).json(newEntry)
})

app.post("/media/:id/file", (req, res) => {
    let media = mediaFiles[req.params.id]

    // Check that media entry is in "upload_pending" state
    if (media.status == STATE_UPLOAD_PENDING) {
        // Get file from the form-data (using express-fileupload)
        let newFile = req.files.newFile
        let filePath = mediaDir + "/" + media.fileName
        newFile.mv(filePath, function(err) {
            // Error handler for mv()
            if (err) {
                return res.status(500).send(err)
            }
            else {
                // TODO Validate file size and hash after upload
                // TODO Trigger thumbnail and exif population

                media.status = STATE_PROCESSING
                res.status(201).json(media)
            }
        })   
    }
    else {
        // Media was not in correct state
        res.sendStatus(409)
    }
})

app.delete("/media/:id", (req, res) => {
    // TODO Check if media with given ID exists
    // TODO Change its state to "in_trash"
    // TODO Store the media into list of media in trash can with date
})

app.listen(port, () => {
    console.log(`Server listening on port ${port}...`)
})

function readMediaFilesSync() {
    let files = []
    fs.readdirSync(mediaDir).forEach(file => {
        files.push({
            "id": files.length,
            "fileName": file,
            "fileSize": -1,
            "hash": null,
            "status": STATE_READY
        })
    })
    return files
}
