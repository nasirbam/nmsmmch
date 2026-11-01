package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NmsRepository
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NmsViewModel(application: Application) : AndroidViewModel(application) {

    val repository = NmsRepository(application.applicationContext)
    val state = repository.state

    // Active Tab in Main Screen
    enum class MainTab(val title: String) {
        ROSTER("Duty Roster"),
        ATTENDANCE("Attendance"),
        LEAVE("Leave"),
        APPROVALS("Approvals"),
        MORE("More")
    }

    enum class MoreSubTab(val title: String) {
        NURSES("Nurses Directory"),
        PROFILE("My Profile"),
        ACR("ACR Evaluation"),
        COMPLAINTS("Complaint Box"),
        WARDS("Hospital Wards"),
        SETTINGS("System Settings"),
        WEB_VIEW("Web Version (NMS Online)")
    }

    private val _currentTab = MutableStateFlow(MainTab.ROSTER)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _currentMoreTab = MutableStateFlow<MoreSubTab?>(null)
    val currentMoreTab: StateFlow<MoreSubTab?> = _currentMoreTab.asStateFlow()

    // Selected Ward for views
    private val _selectedWard = MutableStateFlow("Medicine")
    val selectedWard: StateFlow<String> = _selectedWard.asStateFlow()

    // Selected Month for Roster (YYYY-MM)
    private val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.US)
    private val _selectedMonth = MutableStateFlow(sdfMonth.format(Date()))
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    // Selected Date for Daily Attendance & Leave (YYYY-MM-DD)
    private val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val _selectedDate = MutableStateFlow(sdfDay.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Notifications Dialog
    private val _showNotifications = MutableStateFlow(false)
    val showNotifications: StateFlow<Boolean> = _showNotifications.asStateFlow()

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
        if (tab != MainTab.MORE) {
            _currentMoreTab.value = null
        }
    }

    fun selectMoreSubTab(subTab: MoreSubTab?) {
        _currentMoreTab.value = subTab
    }

    fun setSelectedWard(ward: String) {
        _selectedWard.value = ward
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun toggleNotifications(show: Boolean) {
        _showNotifications.value = show
    }

    fun getPendingApprovalCount(): Int {
        val s = state.value
        val rank = repository.getCurrentRank()
        val isAdmin = s.currentUser?.role == "admin"
        val pendingRegistrations = if (isAdmin) s.nurses.count { it.status == "pend" } else 0
        val pendingUpdates = if (isAdmin) s.nurses.count { it.pend != null } else 0
        val pendingLeaves = s.leaves.count { it.stage in listOf("NI", "SUP", "FIN") }
        val pendingRoster = s.dutyRequests.count { it.stage in listOf("NI", "SUP") }
        return pendingRegistrations + pendingUpdates + pendingLeaves + pendingRoster
    }

    fun getUnreadNotificationCount(): Int {
        return state.value.notifications.size.coerceAtMost(99)
    }

    fun shiftPrevMonth() {
        try {
            val cal = Calendar.getInstance()
            val date = sdfMonth.parse(_selectedMonth.value) ?: Date()
            cal.time = date
            cal.add(Calendar.MONTH, -1)
            _selectedMonth.value = sdfMonth.format(cal.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shiftNextMonth() {
        try {
            val cal = Calendar.getInstance()
            val date = sdfMonth.parse(_selectedMonth.value) ?: Date()
            cal.time = date
            cal.add(Calendar.MONTH, 1)
            _selectedMonth.value = sdfMonth.format(cal.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
