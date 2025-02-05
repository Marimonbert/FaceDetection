package com.example.facedetection.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.facedetection.data.Person
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "faceRecognition.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE people (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                imageUri TEXT NOT NULL,
                embedding TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS people")
        onCreate(db)
    }

    fun insertPerson(person: Person): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", person.name)
            put("imageUri", person.imageUri)
            put("embedding", person.embedding)
        }
        return db.insert("people", null, values).also {
            Log.d("DatabaseHelper", "✅ Pessoa salva: ${person.name}, ID: $it")
        }
    }

    fun getAllPersons(): List<Person> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, name, imageUri, embedding FROM people", null)
        val people = mutableListOf<Person>()

        while (cursor.moveToNext()) {
            people.add(
                Person(
                    id = cursor.getLong(0),
                    name = cursor.getString(1),
                    imageUri = cursor.getString(2),
                    embedding = cursor.getString(3)
                )
            )
        }
        cursor.close()
        return people
    }

    fun deletePerson(personId: Long) {
        val db = writableDatabase
        db.delete("people", "id = ?", arrayOf(personId.toString()))
        db.close()
    }

}
