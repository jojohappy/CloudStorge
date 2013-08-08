package com.ces.cloudstorge.util;

/**
 * Created by MichaelDai on 13-8-1.
 */
public class FileStruct {
    private int fileId;
    private int folderId;
    private int parentFolderId;
    private String name;
    private String mimeType;
    private String share;
    private int size;
    private String last_modified;
    private String username;
    private String createTime;
    private String revisionInfo;
    private String description;
    private int originFolder;

    public FileStruct() {}

    public FileStruct(int fileId, int folderId, int parentFolderId, String name, String mimeType,
                      String share, int size, String last_modified, String username, String createTime,
                      String revisionInfo, String description, int originFolder) {
        this.fileId = fileId;
        this.folderId = folderId;
        this.parentFolderId = parentFolderId;
        this.name = name;
        this.mimeType = mimeType;
        this.share = share;
        this.size = size;
        this.last_modified = last_modified;
        this.username = username;
        this.createTime = createTime;
        this.revisionInfo = revisionInfo;
        this.description = description;
        this.originFolder = originFolder;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public int getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(int parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getShare() {
        return share;
    }

    public void setShare(String share) {
        this.share = share;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getLast_modified() {
        return last_modified;
    }

    public void setLast_modified(String last_modified) {
        this.last_modified = last_modified;
    }

    public int getOriginFolder() {
        return originFolder;
    }

    public void setOriginFolder(int originFolder) {
        this.originFolder = originFolder;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getRevisionInfo() {
        return revisionInfo;
    }

    public void setRevisionInfo(String revisionInfo) {
        this.revisionInfo = revisionInfo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
