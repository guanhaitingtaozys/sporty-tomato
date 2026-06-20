package com.sportytomato.app.service

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import kotlinx.coroutines.runBlocking

/**
 * Kotlin bridge for Health Connect suspend functions and permission APIs.
 */
object HealthConnectBridge {

    private val STEP_READ_PERMISSION = HealthPermission.getReadPermission(StepsRecord::class)

    @JvmStatic
    fun readTodaySteps(client: HealthConnectClient, request: AggregateRequest): Long {
        return runBlocking {
            val result = client.aggregate(request)
            result[StepsRecord.COUNT_TOTAL] ?: 0L
        }
    }

    /** Check if step read permission is already granted. */
    @JvmStatic
    fun hasStepPermission(client: HealthConnectClient): Boolean {
        return runBlocking {
            val granted = client.permissionController.getGrantedPermissions()
            granted.contains(STEP_READ_PERMISSION)
        }
    }
}
