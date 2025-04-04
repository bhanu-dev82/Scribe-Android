// SPDX-License-Identifier: GPL-3.0-or-later

/**
 * A helper class to manage and query plural forms of
 * words from an SQLite database based on the provided language and JSON contract.
 */

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log

class PluralFormsManager(
    private val context: Context,
) {
    fun checkIfWordIsPlural(
        language: String,
        jsonData: DataContract?,
    ): List<String>? {
        if (jsonData?.numbers?.values.isNullOrEmpty()) {
            Log.e("MY-TAG", "JSON data for 'numbers' is null or empty.")
            return null
        }

        val dbFile = context.getDatabasePath("${language}LanguageData.sqlite")
        val pluralForms = jsonData!!.numbers.values.toList()
        Log.d("MY-TAG", "Plural Forms: $pluralForms")

        return queryPluralForms(dbFile.path, pluralForms)
    }

    fun queryPluralRepresentation(
        language: String,
        jsonData: DataContract?,
        noun: String,
    ): Map<String, String?> {
        if (jsonData?.numbers?.values.isNullOrEmpty()) {
            Log.e("MY-TAG", "JSON data for 'numbers' is null or empty.")
            return mapOf()
        }

        val dbFile = context.getDatabasePath("${language}LanguageData.sqlite")
        val pluralForms = jsonData!!.numbers.values.toList()
        val singularForms = jsonData.numbers.keys.toList()

        return queryPluralForms(dbFile.path, pluralForms, singularForms, noun)
    }

    private fun queryPluralForms(
        dbPath: String,
        pluralForms: List<String>,
    ): List<String> {
        val result = mutableListOf<String>()
        val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)

        db.use { database ->
            database.rawQuery("SELECT * FROM nouns", null)?.use { cursor ->
                processPluralFormsCursor(cursor, pluralForms, result)
            }
        }
        db.close()
        return result
    }

    private fun queryPluralForms(
        dbPath: String,
        pluralForms: List<String>,
        singularForms: List<String>,
        noun: String,
    ): Map<String, String?> {
        val result = mutableListOf<String>()
        val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
        val queryBuilder = StringBuilder("SELECT * FROM nouns WHERE")
        val placeholders = singularForms.joinToString("OR ") { "$it = ?" }
        queryBuilder.append(" $placeholders;")
        val selectionArgs = Array(singularForms.size) { noun }

        db.use { database ->
            database.rawQuery(queryBuilder.toString(), selectionArgs)?.use { cursor ->
                processPluralFormsCursor(cursor, pluralForms, result)
            }
        }
        db.close()
        return pluralForms.zip(result).toMap()
    }

    private fun processPluralFormsCursor(
        cursor: Cursor,
        pluralForms: List<String>,
        result: MutableList<String>,
    ) {
        if (!cursor.moveToFirst()) {
            Log.w("MY-TAG", "Cursor is empty, no data found in 'nouns' table.")
            return
        }

        do {
            addPluralForms(cursor, pluralForms, result)
        } while (cursor.moveToNext())
    }

    private fun addPluralForms(
        cursor: Cursor,
        pluralForms: List<String>,
        result: MutableList<String>,
    ) {
        pluralForms.forEach { pluralForm ->
            val columnIndex = cursor.getColumnIndex(pluralForm)
            if (columnIndex != -1) {
                cursor.getString(columnIndex)?.let { result.add(it) }
            } else {
                Log.e("MY-TAG", "Column '$pluralForm' not found in the database.")
            }
        }
    }
}
