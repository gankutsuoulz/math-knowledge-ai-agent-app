package com.mathkatex.verify.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        KnowledgeEntity::class,
        ExampleEntity::class,
        PracticeRecordEntity::class,
        FavoriteEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun exampleDao(): ExampleDao
    abstract fun practiceDao(): PracticeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v1 -> v2：新增 examples 表。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `examples` (" +
                        "`id` TEXT NOT NULL, " +
                        "`knowledgeTitle` TEXT NOT NULL, " +
                        "`grade` TEXT NOT NULL, " +
                        "`difficulty` INTEGER NOT NULL, " +
                        "`question` TEXT NOT NULL, " +
                        "`answer` TEXT NOT NULL, " +
                        "`solution` TEXT NOT NULL, " +
                        "`source` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_examples_knowledgeTitle` ON `examples` (`knowledgeTitle`)",
                )
            }
        }

        /** v2 -> v3：例题加 type/options，新增练习记录和收藏。 */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE examples ADD COLUMN type TEXT NOT NULL DEFAULT 'choice'")
                db.execSQL("ALTER TABLE examples ADD COLUMN options TEXT NOT NULL DEFAULT '[]'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `practice_records` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`exampleId` TEXT NOT NULL, " +
                        "`correct` INTEGER NOT NULL, " +
                        "`userAnswer` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_practice_records_exampleId` ON `practice_records` (`exampleId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites` (" +
                        "`exampleId` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`exampleId`))",
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "math_app.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
