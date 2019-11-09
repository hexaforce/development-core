package development.tool.common;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalyzeGoogle {
	private List<EntitiesToken> entities;
	private List<SyntaxToken> syntax;
}
