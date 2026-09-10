#!/usr/bin/env bash
./mvnw spring-javaformat:apply
./mvnw -DskipTests -Pnative native:compile
./target/javafx-native     