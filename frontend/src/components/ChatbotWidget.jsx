import { useState, useRef, useEffect } from 'react';
import { sendChatbotQuery } from '../api/citizenPortalApi';
import './ChatbotWidget.css';

const DISCLAIMER = 'This chatbot provides guidance only. It does not make official approval or eligibility decisions.';

export default function ChatbotWidget() {
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState([
        {
            role: 'bot',
            text: 'Welcome! I\'m the Social Welfare Assistant. How can I help you today?',
            disclaimer: DISCLAIMER,
        },
    ]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const messagesEndRef = useRef(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const handleSend = async (e) => {
        e.preventDefault();
        if (!input.trim() || loading) return;

        const userMessage = input.trim();
        setInput('');
        setMessages((prev) => [...prev, { role: 'user', text: userMessage }]);
        setLoading(true);

        try {
            const response = await sendChatbotQuery(userMessage);
            if (response?.code === 200 && response?.data) {
                setMessages((prev) => [
                    ...prev,
                    {
                        role: 'bot',
                        text: response.data.message,
                        disclaimer: response.data.disclaimer || DISCLAIMER,
                    },
                ]);
            } else {
                setMessages((prev) => [
                    ...prev,
                    {
                        role: 'bot',
                        text: 'Sorry, I encountered an issue. Please try again.',
                        disclaimer: DISCLAIMER,
                    },
                ]);
            }
        } catch (err) {
            setMessages((prev) => [
                ...prev,
                {
                    role: 'bot',
                    text: 'Sorry, I\'m having trouble connecting. Please try again later.',
                    disclaimer: DISCLAIMER,
                },
            ]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className={`chatbot-widget ${isOpen ? 'chatbot-widget--open' : ''}`}>
            {/* Toggle Button */}
            <button
                className="chatbot-toggle"
                onClick={() => setIsOpen(!isOpen)}
                title="Chat with assistant"
            >
                {isOpen ? '✕' : '💬'}
            </button>

            {/* Chat Panel */}
            {isOpen && (
                <div className="chatbot-panel">
                    <div className="chatbot-header">
                        <h3>Social Welfare Assistant</h3>
                        <span className="chatbot-status">Online</span>
                    </div>

                    {/* Disclaimer Banner */}
                    <div className="chatbot-disclaimer">
                        ⚠️ {DISCLAIMER}
                    </div>

                    {/* Messages */}
                    <div className="chatbot-messages">
                        {messages.map((msg, idx) => (
                            <div key={idx} className={`chatbot-message chatbot-message--${msg.role}`}>
                                <div className="message-bubble">
                                    <p>{msg.text}</p>
                                </div>
                                {msg.disclaimer && msg.role === 'bot' && (
                                    <p className="message-disclaimer">{msg.disclaimer}</p>
                                )}
                            </div>
                        ))}
                        {loading && (
                            <div className="chatbot-message chatbot-message--bot">
                                <div className="message-bubble message-bubble--typing">
                                    <span className="typing-dot"></span>
                                    <span className="typing-dot"></span>
                                    <span className="typing-dot"></span>
                                </div>
                            </div>
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    {/* Input */}
                    <form className="chatbot-input-area" onSubmit={handleSend}>
                        <input
                            type="text"
                            className="chatbot-input"
                            placeholder="Type your question..."
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            disabled={loading}
                            maxLength={1000}
                        />
                        <button
                            type="submit"
                            className="chatbot-send-btn"
                            disabled={!input.trim() || loading}
                        >
                            Send
                        </button>
                    </form>
                </div>
            )}
        </div>
    );
}