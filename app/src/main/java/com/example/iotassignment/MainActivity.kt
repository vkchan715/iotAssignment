package com.example.iotassignment

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.channels.Channel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor


class MainActivity : AppCompatActivity() {
    private var swOn:String =""
    private var swOff:String =""
    private var notifyOn: String=""
    private var relay=""
    lateinit var notificationManager : NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setTitle("Smart Air Conditioner")

        val ref2 = FirebaseDatabase.getInstance().getReference().child("PI_03_CONTROL")
        ref2.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot){
                relay = p0.child("relay").value.toString()// get relay
                if(relay == "0"){
                    btnSwitch.isChecked = false
                }
                else if(relay == "1"){
                    btnSwitch.isChecked = true
                }
            }
            override fun onCancelled(p0: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })
        getData()

        var handler = Handler()
        handler.postDelayed(object : Runnable{
           override fun run() {
                getData()
               handler.postDelayed(this,10000)
           }
        },10000)

        loadSaved()
        if(!swAuto.isChecked) {
            editSwOn.isEnabled = false
            editSwOff.isEnabled = false
            swAuto.text = "Off"
        }
        if(!swNotify.isChecked){
            editNotify.isEnabled =  false
            swNotify.text = "Off"
        }

        swAuto.setOnClickListener {
            setAuto()
        }
        swNotify.setOnClickListener {
           setNotify()
        }
        btnSave.setOnClickListener {
            save()
            getData()
        }

        btnClear.setOnClickListener {
            editSwOn.setText("0")
            editSwOff.setText("0")
            editNotify.setText("0")
            auto = "false"
            notify = "false"
            swAuto.isChecked = false
            editSwOn.isEnabled = false
            editSwOff.isEnabled = false
            swAuto.text = "Off"
            swNotify.isChecked = false
            editNotify.isEnabled =  false
            swNotify.text = "Off"
            save()
       }
    }
    private var auto: String = ""
    private var notify: String = ""

    private fun setAuto(){
        if(!swAuto.isChecked){
            editSwOn.isEnabled = false
            editSwOff.isEnabled = false
            swAuto.text = "Off"
            auto = "false"
        }
        else{
            editSwOn.isEnabled = true
            editSwOff.isEnabled = true
            swAuto.text = "On"
            auto = "true"
        }
    }

    private fun setNotify(){
        if(!swNotify.isChecked){
            editNotify.isEnabled =  false
            swNotify.text = "Off"
            notify = "false"
        }
        else{
            editNotify.isEnabled =  true
            swNotify.text ="On"
            notify = "true"
        }
    }

    private fun save(){
        savedPrefes("1",editSwOn.text.toString())
        savedPrefes("2",editSwOff.text.toString())
        savedPrefes("3",editNotify.text.toString())
        savedPrefes("4",auto)
        savedPrefes("5",notify)
        Toast.makeText(applicationContext, "Saved", Toast.LENGTH_LONG).show()
    }

    private fun loadSaved(){
       val sharedPrefes: SharedPreferences = getPreferences(Context.MODE_PRIVATE)
        editSwOn.setText(sharedPrefes.getString("1",swOn))
        editSwOff.setText(sharedPrefes.getString("2",swOff))
        editNotify.setText(sharedPrefes.getString("3",notifyOn))
        if(sharedPrefes.getString("4",auto) == "true"){
            swAuto.isChecked = true
        }
        else if(sharedPrefes.getString("4",auto) == "false"){
            swAuto.isChecked =  false
        }
        if(sharedPrefes.getString("5",notify) == "true"){
            swNotify.isChecked = true
        }
        else if(sharedPrefes.getString("5",notify) == "false"){
            swNotify.isChecked =  false
        }
    }

    private fun savedPrefes(key:String, value:String) {
        val sharedPrefes: SharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPrefes.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private var tempe: String = ""

    private fun performTask(temp: String){
        var tempeValue : Double

        if(txtTempeValue.text == "null"){ tempeValue= 0.00 }else { tempeValue = temp.toDouble() }

        var swOnValue= editSwOn.text.toString().toDouble()
        var swOffValue= editSwOff.text.toString().toDouble()
        var notifyValue = editNotify.text.toString().toDouble()
        if(swNotify.isChecked) {
            if(notifyValue == 0.00){
                btnSwitch.isChecked = btnSwitch.isChecked
            }
            else if (tempeValue >= notifyValue) {
                var content = "Environment Temperature : " + tempeValue + "°C"
                var builder = NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_message)
                    .setContentTitle("Temperature Detected")
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                with(NotificationManagerCompat.from(this)) {
                    notify(1234, builder.build())
                }
            }
        }

        if(swAuto.isChecked) {
            if(swOnValue == 0.00){
                btnSwitch.isChecked = btnSwitch.isChecked
            }
            else if (tempeValue >= swOnValue) {
                btnSwitch.isChecked = true
                //updateDb("1")
                Toast.makeText(applicationContext, "Air Cond On", Toast.LENGTH_LONG).show()
            } else if (tempeValue <= swOffValue && tempeValue != 0.00) {
                btnSwitch.isChecked = false
                //updateDb("0")
                Toast.makeText(applicationContext, "Air Cond Off", Toast.LENGTH_LONG).show()
            } else if (tempeValue == 0.00) {
                btnSwitch.isChecked = btnSwitch.isChecked
            }
        }
    }

    private fun getData(){
        val sdf = SimpleDateFormat("yyyyMMdd")
        val frmt = DecimalFormat("00")
        val date = sdf.format(Date())
        val ddbdate = "PI_03_" + date
        val hour = SimpleDateFormat("HH").format(Date())
        val min = SimpleDateFormat("mm").format(Date())
        val sec = SimpleDateFormat("ss").format(Date())
        val minSec = min + frmt.format((floor(sec.toDouble()/10)*10).toInt())

        val ref = FirebaseDatabase.getInstance().getReference().child(ddbdate)
        val lastQuery = ref.child(hour).child(minSec)
        lastQuery.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot){
                tempe = p0.child("tempe").value.toString()// get temperature
                if(tempe != null){
                    txtTempeValue.text = tempe
                }
                performTask(tempe)
            }
            override fun onCancelled(p0: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })
    }

    private fun updateDb(relayValue: String){
        var relay = relayValue

        val dbref = FirebaseDatabase.getInstance().getReference("PI_03_CONTROL").child("relay")
        dbref.setValue(relay)
    }

}
