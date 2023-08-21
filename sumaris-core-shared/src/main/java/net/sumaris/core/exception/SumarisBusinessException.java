package net.sumaris.core.exception;

/*-
 * #%L
 * Sumaris3 Core :: Sumaris3 Core Shared
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * <p>SumarisBusinessException class.</p>
 */
public abstract class SumarisBusinessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	protected int code;

	/**
	 * <p>Constructor for SumarisBusinessException.</p>
	 *
	 * @param message a {@link String} object.
	 */
	public SumarisBusinessException(int code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * <p>Constructor for SumarisBusinessException.</p>
	 *
	 * @param message a {@link String} object.
	 * @param cause a {@link Throwable} object.
	 */
	public SumarisBusinessException(int code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * <p>Constructor for SumarisBusinessException.</p>
	 *
	 * @param cause a {@link Throwable} object.
	 */
	public SumarisBusinessException(int code, Throwable cause) {
		super(cause);
		this.code = code;
	}

	public int getCode() {
		return this.code;
	}

	public Map<String, Object> toSpecification(){
		return ImmutableMap
				.of("code", getCode(),
					"message", getMessage());
	}
}
