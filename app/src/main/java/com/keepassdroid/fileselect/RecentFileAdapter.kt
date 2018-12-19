package com.keepassdroid.fileselect

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.android.keepass.R
import kotlinx.android.synthetic.main.file_row.view.filename
import kotlinx.android.synthetic.main.file_row.view.filepath
import android.view.LayoutInflater



class RecentFileAdapter(context: Context, val items: List<String>)
    : ArrayAdapter<String>(context, R.layout.file_row, R.id.file_filename, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        var itemView = convertView
        if( itemView == null ){
            val vi = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            itemView = vi.inflate(R.layout.file_row, null)
        }
        itemView?.filename?.text = items.get(position).substringAfterLast('/')
        itemView?.filepath?.text = items.get(position)

        return itemView
    }
}
