package org.alfresco.services.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Pagination {
    @JsonProperty("totalItems")
    private int totalItems;

    private int offset;
    private int limit;

    @JsonProperty("hasMore")
    private boolean hasMore;

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
