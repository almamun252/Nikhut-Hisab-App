package com.almamun252.nikhuthisab.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore তৈরি করা হলো
val Context.dataStore by preferencesDataStore(name = "settings")

class LanguagePreferences(private val context: Context) {
    companion object {
        val IS_LANGUAGE_SELECTED = booleanPreferencesKey("is_language_selected")
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    }

    // ইউজার আগে ভাষা সিলেক্ট করেছে কি না তা চেক করবে
    val isLanguageSelected: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LANGUAGE_SELECTED] ?: false
    }

    // ভাষা সেভ করা এবং অ্যাপের সিস্টেম ল্যাঙ্গুয়েজ পরিবর্তন করা
    suspend fun saveLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[IS_LANGUAGE_SELECTED] = true
            preferences[SELECTED_LANGUAGE] = languageCode
        }

        // Android 13+ এবং নিচের ভার্সনে অ্যাপ রিস্টার্ট ছাড়াই ভাষা পরিবর্তন করার আধুনিক উপায়
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}