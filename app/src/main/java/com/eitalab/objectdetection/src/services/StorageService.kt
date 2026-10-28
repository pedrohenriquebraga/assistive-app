package com.eitalab.objectdetection.src.services

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class StorageService(activity: Activity) {
    private var sharedPref: SharedPreferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "@ODPrefs"
    }

    fun saveData(key: String, value: String) {
        sharedPref.edit {
            putString(key, value)
        }
    }

    fun getData(key: String, defaultValue: String = ""): String {
        return sharedPref.getString(key, defaultValue) ?: defaultValue
    }
}