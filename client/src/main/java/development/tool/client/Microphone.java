package development.tool.client;

import static java.lang.System.out;
import static java.lang.System.err;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

public class Microphone {

	final String DESTINATION_ENDPOINT = "ws://localhost:8080/" + WebSocketClientHandler.PATH;

	// SampleRate:16000Hz, SampleSizeInBits: 16, Number of channels: 1, Signed:
	// true, bigEndian: false
	public static final AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, false);

	// Set the system information to read from the microphone audio stream
	public static final DataLine.Info targetInfo = new Info(TargetDataLine.class, audioFormat);

	// buffer size in bytes
	final int BYTES_PER_BUFFER = 6400;

	private void recording() throws LineUnavailableException, IOException, InterruptedException, ExecutionException {

		if (!AudioSystem.isLineSupported(targetInfo)) {
			out.println("Microphone not supported");
			return;
		}

		ListenableFuture<WebSocketSession> future = new StandardWebSocketClient().doHandshake(new WebSocketClientHandler(), DESTINATION_ENDPOINT);

		TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(targetInfo);

		targetDataLine.open(audioFormat);
		targetDataLine.start();

		byte[] data = new byte[BYTES_PER_BUFFER];

		while (targetDataLine.isOpen()) {

			int numBytesRead = targetDataLine.read(data, 0, data.length);

			if ((numBytesRead <= 0) && (targetDataLine.isOpen())) {
				continue;
			}

			try {
				future.get().sendMessage(new BinaryMessage(data));
			} catch (IllegalStateException e) {
				err.println(e.getMessage());
				System.exit(0);
			}

		}

	}

	public static void main(String[] args) throws LineUnavailableException, IOException, InterruptedException, ExecutionException {
		new Microphone().recording();
	}

}
