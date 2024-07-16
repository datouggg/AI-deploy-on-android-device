package com.example.mytorchdemo1

import android.os.Message
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import android.os.Handler
import android.graphics.Bitmap
import android.os.Looper
import org.pytorch.Module
import android.widget.ImageView
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import org.pytorch.IValue
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import org.pytorch.MemoryFormat
import java.io.File
import java.io.IOException
import java.io.FileOutputStream

class PytorchActivity : ComponentActivity(), View.OnClickListener {
    private lateinit var imageView: ImageView
    private lateinit var textview: TextView
    private lateinit var bitmap: Bitmap
    private lateinit var module_ori: Module
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



//    val myRunable = Runnable {
//        mHandler.sendEmptyMessage(0)
//        val result: String = predict(1)
//        var msg = Message()
//        msg.what = MSG_DOWN_SUCCESS;
//        msg.obj = result
//        mHandler.sendMessage(msg)
//
//    }


    fun predict(a: Int): String {

        try {
            //读取图片和模型
            BitmapFactory.decodeStream(assets.open("${a%8}.jpg")).apply {
                bitmap = Bitmap.createScaledBitmap(this, 32, 32, true)
            }

            module_ori = Module.load(assetFilePath(this, "simpleRes.pt"))

        } catch (e: IOException) {
            Log.e("PytorchHelloWorld", "Error reading assets", e)
            finish()
        }

        //输入向量
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            bitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB,
            MemoryFormat.CHANNELS_LAST
        )

        if (module_ori == null) {
            return ""
        }
//        Log.i(
//            tag,
//            "input shape: ${inputTensor.shape()[0]}, ${inputTensor.shape()[1]}, ${inputTensor.shape()[2]}, ${inputTensor.shape()[3]}"
//        )


        //运行模型
        var startTime_ori = System.currentTimeMillis()
        val outputTensor_ori: Tensor = module_ori.forward(IValue.from(inputTensor)).toTensor()
        var endTime_ori = System.currentTimeMillis()
        var inferenceTimeori = endTime_ori - startTime_ori

        val scores = outputTensor_ori.dataAsFloatArray
        var maxScore = -Float.MAX_VALUE
        var maxScoreIdx = -1
        for (i in scores.indices) {
            if (scores[i] > maxScore) {
                maxScore = scores[i]
                maxScoreIdx = i
            }
        }
        System.out.println(maxScoreIdx)
        var className = CIfarClassed.IMAGENET_CLASSES[maxScoreIdx]

        val tex =
            "推理结果：" + className + "\n原始模型推理时间：" + inferenceTimeori + "ms" + "\n剪枝模型推理时间：" + 1 + "ms"
        return tex
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.pytorch_activity)
        num=0
        textview = findViewById<TextView>(R.id.text)
        imageView = findViewById<ImageView>(R.id.image)
        var button1 = findViewById<Button>(R.id.lastImage).setOnClickListener(this)
        var button2 = findViewById<Button>(R.id.nextImage).setOnClickListener(this)
//        imageView.setImageBitmap(bitmap)
//        val childThread = Thread(createRunnable(num))
//        childThread.start()

    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.nextImage -> {
                num++
                val childThread = Thread(createRunnable(num))
                childThread.start()
            }
            R.id.lastImage -> {
                if(num>0)
                {
                    num--
                }
                val childThread = Thread(createRunnable(num))
                childThread.start()
            }
        }

    }


    //从Android应用程序的assets目录中读取一个文件（由assetName指定），
//然后将其写入到一个新的文件（由file指定）
    companion object {
        fun assetFilePath(context: Context, assetName: String): String {
            var file = File(context.filesDir, assetName)
            if (file.exists() && file.length() > 0) {
                return file.absolutePath
            }
            //use用于处理需要关闭的资源
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
            }
            return file.absolutePath

        }
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
