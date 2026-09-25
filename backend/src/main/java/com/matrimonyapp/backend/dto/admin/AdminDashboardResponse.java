package com.matrimonyapp.backend.dto.admin;

public class AdminDashboardResponse {

    private int users;
    private int profiles;
    private int openReports;
    private int pendingVerification;

    public AdminDashboardResponse() {
    }

    public AdminDashboardResponse(int users, int profiles, int openReports, int pendingVerification) {
        this.users = users;
        this.profiles = profiles;
        this.openReports = openReports;
        this.pendingVerification = pendingVerification;
    }

    public int getUsers() {
        return users;
    }

    public void setUsers(int users) {
        this.users = users;
    }

    public int getProfiles() {
        return profiles;
    }

    public void setProfiles(int profiles) {
        this.profiles = profiles;
    }

    public int getOpenReports() {
        return openReports;
    }

    public void setOpenReports(int openReports) {
        this.openReports = openReports;
    }

    public int getPendingVerification() {
        return pendingVerification;
    }

    public void setPendingVerification(int pendingVerification) {
        this.pendingVerification = pendingVerification;
    }
}
