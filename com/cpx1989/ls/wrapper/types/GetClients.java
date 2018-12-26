package com.cpx1989.LS.wrapper.types;

public class GetClients {

	private String name;
	private String key;
	
	public GetClients(){}

	public GetClients(String name, String key) {
        super();
        this.name = name;
        this.key = key;
    }
    
    public String getKey(){
    	return this.key;
    }
    
    public String getName(){
    	return this.name;
    }
    

}
