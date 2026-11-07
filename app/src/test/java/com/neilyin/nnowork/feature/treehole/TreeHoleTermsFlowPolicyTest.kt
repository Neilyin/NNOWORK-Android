package com.neilyin.nnowork.feature.treehole

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeHoleTermsFlowPolicyTest {
    @Test
    fun `disclaimer must be agreed before privacy step opens`() {
        assertFalse(
            TreeHoleTermsFlowPolicy.canContinue(
                step = TreeHoleTermsStep.DISCLAIMER,
                disclaimerAgreed = false,
                privacyAgreed = false,
            ),
        )
        assertEquals(
            TreeHoleTermsStep.DISCLAIMER,
            TreeHoleTermsFlowPolicy.nextStep(
                step = TreeHoleTermsStep.DISCLAIMER,
                disclaimerAgreed = false,
                privacyAgreed = false,
            ),
        )
    }

    @Test
    fun `agreeing disclaimer advances to privacy`() {
        assertEquals(
            TreeHoleTermsStep.PRIVACY,
            TreeHoleTermsFlowPolicy.nextStep(
                step = TreeHoleTermsStep.DISCLAIMER,
                disclaimerAgreed = true,
                privacyAgreed = false,
            ),
        )
    }

    @Test
    fun `final acceptance requires both confirmations on privacy step`() {
        assertFalse(
            TreeHoleTermsFlowPolicy.canComplete(
                step = TreeHoleTermsStep.DISCLAIMER,
                disclaimerAgreed = true,
                privacyAgreed = true,
            ),
        )
        assertFalse(
            TreeHoleTermsFlowPolicy.canComplete(
                step = TreeHoleTermsStep.PRIVACY,
                disclaimerAgreed = true,
                privacyAgreed = false,
            ),
        )
        assertTrue(
            TreeHoleTermsFlowPolicy.canComplete(
                step = TreeHoleTermsStep.PRIVACY,
                disclaimerAgreed = true,
                privacyAgreed = true,
            ),
        )
    }

    @Test
    fun `back from privacy returns to disclaimer`() {
        assertEquals(
            TreeHoleTermsStep.DISCLAIMER,
            TreeHoleTermsFlowPolicy.previousStep(TreeHoleTermsStep.PRIVACY),
        )
    }

    @Test
    fun `invalid saved step index safely restarts from disclaimer`() {
        assertEquals(TreeHoleTermsStep.DISCLAIMER, TreeHoleTermsStep.fromIndex(-1))
        assertEquals(TreeHoleTermsStep.DISCLAIMER, TreeHoleTermsStep.fromIndex(99))
    }
}
