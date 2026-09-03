package com.example.data.repository

import com.example.data.dao.EmployeeDao
import com.example.data.dao.OccurrenceDao
import com.example.data.model.Employee
import com.example.data.model.SafetyOccurrence
import kotlinx.coroutines.flow.Flow

class SafetyRepository(
    private val occurrenceDao: OccurrenceDao,
    private val employeeDao: EmployeeDao
) {
    val allOccurrences: Flow<List<SafetyOccurrence>> = occurrenceDao.getAllOccurrences()
    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees()

    suspend fun findEmployee(registro: String): Employee? {
        val trimmed = registro.trim()
        if (trimmed.isEmpty()) return null
        return employeeDao.getEmployeeByRegistro(trimmed)
    }

    suspend fun saveOccurrence(occurrence: SafetyOccurrence): Long {
        return occurrenceDao.insertOccurrence(occurrence)
    }

    suspend fun getUnsyncedOccurrences(): List<SafetyOccurrence> {
        return occurrenceDao.getUnsyncedOccurrences()
    }

    suspend fun updateSyncStatus(id: Long, synced: Boolean) {
        occurrenceDao.updateSyncStatus(id, synced)
    }

    suspend fun deleteOccurrence(occurrence: SafetyOccurrence) {
        occurrenceDao.deleteOccurrence(occurrence)
    }

    suspend fun clearAllOccurrences() {
        occurrenceDao.clearAllOccurrences()
    }

    suspend fun saveEmployee(employee: Employee) {
        employeeDao.insertEmployee(employee)
    }

    suspend fun saveEmployees(employees: List<Employee>) {
        employeeDao.insertAll(employees)
    }

    suspend fun deleteEmployee(employee: Employee) {
        employeeDao.deleteEmployee(employee)
    }

    suspend fun deleteEmployeeByRegistro(registro: String) {
        employeeDao.deleteEmployeeByRegistro(registro)
    }
}
