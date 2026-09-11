package com.example.mcp_client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springaicommunity.mcp.security.client.sync.config.McpClientOAuth2Configurer.mcpClientOAuth2;

@SpringBootApplication
public class McpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpClientApplication.class, args);
    }

    @Bean
    Customizer<HttpSecurity> customizer() {
        return http -> http.with(mcpClientOAuth2());
    }

}


@Controller
@ResponseBody
class AssistantController {

    private final ChatClient chatClient;

    AssistantController(
            ToolCallbackProvider toolCallbackProvider,
            ChatClient.Builder chatClient) {
        this.chatClient = chatClient
                .defaultTools(toolCallbackProvider)
                .defaultSystem("""
                         you are an AI powered assistant to help people adopt a dog from the adoptions 
                         agency named Pooch Palace with locations in Taipei, Utrecht, Seoul, Tokyo, 
                         Singapore, Paris, Mumbai, New Delhi, Barcelona, San Francisco, and London. 
                         Information about the dogs availables will be presented below. 
                         If there is no information, then return a polite response suggesting we
                         don't have any dogs available. If somebody asks you about animals, 
                         and there's no information in the context, then feel free to source the answer 
                         from other places. If somebody asks for a time to pick up the dog, don't 
                         ask other questions: simply provide a time by consulting the tools you have available.\s
                        """)
                .build();
    }

    @GetMapping({"/ask", "/api/ask"})
    String ask(@RequestParam String question) {
        return this.chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}