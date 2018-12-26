package com.cpx1989.LS.enums;

public enum GAME_ID{
	BO2(0x1),
	BO2PUB(0x2),
	GHOST(0x3),
	GHOSTPUB(0x4),
	AW(0x5),
	AWPUB(0x6),
	DEST(0x7),
	MW3(0x8),
	BO1(0x9),
	MW2(0xA),
	WAW(0xB),
	COD4(0xC);
	private final int value;
	private GAME_ID(int value) {
		this.value = value;
	}
	public int getValue() {
        return this.value;
    }
}
