package com.onme.app

import android.Manifest
import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.onme.core.DrivingState

class MainActivity : ComponentActivity() {
    private var car: Car? = null
    private val onme get() = (application as OnmeApplication).onme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTaskDescription(android.app.ActivityManager.TaskDescription.Builder().setLabel("온미").setIcon(R.mipmap.ic_launcher).build())
        releaseOrientation(resources.configuration)
        watchDrivingState()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        savedInstanceState?.let { onme.restore(it.getString("profile"), it.getString("screen")) }
        setContent {
            OnmeUi(onme)
            androidx.compose.runtime.LaunchedEffect(onme.mapSet) { if (onme.mapSet > 0) besideMap() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        onme.profile?.let { outState.putString("profile", it.id); outState.putString("screen", onme.screen.name) }
    }

    override fun onStart() { super.onStart(); onme.onForeground() }
    override fun onStop() { onme.onBackground(); super.onStop() }

    private fun besideMap() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun releaseOrientation(c: Configuration) {
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && c.screenWidthDp < 1200)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); releaseOrientation(newConfig) }

    override fun onDestroy() {
        car?.disconnect()
        super.onDestroy()
    }

    /** 2.4/A5: the platform UX-restriction signal decides; no signal stays UNKNOWN (= driving). */
    private fun watchDrivingState() {
        try {
            val c = Car.createCar(this).also { car = it }
            val ux = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            fun set(restricted: Boolean) { onme.systemDriving = if (restricted) DrivingState.DRIVING else DrivingState.PARKED }
            set(ux.currentCarUxRestrictions.isRequiresDistractionOptimization)
            ux.registerListener { set(it.isRequiresDistractionOptimization) }
        } catch (e: Throwable) {
            Log.w("MainActivity", "CarUxRestrictions unavailable; driving state UNKNOWN", e)
        }
    }
}
