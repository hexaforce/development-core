package development.tool.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KuromojiToken {
	private String partOfSpeechLevel1, partOfSpeechLevel2, partOfSpeechLevel3, partOfSpeechLevel4, conjugationType, conjugationForm, baseForm, reading, pronunciation;
}