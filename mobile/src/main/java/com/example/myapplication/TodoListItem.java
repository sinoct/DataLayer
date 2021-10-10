package com.example.myapplication;

public class TodoListItem implements java.io.Serializable {
    public String title;
    public Boolean status;

    public TodoListItem(String name, Boolean completed) {
        title = name;
        status = completed;
    }
}
