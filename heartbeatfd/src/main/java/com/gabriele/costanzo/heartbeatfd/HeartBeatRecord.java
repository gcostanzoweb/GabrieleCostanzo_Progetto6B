package com.gabriele.costanzo.heartbeatfd;

import java.io.Serializable;
import java.time.Instant;

public class HeartBeatRecord implements Serializable {
    // Campi del VALUE del KafkaMessage
    private long time = Instant.now().getEpochSecond();
    private Object status;
    private String service;

    // Classe one-shot per i messaggi di Expire
    private class ErrorServerUnavailable implements Serializable {
        private String serverUnavailable = "No heart-beat received";
    }

    // Costruttore per i record dell'heartbeatMap o per i KafkaMessage di Down
    public HeartBeatRecord(HeartBeat heartbeat){
        this.status = heartbeat;
        this.service = heartbeat.getServiceName();
    }

    // Costruttore per i KafkaMessage di Expire
    public HeartBeatRecord(String service){
        this.status = new ErrorServerUnavailable();
        this.service = service;
    }

    public long getTime() {
        return time;
    }

    public Object getStatus() {
        return status;
    }

    public String getService() {
        return service;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setStatus(Object status) {
        this.status = status;
    }

    public void setService(String service) {
        this.service = service;
    }
}

