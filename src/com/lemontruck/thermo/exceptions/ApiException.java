package com.lemontruck.thermo.exceptions;

public class ApiException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ApiException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
	
	public ApiException(String detailMessage) {
        super(detailMessage);
    }
}
