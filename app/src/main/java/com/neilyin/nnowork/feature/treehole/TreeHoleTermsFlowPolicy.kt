package com.neilyin.nnowork.feature.treehole

internal enum class TreeHoleTermsStep(val index: Int) {
    DISCLAIMER(0),
    PRIVACY(1),
    ;

    companion object {
        fun fromIndex(index: Int): TreeHoleTermsStep =
            entries.getOrElse(index) { DISCLAIMER }
    }
}

/**
 * Keeps the two legal confirmations explicit and ordered. Partial progress is UI-only;
 * the accepted terms version is persisted by the repository only after [canComplete] is true.
 */
internal object TreeHoleTermsFlowPolicy {
    const val STEP_COUNT = 2

    fun canContinue(
        step: TreeHoleTermsStep,
        disclaimerAgreed: Boolean,
        privacyAgreed: Boolean,
    ): Boolean = when (step) {
        TreeHoleTermsStep.DISCLAIMER -> disclaimerAgreed
        TreeHoleTermsStep.PRIVACY -> disclaimerAgreed && privacyAgreed
    }

    fun nextStep(
        step: TreeHoleTermsStep,
        disclaimerAgreed: Boolean,
        privacyAgreed: Boolean,
    ): TreeHoleTermsStep = when {
        !canContinue(step, disclaimerAgreed, privacyAgreed) -> step
        step == TreeHoleTermsStep.DISCLAIMER -> TreeHoleTermsStep.PRIVACY
        else -> step
    }

    fun previousStep(step: TreeHoleTermsStep): TreeHoleTermsStep = when (step) {
        TreeHoleTermsStep.DISCLAIMER -> TreeHoleTermsStep.DISCLAIMER
        TreeHoleTermsStep.PRIVACY -> TreeHoleTermsStep.DISCLAIMER
    }

    fun canComplete(
        step: TreeHoleTermsStep,
        disclaimerAgreed: Boolean,
        privacyAgreed: Boolean,
    ): Boolean =
        step == TreeHoleTermsStep.PRIVACY && disclaimerAgreed && privacyAgreed
}
