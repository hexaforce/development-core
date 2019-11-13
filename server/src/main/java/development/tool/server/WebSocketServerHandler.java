package development.tool.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.protobuf.ByteString;

import development.tool.common.AbstractWebSocketHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketServerHandler extends AbstractWebSocketHandler {

	private final Map<String, StreamRecognize> process = new ConcurrentHashMap<String, StreamRecognize>();

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message, String text) throws Exception {
		try {
			session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Text messages not supported"));
		} catch (IOException ex) {
			// ignore
		}
	}

	@Override
	protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message, byte[] binary) throws Exception {
		process.get(session.getId()).getSharedQueue().put(ByteString.copyFrom(binary));
	}

	@Override
	protected void openSession(WebSocketSession session) {
		log.info(session.getId());
		StreamRecognize streamRecognize = new StreamRecognize(session);
		process.put(session.getId(), streamRecognize);
		new Thread(streamRecognize).start();
	}

	@Override
	protected void closeSession(WebSocketSession session) {
		log.info(session.getId());
		process.get(session.getId()).setExecution(false);
		process.remove(session.getId());
	}

}