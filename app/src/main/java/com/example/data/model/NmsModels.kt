package com.example.data.model

data class Nurse(
    val id: String,
    val name: String,
    val father: String = "",
    val mother: String = "",
    val dob: String = "",
    val workplace: String = "",
    val phone: String,
    val reg: String = "",
    val hris: String,
    val joinDate: String = "",
    val course: String = "",
    val cat: String = "Staff Nurse",
    val ward: String = "",
    val pass: String = "1234",
    val status: String = "ok", // "ok" or "pend"
    val pend: NursePendingUpdate? = null,
    val extraFields: Map<String, String> = emptyMap()
)

data class NursePendingUpdate(
    val name: String = "",
    val father: String = "",
    val mother: String = "",
    val dob: String = "",
    val workplace: String = "",
    val phone: String = "",
    val reg: String = "",
    val hris: String = "",
    val joinDate: String = "",
    val course: String = "",
    val cat: String = "",
    val ward: String = ""
)

data class DutyRequest(
    val id: String,
    val type: String = "duty", // "duty" or "tr"
    val nurseId: String,
    val date: String,
    val ward: String,
    var shift: String, // "M", "E", "N", "D"
    var stage: String, // "NI", "SUP", "OK", "REJ"
    val toWard: String = ""
)

data class LeaveRecord(
    val id: String,
    val nurseId: String,
    val type: String, // "CL", "ML", "EL", "RRL", etc.
    val from: String,
    val to: String,
    val reason: String = "",
    var stage: String // "NI", "SUP", "FIN", "OK", "REJ"
)

data class LeaveType(
    val code: String,
    val name: String,
    val nurseSelfEntry: Boolean = false
)

data class AttendanceType(
    val code: String,
    val name: String
)

data class Holiday(
    val date: String,
    val name: String
)

data class NewsItem(
    val id: String,
    val text: String
)

data class NotificationItem(
    val id: String,
    val timestamp: Long,
    val text: String,
    val type: String, // "news", "duty", "leave", "complaint"
    val ward: String = "",
    val nurseIds: List<String> = emptyList(),
    val isAll: Boolean = false,
    val by: String = ""
)

data class Complaint(
    val id: String,
    val byNurseId: String,
    val subject: String,
    val text: String,
    val date: String,
    var status: String = "open", // "open" or "closed"
    var level: Int = 2,
    val logs: MutableList<ComplaintLog> = mutableListOf()
)

data class ComplaintLog(
    val uid: String,
    val byName: String,
    val role: String,
    val text: String,
    val action: String,
    val date: String
)

data class CustomField(
    val id: String,
    val label: String,
    val section: String = "Basic"
)

data class HospitalFooter(
    val hospitalName: String = "",
    val address: String = "",
    val links: String = ""
)

data class CurrentUser(
    val role: String, // "admin" or "staff"
    val nurseId: String? = null
)

object NurseHierarchy {
    val CATEGORIES = listOf(
        "Staff Nurse",
        "Senior Staff Nurse",
        "Nurse Incharge",
        "Nursing Supervisor",
        "Deputy Nursing Superintendent",
        "Nursing Superintendent"
    )

    val RANKS = mapOf(
        "Staff Nurse" to 1,
        "Senior Staff Nurse" to 1,
        "Nurse Incharge" to 2,
        "Nursing Supervisor" to 3,
        "Deputy Nursing Superintendent" to 4,
        "Nursing Superintendent" to 5
    )

    fun getRank(category: String, isAdmin: Boolean): Int {
        if (isAdmin) return 9
        return RANKS[category] ?: 1
    }

    val SHIFTS = mapOf(
        "M" to "Morning (8am–2pm)",
        "E" to "Evening (2pm–8pm)",
        "N" to "Night (8pm–8am)",
        "D" to "Day Off (D/O)"
    )

    fun shiftLabel(s: String): String = if (s == "D") "D/O" else s
}
