package com.example.gitlabscanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitLabProjectResponse(
		Long id,
		@JsonProperty("path_with_namespace") String pathWithNamespace,
		String visibility,
		@JsonProperty("default_branch") String defaultBranch,
		@JsonProperty("web_url") String webUrl) {
}
