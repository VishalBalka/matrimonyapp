package com.matrimonyapp.backend.dto.notification;

public class NotificationResponse {

    private String id;
    private String type;
    private String title;
    private String body;
    private String readAt;
    private String createdAt;

    public NotificationResponse() {
    }

    public NotificationResponse(String id, String type, String title, String body, String readAt, String createdAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.body = body;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getReadAt() {
        return readAt;
    }

    public void setReadAt(String readAt) {
        this.readAt = readAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
