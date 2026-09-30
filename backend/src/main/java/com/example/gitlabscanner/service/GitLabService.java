package com.example.gitlabscanner.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.example.gitlabscanner.dto.GitLabProjectResponse;
import com.example.gitlabscanner.dto.GitLabUserResponse;
import com.example.gitlabscanner.dto.TreeItem;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GitLabService {

	private final RestClient restClient;
	private final String baseUrl;
	private final boolean configured;

	public GitLabService(
			@Value("${gitlab.base-url}") String baseUrl,
			@Value("${gitlab.token:}") String token) {
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		this.configured = token != null && !token.isBlank();
		RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
		if (configured) {
			builder.defaultHeader("PRIVATE-TOKEN", token);
		}
		this.restClient = builder.build();
	}

	public GitLabUserResponse currentUser() {
		return get("/api/v4/user", GitLabUserResponse.class);
	}

	public List<GitLabProjectResponse> listProjects() {
		requireToken();
		List<GitLabProjectResponse> projects = restClient.get()
				.uri(builder -> builder.path("/api/v4/projects")
						.queryParam("membership", "true")
						.queryParam("simple", "true")
						.queryParam("per_page", "100")
						.build())
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw gitlabFailure(response);
				})
				.body(new ParameterizedTypeReference<>() {
				});
		return projects == null ? List.of() : projects;
	}

	public List<GitLabProjectResponse> getUserProjects(String username) {
		return listPaged("/api/v4/users/" + encodePath(username) + "/projects");
	}

	public List<GitLabProjectResponse> getGroupProjects(String group) {
		return listPaged("/api/v4/groups/" + encodePath(group) + "/projects");
	}

	public List<TreeItem> getRepositoryTree(Long projectId) {
		List<TreeItem> files = new java.util.ArrayList<>();
		for (int page = 1; page <= 10; page++) {
			int currentPage = page;
			List<TreeItem> batch;
			try {
				batch = restClient.get()
						.uri(builder -> builder.path("/api/v4/projects/{id}/repository/tree")
								.queryParam("recursive", "true")
								.queryParam("per_page", "100")
								.queryParam("page", currentPage)
								.build(projectId))
						.retrieve()
						.onStatus(status -> status.value() == 404, (request, response) -> {
							throw new ResponseStatusException(HttpStatus.NOT_FOUND, "empty repository");
						})
						.onStatus(HttpStatusCode::isError, (request, response) -> {
							throw gitlabFailure(response);
						})
						.body(new ParameterizedTypeReference<>() {
						});
			}
			catch (ResponseStatusException ex) {
				if (ex.getStatusCode().value() == 404) {
					return files;
				}
				throw ex;
			}
			if (batch == null || batch.isEmpty()) {
				break;
			}
			files.addAll(batch);
			if (batch.size() < 100) {
				break;
			}
		}
		return files;
	}

	public String getFileContent(Long projectId, String filePath, String ref) {
		String encodedPath = encodePath(filePath);
		String refQuery = ref == null || ref.isBlank() ? "" : "?ref=" + encodePath(ref);
		URI fileUri = URI.create(baseUrl + "/api/v4/projects/" + projectId + "/repository/files/" + encodedPath + "/raw" + refQuery);
		try {
			return restClient.get()
					.uri(fileUri)
					.retrieve()
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw gitlabFailure(response);
					})
					.body(String.class);
		}
		catch (RuntimeException ex) {
			return null;
		}
	}

	public String describeProject(String projectPath) {
		GitLabProjectResponse project = getProject(projectPath);
		return "id=%d, path=%s, visibility=%s, defaultBranch=%s, webUrl=%s".formatted(
				project.id(),
				project.pathWithNamespace(),
				project.visibility(),
				project.defaultBranch(),
				project.webUrl());
	}

	private GitLabProjectResponse getProject(String projectPath) {
		requireToken();
		GitLabProjectResponse memberProject = listProjects().stream()
				.filter(project -> projectPath.equals(project.pathWithNamespace()))
				.findFirst()
				.orElse(null);
		if (memberProject != null) {
			return memberProject;
		}
		try {
			GitLabProjectResponse project = fetchProjectByPath(projectPath);
			if (project != null && project.pathWithNamespace() != null) {
				return project;
			}
		}
		catch (ResponseStatusException ex) {
			if (ex.getStatusCode().value() != 403 && ex.getStatusCode().value() != 404) {
				throw ex;
			}
		}
		throw new ResponseStatusException(HttpStatus.NOT_FOUND, "GitLab project not found: " + projectPath);
	}

	private List<GitLabProjectResponse> listPaged(String path) {
		List<GitLabProjectResponse> projects = new java.util.ArrayList<>();
		for (int page = 1; page <= 5; page++) {
			int currentPage = page;
			List<GitLabProjectResponse> batch = restClient.get()
					.uri(builder -> builder.path(path)
							.queryParam("simple", "true")
							.queryParam("per_page", "100")
							.queryParam("page", currentPage)
							.build())
					.retrieve()
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw gitlabFailure(response);
					})
					.body(new ParameterizedTypeReference<>() {
					});
			if (batch == null || batch.isEmpty()) {
				break;
			}
			projects.addAll(batch);
			if (batch.size() < 100) {
				break;
			}
		}
		return projects;
	}

	private String encodePath(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private GitLabProjectResponse fetchProjectByPath(String projectPath) {
		String encodedPath = encodePath(projectPath);
		URI projectUri = URI.create(baseUrl + "/api/v4/projects/" + encodedPath);
		return restClient.get()
				.uri(projectUri)
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw gitlabFailure(response);
				})
				.body(GitLabProjectResponse.class);
	}

	private <T> T get(String path, Class<T> type) {
		requireToken();
		return restClient.get()
				.uri(path)
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw gitlabFailure(response);
				})
				.body(type);
	}

	private void requireToken() {
		if (!configured) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GitLab token is not configured");
		}
	}

	private ResponseStatusException gitlabFailure(ClientHttpResponse response) throws IOException {
		HttpStatusCode status = response.getStatusCode();
		HttpStatus httpStatus = HttpStatus.resolve(status.value());
		if (httpStatus == null) {
			httpStatus = HttpStatus.BAD_GATEWAY;
		}
		String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8).trim();
		if (body.length() > 300) {
			body = body.substring(0, 300);
		}
		String message = body.isBlank()
				? "GitLab returned " + status.value()
				: "GitLab returned " + status.value() + ": " + body;
		return new ResponseStatusException(httpStatus, message);
	}

}
