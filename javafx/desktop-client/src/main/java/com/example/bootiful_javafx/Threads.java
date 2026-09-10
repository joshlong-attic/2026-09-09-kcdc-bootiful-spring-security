package com.example.bootiful_javafx;

import javafx.application.Platform;

import java.util.function.Consumer;

class Threads {

	static void offTheFxThread(Runnable runnable) {
		offTheFxThread(runnable, _ -> {
		});
	}

	static void offTheFxThread(Runnable runnable, Consumer<Throwable> onFailure) {
		Thread.ofVirtual() //
			.name("bootiful-javafx-worker") //
			.start(() -> {
				try {
					runnable.run();
				}
				catch (Throwable throwable) {
					onTheFxThread(() -> onFailure.accept(throwable));
				}
			});
	}

	static void onTheFxThread(Runnable runnable) {
		Platform.runLater(runnable);
	}

}
