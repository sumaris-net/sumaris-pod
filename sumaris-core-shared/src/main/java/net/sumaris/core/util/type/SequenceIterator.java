package net.sumaris.core.util.type;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import java.util.Iterator;

public class SequenceIterator implements Iterator<Integer> {

	private int sequence = 0;
	private int increment = 1;

	public SequenceIterator() {
		sequence = 0;
		this.increment = 1;
	}

	public SequenceIterator(int initValue, int increment) {
		sequence = initValue;
		this.increment = increment;
	}

	@Override
	public boolean hasNext() {
		return true;
	}

	@Override
	public Integer next() {
		this.sequence += increment;
		return sequence;
	}

	@Override
	public void remove() {
		// LP : special behavior for this sequence iterator : it decrement the sequence by one increment
		sequence -= increment;
		// the next call of next() will return the actual sequence number
	}

	public int getCurrentValue() {
		return sequence;
	}

}
