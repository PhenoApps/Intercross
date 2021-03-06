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

    fun getParents(eid: Long) = eventsDao.getParents(eid)

    suspend fun getEvent(eid: Long) = eventsDao.selectById(eid)

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