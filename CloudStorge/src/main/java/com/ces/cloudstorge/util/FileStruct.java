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

    public FileStruct(int fileId, int folderId, int parentFolderId, String name, String mimeType,
                      String share, int size, String last_modified) {
        this.fileId = fileId;
        this.folderId = folderId;
        this.parentFolderId = parentFolderId;
        this.name = name;
        this.mimeType = mimeType;
        this.share = share;
        this.size = size;
        this.last_modified = last_modified;
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
}
