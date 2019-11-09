package development.tool.server;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.atilika.kuromoji.ipadic.Tokenizer;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.OutOfRangeException;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.ByteString;

import development.tool.common.AnalyzeGoogle;
import development.tool.common.KuromojiToken;
import development.tool.common.RecognitionResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InfiniteStreamRecognize implements Runnable {

	// ~5 minutes(1 second time lag)
	private static final int STREAMING_LIMIT = 290000;

	private final WebSocketSession session;

	@Getter
	public volatile BlockingQueue<ByteString> sharedQueue = new LinkedBlockingQueue<ByteString>();

	public InfiniteStreamRecognize(WebSocketSession session) {
		this.session = session;
	}

	private final RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()//
			.setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)//
			.setLanguageCode("ja-JP")//
			.setSampleRateHertz(16000)//
			.build();

	private final StreamingRecognitionConfig streamingRecognitionConfig = StreamingRecognitionConfig.newBuilder()//
			.setConfig(recognitionConfig)//
			.setInterimResults(true)//
			.setSingleUtterance(false)//
			.build();

	private StreamController referenceToStreamController;

	private final ResponseObserver<StreamingRecognizeResponse> responseObserver = new ResponseObserver<StreamingRecognizeResponse>() {

		private final LanguageAnalyzeGoogle google = new LanguageAnalyzeGoogle();
		private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		@Override
		public void onStart(StreamController controller) {
			referenceToStreamController = controller;
		}

		@Override
		public void onResponse(StreamingRecognizeResponse response) {

			for (StreamingRecognitionResult result : response.getResultsList()) {

				float stability = result.getStability();

				long end = ((result.getResultEndTime().getSeconds() * 1000) + (result.getResultEndTime().getNanos() / 1000000));
				String resultEndTime = new Timestamp(end).toString().replace("1970-01-01 09:", "");

				boolean _final = result.getIsFinal();

				for (SpeechRecognitionAlternative alternative : result.getAlternativesList()) {

					String transcript = alternative.getTranscript();

					float confidence = alternative.getConfidence();

					RecognitionResponse jsonElement = RecognitionResponse.builder()//
							.resultEndTime(resultEndTime)//
							.transcript(transcript)//
							._final(_final)//
							.stability(stability)//
							.confidence(confidence)//
							.build();

					try {

						log.info(jsonElement.toString());

						if (session.isOpen()) {
							if (_final) {
								
								jsonElement.setTokens(tokenize(transcript));
								
								AnalyzeGoogle analyzeGoogle = google.analyzeText(transcript);
								jsonElement.setSyntax(analyzeGoogle.getSyntax());
								jsonElement.setEntities(analyzeGoogle.getEntities());
								
								session.sendMessage(new TextMessage(gson.toJson(jsonElement), true));
							} else {
								session.sendMessage(new TextMessage(new Gson().toJson(jsonElement), true));
							}
						}

					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}

				}
			}

		}
		
		private List<KuromojiToken> tokenize(String transcript) {
			return new Tokenizer().tokenize(transcript).stream().map(token -> new KuromojiToken(token.getPartOfSpeechLevel1(), token.getPartOfSpeechLevel2(), token.getPartOfSpeechLevel3(), token.getPartOfSpeechLevel4(), token.getConjugationType(), token.getConjugationForm(), token.getBaseForm(), token.getReading(), token.getPronunciation())).collect(Collectors.toList());
		}

		@Override
		public void onError(Throwable t) {
			log.info("onError");
			if (t instanceof OutOfRangeException) {
				log.warn(t.getMessage());
			} else {
				t.printStackTrace();
			}
		}

		@Override
		public void onComplete() {
			log.info("onComplete");
		}

	};

	// The first request in a streaming call has to be a configuration
	private StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder()//
			.setStreamingConfig(streamingRecognitionConfig)//
			.build();

	private ClientStream<StreamingRecognizeRequest> clientStream;

	@Setter
	public volatile boolean Execution = true;

	@Override
	public void run() {

		try (SpeechClient client = SpeechClient.create()) {

			this.clientStream = client.streamingRecognizeCallable().splitCall(responseObserver);

			clientStream.send(request);

			long startTime = System.currentTimeMillis();

			while (Execution) {

				if (System.currentTimeMillis() - startTime >= STREAMING_LIMIT) {

					finish();

					clientStream = client.streamingRecognizeCallable().splitCall(responseObserver);

					request = StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingRecognitionConfig).build();

				} else {

					request = StreamingRecognizeRequest.newBuilder().setAudioContent(sharedQueue.take()).build();

				}

				clientStream.send(request);

			}

		} catch (IOException | InterruptedException e) {
			log.error(e.getMessage(), e);
		}

		finish();

	}

	private void finish() {

		clientStream.closeSend();

		referenceToStreamController.cancel();// remove Observer

	}

}
