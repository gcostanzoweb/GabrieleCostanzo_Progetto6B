# Progetto "6B" per DSBD 2020/21

## Heartbeat Fault Detector, con Spring WebFlux
###Di: Gabriele Costanzo, matr. 1000014221

---

#1. Introduzione
Il progetto 6 variante B prevedeva lo sviluppo di un Fault Detector basato su meccanismo Heartbeat comprendente delle seguenti tecnologie:

- Spring Boot con Spring WebFlux
- Maven (gestione delle dipendenze)
- Kafka e Zookeeper
- Docker e Docker Compose

Non è prevista permanenza dei dati raccolti, i quali sono mantenuti in una HashMap in memoria, come da specifica.

---

#2. Avvio e Testing
Dall'interno del root del progetto, sarà sufficiente compiere i seguenti 3 step:
1. Modificare il file `.env` con i propri valori preferiti:
```
# Check every TASK_DELAY milliseconds
TASK_DELAY = 5000
# Alert inactivity after PERIOD seconds
PERIOD = 30
```
2. Aprire una shell nel root ed eseguire `docker-compose up`
    - Ci si può accertare che i container si siano avviati completamente poiché un messaggio schedulato ogni `TASK_DELAY` 
      millisecondi dovrebbe apparire nella console.
      
3. Si può monitorare lo stato del topic `logging` di Kafka aprendo un'altra shell nel root del progetto ed eseguendo:
```
docker-compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic logging
```

---

A questo punto, per testare il corretto funzionamento del progetto, si potrà utilizzare il proprio client HTTP per inviare
le richieste POST sulla porta `8080`.

Ad esempio, utilizzando Postman, sarà sufficiente configurare i seguenti campi:
- Metodo: `POST`
- URL: `http://127.0.0.1:8080/ping`
- Headers:
```
Content-Type: application/json
```
- Body (esempio):
```
{
"serviceName": "hostname",
"serviceStatus": "up",
"dbStatus": "up"
}
```

---

#3. Funzionamento
Si distinguono, per comodità, due tipi distinti di Fault:
###Down:
Si verifica quando si riceve un Heartbeat che contenga lo stato `down` in almeno uno dei due campi `serviceStatus` e `dbStatus`.
In tal caso, sul topic `logging` di Kafka verrà inviato un messaggio del tipo:
```
{
"key":"service_down",
"value":
    {
    "time": 14000000,
    "status":
        {
        "serviceName": "hostname",
        "serviceStatus": "down",
        "dbStatus": "up"
        },
    "service":"hostname"
    }
}
```

###Expire:
Si verifica quando il task scheduled si accorge - per un dato servizio - che:
```
[ Instant.now().getEpochSecond() - heartbeatRecord.getTimestamp() ] > PERIOD
```
In tal caso, sul topic `logging` di Kafka verrà inviato un messaggio del tipo:
```
{
"key":"service_down",
"value":
    {
    "time": 14000000,
    "status":
        {
        "serverUnavailable": 'No heart-beat received'
        },
    "service":"hostname"
    }
}
```

Si è sfruttata la serializzazione degli oggetti per rispondere a due necessità:
- ottenere un POJO Heartbeat da una richiesta JSON ricevuta
- generare un messaggio JSON per Kafka da un POJO HeartbeatRecord
    - in tal caso, per distinguere i messaggi di `Down` e `Expire` si è sfruttata la stessa classe di partenza,
    `HeartBeatRecord`, da usare come value; tuttavia, in base ai due casi, si riempie diversamente il suo campo `status`
      
    - nel caso `Expire` si è preparata una classe privata `ErrorServerUnavailable` per riempire staticamente il campo `status`
    con il messaggio di `serverUnavailable`
      
Per scelta implementativa, se il servizio comporta un KafkaMessage di `Down`, si attenderà ugualmente il cooldown stabilito.
Nel caso in cui non si dovesse ricevere nessuna notifica di "Up" del servizio, seguirà anche un KafkaMessage di `Expire` per consolidare la non-disponibilità del servizio.

Se si è ricevuto solo il KafkaMessage di `Down`, si può concludere che un nuovo Heartbeat di "Up" è stato ricevuto.

---

#4. Struttura
Le principali classi del progetto sono presenti nel package di base (`com.gabriele.costanzo.heartbeatfd`) del container `heartbeatfd`.

Si distinguono:
###- 2 classi Modello:
##HeartBeat
Rappresentano il JSON che ci si aspetta in arrivo dalla richiesta HTTP:
```
{
    serviceName: String,
    serviceStatus: String,
    dbStatus: String
}
```
Per politica di implementazione, su `serviceStatus` e `dbStatus` si accetta solo la stringa `up` (case-sensitive)
come stato di "Up", e qualunque altra come stato di "Down".

Possiede un metodo `isAlive()` per verificare che entrambi gli status siano "Up".
##HeartBeatRecord
Classe di supporto che viene utilizzata a due scopi:
- sia come contenuto Object della HashMap<Host, Object> dello stato dei servizi
- sia come Value dei KafkaMessage
  
Il formato generico dell'HeartBeatRecord rappresenta il seguente messaggio JSON:
```
{
"key":"service_down",
"value":
    {
    "time": 14000000,
    "status":
        {
        ...
        },
    "service":"hostname"
    }
}
```
Il contenuto di `status` dipende dal tipo di costruttore utilizzato:
- sarà uguale al contenuto dell'Heartbeat ricevuto se si passa nel costruttore
- sarà uguale ad un messaggio costante di `serviceUnavailable` se si passa il nome del servizio nel costruttore

###- e 4 classi dell'Applicazione Spring:
##HeartbeatFaultDetectorApplication
Punto di avvio dell'applicazione Spring. Utilizza l'annotazione `@EnableScheduling` per permettere operazioni schedulate.

##HeartBeatService
Il servizio di base per comunicare con HeartBeat e HeartBeatRecord. Prevede:
- un campo `heartbeatMap` di tipo `HashMap<String, HeartBeatRecord>` che contiene lo stato attuale dei servizi di cui si è ricevuto
almeno un Heartbeat.
- un metodo `process(HeartBeat heartbeat)` che valuta il contenuto dell'HeartBeat ricevuto dall'`HeartBeatController` e distingue due casi:
    - se `heartbeat.isAlive()` restituisce `False`, il service è `Down`, e si notificherà con un Down su Kafka
    - se `heartbeat.isAlive()` restituisce `True`, il service è `Up`: si crafta un `HeartBeatRecord` corrispondente e si aggiunge
    alla `heartbeatMap`, o si aggiorna il valore precedente
  
- un metodo `verify()` che itera su tutti gli `HeartBeatRecord` di `heartbeatMap` e verifica la funzione:
```
[ Instant.now().getEpochSecond() - heartbeatRecord.getTimestamp() ] > PERIOD
```
Nel caso in cui questa ritorna `True`, si notifica un `Expire` su Kafka, e si pone il `timer` dell'`HeartBeatRecord`
ad un valore di errore pari a `-1`.
In questo modo, alle prossime iterazioni, il Record in questione verrà saltato, in attesa di un nuovo HeartBeat che riconfermi lo stato di "Up".

##HeartBeatScheduler
Si tratta di un Component monouso, il cui scopo è quello di invocare `verify()` su `HeartBeatService` con un delay fisso pari a `TASK_DELAY` in millisecondi (dall'env file).

##HeartBeatController
Si tratta di un `RestController` che espone un endpoint `POST: /ping` che si attende una Request serializzabile a POJO `HeartBeat`
e fornisce in risposta un `HTTP 202 Accepted`.

E' previsto anche un `@ExceptionHandler` per rispondere a richieste malformate con un `HTTP 400 Bad Request`,
notificando anche nella console di Docker.

---

In aggiunta, nel package `kafka` sono presenti due classi per la gestione della comunicazione con Kafka.

##KafkaProducerConfig
Una classe di `@Configuration` che rende il container Kafka visibile alla Spring Application, e gestisce la creazione (o verifica) di un topic `logging`.

##KafkaMessage
Un POJO di supporto che prevede:
- un campo `key`, costante e pari a `service_down`
- un campo `value` di tipo `HeartBeatRecord`, riempito in accordo alle regole della classe per rappresentare un valore di `Down` o di `Expire`.

---

#5. Conclusioni
Il microservizio è stato progettato tenendo in considerazione:
- semplicità di build dei container
- semplicità di testing dei Topic Kafka
- semplicità di testing di invio e ricezione delle HTTP Request tramite Postman

L'output su console Docker Compose è stato progettato per essere intuitivo e fornire costantemente un aggiornamento sullo
stato interno dell'applicazione, senza per questo causare un `information overload` all'utente con nozioni non-utili.

In vista di possibili sviluppi futuri dell'applicazione, si prevedono le seguenti aggiunte possibili:
- sistema di setup delle variabili di `environment` più user-friendly e più facilmente scalabile con l'aggiunta di
variabili nell'`application.properties` della Spring Application
- aggiunta di controlli più rigidi (Exception Handlers più specifici, suite di Testing)
- organizzazione più scalabile delle classi in package separati