package com.uade.corehub.channels;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ChannelRegistry {
	private final Map<String, ChannelRegistryProperties.Channel> byName;

	public ChannelRegistry(ChannelRegistryProperties props) {
		this.byName = props.getChannels().stream()
						.collect(Collectors.toUnmodifiableMap(ChannelRegistryProperties.Channel::getName, c -> c));
	}

	public Optional<ChannelRegistryProperties.Channel> find(String name) {
		return Optional.ofNullable(byName.get(name));
	}
}
