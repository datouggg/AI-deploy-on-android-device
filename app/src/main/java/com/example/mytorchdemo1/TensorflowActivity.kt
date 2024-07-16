package com.example.mytorchdemo1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.tensorflow.lite.Interpreter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mytorchdemo1.ui.theme.MyTorchDemo1Theme
import org.pytorch.Module
import java.io.IOException
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.res.AssetManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import java.nio.ByteBuffer

//class DigitClassifier(private val context: Context) {
//    private var interpreter: Interpreter? = null
//    // ...
//}
class TensorflowActivity : ComponentActivity(), View.OnClickListener {
    private var interpreter: Interpreter? = null
    private lateinit var bitmap: Bitmap
    private lateinit var imageView: ImageView
    private lateinit var textview: TextView
    var num: Int = 0
    val MSG_DOWN_START: Int = 0
    val MSG_DOWN_SUCCESS: Int = 1

    private val mHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_DOWN_START -> {
                    textview.text = "开始推理"
                }

                MSG_DOWN_SUCCESS -> {
                    textview.text = msg.obj as String
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.pytorch_activity)
        num = 0
        textview = findViewById<TextView>(R.id.text)
        imageView = findViewById<ImageView>(R.id.image)
        var button1 = findViewById<Button>(R.id.lastImage).setOnClickListener(this)
        var button2 = findViewById<Button>(R.id.nextImage).setOnClickListener(this)

    }

    fun createRunnable(a: Int): Runnable {
        return Runnable {
            mHandler.sendEmptyMessage(0)
            val result: String = predict(a)
            val msg = Message()
            msg.what = MSG_DOWN_SUCCESS
            msg.obj = result
            mHandler.sendMessage(msg)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.nextImage -> {
                num++
                val childThread = Thread(createRunnable(num))
                childThread.start()
            }

            R.id.lastImage -> {
                if (num > 0) {
                    num--
                }
                val childThread = Thread(createRunnable(num))
                childThread.start()
            }
        }

    }

    fun loadModel(context: Context, modelFileName: String): Interpreter {
        val assetFileDescriptor = context.assets.openFd(modelFileName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return Interpreter(
            mapModelInMemory(fileChannel, startOffset, declaredLength),
            Interpreter.Options()
        )
    }

    private fun mapModelInMemory(
        fileChannel: FileChannel,
        startOffset: Long,
        declaredLength: Long
    ): MappedByteBuffer {
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val width = bitmap.width
        val height = bitmap.height
        val channels = 3 // 假设图像是RGB格式，有3个通道

        // 分配一个ByteBuffer，大小为图像的宽度 x 高度 x 通道数
        val byteBuffer = ByteBuffer.allocateDirect(width * height * channels)

        // 遍历图像的每个像素
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)

                // 提取像素的红色、绿色和蓝色分量，并将它们添加到ByteBuffer中
                val r = (pixel shr 16 and 0xff).toByte()
                val g = (pixel shr 8 and 0xff).toByte()
                val b = (pixel and 0xff).toByte()

                byteBuffer.put(r)
                byteBuffer.put(g)
                byteBuffer.put(b)
            }
        }

        // 重置ByteBuffer的position，以便从头开始读取数据
        byteBuffer.rewind()

        return byteBuffer
    }

    fun predict(a: Int): String {
        try {
            val assetManager = assets
            //读取图片和模型
            BitmapFactory.decodeStream(assets.open("${a % 8}.jpg")).apply {
                bitmap = Bitmap.createScaledBitmap(this, 32, 32, true)
            }
            val modelFileName = "simpleRes.tflite"
            val interpreter = loadModel(this, modelFileName)
//            val model = loadModelFile(assetManager,"simpleRes.tflite")
//            module_ori = Module.load(PytorchActivity.assetFilePath(this, "simpleRes.pt"))
        } catch (e: IOException) {
            Log.e("TensorflowHelloWorld", "Error reading assets", e)
            finish()
        }


        val byteBuffer = convertBitmapToByteBuffer(bitmap)
        val output = Array(1) { FloatArray(10) }
        interpreter?.run(byteBuffer, output)
        val result = output[0]
        var maxScore = -Float.MAX_VALUE
        var maxScoreIdx = -1
        for (i in result.indices) {
            if (result[i] > maxScore) {
                maxScore = result[i]
                maxScoreIdx = i
            }
        }
        System.out.println(maxScoreIdx)
        var className = CIfarClassed.IMAGENET_CLASSES[maxScoreIdx]
        val tex =
            "推理结果：" + className + "\n原始模型推理时间：" + 1 + "ms" + "\n剪枝模型推理时间：" + 1 + "ms"
        return tex
    }
}

//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    MyTorchDemo1Theme {
//        Greeting("Android")
//    }
//}