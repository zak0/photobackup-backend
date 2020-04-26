const express = require("express")
const basicAuth = require("express-basic-auth")

const files = require("./core/files")
const config = require("./config")
const processor = require("./core/processor")
const db = require("./db/db")
const authorizer = require("./routes/authorizer")

const app = express()

// Setup auth
app.use(basicAuth({ authorizer: authorizer.checkCredentials }))

// Start the initialization phase.
// First prepares the database, then scans the filesystem for changes and
// triggers appropriate actions if changes are detected.
db.prepareDatabase( _ => {
    authorizer.init()
    files.scanLibrary( _ => {
        processor.processAllUnprocessed()
    })
})

// By default, parse request bodies as JSON
// This declaration is here instead of under ./routes, because this is universal
app.use(express.json())

const port = config.serverPort

// Add routes
app.use(require("./routes/media"))
app.use(authorizer.routing)

app.listen(port, () => {
    console.log(`INIT / Server - Server listening on port ${port}...`)
})
