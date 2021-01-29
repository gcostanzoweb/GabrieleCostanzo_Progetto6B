package com.gabriele.costanzo.heartbeatfd;

import com.gabriele.costanzo.heartbeatfd.kafka.KafkaMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.time.Instant;

@Service
@EnableScheduling
public class HeartBeatService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafkaTopicLogging")
    private String topicLogging;


    @Value(value = "${maxDelay}")
    private String maxDelay;

    private HashMap<String, HeartBeatRecord> heartbeatMap = new HashMap<>();

    public void process(HeartBeat heartbeat){
        HeartBeatRecord record = new HeartBeatRecord(heartbeat);
        if(!heartbeat.isAlive()) notifyServiceDown(record);
        else {
            String service = heartbeat.getServiceName();
            if (heartbeatMap.containsKey(service)) {
                System.out.println("Aggiorno il servizio: " + heartbeat.toString());
                heartbeatMap.remove(service);
            } else System.out.println("Aggiungo nuovo servizio: " + heartbeat.toString());
            heartbeatMap.put(service, record);
            System.out.println("Totale servizi: " + heartbeatMap.size());
        }
    }

    public void verify(){
        int intMaxDelay = Integer.parseInt(this.maxDelay);

        long currentTime = Instant.now().getEpochSecond();
        int numRecords = this.heartbeatMap.size();
        System.out.println("Scheduled task running: "+currentTime+", "+numRecords+" servizi registrati");

        if (numRecords > 0) {
            System.out.println("Map not empty.");
            Iterator it = heartbeatMap.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry<String, HeartBeatRecord> pair = (HashMap.Entry<String, HeartBeatRecord>) it.next();

                HeartBeatRecord record = pair.getValue();
                String service = pair.getKey();
                long recordTime = record.getTime();
                long delay = currentTime - recordTime;
                System.out.println("\t- Servizio \"" + service + "\" last updated " + delay+" secs ago.");
                if (delay > intMaxDelay) {
                    System.out.println("Il servizio " + service + " è down.");
                    notifyServiceExpired(record);
                    it.remove();
                }
            }
        }
    }

    public void notifyServiceDown(HeartBeatRecord record){
        KafkaMessage msg = new KafkaMessage(record);
        kafkaTemplate.send(topicLogging, msg.toString());
        System.out.println("Avviso di Down inviato su Kafka.");
    }

    public void notifyServiceExpired(HeartBeatRecord record){
        String service = record.getService();
        HeartBeatRecord newRecord = new HeartBeatRecord(service);
        KafkaMessage msg = new KafkaMessage(newRecord);
        kafkaTemplate.send(topicLogging, msg.toString());
        System.out.println("Avviso di Expire inviato su Kafka.");
    }
}
