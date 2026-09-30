package com.example.gitlabscanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TreeItem(String id, String name, String type, String path, String mode) {
}
