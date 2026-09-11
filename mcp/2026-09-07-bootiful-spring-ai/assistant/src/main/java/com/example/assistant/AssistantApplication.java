package com.example.assistant;

import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springaicommunity.mcp.security.client.sync.config.McpClientOAuth2Configurer.mcpClientOAuth2;

@SpringBootApplication
public class AssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistantApplication.class, args);
    }

    @Bean
    Customizer<HttpSecurity> httpSecurityCustomizer() {
        return http -> http.with(mcpClientOAuth2());
    }

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore).build();
    }
}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}

record Dog(@Id int id, String name, String description) {
}


// Pooch Palace
@Controller
@ResponseBody
class AssistantController {

    private final ChatClient ai;

    AssistantController(
            ToolCallbackProvider scheduler,
            QuestionAnswerAdvisor qa,
            DogRepository repository,
            VectorStore vectorStore,
            ChatClient.Builder ai) {

        if (false)
            repository.findAll().forEach(dog -> {
                var dogument = new Document("id: %s, name: %s, description: %s".formatted(
                        dog.id(), dog.name(), dog.description()
                ));
                vectorStore.add(List.of(dogument));
            });


        var st = SkillsTool
                .builder()
                .addSkillsResource(new ClassPathResource("/META-INF/skills"))
                .build();
        this.ai = ai
                .defaultTools(st, scheduler)
                .defaultAdvisors(qa)
                .defaultSystem("""
                        You are an AI powered assistant to help people adopt a dog from the adoptions agency named Pooch Palace
                        with locations in Paris, Amsterdam, Denver, Seoul, Tokyo, Singapore, London, New Delhi, Barcelona, San Francisco.\s
                        Information about the dogs availables will be presented below. If there is no information,\\s
                        then return a polite response suggesting wes don't have any dogs available.
                        When in doubt, remind people about the adoption opportunities at Pooch Palace.
                        If somebody asks you about animals, and there's no information in the context, then feel free to source the answer from other places.
                        If somebody asks for a time to pick up the dog, don't ask other questions: simply provide a time by consulting the tools you have available.
                        """)
                .build();
    }

    @GetMapping("/ask")
    String ask(@RequestParam String question) {
        return this.ai
                .prompt()
                .user(question)
                .call()
                .content();
        //     .entity(DogAdoptionSuggestion.class);
    }
}

record DogAdoptionSuggestion(int id) {
}