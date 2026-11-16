package com.example.data

import android.content.Context
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class NmsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("nms_data_prefs", Context.MODE_PRIVATE)
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    data class AppState(
        val adminUser: String = "admin",
        val adminPass: String = "admin123",
        val wards: List<String> = listOf("Medicine", "Surgery", "ICU", "Pediatrics", "Gynae & Obs"),
        val nurses: List<Nurse> = emptyList(),
        val dutyRequests: List<DutyRequest> = emptyList(),
        val leaves: List<LeaveRecord> = emptyList(),
        val leaveTypes: List<LeaveType> = listOf(
            LeaveType("CL", "Casual Leave", nurseSelfEntry = true),
            LeaveType("ML", "Maternity Leave"),
            LeaveType("EL", "Earn Leave"),
            LeaveType("RRL", "Rest & Recreation Leave")
        ),
        val attTypes: List<AttendanceType> = listOf(
            AttendanceType("P", "Present"),
            AttendanceType("A", "Absent"),
            AttendanceType("Late", "Late")
        ),
        val roster: Map<String, List<String>> = emptyMap(), // "date|ward|shift" -> [nurseIds]
        val attendance: Map<String, String> = emptyMap(), // "date|nurseId" -> "P"
        val arrivalTimes: Map<String, List<String>> = emptyMap(), // "date|nurseId" -> ["08:00", "14:15"]
        val acr: Map<String, String> = emptyMap(), // "year|nurseId" -> "Y" / "N"
        val complaints: List<Complaint> = emptyList(),
        val news: List<NewsItem> = listOf(
            NewsItem("n1", "**Welcome!** Welcome to NMS Mobile. **Important notices** will appear here."),
            NewsItem("n2", "**Government Holidays 2026:** Official Bangladesh Gazetted Holiday Calendar is loaded.")
        ),
        val notifications: List<NotificationItem> = emptyList(),
        val holidays: List<Holiday> = emptyList(),
        val clMax: Int = 20,
        val customSections: List<String> = emptyList(),
        val customFields: List<CustomField> = emptyList(),
        val footer: HospitalFooter = HospitalFooter(
            hospitalName = "General Hospital & Medical College",
            address = "Dhaka, Bangladesh",
            links = "Hospital Portal|https://dghs.gov.bd"
        ),
        val passwordResetRequests: List<PasswordResetRequest> = emptyList(),
        val currentUser: CurrentUser? = null
    )

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _syncStatus = MutableStateFlow(SupabaseSync.SyncStatus.IDLE)
    val syncStatus: StateFlow<SupabaseSync.SyncStatus> = _syncStatus.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val defaultHolidays = listOf(
        Holiday("2026-02-04", "Shab-e-Barat"),
        Holiday("2026-02-11", "National Election holiday"),
        Holiday("2026-02-12", "National Election holiday"),
        Holiday("2026-02-21", "Shaheed Dibosh & International Mother Language Day"),
        Holiday("2026-03-17", "Shab-e-Qadr"),
        Holiday("2026-03-18", "Eid-ul-Fitr holiday (executive order)"),
        Holiday("2026-03-19", "Eid-ul-Fitr holiday (executive order)"),
        Holiday("2026-03-20", "Jumatul Wida"),
        Holiday("2026-03-21", "Eid-ul-Fitr"),
        Holiday("2026-03-22", "Eid-ul-Fitr holiday (executive order)"),
        Holiday("2026-03-23", "Eid-ul-Fitr holiday (executive order)"),
        Holiday("2026-03-26", "Independence & National Day"),
        Holiday("2026-04-14", "Pohela Boishakh (Bengali New Year)"),
        Holiday("2026-05-01", "May Day / Buddha Purnima"),
        Holiday("2026-05-25", "Eid-ul-Adha holiday (executive order)"),
        Holiday("2026-05-26", "Eid-ul-Adha holiday (executive order)"),
        Holiday("2026-05-27", "Eid-ul-Adha (Eve)"),
        Holiday("2026-05-28", "Eid-ul-Adha"),
        Holiday("2026-05-29", "Eid-ul-Adha (2nd day)"),
        Holiday("2026-05-30", "Eid-ul-Adha (3rd day)"),
        Holiday("2026-05-31", "Eid-ul-Adha holiday (executive order)"),
        Holiday("2026-06-26", "Ashura"),
        Holiday("2026-08-05", "July Mass Uprising Day"),
        Holiday("2026-08-26", "Eid-e-Miladunnabi"),
        Holiday("2026-09-04", "Janmashtami"),
        Holiday("2026-10-20", "Durga Puja"),
        Holiday("2026-10-21", "Durga Puja (Vijaya Dashami)"),
        Holiday("2026-12-16", "Victory Day"),
        Holiday("2026-12-25", "Christmas Day")
    )

    private val sampleNurses = listOf(
        Nurse(
            id = "nurse_1",
            name = "Nasir Ahmed",
            father = "Abdul Latif",
            mother = "Rasheda Begum",
            dob = "1990-05-12",
            workplace = "Dhaka Medical College Hospital",
            phone = "01711000001",
            reg = "BNMC-84920",
            hris = "1001",
            joinDate = "2015-08-01",
            course = "B.Sc in Nursing",
            cat = "Nurse Incharge",
            ward = "Medicine",
            pass = "1234",
            status = "ok"
        ),
        Nurse(
            id = "nurse_2",
            name = "Naznin Akter",
            father = "Mokhlesur Rahman",
            mother = "Shamsun Nahar",
            dob = "1994-08-20",
            workplace = "Dhaka Medical College Hospital",
            phone = "01711000002",
            reg = "BNMC-91044",
            hris = "1002",
            joinDate = "2018-03-15",
            course = "Diploma in Nursing Science & Midwifery",
            cat = "Senior Staff Nurse",
            ward = "Medicine",
            pass = "1234",
            status = "ok"
        ),
        Nurse(
            id = "nurse_3",
            name = "Fatima Begum",
            father = "Kamal Uddin",
            mother = "Anwara Khatun",
            dob = "1997-11-04",
            workplace = "Dhaka Medical College Hospital",
            phone = "01711000003",
            reg = "BNMC-10382",
            hris = "1003",
            joinDate = "2021-01-10",
            course = "B.Sc in Nursing",
            cat = "Staff Nurse",
            ward = "Medicine",
            pass = "1234",
            status = "ok"
        ),
        Nurse(
            id = "nurse_4",
            name = "Sultana Razia",
            father = "Rafiqul Islam",
            mother = "Momena Begum",
            dob = "1985-02-14",
            workplace = "Dhaka Medical College Hospital",
            phone = "01711000004",
            reg = "BNMC-64719",
            hris = "1004",
            joinDate = "2010-09-01",
            course = "M.Sc in Nursing",
            cat = "Nursing Supervisor",
            ward = "Surgery",
            pass = "1234",
            status = "ok"
        ),
        Nurse(
            id = "nurse_5",
            name = "Jahanara Begum",
            father = "Abul Kashem",
            mother = "Salma Begum",
            dob = "1978-04-25",
            workplace = "Dhaka Medical College Hospital",
            phone = "01711000005",
            reg = "BNMC-32110",
            hris = "1005",
            joinDate = "2003-05-15",
            course = "M.Sc in Nursing",
            cat = "Nursing Superintendent",
            ward = "ICU",
            pass = "1234",
            status = "ok"
        )
    )

    init {
        loadData()
        syncFromCloud(pushIfEmpty = true)
    }

    private fun loadData() {
        val rawJson = prefs.getString("nms_json", null)
        if (rawJson == null) {
            val initial = AppState(
                nurses = sampleNurses,
                holidays = defaultHolidays,
                roster = createSampleRoster()
            )
            _state.value = initial
            val initialJson = serializeStateToJson(initial)
            prefs.edit().putString("nms_json", initialJson).apply()
            return
        }

        val preservedUser = _state.value.currentUser
        val parsed = parseJsonToState(rawJson, preservedUser)
        _state.value = parsed
    }

    private fun parseJsonToState(rawJson: String, preservedUser: CurrentUser?): AppState {
        return try {
            val root = JSONObject(rawJson)
            val adminObj = root.optJSONObject("admin")
            val adminU = adminObj?.optString("u", "admin") ?: "admin"
            val adminP = adminObj?.optString("p", "admin123") ?: "admin123"

            val wardsList = mutableListOf<String>()
            val wardsArr = root.optJSONArray("wards")
            if (wardsArr != null) {
                for (i in 0 until wardsArr.length()) wardsList.add(wardsArr.getString(i))
            } else {
                wardsList.addAll(listOf("Medicine", "Surgery", "ICU", "Pediatrics", "Gynae & Obs"))
            }

            val nursesList = mutableListOf<Nurse>()
            val nursesArr = root.optJSONArray("nurses")
            if (nursesArr != null) {
                for (i in 0 until nursesArr.length()) {
                    val no = nursesArr.getJSONObject(i)
                    val extraMap = mutableMapOf<String, String>()
                    val xo = no.optJSONObject("x")
                    if (xo != null) {
                        val keys = xo.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            extraMap[k] = xo.optString(k, "")
                        }
                    }

                    var pendingUp: NursePendingUpdate? = null
                    val po = no.optJSONObject("pend")
                    if (po != null) {
                        pendingUp = NursePendingUpdate(
                            name = po.optString("name", ""),
                            phone = po.optString("phone", ""),
                            cat = po.optString("cat", ""),
                            ward = po.optString("ward", ""),
                            father = po.optString("father", ""),
                            mother = po.optString("mother", ""),
                            dob = po.optString("dob", ""),
                            workplace = po.optString("workplace", ""),
                            reg = po.optString("reg", ""),
                            hris = po.optString("hris", ""),
                            joinDate = po.optString("join", ""),
                            course = po.optString("course", "")
                        )
                    }

                    nursesList.add(
                        Nurse(
                            id = no.optString("id", UUID.randomUUID().toString()),
                            name = no.optString("name", ""),
                            father = no.optString("father", ""),
                            mother = no.optString("mother", ""),
                            dob = no.optString("dob", ""),
                            workplace = no.optString("workplace", ""),
                            phone = no.optString("phone", ""),
                            reg = no.optString("reg", ""),
                            hris = no.optString("hris", ""),
                            joinDate = no.optString("join", ""),
                            course = no.optString("course", ""),
                            cat = no.optString("cat", "Staff Nurse"),
                            ward = no.optString("ward", ""),
                            pass = no.optString("pass", "1234"),
                            status = no.optString("st", "ok"),
                            pend = pendingUp,
                            extraFields = extraMap
                        )
                    )
                }
            }

            // Roster
            val rosterMap = mutableMapOf<String, List<String>>()
            val ro = root.optJSONObject("roster")
            if (ro != null) {
                val keys = ro.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val arr = ro.optJSONArray(k)
                    val idList = mutableListOf<String>()
                    if (arr != null) {
                        for (j in 0 until arr.length()) idList.add(arr.getString(j))
                    }
                    rosterMap[k] = idList
                }
            }

            // Duty Requests
            val rqList = mutableListOf<DutyRequest>()
            val rqArr = root.optJSONArray("rq")
            if (rqArr != null) {
                for (i in 0 until rqArr.length()) {
                    val qo = rqArr.getJSONObject(i)
                    rqList.add(
                        DutyRequest(
                            id = qo.optString("id", UUID.randomUUID().toString()),
                            type = qo.optString("type", "duty"),
                            nurseId = qo.optString("nurse", ""),
                            date = qo.optString("date", ""),
                            ward = qo.optString("ward", ""),
                            shift = qo.optString("shift", "M"),
                            stage = qo.optString("stage", "NI"),
                            toWard = qo.optString("to", "")
                        )
                    )
                }
            }

            // Leaves
            val leavesList = mutableListOf<LeaveRecord>()
            val lvArr = root.optJSONArray("leaves")
            if (lvArr != null) {
                for (i in 0 until lvArr.length()) {
                    val lo = lvArr.getJSONObject(i)
                    leavesList.add(
                        LeaveRecord(
                            id = lo.optString("id", UUID.randomUUID().toString()),
                            nurseId = lo.optString("nurse", ""),
                            type = lo.optString("type", "CL"),
                            from = lo.optString("from", ""),
                            to = lo.optString("to", ""),
                            reason = lo.optString("reason", ""),
                            stage = lo.optString("stage", "OK")
                        )
                    )
                }
            }

            // Leave Types
            val leaveTypesList = mutableListOf<LeaveType>()
            val ltArr = root.optJSONArray("leaveTypes")
            if (ltArr != null && ltArr.length() > 0) {
                for (i in 0 until ltArr.length()) {
                    val lto = ltArr.getJSONObject(i)
                    leaveTypesList.add(
                        LeaveType(
                            code = lto.optString("c", "CL"),
                            name = lto.optString("n", "Casual Leave"),
                            nurseSelfEntry = lto.optInt("ne", 0) == 1 || lto.optBoolean("ne", false)
                        )
                    )
                }
            } else {
                leaveTypesList.addAll(
                    listOf(
                        LeaveType("CL", "Casual Leave", nurseSelfEntry = true),
                        LeaveType("ML", "Maternity Leave"),
                        LeaveType("EL", "Earn Leave"),
                        LeaveType("RRL", "Rest & Recreation Leave")
                    )
                )
            }

            // Attendance Types
            val attTypesList = mutableListOf<AttendanceType>()
            val atArr = root.optJSONArray("attTypes")
            if (atArr != null && atArr.length() > 0) {
                for (i in 0 until atArr.length()) {
                    val ato = atArr.getJSONObject(i)
                    attTypesList.add(
                        AttendanceType(
                            code = ato.optString("c", "P"),
                            name = ato.optString("n", "Present")
                        )
                    )
                }
            } else {
                attTypesList.addAll(
                    listOf(
                        AttendanceType("P", "Present"),
                        AttendanceType("A", "Absent"),
                        AttendanceType("Late", "Late")
                    )
                )
            }

            // Attendance
            val attMap = mutableMapOf<String, String>()
            val attObj = root.optJSONObject("att")
            if (attObj != null) {
                val keys = attObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    attMap[k] = attObj.optString(k, "")
                }
            }

            // Arrival times
            val atmMap = mutableMapOf<String, List<String>>()
            val atmObj = root.optJSONObject("atm")
            if (atmObj != null) {
                val keys = atmObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val arr = atmObj.optJSONArray(k)
                    val tList = mutableListOf<String>()
                    if (arr != null) {
                        for (j in 0 until arr.length()) tList.add(arr.getString(j))
                    }
                    atmMap[k] = tList
                }
            }

            // ACR
            val acrMap = mutableMapOf<String, String>()
            val acrObj = root.optJSONObject("acr")
            if (acrObj != null) {
                val keys = acrObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    acrMap[k] = acrObj.optString(k, "")
                }
            }

            // Complaints
            val compList = mutableListOf<Complaint>()
            val compArr = root.optJSONArray("complaints")
            if (compArr != null) {
                for (i in 0 until compArr.length()) {
                    val co = compArr.getJSONObject(i)
                    val logsList = mutableListOf<ComplaintLog>()
                    val logArr = co.optJSONArray("log")
                    if (logArr != null) {
                        for (j in 0 until logArr.length()) {
                            val lo = logArr.getJSONObject(j)
                            logsList.add(
                                ComplaintLog(
                                    uid = lo.optString("uid", ""),
                                    byName = lo.optString("by", ""),
                                    role = lo.optString("role", ""),
                                    text = lo.optString("text", ""),
                                    action = lo.optString("act", ""),
                                    date = lo.optString("date", "")
                                )
                            )
                        }
                    }
                    compList.add(
                        Complaint(
                            id = co.optString("id", UUID.randomUUID().toString()),
                            byNurseId = co.optString("by", ""),
                            subject = co.optString("subject", ""),
                            text = co.optString("text", ""),
                            date = co.optString("date", ""),
                            status = co.optString("status", "open"),
                            level = co.optInt("lvl", 2),
                            logs = logsList
                        )
                    )
                }
            }

            // Notifications
            val notesList = mutableListOf<NotificationItem>()
            val notesArr = root.optJSONArray("notes")
            if (notesArr != null) {
                for (i in 0 until notesArr.length()) {
                    val no = notesArr.getJSONObject(i)
                    val idsList = mutableListOf<String>()
                    val idsArr = no.optJSONArray("ids")
                    if (idsArr != null) {
                        for (j in 0 until idsArr.length()) idsList.add(idsArr.getString(j))
                    }
                    notesList.add(
                        NotificationItem(
                            id = no.optString("id", UUID.randomUUID().toString()),
                            timestamp = no.optLong("ts", System.currentTimeMillis()),
                            text = no.optString("text", ""),
                            type = no.optString("type", "news"),
                            ward = no.optString("ward", ""),
                            nurseIds = idsList,
                            isAll = no.optBoolean("all", false),
                            by = no.optString("by", "")
                        )
                    )
                }
            }

            // Custom sections & fields
            val secsList = mutableListOf<String>()
            val secsArr = root.optJSONArray("secs")
            if (secsArr != null) {
                for (i in 0 until secsArr.length()) secsList.add(secsArr.getString(i))
            }

            val fieldsList = mutableListOf<CustomField>()
            val fieldsArr = root.optJSONArray("fields")
            if (fieldsArr != null) {
                for (i in 0 until fieldsArr.length()) {
                    val fo = fieldsArr.getJSONObject(i)
                    fieldsList.add(
                        CustomField(
                            id = fo.optString("id", ""),
                            label = fo.optString("label", ""),
                            section = fo.optString("sec", "Basic")
                        )
                    )
                }
            }

            // Footer
            val fo = root.optJSONObject("footer")
            val footer = HospitalFooter(
                hospitalName = fo?.optString("h", "") ?: "",
                address = fo?.optString("a", "") ?: "",
                links = fo?.optString("l", "") ?: ""
            )

            // News
            val newsList = mutableListOf<NewsItem>()
            val newsArr = root.optJSONArray("news")
            if (newsArr != null) {
                for (i in 0 until newsArr.length()) {
                    val no = newsArr.getJSONObject(i)
                    newsList.add(NewsItem(no.optString("id", ""), no.optString("text", "")))
                }
            }

            // Holidays
            val holList = mutableListOf<Holiday>()
            val holArr = root.optJSONArray("hol")
            if (holArr != null && holArr.length() > 0) {
                for (i in 0 until holArr.length()) {
                    val ho = holArr.getJSONObject(i)
                    holList.add(Holiday(ho.optString("date", ""), ho.optString("name", "")))
                }
            } else {
                holList.addAll(defaultHolidays)
            }

            // Password Reset Requests
            val pwResetsList = mutableListOf<PasswordResetRequest>()
            val pwArr = root.optJSONArray("pwResets")
            if (pwArr != null) {
                for (i in 0 until pwArr.length()) {
                    val po = pwArr.getJSONObject(i)
                    pwResetsList.add(
                        PasswordResetRequest(
                            id = po.optString("id", UUID.randomUUID().toString()),
                            nurseId = po.optString("nurse", ""),
                            name = po.optString("name", ""),
                            phone = po.optString("phone", ""),
                            hris = po.optString("hris", ""),
                            newPass = po.optString("pass", ""),
                            timestamp = po.optLong("ts", System.currentTimeMillis())
                        )
                    )
                }
            }

            AppState(
                adminUser = adminU,
                adminPass = adminP,
                wards = wardsList,
                nurses = nursesList,
                dutyRequests = rqList,
                leaves = leavesList,
                leaveTypes = leaveTypesList,
                attTypes = attTypesList,
                roster = rosterMap,
                attendance = attMap,
                arrivalTimes = atmMap,
                acr = acrMap,
                complaints = compList,
                news = newsList.ifEmpty {
                    listOf(NewsItem("n1", "**Welcome!** Welcome to NMS. **Important notices** will appear here."))
                },
                notifications = notesList,
                holidays = holList,
                clMax = root.optInt("clMax", 20),
                customSections = secsList,
                customFields = fieldsList,
                footer = footer,
                passwordResetRequests = pwResetsList,
                currentUser = preservedUser
            )
        } catch (e: Exception) {
            AppState(nurses = sampleNurses, holidays = defaultHolidays, currentUser = preservedUser)
        }
    }

    private fun createSampleRoster(): Map<String, List<String>> {
        val cal = Calendar.getInstance()
        val curYearMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
        val sample = mutableMapOf<String, List<String>>()
        sample["$curYearMonth-01|Medicine|M"] = listOf("nurse_1", "nurse_2")
        sample["$curYearMonth-01|Medicine|E"] = listOf("nurse_3")
        sample["$curYearMonth-02|Medicine|M"] = listOf("nurse_3")
        sample["$curYearMonth-02|Medicine|E"] = listOf("nurse_1")
        sample["$curYearMonth-03|Medicine|N"] = listOf("nurse_2")
        sample["$curYearMonth-04|Medicine|D"] = listOf("nurse_1")
        return sample
    }

    private fun serializeStateToJson(cur: AppState): String {
        val existingRaw = prefs.getString("nms_json", null)
        val root = try {
            if (existingRaw != null) JSONObject(existingRaw) else JSONObject()
        } catch (e: Exception) {
            JSONObject()
        }

        val adminObj = JSONObject().apply {
            put("u", cur.adminUser)
            put("p", cur.adminPass)
        }
        root.put("admin", adminObj)

        val wardsArr = JSONArray()
        cur.wards.forEach { wardsArr.put(it) }
        root.put("wards", wardsArr)

        val nursesArr = JSONArray()
        cur.nurses.forEach { n ->
            val no = JSONObject()
            no.put("id", n.id)
            no.put("name", n.name)
            no.put("father", n.father)
            no.put("mother", n.mother)
            no.put("dob", n.dob)
            no.put("workplace", n.workplace)
            no.put("phone", n.phone)
            no.put("reg", n.reg)
            no.put("hris", n.hris)
            no.put("join", n.joinDate)
            no.put("course", n.course)
            no.put("cat", n.cat)
            no.put("ward", n.ward)
            no.put("pass", n.pass)
            no.put("st", n.status)

            val xo = JSONObject()
            n.extraFields.forEach { (k, v) -> xo.put(k, v) }
            no.put("x", xo)

            if (n.pend != null) {
                val po = JSONObject()
                po.put("name", n.pend.name)
                po.put("phone", n.pend.phone)
                po.put("cat", n.pend.cat)
                po.put("ward", n.pend.ward)
                po.put("father", n.pend.father)
                po.put("mother", n.pend.mother)
                po.put("dob", n.pend.dob)
                po.put("workplace", n.pend.workplace)
                po.put("reg", n.pend.reg)
                po.put("hris", n.pend.hris)
                po.put("join", n.pend.joinDate)
                po.put("course", n.pend.course)
                no.put("pend", po)
            } else {
                no.remove("pend")
            }
            nursesArr.put(no)
        }
        root.put("nurses", nursesArr)

        val rosterObj = JSONObject()
        cur.roster.forEach { (k, list) ->
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            rosterObj.put(k, arr)
        }
        root.put("roster", rosterObj)

        val rqArr = JSONArray()
        cur.dutyRequests.forEach { q ->
            val qo = JSONObject()
            qo.put("id", q.id)
            qo.put("type", q.type)
            qo.put("nurse", q.nurseId)
            qo.put("date", q.date)
            qo.put("ward", q.ward)
            qo.put("shift", q.shift)
            qo.put("stage", q.stage)
            qo.put("to", q.toWard)
            rqArr.put(qo)
        }
        root.put("rq", rqArr)

        val lvArr = JSONArray()
        cur.leaves.forEach { l ->
            val lo = JSONObject()
            lo.put("id", l.id)
            lo.put("nurse", l.nurseId)
            lo.put("type", l.type)
            lo.put("from", l.from)
            lo.put("to", l.to)
            lo.put("reason", l.reason)
            lo.put("stage", l.stage)
            lvArr.put(lo)
        }
        root.put("leaves", lvArr)

        val ltArr = JSONArray()
        cur.leaveTypes.forEach { lt ->
            val lto = JSONObject()
            lto.put("c", lt.code)
            lto.put("n", lt.name)
            if (lt.nurseSelfEntry) lto.put("ne", 1)
            ltArr.put(lto)
        }
        root.put("leaveTypes", ltArr)

        val atArr = JSONArray()
        cur.attTypes.forEach { at ->
            val ato = JSONObject()
            ato.put("c", at.code)
            ato.put("n", at.name)
            atArr.put(ato)
        }
        root.put("attTypes", atArr)

        val attObj = JSONObject()
        cur.attendance.forEach { (k, v) -> attObj.put(k, v) }
        root.put("att", attObj)

        val atmObj = JSONObject()
        cur.arrivalTimes.forEach { (k, list) ->
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            atmObj.put(k, arr)
        }
        root.put("atm", atmObj)

        val acrObj = JSONObject()
        cur.acr.forEach { (k, v) -> acrObj.put(k, v) }
        root.put("acr", acrObj)

        val compArr = JSONArray()
        cur.complaints.forEach { c ->
            val co = JSONObject()
            co.put("id", c.id)
            co.put("by", c.byNurseId)
            co.put("subject", c.subject)
            co.put("text", c.text)
            co.put("date", c.date)
            co.put("status", c.status)
            co.put("lvl", c.level)
            val logArr = JSONArray()
            c.logs.forEach { l ->
                val lo = JSONObject()
                lo.put("uid", l.uid)
                lo.put("by", l.byName)
                lo.put("role", l.role)
                lo.put("text", l.text)
                lo.put("act", l.action)
                lo.put("date", l.date)
                logArr.put(lo)
            }
            co.put("log", logArr)
            compArr.put(co)
        }
        root.put("complaints", compArr)

        val notesArr = JSONArray()
        cur.notifications.forEach { n ->
            val no = JSONObject()
            no.put("id", n.id)
            no.put("ts", n.timestamp)
            no.put("text", n.text)
            no.put("type", n.type)
            no.put("ward", n.ward)
            val idsArr = JSONArray()
            n.nurseIds.forEach { idsArr.put(it) }
            no.put("ids", idsArr)
            no.put("all", n.isAll)
            no.put("by", n.by)
            notesArr.put(no)
        }
        root.put("notes", notesArr)

        val newsArr = JSONArray()
        cur.news.forEach { nw ->
            val no = JSONObject()
            no.put("id", nw.id)
            no.put("text", nw.text)
            newsArr.put(no)
        }
        root.put("news", newsArr)

        val holArr = JSONArray()
        cur.holidays.forEach { h ->
            val ho = JSONObject()
            ho.put("date", h.date)
            ho.put("name", h.name)
            holArr.put(ho)
        }
        root.put("hol", holArr)
        root.put("holSeed26", 1)
        root.put("doMoved", 1)
        root.put("clMax", cur.clMax)

        val secsArr = JSONArray()
        cur.customSections.forEach { secsArr.put(it) }
        root.put("secs", secsArr)

        val fieldsArr = JSONArray()
        cur.customFields.forEach { f ->
            val fo = JSONObject()
            fo.put("id", f.id)
            fo.put("label", f.label)
            fo.put("sec", if (f.section == "Basic") "" else f.section)
            fieldsArr.put(fo)
        }
        root.put("fields", fieldsArr)

        val footerObj = JSONObject().apply {
            put("h", cur.footer.hospitalName)
            put("a", cur.footer.address)
            put("l", cur.footer.links)
        }
        root.put("footer", footerObj)

        val pwResetsArr = JSONArray()
        cur.passwordResetRequests.forEach { pr ->
            val po = JSONObject().apply {
                put("id", pr.id)
                put("nurse", pr.nurseId)
                put("name", pr.name)
                put("phone", pr.phone)
                put("hris", pr.hris)
                put("pass", pr.newPass)
                put("ts", pr.timestamp)
            }
            pwResetsArr.put(po)
        }
        root.put("pwResets", pwResetsArr)

        return root.toString()
    }

    fun saveData(customState: AppState? = null) {
        val cur = customState ?: _state.value
        if (customState != null) {
            _state.value = customState
        }
        try {
            val json = serializeStateToJson(cur)
            prefs.edit().putString("nms_json", json).apply()
            pushToCloud(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshFromCloud() {
        syncFromCloud(pushIfEmpty = false)
    }

    private fun syncFromCloud(pushIfEmpty: Boolean) {
        syncScope.launch {
            syncMutex.withLock {
                _syncStatus.value = SupabaseSync.SyncStatus.SYNCING
                when (val result = SupabaseSync.fetchState()) {
                    is SupabaseSync.FetchResult.Found -> {
                        val cloudJson = result.dataJson
                        prefs.edit().putString("nms_json", cloudJson).apply()
                        val currentUser = _state.value.currentUser
                        _state.value = parseJsonToState(cloudJson, currentUser)
                        _syncStatus.value = SupabaseSync.SyncStatus.SYNCED
                        _syncMessage.value = "Cloud synced"
                    }
                    is SupabaseSync.FetchResult.Empty -> {
                        if (pushIfEmpty) {
                            val localJson = prefs.getString("nms_json", null)
                                ?: serializeStateToJson(_state.value)
                            val pushed = SupabaseSync.pushState(localJson)
                            if (pushed.isSuccess) {
                                _syncStatus.value = SupabaseSync.SyncStatus.SYNCED
                                _syncMessage.value = "Cloud initialized"
                            } else {
                                _syncStatus.value = SupabaseSync.SyncStatus.OFFLINE
                                _syncMessage.value = pushed.exceptionOrNull()?.message
                            }
                        } else {
                            _syncStatus.value = SupabaseSync.SyncStatus.SYNCED
                        }
                    }
                    is SupabaseSync.FetchResult.Error -> {
                        _syncStatus.value = SupabaseSync.SyncStatus.OFFLINE
                        _syncMessage.value = result.message
                    }
                }
            }
        }
    }

    private fun pushToCloud(json: String) {
        syncScope.launch {
            syncMutex.withLock {
                _syncStatus.value = SupabaseSync.SyncStatus.SYNCING
                val res = SupabaseSync.pushState(json)
                if (res.isSuccess) {
                    _syncStatus.value = SupabaseSync.SyncStatus.SYNCED
                    _syncMessage.value = "Saved to cloud"
                } else {
                    _syncStatus.value = SupabaseSync.SyncStatus.OFFLINE
                    _syncMessage.value = res.exceptionOrNull()?.message
                }
            }
        }
    }

    fun exportJsonString(): String {
        return prefs.getString("nms_json", "{}") ?: "{}"
    }

    fun importJsonString(jsonContent: String): Boolean {
        return try {
            JSONObject(jsonContent) // validate
            prefs.edit().putString("nms_json", jsonContent).apply()
            loadData()
            pushToCloud(jsonContent)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Auth actions
    fun loginAdmin(u: String, p: String): Boolean {
        val cur = _state.value
        if (u.trim() == cur.adminUser && p == cur.adminPass) {
            _state.value = cur.copy(currentUser = CurrentUser(role = "admin"))
            refreshFromCloud()
            return true
        }
        return false
    }

    fun loginNurse(phone: String, pass: String): String? {
        val cur = _state.value
        val nurse = cur.nurses.find { it.phone.trim() == phone.trim() && it.pass == pass }
            ?: return "Invalid phone number or password"

        if (nurse.status == "pend") {
            return "Registration is pending admin approval"
        }

        _state.value = cur.copy(currentUser = CurrentUser(role = "staff", nurseId = nurse.id))
        refreshFromCloud()
        return null
    }

    fun logout() {
        _state.value = _state.value.copy(currentUser = null)
    }

    // Password Restore / Reset
    fun restoreNursePasswordInstant(
        phone: String,
        hris: String,
        dobOrReg: String,
        newPass: String
    ): String? {
        val cleanPhone = phone.trim()
        val cleanHris = hris.trim()
        val cleanVerify = dobOrReg.trim()
        val cleanPass = newPass.trim()

        if (cleanPhone.isEmpty() || cleanHris.isEmpty()) {
            return "Phone number and HRIS ID are required."
        }
        if (cleanPass.length < 6) {
            return "New password must be at least 6 characters."
        }
        if (cleanPass == "1234" || cleanPass == "admin123") {
            return "Please choose a stronger private password."
        }

        val cur = _state.value
        val nurse = cur.nurses.find {
            it.phone.trim() == cleanPhone && it.hris.trim().equals(cleanHris, ignoreCase = true)
        } ?: return "No nurse account found matching that Phone No. and HRIS ID."

        if (nurse.status == "pend") {
            return "Your registration is still pending admin approval."
        }

        val hasDob = nurse.dob.trim().isNotEmpty()
        val hasReg = nurse.reg.trim().isNotEmpty()
        if (!hasDob && !hasReg) {
            return "No Date of Birth or BNMC Reg. No. is saved on your profile for instant verification. Please use 'Send Reset Request to Admin' instead."
        }
        if (cleanVerify.isEmpty()) {
            return "Please enter your Date of Birth (YYYY-MM-DD) or BNMC Reg. No. to verify your identity."
        }

        val dobMatches = hasDob && nurse.dob.trim() == cleanVerify
        val regMatches = hasReg && nurse.reg.trim().equals(cleanVerify, ignoreCase = true)
        if (!dobMatches && !regMatches) {
            return "Verification failed. Date of Birth or BNMC Reg. No. does not match your profile."
        }

        val updatedNurses = cur.nurses.map {
            if (it.id == nurse.id) it.copy(pass = cleanPass) else it
        }
        val updatedResets = cur.passwordResetRequests.filter { it.nurseId != nurse.id }
        _state.value = cur.copy(nurses = updatedNurses, passwordResetRequests = updatedResets)
        addNotification(
            text = "Password restored via self-verification for ${nurse.name}",
            type = "news",
            isAll = false
        )
        saveData()
        return null
    }

    fun requestPasswordReset(
        phone: String,
        hris: String,
        newPass: String
    ): String? {
        val cleanPhone = phone.trim()
        val cleanHris = hris.trim()
        val cleanPass = newPass.trim()

        if (cleanPhone.isEmpty() || cleanHris.isEmpty()) {
            return "Phone number and HRIS ID are required."
        }
        if (cleanPass.length < 6) {
            return "New password must be at least 6 characters."
        }
        if (cleanPass == "1234" || cleanPass == "admin123") {
            return "Please choose a stronger private password."
        }

        val cur = _state.value
        val nurse = cur.nurses.find {
            it.phone.trim() == cleanPhone && it.hris.trim().equals(cleanHris, ignoreCase = true)
        } ?: return "No nurse account found matching that Phone No. and HRIS ID."

        if (nurse.status == "pend") {
            return "Your registration is still pending admin approval."
        }

        val req = PasswordResetRequest(
            id = UUID.randomUUID().toString(),
            nurseId = nurse.id,
            name = nurse.name,
            phone = nurse.phone,
            hris = nurse.hris,
            newPass = cleanPass,
            timestamp = System.currentTimeMillis()
        )
        val updatedResets = cur.passwordResetRequests.filter { it.nurseId != nurse.id } + req
        _state.value = cur.copy(passwordResetRequests = updatedResets)
        addNotification(
            text = "Password reset requested by ${nurse.name} (${nurse.phone})",
            type = "news",
            isAll = false
        )
        saveData()
        return null
    }

    fun approvePasswordReset(requestId: String) {
        val cur = _state.value
        val req = cur.passwordResetRequests.find { it.id == requestId } ?: return
        val updatedNurses = cur.nurses.map {
            if (it.id == req.nurseId) it.copy(pass = req.newPass) else it
        }
        val updatedResets = cur.passwordResetRequests.filter { it.id != requestId }
        _state.value = cur.copy(nurses = updatedNurses, passwordResetRequests = updatedResets)
        addNotification(
            text = "Password reset approved for ${req.name}",
            type = "news",
            isAll = true
        )
        saveData()
    }

    fun rejectPasswordReset(requestId: String) {
        val cur = _state.value
        val updatedResets = cur.passwordResetRequests.filter { it.id != requestId }
        _state.value = cur.copy(passwordResetRequests = updatedResets)
        saveData()
    }

    // Nurse registrations
    fun registerNurse(nurse: Nurse): Boolean {
        val cur = _state.value
        if (cur.nurses.any { it.phone.trim() == nurse.phone.trim() }) return false
        val updated = cur.nurses + nurse.copy(status = "pend")
        _state.value = cur.copy(nurses = updated)
        addNotification(
            text = "New nurse registration: ${nurse.name} (${nurse.phone})",
            type = "news",
            isAll = false
        )
        saveData()
        return true
    }

    fun approveNurseRegistration(nurseId: String, assignedCat: String) {
        val cur = _state.value
        val updated = cur.nurses.map {
            if (it.id == nurseId) it.copy(status = "ok", cat = assignedCat) else it
        }
        _state.value = cur.copy(nurses = updated)
        addNotification("Nurse registration approved: ${getNurse(nurseId)?.name}", "news")
        saveData()
    }

    fun rejectNurseRegistration(nurseId: String) {
        val cur = _state.value
        val updated = cur.nurses.filter { it.id != nurseId }
        _state.value = cur.copy(nurses = updated)
        saveData()
    }

    fun saveNurse(nurse: Nurse) {
        val cur = _state.value
        val exists = cur.nurses.any { it.id == nurse.id }
        val updated = if (exists) {
            cur.nurses.map { if (it.id == nurse.id) nurse else it }
        } else {
            cur.nurses + nurse
        }
        _state.value = cur.copy(nurses = updated)
        saveData()
    }

    fun deleteNurse(nurseId: String) {
        val cur = _state.value
        val updatedNurses = cur.nurses.filter { it.id != nurseId }
        val updatedLeaves = cur.leaves.filter { it.nurseId != nurseId }
        val updatedRoster = cur.roster.mapValues { it.value.filter { id -> id != nurseId } }
        _state.value = cur.copy(
            nurses = updatedNurses,
            leaves = updatedLeaves,
            roster = updatedRoster
        )
        saveData()
    }

    fun requestProfileUpdate(nurseId: String, pend: NursePendingUpdate) {
        val cur = _state.value
        val updated = cur.nurses.map {
            if (it.id == nurseId) it.copy(pend = pend) else it
        }
        _state.value = cur.copy(nurses = updated)
        addNotification("Profile update requested by ${getNurse(nurseId)?.name}", "news")
        saveData()
    }

    fun approveProfileUpdate(nurseId: String) {
        val cur = _state.value
        val nurse = cur.nurses.find { it.id == nurseId } ?: return
        val p = nurse.pend ?: return
        val updatedNurse = nurse.copy(
            name = p.name.ifEmpty { nurse.name },
            phone = p.phone.ifEmpty { nurse.phone },
            cat = p.cat.ifEmpty { nurse.cat },
            ward = p.ward.ifEmpty { nurse.ward },
            father = p.father.ifEmpty { nurse.father },
            mother = p.mother.ifEmpty { nurse.mother },
            dob = p.dob.ifEmpty { nurse.dob },
            workplace = p.workplace.ifEmpty { nurse.workplace },
            reg = p.reg.ifEmpty { nurse.reg },
            hris = p.hris.ifEmpty { nurse.hris },
            joinDate = p.joinDate.ifEmpty { nurse.joinDate },
            course = p.course.ifEmpty { nurse.course },
            pend = null
        )
        val updatedNurses = cur.nurses.map { if (it.id == nurseId) updatedNurse else it }
        _state.value = cur.copy(nurses = updatedNurses)
        addNotification("Profile update approved for ${nurse.name}", "news")
        saveData()
    }

    fun rejectProfileUpdate(nurseId: String) {
        val cur = _state.value
        val updatedNurses = cur.nurses.map { if (it.id == nurseId) it.copy(pend = null) else it }
        _state.value = cur.copy(nurses = updatedNurses)
        saveData()
    }

    // Duty Roster Actions
    fun setRosterShift(ward: String, date: String, nurseId: String, shift: String) {
        val cur = _state.value
        val userRank = getCurrentRank()
        val key = "$date|$ward|$shift"

        if (shift.isNotEmpty()) {
            val hasOtherWard = cur.roster.entries.any {
                it.key.startsWith("$date|") && !it.key.startsWith("$date|$ward|") && it.value.contains(nurseId)
            }
            if (hasOtherWard) return
        }

        if (userRank >= 3 || cur.currentUser?.role == "admin") {
            val newRoster = cur.roster.toMutableMap()
            listOf("M", "E", "N", "D").forEach { s ->
                val k = "$date|$ward|$s"
                val list = (newRoster[k] ?: emptyList()).filter { it != nurseId }
                if (list.isEmpty()) newRoster.remove(k) else newRoster[k] = list
            }
            if (shift.isNotEmpty()) {
                val list = (newRoster[key] ?: emptyList()) + nurseId
                newRoster[key] = list.distinct()
            }
            val newRq = cur.dutyRequests.filterNot { it.date == date && it.nurseId == nurseId && it.ward == ward }
            _state.value = cur.copy(roster = newRoster, dutyRequests = newRq)
            addNotification("Duty set: ${getNurse(nurseId)?.name} - $date - ${NurseHierarchy.shiftLabel(shift)} ($ward)", "duty")
            saveData()
            return
        }

        val nextStage = if (userRank == 2) "SUP" else "NI"
        val existingRq = cur.dutyRequests.find { it.date == date && it.nurseId == nurseId && it.ward == ward && it.stage in listOf("NI", "SUP") }

        val newRqList = cur.dutyRequests.toMutableList()
        if (existingRq != null) {
            if (shift.isEmpty()) {
                newRqList.remove(existingRq)
            } else {
                val idx = newRqList.indexOf(existingRq)
                newRqList[idx] = existingRq.copy(shift = shift)
            }
        } else if (shift.isNotEmpty()) {
            newRqList.add(
                DutyRequest(
                    id = UUID.randomUUID().toString(),
                    nurseId = nurseId,
                    date = date,
                    ward = ward,
                    shift = shift,
                    stage = nextStage
                )
            )
        }

        _state.value = cur.copy(dutyRequests = newRqList)
        addNotification("Duty requested: ${getNurse(nurseId)?.name} - $date - ${NurseHierarchy.shiftLabel(shift)}", "duty")
        saveData()
    }

    fun approveDutyRequest(requestId: String, newShift: String? = null) {
        val cur = _state.value
        val req = cur.dutyRequests.find { it.id == requestId } ?: return
        val currentRank = getCurrentRank()
        val shiftToUse = newShift ?: req.shift

        if (req.type == "tr") {
            val nurse = getNurse(req.nurseId) ?: return
            val updatedNurse = nurse.copy(ward = req.toWard)
            val updatedNurses = cur.nurses.map { if (it.id == nurse.id) updatedNurse else it }
            val newRq = cur.dutyRequests.filter { it.id != requestId }
            _state.value = cur.copy(nurses = updatedNurses, dutyRequests = newRq)
            addNotification("Ward change approved: ${nurse.name} to ${req.toWard}", "duty")
            saveData()
            return
        }

        val isFinal = currentRank >= 3 || cur.currentUser?.role == "admin" || req.stage == "SUP"
        if (isFinal) {
            val newRoster = cur.roster.toMutableMap()
            listOf("M", "E", "N", "D").forEach { s ->
                val k = "${req.date}|${req.ward}|$s"
                val list = (newRoster[k] ?: emptyList()).filter { it != req.nurseId }
                if (list.isEmpty()) newRoster.remove(k) else newRoster[k] = list
            }
            val key = "${req.date}|${req.ward}|$shiftToUse"
            newRoster[key] = ((newRoster[key] ?: emptyList()) + req.nurseId).distinct()
            val newRq = cur.dutyRequests.filter { it.id != requestId }
            _state.value = cur.copy(roster = newRoster, dutyRequests = newRq)
            addNotification("Duty approved: ${getNurse(req.nurseId)?.name} - ${req.date} - ${NurseHierarchy.shiftLabel(shiftToUse)}", "duty")
        } else {
            val newRq = cur.dutyRequests.map {
                if (it.id == requestId) it.copy(shift = shiftToUse, stage = "SUP") else it
            }
            _state.value = cur.copy(dutyRequests = newRq)
            addNotification("Duty forwarded to Supervisor: ${getNurse(req.nurseId)?.name}", "duty")
        }
        saveData()
    }

    fun rejectDutyRequest(requestId: String) {
        val cur = _state.value
        val newRq = cur.dutyRequests.filter { it.id != requestId }
        _state.value = cur.copy(dutyRequests = newRq)
        saveData()
    }

    // Leave Actions
    fun requestLeave(nurseId: String, type: String, from: String, to: String, reason: String): String? {
        val cur = _state.value
        val initialStage = if (getCurrentRank() >= 4 || cur.currentUser?.role == "admin") "OK" else "NI"

        if (type == "CL") {
            val days = calculateDays(from, to)
            val usedCl = getUsedClDays(nurseId, from.take(4))
            if (usedCl + days > cur.clMax) {
                return "CL limit exceeded for ${from.take(4)}: Used $usedCl days + requested $days days > Max ${cur.clMax} days"
            }
        }

        val leave = LeaveRecord(
            id = UUID.randomUUID().toString(),
            nurseId = nurseId,
            type = type,
            from = from,
            to = to,
            reason = reason,
            stage = initialStage
        )
        _state.value = cur.copy(leaves = cur.leaves + leave)
        addNotification("Leave requested: ${getNurse(nurseId)?.name} ($type: $from to $to)", "leave")
        saveData()
        return null
    }

    fun approveLeave(leaveId: String) {
        val cur = _state.value
        val l = cur.leaves.find { it.id == leaveId } ?: return
        val rank = getCurrentRank()

        val nextStage = when {
            rank >= 4 || cur.currentUser?.role == "admin" -> "OK"
            l.stage == "NI" -> "SUP"
            l.stage == "SUP" -> "FIN"
            else -> "OK"
        }

        val updated = cur.leaves.map { if (it.id == leaveId) it.copy(stage = nextStage) else it }
        _state.value = cur.copy(leaves = updated)
        addNotification("Leave updated for ${getNurse(l.nurseId)?.name}: $nextStage", "leave")
        saveData()
    }

    fun rejectLeave(leaveId: String) {
        val cur = _state.value
        val updated = cur.leaves.map { if (it.id == leaveId) it.copy(stage = "REJ") else it }
        _state.value = cur.copy(leaves = updated)
        saveData()
    }

    fun deleteLeave(leaveId: String) {
        val cur = _state.value
        _state.value = cur.copy(leaves = cur.leaves.filter { it.id != leaveId })
        saveData()
    }

    // Attendance
    fun setAttendance(date: String, nurseId: String, status: String) {
        val cur = _state.value
        val key = "$date|$nurseId"
        val newAtt = cur.attendance.toMutableMap()
        if (status.isEmpty()) newAtt.remove(key) else newAtt[key] = status
        _state.value = cur.copy(attendance = newAtt)
        saveData()
    }

    fun addArrivalTime(date: String, nurseId: String, time: String) {
        val cur = _state.value
        val key = "$date|$nurseId"
        val existing = cur.arrivalTimes[key] ?: emptyList()
        val newAtm = cur.arrivalTimes.toMutableMap()
        newAtm[key] = existing + time
        _state.value = cur.copy(arrivalTimes = newAtm)
        saveData()
    }

    fun removeArrivalTime(date: String, nurseId: String, index: Int) {
        val cur = _state.value
        val key = "$date|$nurseId"
        val existing = cur.arrivalTimes[key] ?: return
        if (index in existing.indices) {
            val updated = existing.toMutableList().apply { removeAt(index) }
            val newAtm = cur.arrivalTimes.toMutableMap()
            if (updated.isEmpty()) newAtm.remove(key) else newAtm[key] = updated
            _state.value = cur.copy(arrivalTimes = newAtm)
            saveData()
        }
    }

    // ACR
    fun setAcr(year: String, nurseId: String, status: String) {
        val cur = _state.value
        val key = "$year|$nurseId"
        val newAcr = cur.acr.toMutableMap()
        if (status.isEmpty()) newAcr.remove(key) else newAcr[key] = status
        _state.value = cur.copy(acr = newAcr)
        saveData()
    }

    // Complaints
    fun submitComplaint(subject: String, text: String) {
        val cur = _state.value
        val nurseId = cur.currentUser?.nurseId ?: return
        val complaint = Complaint(
            id = UUID.randomUUID().toString(),
            byNurseId = nurseId,
            subject = subject,
            text = text,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            status = "open",
            level = getCurrentRank() + 1
        )
        _state.value = cur.copy(complaints = cur.complaints + complaint)
        addNotification("New complaint submitted: $subject", "complaint")
        saveData()
    }

    fun actOnComplaint(complaintId: String, replyText: String, forward: Boolean) {
        val cur = _state.value
        val comp = cur.complaints.find { it.id == complaintId } ?: return
        val actorName = if (cur.currentUser?.role == "admin") "Admin" else getNurse(cur.currentUser?.nurseId ?: "")?.name ?: ""
        val actorRole = if (cur.currentUser?.role == "admin") "Admin" else getNurse(cur.currentUser?.nurseId ?: "")?.cat ?: ""

        val newLog = ComplaintLog(
            uid = cur.currentUser?.nurseId ?: "admin",
            byName = actorName,
            role = actorRole,
            text = replyText,
            action = if (forward) "Forwarded" else "Closed",
            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )

        val updatedLogs = comp.logs + newLog
        val updatedComp = if (forward) {
            comp.copy(level = comp.level + 1, logs = updatedLogs.toMutableList())
        } else {
            comp.copy(status = "closed", logs = updatedLogs.toMutableList())
        }

        val updatedList = cur.complaints.map { if (it.id == complaintId) updatedComp else it }
        _state.value = cur.copy(complaints = updatedList)
        saveData()
    }

    // Wards
    fun addWard(ward: String) {
        val cur = _state.value
        if (ward.isNotBlank() && !cur.wards.contains(ward.trim())) {
            _state.value = cur.copy(wards = cur.wards + ward.trim())
            saveData()
        }
    }

    fun renameWard(oldName: String, newName: String) {
        val cur = _state.value
        val updatedWards = cur.wards.map { if (it == oldName) newName else it }
        val updatedNurses = cur.nurses.map { if (it.ward == oldName) it.copy(ward = newName) else it }
        val updatedRoster = mutableMapOf<String, List<String>>()
        cur.roster.forEach { (k, v) ->
            val parts = k.split("|")
            if (parts.size == 3 && parts[1] == oldName) {
                updatedRoster["${parts[0]}|$newName|${parts[2]}"] = v
            } else {
                updatedRoster[k] = v
            }
        }
        _state.value = cur.copy(wards = updatedWards, nurses = updatedNurses, roster = updatedRoster)
        saveData()
    }

    fun deleteWard(ward: String) {
        val cur = _state.value
        val updatedWards = cur.wards.filter { it != ward }
        val updatedNurses = cur.nurses.map { if (it.ward == ward) it.copy(ward = "") else it }
        _state.value = cur.copy(wards = updatedWards, nurses = updatedNurses)
        saveData()
    }

    // News
    fun addNews(text: String) {
        val cur = _state.value
        val item = NewsItem(UUID.randomUUID().toString(), text)
        _state.value = cur.copy(news = cur.news + item)
        addNotification("Announcement: $text", "news")
        saveData()
    }

    fun deleteNews(id: String) {
        val cur = _state.value
        _state.value = cur.copy(news = cur.news.filter { it.id != id })
        saveData()
    }

    // Holidays
    fun addHoliday(date: String, name: String) {
        val cur = _state.value
        if (cur.holidays.any { it.date == date }) return
        val list = (cur.holidays + Holiday(date, name)).sortedBy { it.date }
        _state.value = cur.copy(holidays = list)
        saveData()
    }

    fun deleteHoliday(date: String) {
        val cur = _state.value
        _state.value = cur.copy(holidays = cur.holidays.filter { it.date != date })
        saveData()
    }

    fun reload2026Holidays() {
        val cur = _state.value
        val combined = (cur.holidays + defaultHolidays).distinctBy { it.date }.sortedBy { it.date }
        _state.value = cur.copy(holidays = combined)
        saveData()
    }

    // Helpers
    fun getNurse(id: String): Nurse? = _state.value.nurses.find { it.id == id }

    fun getCurrentRank(): Int {
        val cur = _state.value
        if (cur.currentUser?.role == "admin") return 9
        val nurse = getNurse(cur.currentUser?.nurseId ?: "") ?: return 1
        return NurseHierarchy.getRank(nurse.cat, false)
    }

    fun calculateDays(from: String, to: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d1 = sdf.parse(from)?.time ?: 0L
            val d2 = sdf.parse(to)?.time ?: 0L
            val diff = ((d2 - d1) / (1000 * 60 * 60 * 24)).toInt() + 1
            if (diff > 0) diff else 1
        } catch (e: Exception) {
            1
        }
    }

    fun getUsedClDays(nurseId: String, year: String): Int {
        return _state.value.leaves
            .filter { it.nurseId == nurseId && it.type == "CL" && it.stage != "REJ" && it.from.startsWith(year) }
            .sumOf { calculateDays(it.from, it.to) }
    }

    fun getDayOffAllowanceForMonth(yearMonth: String): Int {
        return try {
            val parts = yearMonth.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, 1)
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            var count = 0
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            for (day in 1..maxDays) {
                cal.set(year, month - 1, day)
                val dateStr = sdf.format(cal.time)
                val isFriday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                val isHoliday = _state.value.holidays.any { it.date == dateStr }
                if (isFriday || isHoliday) count++
            }
            count
        } catch (e: Exception) {
            4
        }
    }

    fun getDayOffUsedForMonth(nurseId: String, yearMonth: String): Int {
        var count = 0
        _state.value.roster.forEach { (k, list) ->
            if (k.startsWith(yearMonth) && k.endsWith("|D") && list.contains(nurseId)) {
                count++
            }
        }
        return count
    }

    private fun addNotification(text: String, type: String, isAll: Boolean = true) {
        val cur = _state.value
        val item = NotificationItem(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            text = text,
            type = type,
            isAll = isAll,
            by = cur.currentUser?.nurseId ?: "admin"
        )
        _state.value = cur.copy(notifications = (cur.notifications + item).takeLast(200))
    }
}
