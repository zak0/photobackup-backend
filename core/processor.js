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
function processIfNeeded(callback) {
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
        let filePath = config.mediaDir + "/" + row.filename

        thumb({
            source: filePath,
            destination: thumbsDir,
            width: 256,
            quiet: true,
            digest: false,
            skip: true,
            basename: "" + row.id
        }, (files, err, stdout, stderr) => {
            if (err) {
                console.log(err)
            }

            if (files.length > 0) {
                let thumbnailFileName = files[0].dstPath.replace(thumbsDir + "/", "")
                let id = thumbnailFileName.split("_")[0]

                db.updateThumbnail(id, thumbnailFileName, err => {
                    if (err) {
                        console.log(err)
                    }
                })
            }

            // TODO Read EXIF and write to exif DB table
            // TODO Write time taken as metadata timestamp
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

    let onRowHandled = function (id, dateTimeOriginal) {
        db.updateMediaTime(id, dateTimeOriginal)

        if (++processedCount >= rowsCount) {
            let duration = Date.now() - startTime
            console.log("CORE / Processing / EXIF - EXIF extraction completed in " + duration + " ms.")
        }
    }

    rows.forEach(row => {
        let iterationScope = function (scopedRow) {
            let filePath = config.mediaDir + "/" + row.filename

            new ExifImage({ image: filePath }, (err, exif) => {
                let fallbackToFileTime = function() {
                    files.getCreateTime(filePath, fileTime => {
                        let exifTime = util.millisToExifTimeStamp(fileTime)
                        onRowHandled(scopedRow.id, exifTime)
                    })
                }

                if (err) {
                    console.log(`CORE / Processing / EXIF - Unable to read EXIF for '${row.filename}'. Falling back to file creation time.`)
                    fallbackToFileTime()
                }
                else {
                    try {
                        onRowHandled(scopedRow.id, exif.exif.DateTimeOriginal)
                    }
                    catch (caughtError) {
                        console.log(caughtError)
                        fallbackToFileTime()
                    }
                }
            })
        }
        iterationScope(row)
    })
}

module.exports = {
    processIfNeeded: processIfNeeded
}
