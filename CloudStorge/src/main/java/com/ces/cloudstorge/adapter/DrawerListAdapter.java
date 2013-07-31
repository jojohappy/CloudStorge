package com.ces.cloudstorge.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ces.cloudstorge.Contract;
import com.ces.cloudstorge.R;


/**
 * Created by Jojohappy on 13-7-27.
 */
public class DrawerListAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private Context mContext;
    // 左侧菜单栏列表title
    private String[] mPlanetTitles;

    public DrawerListAdapter(Context context, LayoutInflater inflater) {
        this.mInflater = inflater;
        this.mContext = context;
        this.mPlanetTitles = this.mContext.getResources().getStringArray(R.array.do_array);
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public Object getItem(int i) {
        return mPlanetTitles[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        DrawerListTag drawerListTag;
        if (null == view) {
            view = mInflater.inflate(R.layout.navigate_drawer_list, null);
            drawerListTag = new DrawerListTag();
            drawerListTag.drawerIcon = (ImageView) view.findViewById(R.id.drawer_icon);
            drawerListTag.drawerTitle = (TextView) view.findViewById(R.id.drawer_title);
            view.setTag(drawerListTag);
        } else
            drawerListTag = (DrawerListTag) view.getTag();
        drawerListTag.drawerTitle.setText(mPlanetTitles[position]);
        switch (position + 1) {
            case Contract.DRAWER_ROOT:
                drawerListTag.drawerIcon.setImageResource(R.drawable.ic_action_root);
                break;
            case Contract.DRAWER_SHARE:
                drawerListTag.drawerIcon.setImageResource(R.drawable.ic_action_share);
                break;
            case Contract.DRAWER_TRASH:
                drawerListTag.drawerIcon.setImageResource(R.drawable.ic_action_trash);
                break;
            case Contract.DRAWER_LOGOUT:
                drawerListTag.drawerIcon.setImageResource(R.drawable.ic_action_logout);
                break;
        }

        return view;
    }

    private class DrawerListTag {
        ImageView drawerIcon;
        TextView drawerTitle;
    }

    ;
}
