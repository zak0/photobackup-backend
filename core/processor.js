/*
Processor for handling file processing such as thumbnail generation and EXIF data etraction.
*/

const thumb = require("node-thumbnail").thumb

const config = require("../config")
const files = require("./files")
const db = require("../db/db")
const util = require("./util")

/** This is a class from node-exif, hence the weird require. Usef for exit data extraction. */
const ExifImage = require("exif").ExifImage

const thumbsDir = config.metaDir + "/thumbs"

/**
 * Queries the database for photos waiting to be processed and processes them.
 * 
 * @param {Function} callback Callback for when processing is done. Has signature function(err) {}.
 */
function processAllUnprocessed(callback) {
    // Fetch all meta data requiring processing
    db.getAllMediaForProcessing((err, rows) => {
        if (err) {
            console.log(err)
        }
        else {
        
            console.log("CORE / Processing - Starting to process " + rows.length + " files.")

            generateThumbnails(rows)
            extractExif(rows)
        }
    })
}

function generateThumbnails(rows) {
    let rowsCount = rows.length
    let processedCount = 0
    let startTime = Date.now()
    rows.forEach(row => {       
        generateThumbnail(row, function () {
            processedCount++
            if (processedCount >= rowsCount) {
                let duration = Date.now() - startTime
                console.log("CORE / Processing / Thumbnails - Thumbnail generation completed in " + duration + " ms.")
            }
        })
    })
}

function extractExif(rows) {
    let rowsCount = rows.length
    let processedCount = 0
    let startTime = Date.now()

    rows.forEach(row => {
        extractCreationTimeFromExif(row, function () {
            if (++processedCount >= rowsCount) {
                let duration = Date.now() - startTime
                console.log("CORE / Processing / EXIF - EXIF extraction completed in " + duration + " ms.")
            }
        })
    })
}

/**
 * @param {Object} fileMeta Object with at least `id` and `filename` fielda
 */
function processFile(fileMeta) {
    generateThumbnail(fileMeta)

    extractCreationTimeFromExif(fileMeta, _ => {
        console.log(`exif read for ${fileMeta}`)
    })
}

/**
 * @param {Object} fileMeta Object with at least `id` and `filename` fields
 * @param {Function} callback Function without params
 */
function generateThumbnail(fileMeta, callback) {
    let filePath = config.mediaDir + "/" + fileMeta.filename

    thumb({
        source: filePath,
        destination: thumbsDir,
        width: 256,
        quiet: true,
        digest: false,
        skip: true,
        basename: `${fileMeta.id}`
    }, (files, err, stdout, stderr) => {
        if (err) {
            console.log(err)
        }

        // Callback doesn't seem to be thread safe in case of successes.
        // So following part is pointless for now and just cannot work when thumb() call is ongoing
        // for multiple files simultaneously.

        // else {
        //     let thumbnailFileName = `${fileMeta.id}_thumb.jpg`
        //     console.log(`update thumbnail id: ${fileMeta.id}, thumbnailFileName: ${thumbnailFileName}`)
        //     db.updateThumbnail(fileMeta.id, thumbnailFileName, err => {
        //         if (err) {
        //             console.log(err)
        //         }
        //     })
        // }

        if (callback) {
            callback()
        }
    })
}

/**
 * @param {Object} fileMeta Object with at least `id` and `filename` fields
 * @param {Function} callback Function without params
 */
function extractCreationTimeFromExif(fileMeta, callback) {
    let onRowHandled = function (id, dateTimeOriginal) {
        db.updateMediaTime(id, dateTimeOriginal)
        callback()
    }

    let filePath = config.mediaDir + "/" + fileMeta.filename

    new ExifImage({ image: filePath }, (err, exif) => {
        let fallbackToFileTime = function() {
            files.getCreateTime(filePath, fileTime => {
                let exifTime = util.millisToExifTimeStamp(fileTime)
                onRowHandled(fileMeta.id, exifTime)
            })
        }

        if (err) {
            console.log(`CORE / Processing / EXIF - Unable to read EXIF for '${fileMeta.filename}'. Falling back to file creation time.`)
            fallbackToFileTime()
        }
        else {
            try {
                onRowHandled(fileMeta.id, exif.exif.DateTimeOriginal)
            }
            catch (caughtError) {
                console.log(caughtError)
                fallbackToFileTime()
            }
        }
    })
}

module.exports = {
    processAllUnprocessed: processAllUnprocessed,
    processFile: processFile
}
