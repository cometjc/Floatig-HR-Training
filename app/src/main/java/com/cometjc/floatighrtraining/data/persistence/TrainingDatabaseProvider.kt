package com.cometjc.floatighrtraining.data.persistence

import android.content.Context
import androidx.room.Room

object TrainingDatabaseProvider {
    @Volatile
    private var instance: TrainingDatabase? = null

    fun get(context: Context): TrainingDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TrainingDatabase::class.java,
                "training.db"
            ).build().also { instance = it }
        }
    }
}
