package com.cpx1989.LS.wrapper.types;

public class Command {
	
	private String key;
	private String cmd; 
	private String name;
	
	public Command(){}

    public Command(String name, String cmd, String key) {
        super();
        this.cmd = cmd;
        this.key = key;
        this.name = name;
    }

    public String getkey() {
        return key;
    }
    
    public String getName(){
    	return name;
    }

    public String getCommand() {
        return cmd;
    }
}
