package com.example.kaafkaproject

import androidx.lifecycle.ViewModel

class MenuViewModel : ViewModel() {
    var selectedItems: Map<MenuItem, Int> = emptyMap()
}