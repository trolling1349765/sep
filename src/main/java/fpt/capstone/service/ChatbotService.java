package fpt.capstone.service;

import fpt.capstone.dto.request.ChatbotAskRequest;
import fpt.capstone.dto.response.ChatbotAskResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface ChatbotService {

    ChatbotAskResponse ask(ChatbotAskRequest request,
                           HttpServletRequest httpRequest,
                           HttpServletResponse httpResponse);
}
