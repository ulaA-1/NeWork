package ru.social.nework.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.social.nework.dao.EventDao
import ru.social.nework.dao.EventRemoteKeyDao
import ru.social.nework.dao.JobDao
import ru.social.nework.dao.PostDao
import ru.social.nework.dao.PostRemoteKeyDao
import ru.social.nework.dao.UserDao
import ru.social.nework.dao.WallRemoteKeyDao
import ru.social.nework.entity.Converters
import ru.social.nework.entity.EventEntity
import ru.social.nework.entity.EventRemoteKeyEntity
import ru.social.nework.entity.JobEntity
import ru.social.nework.entity.PostEntity
import ru.social.nework.entity.PostRemoteKeyEntity
import ru.social.nework.entity.UserEntity
import ru.social.nework.entity.WallRemoteKeyEntity

@Database(
    entities = [
        PostEntity::class,
        PostRemoteKeyEntity::class,
        WallRemoteKeyEntity::class,
        EventEntity::class,
        EventRemoteKeyEntity::class,
        UserEntity::class,
        JobEntity::class
    ], version = 1, exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun postRemoteKeyDao(): PostRemoteKeyDao
    abstract fun wallRemoteKeyDao(): WallRemoteKeyDao
    abstract fun eventDao(): EventDao
    abstract fun eventRemoteKeyDao(): EventRemoteKeyDao
    abstract fun userDao(): UserDao
    abstract fun jobDao(): JobDao

    companion object {
        @Volatile
        private var instance: AppDb? = null

        fun getInstance(context: Context): AppDb {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, AppDb::class.java, "app.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}