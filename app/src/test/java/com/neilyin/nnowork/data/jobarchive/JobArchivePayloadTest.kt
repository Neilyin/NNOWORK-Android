package com.neilyin.nnowork.data.jobarchive

import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.treehole.PostgrestError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** §19.4：payload 欄位與 iOS JobArchivePayload 一致（camelCase、ISO-8601 無毫秒、寬鬆解碼）。 */
class JobArchivePayloadTest {
    /** iOS JobArchiveCoding.encoder（.iso8601、.sortedKeys）會輸出的樣子。 */
    private val iosPayload = """
        {"cardCount":3,"companyName":"某某科技","createdAt":"2026-10-05T08:30:00Z","endDate":"2026-10-05T08:30:00Z",
         "jobIndex":2,"receipt":{"companyName":"某某科技","completionDate":"2026-10-01T09:00:00Z",
         "firstStampDate":"2026-03-02T01:15:00Z","issuedAt":"2026-10-05T08:30:00Z","kind":"master",
         "savedAmount":12000.5,"totalDays":213,"totalStamps":30},
         "salary":{"currencySymbol":"NT$","endMinutes":1080,"hourlyManualMode":false,"hourlyRate":0,
         "monthlySalary":45000,"payMode":0,"startMinutes":540},
         "savedAmount":12000.5,"stampCount":30,
         "stamps":[{"date":"2026-03-02T01:15:00Z","isEasterEgg":false,"reason":"無效會議","roundNumber":1}],
         "startDate":"2026-03-02T01:15:00Z","targetAmount":100000,
         "todos":[{"isCompleted":false,"title":"更新履歷"},{"isCompleted":true,"title":"存錢"}],
         "topReasons":[{"count":12,"reason":"無效會議"}],"version":1,"futureField":{"x":1}}
    """.trimIndent()

    @Test
    fun `iOS payload decodes every field and ignores future keys`() {
        val payload = JobArchiveCoding.decodePayload(iosPayload)!!
        assertEquals(2, payload.jobIndex)
        assertEquals("某某科技", payload.companyName)
        assertEquals(JobReceiptSnapshot.KIND_MASTER, payload.receipt.kind)
        assertEquals(213, payload.receipt.totalDays)
        assertEquals(45000.0, payload.salary!!.monthlySalary, 0.0)
        assertEquals(1, payload.stamps.size)
        assertEquals(listOf(false, true), payload.todos.map { it.isCompleted })
        assertEquals(JobReasonCount("無效會議", 12), payload.topReasons.single())
    }

    @Test
    fun `missing optional fields get iOS defaults and createdAt falls back to endDate`() {
        val minimal = """
            {"endDate":"2026-10-05T08:30:00Z","receipt":{"kind":"resignation","companyName":"","completionDate":"2026-10-05T08:30:00Z",
             "totalDays":0,"totalStamps":0,"savedAmount":0,"issuedAt":"2026-10-05T08:30:00Z"}}
        """.trimIndent()
        val payload = JobArchiveCoding.decodePayload(minimal)!!
        assertEquals(1, payload.version)
        assertEquals(1, payload.jobIndex)
        assertEquals("", payload.companyName)
        assertNull(payload.startDate)
        assertNull(payload.salary)
        assertTrue(payload.stamps.isEmpty())
        assertEquals("2026-10-05T08:30:00Z", payload.createdAt)
    }

    @Test
    fun `endDate and receipt are required and bad dates are rejected`() {
        assertNull(JobArchiveCoding.decodePayload("""{"jobIndex":1}"""))
        assertNull(JobArchiveCoding.decodePayload("""{"endDate":"2026-10-05T08:30:00Z"}"""))
        assertNull(JobArchiveCoding.decodePayload(iosPayload.replace("\"endDate\":\"2026-10-05T08:30:00Z\"", "\"endDate\":\"yesterday\"")))
    }

    @Test
    fun `android encoding uses iOS camelCase keys and omits nil values`() {
        val payload = JobArchiveCoding.decodePayload(iosPayload)!!.copy(salary = null, startDate = null)
        @Suppress("UNCHECKED_CAST")
        val map = JsonProvider.moshi.adapter(Map::class.java).fromJson(JobArchiveCoding.encodePayload(payload)) as Map<String, Any?>
        assertEquals(
            setOf(
                "version", "jobIndex", "companyName", "endDate", "cardCount", "stampCount", "topReasons",
                "savedAmount", "targetAmount", "receipt", "stamps", "todos", "createdAt",
            ),
            map.keys,
        )
        @Suppress("UNCHECKED_CAST")
        val receipt = map["receipt"] as Map<String, Any?>
        assertEquals(
            setOf("kind", "companyName", "firstStampDate", "completionDate", "totalDays", "totalStamps", "savedAmount", "issuedAt"),
            receipt.keys,
        )
        assertFalse(JobArchiveCoding.encodePayload(payload).contains(".000"))
        assertEquals(payload, JobArchiveCoding.decodePayload(JobArchiveCoding.encodePayload(payload)))
    }

    @Test
    fun `remote rows skip a single undecodable payload instead of failing the batch`() {
        val json = """
            [{"id":"3F2B9C1E-0000-4000-8000-000000000001","payload":$iosPayload},
             {"id":"bad","payload":{"version":99}},
             {"id":"3f2b9c1e-0000-4000-8000-000000000002","payload":$iosPayload,"job_index":2}]
        """.trimIndent()
        val rows = JobArchiveCoding.decodeRemoteRows(json)
        assertEquals(
            listOf("3f2b9c1e-0000-4000-8000-000000000001", "3f2b9c1e-0000-4000-8000-000000000002"),
            rows.map { it.id },
        )
        assertEquals(emptyList<JobArchiveRemoteRow>(), JobArchiveCoding.decodeRemoteRows("[]"))
    }

    @Test
    fun `archive_my_job body matches db 031 parameters with explicit null start date`() {
        val payload = JobArchiveCoding.decodePayload(iosPayload)!!.copy(startDate = null)
        val body = JobArchiveWire.archiveRequestJson("3F2B9C1E-0000-4000-8000-00000000000A", payload)
        @Suppress("UNCHECKED_CAST")
        val map = JsonProvider.moshi.adapter(Map::class.java).fromJson(body) as Map<String, Any?>
        assertEquals(setOf("p_id", "p_job_index", "p_company_name", "p_start_date", "p_end_date", "p_payload"), map.keys)
        assertEquals("3f2b9c1e-0000-4000-8000-00000000000a", map["p_id"])
        assertTrue(map.containsKey("p_start_date"))
        assertNull(map["p_start_date"])
        assertEquals("2026-10-05T08:30:00Z", map["p_end_date"])
        assertTrue("p_payload must be a JSON object", map["p_payload"] is Map<*, *>)
        assertTrue(body.contains("\"p_job_index\":2"))
        assertTrue("payload ints stay ints", body.contains("\"jobIndex\":2"))
    }

    @Test
    fun `archive errors map missing backend to server not ready`() {
        assertTrue(JobArchiveErrors.map(404, PostgrestError(code = "PGRST202")) is JobTransitionException.ServerNotReady)
        assertTrue(
            JobArchiveErrors.map(400, PostgrestError(code = "42P01", message = "relation \"job_archives\" does not exist"))
                is JobTransitionException.ServerNotReady,
        )
        assertTrue(JobArchiveErrors.map(401, null) is JobTransitionException.NotSignedIn)
        assertTrue(JobArchiveErrors.map(400, PostgrestError(code = "22023", message = "payload must be an object")) is JobTransitionException.Server)
        assertNotNull(JobArchiveErrors.map(500, null).message)
    }
}
