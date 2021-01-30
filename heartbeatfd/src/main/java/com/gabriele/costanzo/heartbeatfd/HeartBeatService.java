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

    @Value("${kafkaTopicLogging}")
    private String topicLogging;


    @Value("${maxDelay}")
    private String maxDelay;

    private HashMap<String, HeartBeatRecord> heartbeatMap = new HashMap<>();

    /**
     * Funzione che processa una richiesta HTTP ricevuta contenente un oggetto Heartbeat per decidere se
     * aggiornare l'entry corrispondente in "heartbeatMap" o se inviare un notice di Down del servizio su Kafka
     * @param heartbeat: oggetto di classe Heartbeat inferito dal JSON ricevuto
     */
    public void process(HeartBeat heartbeat){
        HeartBeatRecord record = new HeartBeatRecord(heartbeat);    // Crea un HeartBeatRecord provvisorio

        if(!heartbeat.isAlive()){   // CASO 1: il servizio è Down
            notifyServiceDown(record);
            System.out.println("Ho ricevuto: " + heartbeat.toString()+"\n");
        }
        else {  // CASO 2: il servizio è up
            String service = heartbeat.getServiceName();
            if (heartbeatMap.containsKey(service)) {    // CASO 2a) il servizio era già registrato
                System.out.println("Aggiorno il servizio: " + heartbeat.toString());
                heartbeatMap.remove(service);
            }
            else {  // CASO 2b) il servizio è nuovo
                System.out.println("Aggiungo nuovo servizio: " + heartbeat.toString());
            }
            heartbeatMap.put(service, record);
            System.out.println("Totale servizi: " + heartbeatMap.size());
        }
    }

    /**
     * Funzione che verifica che non ci siano servizi presenti in "heartbeatMap" che abbiano superato
     * il delay massimo concesso fra due Heartbeat.
     * In tal caso, li elimina dal controllo successivo e notifica l'Expire a Kafka.
     * Questa funzione è chiamata da un metodo scheduled dell'HeartBeatScheduler
     */
    public void verify(){
        int intMaxDelay = Integer.parseInt(this.maxDelay);  // ottenuto dall'environment

        long currentTime = Instant.now().getEpochSecond();
        int numRecords = this.heartbeatMap.size();
        System.out.println("Scheduled task running: "+currentTime+", "+numRecords+" servizi registrati");

        if (numRecords > 0) {
            Iterator it = heartbeatMap.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry<String, HeartBeatRecord> pair = (HashMap.Entry<String, HeartBeatRecord>) it.next();

                HeartBeatRecord record = pair.getValue();
                String service = pair.getKey();
                long recordTime = record.getTime();
                if(recordTime<0) continue;  // si saltano i servizi Expired
                long delay = currentTime - recordTime;
                System.out.println("\t- Servizio \"" + service + "\" last updated " + delay+" secs ago.");

                if (delay >= intMaxDelay) {
                    System.out.println("Il servizio " + service + " è down.");
                    notifyServiceExpired(record);
                    record.setTime(-1); // elimina questo servizio dal controllo fino al prossimo update
                }
            }
            System.out.println("\n");
        }
    }

    /**
     * Questa funzione è invocata quando si riceve una richiesta HTTP contenente un Heartbeat di un servizio
     * parzialmente o totalmente down.
     * Si vuole lasciare in alterato il record per verificarne lo stato di Down.
     * @param record: record proveniente da una richiesta HTTP, da lasciare inalterato
     */
    public void notifyServiceDown(HeartBeatRecord record){
        KafkaMessage msg = new KafkaMessage(record);
        kafkaTemplate.send(topicLogging, msg.toString());
        System.out.println("\nAvviso di Down inviato su Kafka.");
    }

    /**
     * Questa funzione è invocata quando, durante il controllo periodico, si trova un servizio con un Timestamp scaduto.
     * Si vuole generare un nuovo record con uno status predefinito di "serverUnavailable" a partire dall'originale.
     * @param record: record proveniente dalla "heartbeatMap", va aggiornato lo status
     */
    public void notifyServiceExpired(HeartBeatRecord record){
        String service = record.getService();
        HeartBeatRecord newRecord = new HeartBeatRecord(service);   // nuovo record con lo stesso serviceName ma status di "serverUnavailable"
        KafkaMessage msg = new KafkaMessage(newRecord);
        kafkaTemplate.send(topicLogging, msg.toString());
        System.out.println("\nAvviso di Expire inviato su Kafka.");
    }
}
