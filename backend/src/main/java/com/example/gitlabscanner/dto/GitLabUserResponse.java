package com.example.gitlabscanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitLabUserResponse(
		Long id,
		String username,
		String name,
		String state) {
}
