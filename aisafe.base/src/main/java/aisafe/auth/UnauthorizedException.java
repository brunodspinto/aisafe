package aisafe.auth;

/**
 * Thrown when an operation is attempted without proper authorization.
 */
public class UnauthorizedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UnauthorizedException(final String message) {
		super(message);
	}

	public UnauthorizedException(final String message, final Throwable cause) {
		super(message, cause);
	}
}

