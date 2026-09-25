package com.matrimonyapp.data.remote

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSearchDtosTest {
    private val gson = Gson()

    @Test
    fun parsesSearchResponse() {
        val json = """
            {"items":[
              {"profileId":"11111111-1111-1111-1111-111111111111","displayName":"Asha","age":29,
               "country":"India","stateProvince":"Telangana","city":"Hyderabad",
               "photoAvailable":true,"photoUrl":"/api/v1/profiles/11111111-1111-1111-1111-111111111111/photo"},
              {"profileId":"22222222-2222-2222-2222-222222222222","displayName":"Bina","age":31,
               "country":"India","stateProvince":"Kerala","city":"Kochi",
               "photoAvailable":false,"photoUrl":null}
            ],"page":0,"pageSize":20,"totalItems":2,"totalPages":1}
        """.trimIndent()

        val dto = gson.fromJson(json, ProfileSearchResponseDto::class.java)

        assertEquals(2, dto.items.size)
        assertEquals("Asha", dto.items[0].displayName)
        assertEquals(29, dto.items[0].age)
        assertEquals("Telangana", dto.items[0].stateProvince)
        assertTrue(dto.items[0].photoAvailable)
        assertFalse(dto.items[1].photoAvailable)
        assertNull(dto.items[1].photoUrl)
        assertEquals(0, dto.page)
        assertEquals(20, dto.pageSize)
        assertEquals(2L, dto.totalItems)
        assertEquals(1, dto.totalPages)
    }

    @Test
    fun parsesEmptySearchResponse() {
        val dto = gson.fromJson(
            """{"items":[],"page":0,"pageSize":20,"totalItems":0,"totalPages":0}""",
            ProfileSearchResponseDto::class.java
        )

        assertTrue(dto.items.isEmpty())
        assertEquals(0L, dto.totalItems)
    }

    @Test
    fun ignoresUnknownServerFields() {
        val dto = gson.fromJson(
            """{"profileId":"p1","displayName":"Asha","age":29,"country":"India","stateProvince":"T",
               "city":"H","photoAvailable":false,"photoUrl":null,"somethingNew":"ignored"}""",
            ProfileDetailDto::class.java
        )

        assertEquals("p1", dto.profileId)
        assertEquals(29, dto.age)
    }

    @Test
    fun parsesLargeTotals() {
        val dto = gson.fromJson(
            """{"items":[],"page":0,"pageSize":50,"totalItems":5000000000,"totalPages":100000000}""",
            ProfileSearchResponseDto::class.java
        )
        assertEquals(5_000_000_000L, dto.totalItems)
    }

    @Test
    fun discoveryDtosDeclareNoPrivateOrInternalFields() {
        val forbidden = setOf(
            "password", "passwordhash", "token", "tokenhash", "phone", "phonenumber",
            "photostoragekey", "storagekey", "path", "userid", "email", "dateofbirth", "bio"
        )
        listOf(
            ProfileSearchItemDto::class.java,
            ProfileSearchResponseDto::class.java,
            ProfileDetailDto::class.java
        ).forEach { type ->
            type.declaredFields.forEach { field ->
                assertFalse(
                    "${type.simpleName} must not declare '${field.name}'",
                    field.name.lowercase() in forbidden
                )
            }
        }
    }
}
