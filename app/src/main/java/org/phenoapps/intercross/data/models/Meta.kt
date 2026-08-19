package org.phenoapps.intercross.data.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Metadata table to hold default values, unique property names.
 */
@Keep
@Entity(tableName = "metadata",
        indices = [Index(value = ["property"], unique = true)])
data class Meta(

        @ColumnInfo(name = "property")
        var property: String,

        @ColumnInfo(name = "defaultValue")
        var defaultValue: Int? = null,

        @ColumnInfo(name = "icon")
        var icon: String? = null,

        @ColumnInfo(name = "mid")
        @PrimaryKey(autoGenerate = true)
        var id: Long? = null): BaseTable()