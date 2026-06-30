package org.phenoapps.intercross.ui.preview

import android.graphics.Color
import com.google.gson.JsonObject
import org.brapi.v2.model.germ.BrAPICross
import org.brapi.v2.model.germ.BrAPICrossParent
import org.brapi.v2.model.germ.BrAPICrossingProject
import org.brapi.v2.model.germ.BrAPIParentType
import org.brapi.v2.model.germ.BrAPIPlannedCross
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.ui.crosstracker.CrossBlockCell
import org.phenoapps.intercross.ui.crosstracker.CrossBlockMatrix
import org.phenoapps.intercross.ui.crosstracker.CrossDateCount
import org.phenoapps.intercross.ui.crosstracker.CrossPersonCount
import org.phenoapps.intercross.ui.crosstracker.CrossTrackerRow
import org.phenoapps.intercross.ui.crosstracker.WishlistProgressItem
import org.phenoapps.intercross.ui.parents.ParentListRow
import org.phenoapps.intercross.ui.summary.SummaryChartData
import org.phenoapps.intercross.ui.summary.SummaryPoint
import org.phenoapps.intercross.ui.summary.SummarySlice
import org.phenoapps.intercross.ui.wishlist.WishDraft
import org.phenoapps.intercross.ui.wishlist.WishParentOption

/**
 * Centralized sample data for Compose @Preview functions.
 */
object PreviewSampleData {

    // ─── Parents ────────────────────────────────────────────────────────────────

    val femaleParents: List<Parent> = listOf(
        Parent("HC001", 0).apply { name = "Honeycrisp"; id = 1L },
        Parent("GD002", 0).apply { name = "Golden Delicious"; id = 2L },
    )

    val maleParents: List<Parent> = listOf(
        Parent("FJ003", 1).apply { name = "Fuji"; id = 3L },
        Parent("GL004", 1).apply { name = "Gala"; id = 4L },
    )

    val allParents: List<Parent> = femaleParents + maleParents

    // ─── Events ─────────────────────────────────────────────────────────────────

    val events: List<Event> = listOf(
        // BIPARENTAL: mom != dad, dad != "blank"
        Event(
            eventDbId = "EVT001",
            femaleObsUnitDbId = "HC001",
            maleObsUnitDbId = "FJ003",
            readableName = "Cross-1",
            timestamp = "2024-06-15_10_30_00_000",
            person = "Jane",
            experiment = "AppleTrial2024",
        ).apply { id = 1L },
        // BIPARENTAL: different pair
        Event(
            eventDbId = "EVT002",
            femaleObsUnitDbId = "GD002",
            maleObsUnitDbId = "GL004",
            readableName = "Cross-2",
            timestamp = "2024-06-16_14_15_30_500",
            person = "John",
            experiment = "AppleTrial2024",
        ).apply { id = 2L },
        // OPEN: dad == "blank"
        Event(
            eventDbId = "EVT003",
            femaleObsUnitDbId = "HC001",
            maleObsUnitDbId = "blank",
            readableName = "Open-1",
            timestamp = "2024-06-17_09_00_00_000",
            person = "Jane",
            experiment = "AppleTrial2024",
        ).apply { id = 3L },
    )

    // ─── Cross Tracker Rows ─────────────────────────────────────────────────────

    val crossTrackerRows: List<CrossTrackerRow> = listOf(
        // Planned row – incomplete (progress < min)
        CrossTrackerRow.Planned(
            male = "Fuji",
            female = "Honeycrisp",
            count = 2,
            maleId = "FJ003",
            femaleId = "HC001",
            persons = listOf(CrossPersonCount("Jane", 2)),
            dates = listOf(CrossDateCount("2024-06-15", 2)),
            wishes = listOf(WishlistProgressItem("Cross", min = 5, max = 10, progress = 2)),
        ),
        // Unplanned row with count ≥ 1
        CrossTrackerRow.Unplanned(
            male = "Gala",
            female = "Golden Delicious",
            count = 3,
            maleId = "GL004",
            femaleId = "GD002",
            persons = listOf(CrossPersonCount("John", 3)),
            dates = listOf(CrossDateCount("2024-06-16", 3)),
        ),
    )

    val crossTrackerRowsWithComplete: List<CrossTrackerRow> = listOf(
        // Planned row – complete (progress ≥ min)
        CrossTrackerRow.Planned(
            male = "Fuji",
            female = "Honeycrisp",
            count = 7,
            maleId = "FJ003",
            femaleId = "HC001",
            persons = listOf(CrossPersonCount("Jane", 7)),
            dates = listOf(CrossDateCount("2024-06-15", 4), CrossDateCount("2024-06-16", 3)),
            wishes = listOf(WishlistProgressItem("Cross", min = 5, max = 10, progress = 7)),
        ),
        // Planned row – incomplete
        CrossTrackerRow.Planned(
            male = "Gala",
            female = "Golden Delicious",
            count = 1,
            maleId = "GL004",
            femaleId = "GD002",
            persons = listOf(CrossPersonCount("John", 1)),
            dates = listOf(CrossDateCount("2024-06-17", 1)),
            wishes = listOf(WishlistProgressItem("Seed", min = 3, max = 8, progress = 1)),
        ),
    )

    // ─── Parent List Rows ───────────────────────────────────────────────────────

    val parentListRows: List<ParentListRow> = listOf(
        ParentListRow(codeId = "HC001", name = "Honeycrisp", sex = 0, selected = true, crossCount = 4, isGroup = false),
        ParentListRow(codeId = "FJ003", name = "Fuji", sex = 1, selected = false, crossCount = 2, isGroup = false),
        ParentListRow(codeId = "PG001", name = "Spring Pollen Mix", sex = 1, selected = false, crossCount = 5, isGroup = true),
        ParentListRow(codeId = "GD002", name = "Golden Delicious", sex = 0, selected = false, crossCount = 1, isGroup = false),
    )

    // ─── Summary Chart Data ─────────────────────────────────────────────────────

    val populatedChartData: SummaryChartData = SummaryChartData(
        typeSlices = listOf(
            SummarySlice("Biparental", 5f),
            SummarySlice("Open", 2f),
        ),
        metadataBars = listOf(
            SummarySlice("Seeds", 12f),
        ),
        crossesOverTime = listOf(
            SummaryPoint("2024-06-15", 2f),
            SummaryPoint("2024-06-16", 5f),
            SummaryPoint("2024-06-17", 7f),
        ),
    )

    val emptyChartData: SummaryChartData = SummaryChartData(
        typeSlices = emptyList(),
        metadataBars = emptyList(),
        crossesOverTime = emptyList(),
    )

    // ─── Wishlist ───────────────────────────────────────────────────────────────

    internal val wishParentOptions: List<WishParentOption> = listOf(
        WishParentOption(codeId = "HC001", name = "Honeycrisp"),
        WishParentOption(codeId = "FJ003", name = "Fuji"),
    )

    internal val wishDrafts: List<WishDraft> = listOf(
        WishDraft(type = "Cross", min = "5", max = "10"),
        WishDraft(type = "Seed", min = "3", max = "8"),
    )

    val crossBlockMatrix = CrossBlockMatrix(
        maleHeaders = listOf(
            "FJ003" to "Fuji",
            "GL004" to "Gala",
            "GS005" to "Granny Smith",
            "MC006" to "McIntosh",
        ),
        femaleHeaders = listOf(
            "HC001" to "Honeycrisp",
            "GD002" to "Golden Delicious",
            "EM007" to "Empire",
            "AR008" to "Arkansas Black",
            "NJ009" to "Northern Spy",
        ),
        rows = listOf(
            // Honeycrisp x [Fuji, Gala, Granny Smith, McIntosh]
            listOf(
                CrossBlockCell(femaleId = "HC001", maleId = "FJ003", progressColor = 0xFF2E7D32.toInt(), hasWish = true),    // ≥ Max
                CrossBlockCell(femaleId = "HC001", maleId = "GL004", progressColor = 0xFF8BC34A.toInt(), hasWish = true),    // ≥ Min
                CrossBlockCell(femaleId = "HC001", maleId = "GS005", progressColor = 0xAAAAAAAA.toInt(), hasWish = false),   // Empty
                CrossBlockCell(femaleId = "HC001", maleId = "MC006", progressColor = 0xFFFF9800.toInt(), hasWish = false),   // > 33%
            ),
            // Golden Delicious x [...]
            listOf(
                CrossBlockCell(femaleId = "GD002", maleId = "FJ003", progressColor = 0xFFFFEB3B.toInt(), hasWish = true),  // > 66%
                CrossBlockCell(femaleId = "GD002", maleId = "GL004", progressColor = 0xFF2E7D32.toInt(), hasWish = true),  // ≥ Max
                CrossBlockCell(femaleId = "GD002", maleId = "GS005", progressColor = 0xFFF44336.toInt(), hasWish = false), // Started
                CrossBlockCell(femaleId = "GD002", maleId = "MC006", progressColor = 0xAAAAAAAA.toInt(), hasWish = false), // Empty
            ),
            // Empire x [...]
            listOf(
                CrossBlockCell(femaleId = "EM007", maleId = "FJ003", progressColor = 0xFF8BC34A.toInt(), hasWish = true),   // ≥ Min
                CrossBlockCell(femaleId = "EM007", maleId = "GL004", progressColor = 0xFFFF9800.toInt(), hasWish = false),  // > 33%
                CrossBlockCell(femaleId = "EM007", maleId = "GS005", progressColor = 0xFFFFEB3B.toInt(), hasWish = true),   // > 66%
                CrossBlockCell(femaleId = "EM007", maleId = "MC006", progressColor = 0xFFF44336.toInt(), hasWish = false),  // Started
            ),
            // Arkansas Black x [...]
            listOf(
                CrossBlockCell(femaleId = "AR008", maleId = "FJ003", progressColor = 0xAAAAAAAA.toInt(), hasWish = false), // Empty
                CrossBlockCell(femaleId = "AR008", maleId = "GL004", progressColor = 0xFF2E7D32.toInt(), hasWish = true), // ≥ Max
                CrossBlockCell(femaleId = "AR008", maleId = "GS005", progressColor = 0xFF8BC34A.toInt(), hasWish = true), // ≥ Min
                CrossBlockCell(femaleId = "AR008", maleId = "MC006", progressColor = 0xFFFF9800.toInt(), hasWish = false),// > 33%
            ),
            // Northern Spy x [...]
            listOf(
                CrossBlockCell(femaleId = "NJ009", maleId = "FJ003", progressColor = 0xFFF44336.toInt(), hasWish = false), // Started
                CrossBlockCell(femaleId = "NJ009", maleId = "GL004", progressColor = 0xFFFFEB3B.toInt(), hasWish = true), // > 66%
                CrossBlockCell(femaleId = "NJ009", maleId = "GS005", progressColor = 0xAAAAAAAA.toInt(), hasWish = false), // Empty
                CrossBlockCell(femaleId = "NJ009", maleId = "MC006", progressColor = 0xFF2E7D32.toInt(), hasWish = true), // ≥ Max
            ),
        ),
    )

    // ─── BrAPI ──────────────────────────────────────────────────────────────────

    val brapiCrossingProjects: List<BrAPICrossingProject> = listOf(
        BrAPICrossingProject().apply {
            crossingProjectDbId = "PRJ001"
            crossingProjectName = "Apple Breeding 2024"
            crossingProjectDescription = "Developing disease-resistant cultivars"
            programName = "Fruit Tree Improvement"
            commonCropName = "Apple"
            potentialParents = listOf(BrAPICrossParent(), BrAPICrossParent())
        },
        BrAPICrossingProject().apply {
            crossingProjectDbId = "PRJ002"
            crossingProjectName = "Pear Trial"
            crossingProjectDescription = "Heat tolerance assessment"
            programName = "Fruit Tree Improvement"
            commonCropName = "Pear"
            potentialParents = emptyList()
        }
    )

    val brapiPotentialParents: List<BrAPICrossParent> = listOf(
        BrAPICrossParent().apply {
            observationUnitDbId = "HC001"
            observationUnitName = "Honeycrisp"
            parentType = BrAPIParentType.FEMALE
        },
        BrAPICrossParent().apply {
            observationUnitDbId = "FJ003"
            observationUnitName = "Fuji"
            parentType = BrAPIParentType.MALE
        },
        BrAPICrossParent().apply {
            observationUnitDbId = "GD002"
            observationUnitName = "Golden Delicious"
            parentType = BrAPIParentType.FEMALE
        }
    )

    val brapiPlannedCrosses: List<BrAPIPlannedCross> = listOf(
        BrAPIPlannedCross().apply {
            plannedCrossDbId = "PLC001"
            plannedCrossName = "Honeycrisp x Fuji"
            parent1 = BrAPICrossParent().apply {
                observationUnitDbId = "HC001"
                observationUnitName = "Honeycrisp"
                parentType = BrAPIParentType.FEMALE
            }
            parent2 = BrAPICrossParent().apply {
                observationUnitDbId = "FJ003"
                observationUnitName = "Fuji"
                parentType = BrAPIParentType.MALE
            }
            additionalInfo = JsonObject().apply {
                addProperty("wishType", "Seeds")
                addProperty("wishMin", 10)
                addProperty("wishMax", 50)
            }
        }
    )

    val brapiCrosses: List<BrAPICross> = listOf(
        BrAPICross().apply {
            crossDbId = "BC001"
            crossName = "Honeycrisp x Fuji"
            parent1 = BrAPICrossParent().apply {
                observationUnitDbId = "HC001"
                observationUnitName = "Honeycrisp"
                parentType = BrAPIParentType.FEMALE
            }
            parent2 = BrAPICrossParent().apply {
                observationUnitDbId = "FJ003"
                observationUnitName = "Fuji"
                parentType = BrAPIParentType.MALE
            }
        }
    )
}
