package com.neilyin.nnowork.feature.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateBannerPolicyTest {
    @Test fun showsWhenAvailableAndNeverDismissed() {
        assertTrue(UpdateBannerPolicy.shouldShow(true, 12, 0))
    }

    @Test fun hiddenWhenNoUpdate() {
        assertFalse(UpdateBannerPolicy.shouldShow(false, 12, 0))
    }

    @Test fun hiddenAfterDismissingSameVersion() {
        assertFalse(UpdateBannerPolicy.shouldShow(true, 12, 12))
    }

    @Test fun showsAgainForNewerVersion() {
        assertTrue(UpdateBannerPolicy.shouldShow(true, 13, 12))
    }

    @Test fun invalidVersionCodeNeverShows() {
        assertFalse(UpdateBannerPolicy.shouldShow(true, 0, 0))
    }
}
