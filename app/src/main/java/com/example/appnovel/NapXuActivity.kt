package com.example.appnovel

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.appnovel.databinding.ActivityNapXuBinding

class NapXuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNapXuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNapXuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnBank.setOnClickListener {
            startActivity(
                Intent(this, ChonGoiActivity::class.java)
                    .putExtra("METHOD", "BANK")

            )
        }

        binding.btnMomo.setOnClickListener {
            startActivity(
                Intent(this, ChonGoiActivity::class.java)
                    .putExtra("METHOD", "MOMO")
            )
        }
    }
}