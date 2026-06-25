package fpt.capstone.service;

import fpt.capstone.dto.response.ChatbotResponse;
import fpt.capstone.entity.User;

public interface ChatbotService {
    ChatbotResponse processQuery(User user, String message);
}