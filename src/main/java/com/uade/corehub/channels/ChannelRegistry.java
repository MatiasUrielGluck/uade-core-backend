package com.uade.corehub.channels;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChannelRegistry {
	private final Map<String, ChannelRegistryProperties.Channel> byName;

	public ChannelRegistry(ChannelRegistryProperties props) {
		this.byName = new ConcurrentHashMap<>(
			props.getChannels().stream()
				.collect(Collectors.toMap(ChannelRegistryProperties.Channel::getName, c -> c))
		);
		log.info("ChannelRegistry initialized with {} channels", byName.size());
	}

	public Optional<ChannelRegistryProperties.Channel> find(String name) {
		return Optional.ofNullable(byName.get(name));
	}

	public Map<String, ChannelRegistryProperties.Channel> getAllChannels() {
		return Map.copyOf(byName);
	}

	/**
	 * Agrega un canal dinámicamente al registry
	 */
	public boolean addChannel(ChannelRegistryProperties.Channel channel) {
		if (byName.containsKey(channel.getName())) {
			log.warn("Channel {} already exists, not adding", channel.getName());
			return false;
		}
		
		byName.put(channel.getName(), channel);
		log.info("Added dynamic channel: {} -> exchange: {}, routingKey: {}", 
				channel.getName(), channel.getExchange(), channel.getRoutingKey());
		return true;
	}
}
