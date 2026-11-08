package com.neilyin.nnowork.data.identity

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceIdentityCodecTest {
    @Test
    fun `new install always uses random UUID seed`() {
        assertEquals(
            "new-random-uuid",
            DeviceIdentityCodec.initialSeed(
                hadLegacyLocalData = false,
                legacyAndroidId = "legacy-android-id",
                randomUuid = "new-random-uuid",
            ),
        )
    }

    @Test
    fun `upgrade preserves legacy Android ID when local data exists`() {
        assertEquals(
            "legacy-android-id",
            DeviceIdentityCodec.initialSeed(
                hadLegacyLocalData = true,
                legacyAndroidId = "legacy-android-id",
                randomUuid = "fallback-random-uuid",
            ),
        )
    }

    @Test
    fun `upgrade falls back to random UUID when Android ID is blank`() {
        assertEquals(
            "fallback-random-uuid",
            DeviceIdentityCodec.initialSeed(
                hadLegacyLocalData = true,
                legacyAndroidId = " ",
                randomUuid = "fallback-random-uuid",
            ),
        )
    }

    @Test
    fun `sha256 is stable lowercase hex`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            DeviceIdentityCodec.sha256Hex("abc"),
        )
    }
}
