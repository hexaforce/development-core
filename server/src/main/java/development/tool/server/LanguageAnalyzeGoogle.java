package development.tool.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.cloud.language.v1.AnalyzeEntitiesRequest;
import com.google.cloud.language.v1.AnalyzeEntitiesResponse;
import com.google.cloud.language.v1.AnalyzeSyntaxRequest;
import com.google.cloud.language.v1.AnalyzeSyntaxResponse;
import com.google.cloud.language.v1.DependencyEdge;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.EncodingType;
import com.google.cloud.language.v1.Entity;
import com.google.cloud.language.v1.EntityMention;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.PartOfSpeech;
import com.google.cloud.language.v1.TextSpan;
import com.google.cloud.language.v1.Token;

import development.tool.common.AnalyzeGoogle;
import development.tool.common.EntitiesToken;
import development.tool.common.SyntaxToken;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LanguageAnalyzeGoogle {

	public AnalyzeGoogle analyzeText(String transcript) {
		try (LanguageServiceClient language = LanguageServiceClient.create()) {
			Document doc = Document.newBuilder().setContent(transcript).setType(Type.PLAIN_TEXT).setLanguage("ja").build();
			return new AnalyzeGoogle(analyzeEntitiesText(language, doc), analyzeSyntaxText(language, doc));
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	private List<EntitiesToken> analyzeEntitiesText(LanguageServiceClient language, Document doc) {
		List<EntitiesToken> entitiesTokens = new ArrayList<>();
		AnalyzeEntitiesRequest request = AnalyzeEntitiesRequest.newBuilder().setDocument(doc).setEncodingType(EncodingType.UTF16).build();
		AnalyzeEntitiesResponse response = language.analyzeEntities(request);
		for (Entity entity : response.getEntitiesList()) {
			for (EntityMention mention : entity.getMentionsList()) {
				entity.getMetadataMap().entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).forEach(log::info);
				entitiesTokens.add(new EntitiesToken(entity.getName(), entity.getType(), entity.getSalience(), entity.getMetadataMap().entrySet(), mention.getText().getContent(), mention.getType()));
			}
		}
		return entitiesTokens;
	}

	private List<SyntaxToken> analyzeSyntaxText(LanguageServiceClient language, Document doc) {
		AnalyzeSyntaxRequest request = AnalyzeSyntaxRequest.newBuilder().setDocument(doc).setEncodingType(EncodingType.UTF16).build();
		AnalyzeSyntaxResponse response = language.analyzeSyntax(request);
		for (Token token : response.getTokensList()) {
			log.info(token.getLemma());
		}
		return response.getTokensList().stream().map(token -> japanese(token.getText(), token.getPartOfSpeech(), token.getDependencyEdge())).collect(Collectors.toList());
	}

	private SyntaxToken japanese(TextSpan text, PartOfSpeech part, DependencyEdge edge) {
		return new SyntaxToken(text.getContent(), part.getTag(), part.getForm(), part.getProper(), edge.getLabel());
	}

}
