package com.cpx1989.LS.wrapper.types;


public class Stats {
	
	private float load;
	private long freemem, usedmem, totalmem;
	private int cores, clients;
	private String os, javaver; 
	
	public Stats(){}

    public Stats(float load, int cores, long freemem, long usedmem, long totalmem, int clients, String os, String javaver) {
        super();
        this.load = load;
        this.cores = cores;
        this.freemem = freemem;
        this.usedmem = usedmem;
        this.totalmem = totalmem;
        this.clients = clients;
        this.os = os;
        this.javaver = javaver;
    }
	
	public float getLoad() {
		return load;
	}

	public void setLoad(float load) {
		this.load = load;
	}

	public int getCores() {
		return cores;
	}

	public void setCores(int cores) {
		this.cores = cores;
	}

	public long getFreemem() {
		return freemem;
	}

	public void setFreemem(long freemem) {
		this.freemem = freemem;
	}

	public long getTotalmem() {
		return totalmem;
	}

	public void setTotalmem(long totalmem) {
		this.totalmem = totalmem;
	}

	public int getClients() {
		return clients;
	}

	public void setClients(int clients) {
		this.clients = clients;
	}

	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}

	public String getJavaver() {
		return javaver;
	}

	public void setJavaver(String javaver) {
		this.javaver = javaver;
	}

	public long getUsedmem() {
		return usedmem;
	}

	public void setUsedmem(long usedmem) {
		this.usedmem = usedmem;
	}
	
	
}
