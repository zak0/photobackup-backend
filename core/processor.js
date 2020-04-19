/*
Processor for handling file processing such as thumbnail generation and EXIF data etraction.
*/

const thumb = require("node-thumbnail").thumb
const config = require("../config")
const db = require("../db/db")

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
            let rowsCount = rows.length
            let processedCount = 0

            console.log("CORE / Processing - Starting to process " + rowsCount + " files.")

            rows.forEach(row => {
                
                // TODO Generate thumbnails
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
                        console.log("CORE / Processing - Processing completed.")
                    }
                })
            })
        }
    })
}

module.exports = {
    processIfNeeded: processIfNeeded
}
