package com.neilyin.nnowork.data.backup

import com.neilyin.nnowork.data.JsonProvider
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID

/** 備份 JSON 解碼、正規化與寫入前驗證；不依賴 Android API，可直接做 JVM 單元測試。 */
class BackupCodec(moshi: Moshi = JsonProvider.moshi) {
    private val adapter = moshi.adapter(BackupPayload::class.java)

    fun encode(payload: BackupPayload): String {
        val normalized = normalize(payload.copy(schemaVersion = BackupPayload.CURRENT_SCHEMA_VERSION))
        validate(normalized)
        return try {
            adapter.indent("  ").toJson(normalized)
        } catch (error: RuntimeException) {
            throw BackupEncodingException("Unable to encode backup JSON", error)
        }
    }

    /** 完整解碼、正規化、驗證後才回傳；本函式絕不會寫入任何 App 資料。 */
    fun decodeAndValidate(json: String): BackupPayload {
        if (json.isBlank()) throw BackupDecodingException("Backup JSON is empty")
        if (json.length > MAX_JSON_CHARS) {
            throw BackupDecodingException("Backup JSON exceeds the ${MAX_JSON_CHARS / 1_048_576} MiB limit")
        }

        val decoded = try {
            adapter.fromJson(json) ?: throw BackupDecodingException("Backup JSON contains no payload")
        } catch (error: BackupDecodingException) {
            throw error
        } catch (error: JsonDataException) {
            throw BackupDecodingException("Backup JSON has an invalid structure", error)
        } catch (error: IOException) {
            throw BackupDecodingException("Backup JSON could not be read", error)
        } catch (error: RuntimeException) {
            throw BackupDecodingException("Backup JSON could not be decoded", error)
        }

        return normalize(decoded).also(::validate)
    }

    fun validate(payload: BackupPayload) {
        val issues = mutableListOf<BackupValidationIssue>()
        fun issue(path: String, message: String) {
            if (issues.size < MAX_REPORTED_ISSUES) issues += BackupValidationIssue(path, message)
        }
        fun validDate(path: String, value: String?) {
            if (value == null) return
            if (runCatching { BackupDateCodec.parse(value) }.isFailure) issue(path, "must be an ISO-8601 date")
        }
        fun validText(path: String, value: String, maxLength: Int = MAX_TEXT_LENGTH) {
            if (value.length > maxLength) issue(path, "exceeds $maxLength characters")
        }
        fun validUuid(path: String, value: String) {
            val canonical = runCatching { UUID.fromString(value).toString() }.getOrNull()
            if (canonical == null || !canonical.equals(value, ignoreCase = true)) {
                issue(path, "must be a UUID")
            }
        }
        fun duplicateIds(path: String, ids: List<String>) {
            val duplicate = ids.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }?.key
            if (duplicate != null) issue(path, "contains a duplicate id: $duplicate")
        }

        if (payload.schemaVersion !in 1..BackupPayload.CURRENT_SCHEMA_VERSION) {
            issue("schemaVersion", "unsupported schema version ${payload.schemaVersion}")
        }
        validDate("exportDate", payload.exportDate)

        if (payload.appSettings.size != 1) {
            issue("appSettings", "must contain exactly one record")
        }
        payload.appSettings.forEachIndexed { index, settings ->
            val path = "appSettings[$index]"
            validText("$path.companyName", settings.companyName)
            if (settings.cardStyle < 1) issue("$path.cardStyle", "must be at least 1")
            if (settings.maxStamps !in 1..MAX_STAMPS_PER_CARD) {
                issue("$path.maxStamps", "must be between 1 and $MAX_STAMPS_PER_CARD")
            }
            if (settings.currentRound < 1) issue("$path.currentRound", "must be at least 1")
            if (settings.widgetRefreshInterval < 0) {
                issue("$path.widgetRefreshInterval", "must not be negative")
            }
            if (settings.coinCount < 0) issue("$path.coinCount", "must not be negative")
            validDate("$path.firstStampDate", settings.firstStampDate)
            validDate("$path.cardCompletedDate", settings.cardCompletedDate)
            validDate("$path.lastCoinEarnedDate", settings.lastCoinEarnedDate)
            validDate("$path.lastTreeHoleShareDate", settings.lastTreeHoleShareDate)
            settings.roundStartDates.forEachIndexed { dateIndex, date ->
                validDate("$path.roundStartDates[$dateIndex]", date)
            }
            settings.roundCompletedDates.forEachIndexed { dateIndex, date ->
                validDate("$path.roundCompletedDates[$dateIndex]", date)
            }
            settings.commonReasons.forEachIndexed { reasonIndex, reason ->
                validText("$path.commonReasons[$reasonIndex]", reason)
            }
        }

        if (payload.stampRecords.size > MAX_COLLECTION_RECORDS) {
            issue("stampRecords", "contains more than $MAX_COLLECTION_RECORDS records")
        }
        duplicateIds("stampRecords", payload.stampRecords.map { it.id })
        payload.stampRecords.forEachIndexed { index, stamp ->
            val path = "stampRecords[$index]"
            if (stamp.id.isBlank()) issue("$path.id", "must not be blank")
            else validUuid("$path.id", stamp.id)
            validDate("$path.date", stamp.date)
            validText("$path.reason", stamp.reason)
            if (stamp.roundNumber < 1) issue("$path.roundNumber", "must be at least 1")
        }

        if (payload.resignationPlans.size != 1) {
            issue("resignationPlans", "must contain exactly one record")
        }
        payload.resignationPlans.forEachIndexed { index, plan ->
            val path = "resignationPlans[$index]"
            if (!plan.targetAmount.isFinite() || plan.targetAmount < 0) {
                issue("$path.targetAmount", "must be a finite non-negative number")
            }
            if (!plan.currentSaved.isFinite() || plan.currentSaved < 0) {
                issue("$path.currentSaved", "must be a finite non-negative number")
            }
            val todos = plan.todoItems + plan.completedTodoItems
            if (todos.size > MAX_TODO_RECORDS) issue(path, "contains too many todo items")
            duplicateIds("$path.todoItems", todos.map { it.id })
            todos.forEachIndexed { todoIndex, todo ->
                val todoPath = "$path.todoItems[$todoIndex]"
                if (todo.id.isBlank()) issue("$todoPath.id", "must not be blank")
                else validUuid("$todoPath.id", todo.id)
                validText("$todoPath.title", todo.title)
                validDate("$todoPath.dueDate", todo.dueDate)
                validDate("$todoPath.createdAt", todo.createdAt)
            }
            plan.todoList.forEachIndexed { todoIndex, title ->
                validText("$path.todoList[$todoIndex]", title)
            }
            plan.completedTodoList.forEachIndexed { todoIndex, title ->
                validText("$path.completedTodoList[$todoIndex]", title)
            }
        }

        validateNiuMaConfig(payload.niuMaConfig, ::issue, ::validText)

        if (payload.coinLedgerEntries.size > MAX_COLLECTION_RECORDS) {
            issue("coinLedgerEntries", "contains too many records")
        }
        duplicateIds("coinLedgerEntries", payload.coinLedgerEntries.map { it.id })
        var nonMigrationBalance = 0L
        var earliestMigrationAt: Long? = null
        var earliestMigrationAmount = 0L
        var ledgerOverflow = false
        payload.coinLedgerEntries.forEachIndexed { index, entry ->
            val path = "coinLedgerEntries[$index]"
            if (entry.id.isBlank()) issue("$path.id", "must not be blank")
            else validUuid("$path.id", entry.id)
            validText("$path.type", entry.type)
            validText("$path.note", entry.note)
            validDate("$path.createdAt", entry.createdAt)
            if (!ledgerOverflow) {
                if (entry.type == "migration") {
                    runCatching { BackupDateCodec.parse(entry.createdAt) }.getOrNull()?.let { createdAt ->
                        if (earliestMigrationAt == null || createdAt < requireNotNull(earliestMigrationAt)) {
                            earliestMigrationAt = createdAt
                            earliestMigrationAmount = entry.amount.toLong()
                        }
                    }
                } else {
                    nonMigrationBalance = try {
                        Math.addExact(nonMigrationBalance, entry.amount.toLong())
                    } catch (_: ArithmeticException) {
                        ledgerOverflow = true
                        nonMigrationBalance
                    }
                }
            }
        }
        val ledgerBalance = try {
            Math.addExact(nonMigrationBalance, earliestMigrationAmount)
        } catch (_: ArithmeticException) {
            ledgerOverflow = true
            nonMigrationBalance
        }
        if (ledgerOverflow || ledgerBalance !in 0L..Int.MAX_VALUE.toLong()) {
            issue("coinLedgerEntries", "must fold to a balance between 0 and ${Int.MAX_VALUE}")
        }

        if (payload.shopPurchaseRecords.size > MAX_COLLECTION_RECORDS) {
            issue("shopPurchaseRecords", "contains too many records")
        }
        duplicateIds("shopPurchaseRecords", payload.shopPurchaseRecords.map { it.id })
        payload.shopPurchaseRecords.forEachIndexed { index, purchase ->
            val path = "shopPurchaseRecords[$index]"
            if (purchase.id.isBlank()) issue("$path.id", "must not be blank")
            else validUuid("$path.id", purchase.id)
            if (purchase.quantity < 1) issue("$path.quantity", "must be at least 1")
            if (purchase.unitCoinCost < 0) issue("$path.unitCoinCost", "must not be negative")
            if (purchase.totalCoinCost < 0) issue("$path.totalCoinCost", "must not be negative")
            validText("$path.productType", purchase.productType)
            validText("$path.productID", purchase.productID)
            validText("$path.productName", purchase.productName)
            validText("$path.currency", purchase.currency)
            validText("$path.status", purchase.status)
            validText("$path.source", purchase.source)
            validDate("$path.purchasedAt", purchase.purchasedAt)
            validDate("$path.syncedAt", purchase.syncedAt)
        }

        payload.lastAuthProvider?.let { provider ->
            if (provider.isBlank()) issue("lastAuthProvider", "must not be blank")
            validText("lastAuthProvider", provider, MAX_AUTH_PROVIDER_LENGTH)
        }

        if (issues.isNotEmpty()) throw BackupValidationException(issues)
    }

    private fun validateNiuMaConfig(
        config: BackupNiuMaConfig,
        issue: (String, String) -> Unit,
        validText: (String, String, Int) -> Unit,
    ) {
        if (config.payMode !in 0..1) issue("niuMaConfig.payMode", "must be 0 or 1")
        if (!config.monthlySalary.isFinite() || config.monthlySalary < 0) {
            issue("niuMaConfig.monthlySalary", "must be a finite non-negative number")
        }
        if (!config.hourlyRate.isFinite() || config.hourlyRate < 0) {
            issue("niuMaConfig.hourlyRate", "must be a finite non-negative number")
        }
        if (config.startMinutes !in MINUTES_IN_DAY) issue("niuMaConfig.startMinutes", "must be 0...1439")
        if (config.endMinutes !in MINUTES_IN_DAY) issue("niuMaConfig.endMinutes", "must be 0...1439")
        if (config.workDays.any { it !in 1..7 }) issue("niuMaConfig.workDays", "contains an invalid weekday")
        if (config.paydayDays.any { it !in 1..30 }) issue("niuMaConfig.paydayDays", "contains an invalid day")
        if (config.lunchMinutes !in 0..1_440) issue("niuMaConfig.lunchMinutes", "must be 0...1440")
        if (config.monthlyScheduleMode !in 0..1) {
            issue("niuMaConfig.monthlyScheduleMode", "must be 0 or 1")
        }
        validText("niuMaConfig.currencySymbol", config.currencySymbol, 32)
        config.customRestDays.forEachIndexed { index, day ->
            if (!isDateKey(day)) issue("niuMaConfig.customRestDays[$index]", "must use a valid yyyy-MM-dd date")
        }
        duplicateIdsForConfig(config, issue)
        config.shifts.forEachIndexed { index, shift ->
            val path = "niuMaConfig.shifts[$index]"
            if (shift.id.isBlank()) issue("$path.id", "must not be blank")
            if (shift.startMinutes !in MINUTES_IN_DAY) issue("$path.startMinutes", "must be 0...1439")
            if (shift.endMinutes !in MINUTES_IN_DAY) issue("$path.endMinutes", "must be 0...1439")
            validText("$path.name", shift.name, MAX_TEXT_LENGTH)
            validText("$path.colorHex", shift.colorHex, 32)
            validText("$path.icon", shift.icon, 128)
        }
        config.dayShifts.forEach { (day, shiftId) ->
            if (!isDateKey(day)) issue("niuMaConfig.dayShifts", "contains an invalid date key: $day")
            if (shiftId.isBlank()) issue("niuMaConfig.dayShifts[$day]", "shift id must not be blank")
        }
    }

    private fun duplicateIdsForConfig(config: BackupNiuMaConfig, issue: (String, String) -> Unit) {
        val duplicate = config.shifts.groupingBy { it.id }.eachCount().entries.firstOrNull { it.value > 1 }?.key
        if (duplicate != null) issue("niuMaConfig.shifts", "contains a duplicate id: $duplicate")
    }

    private fun normalize(payload: BackupPayload): BackupPayload = payload.copy(
        stampRecords = payload.stampRecords.mapIndexed { index, stamp ->
            stamp.copy(id = normalizedUuid(stamp.id, "stamp|$index|${stamp.date}|${stamp.reason}|${stamp.roundNumber}"))
        },
        resignationPlans = payload.resignationPlans.mapIndexed { planIndex, plan ->
            plan.copy(
                todoItems = plan.todoItems.mapIndexed { index, todo ->
                    todo.copy(id = normalizedUuid(todo.id, "todo|$planIndex|active|$index|${todo.title}"))
                },
                completedTodoItems = plan.completedTodoItems.mapIndexed { index, todo ->
                    todo.copy(id = normalizedUuid(todo.id, "todo|$planIndex|completed|$index|${todo.title}"))
                },
            )
        },
        niuMaConfig = payload.niuMaConfig.copy(
            shifts = payload.niuMaConfig.shifts.mapIndexed { index, shift ->
                shift.copy(id = normalizedIdentifier(shift.id, "shift|$index|${shift.name}|${shift.startMinutes}"))
            },
        ),
        coinLedgerEntries = payload.coinLedgerEntries.mapIndexed { index, entry ->
            entry.copy(id = normalizedUuid(entry.id, "ledger|$index|${entry.createdAt}|${entry.amount}|${entry.type}"))
        },
        shopPurchaseRecords = payload.shopPurchaseRecords.mapIndexed { index, purchase ->
            purchase.copy(id = normalizedUuid(purchase.id, "purchase|$index|${purchase.purchasedAt}|${purchase.productID}"))
        },
        lastAuthProvider = payload.lastAuthProvider?.trim()?.takeIf { it.isNotEmpty() },
    )

    private fun normalizedUuid(value: String, seed: String): String {
        if (value.isBlank()) return UUID.nameUUIDFromBytes(seed.toByteArray(StandardCharsets.UTF_8)).toString()
        val canonical = runCatching { UUID.fromString(value).toString() }.getOrNull()
        return canonical?.takeIf { it.equals(value, ignoreCase = true) } ?: value
    }

    /** NiuMaShift.id 在 iOS 是 String，不強制 UUID；只在缺少時補穩定 id。 */
    private fun normalizedIdentifier(value: String, seed: String): String =
        value.takeIf { it.isNotBlank() } ?: UUID.nameUUIDFromBytes(seed.toByteArray(StandardCharsets.UTF_8)).toString()

    private fun isDateKey(value: String): Boolean = DATE_KEY.matches(value) &&
        runCatching { LocalDate.parse(value) }.isSuccess

    companion object {
        private const val MAX_JSON_CHARS = 16 * 1_048_576
        private const val MAX_REPORTED_ISSUES = 50
        private const val MAX_COLLECTION_RECORDS = 250_000
        private const val MAX_TODO_RECORDS = 10_000
        private const val MAX_TEXT_LENGTH = 10_000
        private const val MAX_AUTH_PROVIDER_LENGTH = 64
        private const val MAX_STAMPS_PER_CARD = 10_000
        private val MINUTES_IN_DAY = 0..1_439
        private val DATE_KEY = Regex("\\d{4}-\\d{2}-\\d{2}")
    }
}

data class BackupValidationIssue(val path: String, val message: String)

sealed class BackupException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

class BackupDecodingException(message: String, cause: Throwable? = null) : BackupException(message, cause)

class BackupEncodingException(message: String, cause: Throwable? = null) : BackupException(message, cause)

class BackupValidationException(val issues: List<BackupValidationIssue>) : BackupException(
    issues.joinToString(prefix = "Backup validation failed: ", separator = "; ") { "${it.path} ${it.message}" },
)
