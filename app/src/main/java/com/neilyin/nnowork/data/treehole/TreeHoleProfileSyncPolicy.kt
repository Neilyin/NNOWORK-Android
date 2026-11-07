package com.neilyin.nnowork.data.treehole

/** Completes the account side of profile sync without weakening local-first persistence. */
internal suspend fun completeProfileSync(
    upsertSucceeded: Boolean,
    signedIn: Boolean,
    claimDeviceData: suspend () -> Boolean,
): Boolean {
    if (!upsertSucceeded) return false
    if (!signedIn) return true
    return claimDeviceData()
}

/** Prevents the post-industry mutation from racing ahead of the canonical profile upsert. */
internal suspend fun <T> mutateAfterProfileSync(
    localProfile: TreeHoleProfile?,
    syncProfile: suspend (TreeHoleProfile) -> Boolean,
    mutation: suspend () -> T,
): T {
    val completeProfile = localProfile ?: error("請先建立樹洞身份。")
    require(completeProfile.isComplete) { "請先完成樹洞身份。" }
    check(syncProfile(completeProfile)) { "個人資料同步失敗，請稍後再試。" }
    return mutation()
}
