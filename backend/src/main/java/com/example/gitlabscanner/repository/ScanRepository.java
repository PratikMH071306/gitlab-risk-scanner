package com.example.gitlabscanner.repository;

import java.util.List;

import com.example.gitlabscanner.entity.Scan;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanRepository extends JpaRepository<Scan, Long> {

	List<Scan> findAllByOrderByScannedAtDesc();

}
