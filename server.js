const express = require("express")
const files = require("./core/files")
const config = require("./config")

const app = express()
const db = require("./db/db")

// Start the initialization phase.
// First prepares the database, then scans the filesystem for changes and
// triggers appropriate actions if changes are detected.
db.prepareDatabase( _ => {
    files.scanLibrary()
})

// By default, parse request bodies as JSON
// This declaration is here instead of under ./routes, because this is universal
app.use(express.json())

const port = config.serverPort

app.post("/", (req, res, next) => {
    let name = req.body.name
    let message = `Hello ${name}!`
    res.json({
        "message": message
    })
})

// Add routes
app.use(require("./routes/media"))

app.listen(port, () => {
    console.log(`INIT / Server - Server listening on port ${port}...`)
})
