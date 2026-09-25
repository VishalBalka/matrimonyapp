package com.matrimonyapp.data

import com.google.gson.Gson
import com.matrimonyapp.data.remote.AdminDashboardDto
import com.matrimonyapp.data.remote.AdminReportDto
import com.matrimonyapp.data.remote.CodeRequest
import com.matrimonyapp.data.remote.LoginResponseDto
import com.matrimonyapp.data.remote.MeResponseDto
import com.matrimonyapp.data.remote.MfaSetupDto
import com.matrimonyapp.data.remote.MfaVerifyRequestDto
import com.matrimonyapp.data.remote.NotificationDto
import com.matrimonyapp.data.remote.PrivacyDto
import com.matrimonyapp.data.remote.ProfileResponseDto
import com.matrimonyapp.data.remote.ProfileUpdateRequestDto
import com.matrimonyapp.data.remote.ReportRequest
import com.matrimonyapp.data.remote.SecurityStateDto
import com.matrimonyapp.data.remote.StatusRequest
import com.matrimonyapp.data.remote.VerifyRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JSON shaped exactly like the backend records (field names and nullability), so a rename on either
 * side is caught here instead of at runtime.
 */
class BackendContractDtosTest {
    private val gson = Gson()

    @Test
    fun loginResponseWithSession() {
        val dto = gson.fromJson(
            """{"token":"t","userId":"u","displayName":"A","email":"a@x","expiresAt":"2026-09-19T10:15:00Z",
               "mfaRequired":false,"mfaChallengeId":null,"role":"ADMIN"}""",
            LoginResponseDto::class.java
        )

        assertEquals("t", dto.token); assertFalse(dto.mfaRequired); assertNull(dto.mfaChallengeId); assertEquals("ADMIN", dto.role)
    }

    @Test
    fun loginResponseForAnMfaChallenge() {
        val dto = gson.fromJson(
            """{"token":"","userId":"u","displayName":"A","email":"a@x","expiresAt":"",
               "mfaRequired":true,"mfaChallengeId":"c-1","role":"MEMBER"}""",
            LoginResponseDto::class.java
        )

        assertTrue(dto.mfaRequired); assertEquals("c-1", dto.mfaChallengeId); assertEquals("", dto.token)
    }

    @Test
    fun meResponseCarriesRoleAndMfaState() {
        val dto = gson.fromJson(
            """{"userId":"u","displayName":"A","email":"a@x","role":"MEMBER","mfaEnabled":true}""",
            MeResponseDto::class.java
        )

        assertEquals("MEMBER", dto.role); assertTrue(dto.mfaEnabled)
    }

    @Test
    fun mfaVerifyRequestUsesBackendFieldNames() {
        assertEquals("""{"challengeId":"c","code":"123456"}""", gson.toJson(MfaVerifyRequestDto("c", "123456")))
    }

    @Test
    fun securityDtos() {
        val state = gson.fromJson(
            """{"mfaEnabled":true,"visibility":"PRIVATE","profileLocked":true,"showPhone":false,"showSalary":true,"showSocial":false}""",
            SecurityStateDto::class.java
        )
        assertTrue(state.mfaEnabled); assertEquals("PRIVATE", state.visibility); assertTrue(state.profileLocked)
        assertFalse(state.showPhone); assertTrue(state.showSalary); assertFalse(state.showSocial)

        assertEquals(
            """{"visibility":"PUBLIC","profileLocked":false,"showPhone":true,"showSalary":false,"showSocial":true}""",
            gson.toJson(PrivacyDto("PUBLIC", false, true, false, true))
        )
        assertEquals("""{"code":"123456"}""", gson.toJson(CodeRequest("123456")))
        assertEquals("""{"reason":"Spam"}""", gson.toJson(ReportRequest("Spam", null)))
        assertEquals("S", gson.fromJson("""{"secret":"S","message":"m"}""", MfaSetupDto::class.java).secret)
    }

    @Test
    fun notificationDto() {
        val dto = gson.fromJson(
            """{"id":"i","type":"REPORT","title":"T","body":"B","readAt":null,"createdAt":"2026-09-19T10:00:00Z"}""",
            NotificationDto::class.java
        )
        assertEquals("i", dto.id); assertNull(dto.readAt); assertEquals("REPORT", dto.type)
    }

    @Test
    fun adminDtos() {
        val dash = gson.fromJson("""{"users":5,"profiles":4,"openReports":2,"pendingVerification":1}""", AdminDashboardDto::class.java)
        assertEquals(5, dash.users); assertEquals(1, dash.pendingVerification)

        val report = gson.fromJson(
            """{"id":"r","reporterUserId":"a","reportedUserId":"b","reason":"Spam","details":null,"status":"OPEN","createdAt":"2026-09-19T10:00:00Z"}""",
            AdminReportDto::class.java
        )
        assertEquals("b", report.reportedUserId); assertNull(report.details)

        assertEquals("""{"verified":true}""", gson.toJson(VerifyRequest(true)))
        assertEquals("""{"status":"RESOLVED"}""", gson.toJson(StatusRequest("RESOLVED")))
    }

    @Test
    fun profileResponseWithAllPhase12Fields() {
        val dto = gson.fromJson(
            """{"profileId":"p","userId":"u","displayName":"A","dateOfBirth":"1995-05-20","gender":"FEMALE",
               "country":"India","stateProvince":"TS","city":"Hyd","bio":"b","phoneNumber":null,"photoAvailable":true,
               "profession":"Eng","employer":null,"salaryRange":"10-15 LPA","education":null,"skills":"Kotlin",
               "linkedinUrl":"https://www.linkedin.com/in/a","instagramUrl":null,"facebookUrl":null,"websiteUrl":null,
               "profileVisibility":"PUBLIC","showPhone":false,"showSalary":true,"showSocial":true,
               "backgroundCheckStatus":"UNREQUESTED","verificationStatus":"UNVERIFIED","verifiedAt":null,"profileLocked":false}""",
            ProfileResponseDto::class.java
        )

        assertEquals("Eng", dto.profession); assertEquals("PUBLIC", dto.profileVisibility)
        assertTrue(dto.showSalary); assertEquals("UNVERIFIED", dto.verificationStatus); assertNull(dto.verifiedAt)
    }

    @Test
    fun profileUpdateRequestAlwaysSendsTheBooleans() {
        // The backend record uses primitive booleans: omitting one would silently read as false.
        val json = gson.toJson(
            ProfileUpdateRequestDto(
                "A", "1995-05-20", "MALE", "India", "TS", "Hyd", "", null, null, null, null, null, null,
                null, null, null, null, "PUBLIC", false, false, true, false
            )
        )

        for (key in listOf("showPhone", "showSalary", "showSocial", "profileLocked", "profileVisibility")) {
            assertTrue("$key must be serialized: $json", json.contains("\"$key\":"))
        }
    }
}
