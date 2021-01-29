package com.gabriele.costanzo.heartbeatfd;

import java.io.Serializable;

public class HeartBeat implements Serializable {
    private String serviceName;
    private String serviceStatus;
    private String dbStatus;

    public boolean isAlive(){ return this.getServiceStatus()=="up" && this.getDbStatus()=="up"; }

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
