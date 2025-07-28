package com.example.hello;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.hello.models.Event;
import java.util.Calendar;

public class CreateEventActivity extends AppCompatActivity {
    private TextInputEditText titleInput, descriptionInput, locationInput;
    private MaterialButton btnSelectDate, btnSelectTime, btnSelectLocation, btnCreateEvent;
    private Calendar selectedDateTime;
    private double selectedLatitude, selectedLongitude;
    private String communityId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        communityId = getIntent().getStringExtra("communityId");
        selectedDateTime = Calendar.getInstance();

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        locationInput = findViewById(R.id.locationInput);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
    }

    private void setupListeners() {
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        btnSelectLocation.setOnClickListener(v -> openLocationPicker());
        btnCreateEvent.setOnClickListener(v -> createEvent());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateTimeButton();
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showTimePicker() {
        new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);
                updateDateTimeButton();
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            false
        ).show();
    }

    private void openLocationPicker() {
        // Open map activity to select location
        // You can reuse your existing map implementation
    }

    private void updateDateTimeButton() {
        // Update button text with selected date and time
    }

    private void createEvent() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("Events");
        String eventId = eventsRef.push().getKey();

        Event event = new Event(
            title,
            description,
            communityId,
            currentUserId,
            selectedDateTime.getTimeInMillis(),
            location,
            selectedLatitude,
            selectedLongitude
        );
        event.setId(eventId);

        if (eventId != null) {
            eventsRef.child(eventId).setValue(event)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event created successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show());
        }
    }
} 