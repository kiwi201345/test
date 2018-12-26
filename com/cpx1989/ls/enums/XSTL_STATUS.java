package com.cpx1989.LS.enums;

public enum XSTL_STATUS{
	NOT_AUTHED(0x10000001),
	UPDATE(0x20000002),
	EXPIRED(0x30000003),
	ERROR(0x40000004),
	TAMPERED(0x50000005),
	XNOTIFY(0x60000006),
	BAD_TOKEN(0x70000007),
	BLACKLIST(0x80000008),
	MSGBOXUI(0x90000009),
	AUTHED(0xA5000000),
	AUTHEDND(0xA5D00000),
	AUTHEDES(0xA5E00000),
	LIFETIME(0xA5F00000);
	private final int value;
	private XSTL_STATUS(int value) {
		this.value = value;
	}
	public int getValue() {
        return this.value;
    }
}
