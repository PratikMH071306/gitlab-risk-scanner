package com.example.gitlabscanner.controller;

import java.util.List;

import com.example.gitlabscanner.dto.GitLabProjectResponse;
import com.example.gitlabscanner.dto.GitLabUserResponse;
import com.example.gitlabscanner.service.GitLabService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gitlab")
public class GitLabController {

	private final GitLabService gitLabService;

	public GitLabController(GitLabService gitLabService) {
		this.gitLabService = gitLabService;
	}

	@GetMapping("/me")
	public GitLabUserResponse me() {
		return gitLabService.currentUser();
	}

	@GetMapping("/projects")
	public List<GitLabProjectResponse> projects() {
		return gitLabService.listProjects();
	}

}
