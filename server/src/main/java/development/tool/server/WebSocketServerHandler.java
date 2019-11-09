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

	private final Map<String, InfiniteStreamRecognize> x = new ConcurrentHashMap<String, InfiniteStreamRecognize>();

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
		x.get(session.getId()).getSharedQueue().put(ByteString.copyFrom(binary));
	}

	@Override
	protected void openSession(WebSocketSession session) {
		log.info(session.getId());
		InfiniteStreamRecognize t = new InfiniteStreamRecognize(session);
		x.put(session.getId(), t);
		new Thread(t).start();
	}

	@Override
	protected void closeSession(WebSocketSession session) {
		log.info(session.getId());
		x.get(session.getId()).setExecution(false);
		x.remove(session.getId());
	}

}