package com.episode6.headachetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.episode6.headachetracker.model.HeadacheEntry

@Database(entities = [HeadacheEntry::class], version = 4, exportSchema = false)
abstract class HeadacheDatabase : RoomDatabase() {
    abstract fun headacheDao(): HeadacheDao

    companion object {
        @Volatile
        private var INSTANCE: HeadacheDatabase? = null

        fun getDatabase(context: Context): HeadacheDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HeadacheDatabase::class.java,
                    "headache_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
