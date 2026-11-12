package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {
    @Query("SELECT * FROM user_profile WHERE id = 0")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    @Query("SELECT * FROM experience")
    fun getAllExperience(): Flow<List<Experience>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExperience(experience: Experience)

    @Delete
    suspend fun deleteExperience(experience: Experience)

    @Query("SELECT * FROM skills")
    fun getAllSkills(): Flow<List<Skill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: Skill)

    @Delete
    suspend fun deleteSkill(skill: Skill)

    @Query("SELECT * FROM cover_letters ORDER BY date DESC")
    fun getAllCoverLetters(): Flow<List<CoverLetter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoverLetter(letter: CoverLetter)

    @Query("SELECT * FROM cover_letters WHERE id = :id")
    suspend fun getCoverLetterById(id: Long): CoverLetter?

    @Query("UPDATE cover_letters SET status = :status WHERE id = :id")
    suspend fun updateCoverLetterStatus(id: Long, status: String)

    @Delete
    suspend fun deleteCoverLetter(letter: CoverLetter)

    @Query("SELECT * FROM vault_documents ORDER BY createdAt DESC")
    fun getAllVaultDocuments(): Flow<List<VaultDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultDocument(doc: VaultDocument)

    @Delete
    suspend fun deleteVaultDocument(doc: VaultDocument)
}
