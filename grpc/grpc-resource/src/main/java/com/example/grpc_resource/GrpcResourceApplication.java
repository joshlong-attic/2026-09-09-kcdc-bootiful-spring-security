package com.example.grpc_resource;

import com.example.client_grpc.MessageRequest;
import com.example.client_grpc.MessageResponse;
import com.example.client_grpc.MessageServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class GrpcResourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrpcResourceApplication.class, args);
    }

    @Bean
    @GlobalServerInterceptor
    AuthenticationProcessInterceptor authenticationProcessInterceptor(
            GrpcSecurity grpcSecurity) throws Exception {
        return grpcSecurity
                .authorizeRequests(requests -> requests
                        .methods("MessageService/Message").authenticated()
                        .methods("grpc.*/*").permitAll()
                        .allRequests().denyAll()
                )
                .oauth2ResourceServer(c -> c.jwt(Customizer.withDefaults()))
                .build();
    }


}

@Service
class DefaultMessageService extends MessageServiceGrpc.MessageServiceImplBase {

    @Override
    public void message(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
//		var name = request.getName();
        var name = SecurityContextHolder
                .getContextHolderStrategy()
                .getContext()
                .getAuthentication()
                .getName();
        responseObserver.onNext(MessageResponse.newBuilder()
                .setMessage("Hello, " + name + "!")
                .build());
        responseObserver.onCompleted();
    }
}