package com.uade.corehub.channels;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "corehub")
public class ChannelRegistryProperties {
	private List<Channel> channels;
	public List<Channel> getChannels() { return channels; }
	public void setChannels(List<Channel> channels) { this.channels = channels; }

	public static class Channel {
		private String name;
		private String exchange;
		private String routingKey;

		public String getName() {return name;}
		public void setName(String name){this.name=name;}
		public String getExchange(){return exchange;}
		public void setExchange(String exchange){this.exchange=exchange;}
		public String getRoutingKey(){return routingKey;}
		public void setRoutingKey(String routingKey){this.routingKey=routingKey;}
	}
}
