package com.mindlyric.mindlyric0.data.local.db

import android.content.Context
import androidx.room.Room

object DbProvider {

    @Volatile
    private var INSTANCE: MindLyricDatabase? = null

    fun get(context: Context): MindLyricDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                MindLyricDatabase::class.java,
                "mindlyric.db"
            ).build().also { INSTANCE = it }
        }
    }
}