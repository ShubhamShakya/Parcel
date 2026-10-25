package com.parcel.data

import android.content.Context
import androidx.room.Room

fun createAndroidDatabase(context: Context): ParcelDatabase = buildDatabase(
    Room.databaseBuilder<ParcelDatabase>(context.applicationContext, context.getDatabasePath("parcel.db").absolutePath)
)
