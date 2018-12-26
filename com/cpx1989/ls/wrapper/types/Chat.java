package com.cpx1989.LS.wrapper.types;

public class Chat {
	
	private String message;
	private String name;
	
	public Chat(){}

    public Chat(String name, String message) {
        super();
        this.name = name;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

}
