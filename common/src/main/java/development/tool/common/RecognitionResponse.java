package development.tool.common;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecognitionResponse {
	private String resultEndTime;
	private String transcript;
	private boolean _final;
	private float stability;
	private float confidence;
	private List<KuromojiToken> tokens;
	private List<EntitiesToken> entities;
	private List<SyntaxToken> syntax;
}
