package com.example.bootiful_javafx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class StageInitializer {

    private final SystemBrowserOAuth2Login login;
    private Label greeting;
    private TextArea output;
    private Button call;
    private Button signIn;
    private final Resource fxml = new ClassPathResource("/fxml/ui.fxml");

    static final String CLIENT_REGISTRATION_ID = "javafx";

    private final MessageClient messageClient;

    StageInitializer(SystemBrowserOAuth2Login login, //
                     MessageClient messageClient //
    ) {
        this.login = login;
        this.messageClient = messageClient;
    }


    @EventListener
    void on(StageReadyEvent event) throws Exception {
        var loader = new FXMLLoader();
        var root = (Parent) null;
        try (var fxmlInputStream = this.fxml.getInputStream()) {
            root = loader.load(fxmlInputStream);
        }
        var scene = new Scene(root);

        this.greeting = (Label) scene.lookup("#greeting");
        this.output = (TextArea) scene.lookup("#output");

        this.signIn = (Button) scene.lookup("#signIn"); //
        this.signIn.setOnAction(e -> Threads.offTheFxThread(() -> this.login.start(CLIENT_REGISTRATION_ID)));

        this.call = (Button) scene.lookup("#call");
        this.call.setOnAction(a -> {
            Threads.offTheFxThread(() -> {
                try {
                    var message = this.messageClient.message();
                    Threads.onTheFxThread(() -> {
                        this.output.setText(message.message());
                    });
                } catch (Throwable throwable) {
                    IO.println(throwable.getMessage());
                }

            });
        });

        var stage = event.stage();
        stage.setTitle("JavaFX + Spring Boot + GraalVM");
        stage.setScene(scene);
        stage.setOnHidden(_ -> System.exit(0));
        stage.show();
    }

    @EventListener
    void on(UserSignedInEvent event) {
        Threads.onTheFxThread(() -> {
            this.greeting.setText("Hello, " + event.name() + ".");
            this.output.setText(claims(event.user().getClaims()));
            this.call.setDisable(false);
        });
    }

    private String claims(Map<String, Object> claims) {
        var claimsString = new StringBuilder();
        var template = "%s: %s" + System.lineSeparator();
        for (var entry : claims.entrySet())
            claimsString.append(template.formatted(entry.getKey(), entry.getValue()));
        return claimsString.toString();
    }

}

