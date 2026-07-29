package com.kachat.app.services.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kachat.app.models.ReactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReactionDao {

    /** Replaces any existing (targetTxId, walletAddress, reactorAddress) row - picking a new emoji on a message you've already reacted to overwrites your previous one. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReaction(reaction: ReactionEntity)

    @Query("DELETE FROM reactions WHERE targetTxId = :targetTxId AND walletAddress = :walletAddress AND reactorAddress = :reactorAddress")
    suspend fun deleteReaction(targetTxId: String, walletAddress: String, reactorAddress: String)

    @Query("SELECT * FROM reactions WHERE walletAddress = :walletAddress AND contactId = :contactId")
    fun getReactionsForContact(contactId: String, walletAddress: String): Flow<List<ReactionEntity>>

    @Query("SELECT * FROM reactions WHERE walletAddress = :walletAddress AND groupId = :groupId")
    fun getReactionsForGroup(groupId: String, walletAddress: String): Flow<List<ReactionEntity>>

    @Query("SELECT * FROM reactions WHERE targetTxId = :targetTxId AND walletAddress = :walletAddress AND reactorAddress = :reactorAddress LIMIT 1")
    suspend fun getReaction(targetTxId: String, walletAddress: String, reactorAddress: String): ReactionEntity?

    @Query("DELETE FROM reactions WHERE walletAddress = :walletAddress AND contactId = :contactId")
    suspend fun deleteAllForContact(contactId: String, walletAddress: String)

    @Query("DELETE FROM reactions WHERE walletAddress = :walletAddress AND groupId = :groupId")
    suspend fun deleteAllForGroup(groupId: String, walletAddress: String)

    @Query("DELETE FROM reactions WHERE walletAddress = :walletAddress")
    suspend fun deleteAllForWallet(walletAddress: String)
}
