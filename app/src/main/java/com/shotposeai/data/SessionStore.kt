package com.shotposeai.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore("shotpose_session")

class SessionStore(private val context: Context) {
    suspend fun save(user: UserEntity, remember: Boolean) = context.sessionDataStore.edit {
        it[USER_ID] = user.id; it[NAME] = user.name; it[EMAIL] = user.email; it[REMEMBER] = remember
    }
    suspend fun current(): Session? {
        val p = context.sessionDataStore.data.first()
        val id = p[USER_ID] ?: return null
        return Session(id, p[NAME].orEmpty(), p[EMAIL].orEmpty(), p[REMEMBER] ?: false)
    }
    suspend fun clear() = context.sessionDataStore.edit { it.clear() }
    data class Session(val userId: Long, val name: String, val email: String, val remember: Boolean)
    private companion object {
        val USER_ID = longPreferencesKey("user_id"); val NAME = stringPreferencesKey("name")
        val EMAIL = stringPreferencesKey("email"); val REMEMBER = booleanPreferencesKey("remember")
    }
}
