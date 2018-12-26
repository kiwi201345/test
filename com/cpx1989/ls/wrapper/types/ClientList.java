package com.cpx1989.LS.wrapper.types;

import java.util.List;

public class ClientList {
	
	private List<String> clients;
	private List<String> ips;
	
	public ClientList(){}

    public ClientList(List<String> clients, List<String> ips) {
        super();
        this.clients = clients;
        this.ips = ips;
    }
    
    public void setIPs(List<String> ips){
        this.ips = ips;
    }
    
    public void setClients(List<String> clients){
    	this.clients = clients;
    }
    
    public List<String> getIPs(){
    	return ips;
    }
    
    public List<String> getClients(){
    	return clients;
    }

}
