package org.phenoapps.intercross.data

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.phenoapps.intercross.data.dao.BaseDao

open class BaseRepository<T>(private val dao: BaseDao<T>) {

    suspend fun insert(vararg items: T) {

        withContext(IO) {

            dao.insert(*items)
        }
    }

    suspend fun update(vararg items: T) {

        withContext(IO) {

            dao.update(*items)
        }
    }

    suspend fun delete(vararg items: T) {

        withContext(IO) {

            dao.delete(*items)
        }
    }
}