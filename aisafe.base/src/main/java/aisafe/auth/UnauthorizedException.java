package aisafe.auth;

/**
 * Thrown when an operation is attempted without proper authorization.
 */
public class UnauthorizedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * @param message description of the authorization failure
	 */
	public UnauthorizedException(final String message) {
		super(message);
	}

	/**
	 * @param message description of the authorization failure
	 * @param cause   the underlying cause
	 */
	public UnauthorizedException(final String message, final Throwable cause) {
		super(message, cause);
	}
}

