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
    let files = fs.readdirSync(config.mediaDir)
    let filesCount = files.length
    var newFileCount = 0
    var handledFilesCount = 0

    console.log("INIT / Files - " + filesCount + " files detected. Starting scan...")
    let scanStartTime = Date.now()

    files.forEach(fileName => {
        if (!isImage(fileName)) {
            handledFilesCount++
            console.log(fileName + " is not an image")
            return // this it the "continue" of forEach()
        }

        let filePath = config.mediaDir + "/" + fileName
        let hash = md5.sync(filePath)

        fs.stat(filePath, (err, stat) => {
            let file = {
                "id": -1,
                "fileName": fileName,
                "fileSize": stat.size,
                "hash": hash,
                "status": constants.MediaState.STATE_PROCESSING
            }
            db.insertFileMetaToDbIfNotExists(file, didNotExist => {
                if (didNotExist) {
                    newFileCount++
                }

                if (++handledFilesCount >= filesCount) {
                    let scanDuration = Date.now() - scanStartTime
                    console.log("INIT / Files - Scan completed in " + scanDuration + " ms. " + newFileCount + " new files detected.")

                    callback()
                }
            })
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
function getCreateTime(filePath, callback) {
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
    getCreateTime: getCreateTime
}
