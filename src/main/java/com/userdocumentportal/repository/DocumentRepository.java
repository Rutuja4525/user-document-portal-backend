package com.userdocumentportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.userdocumentportal.entity.Document;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("SELECT d FROM Document d WHERE d.user.id = :userId AND (d.originalDeleted = false OR d.processedDeleted = false)")
    List<Document> findActiveDocumentsByUserId(@Param("userId") Long userId);

    @Query("SELECT d FROM Document d WHERE d.originalDeleted = false OR d.processedDeleted = false")
    List<Document> findActiveDocuments();
}
