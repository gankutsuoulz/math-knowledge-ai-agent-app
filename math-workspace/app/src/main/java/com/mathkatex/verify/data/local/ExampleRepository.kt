package com.mathkatex.verify.data.local

import android.content.Context
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 首次启动/版本更新时把 assets/examples.json 幂等写入；同 id 覆盖。 */
object ExampleImporter {
    suspend fun importSeed(context: Context, dao: ExampleDao): Int = withContext(Dispatchers.IO) {
        val raw = context.assets.open("examples.json").bufferedReader().use { it.readText() }
        val array = JsonParser.parseString(raw).asJsonArray
        val items = array.map { element ->
            val obj = element.asJsonObject
            ExampleEntity(
                id = obj.get("id").asString,
                knowledgeTitle = obj.get("knowledgeTitle").asString,
                grade = obj.get("grade").asString,
                difficulty = obj.get("difficulty").asInt,
                question = obj.get("question").asString,
                answer = obj.get("answer").asString,
                solution = obj.get("solution").asString,
                source = obj.get("source")?.asString ?: "local",
                type = obj.get("type")?.asString ?: "choice",
                options = obj.get("options")?.toString() ?: "[]",
            )
        }
        dao.insertAll(items)
        items.size
    }
}

/** 例题 + 练习记录 + 收藏的轻量仓库。 */
object ExampleRepository {
    private var exampleDao: ExampleDao? = null
    private var practiceDao: PracticeDao? = null

    suspend fun init(context: Context) {
        if (exampleDao != null) return
        val database = AppDatabase.getInstance(context)
        exampleDao = database.exampleDao()
        practiceDao = database.practiceDao()
        ExampleImporter.importSeed(context, exampleDao!!)
    }

    suspend fun getAll(): List<ExampleEntity> = withContext(Dispatchers.IO) {
        exampleDao?.getAll() ?: emptyList()
    }

    suspend fun byKnowledge(title: String): List<ExampleEntity> = withContext(Dispatchers.IO) {
        exampleDao?.byKnowledge(title) ?: emptyList()
    }

    suspend fun wrongExamples(): List<ExampleEntity> = withContext(Dispatchers.IO) {
        val ids = practiceDao?.wrongExampleIds() ?: emptyList()
        if (ids.isEmpty()) emptyList() else exampleDao?.getByIds(ids) ?: emptyList()
    }

    suspend fun favoriteExamples(): List<ExampleEntity> = withContext(Dispatchers.IO) {
        val ids = practiceDao?.favoriteIds() ?: emptyList()
        if (ids.isEmpty()) emptyList() else exampleDao?.getByIds(ids) ?: emptyList()
    }

    suspend fun favoriteIds(): Set<String> = withContext(Dispatchers.IO) {
        practiceDao?.favoriteIds()?.toSet() ?: emptySet()
    }

    suspend fun toggleFavorite(exampleId: String, favorite: Boolean) = withContext(Dispatchers.IO) {
        if (favorite) practiceDao?.addFavorite(FavoriteEntity(exampleId, System.currentTimeMillis()))
        else practiceDao?.removeFavorite(exampleId)
    }

    suspend fun record(exampleId: String, correct: Boolean, userAnswer: String) = withContext(Dispatchers.IO) {
        practiceDao?.insertRecord(
            PracticeRecordEntity(
                exampleId = exampleId,
                correct = correct,
                userAnswer = userAnswer,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun stats(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val total = practiceDao?.totalCount() ?: 0
        val correct = practiceDao?.correctCount() ?: 0
        total to correct
    }
}
