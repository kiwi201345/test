package com.cpx1989.LS.wrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.joda.time.DateTime;

import com.cpx1989.LS.LS;
import com.cpx1989.LS.wrapper.types.Stats;
import com.jezhumble.javasysmon.CpuTimes;
import com.jezhumble.javasysmon.JavaSysMon;

public class Metrics {
	
	private JavaSysMon monitor;
	private CpuTimes now, prev;
	private Stats stats;
	private Timer runner1, runner2, runner3;
	private static Map<String, DateTime> pings = new HashMap<String, DateTime>();
	
	public Metrics(){
		monitor = new JavaSysMon();
        if (!monitor.supportedPlatform()) {
        	LS.saveToLogger("Performance monitoring unsupported! Disabling Metrics.");
            monitor = null;
            return;
        } else {
            now = monitor.cpuTimes();
        }
		stats = new Stats(0,0,0,0,0,0,"","");
		
		runner1 = new Timer();
		runner2 = new Timer();
		runner3 = new Timer();
		//This is where we get the stats
		runner1.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				prev = now;
				now = monitor.cpuTimes();
				stats.setLoad(now.getCpuUsage(prev)* 100);
				stats.setCores(monitor.numCpus());
				stats.setFreemem(Math.round(Runtime.getRuntime().freeMemory() / 1048576));
				stats.setUsedmem(Math.round((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576));
				stats.setTotalmem(Math.round(Runtime.getRuntime().totalMemory() / 1048576));
				stats.setClients(getClients());
				stats.setOs(monitor.osName());
				stats.setJavaver(Runtime.getRuntime().getClass().getPackage().getImplementationVersion());
			}
		}, 0, 100);

		//This is where we send the stats
		runner2.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				LS.ioserver.server.getBroadcastOperations().sendEvent("stats", stats);
			}
		}, 0, 1000);
		
		//This calls garbage collect every 10 minutes
		runner3.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				System.gc();
			}
		}, 600000, 600000);
	}

	protected int getClients() {
		int tmp = 0;
		DateTime now = new DateTime(System.currentTimeMillis());
		if (!pings.isEmpty()){
			Map<String, DateTime> tmpPings = new HashMap<String, DateTime>(pings);
			for (Entry<String, DateTime> entry : tmpPings.entrySet()) {
				if (entry.getValue().isAfter(now.minusSeconds(60))){
					tmp += 1;
				} else {
					pings.remove(entry.getKey());
				}
			}
		}
		return tmp;
	}
	
	public static void logClient(String cpukey){
		pings.put(cpukey, new DateTime(System.currentTimeMillis()));
	}
}
