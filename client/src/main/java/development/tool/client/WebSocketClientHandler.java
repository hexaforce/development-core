package development.tool.client;

import java.io.IOException;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import development.tool.common.AbstractWebSocketHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketClientHandler extends AbstractWebSocketHandler {

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message, String text) throws Exception {
		log.info(text);
	}

	@Override
	protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message, byte[] binary) throws Exception {
		try {
			session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Binary messages not supported"));
		} catch (IOException ex) {
			// ignore
		}
	}

	@Override
	protected void openSession(WebSocketSession session) {
		log.info(session.getId());
	}

	@Override
	protected void closeSession(WebSocketSession session) {
		log.info(session.getId());
	}

}
