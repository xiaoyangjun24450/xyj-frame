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
            callback(
                Result.success(
                    listOf(
                        mockMathPlan(),
                        mockChinesePlan(),
                        mockEnglishPlan(),
                        mockPhysicsPlan(),
                        mockChemistryPlan(),
                        mockHistoryPlan()
                    )
                )
            )
        }, MOCK_DELAY_SHORT_MS)
    }

    private fun mockMathPlan(): StudyPlan {
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

    private fun mockChinesePlan(): StudyPlan {
        val examples = listOf(
            Question(
                id = "chinese-ex-1",
                title = "例题 1：概括段意",
                prompt = "阅读短文第一段，用一句话概括这一段主要写了什么。",
                subject = "语文"
            ),
            Question(
                id = "chinese-ex-2",
                title = "例题 2：修辞判断",
                prompt = "句子“月亮像小船一样挂在天空”用了什么修辞手法？有什么表达效果？",
                subject = "语文"
            )
        )
        val exams = listOf(
            Question(
                id = "chinese-exam-1",
                title = "考试题 1：词语理解",
                prompt = "结合上下文，解释“坚持不懈”在句子中的意思。",
                subject = "语文"
            ),
            Question(
                id = "chinese-exam-2",
                title = "考试题 2：仿写句子",
                prompt = "仿照“春风轻轻地吹醒了小草”，写一个拟人句。",
                subject = "语文"
            )
        )
        return StudyPlan(
            id = "plan-local-chinese",
            title = "语文阅读表达训练",
            subject = "语文",
            estimatedMinutes = 20,
            passScore = 80,
            exampleQuestions = examples,
            examQuestions = exams
        )
    }

    private fun mockEnglishPlan(): StudyPlan {
        val examples = listOf(
            Question(
                id = "english-ex-1",
                title = "例题 1：一般现在时",
                prompt = "用正确形式填空：She usually ______ (go) to school by bus.",
                subject = "英语"
            ),
            Question(
                id = "english-ex-2",
                title = "例题 2：阅读细节",
                prompt = "Read the sentence: Tom has breakfast at seven. What time does Tom have breakfast?",
                subject = "英语"
            )
        )
        val exams = listOf(
            Question(
                id = "english-exam-1",
                title = "考试题 1：单词拼写",
                prompt = "根据中文写英文单词：图书馆。",
                subject = "英语"
            ),
            Question(
                id = "english-exam-2",
                title = "考试题 2：句型转换",
                prompt = "把句子改为否定句：He likes apples.",
                subject = "英语"
            )
        )
        return StudyPlan(
            id = "plan-local-english",
            title = "英语基础句型训练",
            subject = "英语",
            estimatedMinutes = 18,
            passScore = 75,
            exampleQuestions = examples,
            examQuestions = exams
        )
    }

    private fun mockPhysicsPlan(): StudyPlan {
        val examples = listOf(
            Question(
                id = "physics-ex-1",
                title = "例题 1：速度计算",
                prompt = "小车 10 秒行驶 50 米，它的平均速度是多少米每秒？",
                subject = "物理"
            ),
            Question(
                id = "physics-ex-2",
                title = "例题 2：力的作用",
                prompt = "用手推门，门会转动。这个现象说明力可以改变物体的什么？",
                subject = "物理"
            )
        )
        val exams = listOf(
            Question(
                id = "physics-exam-1",
                title = "考试题 1：密度",
                prompt = "一个物体质量 200 克，体积 100 立方厘米，它的密度是多少？",
                subject = "物理"
            ),
            Question(
                id = "physics-exam-2",
                title = "考试题 2：光的反射",
                prompt = "照镜子时能看到自己，主要利用了光的什么现象？",
                subject = "物理"
            )
        )
        return StudyPlan(
            id = "plan-local-physics",
            title = "物理基础概念训练",
            subject = "物理",
            estimatedMinutes = 22,
            passScore = 80,
            exampleQuestions = examples,
            examQuestions = exams
        )
    }

    private fun mockChemistryPlan(): StudyPlan {
        val examples = listOf(
            Question(
                id = "chemistry-ex-1",
                title = "例题 1：物理变化",
                prompt = "冰融化成水，物质本身有没有变成新物质？这属于什么变化？",
                subject = "化学"
            ),
            Question(
                id = "chemistry-ex-2",
                title = "例题 2：空气成分",
                prompt = "空气中含量最多的气体是什么？",
                subject = "化学"
            )
        )
        val exams = listOf(
            Question(
                id = "chemistry-exam-1",
                title = "考试题 1：实验现象",
                prompt = "蜡烛燃烧时会发光发热，这说明发生了什么变化？",
                subject = "化学"
            ),
            Question(
                id = "chemistry-exam-2",
                title = "考试题 2：物质分类",
                prompt = "水、氧气、铁三种物质中，哪一种是化合物？",
                subject = "化学"
            )
        )
        return StudyPlan(
            id = "plan-local-chemistry",
            title = "化学入门现象训练",
            subject = "化学",
            estimatedMinutes = 20,
            passScore = 78,
            exampleQuestions = examples,
            examQuestions = exams
        )
    }

    private fun mockHistoryPlan(): StudyPlan {
        val examples = listOf(
            Question(
                id = "history-ex-1",
                title = "例题 1：时间顺序",
                prompt = "把“秦统一六国”和“汉朝建立”按发生时间先后排序。",
                subject = "历史"
            ),
            Question(
                id = "history-ex-2",
                title = "例题 2：人物事件",
                prompt = "说出与“商鞅变法”相关的一个主要影响。",
                subject = "历史"
            )
        )
        val exams = listOf(
            Question(
                id = "history-exam-1",
                title = "考试题 1：朝代判断",
                prompt = "“开元盛世”出现在哪个朝代？",
                subject = "历史"
            ),
            Question(
                id = "history-exam-2",
                title = "考试题 2：历史意义",
                prompt = "秦统一文字和度量衡，对国家管理有什么帮助？",
                subject = "历史"
            )
        )
        return StudyPlan(
            id = "plan-local-history",
            title = "历史基础线索训练",
            subject = "历史",
            estimatedMinutes = 24,
            passScore = 80,
            exampleQuestions = examples,
            examQuestions = exams
        )
    }
}
