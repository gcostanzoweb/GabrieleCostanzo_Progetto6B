package com.gabriele.costanzo.heartbeatfd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.validation.ValidationErrors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
public class HeartBeatController {
    @Autowired
    private HeartBeatService heartBeatService;

    /**
     * ROUTE: POST /ping
     * RISPOSTA: HTTP 202 Accepted
     * @param hb: oggetto Heartbeat inferito dal JSON della richiesta
     * Si effettua un "process" dell'oggetto tramite HeartBeatService.
     */
    @RequestMapping(value = {"/ping"}, method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void beat(@RequestBody HeartBeat hb){
        heartBeatService.process(hb);
    }

    @ExceptionHandler
    public ResponseEntity<String> handleException(Exception exception) {
        System.out.println("\nRichiesta malformata ricevuta...\n");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }
}
