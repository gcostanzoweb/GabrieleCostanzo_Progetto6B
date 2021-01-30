package com.gabriele.costanzo.heartbeatfd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HeartBeatScheduler {

    @Autowired
    private HeartBeatService service;

    @Scheduled(fixedDelayString = "${checkInterval}")
    public void checkRecordHealth(){
        service.verify();
    }   // verifica l'expire dei servizi a intervalli di "checkInterval" ms
}
