package dev.ruizj.appnasa.neows

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class NeoWsDataTest {
    @Test
    fun parsesGroupedFeedFlattensApproachesAndReadsNumericStrings() {
        val result = parseFeed(
            """
            {
              "near_earth_objects": {
                "2026-11-14": [
                  {
                    "id": "42",
                    "name": "Test Asteroid",
                    "is_potentially_hazardous_asteroid": true,
                    "estimated_diameter": {
                      "meters": {
                        "estimated_diameter_min": 12.345,
                        "estimated_diameter_max": 27.891
                      }
                    },
                    "close_approach_data": [{
                      "close_approach_date": "2026-11-14",
                      "relative_velocity": {"kilometers_per_hour": "12345.678"},
                      "miss_distance": {"kilometers": "98765.4321"}
                    }]
                  }
                ],
                "2026-11-13": [
                  {
                    "id": "7",
                    "name": "Older Asteroid",
                    "is_potentially_hazardous_asteroid": false,
                    "estimated_diameter": {
                      "meters": {
                        "estimated_diameter_min": 1.0,
                        "estimated_diameter_max": 2.0
                      }
                    },
                    "close_approach_data": [{
                      "close_approach_date": "2026-11-13",
                      "relative_velocity": {"kilometers_per_hour": "5"},
                      "miss_distance": {"kilometers": "6"}
                    }]
                  }
                ]
              }
            }
            """.trimIndent(),
            LocalDate.parse("2026-11-08"),
            LocalDate.parse("2026-11-14"),
        )

        assertEquals(2, result.items.size)
        assertEquals("2026-11-14", result.items.first().closeApproachDate)
        assertEquals(12345.678, result.items.first().relativeVelocityKph!!, 0.0001)
        assertEquals(98765.4321, result.items.first().missDistanceKm!!, 0.0001)
        assertTrue(result.items.first().isPotentiallyHazardous!!)
        assertFalse(result.items.last().isPotentiallyHazardous!!)
        assertEquals(0, result.incompleteRecords)
    }

    @Test
    fun incompleteFeedEntriesRemainDisplayableAndReportMissingFields() {
        val result = parseFeed(
            """
            {
              "near_earth_objects": {
                "2026-11-14": [
                  {
                    "id": "42",
                    "name": null,
                    "is_potentially_hazardous_asteroid": null,
                    "estimated_diameter": {"meters": {"estimated_diameter_max": "bad"}},
                    "close_approach_data": [{
                      "close_approach_date": "2026-11-14",
                      "relative_velocity": {"kilometers_per_hour": null},
                      "miss_distance": {}
                    }]
                  },
                  {"name": "Missing id"}
                ]
              }
            }
            """.trimIndent(),
            LocalDate.parse("2026-11-08"),
            LocalDate.parse("2026-11-14"),
        )

        assertEquals(1, result.items.size)
        assertEquals(2, result.incompleteRecords)
        assertEquals("Asteroide sin nombre", result.items.single().name)
        assertNull(result.items.single().estimatedDiameterMinMeters)
        assertNull(result.items.single().estimatedDiameterMaxMeters)
        assertNull(result.items.single().relativeVelocityKph)
        assertNull(result.items.single().missDistanceKm)
        assertNull(result.items.single().isPotentiallyHazardous)
    }

    @Test
    fun filtersHazardousObjectsAndRoundsToAtMostTwoDecimals() {
        val asteroids = listOf(
            asteroid("1", "2026-11-13", true),
            asteroid("2", "2026-11-14", false),
            asteroid("3", "2026-11-12", true),
        )

        assertEquals(
            listOf("1", "3"),
            asteroids.filterHazardousOnly(true).map(NearEarthObject::id),
        )
        assertEquals(
            listOf("2", "1", "3"),
            asteroids.sortedByRecentApproach().map(NearEarthObject::id),
        )
        assertEquals("12.35", formatNumber(12.345))
        assertEquals("12", formatNumber(12.0))
        assertEquals("No disponible", formatNumber(null))
    }

    private fun asteroid(id: String, date: String, hazardous: Boolean) = NearEarthObject(
        id = id,
        name = "Asteroid $id",
        closeApproachDate = date,
        estimatedDiameterMinMeters = 1.0,
        estimatedDiameterMaxMeters = 2.0,
        relativeVelocityKph = 3.0,
        missDistanceKm = 4.0,
        isPotentiallyHazardous = hazardous,
    )
}
