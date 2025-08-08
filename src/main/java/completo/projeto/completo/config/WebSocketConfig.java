package completo.projeto.completo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // onde os clientes vão escutar
        config.setApplicationDestinationPrefixes("/app"); // prefixo das mensagens enviadas
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // endpoint para conexão
                .setAllowedOriginPatterns("*") // permite qualquer origem (ajuste em produção)
                .withSockJS(); // fallback para browsers que não suportam WS
    }
}

