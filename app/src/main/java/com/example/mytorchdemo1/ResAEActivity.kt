package com.example.mytorchdemo1

import java.nio.FloatBuffer
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.TextView
import android.content.res.AssetManager
import android.view.View
import android.widget.Button
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
import org.pytorch.IValue
import org.pytorch.MemoryFormat
import org.pytorch.Module
import org.pytorch.Tensor

import org.pytorch.torchvision.TensorImageUtils
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ResAEActivity : ComponentActivity(), View.OnClickListener {

    private lateinit var textView: TextView
    private lateinit var textView2: TextView
    private lateinit var textView3: TextView
    private lateinit var textView4: TextView
    private lateinit var textView5: TextView
    private lateinit var module_ori: Module
    private lateinit var module_ori_cut: Module
    private lateinit var inputTensor: Tensor
    private lateinit var dataModule: Module
    private var dataList = mutableListOf<MutableList<Float>>()
    private val MSG_DOWN_SUCCESS: Int = 1
    private var num = 0

    //推理数据传输
    private var mhandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val floatArray = msg.obj as FloatArray
            textView.text = "The first para is %.3f m".format(floatArray[0])
            textView2.text = "The second para is %.3f m".format(floatArray[1])
            textView3.text = "The third para is %.3f m".format(floatArray[2])
            textView4.text = "The fourth para is %.3f m".format(floatArray[3])
        }
    }

    //推理时间传输
    private var handlerTime = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            textView5.text = "Total time is ${msg.obj as Long} ms "
        }
    }

    //读取assets里面的csv文件，并返回一个bufferreader
    fun getcsv(pathName: String): BufferedReader {
        var inputStream = assets.open(pathName)
        var isr = InputStreamReader(inputStream)
        var br = BufferedReader(isr)
        return br
    }

    //读取一个bufferreader里面的数据，并传入一个list,csv纵列
    fun buffer2Floatlist(br: BufferedReader, dataList: MutableList<Float>) {
        var line: String?
        while (br.readLine().also { line = it } != null) {
            val values = line!!.split(",")
            val data = values[0].toFloat()
            dataList.add(data)
        }
    }

    //读取一个bufferreader里面的数据,并传入一个二维mutableList
    fun buffer2Floatlistv2(br: BufferedReader, dataList: MutableList<MutableList<Float>>) {
        var line: String?
        while (br.readLine().also { line = it } != null) {
            val row = mutableListOf<Float>()
            val values = line!!.split(",")
            for (value in values) {
                val data = value.toFloat()
                row.add(data)
            }
            dataList.add(row)

        }
    }

    fun buffer2FloatArray(br: BufferedReader): FloatArray {
        val dataList = mutableListOf<Float>()
        var line: String? = br.readLine()
        while (line != null) {
            dataList.addAll(line.split(",").map { it.toFloat() })
            line = br.readLine()
        }
        return dataList.toFloatArray()
    }

//    fun gettensor(path:String):Tensor{
//        var assetManager=getAssets();
//        var inputStream=assetManager.open(path)
//        var byteArray=inputStream.readBytes()
////        var tensor=Tensor.fromBlob(byteArray,)
//        return
//    }


    fun predict(groupID: Int, dataList: MutableList<MutableList<Float>>): FloatArray {

        try {
            var assetManager = getAssets();
            var inputStream = assetManager.open("fbank.pt")
            var byteArray = inputStream.readBytes()

//            dataModule=Module.load(PytorchActivity.assetFilePath(this,"fbank.pt"))
//            var dataIValue=dataModule.runMethod("get")
//            var datatensor=dataIValue.toTensor()
            //dataList2buffer
            var directBuffer = ByteBuffer.allocateDirect(dataList[0].size * 4)
            directBuffer.order(ByteOrder.nativeOrder())
            val floatBuffer = directBuffer.asFloatBuffer()
            for (value in dataList[groupID]) {
                floatBuffer.put(value)
            }
            floatBuffer.flip()

            //buffer2tensor
            val shape = longArrayOf(1, 1, 700L)
            inputTensor = Tensor.fromBlob(floatBuffer, shape)

            //读取裁剪前和裁剪后的模型，分别部署，分别推理
            module_ori = Module.load(PytorchActivity.assetFilePath(this, "mobile_model.pt"))

            module_ori_cut = Module.load(PytorchActivity.assetFilePath(this, "CUT_mobile_model.pt"))


        } catch (e: IOException) {
            Log.e("PytorchHelloWorld", "Error reading assets", e)
            finish()
        }

        //输入向量

        if (module_ori == null) {
            return floatArrayOf()
        }

        //运行模型
        var startTime_ori = System.currentTimeMillis()
        val outputTensor_ori: Tensor =
            module_ori.forward(IValue.from(inputTensor)).toTuple()[0].toTensor()
        val outputTensor_cut: Tensor = module_ori_cut.forward(IValue.from(inputTensor)).toTensor()
        var endTime_ori = System.currentTimeMillis()
        var inferenceTimeori = endTime_ori - startTime_ori
        val msgTime = Message()
        msgTime.what = MSG_DOWN_SUCCESS
        msgTime.obj = inferenceTimeori
        handlerTime.sendMessage(msgTime)

        val scores = outputTensor_ori.dataAsFloatArray
        val scores_cut = outputTensor_cut.dataAsFloatArray

        val msg = Message()
        msg.what = MSG_DOWN_SUCCESS
        msg.obj = scores_cut
        mhandler.sendMessage(msg)
        return scores
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.res_ae_activity)
        textView = findViewById(R.id.text)
        textView2 = findViewById(R.id.text2)
        textView3 = findViewById(R.id.text3)
        textView4 = findViewById(R.id.text4)
        textView5 = findViewById(R.id.text5)
        //读取推理数据，并保存在一个二维数组里
        var myBufferReader = getcsv("num100.csv")
        buffer2Floatlistv2(myBufferReader, dataList)
        myBufferReader.close()
        //尝试载入一个二维的向量
        var tempBufferedReader = getcsv("fbank.csv")
        val floatArray = buffer2FloatArray(tempBufferedReader)
        val shape = longArrayOf(1,601, 80)
        val datatensor = Tensor.fromBlob(floatArray, shape)

        tempBufferedReader.close()
        var buttondd = findViewById<Button>(R.id.button_next_group).setOnClickListener(this)


        Thread { predict(num, dataList) }.start()

    }

    override fun onClick(v: View?) {
        Thread { predict(num, dataList) }.start()
        num++
    }

//    fun assetFilePath(context: Context, assetName: String): String {
//        var file = File(context.filesDir, assetName)
//        if (file.exists() && file.length() > 0) {
//            return file.absolutePath
//        }
//        //use用于处理需要关闭的资源
//        context.assets.open(assetName).use { inputStream ->
//            FileOutputStream(file).use { outputStream ->
//                val buffer = ByteArray(4 * 1024)
//                var read: Int
//                while (inputStream.read(buffer).also { read = it } != -1) {
//                    outputStream.write(buffer, 0, read)
//                }
//                outputStream.flush()
//            }
//        }
//        return file.absolutePath
//
//    }
}






