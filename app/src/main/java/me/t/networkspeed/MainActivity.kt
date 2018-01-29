package me.t.networkspeed

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val fragmentManager = fragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.add(R.id.id_container, SettingsFragment())
        transaction.commit()
    }
}
