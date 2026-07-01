package com.xyj.focuspod.service.api

import com.xyj.focuspod.model.StudyPlan

interface StudyPlanApi {
    fun getPlans(callback: (Result<List<StudyPlan>>) -> Unit)
}
