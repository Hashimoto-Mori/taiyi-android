package com.jinggong.timer.data.repo

import com.jinggong.timer.data.db.DailyModeTotal
import com.jinggong.timer.data.db.ModeTotal
import com.jinggong.timer.data.db.SessionDao
import com.jinggong.timer.data.db.SessionEntity
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val dao: SessionDao) {

    fun observeAll(): Flow<List<SessionEntity>> = dao.observeAll()

    fun observeByMode(mode: String): Flow<List<SessionEntity>> = dao.observeByMode(mode)

    fun observeDailyTotals(from: Long, to: Long): Flow<List<DailyModeTotal>> =
        dao.observeDailyTotals(from, to)

    fun observeAllDailyTotals(): Flow<List<DailyModeTotal>> = dao.observeAllDailyTotals()

    fun observePracticeDays(): Flow<List<String>> = dao.observePracticeDays()

    fun observeLatestStart(mode: String): Flow<Long?> = dao.observeLatestStart(mode)

    fun observeModeTotals(from: Long, to: Long): Flow<List<ModeTotal>> =
        dao.observeModeTotals(from, to)

    suspend fun insert(session: SessionEntity): Long = dao.insert(session)

    suspend fun update(session: SessionEntity) = dao.update(session)

    suspend fun delete(session: SessionEntity) = dao.delete(session)

    suspend fun clearAll() = dao.deleteAll()

    suspend fun getAllOnce(): List<SessionEntity> = dao.getAllOnce()
}
