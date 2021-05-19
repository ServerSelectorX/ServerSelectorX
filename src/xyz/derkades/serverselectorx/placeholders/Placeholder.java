package xyz.derkades.serverselectorx.placeholders;

import java.util.Objects;

public abstract class Placeholder {

	private final String key;

	public Placeholder(final String key) {
		this.key = Objects.requireNonNull(key, "Key is null");
	}

	public String getKey() {
		return this.key;
	}

}
