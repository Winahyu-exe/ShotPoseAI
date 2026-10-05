package com.shotposeai.data

import android.content.Context

class AppRepository(context: Context) {
    private val dao = DatabaseProvider.get(context).appDao()
    fun dashboard(): DashboardCounts = DashboardCounts(
        classCount = dao.classCount(),
        studentCount = dao.studentCount(),
        trainingCount = dao.trainingCount(),
        averageScore = dao.averageScore(),
        passedCount = dao.passedCount()
    )
    data class DashboardCounts(
        val classCount: Int,
        val studentCount: Int,
        val trainingCount: Int,
        val averageScore: Double,
        val passedCount: Int
    )
}
