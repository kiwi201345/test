package com.cpx1989.LS.enums;

public enum MODULE_STATUS{
	USER_OFF(3948193),
	SRV_OFF(2849239);
	private final int value;
	private MODULE_STATUS(int value) {
		this.value = value;
	}
	public int getValue() {
        return this.value;
    }
}
