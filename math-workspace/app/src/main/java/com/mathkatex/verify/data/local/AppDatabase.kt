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
        KnowledgeEdgeEntity::class,
        KnowledgeAliasEntity::class,
        KnowledgePathEntity::class,
        KnowledgePathItemEntity::class,
        KnowledgeModelEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun exampleDao(): ExampleDao
    abstract fun practiceDao(): PracticeDao
    abstract fun knowledgeGraphDao(): KnowledgeGraphDao

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

        /** v3 -> v4：知识图谱关系表、别名、路径、题型模型；三张旧表补 knowledgeId/abilityLevel。 */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE knowledge_points ADD COLUMN abilityLevel TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE examples ADD COLUMN knowledgeId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE practice_records ADD COLUMN knowledgeId TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `knowledge_edges` (" +
                        "`fromId` TEXT NOT NULL, `toId` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                        "`confidence` REAL NOT NULL, `note` TEXT NOT NULL, " +
                        "PRIMARY KEY(`fromId`, `toId`, `type`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_edges_toId` ON `knowledge_edges` (`toId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `knowledge_aliases` (" +
                        "`alias` TEXT NOT NULL, `knowledgeId` TEXT NOT NULL, " +
                        "PRIMARY KEY(`alias`, `knowledgeId`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_aliases_knowledgeId` ON `knowledge_aliases` (`knowledgeId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `knowledge_paths` (" +
                        "`id` TEXT NOT NULL, `title` TEXT NOT NULL, `grade` TEXT NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `knowledge_path_items` (" +
                        "`pathId` TEXT NOT NULL, `knowledgeId` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`pathId`, `knowledgeId`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_path_items_knowledgeId` ON `knowledge_path_items` (`knowledgeId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `knowledge_models` (" +
                        "`modelName` TEXT NOT NULL, `knowledgeId` TEXT NOT NULL, `confidence` REAL NOT NULL, " +
                        "PRIMARY KEY(`modelName`, `knowledgeId`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_models_knowledgeId` ON `knowledge_models` (`knowledgeId`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "math_app.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
