package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.GaleriDatabase
import com.example.data.local.PhotoFileStorage
import com.example.data.model.IndonesianMonth
import com.example.data.repository.GaleriRepository
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: GaleriDatabase
    private lateinit var repository: GaleriRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, GaleriDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GaleriRepository(
            dao = database.galeriDao(),
            fileStorage = PhotoFileStorage(context)
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `read app name from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Galeri Desa", appName)
    }

    @Test
    fun `default seven villages and twelve month folders are created`() = runBlocking {
        repository.ensureDefaultVillagesSeeded()

        val villages = repository.villages.first()
        val expectedNames = listOf(
            "Ngabar Selatan",
            "Ngabar Timur",
            "Ngabar Krajan",
            "Sukodani Krajan",
            "Karang Nongko",
            "Sukodani Barat",
            "Sukodani Utara"
        )
        assertEquals(expectedNames, villages.map { it.name })

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val firstVillageDir = File(context.filesDir, "GaleriDesa/ngabar_selatan/$currentYear")
        assertTrue(firstVillageDir.exists())
        IndonesianMonth.entries.forEach { month ->
            assertTrue(
                "Expected folder ${month.folderName} to exist",
                File(firstVillageDir, month.folderName).exists()
            )
        }
    }
}
