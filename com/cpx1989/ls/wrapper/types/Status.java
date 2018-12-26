package com.cpx1989.LS.wrapper.types;

public class Status {
	
	private String status;
	private String name;
	
	public Status(){}

    public Status(String name, String status) {
        super();
        this.name = name;
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
