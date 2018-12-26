package com.cpx1989.LS.enums;

public enum TOKEN_STATUS{
	ERROR(462938),
	USED(372839),
	SUCCESS(2849230),
	BLACKLIST(223829);
	private final int value;
	private TOKEN_STATUS(int value) {
		this.value = value;
	}
	public int getValue() {
        return this.value;
    }
}
