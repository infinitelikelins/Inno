package com.bearya.mobile.inno.adapter

import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.DiffUtil
import com.bearya.mobile.inno.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.luck.picture.lib.entity.LocalMedia

class ImageAdapter : BaseQuickAdapter<LocalMedia, BaseViewHolder>(R.layout.item_image) {

    init {
        setDiffCallback(object : DiffUtil.ItemCallback<LocalMedia>() {
            override fun areContentsTheSame(oldItem: LocalMedia, newItem: LocalMedia): Boolean =
                oldItem.fileName == newItem.fileName

            override fun areItemsTheSame(oldItem: LocalMedia, newItem: LocalMedia): Boolean =
                oldItem.path == newItem.path
        })
    }

    override fun convert(holder: BaseViewHolder, item: LocalMedia) {
        if (item.path == null)
            holder.setImageResource(R.id.message, R.drawable.ic_baseline_add_circle_24)
        else
            Glide.with(holder.itemView.context)
                .load(item.path)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(holder.getView(R.id.message) as AppCompatImageView)
    }

}