const fs = require("fs")
const constants = require("./constants")
const db = require("../db/db")
const config = require("../config")

function readMediaFilesSync() {
    fs.readdirSync(config.mediaDir).forEach(fileName => {
        fs.stat(config.mediaDir + "/" + fileName, (err, stat) => {
            let file = {
                "id": -1,
                "fileName": fileName,
                "fileSize": stat.size,
                "hash": null,
                "status": constants.MediaState.STATE_READY
            }
            db.insertFileMetaToDb(file, err => {
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

module.exports = {
    scanLibrary: scanLibrary
}