package com.cpx1989.LS.wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.cpx1989.LS.LS;
import com.cpx1989.LS.wrapper.types.Chat;
import com.cpx1989.LS.wrapper.types.ChatReturn;
import com.cpx1989.LS.wrapper.types.ClientInfo;
import com.cpx1989.LS.wrapper.types.ClientList;
import com.cpx1989.LS.wrapper.types.Command;
import com.cpx1989.LS.wrapper.types.GetClients;
import com.cpx1989.LS.wrapper.types.Status;

public class WrapperThread {
	
	public SocketIOServer server;
	public static String key = "PyXBd5uQaKkpvEQY";
	public static String key1 = "wxcaL2YADUMr8ZQJ";
	public static List<String> chatlog = new ArrayList<String>();
	
	public static Map<String, SocketIOClient> clients = new HashMap<String, SocketIOClient>();
	
	public void saveToChat(String name, String message){
		DateTimeFormatter fmt = DateTimeFormat.forPattern("MMM-dd hh:mm:ss a");
		DateTime now = new DateTime();
		String fnl = "["+fmt.print(now)+"] "+name+": "+message;
		chatlog.add(fnl);
		if(chatlog.size() > 151){
			List<String> newlog = new ArrayList<String>();
			for (int i = 1; i < 152; i++){
				newlog.add(chatlog.get(i).toString());
			}
			chatlog = newlog;
		}
		server.getBroadcastOperations().sendEvent("chat", new ChatReturn(name, chatlog));
	}
	
	public WrapperThread(String host, int port){
		
		Configuration config = new Configuration();
	    config.setHostname(host);
	    config.setPort(port);
	    config.setOrigin("http://xblLS.com");
	    SocketConfig sc = config.getSocketConfig();
	    sc.setReuseAddress(true);
	    config.setSocketConfig(sc);
	    
	    server = new SocketIOServer(config);
	    
	    server.addEventListener("clientInfo", ClientInfo.class, new DataListener<ClientInfo>() {
	    	@Override
	    	public void onData(SocketIOClient client, ClientInfo data, AckRequest ackRequest) {
				if (data.getName() != null && data.getName() != "" && !(data.getName().isEmpty())){
	        		String ip = client.getRemoteAddress().toString().replaceAll("/", "").split(":")[0];
					if (data.getkey().equalsIgnoreCase(key)){
						clients.put(data.getName(), client);
		        		LS.saveToLogger("\nWrapper: "+data.getName()+" connected from "+ ip);
		        		client.sendEvent("chat", new ChatReturn(data.getName(), chatlog));
					} else {
						LS.saveToLogger("\nWrapper: "+data.getName()+" failed auth\n  IP: "+ ip + "\n  Key:" + data.getkey());
					}
	        	}
	        }
	    });
	    
	    server.addEventListener("chat", Chat.class, new DataListener<Chat>() {
	    	@Override
	    	public void onData(SocketIOClient client, Chat data, AckRequest ackRequest) {
				if (data.getName() != null && data.getName() != "" && !(data.getName().isEmpty())){
	        		String ip = client.getRemoteAddress().toString().replaceAll("/", "").split(":")[0];
	        		if (data.getMessage().equalsIgnoreCase("/restart")){
	        			LS.on = false;
						new Thread(new Runnable(){
							@Override
							public void run() {
								try {
									Thread.sleep(3000);
									LS.listenServer();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}).start();
		        		LS.saveToLogger("\nWrapper: Name: "+ data.getName() + " restarted the server. IP: "+ip);
	        			saveToChat(data.getName(), "*** Restarted the server ***");
	        		} else if (data.getMessage().equalsIgnoreCase("/stop")){
	        			if (LS.on == true){
							LS.on = false;
			        		LS.saveToLogger("\nWrapper: Name: "+ data.getName() + " stopped the server. IP: "+ip);
		        			saveToChat(data.getName(), "*** Stopped the server ***");
						}
	        		} else if (data.getMessage().equalsIgnoreCase("/start")){
	        			if(LS.on != true){
							LS.on = true;
							LS.listenServer();
			        		LS.saveToLogger("\nWrapper: Name: "+ data.getName() + " started the server. IP: "+ip);
		        			saveToChat(data.getName(), "*** Started the server ***");
						}
	        		} else if (data.getMessage().equalsIgnoreCase("/cc")){
	        			chatlog = new ArrayList<String>();
		        		LS.saveToLogger("\nWrapper: Name: "+ data.getName() + " cleared the chat. IP: "+ip);
	        			saveToChat(data.getName(), "*** Cleared the chat ***");
	        		} else {
	        			if (data.getMessage().equalsIgnoreCase("")) return;
	        			System.out.println("\nWrapper: Chat - "+data.getName()+" said \""+ data.getMessage() + "\" IP: "+ip);
	        			saveToChat(data.getName(), data.getMessage());
	        		}
	        	}
	        }
	    });
	    
	    server.addEventListener("command", Command.class, new DataListener<Command>() {
	    	@Override
	    	public void onData(SocketIOClient client, Command data, AckRequest ackRequest) {
				if (data.getkey() != null && data.getkey() != "" && !(data.getkey().isEmpty())){
	        		String ip = client.getRemoteAddress().toString().replaceAll("/", "").split(":")[0];
					if (data.getkey().equalsIgnoreCase(key1)){
						if (data.getCommand().equalsIgnoreCase("restart")){
							LS.on = false;
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							LS.startServer();
			        		LS.saveToLogger("\nWrapper: IP: "+ ip + " restarted the server.");
						} else if (data.getCommand().equalsIgnoreCase("start")){
							if(LS.on != true){
								LS.on = true;
								LS.startServer();
				        		LS.saveToLogger("\nWrapper: IP: "+ip+" and started the server.");
							}
						} else if (data.getCommand().equalsIgnoreCase("stop")){
							if (LS.on == true){
								LS.on = false;
				        		LS.saveToLogger("\nWrapper: IP: "+ip+" and stopped the server.");
							}
						}
					} else {
						LS.saveToLogger("\nWrapper: Failed Auth - IP: "+ip+"\n  Key:" + data.getkey());
					}
	        		
	        	}
	        }
	    });
	    
	    server.addEventListener("status", Status.class, new DataListener<Status>() {
	    	@Override
	    	public void onData(SocketIOClient client, Status data, AckRequest ackRequest) {
				if (data.getStatus().equalsIgnoreCase(key)){
					client.sendEvent("status", new Status((LS.on == true) ? "true" : "false", "LS"));
	        	}
	        }
	    });
	    
	    server.addEventListener("getClients", GetClients.class, new DataListener<GetClients>() {
	    	@Override
	    	public void onData(SocketIOClient client, GetClients data, AckRequest ackRequest) {
				if (data.getKey().equalsIgnoreCase(key)){
					List<String> names = new ArrayList<String>();
					List<String> ips = new ArrayList<String>();
					for (Map.Entry<String, SocketIOClient> entry : clients.entrySet()) {
						if (entry.getValue() != null && entry.getValue().isChannelOpen() != false){
							names.add(entry.getKey());
							ips.add(entry.getValue().getRemoteAddress().toString().replaceAll("/", "").split(":")[0]);
						}
					}
					client.sendEvent("clientList", new ClientList(names, ips));
	        	}
	        }
	    });
	    
	    server.addDisconnectListener(new DisconnectListener() {
	        @Override
	        public void onDisconnect(SocketIOClient client) {
	        	if(clients.get(client) != null){
	        		clients.remove(client);
	        	}
	        }
	    });
	    
	   server.start();
	}
}
