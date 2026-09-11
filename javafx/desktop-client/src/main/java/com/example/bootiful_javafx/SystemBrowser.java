package com.example.bootiful_javafx;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

interface AuthorizationBrowser {

    void open(String authorizationRequestUri);

}

@Component
class SystemBrowser implements AuthorizationBrowser {

    @Override
    public void open(String authorizationRequestUri) {
        try {
            var command = List.of("open", authorizationRequestUri);
            new ProcessBuilder(command)//
                    .start();
        } //
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
