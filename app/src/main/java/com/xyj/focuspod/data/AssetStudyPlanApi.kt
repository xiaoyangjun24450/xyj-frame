package com.xyj.focuspod.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.xyj.focuspod.model.StudyPlan
import com.xyj.focuspod.service.api.StudyPlanApi

class AssetStudyPlanApi(
    private val context: Context,
    private val handler: Handler = Handler(Looper.getMainLooper())
) : StudyPlanApi {

    override fun getPlans(callback: (Result<List<StudyPlan>>) -> Unit) {
        handler.post {
            callback(
                runCatching {
                    val json = context.assets.open(ASSET_FILE_NAME)
                        .bufferedReader()
                        .use { it.readText() }
                    StudyPlanJsonParser.parsePlans(json)
                }
            )
        }
    }

    private companion object {
        const val ASSET_FILE_NAME = "study_plans.json"
    }
}
