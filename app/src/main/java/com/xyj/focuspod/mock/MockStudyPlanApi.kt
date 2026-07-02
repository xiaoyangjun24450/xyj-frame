package com.xyj.focuspod.mock

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.xyj.focuspod.data.StudyPlanJsonParser
import com.xyj.focuspod.model.Question
import com.xyj.focuspod.model.StudyPlan
import com.xyj.focuspod.service.api.StudyPlanApi

class MockStudyPlanApi(
    private val context: Context,
    private val handler: Handler = Handler(Looper.getMainLooper())
) : StudyPlanApi {

    override fun getPlans(callback: (Result<List<StudyPlan>>) -> Unit) {
        handler.postDelayed({
            callback(
                runCatching {
                    val json = context.assets.open(ASSET_FILE_NAME)
                        .bufferedReader()
                        .use { it.readText() }
                    StudyPlanJsonParser.parsePlans(json)
                }.recover {
                    fallbackPlans()
                }
            )
        }, MOCK_DELAY_SHORT_MS)
    }

    private fun fallbackPlans(): List<StudyPlan> {
        return listOf(
            StudyPlan(
                id = "plan-local-math",
                title = "分数应用题练习",
                subject = "数学",
                passScore = 80,
                estimatedMinutes = 20,
                exampleQuestions = listOf(
                    Question(
                        id = "example-001",
                        title = "例题：分数应用题",
                        questionMarkdown = "一根绳子长 **24 米**，用去了 $\\frac{1}{3}$。还剩多少米？",
                        answer = "16 米",
                        solutionMarkdown = "先算用去多少米：$24 \\times \\frac{1}{3} = 8$。\n\n再算剩下多少米：$24 - 8 = 16$。",
                        knowledgePoints = listOf("求一个数的几分之几", "剩余量计算"),
                        tutoringPrompt = "引导学生先找总量，再理解用去了三分之一，最后思考剩余量怎么计算。不要直接告诉最终答案。",
                        gradingPrompt = "检查学生是否先算出用去 8 米，再算出剩余 16 米，并注意单位。"
                    )
                ),
                examQuestions = listOf(
                    Question(
                        id = "exam-001",
                        title = "考试题：折扣计算",
                        questionMarkdown = "一件商品原价 **120 元**，打八折后又优惠 **6 元**，实际付款多少元？",
                        answer = "90 元",
                        solutionMarkdown = "八折后的价格：$120 \\times 0.8 = 96$ 元。\n\n再优惠 6 元：$96 - 6 = 90$ 元。",
                        knowledgePoints = listOf("折扣", "多步计算"),
                        tutoringPrompt = "引导学生先理解八折表示原价的 80%，再扣掉优惠金额。不要直接告诉最终答案。",
                        gradingPrompt = "检查学生是否先算出八折价 96 元，再减去 6 元得到 90 元。"
                    )
                )
            )
        )
    }

    private companion object {
        const val ASSET_FILE_NAME = "study_plans.json"
    }
}
