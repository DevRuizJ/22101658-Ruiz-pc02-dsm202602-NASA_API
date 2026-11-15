package dev.ruizj.appnasa.apod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ApodRangeTest {
    @Test
    fun filtersToInclusiveSevenDayRangeAndSortsNewestFirst() {
        val start = LocalDate.parse("2026-11-08")
        val end = LocalDate.parse("2026-11-14")
        val items = listOf(
            item("2026-11-14"),
            item("2026-11-07"),
            item("2026-11-08"),
            item("2026-11-10", mediaType = "video"),
            item("2026-11-15"),
        )

        val result = items.filterApodRange(start, end)

        assertEquals(
            listOf("2026-11-14", "2026-11-10", "2026-11-08"),
            result.map(ApodItem::date),
        )
        assertEquals(true, result[1].isVideo)
    }

    @Test
    fun incompleteApodRecordsUseSafeFallbacksAndAreReported() {
        val result = parseApodResponse(
            """
            [
              {
                "date": "2026-11-14",
                "title": null,
                "explanation": null,
                "media_type": "image",
                "hdurl": null,
                "url": "https://example.test/article"
              },
              {
                "date": "2026-11-13",
                "title": "Video APOD",
                "media_type": "video",
                "hdurl": "https://example.test/thumbnail.jpg"
              },
              {"title": "Missing date"}
            ]
            """.trimIndent(),
            LocalDate.parse("2026-11-08"),
            LocalDate.parse("2026-11-14"),
        )

        assertEquals(2, result.items.size)
        assertEquals(3, result.incompleteRecords)
        assertEquals("Sin título", result.items[0].title)
        assertNull(result.items[0].imageUrl)
        assertEquals("Descripción no disponible.", result.items[0].explanation)
        assertEquals(true, result.items[1].isVideo)
        assertNull(result.items[1].imageUrl)
    }

    private fun item(date: String, mediaType: String = "image") = ApodItem(
        date = date,
        title = "APOD $date",
        explanation = "Explanation",
        mediaType = mediaType,
        imageUrl = if (mediaType == "image") "https://example.test/$date.jpg" else null,
        credit = null,
    )
}
