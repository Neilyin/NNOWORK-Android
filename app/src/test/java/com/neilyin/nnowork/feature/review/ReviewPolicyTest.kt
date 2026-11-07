package com.neilyin.nnowork.feature.review

import com.neilyin.nnowork.data.settings.ReviewMoment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPolicyTest {
    @Test fun firstRequestAllowed() {
        assertTrue(ReviewPolicy.shouldRequest(momentAlreadyUsed = false, requestedThisSession = false, hasForegroundActivity = true))
    }

    @Test fun usedMomentIsSkipped() {
        assertFalse(ReviewPolicy.shouldRequest(momentAlreadyUsed = true, requestedThisSession = false, hasForegroundActivity = true))
    }

    @Test fun onlyOncePerSession() {
        assertFalse(ReviewPolicy.shouldRequest(momentAlreadyUsed = false, requestedThisSession = true, hasForegroundActivity = true))
    }

    @Test fun noForegroundActivityIsSkipped() {
        assertFalse(ReviewPolicy.shouldRequest(momentAlreadyUsed = false, requestedThisSession = false, hasForegroundActivity = false))
    }

    @Test fun requestedRecentlyWindow() {
        assertFalse(ReviewPolicy.requestedRecently(null, 1_000))
        assertTrue(ReviewPolicy.requestedRecently(1_000, 1_000 + 9_999))
        assertFalse(ReviewPolicy.requestedRecently(1_000, 1_000 + 10_000))
        // 預計時間在未來（delay 尚未到）也算最近
        assertTrue(ReviewPolicy.requestedRecently(5_000, 1_000))
    }

    @Test fun momentFlagsMatchIos() {
        assertEquals(
            listOf("review_milestone_timerSetup", "review_milestone_firstStamp", "review_milestone_cardComplete"),
            ReviewMoment.entries.map { it.prefKey },
        )
    }
}
