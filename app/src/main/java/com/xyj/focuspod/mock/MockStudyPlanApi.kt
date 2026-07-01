package com.xyj.focuspod.mock

import android.os.Handler
import android.os.Looper
import com.xyj.focuspod.model.Question
import com.xyj.focuspod.model.StudyPlan
import com.xyj.focuspod.service.api.StudyPlanApi

class MockStudyPlanApi(
    private val handler: Handler = Handler(Looper.getMainLooper())
) : StudyPlanApi {

    override fun getPlans(callback: (Result<List<StudyPlan>>) -> Unit) {
        handler.postDelayed({
            callback(Result.success(listOf(mockPlan())))
        }, MOCK_DELAY_SHORT_MS)
    }

    private fun mockPlan(): StudyPlan {
        val examples = listOf(
            Question(
                id = "ex-1",
                title = "例题 1：分数应用",
                prompt = "一根绳子长 24 米，用去 1/3 后，又用去剩下的 1/4。还剩多少米？",
                subject = "数学"
            ),
            Question(
                id = "ex-2",
                title = "例题 2：行程问题",
                prompt = "甲乙两地相距 180 千米，一辆车 3 小时行完全程。平均每小时行多少千米？",
                subject = "数学"
            )
        )
        val exams = listOf(
            Question(
                id = "exam-1",
                title = "考试题 1：比例",
                prompt = "一件商品原价 120 元，打八折后又优惠 6 元，实际付款多少元？",
                subject = "数学"
            ),
            Question(
                id = "exam-2",
                title = "考试题 2：工程问题",
                prompt = "一本书 96 页，小明 4 天读了全书的 1/2。照这样读完还需要几天？",
                subject = "数学"
            )
        )
        return StudyPlan(
            id = "plan-local-math",
            title = "六年级数学闭环训练",
            subject = "数学",
            estimatedMinutes = 25,
            passScore = 80,
            exampleQuestions = examples,
            examQuestions = exams
        )
    }
}
