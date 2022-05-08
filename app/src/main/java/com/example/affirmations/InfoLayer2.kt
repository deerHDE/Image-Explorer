package com.example.affirmations

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_2.*
import kotlinx.android.synthetic.main.activity_info_layer2.*
import java.io.IOException
import java.io.InputStream

class InfoLayer2 : AppCompatActivity() {
    var scale: Float = 1.5F
    var touchedItem: String = ""
    var objCoords: List<List<Float>> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_layer2)

        val selectedObject = intent.getStringExtra("object")
        Log.i("debug", "${selectedObject}")
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
        var coordinateMaps = bedroomMap["maskrcnn"] as List<Map<String, Any>>
        for (coordinatemap in coordinateMaps)
        {
            if (coordinatemap["label"] == selectedObject)
            {
                var temp = coordinatemap["coordinates"] as List<List<List<Float>>>
                objCoords = temp[0]
                break
            }
        }
        Log.i("debug", "${objCoords}")
        var captions: List<Map<String, Any>> = bedroomMap["captions"] as List<Map<String, Any>>
        for (caption in captions)
        {
            val box: List<Int> = caption["bounding_box"] as List<Int>
//            Log.i("data", "${box}")
            if (getItem((box[0] + box[2] / 2) * scale.toFloat(), (box[1] + box[3] / 2) * scale.toFloat()))
            {
                var rect: ShapeDrawable = ShapeDrawable(RectShape())
                rect.setBounds((box[0] * scale).toInt(), (box[1] * scale).toInt(), ((box[0] + box[2]) * scale).toInt(), ((box[1] + box[3]) * scale).toInt())
                rect.paint.apply {
                    isAntiAlias = true
                    color = Color.parseColor("#DC143C")
                    strokeWidth = 3F
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                }
                rect.draw(canvas)
                val textSize = 20F
                val textPaint: Paint = Paint()
                textPaint.setColor(Color.GREEN)
                textPaint.style = Paint.Style.FILL
                textPaint.textSize = textSize
                canvas.drawText(caption["caption"] as String, box[0] * scale, box[1] * scale + textSize + 3, textPaint)

            }
        }

        imageView2.background = BitmapDrawable(resources, bitmap)
        imageView2.setOnClickListener(object: DoubleClickListener() {
            override fun onDoubleClick(v: View?) {
                val intent = Intent(this@InfoLayer2, Activity2::class.java)
                ContextCompat.startActivity(this@InfoLayer2, intent, null)
            }
        })
    }
    abstract class DoubleClickListener : View.OnClickListener {
        var lastClickTime: Long = 0
        override fun onClick(v: View?) {
            val clickTime = System.currentTimeMillis()
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                onDoubleClick(v)
            }
            lastClickTime = clickTime
        }

        abstract fun onDoubleClick(v: View?)

        companion object {
            private const val DOUBLE_CLICK_TIME_DELTA: Long = 300 //milliseconds
        }
    }
    private fun getItem(x: Float, y: Float): Boolean {
        touchedItem = "b"
        var numCross: Int =0

        var prevUp: Boolean = false // previous coord is lower
        if (objCoords[0][1] * scale < y)
            prevUp = true
        var upX: Float = 0F
        for (coord in objCoords)
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
        Log.i("debug", "${numCross}")
//            Log.i("data", "label: ${itemCoord["label"]}, numCross: ${numCross}")
        return numCross % 2 == 1
    }


}