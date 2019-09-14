package xyz.derkades.serverselectorx.placeholders;

public abstract class Placeholder {
	
	private final String key;
	
	public Placeholder(final String key) {
		this.key = key;
	}
	
	public String getKey() {
		return this.key;
	}

}
