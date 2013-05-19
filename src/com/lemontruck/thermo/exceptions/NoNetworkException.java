package com.lemontruck.thermo.exceptions;

public class NoNetworkException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NoNetworkException(String detailMessage) {
		super(detailMessage);
    }
}
