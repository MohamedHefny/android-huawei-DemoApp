package com.mohamedhefny.huawei

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.toolbar_home.*

class HomeActivity : AppCompatActivity() {

    private val TAG: String = HomeActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (HuaweiIdAuthManager.getAuthResult() == null) {
            Log.e(TAG, "User didn't logged in yet!")
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        bindUserData()
    }

    private fun bindUserData() {
        home_username.text = HuaweiIdAuthManager.getAuthResult()
            .familyName.plus(" ${HuaweiIdAuthManager.getAuthResult().givenName}")

        Picasso.get().load(HuaweiIdAuthManager.getAuthResult().avatarUri)
            .error(R.mipmap.ic_launcher).into(home_user_pic)
    }
}