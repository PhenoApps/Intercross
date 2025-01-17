package org.phenoapps.intercross.data

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.phenoapps.intercross.data.dao.EventsDao
import org.phenoapps.intercross.data.models.Event

class EventsRepository
    private constructor(private val eventsDao: EventsDao): BaseRepository<Event>(eventsDao) {

    fun selectAll() = eventsDao.selectAll()

    fun getParentCount() = eventsDao.getParentCount()

    fun getAllParents() = eventsDao.getAllParents()

    fun getMetadata() = eventsDao.getMetadata()

    fun getMetadata(eid: Long) = eventsDao.getMetadata(eid)

    fun getParents(eid: Long) = eventsDao.getParents(eid)

    suspend fun getEvent(eid: Long) = eventsDao.selectById(eid)

    fun getRowid(e: Event): Long = eventsDao.getRowid(e.eventDbId, e.femaleObsUnitDbId, e.maleObsUnitDbId, e.timestamp)

    suspend fun drop() {

        withContext(IO) {

            eventsDao.drop()

        }
    }

    fun deleteById(eid: Long) {

        runBlocking {

            eventsDao.deleteById(eid)

        }
    }

    fun insert(event: Event): Long = eventsDao.insertEvent(event)

    fun loadCrosses() = eventsDao.selectAllLive()

    companion object {
        @Volatile private var instance: EventsRepository? = null

        fun getInstance(eventsDao: EventsDao) =
                instance ?: synchronized(this) {
                    instance ?: EventsRepository(eventsDao)
                        .also { instance = it }
                }
    }
}