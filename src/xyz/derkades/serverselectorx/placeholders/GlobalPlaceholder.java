package xyz.derkades.serverselectorx.placeholders;

public class GlobalPlaceholder extends Placeholder {

	private final String value;

	public GlobalPlaceholder(final String key, final String value) {
		super(key);
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

}
