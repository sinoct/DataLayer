package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class TodoListActivity extends MainActivity {

    ListView todoListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.todolist_layout);
        todoListView = findViewById(R.id.todoListView);
        ArrayList<String> lista =  new ArrayList<String>();
        for (String item : MainActivity.todoList) {
            lista.add(item);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.simple_text,lista);
        todoListView.setAdapter(adapter);

        todoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setBackgroundColor(getColor(R.color.selected));
            }
        });

    }
}
