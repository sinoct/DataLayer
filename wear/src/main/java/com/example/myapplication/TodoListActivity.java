package com.example.myapplication;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class TodoListActivity extends MainActivity{

    ListView todoListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.todolist_layout);
        todoListView = findViewById(R.id.todoListView);
        final ArrayList<String> lista =  new ArrayList<String>();
        for (String item : MainActivity.todoList) {
            lista.add(item);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.simple_text,lista);
        todoListView.setAdapter(adapter);

        todoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setBackgroundColor(Color.parseColor("#32ad09"));
            }
        });

    }
}
