package com.xyj.focuspod

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 最小入口页面：只验证应用框架可以启动，后续业务从这里继续扩展。
        val textView = TextView(this)
        textView.text = getString(R.string.app_name)
        textView.gravity = Gravity.CENTER
        textView.textSize = 24f

        setContentView(textView)
    }
}
