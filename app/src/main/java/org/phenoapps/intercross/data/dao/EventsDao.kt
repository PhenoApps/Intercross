package org.phenoapps.intercross.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.phenoapps.intercross.data.models.Event

@Dao
interface EventsDao : BaseDao<Event> {

    data class CrossMetadata(val eid: Int, val property: String, val value: Int?)

    data class CrossMetadataWithDefaults(val eid: Int, val property: String, val value: Int?, val defaultValue: Int)

    data class ParentCount(val mom: String, val momReadable: String,
                           val dad: String, val dadReadable: String,
                           val person: String, val date: String, val count: Int)

    data class ParentData(val momCode: String, val momReadableName: String,
                          val dadCode: String, val dadReadableName: String)

    @Query("SELECT * FROM events WHERE events.eid == :eid")
    suspend fun getEvent(eid: Long?): Event

    @Query("SELECT * FROM events ORDER BY date DESC")
    fun selectAll(): LiveData<List<Event>>

    @Query("""
        SELECT DISTINCT x.mom, female.name as "momReadable", x.dad, male.name as "dadReadable", MAX(x.person) as "person", MAX(x.date) as "date",
            (SELECT COUNT(*)
            FROM events as y
            WHERE y.mom = x.mom and y.dad = x.dad) as count
        FROM events as x, parents as male, parents as female
        WHERE x.dad = male.codeId and x.mom = female.codeId
        GROUP BY x.mom, "momReadable", x.dad, "dadReadable"
    """)
    fun getParentCount(): LiveData<List<ParentCount>>

    @Query("""
    SELECT DISTINCT x.mom, female.name as "momReadable", x.dad, male.name as "dadReadable", 
        x.person as "person", x.date as "date",
        COUNT(*) as count
    FROM events as x, parents as male, parents as female
    WHERE x.dad = male.codeId and x.mom = female.codeId
    GROUP BY x.mom, "momReadable", x.dad, "dadReadable", x.person, x.date
""")
    fun getAllParents(): LiveData<List<ParentCount>>

    @Query("""
        SELECT m.codeId as momCode, m.name as momReadableName, d.codeId as dadCode, d.name as dadReadableName
        FROM parents as m, parents as d, events as e
        WHERE e.eid = :eid and e.mom = m.codeId and e.dad = d.codeId
    """)
    fun getParents(eid: Long): LiveData<ParentData>

    @Query("""SELECT DISTINCT E.eid, M.property, V.value 
                    FROM events as E, metaValues as V, metadata as M 
                    JOIN events ON E.eid = V.eid 
                    JOIN metadata ON M.mid = V.metaId""")
    fun getMetadata(): LiveData<List<CrossMetadata>>

    @Query("""SELECT DISTINCT E.eid, M.property, V.value, M.defaultValue
                    FROM events as E, metaValues as V, metadata as M 
                    JOIN events ON E.eid = V.eid 
                    JOIN metadata ON M.mid = V.metaId
                    WHERE E.eid = :eid""")
    fun getMetadata(eid: Long): LiveData<List<CrossMetadataWithDefaults>>

    @Query("""SELECT m.*, d.*
                    FROM events as m, events as d, events as x
                    WHERE d.codeId = x.dad and m.codeId = x.mom AND x.eid = :eid""")
    suspend fun selectEventParents(eid: Long): List<Event>

    @Query("SELECT eid FROM events WHERE codeId = :code AND mom = :mom AND dad = :dad AND date = :date")
    fun getRowid(code: String, mom: String, dad: String, date: String): Long

    @Query("SELECT * FROM events WHERE eid = :eid LIMIT 1")
    suspend fun selectById(eid: Long): Event

    @Query("DELETE FROM events WHERE events.eid = :eid")
    suspend fun deleteById(eid: Long)

    @Query("SELECT DISTINCT x.codeId FROM events as x WHERE x.codeId = :code")
    fun getEventsWithCode(code: String): List<String>

    @Query("SELECT DISTINCT * FROM events WHERE :dbId=codeId")
    fun findCross(dbId: String): LiveData<Event>

    @Query("SELECT * FROM events")
    fun selectAllLive(): LiveData<List<Event>>

    @Query("DELETE FROM events WHERE :dbId = codeId")
    fun delete(dbId: String): Int

    @Query("DELETE FROM events")
    fun deleteAll(): Int

    @Query("SELECT DISTINCT * FROM events")
    fun getCrosses(): List<Event>

    @Transaction
    @Query("DELETE FROM events")
    fun drop()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertEvent(event: Event): Long
}