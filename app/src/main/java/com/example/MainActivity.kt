package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.GaleriDatabase
import com.example.data.local.PhotoFileStorage
import com.example.data.repository.GaleriRepository
import com.example.ui.navigation.GaleriNavHost
import com.example.ui.theme.GaleriDesaTheme
import com.example.ui.viewmodel.GaleriViewModel
import com.example.ui.viewmodel.GaleriViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = GaleriDatabase.getInstance(applicationContext)
        val fileStorage = PhotoFileStorage(applicationContext)
        val repository = GaleriRepository(database.galeriDao(), fileStorage)
        val factory = GaleriViewModelFactory(repository)

        setContent {
            GaleriDesaTheme {
                val viewModel: GaleriViewModel = viewModel(factory = factory)
                GaleriNavHost(viewModel = viewModel)
            }
        }
    }
}
