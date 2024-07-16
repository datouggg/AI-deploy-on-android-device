package com.example.mytorchdemo1

import android.content.res.TypedArray
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.util.Log
import org.pytorch.IValue
import android.widget.Button
import android.widget.TextView
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
import org.pytorch.Tensor
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.concurrent.thread

class ASRActivity : ComponentActivity(), View.OnClickListener {

    private lateinit var module_ori: Module
    private var eos_id = 1
    private lateinit var textview1:TextView
    private lateinit var textview2:TextView



    private var mhandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val getSentence = msg.obj as String
            textview1.text = getSentence
        }
    }

    private var timehandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            textview2.text = "Total time is ${msg.obj as Long} ms "
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.asractivity)
        var button = findViewById<Button>(R.id.button_play).setOnClickListener(this)
        var button2 = findViewById<Button>(R.id.button_analyze).setOnClickListener(this)
        textview1=findViewById<TextView>(R.id.predict_sentence)
        textview2=findViewById<TextView>(R.id.predict_time)
    }


    override fun onClick(view: View) {

        when (view.id) {
            R.id.button_play -> {
                var mediaplayer=MediaPlayer()
                val afd=assets.openFd("61-70968-0000.flac")
                mediaplayer.setDataSource(afd.fileDescriptor,afd.startOffset,afd.length)
                mediaplayer.prepare()
                mediaplayer.start()
            }

            R.id.button_analyze -> {
                Thread {
                    var startTime_ori = System.currentTimeMillis()
                    var tokenarray = predict()
                    tokenarray = tokenarray.copyOfRange(1, tokenarray.size)
                    var temp = mapEstablish()
                    var ans=""
                    for(value in tokenarray){
                        ans=ans+temp[value.toInt()]
                    }
                    var real_ans=ans.replace('▁',' ')
                    var endTime_ori = System.currentTimeMillis()
                    var inferenceTimeori = endTime_ori - startTime_ori
                    val msg=Message()
                    msg.obj=real_ans
                    mhandler.sendMessage(msg)
                    val msgTime=Message()
                    msgTime.obj=inferenceTimeori
                    timehandler.sendMessage(msgTime)

                }.start()


            }
        }
    }


    fun mapEstablish(): Array<String> {
        val data = """
        <sos>	0
        <eos>	0
        <blank>	0
        ©	0
        ▁T	-0
        HE	-1
        ▁A	-2
        ▁THE	-3
        IN	-4
        ▁W	-5
        ▁S	-6
        ▁O	-7
        RE	-8
        ND	-9
        ▁H	-10
        ▁B	-11
        ER	-12
        ▁M	-13
        OU	-14
        ▁I	-15
        ▁C	-16
        ED	-17
        ▁F	-18
        AT	-19
        EN	-20
        ▁AND	-21
        ▁TO	-22
        ▁OF	-23
        ON	-24
        IS	-25
        ING	-26
        ▁P	-27
        ▁TH	-28
        ▁D	-29
        OR	-30
        ▁HE	-31
        AS	-32
        ES	-33
        ▁L	-34
        ▁IN	-35
        AR	-36
        AN	-37
        IT	-38
        LL	-39
        ▁N	-40
        ▁G	-41
        OM	-42
        ▁HA	-43
        ▁BE	-44
        ▁E	-45
        LE	-46
        IC	-47
        OT	-48
        UT	-49
        OW	-50
        ▁Y	-51
        ▁WAS	-52
        ▁WH	-53
        ▁IT	-54
        LD	-55
        ▁THAT	-56
        VE	-57
        LY	-58
        SE	-59
        ID	-60
        ▁ON	-61
        GH	-62
        ENT	-63
        ST	-64
        ▁RE	-65
        IM	-66
        ▁YOU	-67
        ▁U	-68
        ▁	-69
        E	-70
        T	-71
        A	-72
        O	-73
        N	-74
        I	-75
        H	-76
        S	-77
        R	-78
        D	-79
        L	-80
        U	-81
        M	-82
        C	-83
        W	-84
        F	-85
        G	-86
        Y	-87
        P	-88
        B	-89
        V	-90
        K	-91
        '	-92
        X	-93
        J	-94
        Q	-95
""".trimIndent()

// Split the data into lines
        val lines = data.lines()

// Initialize an ArrayList to store the first column values
        val firstColumn = ArrayList<String>()

// Iterate through each line
        for (line in lines) {
            // Split the line into columns using a regular expression for whitespace
            val columns = line.split("\\s+".toRegex())

            // Add the first column value to the ArrayList
            firstColumn.add(columns[0])
        }

// Convert the ArrayList to a string array
        val firstColumnArray = firstColumn.toTypedArray()
        return firstColumnArray

    }


    fun getcsv(pathName: String): BufferedReader {
        var inputStream = assets.open(pathName)
        var isr = InputStreamReader(inputStream)
        var br = BufferedReader(isr)
        return br
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

//        fun logSoftmax(input: Tensor, dim: Int): Tensor {
//            val max = input.max(dim)
//            val exp = input.sub(max).exp()
//            val sum = exp.sum(dim)
//            val logSum = sum.log()
//            return input.sub(max).sub(logSum)
//        }


    fun predict(): LongArray {
        //获取forward三个输入，读取模型

//              第一个参数向量化
        var tempBufferedReader = getcsv("fbank.csv")
        val floatArray = buffer2FloatArray(tempBufferedReader)
        val shape = longArrayOf(1, 489, 80)
        val fbank_feat = Tensor.fromBlob(floatArray, shape)
//              第二个参数向量化
        var feat_len_float = FloatArray(1) { 498.0f }
        val feat_len_shape = longArrayOf(1)
        val feat_len = Tensor.fromBlob(feat_len_float, feat_len_shape)
//              第三个参数向量化
        var ys_in_shape = longArrayOf(1, 1) // 定义Tensor的形状
        var ys_in = longArrayOf(0) // 定义Tensor的数据
        var ys_in_pad = Tensor.fromBlob(ys_in, ys_in_shape) // 创建Tensor
        module_ori = Module.load(PytorchActivity.assetFilePath(this, "CUT_ASR_model.pt"))


        //推理过程
        while (true) {
            //返回一个二维tensor,分别是概率分布和最大概率·的索引
            var result = module_ori.forward(
                IValue.from(fbank_feat),
                IValue.from(feat_len),
                IValue.from(ys_in_pad)
            ).toTuple()
            var logits = result[0].toTensor()
            var y_hat = result[1].toTensor()

            //看是不是结束到结束token
            val y_check = y_hat.dataAsLongArray[0]
            if (y_check == eos_id.toLong()) {
                break
            }

            //把y_hat接在ys_in_pad后面
            var ys_in_pad_long = ys_in_pad.dataAsLongArray
            ys_in_pad_long = ys_in_pad_long + y_check
            var new_shape = longArrayOf(1, ys_in_pad_long.size.toLong())
            ys_in_pad = Tensor.fromBlob(ys_in_pad_long, new_shape)


            if (ys_in_pad.shape()[0] > 100) {
                break
            }
        }

        return ys_in_pad.dataAsLongArray
    }


}




