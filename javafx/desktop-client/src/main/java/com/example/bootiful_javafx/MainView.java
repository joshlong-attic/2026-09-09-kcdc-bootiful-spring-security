package com.example.bootiful_javafx;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.springframework.context.event.EventListener;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

// an ordinary Spring bean that happens to draw a window: constructor
// injection, application events, and - because it is a bean - anything
// else you would put on a Spring bean.
@Component
class MainView {

    private final MessageClient messages;

    private final AsyncTaskExecutor executor;

    private Label greeting;

    private Button call;

    private TextArea output;

    MainView(MessageClient messages, AsyncTaskExecutor applicationTaskExecutor) {
        this.messages = messages;
        this.executor = applicationTaskExecutor;
    }

    // three controls, and the whole demo: who are you, and what did the API say?
    @EventListener
    void on(StageReadyEvent event) {
        this.greeting = new Label("Nobody is signed in.");
        this.greeting.getStyleClass().add("greeting");

        this.call = new Button("Call the API");
        this.call.setDefaultButton(true);
        this.call.setOnAction(_ -> call());

        this.output = new TextArea();
        this.output.setEditable(false);

        var layout = new VBox(16, this.greeting, this.call, this.output);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(32));

        var scene = new Scene(layout, 560, 320);
        scene.getStylesheets().add("/styles.css");

        var stage = event.stage();
        stage.setTitle("Bootiful JavaFX");
        stage.setScene(scene);
        stage.setOnHidden(_ -> System.exit(0));
        stage.show();
    }

    // there is no sign-in button: the call needs a token, so Spring
    // Security goes and gets one. The work runs on one of Spring Boot's
    // virtual threads and the result lands back on the JavaFX application
    // thread, because `Platform::runLater` *is* an `Executor`.
    private void call() {
        this.call.setDisable(true);
        this.output.setText("Calling http://localhost:8081/message ...");
        CompletableFuture.supplyAsync(this.messages::message, this.executor)
                .handleAsync((message, failure) -> done(message, failure), Platform::runLater);
    }

    private Void done(Message message, Throwable failure) {
        this.output.setText(
                failure == null ? message.message() : NestedExceptionUtils.getMostSpecificCause(failure).getMessage());
        this.call.setDisable(false);
        return null;
    }

    @EventListener
    void on(UserSignedInEvent event) {
        Platform.runLater(() -> this.greeting.setText("Hello, " + event.name() + "."));
    }

}
