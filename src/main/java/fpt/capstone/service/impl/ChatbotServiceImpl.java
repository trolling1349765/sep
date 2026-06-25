package fpt.capstone.service.impl;

import fpt.capstone.dto.response.ChatbotResponse;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.User;
import fpt.capstone.repository.SystemLogRepository;
import fpt.capstone.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    private static final String DISCLAIMER = "This information is for guidance purposes only and does not constitute an official approval or eligibility decision. Please contact your local social welfare office for official determinations.";

    private final SystemLogRepository systemLogRepository;

    @Override
    @Transactional
    public ChatbotResponse processQuery(User user, String message) {
        String normalizedMessage = message.toLowerCase().trim();
        String response;

        if (normalizedMessage.contains("hello") || normalizedMessage.contains("hi")
                || normalizedMessage.contains("xin chào") || normalizedMessage.contains("chào")) {
            response = "Welcome! I'm the Social Welfare Assistant. I can help you with information about social assistance policies, application procedures, and required documents. How can I assist you today?";
        } else if (normalizedMessage.contains("eligibility") || normalizedMessage.contains("eligible")
                || normalizedMessage.contains("điều kiện") || normalizedMessage.contains("đủ điều kiện")) {
            response = "To check your eligibility for a specific policy, please navigate to the policy detail page and use the 'Check Eligibility' button. You can find policies by searching in the Policy Directory above.";
        } else if (normalizedMessage.contains("document") || normalizedMessage.contains("giấy tờ")
                || normalizedMessage.contains("hồ sơ") || normalizedMessage.contains("thủ tục")) {
            response = "Required documents vary by policy. Please select a specific policy from the directory and view its details. Each policy page lists the required documents and eligibility conditions.";
        } else if (normalizedMessage.contains("apply") || normalizedMessage.contains("nộp")
                || normalizedMessage.contains("đăng ký") || normalizedMessage.contains("application")) {
            response = "Once you have reviewed a policy and confirmed your eligibility, you can click the 'Apply Now' button on the policy detail page to start your application. Make sure you have all required documents ready.";
        } else if (normalizedMessage.contains("policy") || normalizedMessage.contains("chính sách")
                || normalizedMessage.contains("trợ cấp") || normalizedMessage.contains("support")) {
            response = "You can browse all available social assistance policies in the Policy Directory above. Use the search bar to find specific policies or filter by category to explore what's available.";
        } else if (normalizedMessage.contains("thank") || normalizedMessage.contains("cảm ơn")) {
            response = "You're welcome! If you have any more questions, feel free to ask. I'm here to help guide you through social welfare policies and procedures.";
        } else {
            response = "I'm sorry, I don't have enough information to answer that question. Please try rephrasing or contact our support team for more detailed assistance. You can also browse the Policy Directory for specific policy information.";
        }

        // Audit logging
        SystemLog systemLog = SystemLog.builder()
                .userId(user != null ? user.getId() : "anonymous")
                .action("CHATBOT_QUERY")
                .entityType("CHATBOT")
                .entityId(null)
                .oldValue(null)
                .newValue("User query: " + message + " | Response: " + response)
                .createdAt(new Date())
                .build();
        systemLogRepository.save(systemLog);

        log.info("Chatbot query processed for user: {}", user != null ? user.getEmail() : "anonymous");

        return ChatbotResponse.builder()
                .message(response)
                .disclaimer(DISCLAIMER)
                .build();
    }
}