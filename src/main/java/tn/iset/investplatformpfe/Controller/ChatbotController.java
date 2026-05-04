package tn.iset.investplatformpfe.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Dto.ChatRequest;
import tn.iset.investplatformpfe.Dto.ChatResponse;
import tn.iset.investplatformpfe.Service.ChatbotService;

@RestController
@RequestMapping("/api/public/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String reply = chatbotService.chat(request.getMessages());
        return ResponseEntity.ok(new ChatResponse(reply));
    }
}

