package development.tool.common;

import java.util.Map.Entry;
import java.util.Set;

import com.google.cloud.language.v1.Entity;
import com.google.cloud.language.v1.EntityMention;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EntitiesToken {
	private String name;
	private Entity.Type entityType;
	private float salience;
	private Set<Entry<String, String>> entrySet;
	private String content;
	private EntityMention.Type mentionType;
}