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
        // 🔥 কারেন্সির জন্য নতুন কী (Key) যুক্ত করা হলো
        val SELECTED_CURRENCY = stringPreferencesKey("selected_currency")
    }

    // ইউজার আগে ভাষা (এবং সেটআপ) সিলেক্ট করেছে কি না তা চেক করবে
    val isLanguageSelected: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LANGUAGE_SELECTED] ?: false
    }

    // 🔥 সিলেক্ট করা কারেন্সি সিম্বল (ডিফল্ট হিসেবে "৳" দেওয়া হলো)
    val selectedCurrency: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_CURRENCY] ?: "৳"
    }

    // ফার্স্ট টাইম ভাষা এবং কারেন্সি একসাথে সেভ করার জন্য
    suspend fun saveLanguageAndCurrency(languageCode: String, currencySymbol: String) {
        context.dataStore.edit { preferences ->
            preferences[IS_LANGUAGE_SELECTED] = true
            preferences[SELECTED_LANGUAGE] = languageCode
            preferences[SELECTED_CURRENCY] = currencySymbol
        }
        applyLanguage(languageCode)
    }

    // শুধু ভাষা সেভ করা (প্রোফাইল থেকে পরিবর্তনের জন্য)
    suspend fun saveLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[IS_LANGUAGE_SELECTED] = true
            preferences[SELECTED_LANGUAGE] = languageCode
        }
        applyLanguage(languageCode)
    }

    // 🔥 শুধু কারেন্সি সেভ করা (প্রোফাইল থেকে পরিবর্তনের জন্য)
    suspend fun saveCurrency(currencySymbol: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_CURRENCY] = currencySymbol
        }
    }

    // Android 13+ এবং নিচের ভার্সনে অ্যাপ রিস্টার্ট ছাড়াই ভাষা পরিবর্তন করার আধুনিক উপায়
    private fun applyLanguage(languageCode: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}