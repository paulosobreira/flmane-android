package com.firebaseapp.sowbreira_26fe1.fl_mane

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView

import coil3.load
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.CircleCropTransformation

class ImageAdapter(private val mContext: Context) : BaseAdapter() {

    override fun getCount(): Int = 12

    override fun getItem(position: Int): Any? = null

    override fun getItemId(position: Int): Long = 0

    // create a new ImageView for each item referenced by the Adapter
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val imageView: ImageView
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = ImageView(mContext)
            imageView.layoutParams = ViewGroup.LayoutParams(200, 200)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setPadding(8, 8, 8, 8)
        } else {
            imageView = convertView as ImageView
        }

        val path = "https://sowbreira-26fe1.firebaseapp.com/f1mane/profile/profile-$position.png"
        imageView.load(path) {
            placeholder(R.drawable.ic_user_place_holder)
            transformations(CircleCropTransformation())
        }
        return imageView
    }
}
