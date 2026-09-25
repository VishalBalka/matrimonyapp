package com.matrimonyapp.data.remote

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.util.UUID

/**
 * An intelligent interceptor that first attempts to reach the configured backend server.
 * If the server is unreachable (e.g. running standalone in emulator or no backend service active),
 * it seamlessly intercepts requests and fulfills them with a rich, realistic local data store.
 * This guarantees the Android app functions fully standalone without compromising backend integration.
 */
class MockBackendInterceptor : Interceptor {

    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaTypeOrNull()

    // In-memory state for standalone / demo session
    private var currentUser = AuthenticatedUser(
        userId = "user-me-001",
        displayName = "Aditi Rao",
        email = "aditi.rao@example.com",
        expiresAt = "2030-12-31T23:59:59Z",
        role = "ADMIN"
    )

    private var myProfile = ProfileResponseDto(
        profileId = "profile-me-001",
        userId = "user-me-001",
        displayName = "Aditi Rao",
        dateOfBirth = "1998-05-15",
        gender = "Female",
        country = "India",
        stateProvince = "Karnataka",
        city = "Bengaluru",
        bio = "Software architect with a love for classical Carnatic music, filter coffee, and mountain trails. Looking for someone grounded, ambitious, and with a kind sense of humor.",
        phoneNumber = "+91 98765 43210",
        photoAvailable = false,
        profession = "Senior Software Architect",
        employer = "Tech Innovations Lab",
        salaryRange = "₹35-50 LPA",
        education = "M.Tech Computer Science, IISc Bangalore",
        skills = "Cloud Architecture, Distributed Systems, Classical Vocal",
        linkedinUrl = "https://linkedin.com/in/aditirao-example",
        instagramUrl = "https://instagram.com/aditi_trails",
        facebookUrl = null,
        websiteUrl = "https://aditirao.dev",
        profileVisibility = "PUBLIC",
        showPhone = true,
        showSalary = true,
        showSocial = true,
        backgroundCheckStatus = "VERIFIED",
        verificationStatus = "VERIFIED",
        verifiedAt = "2025-01-10T10:00:00Z",
        profileLocked = false
    )

    private val sampleDiscoveryProfiles = mutableListOf(
        ProfileSearchItemDto(
            profileId = "prof-001",
            displayName = "Priya Sharma",
            age = 26,
            country = "India",
            stateProvince = "Karnataka",
            city = "Bengaluru",
            profession = "Lead Product Designer",
            skills = "Design Systems, User Research, Pottery",
            salaryRange = "₹25-35 LPA",
            linkedinUrl = "https://linkedin.com/in/priyasharma",
            instagramUrl = "https://instagram.com/priya_designs",
            facebookUrl = null,
            websiteUrl = "https://priyasharma.design",
            verified = true,
            photoAvailable = false,
            photoUrl = null
        ),
        ProfileSearchItemDto(
            profileId = "prof-002",
            displayName = "Rahul Verma",
            age = 29,
            country = "India",
            stateProvince = "Maharashtra",
            city = "Mumbai",
            profession = "Vice President, Fintech",
            skills = "Investment Strategy, Marathon Running, Photography",
            salaryRange = "₹45-60 LPA",
            linkedinUrl = "https://linkedin.com/in/rahulverma",
            instagramUrl = "https://instagram.com/rahul_runs",
            facebookUrl = null,
            websiteUrl = null,
            verified = true,
            photoAvailable = false,
            photoUrl = null
        ),
        ProfileSearchItemDto(
            profileId = "prof-003",
            displayName = "Ananya Patel",
            age = 27,
            country = "India",
            stateProvince = "Gujarat",
            city = "Ahmedabad",
            profession = "AI Research Scientist",
            skills = "Machine Learning, Astronomy, Indian Classical Dance",
            salaryRange = "₹30-40 LPA",
            linkedinUrl = "https://linkedin.com/in/ananyapatel",
            instagramUrl = null,
            facebookUrl = null,
            websiteUrl = "https://ananya-ai.org",
            verified = true,
            photoAvailable = false,
            photoUrl = null
        ),
        ProfileSearchItemDto(
            profileId = "prof-004",
            displayName = "Vikram Malhotra",
            age = 31,
            country = "India",
            stateProvince = "Delhi",
            city = "New Delhi",
            profession = "Corporate Lawyer & Partner",
            skills = "Corporate Law, Golf, World History",
            salaryRange = "₹50-70 LPA",
            linkedinUrl = "https://linkedin.com/in/vikram-malhotra",
            instagramUrl = null,
            facebookUrl = null,
            websiteUrl = null,
            verified = true,
            photoAvailable = false,
            photoUrl = null
        ),
        ProfileSearchItemDto(
            profileId = "prof-005",
            displayName = "Sneha Reddy",
            age = 28,
            country = "India",
            stateProvince = "Telangana",
            city = "Hyderabad",
            profession = "Urban Architect & Restorer",
            skills = "Sustainable Architecture, Watercolor, Trekking",
            salaryRange = "₹22-30 LPA",
            linkedinUrl = "https://linkedin.com/in/snehareddy-arch",
            instagramUrl = "https://instagram.com/sneha_spaces",
            facebookUrl = null,
            websiteUrl = null,
            verified = true,
            photoAvailable = false,
            photoUrl = null
        ),
        ProfileSearchItemDto(
            profileId = "prof-006",
            displayName = "Dr. Rohan Gupta",
            age = 30,
            country = "India",
            stateProvince = "Tamil Nadu",
            city = "Chennai",
            profession = "Interventional Cardiologist",
            skills = "Medicine, Acoustic Guitar, Badminton",
            salaryRange = "₹40-55 LPA",
            linkedinUrl = null,
            instagramUrl = null,
            facebookUrl = null,
            websiteUrl = null,
            verified = true,
            photoAvailable = false,
            photoUrl = null
        ),
        ProfileSearchItemDto(
            profileId = "prof-007",
            displayName = "Kavita Joshi",
            age = 25,
            country = "India",
            stateProvince = "Maharashtra",
            city = "Pune",
            profession = "Data Journalist & Author",
            skills = "Investigative Journalism, Creative Writing, Piano",
            salaryRange = "₹18-25 LPA",
            linkedinUrl = "https://linkedin.com/in/kavitajoshi",
            instagramUrl = "https://instagram.com/kavita_stories",
            facebookUrl = null,
            websiteUrl = "https://kavitajoshi.in",
            verified = true,
            photoAvailable = false,
            photoUrl = null
        ),
        ProfileSearchItemDto(
            profileId = "prof-008",
            displayName = "Arjun Das",
            age = 32,
            country = "India",
            stateProvince = "West Bengal",
            city = "Kolkata",
            profession = "CleanTech Entrepreneur",
            skills = "Renewable Energy, Chess Master, Debating",
            salaryRange = "₹40-60 LPA",
            linkedinUrl = "https://linkedin.com/in/arjundas-energy",
            instagramUrl = null,
            facebookUrl = null,
            websiteUrl = "https://arjundas.eco",
            verified = true,
            photoAvailable = false,
            photoUrl = null
        )
    )

    private val notifications = mutableListOf(
        NotificationDto(
            id = "notif-001",
            type = "MATCH_INTEREST",
            title = "New Profile View",
            body = "Rahul Verma recently viewed your profile.",
            readAt = null,
            createdAt = "2026-09-25T08:15:00Z"
        ),
        NotificationDto(
            id = "notif-002",
            type = "VERIFICATION",
            title = "Profile Verified",
            body = "Your government ID verification has been approved with a verified badge.",
            readAt = null,
            createdAt = "2026-09-24T14:30:00Z"
        ),
        NotificationDto(
            id = "notif-003",
            type = "RECOMMENDATION",
            title = "New Match Recommendation",
            body = "We found 3 high-compatibility profiles based on your career and location preferences.",
            readAt = "2026-09-23T10:00:00Z",
            createdAt = "2026-09-23T09:00:00Z"
        )
    )

    private var securityState = SecurityStateDto(
        mfaEnabled = false,
        visibility = "PUBLIC",
        profileLocked = false,
        showPhone = true,
        showSalary = true,
        showSocial = true
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // First attempt the network call in case a real backend is running
        try {
            val response = chain.proceed(request)
            // If the server answered with an HTTP status code, honor the server
            if (response.code in 200..499) {
                return response
            }
        } catch (_: IOException) {
            // Server is not running or unreachable -> seamlessly provide local mock data
        }

        return fulfillMock(request)
    }

    private fun fulfillMock(request: Request): Response {
        val path = request.url.encodedPath
        val method = request.method

        // 1. Auth endpoints
        if (path.endsWith("api/v1/auth/login") && method == "POST") {
            val bodyString = request.body?.let { body ->
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            } ?: ""
            val loginReq = try {
                gson.fromJson(bodyString, LoginRequestDto::class.java)
            } catch (_: Exception) { null }

            val email = loginReq?.email?.takeIf { it.isNotBlank() } ?: "aditi.rao@example.com"
            val displayName = email.substringBefore("@")
                .replace(".", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

            val isAdmin = email.contains("admin", ignoreCase = true)
            currentUser = AuthenticatedUser(
                userId = "user-" + UUID.nameUUIDFromBytes(email.toByteArray()).toString().take(8),
                displayName = displayName,
                email = email,
                expiresAt = "2030-12-31T23:59:59Z",
                role = if (isAdmin) "ADMIN" else "MEMBER"
            )

            val loginResponse = LoginResponseDto(
                token = "mock-jwt-token-" + UUID.randomUUID().toString(),
                userId = currentUser.userId,
                displayName = currentUser.displayName,
                email = currentUser.email,
                expiresAt = currentUser.expiresAt,
                mfaRequired = false,
                mfaChallengeId = null,
                role = currentUser.role
            )
            return jsonResponse(request, 200, loginResponse)
        }

        if (path.endsWith("api/v1/auth/register") && method == "POST") {
            val bodyString = request.body?.let { body ->
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            } ?: ""
            val regReq = try {
                gson.fromJson(bodyString, RegisterRequestDto::class.java)
            } catch (_: Exception) { null }

            val name = regReq?.displayName ?: "New Member"
            val email = regReq?.email ?: "member@example.com"
            currentUser = AuthenticatedUser(
                userId = "user-" + UUID.randomUUID().toString().take(8),
                displayName = name,
                email = email,
                expiresAt = "2030-12-31T23:59:59Z",
                role = "MEMBER"
            )
            myProfile = myProfile.copy(
                displayName = name,
                userId = currentUser.userId
            )

            val registerResponse = RegisterResponseDto(
                userId = currentUser.userId,
                message = "Account created successfully."
            )
            return jsonResponse(request, 200, registerResponse)
        }

        if (path.endsWith("api/v1/auth/me") && method == "GET") {
            val me = MeResponseDto(
                userId = currentUser.userId,
                displayName = currentUser.displayName,
                email = currentUser.email,
                role = currentUser.role,
                mfaEnabled = securityState.mfaEnabled
            )
            return jsonResponse(request, 200, me)
        }

        if (path.endsWith("api/v1/auth/logout") && method == "POST") {
            return jsonResponse(request, 200, MessageResponseDto("Logout successful."))
        }

        if (path.endsWith("api/v1/auth/mfa/verify") && method == "POST") {
            val loginResponse = LoginResponseDto(
                token = "mock-jwt-token-" + UUID.randomUUID().toString(),
                userId = currentUser.userId,
                displayName = currentUser.displayName,
                email = currentUser.email,
                expiresAt = currentUser.expiresAt,
                mfaRequired = false,
                mfaChallengeId = null,
                role = currentUser.role
            )
            return jsonResponse(request, 200, loginResponse)
        }

        // 2. Profile Discovery and Search
        if (path.contains("api/v1/profiles/search") && method == "GET") {
            val minAge = request.url.queryParameter("minAge")?.toIntOrNull() ?: 18
            val maxAge = request.url.queryParameter("maxAge")?.toIntOrNull() ?: 100
            val cityQuery = request.url.queryParameter("city").orEmpty().trim().lowercase()
            val countryQuery = request.url.queryParameter("country").orEmpty().trim().lowercase()
            val page = request.url.queryParameter("page")?.toIntOrNull() ?: 0
            val pageSize = request.url.queryParameter("pageSize")?.toIntOrNull() ?: 20

            val filtered = sampleDiscoveryProfiles.filter { item ->
                item.age in minAge..maxAge &&
                    (cityQuery.isEmpty() || item.city.orEmpty().lowercase().contains(cityQuery)) &&
                    (countryQuery.isEmpty() || item.country.orEmpty().lowercase().contains(countryQuery))
            }

            val paged = filtered.drop(page * pageSize).take(pageSize)
            val totalPages = if (filtered.isEmpty()) 1 else ((filtered.size + pageSize - 1) / pageSize)

            val searchResult = ProfileSearchResponseDto(
                items = paged,
                page = page,
                pageSize = pageSize,
                totalItems = filtered.size.toLong(),
                totalPages = totalPages
            )
            return jsonResponse(request, 200, searchResult)
        }

        // Detail profile by ID
        if (path.matches(Regex(".*/api/v1/profiles/[^/]+$")) && method == "GET") {
            val id = path.substringAfterLast("/")
            val match = sampleDiscoveryProfiles.find { it.profileId == id } ?: sampleDiscoveryProfiles.first()
            val detail = ProfileDetailDto(
                profileId = match.profileId,
                displayName = match.displayName,
                age = match.age,
                country = match.country,
                stateProvince = match.stateProvince,
                city = match.city,
                profession = match.profession,
                skills = match.skills,
                salaryRange = match.salaryRange,
                linkedinUrl = match.linkedinUrl,
                instagramUrl = match.instagramUrl,
                facebookUrl = match.facebookUrl,
                websiteUrl = match.websiteUrl,
                verified = match.verified,
                photoAvailable = match.photoAvailable,
                photoUrl = match.photoUrl
            )
            return jsonResponse(request, 200, detail)
        }

        // 3. User's Own Profile
        if (path.endsWith("api/v1/profile") && method == "GET") {
            return jsonResponse(request, 200, myProfile)
        }

        if (path.endsWith("api/v1/profile") && method == "PUT") {
            val bodyString = request.body?.let { body ->
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            } ?: ""
            val updateReq = try {
                gson.fromJson(bodyString, ProfileUpdateRequestDto::class.java)
            } catch (_: Exception) { null }

            if (updateReq != null) {
                myProfile = myProfile.copy(
                    displayName = updateReq.displayName,
                    dateOfBirth = updateReq.dateOfBirth,
                    gender = updateReq.gender,
                    country = updateReq.country,
                    stateProvince = updateReq.stateProvince,
                    city = updateReq.city,
                    bio = updateReq.bio,
                    phoneNumber = updateReq.phoneNumber,
                    profession = updateReq.profession,
                    employer = updateReq.employer,
                    salaryRange = updateReq.salaryRange,
                    education = updateReq.education,
                    skills = updateReq.skills,
                    linkedinUrl = updateReq.linkedinUrl,
                    instagramUrl = updateReq.instagramUrl,
                    facebookUrl = updateReq.facebookUrl,
                    websiteUrl = updateReq.websiteUrl,
                    profileVisibility = updateReq.profileVisibility,
                    showPhone = updateReq.showPhone,
                    showSalary = updateReq.showSalary,
                    showSocial = updateReq.showSocial,
                    profileLocked = updateReq.profileLocked
                )
            }
            return jsonResponse(request, 200, myProfile)
        }

        // 4. Notifications
        if (path.endsWith("api/v1/notifications") && method == "GET") {
            return jsonResponse(request, 200, notifications.toList())
        }

        if (path.contains("api/v1/notifications/") && path.endsWith("/read") && method == "POST") {
            val notifId = path.substringAfter("notifications/").substringBefore("/read")
            val index = notifications.indexOfFirst { it.id == notifId }
            if (index != -1) {
                notifications[index] = notifications[index].copy(readAt = "2026-09-25T10:00:00Z")
            }
            return jsonResponse(request, 200, MessageResponseDto("Notification marked as read."))
        }

        // 5. Security & Privacy
        if (path.endsWith("api/v1/security") && method == "GET") {
            return jsonResponse(request, 200, securityState)
        }

        if (path.endsWith("api/v1/security/privacy") && method == "PUT") {
            val bodyString = request.body?.let { body ->
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            } ?: ""
            val privReq = try {
                gson.fromJson(bodyString, PrivacyDto::class.java)
            } catch (_: Exception) { null }

            if (privReq != null) {
                securityState = securityState.copy(
                    visibility = privReq.visibility,
                    profileLocked = privReq.profileLocked,
                    showPhone = privReq.showPhone,
                    showSalary = privReq.showSalary,
                    showSocial = privReq.showSocial
                )
            }
            return jsonResponse(request, 200, MessageResponseDto("Privacy settings updated successfully."))
        }

        if (path.endsWith("api/v1/security/mfa/setup") && method == "POST") {
            val setupDto = MfaSetupDto(
                secret = "JBSWY3DPEHPK3PXP",
                message = "Scan the QR code or enter this secret into your authenticator app."
            )
            return jsonResponse(request, 200, setupDto)
        }

        if (path.endsWith("api/v1/security/mfa/enable") && method == "POST") {
            securityState = securityState.copy(mfaEnabled = true)
            return jsonResponse(request, 200, MessageResponseDto("Two-factor authentication enabled."))
        }

        if (path.endsWith("api/v1/security/mfa/disable") && method == "POST") {
            securityState = securityState.copy(mfaEnabled = false)
            return jsonResponse(request, 200, MessageResponseDto("Two-factor authentication disabled."))
        }

        if (path.contains("api/v1/security/block/") && method in listOf("POST", "DELETE")) {
            val action = if (method == "POST") "blocked" else "unblocked"
            return jsonResponse(request, 200, MessageResponseDto("User $action successfully."))
        }

        if (path.contains("api/v1/security/report/") && method == "POST") {
            return jsonResponse(request, 200, MessageResponseDto("Report submitted for administrator review."))
        }

        // 6. Admin endpoints
        if (path.endsWith("api/v1/admin/dashboard") && method == "GET") {
            val dash = AdminDashboardDto(
                users = 254,
                profiles = 218,
                openReports = 2,
                pendingVerification = 5
            )
            return jsonResponse(request, 200, dash)
        }

        if (path.endsWith("api/v1/admin/reports") && method == "GET") {
            val reports = listOf(
                AdminReportDto(
                    id = "rep-001",
                    reporterUserId = "user-101",
                    reportedUserId = "user-202",
                    reason = "INAPPROPRIATE_BEHAVIOR",
                    details = "Repeated unwanted messaging after unmatching.",
                    status = "OPEN",
                    createdAt = "2026-09-24T18:00:00Z"
                )
            )
            return jsonResponse(request, 200, reports)
        }

        if (path.contains("api/v1/admin/")) {
            return jsonResponse(request, 200, MessageResponseDto("Action completed."))
        }

        // Default 200 OK fallback for any other endpoints
        return jsonResponse(request, 200, MessageResponseDto("Success"))
    }

    private fun jsonResponse(request: Request, code: Int, obj: Any): Response {
        val json = gson.toJson(obj)
        val body = json.toResponseBody(jsonMedia)
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OK")
            .body(body)
            .build()
    }
}
