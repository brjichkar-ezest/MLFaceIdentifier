package com.ezest.mlfaceidentifier.ui_section

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ViewStub
import android.widget.ImageView
import android.widget.TextView
import com.ezest.mlfaceidentifier.R


class ThankYouActivity: AppCompatActivity() {

    lateinit var ivThanksImage:ImageView
    lateinit var tvThanksText:TextView
    lateinit var tvThanksDescription:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.welcome_slide3)

        ivThanksImage=findViewById(R.id.iv_thank_you)
        tvThanksText=findViewById(R.id.tv_thank_you)
        tvThanksDescription=findViewById(R.id.tv_thanks_description)

        ivThanksImage.visibility=ViewStub.GONE;
        tvThanksText.text="Upload Successfull."
        tvThanksDescription.text="Thank you.. Your record has been stored successfully."
    }

}