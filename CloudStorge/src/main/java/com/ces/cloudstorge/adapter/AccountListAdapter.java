package com.ces.cloudstorge.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ces.cloudstorge.R;

import java.util.List;

/**
 * Created by Jojohappy on 13-8-11.
 */
public class AccountListAdapter extends ArrayAdapter {
    private LayoutInflater mInflater = null;
    private List<String> listAccount;

    public AccountListAdapter(Context context, int textViewResourceId, List objects, LayoutInflater inflater) {
        super(context, textViewResourceId, objects);
        listAccount = objects;
        mInflater = inflater;
    }

    @Override
    public int getCount() {
        return listAccount.size();
    }

    @Override
    public Object getItem(int i) {
        return listAccount.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        if (null == view) {
            view = mInflater.inflate(R.layout.navigate_drawer_header, viewGroup, false);
            view.setTag(1);
        }
        String accountName = listAccount.get(position);
        TextView textView = (TextView) view.findViewById(R.id.drawer_header_title);
        textView.setText(accountName);
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = mInflater.inflate(R.layout.navigate_drawer_header, parent, false);
            convertView.setTag(1);
        }
        String accountName = listAccount.get(position);
        TextView textView = (TextView) convertView.findViewById(R.id.drawer_header_title);
        textView.setText(accountName);
        return convertView;
    }
}
