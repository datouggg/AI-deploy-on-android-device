package com.example.mytorchdemo1
import android.content.Intent
import android.os.Bundle
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

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.welcome)
        val button1=findViewById<Button>(R.id.button1)
        val button2=findViewById<Button>(R.id.button2)
        val button3=findViewById<Button>(R.id.button3)
        val button4=findViewById<Button>(R.id.button4)
        button1.setOnClickListener{
            startActivity(Intent(this@WelcomeActivity,PytorchActivity::class.java))
        }
        button2.setOnClickListener{
            startActivity(Intent(this@WelcomeActivity,TensorflowActivity::class.java))
        }
        button3.setOnClickListener{
            startActivity(Intent(this@WelcomeActivity,ASRActivity::class.java))
        }
        button4.setOnClickListener{
            startActivity(Intent(this@WelcomeActivity,ResAEActivity::class.java))
        }

    }
}

@Composable
fun Greeting2(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    MyTorchDemo1Theme {
        Greeting2("Android")
    }
}