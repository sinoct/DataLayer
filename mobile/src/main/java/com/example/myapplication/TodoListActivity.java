package com.example.myapplication;

import android.os.Bundle;
import android.widget.TextView;

public class TodoListActivity extends MainActivity {

    TextView todoListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.todolist_layout);
        todoListView = findViewById(R.id.todoListView);
        String list = "";
        for (String item : MainActivity.todoList) {
            list = list + item + "\n";
        }

        todoListView.setText(list);

    }
}
