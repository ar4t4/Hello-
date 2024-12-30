package com.example.hello;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class MemberListAdapter extends ArrayAdapter<HashMap<String, String>> {

    private Context mContext;

    public MemberListAdapter(@NonNull Context context, @NonNull ArrayList<HashMap<String, String>> members) {
        super(context, 0, members);
        this.mContext = context; // Save context for further usage
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Inflate custom layout for list items
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_member, parent, false);
        }

        // Get the current member details
        HashMap<String, String> member = getItem(position);

        // Bind member details to the view
        TextView nameTextView = convertView.findViewById(R.id.tv_member_name);
        TextView bloodGroupTextView = convertView.findViewById(R.id.tv_member_blood_group);
        TextView collegeTextView = convertView.findViewById(R.id.tv_member_college);
        TextView homeTextView = convertView.findViewById(R.id.tv_member_home);

        if (member != null) {
            nameTextView.setText("Name: " + member.get("name"));
            bloodGroupTextView.setText("Blood Group: " + member.get("bloodGroup"));
            collegeTextView.setText("College: " + member.get("college"));
            homeTextView.setText("Home: " + member.get("home"));

            // Add click listener for full details
            convertView.setOnClickListener(v -> {
                AlertDialog.Builder dialog = new AlertDialog.Builder(mContext); // Use mContext
                dialog.setTitle("User Details");
                dialog.setMessage("Name: " + member.get("name") +
                        "\nBlood Group: " + member.get("bloodGroup") +
                        "\nCollege: " + member.get("college") +
                        "\nHome: " + member.get("home") +
                        "\nDistrict: " + member.get("district") +
                        "\nUniversity: " + member.get("university") +
                        "\nSchool: " + member.get("school") +
                        "\nEmail: " + member.get("email"));
                dialog.setPositiveButton("OK", null);
                dialog.show();
            });
        }

        return convertView;
    }
}
