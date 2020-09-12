package jaaska.jaakko.photosapp.server.processor

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifImageDescriptor
import com.drew.metadata.exif.ExifSubIFDDirectory
import jaaska.jaakko.photosapp.server.extension.absoluteFilePath
import jaaska.jaakko.photosapp.server.model.MediaMeta
import java.io.File

class ExifProcessor {

    fun getDateTimeOriginal(mediaMeta: MediaMeta): String? {
        val mediaFile = File(mediaMeta.absoluteFilePath)
        val exif = ImageMetadataReader.readMetadata(mediaFile).getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)

        return if (exif?.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL) == true) {
            exif.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
        } else {
            null
        }
    }

    fun getOrientation(mediaMeta: MediaMeta): Int? {
        val mediaFile = File(mediaMeta.absoluteFilePath)
        return getOrientation(mediaFile)
    }

    fun getOrientation(file: File): Int? {
        val exif = ImageMetadataReader.readMetadata(file).getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)

        return if (exif?.parent?.containsTag(ExifSubIFDDirectory.TAG_ORIENTATION) == true) {
            exif.parent?.getInt(ExifSubIFDDirectory.TAG_ORIENTATION)
        } else {
            null
        }
    }

}