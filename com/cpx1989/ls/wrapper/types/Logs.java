package com.cpx1989.LS.wrapper.types;

import java.util.ArrayList;
import java.util.List;

public class Logs {

	private int placeholder;
	List<String> logs = new ArrayList<String>();
	
	public Logs(){}

    public Logs(int placeholder, List<String> logs) {
        super();
        this.placeholder = placeholder;
        this.logs = logs;
    }

    public List<String> getLogs() {
        return logs;
    }
    public int getPlaceholder(){
    	return placeholder;
    }
}
