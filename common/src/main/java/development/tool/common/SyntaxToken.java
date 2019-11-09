package development.tool.common;

import com.google.cloud.language.v1.DependencyEdge.Label;
import com.google.cloud.language.v1.PartOfSpeech.Form;
import com.google.cloud.language.v1.PartOfSpeech.Proper;
import com.google.cloud.language.v1.PartOfSpeech.Tag;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SyntaxToken {
	private String content;
	private Tag tag;
	private Form form;
	private Proper proper;
	private Label label;
}
