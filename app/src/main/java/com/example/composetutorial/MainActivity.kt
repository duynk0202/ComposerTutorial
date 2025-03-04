package com.example.composetutorial

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import androidx.compose.ui.tooling.preview.Preview
import com.example.composetutorial.model.ProfileState
import com.example.composetutorial.model.ProfileViewModel
import com.example.composetutorial.ui.theme.AppTheme
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import retrofit2.http.GET
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Parcelize
data class Profile(val id: Int, val name: String, val age: Int, val imageUrl: String) : Parcelable


@Composable
fun AppNavigation(viewModel: ProfileViewModel = viewModel()) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "profile_list",
        enterTransition = { fadeIn(animationSpec = tween(500)) },
        exitTransition = { fadeOut(animationSpec = tween(500)) }
    ) {
        composable("profile_list") {
            ProfileList(navController = navController, viewModel = viewModel)
        }
        composable(
            route = "profile_detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val profile = viewModel.getProfileById(id) // Lấy Profile từ ViewModel

            if (profile != null) {
                ProfileDetail(
                    profile = profile,
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProfileList(navController: androidx.navigation.NavController, viewModel: ProfileViewModel) {
    val state = viewModel.profileState.collectAsState().value
    val searchQuery = viewModel.searchQuery.collectAsState().value
    val updatedProfile = navController.currentBackStackEntry?.savedStateHandle?.get<Profile>("updatedProfile")

    LaunchedEffect(updatedProfile) {
        updatedProfile?.let {
            viewModel.updateProfile(it) // Cập nhật danh sách
            navController.currentBackStackEntry?.savedStateHandle?.remove<Profile>("updatedProfile") // Xóa để tránh cập nhật lại lần sau
        }
    }

    Log.d("ProfileList", "State: ${Gson().toJson(state)}")

    AnimatedContent(
        targetState = state,
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) with
                    fadeOut(animationSpec = tween(500)) + slideOutVertically(animationSpec = tween(500))
        }
    ) { targetState ->
        when (targetState) {
            is ProfileState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ProfileState.Success -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "User Profiles",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { viewModel.refreshProfiles() }) {
                            Text("Refresh")
                        }
                    }
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        label = { Text("Search by name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        itemsIndexed(targetState.profiles) { _, profile ->
                            ProfileCard(profile = profile, onClick = {
                                navController.navigate("profile_detail/${profile.id}")
                            })
                        }
                    }
                }
            }
            is ProfileState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = targetState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refreshProfiles() }) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCard(profile: Profile, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable(onClick = onClick)
                .animateContentSize(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = profile.imageUrl,
                    contentDescription = "${profile.name}'s picture",
                    modifier = Modifier
                        .size(50.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Name: ${profile.name}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Age: ${profile.age}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDetail(profile: Profile, navController: androidx.navigation.NavController, viewModel: ProfileViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(profile.name) }
    var editedAge by remember { mutableStateOf(profile.age.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Button(onClick = { showDialog = true }) {
                Text("Edit")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Profile Detail",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        AsyncImage(
            model = profile.imageUrl,
            contentDescription = "${profile.name}'s picture",
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Name: ${profile.name}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Age: ${profile.age}",
            style = MaterialTheme.typography.bodyMedium
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column {
                    TextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = editedAge,
                        onValueChange = { editedAge = it.filter { char -> char.isDigit() } },
                        label = { Text("Age") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ageInt = editedAge.toIntOrNull() ?: profile.age
                        viewModel.updateProfile(Profile(profile.id, editedName, ageInt, profile.imageUrl))
                        showDialog = false
                    }
                ) {
                    Text("Save")
                    val updatedProfile = profile.copy(name = editedName, age = editedAge.toInt())
                    navController.previousBackStackEntry?.savedStateHandle?.set("updatedProfile", updatedProfile)
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}



@Preview(showBackground = true)
@Composable
fun ProfileListPreview() {
    AppTheme {
        ProfileList(navController = rememberNavController(), viewModel = viewModel())
    }
}