package com.gabriele.costanzo.heartbeatfd.kafka;

import com.gabriele.costanzo.heartbeatfd.HeartBeatRecord;
import com.google.gson.Gson;

public class KafkaMessage {
    private String key = "service_down";
    private HeartBeatRecord value;

    public KafkaMessage(HeartBeatRecord value){
        this.value = value;
    }

    public String toString(){
        return new Gson().toJson(this);
    }
}
