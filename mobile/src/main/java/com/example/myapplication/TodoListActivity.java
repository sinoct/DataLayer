package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class TodoListActivity extends AppCompatActivity {

    ListView todoListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.todolist_layout);
        todoListView = findViewById(R.id.todoListView);
        ArrayList<String> list =  new ArrayList<>();
        for (TodoListItem item : MainActivity.todoList) {
            list.add(item.title);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.simple_text,list);
        todoListView.setAdapter(adapter);

        todoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setBackgroundColor(getColor(R.color.selected));
            }
        });

    }

}
