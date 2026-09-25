package com.matrimonyapp.ui.auth
import com.matrimonyapp.ui.theme.AppTextPrimary
import com.matrimonyapp.ui.theme.AppPrimary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.core.security.AppLockStore
import com.matrimonyapp.data.discovery.ProfileSearchRepository
import com.matrimonyapp.data.notification.NotificationRepository
import com.matrimonyapp.data.profile.ProfileRepository
import com.matrimonyapp.data.remote.AuthenticatedUser
import com.matrimonyapp.data.repository.AuthRepository
import com.matrimonyapp.data.security.SecurityRepository
import com.matrimonyapp.ui.admin.AdminScreen
import com.matrimonyapp.ui.home.HomeScreen
import com.matrimonyapp.ui.notification.NotificationScreen
import com.matrimonyapp.ui.profile.ProfileScreen
import com.matrimonyapp.ui.search.ProfileDetailScreen
import com.matrimonyapp.ui.search.SearchScreen
import com.matrimonyapp.ui.search.SearchScreenModel
import com.matrimonyapp.ui.security.SecurityScreen
import com.matrimonyapp.ui.theme.CloudGlassBackground
import kotlinx.coroutines.launch

private val NavigationGlass = Color(0xF2FFFFFF)
private val SelectedGlass = AppPrimary
private val SecondaryGlass = Color.Transparent
private val SelectedBorder = AppPrimary
private val NavigationBlack = AppTextPrimary

private enum class AuthenticatedTab {
    HOME,
    SEARCH,
    MATCHES,
    PROFILE
}

@Composable
fun AuthenticatedScreen(
    user: AuthenticatedUser,
    repository: AuthRepository,
    profileRepository: ProfileRepository,
    searchRepository: ProfileSearchRepository,
    securityRepository: SecurityRepository,
    notificationRepository: NotificationRepository,
    appLockStore: AppLockStore,
    adminApi: com.matrimonyapp.data.remote.AdminApi,
    onLoggedOut: (String?) -> Unit,
    onAuthenticationLost: () -> Unit,
    modifier: Modifier = Modifier
) {
    var loading by remember { mutableStateOf(false) }
    var selectedTab by remember {
        mutableStateOf(AuthenticatedTab.HOME)
    }
    var subScreen by remember {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()
    val searchModel = remember {
        SearchScreenModel()
    }

    LaunchedEffect(Unit) {
        notificationRepository.refresh()
        notificationRepository.start()
    }

    DisposableEffect(Unit) {
        onDispose {
            notificationRepository.stop()
        }
    }

    CloudGlassBackground(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            when {
                subScreen?.startsWith("profile:") == true -> {
                    val id = subScreen!!.removePrefix("profile:")
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    ) {
                        ProfileDetailScreen(
                        profileId = id,
                        repository = searchRepository,
                        securityRepository = securityRepository,
                        photoCache = searchModel.photoCache,
                        onBack = { subScreen = null },
                        onUnauthorized = onAuthenticationLost,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                subScreen == "notifications" -> {
                    NotificationScreen(
                        notificationRepository,
                        { subScreen = null },
                        Modifier.padding(bottom = 96.dp)
                    )
                }

                subScreen == "security" -> {
                    SecurityScreen(
                        securityRepository,
                        appLockStore,
                        { subScreen = null },
                        Modifier.padding(bottom = 96.dp)
                    )
                }

                subScreen == "admin" -> {
                    AdminScreen(
                        adminApi,
                        { subScreen = null },
                        Modifier.padding(bottom = 96.dp)
                    )
                }

                else -> {
                    when (selectedTab) {
                        AuthenticatedTab.HOME -> {
                            HomeScreen(
                                user,
                                searchRepository,
                                notificationRepository,
                                onOpenProfile = { id -> subScreen = "profile:$id" },
                                onOpenNotifications = { subScreen = "notifications" },
                                onSignOut = {
                                    if (!loading) {
                                        loading = true
                                        scope.launch {
                                            when (val result = repository.logout()) {
                                                is AppResult.Success -> {
                                                    loading = false
                                                    onLoggedOut(null)
                                                }
                                                is AppResult.Failure -> {
                                                    loading = false
                                                    onLoggedOut(result.error.message)
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.padding(bottom = 96.dp)
                            )
                        }

                        AuthenticatedTab.SEARCH -> {
                            SearchScreen(
                                model = searchModel,
                                repository = searchRepository,
                                securityRepository = securityRepository,
                                onUnauthorized = onAuthenticationLost,
                                modifier = Modifier.padding(bottom = 96.dp)
                            )
                        }

                        AuthenticatedTab.MATCHES -> {
                            PlaceholderTab(title = "Matches")
                        }

                        AuthenticatedTab.PROFILE -> {
                            ProfileScreen(
                                repository = profileRepository,
                                onUnauthorized = onAuthenticationLost,
                                onOpenSecurity = { subScreen = "security" },
                                onOpenAdmin = if (user.role == "ADMIN") { { subScreen = "admin" } } else null,
                                modifier = Modifier.padding(bottom = 96.dp)
                            )
                        }
                    }
                }
            }

            if (
                subScreen == null ||
                subScreen == "notifications" ||
                subScreen == "security" ||
                subScreen == "admin"
            ) {
                GlassBottomNavigation(
                    selected = selectedTab,

                    onSelected = {
                        subScreen = null
                        selectedTab = it
                    },

                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(
                            horizontal = 18.dp,
                            vertical = 10.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun PlaceholderTab(
    title: String
) {
    if (title == "Matches") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(44.dp))
                    .background(Color(0xFFFFE9EF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = null,
                    tint = AppPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "No matches yet",
                color = NavigationBlack,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Start exploring and connect with people who match your preferences.",
                color = Color(0xFF42546A),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = NavigationBlack,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun GlassBottomNavigation(
    selected: AuthenticatedTab,
    onSelected: (AuthenticatedTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(36.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NavigationGlass)
            .border(
                width = 1.dp,
                color = AppTextPrimary.copy(alpha = 0.18f),
                shape = shape
            )
            .padding(6.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        NavigationItem(
            icon = Icons.Outlined.Home,
            label = "Home",
            selected = selected == AuthenticatedTab.HOME,
            onClick = {
                onSelected(AuthenticatedTab.HOME)
            }
        )

        NavigationItem(
            icon = Icons.Outlined.Search,
            label = "Search",
            selected = selected == AuthenticatedTab.SEARCH,
            onClick = {
                onSelected(AuthenticatedTab.SEARCH)
            }
        )

        NavigationItem(
            icon = Icons.Outlined.FavoriteBorder,
            label = "Matches",
            selected = selected == AuthenticatedTab.MATCHES,
            onClick = {
                onSelected(AuthenticatedTab.MATCHES)
            }
        )

        NavigationItem(
            icon = Icons.Outlined.PersonOutline,
            label = "Profile",
            selected = selected == AuthenticatedTab.PROFILE,
            onClick = {
                onSelected(AuthenticatedTab.PROFILE)
            }
        )
    }
}

@Composable
private fun RowScope.NavigationItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(30.dp)

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .background(
                if (selected) {
                    SelectedGlass
                } else {
                    SecondaryGlass
                }
            )
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.dp,
                        color = SelectedBorder,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                onClick = onClick
            )
            .padding(
                vertical = 9.dp,
                horizontal = 3.dp
            ),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.White else NavigationBlack,
            modifier = Modifier.size(25.dp)
        )

        Spacer(
            modifier = Modifier.height(3.dp)
        )

        Text(
            text = label,
            color = if (selected) Color.White else NavigationBlack,
            fontSize = 12.sp,
            fontWeight = if (selected) {
                FontWeight.SemiBold
            } else {
                FontWeight.Medium
            }
        )
    }
}
