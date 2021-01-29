package com.gabriele.costanzo.heartbeatfd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class HeartBeatController {
    @Autowired
    private HeartBeatService heartBeatService;

    @RequestMapping(value = {"/ping"}, method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void beat(@RequestBody HeartBeat hb){
        heartBeatService.process(hb);
    }
}
