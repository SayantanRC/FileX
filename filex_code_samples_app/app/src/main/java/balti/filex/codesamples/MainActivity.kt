package balti.filex.codesamples

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import balti.filex.FileXInit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize FileX
        FileXInit(this, false)
    }
}