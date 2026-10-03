package com.sagun12.vozemcena.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sagun12.vozemcena.data.mock.MockData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProjectEntity::class,
        SceneEntity::class,
        VoiceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun voiceDao(): VoiceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voz_em_cena_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = false)
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database)
                    }
                }
            }
        }

        suspend fun populateDatabase(database: AppDatabase) {
            val voiceDao = database.voiceDao()
            val projectDao = database.projectDao()

            // Prepopulate default voices
            val defaultVoiceEntities = MockData.defaultVoices.map { it.toEntity() }
            voiceDao.insertVoices(defaultVoiceEntities)

            // Prepopulate sample projects if none exist
            val sampleProjects = MockData.createSampleProjects()
            sampleProjects.forEach { project ->
                projectDao.insertProject(project.toEntity())
                projectDao.insertScenes(project.scenes.map { it.toEntity() })
            }
        }
    }
}
