package com.finalproject.onchat.models

class ChatMessage(val id: String, val text: String, val fromId: String, val toId: String, val timeStamp: Long) {
    constructor(): this("", "", "", "", -1)
}