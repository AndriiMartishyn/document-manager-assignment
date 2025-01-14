package com.martishyn;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * For implement this task focus on clear code, and make this solution as simple readable as possible
 * Don't worry about performance, concurrency, etc
 * You can use in Memory collection for sore data
 * <p>
 * Please, don't change class name, and signature for methods save, search, findById
 * Implementations should be in a single class
 * This class could be auto tested
 */
public class DocumentManager {

    private final Map<Long, Document> documents = new HashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * Implementation of this method should upsert the document to your storage
     * And generate unique id if it does not exist, don't change [created] field
     *
     * @param document - document content and author data
     * @return saved document
     */
    public Document save(Document document) {
        Objects.requireNonNull(document, "Passed document cannot be null");
        if (document.getId() == null) {
            long newlyGeneratedId = idGenerator.getAndIncrement();
            document.setId(String.valueOf(newlyGeneratedId));
            documents.put(newlyGeneratedId, document);
        } else {
            long existingId = Long.parseLong(document.getId());
            documents.put(existingId, document);
            idGenerator.updateAndGet(current -> Math.max(current, existingId + 1));
        }
        return document;
    }

    /**
     * Implementation this method should find documents which match with request
     *
     * @param request - search request, each field could be null
     * @return list matched documents
     */
    public List<Document> search(SearchRequest request) {
        return documents.values().stream()
                .filter(documentMatchesSearchRequest(request))
                .toList();
    }

    public static Predicate<Document> documentMatchesSearchRequest(SearchRequest request) {
        return document -> {
            boolean documentMatchesRequest = false;

            if (collectionIsNotNullOrEmpty(request.authorIds)) {
                documentMatchesRequest |= request.getAuthorIds().contains(document.author.id);
            }
            if (collectionIsNotNullOrEmpty(request.titlePrefixes)) {
                documentMatchesRequest |= request.getTitlePrefixes().stream().anyMatch(document.getTitle()::startsWith);
            }
            if (collectionIsNotNullOrEmpty(request.containsContents)) {
                documentMatchesRequest |= request.getContainsContents().stream().anyMatch(document.getContent()::contains);
            }
            if (request.getCreatedFrom() != null && request.getCreatedTo() != null) {
                Instant documentCreateTimeStamp = document.getCreated();
                documentMatchesRequest |= documentCreateTimeStamp.isAfter(request.getCreatedFrom()) && documentCreateTimeStamp.isBefore(request.getCreatedTo());
            }
            return documentMatchesRequest;
        };
    }

    private static boolean collectionIsNotNullOrEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * Implementation this method should find document by id
     *
     * @param id - document id
     * @return optional document
     */
    public Optional<Document> findById(String id) {
        try {
            long searchId = Long.parseLong(id);
            return Optional.of(documents.get(searchId));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    @Data
    @Builder
    public static class SearchRequest {
        private List<String> titlePrefixes;
        private List<String> containsContents;
        private List<String> authorIds;
        private Instant createdFrom;
        private Instant createdTo;
    }

    @Data
    @Builder
    public static class Document {
        private String id;
        private String title;
        private String content;
        private Author author;
        private Instant created;
    }

    @Data
    @Builder
    public static class Author {
        private String id;
        private String name;
    }
}