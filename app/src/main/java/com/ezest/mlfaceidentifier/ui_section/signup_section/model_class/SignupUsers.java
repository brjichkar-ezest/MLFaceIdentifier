package com.ezest.mlfaceidentifier.ui_section.signup_section.model_class;

public class SignupUsers {
    String firstname;
    String lastname;
    String emailAddress;
    String videoUrl;
    String mobile;
    String fileName;

    public SignupUsers() {
    }

    public SignupUsers(String firstname, String lastname, String emailAddress, String videoUrl,String mobile,String fileName) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.emailAddress = emailAddress;
        this.videoUrl = videoUrl;
        this.mobile = mobile;
        this.fileName = fileName;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
