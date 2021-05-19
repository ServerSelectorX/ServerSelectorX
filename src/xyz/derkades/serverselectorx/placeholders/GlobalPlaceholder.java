package xyz.derkades.serverselectorx.placeholders;

import java.util.Objects;

public class GlobalPlaceholder extends Placeholder {

	private final String value;

	public GlobalPlaceholder(final String key, final String value) {
		super(key);
		this.value = Objects.requireNonNull(value, "Placeholder value is null");
	}

	public String getValue() {
		return this.value;
	}

}
