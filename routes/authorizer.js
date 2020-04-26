const app = require("express")()

const basicAuth = require("express-basic-auth")
const crypto = require("crypto")

const config = require("../config")
const db = require("../db/db")

// Map from username to user data
var users = {}

function init() {
    db.getAllUsers((err, rows) => {
        if (err) {
            console.log(err)
        }
        else {
            rows.forEach(row => {
                users[row.username] = {
                    id: row.id,
                    password: row.password
                }
            })
            console.log("INIT / Auth - Init successful")
        }
    })
}

function checkCredentials(username, pw) {
    if (isAdmin({ user: username, password: pw })) {
        console.log(`SECURITY / Admin auth detected`)
        return true
    }
    else {
        if (users[username]) {
            let pwHash = md5String(pw)
            return basicAuth.safeCompare(pwHash, users[username].password)
        }
    }

    return false
}

function isAdmin(auth) {
    const userMatches = basicAuth.safeCompare(auth.user, config.adminUsername)
    const passwordMatches = basicAuth.safeCompare(auth.password, config.adminPassword)
    return userMatches & passwordMatches
}

function md5String(input) {
    let md5sum = crypto.createHash("md5")
    md5sum.update(input)
    return md5sum.digest("hex")
}

function getUserId(auth) {
    return users[auth.user].id
}

/**
 * Route / endpoint for creating a new user. Only admin should be able to do this.
 */
app.post("/user", (req, res) => {
    if (isAdmin(req.auth)) {
        db.insertUser(req.body.username, req.body.password, err => {
            if (err) {
                console.log(err)
                res.sendStatus(400)
            }
            else {
                // Respond with { id: 123, username: name_of_user }
                db.getUserForName(req.body.username, (err, row) => {
                    if (err) {
                        console.log(err)
                        res.sendStatus(400)
                    }
                    else {
                        users[req.body.username] = {
                            id: row.id,
                            password: req.body.password
                        }
                        res.status(201).json(row)
                    }
                })
            }
        })
        
    }
    else {
        res.sendStatus(401)
    }
})

module.exports = {
    init: init,
    checkCredentials: checkCredentials,
    routing: app,
    getUserId: getUserId
}
