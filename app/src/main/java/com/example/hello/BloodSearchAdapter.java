package com.example.hello;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BloodSearchAdapter extends ArrayAdapter<String> {
    private Context context;
    private List<String> names;
    private Map<String, String> phoneMap;

    public BloodSearchAdapter(Context context, List<String> names, Map<String, String> phoneMap) {
        super(context, R.layout.item_blood_search, names);
        this.context = context;
        this.names = names;
        this.phoneMap = phoneMap;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_blood_search, parent, false);
        }

        TextView userName = convertView.findViewById(R.id.userName);
        TextView phoneNumber = convertView.findViewById(R.id.phoneNumber);
        Button callButton = convertView.findViewById(R.id.callButton);

        String name = names.get(position);
        userName.setText(name);

        String phone = phoneMap.get(name);
        if (phone != null && !phone.isEmpty()) {
            phoneNumber.setText(phone);
            callButton.setVisibility(View.VISIBLE);
            callButton.setOnClickListener(v -> {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + phone));
                context.startActivity(callIntent);
            });
        } else {
            phoneNumber.setText("Phone number not available");
            callButton.setVisibility(View.GONE);
        }

        return convertView;
    }
}
