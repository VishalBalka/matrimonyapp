package com.matrimonyapp.ui.search
import com.matrimonyapp.ui.theme.AppFieldBorder
import com.matrimonyapp.ui.theme.AppTextPrimary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.data.discovery.DiscoveryProfile
import com.matrimonyapp.data.discovery.ProfileSearchFilters
import com.matrimonyapp.data.discovery.ProfileSearchRepository
import com.matrimonyapp.data.discovery.SearchFilterErrors
import com.matrimonyapp.data.discovery.SearchFilterValidation
import com.matrimonyapp.data.discovery.SearchPhase
import com.matrimonyapp.data.discovery.isAuthenticationFailure
import com.matrimonyapp.data.discovery.validateSearchFilters
import com.matrimonyapp.data.security.SecurityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ScreenBackground = Color.Transparent
private val CardBackground = Color.White
private val TextBlack = AppTextPrimary
private val BorderBlack = Color.Black
private val PrimaryWine = Color(0xFF9E1B4D)
private val ErrorRed = Color(0xFFB3261E)

@Composable
fun SearchScreen(
    model: SearchScreenModel,
    repository: ProfileSearchRepository,
    securityRepository: SecurityRepository,
    onUnauthorized: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedId = model.selectedProfileId

    if (selectedId != null) {
        BackHandler {
            model.selectedProfileId = null
        }

        ProfileDetailScreen(
            profileId = selectedId,
            repository = repository,
            securityRepository = securityRepository,
            photoCache = model.photoCache,
            onBack = {
                model.selectedProfileId = null
            },
            onUnauthorized = onUnauthorized,
            modifier = modifier
        )
    } else {
        SearchContent(
            model = model,
            repository = repository,
            securityRepository = securityRepository,
            onUnauthorized = onUnauthorized,
            modifier = modifier
        )
    }
}

@Composable
private fun SearchContent(
    model: SearchScreenModel,
    repository: ProfileSearchRepository,
    securityRepository: SecurityRepository,
    onUnauthorized: () -> Unit,
    modifier: Modifier
) {
    val scope = rememberCoroutineScope()

    fun startSearch(filters: ProfileSearchFilters) {
        model.search = model.search.beginSearch(filters)
        val requestId = model.search.requestId

        scope.launch {
            val result = repository.search(
                filters = filters,
                page = 0
            )

            if (result is AppResult.Failure &&
                result.error.isAuthenticationFailure()
            ) {
                onUnauthorized()
            } else {
                model.search =
                    model.search.applyFirstPage(
                        requestId,
                        result
                    )
            }
        }
    }

    fun submit() {
        when (
            val validation = validateSearchFilters(
                country = model.country,
                state = model.state,
                city = model.city,
                minAge = model.minAge,
                maxAge = model.maxAge
            )
        ) {
            is SearchFilterValidation.Invalid -> {
                model.fieldErrors = validation.errors
            }

            is SearchFilterValidation.Valid -> {
                model.fieldErrors = SearchFilterErrors()
                startSearch(validation.filters)
            }
        }
    }

    fun loadMore() {
        if (!model.search.canLoadMore) return

        val filters = model.search.filters
        val page = model.search.nextPage

        model.search = model.search.beginLoadMore()
        val requestId = model.search.requestId

        scope.launch {
            val result = repository.search(
                filters = filters,
                page = page
            )

            if (result is AppResult.Failure &&
                result.error.isAuthenticationFailure()
            ) {
                onUnauthorized()
            } else {
                model.search =
                    model.search.applyNextPage(
                        requestId,
                        result
                    )
            }
        }
    }

    val state = model.search

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Discover",
                    color = TextBlack,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Find people by location and age.",
                    color = TextBlack,
                    fontSize = 15.sp
                )
            }
        }

        item {
            FilterCard(
                model = model,
                searching = state.phase == SearchPhase.LOADING,
                onSearch = {
                    submit()
                },
                onClear = {
                    model.clearFilters()
                }
            )
        }

        when (state.phase) {

            SearchPhase.INITIAL -> {
                item {
                    MessageCard(
                        title = "Start your search",
                        body = "Set any filters you like and tap Search. Leave them empty to browse everyone."
                    )
                }
            }

            SearchPhase.LOADING -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryWine
                        )
                    }
                }
            }

            SearchPhase.EMPTY -> {
                item {
                    MessageCard(
                        title = "No profiles found",
                        body = "Nothing matches these filters. Try widening the age range or removing a location."
                    )
                }
            }

            SearchPhase.ERROR -> {
                item {
                    ErrorCard(
                        message = state.errorMessage
                            ?: "The search could not be completed.",
                        onRetry = {
                            startSearch(state.filters)
                        }
                    )
                }
            }

            SearchPhase.RESULTS -> {
                item {
                    Text(
                        text = if (state.totalItems == 1L) {
                            "1 profile found"
                        } else {
                            "${state.totalItems} profiles found"
                        },
                        color = TextBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                items(
                    state.items,
                    key = { it.profileId }
                ) { profile ->

                    ProfileResultCard(
                        profile = profile,
                        model = model,
                        repository = repository,
                        onUnauthorized = onUnauthorized
                    )
                }

                item {
                    LoadMoreFooter(
                        loading = state.loadingMore,
                        error = state.loadMoreError,
                        hasMore = state.hasMore,
                        onLoadMore = {
                            loadMore()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterCard(
    model: SearchScreenModel,
    searching: Boolean,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    val errors = model.fieldErrors

    GlassCard {
        SearchField(
            value = model.country,
            label = "Country",
            onValueChange = {
                if (it.length <= 100) {
                    model.country = it
                }
            },
            isError = errors.country != null
        )

        SearchField(
            value = model.state,
            label = "State / Province",
            onValueChange = {
                if (it.length <= 100) {
                    model.state = it
                }
            },
            isError = errors.state != null
        )

        SearchField(
            value = model.city,
            label = "City",
            onValueChange = {
                if (it.length <= 120) {
                    model.city = it
                }
            },
            isError = errors.city != null
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SearchField(
                value = model.minAge,
                label = "Min age",
                onValueChange = {
                    if (
                        it.length <= 3 &&
                        it.all { c -> c in '0'..'9' }
                    ) {
                        model.minAge = it
                    }
                },
                keyboardType = KeyboardType.Number,
                isError = errors.minAge != null,
                modifier = Modifier.weight(1f)
            )

            SearchField(
                value = model.maxAge,
                label = "Max age",
                onValueChange = {
                    if (
                        it.length <= 3 &&
                        it.all { c -> c in '0'..'9' }
                    ) {
                        model.maxAge = it
                    }
                },
                keyboardType = KeyboardType.Number,
                isError = errors.maxAge != null,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onClear,
                enabled = !searching
            ) {
                Text(
                    "Clear filters",
                    color = TextBlack
                )
            }

            Button(
                onClick = onSearch,
                enabled = !searching,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryWine,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Color.White
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    "Search",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                label,
                color = if (isError) ErrorRed else TextBlack
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextBlack,
            unfocusedTextColor = TextBlack,
            disabledTextColor = TextBlack,

            focusedLabelColor = TextBlack,
            unfocusedLabelColor = TextBlack,
            disabledLabelColor = TextBlack,

            cursorColor = TextBlack,

            focusedBorderColor = BorderBlack,
            unfocusedBorderColor = BorderBlack,
            disabledBorderColor = BorderBlack,

            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground,
            disabledContainerColor = CardBackground,

            errorTextColor = ErrorRed,
            errorLabelColor = ErrorRed,
            errorBorderColor = ErrorRed,
            errorContainerColor = CardBackground
        )
    )
}

@Composable
private fun ProfileResultCard(
    profile: DiscoveryProfile,
    model: SearchScreenModel,
    repository: ProfileSearchRepository,
    onUnauthorized: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBackground)
            .border(
                1.dp,
                BorderBlack,
                shape
            )
            .clickable {
                model.selectedProfileId = profile.profileId
            }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfilePhotoCircle(
            profileId = profile.profileId,
            photoAvailable = profile.photoAvailable,
            size = 72.dp,
            cache = model.photoCache,
            repository = repository,
            onUnauthorized = onUnauthorized
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                profile.displayName,
                color = TextBlack,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                "${profile.age} years",
                color = TextBlack,
                fontSize = 14.sp
            )

            val place = listOfNotNull(
                profile.city,
                profile.stateProvince,
                profile.country
            ).joinToString(", ")

            if (place.isNotEmpty()) {
                Text(
                    place,
                    color = TextBlack,
                    fontSize = 14.sp
                )
            }

            profile.profession
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    Text(
                        it,
                        color = TextBlack,
                        fontSize = 14.sp
                    )
                }
        }
    }
}

@Composable
internal fun ProfilePhotoCircle(
    profileId: String,
    photoAvailable: Boolean,
    size: Dp,
    cache: PhotoCache,
    repository: ProfileSearchRepository,
    onUnauthorized: () -> Unit
) {
    var bitmap by remember(profileId) {
        mutableStateOf(cache.get(profileId))
    }

    LaunchedEffect(profileId, photoAvailable) {
        if (photoAvailable && bitmap == null) {
            when (
                val result = repository.getPhotoBytes(profileId)
            ) {
                is AppResult.Success -> {
                    val decoded = withContext(Dispatchers.Default) {
                        decodeSampledBitmap(result.value)
                    }

                    if (decoded != null) {
                        cache.put(profileId, decoded)
                        bitmap = decoded
                    }
                }

                is AppResult.Failure -> {
                    if (result.error.isAuthenticationFailure()) {
                        onUnauthorized()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFFE7F2F7))
            .border(
                2.dp,
                BorderBlack,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        val current = bitmap

        if (current != null) {
            Image(
                bitmap = current.asImageBitmap(),
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Outlined.PersonOutline,
                contentDescription = null,
                tint = TextBlack,
                modifier = Modifier.size(size / 2)
            )
        }
    }
}

@Composable
private fun LoadMoreFooter(
    loading: Boolean,
    error: String?,
    hasMore: Boolean,
    onLoadMore: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            loading -> {
                CircularProgressIndicator(
                    color = PrimaryWine,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp
                )
            }

            error != null -> {
                Text(
                    error,
                    color = ErrorRed,
                    fontSize = 13.sp
                )

                TextButton(
                    onClick = onLoadMore
                ) {
                    Text(
                        "Retry",
                        color = TextBlack
                    )
                }
            }

            hasMore -> {
                TextButton(
                    onClick = onLoadMore
                ) {
                    Text(
                        "Load more",
                        color = TextBlack
                    )
                }
            }

            else -> {
                Text(
                    "You have reached the end.",
                    color = TextBlack,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    body: String
) {
    GlassCard {
        Text(
            title,
            color = TextBlack,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            body,
            color = TextBlack,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    GlassCard {
        Text(
            "Search failed",
            color = TextBlack,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            message,
            color = ErrorRed,
            fontSize = 14.sp
        )

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryWine,
                contentColor = Color.White
            )
        ) {
            Text(
                "Retry",
                color = Color.White
            )
        }
    }
}

@Composable
internal fun GlassCard(
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(28.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBackground)
            .border(
                1.dp,
                BorderBlack,
                shape
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}
