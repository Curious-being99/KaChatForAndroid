package com.kachat.app.services.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kachat.app.models.PortfolioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDefinitionDao {
    @Query("SELECT * FROM portfolios WHERE walletAddress = :walletAddress ORDER BY sortOrder ASC")
    fun getPortfolios(walletAddress: String): Flow<List<PortfolioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(portfolio: PortfolioEntity)

    @Query("DELETE FROM portfolios WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM portfolios WHERE walletAddress = :walletAddress")
    suspend fun count(walletAddress: String): Int

    @Query("SELECT * FROM portfolios WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PortfolioEntity?
}
