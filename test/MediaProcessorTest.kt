package jaaska.jaakko

import jaaska.jaakko.photosapp.server.extension.regexSubstring
import java.time.format.DateTimeFormatter
import kotlin.test.*

// TODO Just a dummy for now. Write proper tests...
class FileNameDateParser {

    @Test
    fun testFilenameDateParser() {

        data class TestParams(
            val datePattern: String,
            val dateRegex: String,
            val fileName: String,
            val expectedTime: String?
        )

        listOf(
            TestParams("yyyyMMdd_HHmmss", "(?=.{4})([0-9]{8})_([0-9]{6})", "IMG_20200815_204402.jpg", "2020:08:15 20:44:02"),
            TestParams("yyyyMMdd_HHmmss", "(?=.{4})([0-9]{8})_([0-9]{6})", "PXL_20200815_204402456.jpg", "2020:08:15 20:44:02")
        ).forEach {
            assertEquals(it.expectedTime, getTimeOrNull(it.datePattern, it.dateRegex, it.fileName))
        }
    }

    private fun getTimeOrNull(datePattern: String, regex: String, fileName: String): String? {
        val rawName = fileName.substringBeforeLast(".")

        val datePart = rawName.regexSubstring(regex)
        val inputFormatter = DateTimeFormatter.ofPattern(datePattern)

        return try {
            val inputTemporal = inputFormatter.parse(datePart)

            val outputFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
            outputFormatter.format(inputTemporal)
        } catch (t: Throwable) {
            null
        }
    }

}