package com.nramos.finance_report.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileUpdateEvent @Inject constructor() {
    private val _updateFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val updateFlow = _updateFlow.asSharedFlow()

    suspend fun emitUpdate() {
        _updateFlow.emit(Unit)
    }
}