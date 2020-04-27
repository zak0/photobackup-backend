/*
 Wrapper for file system access.
*/

const fs = require("fs")
const path = require("path")
const md5 = require("md5-file")

const constants = require("./constants")
const db = require("../db/db")
const config = require("../config")

/**
 * Scans the library files directory for image files. Image files are detected by their extension
 * (see constants.fileExtensions).
 * 
 * When new files are detected, they are processed and added into the database.
 * 
 * TODO - When previously existing files are deleted, they will be removed from the database.
 * 
 * @param {Function} callback Callback that gets called when scan completes. Has signature function() {}.
 */
function scanLibrary(callback) {
    console.log("INIT / Files - Starting scan...")
    let scanStartTime = Date.now()

    let scanProgressHandler = {
        detectedFiles: 0,
        handledFiles: 0,
        onFileDetected: function () {
            this.detectedFiles++
        },
        onFileHandled: function () {
            this.handledFiles++
            if (this.handledFiles >= this.detectedFiles) {
                // This means that the scan is done
                let duration = Date.now() - scanStartTime
                console.log(`INIT / Files - Scan completed in ${duration} ms.`)
                callback()
            }
        }
    }

    scanDirAndHandleFiles(config.mediaDir, scanProgressHandler)
}

function scanDirAndHandleFiles(dirPath, progressHandler) {
    let files = fs.readdirSync(dirPath, { withFileTypes: true})

    files.forEach(file => {
        let newPath = `${dirPath}/${file.name}`

        if (file.isDirectory()) {
            scanDirAndHandleFiles(newPath, progressHandler)
        }
        
        if (file.isFile() && isImage(file.name)) {
            progressHandler.onFileDetected()
            handleImage(file.name, dirPath, progressHandler)
        }
    })
}

function handleImage(fileName, dirPath, progressHandler) {
    let filePath = `${dirPath}/${fileName}`
    let hash = md5.sync(filePath)

    // Directory path stored into the DB must be relative to the root media dir
    // ...so let's trim it a bit.
    let trimmedPath = `.${dirPath.slice(config.mediaDir.length)}`

    fs.stat(filePath, (err, stat) => {
        let file = {
            "id": -1,
            "fileName": fileName,
            "dirPath": trimmedPath,
            "fileSize": stat.size,
            "hash": hash,
            "status": constants.MediaState.STATE_PROCESSING
        }

        db.insertFileMetaToDbIfNotExists(file, didNotExist => {
            // TODO Do something with `didNotExist`?
            progressHandler.onFileHandled()
        })
    })
}

/**
 * Attempts to fetch the time when a file was modified through filesystem
 * stats.
 * 
 * NOTE! This time is UTC. Timezone information is lost.
 * TODO Consider the need to convert this to local time of the server
 * 
 * @param {String} filePath Absolute or relative path to the file to resolve.
 */
function getModifiedTime(filePath, callback) {
    fs.stat(filePath, (err, stat) => {
        if (err) {
            console.log(err)
        }
        else {
            callback(Math.round(stat.mtimeMs))
        }
    })
}

/**
 * Checks if the filename is of one of the supported file types. Check is made based
 * on file extension.
 * 
 * @param {String} fileName Name of the file to check, e.g. "photo_2020_04_18.jpg".
 */
function isImage(fileName) {
    return constants.fileExtensions.indexOf(path.extname(fileName).toLowerCase()) > -1
}

module.exports = {
    scanLibrary: scanLibrary,
    getModifiedTime: getModifiedTime
}
