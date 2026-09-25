package com.matrimonyapp.ui.profile

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.data.profile.ProfileRepository
import com.matrimonyapp.data.profile.UserProfile
import com.matrimonyapp.data.profile.validateProfile
import com.matrimonyapp.ui.theme.CloudNavy
import com.matrimonyapp.ui.theme.CloudNavySoft
import com.matrimonyapp.ui.theme.appOutlinedTextFieldColors
import com.matrimonyapp.ui.theme.GlassBorder
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset

private val ProfileCard = Color.White
private const val MaxPhotoBytes = 5 * 1024 * 1024

@Composable
fun ProfileScreen(
    repository: ProfileRepository,
    onUnauthorized: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAdmin: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var photoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var photoBusy by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var fieldErrors by remember { mutableStateOf(ProfileFieldErrors()) }
    var pendingPhotoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingPhotoMimeType by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun handleFailure(message: String, code: String?, requiresAuthentication: Boolean) {
        if (requiresAuthentication || code == "UNAUTHORIZED" || code == "AUTHENTICATION_REQUIRED") {
            onUnauthorized()
        } else {
            error = message
        }
    }

    fun loadPhotoIfAvailable(currentProfile: UserProfile?) {
        if (currentProfile?.photoAvailable != true) {
            photoBitmap = null
            return
        }
        scope.launch {
            when (val result = repository.getPhotoBytes()) {
                is AppResult.Success -> {
                    photoBitmap = BitmapFactory.decodeByteArray(result.value, 0, result.value.size)
                }
                is AppResult.Failure -> {
                    if (result.error.requiresAuthentication || result.error.code == "UNAUTHORIZED") {
                        onUnauthorized()
                    }
                }
            }
        }
    }

    fun load() {
        scope.launch {
            loading = true
            error = null
            when (val result = repository.getProfile()) {
                is AppResult.Success -> {
                    profile = result.value
                    loading = false
                    editing = result.value == null
                    loadPhotoIfAvailable(result.value)
                }
                is AppResult.Failure -> {
                    loading = false
                    handleFailure(result.error.message, result.error.code, result.error.requiresAuthentication)
                }
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Box(modifier = modifier.fillMaxSize()) {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CloudNavy)
            }
        } else if (editing) {
            BackHandler(enabled = editing) {
                if (!saving && profile != null) {
                    editing = false
                }
            }
            ProfileEditor(
                initial = profile,
                photoBitmap = photoBitmap,
                photoBusy = photoBusy,
                saving = saving,
                error = error,
                fieldErrors = fieldErrors,
                successMessage = saveMessage,
                onPickPhoto = { uri ->
                    scope.launch {
                        photoBusy = true
                        error = null
                        fieldErrors = ProfileFieldErrors()
                        saveMessage = null
                        val contentResolver = context.contentResolver
                        val bytes = runCatching {
                            contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }.getOrNull()
                        val mimeType = contentResolver.getType(uri) ?: ""
                        if (bytes == null || bytes.isEmpty()) {
                            error = "The selected image could not be read."
                        } else if (bytes.size > MaxPhotoBytes) {
                            error = "Profile photo must be 5 MB or smaller."
                        } else if (mimeType !in setOf("image/jpeg", "image/png")) {
                            error = "Choose a JPEG or PNG image."
                        } else if (profile == null) {
                            pendingPhotoBytes = bytes
                            pendingPhotoMimeType = mimeType
                            photoBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            saveMessage = "Photo selected. Save your profile to upload it."
                        } else {
                            when (val result = repository.uploadPhoto(bytes, mimeType)) {
                                is AppResult.Success -> {
                                    profile = result.value
                                    photoBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    saveMessage = "Profile photo updated."
                                }
                                is AppResult.Failure -> handleFailure(result.error.message, result.error.code, result.error.requiresAuthentication)
                            }
                        }
                        photoBusy = false
                    }
                },
                onDeletePhoto = {
                    scope.launch {
                        photoBusy = true
                        error = null
                        fieldErrors = ProfileFieldErrors()
                        saveMessage = null
                        when (val result = repository.deletePhoto()) {
                            is AppResult.Success -> {
                                profile = result.value
                                photoBitmap = null
                                saveMessage = "Profile photo removed."
                            }
                            is AppResult.Failure -> handleFailure(result.error.message, result.error.code, result.error.requiresAuthentication)
                        }
                        photoBusy = false
                    }
                },
                onCancel = {
                    if (!saving) {
                        if (profile == null) {
                            pendingPhotoBytes = null
                            pendingPhotoMimeType = null
                            photoBitmap = null
                        }
                        if (profile != null) {
                            editing = false
                        }
                        error = null
                        fieldErrors = ProfileFieldErrors()
                        saveMessage = null
                    }
                },
                onSave = { displayName, dateOfBirth, gender, country, stateProvince, city, bio, phoneNumber, profession, employer, salaryRange, education, skills, linkedinUrl, instagramUrl, facebookUrl, websiteUrl, profileVisibility, showPhone, showSalary, showSocial, profileLocked ->
                    val validation = validateProfile(
                        displayName, dateOfBirth, gender, country, stateProvince, city, bio, phoneNumber
                    )
                    val clientAgeError = runCatching {
                        val dob = LocalDate.parse(dateOfBirth.trim())
                        val age = Period.between(dob, LocalDate.now()).years
                        if (age !in 18..100) "Age must be between 18 and 100." else null
                    }.getOrNull()
                    val errors = validation.errors.toMutableList()
                    if (clientAgeError != null && errors.none { it == clientAgeError }) errors += clientAgeError
                    if (errors.isNotEmpty()) {
                        fieldErrors = ProfileFieldErrors.from(errors)
                        error = null
                        saveMessage = null
                        return@ProfileEditor
                    }

                    fieldErrors = ProfileFieldErrors()
                    scope.launch {
                        saving = true
                        error = null
                        saveMessage = null
                        when (val result = repository.updateProfile(
                            displayName, dateOfBirth, gender, country, stateProvince, city, bio, phoneNumber,
                            profession, employer, salaryRange, education, skills, linkedinUrl, instagramUrl, facebookUrl, websiteUrl,
                            profileVisibility, showPhone, showSalary, showSocial, profileLocked
                        )) {
                            is AppResult.Success -> {
                                profile = result.value
                                val photoBytes = pendingPhotoBytes
                                val photoMimeType = pendingPhotoMimeType
                                pendingPhotoBytes = null
                                pendingPhotoMimeType = null

                                if (photoBytes != null && photoMimeType != null) {
                                    photoBusy = true
                                    when (val photoResult = repository.uploadPhoto(photoBytes, photoMimeType)) {
                                        is AppResult.Success -> {
                                            profile = photoResult.value
                                            photoBitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
                                            saveMessage = "Profile saved and photo uploaded."
                                            editing = false
                                            loadPhotoIfAvailable(photoResult.value)
                                        }
                                        is AppResult.Failure -> {
                                            saveMessage = "Profile saved, but the photo could not be uploaded."
                                            handleFailure(photoResult.error.message, photoResult.error.code, photoResult.error.requiresAuthentication)
                                        }
                                    }
                                    photoBusy = false
                                } else {
                                    editing = false
                                    saveMessage = "Profile saved."
                                    loadPhotoIfAvailable(result.value)
                                }
                                saving = false
                            }
                            is AppResult.Failure -> {
                                saving = false
                                handleFailure(result.error.message, result.error.code, result.error.requiresAuthentication)
                            }
                        }
                    }
                }
            )
        } else {
            ProfileDetails(
                profile = profile!!,
                photoBitmap = photoBitmap,
                photoBusy = photoBusy,
                successMessage = saveMessage,
                onOpenSecurity = onOpenSecurity,
                onOpenAdmin = onOpenAdmin,
                onEdit = {
                    error = null
                    saveMessage = null
                    editing = true
                },
                onPickPhoto = { uri ->
                    scope.launch {
                        photoBusy = true
                        error = null
                        fieldErrors = ProfileFieldErrors()
                        saveMessage = null
                        val contentResolver = context.contentResolver
                        val bytes = runCatching {
                            contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }.getOrNull()
                        val mimeType = contentResolver.getType(uri) ?: ""
                        if (bytes == null || bytes.isEmpty()) {
                            error = "The selected image could not be read."
                        } else if (bytes.size > MaxPhotoBytes) {
                            error = "Profile photo must be 5 MB or smaller."
                        } else if (mimeType !in setOf("image/jpeg", "image/png")) {
                            error = "Choose a JPEG or PNG image."
                        } else {
                            when (val result = repository.uploadPhoto(bytes, mimeType ?: "")) {
                                is AppResult.Success -> {
                                    profile = result.value
                                    photoBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    saveMessage = "Profile photo updated."
                                }
                                is AppResult.Failure -> handleFailure(result.error.message, result.error.code, result.error.requiresAuthentication)
                            }
                        }
                        photoBusy = false
                    }
                },
                onDeletePhoto = {
                    scope.launch {
                        photoBusy = true
                        error = null
                        when (val result = repository.deletePhoto()) {
                            is AppResult.Success -> {
                                profile = result.value
                                photoBitmap = null
                                saveMessage = "Profile photo removed."
                            }
                            is AppResult.Failure -> handleFailure(result.error.message, result.error.code, result.error.requiresAuthentication)
                        }
                        photoBusy = false
                    }
                }
            )

            FloatingActionButton(
                onClick = {
                    error = null
                    saveMessage = null
                    editing = true
                },
                containerColor = CloudNavy,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Profile", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileDetails(
    profile: UserProfile,
    photoBitmap: android.graphics.Bitmap?,
    photoBusy: Boolean,
    successMessage: String?,
    onOpenSecurity: () -> Unit,
    onOpenAdmin: (() -> Unit)?,
    onEdit: () -> Unit,
    onPickPhoto: (Uri) -> Unit,
    onDeletePhoto: () -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(onPickPhoto)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 30.dp)
            .padding(bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("My Profile", color = CloudNavy, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                Text("Your introduction to people you may meet.", color = Color.Black, fontSize = 14.sp)
            }
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = CloudNavy, contentColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        GlassCard {
            ProfilePhoto(
                bitmap = photoBitmap,
                busy = photoBusy,
                onPick = { picker.launch("image/*") },
                onDelete = if (profile.photoAvailable) onDeletePhoto else null
            )
            Text(profile.displayName, color = CloudNavy, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            DetailRow(Icons.Outlined.CalendarMonth, "Date of birth", profile.dateOfBirth)
            DetailRow(Icons.Outlined.PersonOutline, "Gender", profile.gender.lowercase().replaceFirstChar { it.uppercase() })
            DetailRow(Icons.Outlined.LocationOn, "Country", profile.country ?: "Not set")
            DetailRow(Icons.Outlined.LocationOn, "State / Province", profile.stateProvince ?: "Not set")
            DetailRow(Icons.Outlined.LocationOn, "City", profile.city)
            DetailRow(Icons.Outlined.Phone, "Private phone", profile.phoneNumber ?: "Not shared")
            if (profile.bio.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(profile.bio, color = Color.Black, fontSize = 16.sp)
            }

            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CloudNavy, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit Profile Details", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (!profile.profession.isNullOrBlank()) DetailRow(Icons.Outlined.Work, "Profession", profile.profession)
        if (!profile.education.isNullOrBlank()) DetailRow(Icons.Outlined.School, "Education", profile.education)
        if (!profile.skills.isNullOrBlank()) DetailRow(Icons.Outlined.Build, "Skills", profile.skills)
        if (!profile.salaryRange.isNullOrBlank() && profile.showSalary) DetailRow(Icons.Outlined.Payments, "Salary range", profile.salaryRange)
        if (profile.verificationStatus == "VERIFIED") {
            Text("✓ Verified profile", color = Color(0xFF1E66D0), fontWeight = FontWeight.SemiBold)
        }
        if (profile.backgroundCheckStatus != "UNREQUESTED") {
            Text("Background check: ${profile.backgroundCheckStatus}", color = Color.Black, fontSize = 13.sp)
        }
        successMessage?.let { Text(it, color = CloudNavy, fontSize = 14.sp, fontWeight = FontWeight.Medium) }

        Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CloudNavy, contentColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Edit profile", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onOpenSecurity, modifier = Modifier.fillMaxWidth()) {
            Text("Security & privacy", color = CloudNavy, fontSize = 17.sp)
        }
        onOpenAdmin?.let {
            TextButton(onClick = it, modifier = Modifier.fillMaxWidth()) {
                Text("Admin dashboard", color = CloudNavy, fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun ProfilePhoto(
    bitmap: android.graphics.Bitmap?,
    busy: Boolean,
    onPick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(Color(0xFFE7F2F7))
                .border(2.dp, GlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = CloudNavySoft, modifier = Modifier.size(48.dp))
            }
            if (busy) CircularProgressIndicator(color = CloudNavy, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Profile photo", color = CloudNavy, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("JPEG or PNG, up to 5 MB", color = Color.Black, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onPick, enabled = !busy) {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = CloudNavy)
                    Spacer(Modifier.width(5.dp))
                    Text(if (bitmap == null) "Add" else "Change", color = CloudNavy)
                }
                onDelete?.let {
                    IconButton(onClick = it, enabled = !busy) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove photo", tint = CloudNavy)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEditor(
    initial: UserProfile?,
    photoBitmap: android.graphics.Bitmap?,
    photoBusy: Boolean,
    saving: Boolean,
    error: String?,
    fieldErrors: ProfileFieldErrors,
    successMessage: String?,
    onPickPhoto: (Uri) -> Unit,
    onDeletePhoto: () -> Unit,
    onCancel: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var displayName by remember(initial) { mutableStateOf(initial?.displayName.orEmpty()) }
    var dateOfBirth by remember(initial) { mutableStateOf(initial?.dateOfBirth.orEmpty()) }
    var gender by remember(initial) { mutableStateOf(initial?.gender.orEmpty()) }
    var country by remember(initial) { mutableStateOf(initial?.country.orEmpty()) }
    var stateProvince by remember(initial) { mutableStateOf(initial?.stateProvince.orEmpty()) }
    var city by remember(initial) { mutableStateOf(initial?.city.orEmpty()) }
    var bio by remember(initial) { mutableStateOf(initial?.bio.orEmpty()) }
    var phoneNumber by remember(initial) { mutableStateOf(initial?.phoneNumber.orEmpty()) }
    var profession by remember(initial) { mutableStateOf(initial?.profession.orEmpty()) }
    var employer by remember(initial) { mutableStateOf(initial?.employer.orEmpty()) }
    var salaryRange by remember(initial) { mutableStateOf(initial?.salaryRange.orEmpty()) }
    var education by remember(initial) { mutableStateOf(initial?.education.orEmpty()) }
    var skills by remember(initial) { mutableStateOf(initial?.skills.orEmpty()) }
    var linkedinUrl by remember(initial) { mutableStateOf(initial?.linkedinUrl.orEmpty()) }
    var instagramUrl by remember(initial) { mutableStateOf(initial?.instagramUrl.orEmpty()) }
    var facebookUrl by remember(initial) { mutableStateOf(initial?.facebookUrl.orEmpty()) }
    var websiteUrl by remember(initial) { mutableStateOf(initial?.websiteUrl.orEmpty()) }
    var profileVisibility by remember(initial) { mutableStateOf(initial?.profileVisibility ?: "PUBLIC") }
    var showPhone by remember(initial) { mutableStateOf(initial?.showPhone ?: false) }
    var showSalary by remember(initial) { mutableStateOf(initial?.showSalary ?: false) }
    var showSocial by remember(initial) { mutableStateOf(initial?.showSocial ?: true) }
    var profileLocked by remember(initial) { mutableStateOf(initial?.profileLocked ?: false) }
    var genderMenuExpanded by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(onPickPhoto)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 30.dp)
            .padding(bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(if (initial == null) "Create your profile" else "Edit profile", color = CloudNavy, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Text("Add the details you want to share. Your phone stays private.", color = Color.Black, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancel, enabled = !saving) { Text("Cancel", color = Color.Black) }
                Button(
                    onClick = {
                        onSave(displayName,dateOfBirth,gender,country,stateProvince,city,bio,phoneNumber,profession,employer,salaryRange,education,skills,linkedinUrl,instagramUrl,facebookUrl,websiteUrl,profileVisibility,showPhone,showSalary,showSocial,profileLocked)
                    },
                    enabled = !saving,
                    colors = ButtonDefaults.buttonColors(containerColor = CloudNavy, contentColor = Color.White)
                ) {
                    if (saving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        GlassCard {
            ProfilePhoto(
                bitmap = photoBitmap,
                busy = photoBusy,
                onPick = { picker.launch("image/*") },
                onDelete = if (initial?.photoAvailable == true) onDeletePhoto else null
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display name") },
                singleLine = true,
                isError = fieldErrors.displayName != null,
                supportingText = fieldErrors.displayName?.let { { Text(it) } },
                leadingIcon = { Icon(Icons.Outlined.PersonOutline, null) },
                colors = appOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            DateOfBirthField(value = dateOfBirth, onValueChange = { dateOfBirth = it }, error = fieldErrors.dateOfBirth)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    isError = fieldErrors.gender != null,
                    supportingText = fieldErrors.gender?.let { { Text(it) } },
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { genderMenuExpanded = true },
                    trailingIcon = { TextButton(onClick = { genderMenuExpanded = true }) { Text("Choose", color = CloudNavy) } }
                )
                DropdownMenu(expanded = genderMenuExpanded, onDismissRequest = { genderMenuExpanded = false }) {
                    listOf("MALE", "FEMALE", "OTHER").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.lowercase().replaceFirstChar { it.uppercase() }, color = CloudNavy) },
                            onClick = { gender = option; genderMenuExpanded = false }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                label = { Text("Country") },
                singleLine = true,
                isError = fieldErrors.country != null,
                supportingText = fieldErrors.country?.let { { Text(it) } },
                leadingIcon = { Icon(Icons.Outlined.LocationOn, null) },
                colors = appOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = stateProvince,
                onValueChange = { stateProvince = it },
                label = { Text("State / Province") },
                singleLine = true,
                isError = fieldErrors.stateProvince != null,
                supportingText = fieldErrors.stateProvince?.let { { Text(it) } },
                colors = appOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City") },
                singleLine = true,
                isError = fieldErrors.city != null,
                supportingText = fieldErrors.city?.let { { Text(it) } },
                leadingIcon = { Icon(Icons.Outlined.LocationOn, null) },
                colors = appOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 500) bio = it },
                label = { Text("About Me / Bio") },
                placeholder = { Text("Describe yourself, hobbies, values, and what you are looking for...") },
                minLines = 3,
                maxLines = 6,
                isError = fieldErrors.bio != null,
                supportingText = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(fieldErrors.bio ?: "")
                        Text("${bio.length}/500")
                    }
                },
                colors = appOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { if (it.length <= 24) phoneNumber = it },
                label = { Text("Private phone (optional)") },
                singleLine = true,
                isError = fieldErrors.phoneNumber != null,
                supportingText = fieldErrors.phoneNumber?.let { { Text(it) } },
                leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                colors = appOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(value=profession,onValueChange={if(it.length<=120)profession=it},label={Text("Profession")},singleLine=true,colors=appOutlinedTextFieldColors(),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=employer,onValueChange={if(it.length<=160)employer=it},label={Text("Employer / work")},singleLine=true,colors=appOutlinedTextFieldColors(),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=salaryRange,onValueChange={if(it.length<=80)salaryRange=it},label={Text("Salary range (optional)")},singleLine=true,colors=appOutlinedTextFieldColors(),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=education,onValueChange={if(it.length<=180)education=it},label={Text("Education")},singleLine=true,colors=appOutlinedTextFieldColors(),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=skills,onValueChange={if(it.length<=500)skills=it},label={Text("Skills")},singleLine=false,colors=appOutlinedTextFieldColors(),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=linkedinUrl,onValueChange={linkedinUrl=it},label={Text("LinkedIn HTTPS URL")},singleLine=true,colors=appOutlinedTextFieldColors(),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=instagramUrl,onValueChange={instagramUrl=it},label={Text("Instagram HTTPS URL")},singleLine=true,colors=appOutlinedTextFieldColors(),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=facebookUrl,onValueChange={facebookUrl=it},label={Text("Facebook HTTPS URL")},singleLine=true,colors=appOutlinedTextFieldColors(),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=websiteUrl,onValueChange={websiteUrl=it},label={Text("Website HTTPS URL")},singleLine=true,colors=appOutlinedTextFieldColors(),modifier=Modifier.fillMaxWidth())
            Text("Profile visibility: $profileVisibility",color=Color.Black)
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                listOf("PUBLIC","PRIVATE","LOCKED").forEach{TextButton(onClick={profileVisibility=it}){Text(it,color=CloudNavy)}}
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Show phone",color=Color.Black);Switch(showPhone,{showPhone=it})}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Show salary",color=Color.Black);Switch(showSalary,{showSalary=it})}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Show social links",color=Color.Black);Switch(showSocial,{showSocial=it})}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Lock profile",color=Color.Black);Switch(profileLocked,{profileLocked=it})}
            error?.let { Text(it, color = Color(0xFFB3261E), fontSize = 13.sp) }
            successMessage?.let { Text(it, color = CloudNavy, fontSize = 13.sp) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel, enabled = !saving) { Text("Cancel", color = Color.Black) }
                Button(
                    onClick = { onSave(displayName,dateOfBirth,gender,country,stateProvince,city,bio,phoneNumber,profession,employer,salaryRange,education,skills,linkedinUrl,instagramUrl,facebookUrl,websiteUrl,profileVisibility,showPhone,showSalary,showSocial,profileLocked) },
                    enabled = !saving,
                    colors = ButtonDefaults.buttonColors(containerColor = CloudNavy, contentColor = Color.White)
                ) {
                    if (saving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateOfBirthField(value: String, onValueChange: (String) -> Unit, error: String? = null) {
    var open by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val minimumDate = remember(today) { today.minusYears(100) }
    val maximumDate = remember(today) { today.minusYears(18) }
    val validYearRange = remember(today) { minimumDate.year..maximumDate.year }
    val initialMillis = remember(value) {
        runCatching { LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
    }
    val selectableDates = remember(minimumDate, maximumDate) {
        object : SelectableDates {
            override fun isSelectableYear(year: Int) = year in validYearRange
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return date in minimumDate..maximumDate
            }
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = validYearRange,
        selectableDates = selectableDates
    )
    val pickerColors = DatePickerDefaults.colors().copy(
        containerColor = Color(0xFFF8FAFF),
        titleContentColor = CloudNavy,
        headlineContentColor = CloudNavy,
        weekdayContentColor = CloudNavySoft,
        navigationContentColor = CloudNavy,
        yearContentColor = CloudNavy,
        disabledYearContentColor = CloudNavy,
        currentYearContentColor = CloudNavy,
        selectedYearContentColor = Color.White,
        selectedYearContainerColor = CloudNavy,
        dayContentColor = CloudNavy,
        disabledDayContentColor = CloudNavy,
        selectedDayContentColor = Color.White,
        selectedDayContainerColor = CloudNavy,
        todayContentColor = CloudNavy,
        todayDateBorderColor = CloudNavy,
        dividerColor = CloudNavySoft.copy(alpha = 0.18f)
    )

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text("Date of birth") },
        placeholder = { Text("YYYY-MM-DD") },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null, tint = CloudNavy) },
        colors = appOutlinedTextFieldColors(),
         trailingIcon = { IconButton(onClick = { open = true }) { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choose date") } },
        modifier = Modifier.fillMaxWidth()
    )

    if (open) {
        DatePickerDialog(
            onDismissRequest = { open = false },
            colors = pickerColors,
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onValueChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    open = false
                }) { Text("Select", color = CloudNavy, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel", color = CloudNavy) } }
        ) {
            DatePicker(state = state, colors = pickerColors, showModeToggle = true)
        }
    }
}

private data class ProfileFieldErrors(
    val displayName: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val country: String? = null,
    val stateProvince: String? = null,
    val city: String? = null,
    val bio: String? = null,
    val phoneNumber: String? = null
) {
    companion object {
        fun from(errors: List<String>): ProfileFieldErrors = ProfileFieldErrors(
            displayName = errors.firstOrNull { it.startsWith("Display name ") },
            dateOfBirth = errors.firstOrNull { it.startsWith("Date of birth ") || it.startsWith("Age must ") },
            gender = errors.firstOrNull { it.startsWith("Select a valid gender") },
            country = errors.firstOrNull { it.startsWith("Country ") },
            stateProvince = errors.firstOrNull { it.startsWith("State or province ") },
            city = errors.firstOrNull { it.startsWith("City ") },
            bio = errors.firstOrNull { it.startsWith("Bio ") },
            phoneNumber = errors.firstOrNull { it.startsWith("Phone number ") }
        )
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ProfileCard)
            .border(1.dp, GlassBorder, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = CloudNavySoft)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = Color.Black, fontSize = 12.sp)
            Text(value, color = CloudNavy, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
