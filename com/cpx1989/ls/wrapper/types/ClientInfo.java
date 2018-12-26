package com.cpx1989.LS.wrapper.types;

public class ClientInfo {
	
	private String name;
	private String key;
	
	public ClientInfo(){}

    public ClientInfo(String name, String key) {
        super();
        this.name = name;
        this.key = key;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getkey() {
        return key;
    }
    
    public void setkey(String key) {
        this.key = key;
    }

}
