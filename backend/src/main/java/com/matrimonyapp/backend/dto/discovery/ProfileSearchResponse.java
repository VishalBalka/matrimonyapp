package com.matrimonyapp.backend.dto.discovery;

import java.util.Collections;
import java.util.List;

public class ProfileSearchResponse {

    private List<ProfileSearchItemDto> items = Collections.emptyList();
    private int page;
    private int pageSize;
    private long totalItems;
    private int totalPages;

    public ProfileSearchResponse() {
    }

    public ProfileSearchResponse(List<ProfileSearchItemDto> items, int page, int pageSize, long totalItems, int totalPages) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }

    public List<ProfileSearchItemDto> getItems() {
        return items;
    }

    public void setItems(List<ProfileSearchItemDto> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
