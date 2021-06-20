# What is this?

A personal project of mine of an HTTP server for backing up photos from mobile devices, and for browsing the uploaded photos.

While I do use this to back up photos and videos from my Android phone, this project is very much in WIP state and should **NOT** be trusted by anyone for their only means of back up of precious photos.

GitHub project for the Android client is available here -> https://github.com/zak0/photobackup-android

# Features

- Non-destructive. Does not alter the files in any way. All metadata stays intact.
- Compiles into Java bytecode, so it runs on almost anything.
- Generates thumbnails.

# Technical details

- HTTP'ing is built with Ktor.
