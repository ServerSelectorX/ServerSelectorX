package xyz.derkades.serverselectorx.utils;

public class PingException extends Exception {
	
	public PingException(final String message) {
		super(message);
	}

	public PingException(final Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = 1L;

}
