package com.example.affirmations

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import android.content.Context
import android.content.Intent
import java.io.IOException
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.speech.tts.TextToSpeech
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_2.*
import java.io.InputStream
import java.util.*

fun readJson(context: Context, filename: String) :String? {
    val jsonString: String
    try {
        jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        return null
    }
    return jsonString
}


class Activity2 : AppCompatActivity(), TextToSpeech.OnInitListener {
    var tts: TextToSpeech? = null
    var buttonSpeak: Button? = null
    var touchedItem: String = "b"
    var coordinateMaps: List<Map<String, Any>> = listOf()
    val scale: Float = 1.5F
    //    var boundaryMap: MutableMap<String, List<Float>> = mutableMapOf<String, List<Float>>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_2)

        buttonSpeak = button
        buttonSpeak!!.isEnabled = false
        tts = TextToSpeech(this, this)
        buttonSpeak!!.setOnClickListener{ speakOut() }

        // load image and borders
        val bitmap: Bitmap = Bitmap.createBitmap(1000, 800, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)


        var d: Drawable = getResources().getDrawable(R.drawable.origin_bedroom)
        d.setBounds(0, 0, (640 * scale).toInt(), (480 * scale).toInt())
        d.draw(canvas)

        var jsonFileString: String? = ""
        try{
            val inputStream: InputStream = assets.open("bedroom.json")
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            jsonFileString = String(buffer)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val gson = Gson()
        var bedroomMap: Map<String, Any> = gson.fromJson(jsonFileString, object : TypeToken<Map<String, Any>>() {}.type)
        coordinateMaps = bedroomMap["maskrcnn"] as List<Map<String, Any>>
        for (coordinatemap in coordinateMaps) {
            var itemCoord : List<List<List<Float>>> = coordinatemap["coordinates"] as List<List<List<Float>>>

            val paintStroke = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#DC143C")
                strokeWidth = 3F
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }

            val path = Path().apply{
                fillType = Path.FillType.EVEN_ODD

                val margin = 100

                moveTo(itemCoord[0][0][0] * scale, itemCoord[0][0][1] * scale)
                for (i in itemCoord[0].indices) {
                    lineTo(itemCoord[0][i][0] * scale, itemCoord[0][i][1] * scale)
                }
                close()
            }
            canvas.drawPath(path, paintStroke)
        }


//        Log.i("data", "${coordinateMaps}")

        imageView.background = BitmapDrawable(resources, bitmap)
        // load image done

        var thisDown: Long = 0
        var lastUp: Long = 0
        var interval: Long = 10000
        var lastDuration: Long = 10000

        imageView.setOnTouchListener(object: View.OnTouchListener {
            var mHandler: Handler? = null
            var touchedX: Float = 0F
            var touchedY: Float = 0F
            var longPress: Boolean = true

            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                if (p1 != null) {
                    if (p1.action == MotionEvent.ACTION_DOWN) {
//                        if (mHandler != null) return true
                        thisDown = System.currentTimeMillis()
                        interval = thisDown - lastUp
//                        Log.i("debug", "interval: ${interval}, thisDown: ${thisDown}")
                        longPress = true
                        touchedX = p1.x
                        touchedY = p1.y
                        getItem(touchedX, touchedY)
//                        Log.i("data", "pressed")
                        val soundThread = object: Thread() {
                            override fun run() {
                                super.run()
                                while (longPress)
                                {
                                    if (touchedItem != "b")
                                    {
                                        speakOut()
                                        Thread.sleep(3000)
                                    }
                                    else
                                    {
                                        speakOut()
                                        Thread.sleep(500)
                                    }
                                }
                            }
                        }
                        soundThread.start()
                        return true

//                        mHandler = object: Handler(){}
//                        (mHandler as Handler).postDelayed(mAction, 1000)
                    }
                    else if (p1.action == MotionEvent.ACTION_UP)
                    {
//                        if (mHandler == null) return true
                        longPress = false
                        lastUp = System.currentTimeMillis()
//                        Log.i("debug", "up: ${lastUp}")
                        var currentDuration = lastUp - thisDown
                        if (lastDuration < 300 && interval < 300 && currentDuration < 300) // Double click
                        {
                            Log.i("data", "double click detected")
                            if (touchedItem != "b" && touchedItem != "")
                            {
                                val intent = Intent(this@Activity2, InfoLayer2::class.java).apply {
                                    putExtra("object", touchedItem)
                                }
                                ContextCompat.startActivity(this@Activity2, intent, null)
                            }

                        }
                        lastDuration = currentDuration

//                        Log.i("data", "lifted")
//                        mHandler?.removeCallbacks(mAction)
//                        mHandler = null
                    }
                    else if (p1.action == MotionEvent.ACTION_MOVE)
                    {
                        touchedX = p1.x
                        touchedY = p1.y
                        getItem(touchedX, touchedY)
//                        Log.i("data", "touched: ${touchedX}, ${touchedY}")
                    }
                }
                return false
            }

//            var mAction: Runnable = object: Runnable {
//                override fun run() {
//                    speakOut()
//                    mHandler?.postDelayed(this,1000)
//                }
//            }
        })
    }

    override fun onInit(p0: Int) {
        if (p0 == TextToSpeech.SUCCESS)
        {
            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
            {
                Log.e("TTS","The Language specified is not supported!")

            }
            else
            {
                buttonSpeak!!.isEnabled = true
            }
        }
        else
        {
            Log.e("TTS","Init failed")
        }
    }

    private fun speakOut() {
        tts!!.speak(touchedItem, TextToSpeech.QUEUE_ADD, null,"")
        Log.i("data", "speaking ${touchedItem}")
    }



    private fun getItem(x: Float, y: Float) {
        touchedItem = "b"
        var numCross: Int =0
        for (itemCoord in coordinateMaps)
        {
            var temp: List<List<List<Float>>> = itemCoord["coordinates"] as List<List<List<Float>>>
            var coordList: List<List<Float>> = temp[0]
            var prevUp: Boolean = false // previous coord is lower
            if (coordList[0][1] * scale < y)
                prevUp = true
            var upX: Float = 0F
            for (coord in coordList)
            {
                if (prevUp)
                {
                    if (coord[1] * scale > y)
                    {
                        prevUp = false
                        if (upX > x || coord[0] * scale > x)
                        {
                            numCross += 1
                        }
                    }
                }
                else
                {
                    if (coord[1] * scale < y)
                    {
                        prevUp = true
                        if (upX > x || coord[0] * scale > x)
                        {
                            numCross += 1
                        }
                    }
                }
                upX = x
            }
//            Log.i("data", "label: ${itemCoord["label"]}, numCross: ${numCross}")
            if (numCross % 2 == 1)
            {
                touchedItem = itemCoord["label"] as String
                break
            }
        }

    }

    override fun onDestroy() {
        if (tts != null)
        {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }


}

