/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.novo.utils;

import java.util.LinkedList;

/**
 *
 * @author Eunovo
 */
public class LinkedBlockingQueue {
    private final LinkedList<Object> taskList = new LinkedList<>();
    
    public void clear() {
        synchronized(taskList) {
            taskList.clear();
        }
    }
    
    public void add(Object task) {
        synchronized(taskList) {
            taskList.addLast(task);
            taskList.notify();
        }
    }
    
    public Object take() throws InterruptedException {
        synchronized(taskList) {
            while (taskList.isEmpty())
                taskList.wait();
            return taskList.removeFirst();
        }
    }
    
}
