package edu.uwm.util;

/**
 * An error while parsing JSON.
 */
public class JSONParseError extends Error {

	/**
	 * KEH
	 */
	private static final long serialVersionUID = 1L;

	public JSONParseError(String reason) {
		super(reason);
	}
}
