package org.phenoapps.intercross.interfaces

/**
 * This interface is used to manage json metadata between an adapter and fragment.
 * Adapters listen for value updates, new metadata properties, and property deletions.
 * See EventDetailFragment and MetadataAdapter as reference.
 */
interface MetadataManager {

    fun onMetadataCreated(property: String, value: String?) = Unit

    fun onMetadataUpdated(property: String, value: Int?) = Unit

    fun onMetadataDefaultUpdated(rowid: Long, property: String, value: Int) = Unit

    fun onMetadataLongClicked(rowid: Long, property: String) = Unit
}