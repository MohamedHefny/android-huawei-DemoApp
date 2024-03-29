package com.mohamedhefny.huawei.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.huawei.hms.iap.entity.ProductInfo
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.mohamedhefny.huawei.R
import com.mohamedhefny.huawei.ui.signin.SignInActivity
import com.mohamedhefny.huawei.ui.sub_features.products.ProductCallback
import com.mohamedhefny.huawei.ui.sub_features.products.ProductsSheet
import com.mohamedhefny.huawei.utils.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.toolbar_home.*

class HomeActivity : AppCompatActivity(), ProductCallback {

    private val homeViewModel: HomeViewModel by
    lazy { ViewModelProviders.of(this).get(HomeViewModel::class.java) }

    private val TAG: String = HomeActivity::class.java.simpleName

    private var videoPrepared: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (HuaweiIdAuthManager.getAuthResult() == null) {
            Log.e(TAG, "User didn't logged in yet!")
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        bindUserData()

        initVideo()

        home_video_view.setOnClickListener {
            if (homeViewModel.canPlayVideo) {
                if (!videoPrepared) {
                    showToast(R.string.still_loading_video)
                    return@setOnClickListener
                }
                if (home_video_view.isPlaying) {
                    home_video_play_btn.show()
                    home_video_view.pause()
                } else {
                    home_video_play_btn.hide()
                    home_video_view.start()
                }
            } else {
                showLoading()
                getAvailableProducts()
            }
        }

        observePaymentErrors()
    }

    private fun bindUserData() {
        home_username.text = HuaweiIdAuthManager.getAuthResult()
            .familyName.plus(" ${HuaweiIdAuthManager.getAuthResult().givenName}")

        Picasso.get().load(HuaweiIdAuthManager.getAuthResult().avatarUri)
            .placeholder(R.drawable.ic_user).error(R.mipmap.ic_launcher)
            .into(home_user_pic)
    }

    private fun initVideo() {
        home_video_view.setVideoURI(Uri.parse(VIDEO_URL))
        home_video_view.setOnPreparedListener {
            videoPrepared = true
            it.seekTo(100)
            home_video_loading.hide()
        }
        home_video_view.setOnCompletionListener {
            home_video_play_btn.show()
        }
    }

    private fun getAvailableProducts() {
        homeViewModel.getAvailableProducts()
            .observe(this, Observer {
                hideLoading()
                if (it.isNotEmpty())
                    ProductsSheet().apply {
                        setProductList(it)
                        setProductCallback(this@HomeActivity)
                        show(supportFragmentManager, "ProductsFragment")
                    }
                else
                    showToast(R.string.not_products_available, Toast.LENGTH_LONG)
            })
    }

    private fun observePaymentStatus() {
        homeViewModel.getPaymentStatus().observe(this, Observer {
            hideLoading()
            if (it) homeViewModel.productPayed()
            else showToast("Paying Error!")
        })
    }

    private fun observePaymentErrors() {
        homeViewModel.getErrorObservable().observe(this, Observer {
            hideLoading()
            when (it) {
                PaymentHelper.GET_PRODUCT_INFO_ERROR ->
                    showToast(R.string.cant_load_products)
                PaymentHelper.PAY_FOR_PRODUCT_ERROR ->
                    showToast(R.string.pay_api_error, Toast.LENGTH_LONG)
                PaymentHelper.Pay_SUCCESSFUL_SIGN_FAILED ->
                    showToast(R.string.cant_verify_payment, Toast.LENGTH_LONG)
                PaymentHelper.USER_CANCEL_PAYMENT ->
                    showToast(R.string.payment_canceled)
                PaymentHelper.YOU_OWEN_PRODUCT ->
                    showToast(R.string.product_owned)
                PaymentHelper.PAY_FIELD ->
                    showToast(R.string.product_owned)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE_BUY) {
            if (data != null) {
                observePaymentStatus()
                homeViewModel.sendPaymentResultData(data)
            } else
                Toast.makeText(this, "PaymentError", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Callback for the selected product.
     * You can implement and use it to get the selected product info.
     * @param productInfo is the selected product object.
     */
    override fun onProductSelected(productInfo: ProductInfo) {
        showLoading()
        homeViewModel.goToPay(this, productInfo.productId)
    }

    override fun onStop() {
        super.onStop()
        home_video_view.stopPlayback()
    }
}