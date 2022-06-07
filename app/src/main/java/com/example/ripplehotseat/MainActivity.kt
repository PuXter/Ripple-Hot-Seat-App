package com.example.ripplehotseat

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.nfc.Tag
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.TextureView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.*
import com.example.ripplehotseat.databinding.ActivityMainBinding
import org.altbeacon.beacon.*
import org.json.JSONObject
import java.lang.Exception
import java.util.*

class MainActivity : AppCompatActivity(), BeaconConsumer{

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var beaconManager: BeaconManager
    private lateinit var requestQueue: RequestQueue
    private lateinit var locationManager: LocationManager
    private var response: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        checkForPermissions()

        beaconManager = BeaconManager.getInstanceForApplication(this)

        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))

        beaconManager.bind(this)

        requestQueue = RequestQueue(DiskBasedCache(cacheDir,1024*1024), BasicNetwork((HurlStack()))).apply { start() }
//        val request = StringRequest(Request.Method.GET, "https://jsonplaceholder.typicode.com/users", {
//            response -> Log.i("TAG", response)
//        }, { error -> Log.e("resError", error.toString())})
//        requestQueue.add(request)
    }

    fun sendName(name: String){
        val jsonObject = JSONObject()
        jsonObject.put("Name", name)
        val request = JsonObjectRequest(Request.Method.POST, "", jsonObject, {
            response -> Log.i("Git","Super")
            this.response = response.getString("UUID")
            //sprawdzić co się stanie
            beaconManager.bind(this)
        }, {
            error -> Log.e("Error", "Cos nie tak")
        })
        requestQueue.add(request)
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.unbind(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onBeaconServiceConnect() {
        val TAG = "BeaconsEverywhere"
//        val region: Region = Region("MyBeacons", Identifier.parse(response!!), null, null)
        val region: Region = Region("MyBeac", Identifier.parse("d4070339-6da4-4e50-a375-bade13be6daa"), null, null)
        beaconManager.setMonitorNotifier(object: MonitorNotifier{
            override fun didEnterRegion(region: Region) {
                try {
                    Log.d(TAG, "didEnterRegion")
                    beaconManager.startRangingBeaconsInRegion(region)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun didExitRegion(region: Region) {
                try {
                    Log.d(TAG, "didExitRegion")
                    beaconManager.stopRangingBeaconsInRegion(region)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun didDetermineStateForRegion(i: Int, region: Region) {

            }
        })
        beaconManager.setRangeNotifier(object: RangeNotifier{
            override fun didRangeBeaconsInRegion(beacons: Collection<Beacon>, region: Region) {
                for (oneBeacon: Beacon in beacons){
                    Log.d(TAG, "distance: " + oneBeacon.distance + " id: " + oneBeacon.id1 + "/" + oneBeacon.id2 + "/" + oneBeacon.id3)
                }
            }
        })
        try {
            beaconManager.startMonitoringBeaconsInRegion(region)
        }catch (e: RemoteException){
            e.printStackTrace()
            Log.e("StartError", e.printStackTrace().toString())
        }
    }

    private val locationListener: LocationListener = object : LocationListener{
        override fun onLocationChanged(location: Location) {

        }

    }

    private fun checkForPermissions() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) &&
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

                AlertDialog.Builder(this)
                    .setTitle("Wymagana zgoda na lokalizacje.")
                    .setMessage("Ta aplikacja wymaga zgody na przetwarzanie lokalizacji.")
                    .setPositiveButton(
                        "OK"
                    ) { _ , _ ->
                        ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),99)
                    }
                    .create()
                    .show()
            }
            else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),99)
            }
        }
        else {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0,
                0F,
                locationListener
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.i("t","per")

        when (requestCode) {
            99 -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        0,
                        0F,
                        locationListener
                    )
                }
                else {
                    AlertDialog.Builder(this)
                        .setTitle("Brak zgody")
                        .setMessage("Ta aplikacja wymaga przetwarzania lokalizacji.")
                        .setPositiveButton(
                            "OK"
                        ) { _ , _ ->

                        }
                        .create()
                        .show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}