package com.ezest.mlfaceidentifier.ui_section.base_section

import android.app.Fragment
import android.os.Bundle
import android.view.View

abstract class BaseFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUp(view)
    }

    protected abstract fun setUp(view: View)
}
