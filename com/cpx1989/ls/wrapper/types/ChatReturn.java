package com.cpx1989.LS.wrapper.types;

import java.util.ArrayList;
import java.util.List;

public class ChatReturn{
	private String name;
	private List<String> msgarray = new ArrayList<String>();
	
	public ChatReturn(){}

    public ChatReturn(String name, List<String> message) {
        super();
        this.name = name;
        this.msgarray = message;
    }

    public List<String> getMsgArray() {
        return msgarray;
    }
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

}
