package br.com.tairoroberto.filesencryptfiap.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.reactivex.Flowable

/**
 * Created by tairo on 12/12/17 3:03 PM.
 */
@Dao
interface ItemsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(item: Item): Long

    @Query("DELETE FROM items")
    fun deleteAll()

    @Query("select * from items where id = :id")
    fun getByID(id: Int): Flowable<Item>
}