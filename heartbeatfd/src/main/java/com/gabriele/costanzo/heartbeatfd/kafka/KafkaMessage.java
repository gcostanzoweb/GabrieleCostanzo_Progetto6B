package com.gabriele.costanzo.heartbeatfd.kafka;

import com.gabriele.costanzo.heartbeatfd.HeartBeatRecord;
import com.google.gson.Gson;

public class KafkaMessage {
    private String key = "service_down";
    private HeartBeatRecord value;
    /* Implementazione semplice: la struttura del value è sempre un HeartBeatRecord.
     * Cambia solo il campo interno "status" in base che sia un Down o un Expire.
     */

    public KafkaMessage(HeartBeatRecord value){
        this.value = value;
    }

    public String toString(){
        return new Gson().toJson(this);
    }
}
