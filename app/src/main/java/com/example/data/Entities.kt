package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "user_profile")
@Serializable
data class UserProfile(
    @PrimaryKey val id: Int = 0,
    val firstName: String = "",
    val lastName: String = "",
    val street: String = "",
    val city: String = "",
    val zipCode: String = "",
    val email: String = "",
    val phone: String = "",
    val aboutMe: String = ""
)

@Entity(tableName = "experience")
@Serializable
data class Experience(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val company: String,
    val duration: String,
    val description: String
)

@Entity(tableName = "skills")
@Serializable
data class Skill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "cover_letters")
@Serializable
data class CoverLetter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: Long = System.currentTimeMillis(),
    val content: String,
    val jobDescription: String = "",
    val scannedNotes: String = "",
    val template: String = "Classic",
    val attachments: String = "Lebenslauf, Zeugnisse",
    val recipientCompany: String = "",
    val recipientPerson: String = "",
    val recipientStreet: String = "",
    val recipientCityZip: String = "",
    val status: String = "Entwurf"
)

@Entity(tableName = "vault_documents")
@Serializable
data class VaultDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceType: String = "Notiz",
    val extractedText: String,
    val createdAt: Long = System.currentTimeMillis()
)
