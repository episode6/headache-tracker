package com.episode6.headachetracker.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE headache_entries ADD COLUMN pillsTaken INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE headache_entries ADD COLUMN firstPillTime INTEGER")
        db.execSQL("ALTER TABLE headache_entries ADD COLUMN secondPillTime INTEGER")
    }
}
