package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Employee
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees WHERE registro = :registro LIMIT 1")
    suspend fun getEmployeeByRegistro(registro: String): Employee?

    @Query("SELECT * FROM employees ORDER BY nome ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(employees: List<Employee>)

    @androidx.room.Delete
    suspend fun deleteEmployee(employee: Employee)

    @Query("DELETE FROM employees WHERE registro = :registro")
    suspend fun deleteEmployeeByRegistro(registro: String)
}
