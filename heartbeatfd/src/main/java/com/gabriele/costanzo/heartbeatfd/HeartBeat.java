package com.gabriele.costanzo.heartbeatfd;

import java.io.Serializable;

public class HeartBeat implements Serializable {
    // Campi corrispondenti a quelli attesi nel JSON della richiesta
    private String serviceName;
    private String serviceStatus;
    private String dbStatus;

    /**
     * Controlla che entrambi i campi "serviceStatus" e "dbStatus" dell'heartbeat siano "up"
     * @return true, se il service è completamente "up"; false, altrimenti
     */
    public boolean isAlive(){ return this.getServiceStatus().compareTo("up")==0 && this.getDbStatus().compareTo("up")==0; }

    public String getServiceName(){ return this.serviceName; }
    public String getServiceStatus(){ return this.serviceStatus; }
    public String getDbStatus(){ return this.dbStatus; }

    public void setServiceName(String serviceName){ this.serviceName = serviceName; }
    public void setServiceStatus(String serviceStatus){ this.serviceStatus = serviceStatus; }
    public void setDbStatus(String dbStatus){ this.dbStatus = dbStatus; }

    public String toString(){
        return "{\n\t\"serviceName\": \""+this.getServiceName()+"\",\n\t\"serviceStatus\": \""+this.getServiceStatus()+"\"\n\t\"dbStatus\": \""+this.getDbStatus()+"\"\n}";
    }
}
