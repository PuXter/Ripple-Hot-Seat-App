package com.example.ripplehotseat

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
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.StringRequest
import com.example.ripplehotseat.databinding.ActivityMainBinding
import org.altbeacon.beacon.*
import java.lang.Exception
import java.util.*

class MainActivity : AppCompatActivity(), BeaconConsumer{

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var beaconManager: BeaconManager
    private lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        beaconManager = BeaconManager.getInstanceForApplication(this)

        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"))

        beaconManager.bind(this)

        requestQueue = RequestQueue(DiskBasedCache(cacheDir,1024*1024), BasicNetwork((HurlStack()))).apply { start() }
        val request = StringRequest(Request.Method.GET, "https://jsonplaceholder.typicode.com/users", {
            response -> Log.i("TAG", response)
        }, { error -> Log.e("resError", error.toString())})
        requestQueue.add(request)
        findViewById<TextView>(R.id.textview_first).text = "cos"
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
//        val region: Region = Region("MyBeacons", Identifier.parse("F0018B9B-7509-4C31-A905-1A27D39C003C"), null, null)
        val region: Region = Region("MyBeacons", null, null, null)
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
//            beaconManager.getRegionViewModel(region).rangedBeacons.observe(this, rangingObserver)
//            beaconManager.startRangingBeacons(region)
//            beaconManager.reg
            beaconManager.startMonitoringBeaconsInRegion(region)
        }catch (e: RemoteException){
            e.printStackTrace()
            Log.e("StartError", e.printStackTrace().toString())
        }
    }
//    val rangingObserver = { beacons: Collection<Beacon> ->
//        Log.d("Count", "Ranged: ${beacons.count()} beacons")
//        for (beacon: Beacon in beacons) {
//            Log.d("Meters", "$beacon about ${beacon.distance} meters away")
//        }
//    }

}