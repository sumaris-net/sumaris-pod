package net.sumaris.core.util.type;

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
