package org.phenoapps.intercross.util

//import org.apache.poi.ss.usermodel.WorkbookFactory

import android.content.ContentUris
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.IntercrossDatabase
import org.phenoapps.intercross.data.models.*
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


class FileUtil(private val ctx: Context) {

    /**
     * Parent's input file header fields. These are queried from the strings XML.
     */
    private val parentsCodeIdHeader: String by lazy { ctx.getString(R.string.parents_header_code_id) }

    private val parentsNameHeader: String by lazy { ctx.getString(R.string.parents_header_readable_name) }

    private val parentsSexHeader: String by lazy { ctx.getString(R.string.parents_header_sex) }

    private val parentHeaders = setOf(parentsCodeIdHeader, parentsNameHeader, parentsSexHeader)

    /**
     * Wishlist input file header fields. These are queried from the strings XML.
     */
    private val wishlistFemaleIdHeader: String by lazy { ctx.getString(R.string.wishlist_header_female_id) }

    private val wishlistMaleIdHeader: String by lazy { ctx.getString(R.string.wishlist_header_male_id) }

    private val wishlistMaleNameHeader: String by lazy { ctx.getString(R.string.wishlist_header_male_name) }

    private val wishlistFemaleNameHeader: String by lazy { ctx.getString(R.string.wishlist_header_female_name) }

    private val wishlistTypeHeader: String by lazy { ctx.getString(R.string.wishlist_header_type) }

    private val wishlistMinHeader: String by lazy { ctx.getString(R.string.wishlist_header_min) }

    private val wishlistMaxHeader: String by lazy { ctx.getString(R.string.wishlist_header_max) }

    private val wishlistHeaders = setOf(
            wishlistFemaleIdHeader,
            wishlistMaleIdHeader,
            wishlistMaleNameHeader,
            wishlistFemaleIdHeader,
            wishlistFemaleNameHeader,
            wishlistTypeHeader,
            wishlistMinHeader,
            wishlistMaxHeader)

    /**
     * Cross table export file header fields.
     */

    private val crossIdHeader: String by lazy { ctx.getString(R.string.crosses_export_id_header) }

    private val crossMomHeader: String by lazy { ctx.getString(R.string.crosses_export_mom_header) }

    private val crossDadHeader: String by lazy { ctx.getString(R.string.crosses_export_dad_header) }

    private val crossTimestampHeader: String by lazy { ctx.getString(R.string.crosses_export_date_header) }

    private val crossPersonHeader: String by lazy { ctx.getString(R.string.crosses_export_person_header) }

    private val crossExperimentHeader: String by lazy { ctx.getString(R.string.crosses_export_experiment_header) }

    private val crossTypeHeader: String by lazy { ctx.getString(R.string.crosses_export_type_header) }

    private val crossHeaders = setOf(crossIdHeader, crossMomHeader, crossDadHeader,
            crossTimestampHeader, crossPersonHeader, crossExperimentHeader,
            crossTypeHeader)

    private val eventModelHeaderString by lazy {
        crossHeaders.joinToString(",")
    }

    private val mPref by lazy {
        PreferenceManager.getDefaultSharedPreferences(ctx)
    }

    private val mKeyUtil by lazy {
        KeyUtil(ctx)
    }

    /***
     * Main parse driver. Either a parents or wishlist file can be loaded.
     * Parents files populate a table of barcodeIds with readable names.
     *  --Parents table is specifically used to print barcodes that don't exist in the crosses table (CIP)
     *  --But this table is also updated during normal cross entry. (Parents are inserted from data entry)
     * The wishlist table contains rows of samples to be crossed with minimum and maximum requirements
     *
     * This driver function parses a text file and determines the input file type.
     * If a wishType column is detected, it is assumed to be a wishlist file.
     *
     * TODO Localizations must require that the parents input file header should not contain the wishlist type header.
     */
    fun parseInputFile(uri: Uri): Array<List<BaseTable>> {

        val metadata = ArrayList<Meta>()

        val metaValues = ArrayList<MetadataValues>()

        val wishlist = ArrayList<Wishlist>()

        val parents = ArrayList<Parent>()

        val crosses = ArrayList<Event>()

        val lines = parseTextFile(uri)

        if (lines.isNotEmpty()) {

            val headers = lines[0].split(",").map { it -> it.replace(" ", "") }

            //ensure the headers size > 0
            if (headers.isNotEmpty()) {

                var diffHeaders: Set<String>

                if (headers.find { it == wishlistTypeHeader }.isNullOrBlank()) {

                    if (headers.find { it == crossTypeHeader }.isNullOrBlank()) {

                        diffHeaders = parentHeaders-headers.toSet()

                        if (diffHeaders.isEmpty()) {

                            loadParents(headers, lines - lines[0], parents)

                        }

                    } else {

                        diffHeaders = crossHeaders-headers.toSet()

                        if (diffHeaders.isEmpty()) {

                            loadCrosses(headers, lines-lines[0], crosses, metadata, metaValues)

                        }
                    }

                } else {

                    diffHeaders = wishlistHeaders-headers.toSet()

                    if (diffHeaders.isEmpty()) {

                        loadWishlist(headers, lines - lines[0], wishlist, parents)

                    }

                }

                if (diffHeaders.isNotEmpty()) {

                    Dialogs.notify(
                            AlertDialog.Builder(ctx),
                            ctx.getString(R.string.missing_headers),
                            message = diffHeaders.joinToString("\n"))


                }
            }

        }

        return arrayOf(crosses, parents, wishlist, metadata, metaValues)
    }


    //Name's must be to specification / order doesn't matter
    //Barcodes required, names copy to barcode

    /**
     * Input File expected format:
     *      header1, header2
     *      code1,  readableName
     *      code2, readableName2,
     *      ...
     *
     * Where header1 and header2 are string texts defined in XML (to be translated if necessary)
     *
     */
    //TODO Low-priority: switch to yield/iterator to reduce heap usage
    private fun loadParents(headers: List<String>,
                            lines: List<String>,
                            parents: ArrayList<Parent>) {

        //the headers must include at least the code id header
        if (!headers.find { it == parentsCodeIdHeader }.isNullOrBlank()) {

            val headerToIndex = headers
                    .mapIndexed { index, s -> s to index }
                    .toMap()

            lines.forEach { it ->

                val row = it.split(",").map { it.trim() }

                //if the rowsize is 2 it must be the parents code id and sex
                if (row.size == 2 && parentsCodeIdHeader in headerToIndex.keys
                        && parentsSexHeader in headerToIndex.keys) {

                    headerToIndex[parentsCodeIdHeader]?.let { codeKey ->

                        headerToIndex[parentsSexHeader]?.let { sexKey ->

                            parents.add(Parent(row[codeKey], row[sexKey].toInt()))
                        }
                    }

                } else if (row.size == 3) {

                    //ensure code id exists
                    headerToIndex[parentsCodeIdHeader]?.let { key ->

                        headerToIndex[parentsSexHeader]?.let { sexKey ->

                            val readableNameIndex = headerToIndex[parentsNameHeader]

                            if (readableNameIndex == null) {

                                parents.add(
                                        Parent(row[key], row[sexKey].toInt())
                                )

                            } else {

                                parents.add(
                                        Parent(
                                                row[key],
                                                row[sexKey].toInt(),
                                                row[readableNameIndex])
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadCrosses(headers: List<String>,
                            lines: List<String>,
                            crosses: ArrayList<Event>,
                            metadata: ArrayList<Meta>,
                            metaValues: ArrayList<MetadataValues>) {

        //the headers must include at least the code id header
        if (!headers.find { it == crossIdHeader }.isNullOrBlank()) {

            val headerToIndex = headers
                    .mapIndexed { index, s -> s to index }
                    .toMap()

            lines.forEach { it ->

                val row = it.split(",").map { it.trim() }

                val metadataFields = row.size - crossHeaders.size

                headerToIndex[crossIdHeader]?.let { crossIdKey ->

                    headerToIndex[crossMomHeader]?.let { momKey ->

                        headerToIndex[crossDadHeader]?.let { dadKey ->

                            headerToIndex[crossTimestampHeader]?.let { time ->

                                headerToIndex[crossPersonHeader]?.let { personKey ->

                                    headerToIndex[crossExperimentHeader]?.let { expKey ->

                                        headerToIndex[crossTypeHeader]?.let { typeKey ->

                                            crosses.add(
                                                Event(
                                                    row[crossIdKey],
                                                    row[momKey],
                                                    row[dadKey],
                                                    timestamp = row[time],
                                                    person = row[personKey],
                                                    experiment = row[expKey],
                                                    type = when (row[typeKey]) {
                                                        "BIPARENTAL" -> CrossType.BIPARENTAL
                                                        "OPEN" -> CrossType.OPEN
                                                        "POLY" -> CrossType.POLY
                                                        "SELF" -> CrossType.SELF
                                                        else -> CrossType.UNKNOWN
                                                    }
                                                )
                                            )
                                            //only metadata values (not default values) are persisted across import/exports
                                            //must add fake eid and metaId until we insert the actual data into Room
                                            for (i in crossHeaders.size until crossHeaders.size+metadataFields) {
                                                metadata.add(Meta(headers[i]))
                                                metaValues.add(MetadataValues(-1, -1, row[i].toIntOrNull() ?: 0))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This function returns True if the required headers exist in the input file
     */
    private fun validateHeaders(headers: List<String>, ensured: List<String>): Boolean {

        return headers.intersect(ensured).size == ensured.size
    }

    //TODO Low-priority: switch to yield/iterator
    //TODO maybe add wish types to metadata fields automatically?
    private fun loadWishlist(headers: List<String>,
                             lines: List<String>,
                             wishlist: ArrayList<Wishlist>,
                             parents: ArrayList<Parent>) {

        if (validateHeaders(headers, listOf(
                        wishlistFemaleIdHeader,
                        wishlistMaleIdHeader,
                        wishlistTypeHeader,
                        wishlistMinHeader))) {

            val headerIndices = headers
                    .mapIndexed { index, s -> s to index }
                    .toMap()

            lines.forEach { rawRow ->

                if (rawRow.isNotBlank()) {

                    val row = rawRow.split(',').map { it.trim() }

                    var readableMaleName: String? = null

                    var readableFemaleName: String? = null

                    var wishlistMax: Int? = null

                    /*
                    Try to parse all optional columns.
                     */
                    if (wishlistMaleNameHeader in headerIndices) {

                        headerIndices[wishlistMaleNameHeader]?.let { key ->

                            readableMaleName = row[key]
                        }
                    }

                    if (wishlistFemaleNameHeader in headerIndices) {

                        headerIndices[wishlistFemaleNameHeader]?.let { key ->

                            readableFemaleName = row[key]
                        }
                    }

                    if (wishlistMaxHeader in headerIndices) {

                        headerIndices[wishlistMaxHeader]?.let { key ->

                            wishlistMax = row[key].toInt()
                        }
                    }

                    /**
                     * finally ensure that required columns exist, and add wishlists to ref array
                     */

                    if (wishlistMaleIdHeader in headerIndices
                        && wishlistFemaleIdHeader in headerIndices
                        && wishlistTypeHeader in headerIndices
                        && wishlistMinHeader in headerIndices) {

                        headerIndices[wishlistMaleIdHeader]?.let { maleId ->

                            headerIndices[wishlistFemaleIdHeader]?.let { femaleId ->

                                headerIndices[wishlistTypeHeader]?.let { type ->

                                    headerIndices[wishlistMinHeader]?.let { min ->

                                        wishlist.add(Wishlist(
                                            femaleDbId = row[femaleId],
                                            maleDbId = row[maleId],
                                            femaleName = readableFemaleName ?: row[femaleId],
                                            maleName = readableMaleName ?: row[maleId],
                                            wishType = row[type],
                                            wishMin = row[min].toInt(),
                                            wishMax = wishlistMax
                                        ))

                                        /**
                                         * Add all parents parsed from the wishlist
                                         */
                                        parents.add(Parent(row[femaleId], 0).apply {
                                            name = readableFemaleName ?: row[femaleId]
                                        })

                                        parents.add(Parent(row[maleId], 1).apply {
                                            name = readableMaleName ?: row[maleId]
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun ringNotification(success: Boolean) {

        if (mPref.getBoolean(mKeyUtil.soundNotificationKey, false)) {
            try {
                when (success) {
                    true -> {
                        val chimePlayer = MediaPlayer.create(ctx, ctx.resources.getIdentifier("plonk", "raw", ctx.packageName))
                        chimePlayer.start()
                        chimePlayer.setOnCompletionListener {
                            chimePlayer.release()
                        }
                    }
                    false -> {
                        val chimePlayer = MediaPlayer.create(ctx, ctx.resources.getIdentifier("error", "raw", ctx.packageName))
                        chimePlayer.start()
                        chimePlayer.setOnCompletionListener {
                            chimePlayer.release()
                        }
                    }
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    fun exportCrossesToFile(uri: Uri, crosses: List<Event>, parents: List<Parent>, groups: List<PollenGroup>,
                            metadata: List<Meta>, metaValues: List<MetadataValues>) {

        val newLine: ByteArray = System.getProperty("line.separator")?.toByteArray() ?: "\n".toByteArray()

        try {

            ctx.contentResolver.openOutputStream(uri).apply {

                if (crosses.isNotEmpty()) {

                    this?.let {

                        val properties = if (metadata.isNotEmpty()) metadata.joinToString(",", ",") { it.property }
                                         else ""
                        val propMap = metadata.map { it.id to it.property }

                        //add metadata properties as headers to the export file
                        write((eventModelHeaderString + properties).toByteArray())

                        write(newLine)

                        crosses.forEachIndexed { index, cross ->

                            //print either the actual saved values for each property or its default value
                            val values = ArrayList<String>()
                            for (keyVal in propMap) {
                                val actuals = metaValues.filter { it.eid == cross.id?.toInt()
                                        && keyVal.first?.toInt() == it.metaId }
                                if (actuals.isNotEmpty()) {
                                    values.add(actuals.first().value.toString())
                                } else values.add(metadata
                                    .find { it.property == keyVal.second }?.defaultValue?.toString() ?: "0")
                            }

                            val valueString = if (values.isNotEmpty()) values.joinToString(",", ",") { it }
                                              else ""

                            //val values = metaValues.filter { it.eid == cross.id?.toInt() }
                            if (groups.any { g -> g.codeId == cross.maleObsUnitDbId }) {

                                var groupName = groups.find { g -> g.codeId == cross.maleObsUnitDbId }?.name

                                val males = groups.filter { g -> g.codeId == cross.maleObsUnitDbId }
                                    .map { g ->
                                        parents.find { c -> c.id == g.maleId }.let {
                                            it?.codeId
                                        }
                                    }.joinToString(";", "{", "}")

                                write((cross.toPollenGroupString(males, groupName) + valueString).toByteArray())

                                write(newLine)

                            } else {

                                write((cross.toString() + valueString).toByteArray())

                                write(newLine)

                            }
                        }

                        close()
                    }
                }
            }

        } catch (exception: FileNotFoundException) {

            Log.e("IntFileNotFound", "Chosen uri path was not found: $uri")

        }

        MediaScannerConnection.scanFile(ctx, arrayOf(uri.path), arrayOf("*/*"), null)

    }

    //    fun parseUri(uri: Uri): List<String> {
    //
    //        val fileUri = uri.path ?: ""
    //       /* val fileName =
    //                if (fileUri.lastIndexOf('/') != -1) {
    //                    fileUri.substring(fileUri.lastIndexOf('/') + 1)
    //                } else ""*/
    //        val filePath = FileUtil(ctx).getPath(uri)
    //
    //        val lastDot = fileUri.lastIndexOf(".")
    //
    //        return when (fileUri.substring(lastDot + 1)) {
    //            "xlsx", "xls" -> {
    //                parseExcelSheet(filePath)
    //            }
    //            "tsv" -> {
    //                parseTextFile(uri, "\t")
    //            }
    //            "csv", "txt" -> {
    //                parseTextFile(uri, ",")
    //            }
    //            else -> ArrayList()
    //        }
    //
    //    }

    @WorkerThread
    fun getFilePath(context: Context, uri: Uri): String? = context.run {
        when {

            Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ->
                getDataColumn(uri, null, null)

            else -> getPathKitkatPlus(uri)
        }
    }

    private fun Context.getPathKitkatPlus(uri: Uri): String? {
        when {
            DocumentsContract.isDocumentUri(applicationContext, uri) -> {
                val docId = DocumentsContract.getDocumentId(uri)
                when {
                    uri.isExternalStorageDocument -> {
                        val parts = docId.split(":")
                        if ("primary".equals(parts[0], true)) {
                            return "${Environment.getExternalStorageDirectory()}/${parts[1]}"
                        }
                    }
                    uri.isDownloadsDocument -> {
                        val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"),
                                docId.toLong()
                        )
                        return getDataColumn(contentUri, null, null)
                    }
                    uri.isMediaDocument -> {
                        val parts = docId.split(":")
                        val contentUri = when (parts[0].lowercase(Locale.ROOT)) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> return null
                        }
                        return getDataColumn(contentUri, "_id=?", arrayOf(parts[1]))
                    }
                }
            }
            "content".equals(uri.scheme, true) -> {
                return if (uri.isGooglePhotosUri) {
                    uri.lastPathSegment
                } else {
                    getDataColumn(uri, null, null)
                }
            }
            "file".equals(uri.scheme, true) -> {
                return uri.path
            }
        }
        return null
    }

    private fun Context.getDataColumn(uri: Uri, selection: String?, args: Array<String>?): String? {
        contentResolver?.query(uri, arrayOf("_data"), selection, args, null)?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow("_data"))
            }
        }
        return null
    }

    private val Uri.isExternalStorageDocument: Boolean
        get() = authority == "com.android.externalstorage.documents"

    private val Uri.isDownloadsDocument: Boolean
        get() = authority == "com.android.providers.downloads.documents"

    private val Uri.isMediaDocument: Boolean
        get() = authority == "com.android.providers.media.documents"

    private val Uri.isGooglePhotosUri: Boolean
        get() = authority == "com.google.android.apps.photos.content"

//        private fun parseExcelSheet(filePath: String): List<String> {
//    //        val workbook = WorkbookFactory.create(File(filePath))
//    //        return if (workbook.numberOfSheets > 0) {
//    //            workbook.getSheetAt(0).rowIterator().asSequence().toList().map {
//    //                it.cellIterator().asSequence().joinToString(",")
//    //            }
//    //        } else ArrayList()
//            return ArrayList()
//        }

    /**
     * Opens an input stream from the user-selected uri and copies it to the app specific database.
     */
    fun importDatabase(uri: Uri) {

        //open cache directory to temporarily unzip db and prefs file to
        ctx.externalCacheDir?.path?.let { parent ->

            try {

                val stream = ctx.getDatabasePath(IntercrossDatabase.DATABASE_NAME).outputStream()

                val inputStream = ctx.contentResolver.openInputStream(uri)

                val dir = File(parent, "temp")

                if (dir.mkdir() || dir.exists()) {

                    unzip(inputStream, stream)

                }

                dir.delete()

            } catch (e: IOException) {

                e.printStackTrace()

            }
        }
    }

    /**
     * Opens the default database location /data/data/org.../databases/intercross.db as an input stream
     * The stream is then copied to the parameter uri which is chosen by the user.
     *
     * Also backup /data/data/org.phenoapps.intercross/shared_prefs/org.phenoapps.intercross_preferences.xml
     * and compress the .db and .xml files to a zip
     */
    fun exportDatabase(uri: Uri) {

        //create parent directory for storing intercross.db and shared_prefs.xml
        //this directory is temporary and will be used to create a zip file
        ctx.externalCacheDir?.path?.let { parent ->

            try {

                val stream = ctx.getDatabasePath(IntercrossDatabase.DATABASE_NAME).inputStream()

                val zipOutput = ctx.contentResolver.openOutputStream(uri)

                val dir = File(parent, "backup")

                if (dir.mkdir() || dir.exists()) {

                    val dbFile = File(dir.path, "intercross.db")
                    val databaseOutput = ctx.contentResolver.openOutputStream(dbFile.toUri())

                    val prefFile = File(dir.path, "preferences_backup")
                    val prefOutput = ctx.contentResolver.openOutputStream(prefFile.toUri())

                    stream.write(databaseOutput)

                    val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

                    val objectOutputStream = ObjectOutputStream(prefOutput)

                    objectOutputStream.writeObject(prefs.all)

                    zip(arrayOf(dbFile.path, prefFile.path), zipOutput)

                    dir.delete()

                } else throw IOException()

            } catch (e: IOException) {

                e.printStackTrace()

            }
        }
    }

    //reference https://stackoverflow.com/questions/7485114/how-to-zip-and-unzip-the-files
    @Throws(IOException::class)
    private fun zip(files: Array<String>, zipFile: OutputStream?) {

        ZipOutputStream(BufferedOutputStream(zipFile)).use { output ->

            var origin: BufferedInputStream? = null

            val bufferSize = 8192 //default buffersize for BufferedWriter
            val data = ByteArray(bufferSize)

            for (i in files.indices) {

                val fi = FileInputStream(files[i])

                origin = BufferedInputStream(fi, bufferSize)

                try {

                    val entry = ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1))

                    output.putNextEntry(entry)

                    var count: Int

                    while (origin.read(data, 0, bufferSize).also { count = it } != -1) {

                        output.write(data, 0, count)

                    }
                } finally {

                    origin.close()

                }
            }
        }
    }

    //the expected zip file format contains two files
    //1. intercross.db this can be directly copied to the data dir
    //2. preferences_backup needs to:
    //  a. read and converted to a map <string to any (which is only boolean or string)>
    //  b. preferences should be cleared of the old values
    //  c. iterate over the converted map and populate the preferences
    @Throws(IOException::class)
    fun unzip(zipFile: InputStream?, databaseStream: OutputStream) {

        try {

            ZipInputStream(zipFile).use { zin ->

                var ze: ZipEntry? = null

                while (zin.nextEntry.also { ze = it } != null) {

                    when (ze?.name) {

                        null -> throw IOException()

                        "intercross.db" -> {

                            databaseStream.use { output ->

                                zin.copyTo(output)

                            }
                        }

                        "preferences_backup" -> {

                            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx)

                            ObjectInputStream(zin).use { objectStream ->

                                val prefMap = objectStream.readObject() as Map<*, *>

                                with (prefs.edit()) {

                                    clear()

                                    //keys are always string, do a quick map to type cast
                                    //put values into preferences based on their types
                                    prefMap.entries.map { it.key as String to it.value }
                                        .forEach {

                                            val key = it.first

                                            //right now Intercross only has string and boolean preferences
                                            when (val x = it.second) {

                                                is Boolean -> putBoolean(key, x)

                                                is String -> putString(key, x)
                                            }
                                        }

                                    apply()
                                }
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {

            Log.e("FileUtil", "Unzip exception", e)

        }
    }

    private fun InputStream.write(outStream: OutputStream?) {

        use { input ->

            outStream.use { output ->

                output?.let { outstream ->

                    input.copyTo(outstream)

                }
            }
        }
    }

    private fun parseTextFile(it: Uri): List<String> {

        var ret = ArrayList<String>()

        try {

            val stream = ctx.contentResolver.openInputStream(it)

            stream?.let {

                val reader = BufferedReader(InputStreamReader(it))

                    ret = ArrayList(reader.readLines().map { line ->
                        line.replace("\uFEFF", "") //erase BOM
                    })
                }
            } catch (fo: FileNotFoundException) {
                fo.printStackTrace()
            } catch (io: IOException) {
                io.printStackTrace()
            }

        return ret
    }
}