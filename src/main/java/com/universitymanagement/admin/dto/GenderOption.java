package com.universitymanagement.admin.dto;

public enum GenderOption {
    FEMALE("Female"), MALE("Male"), OTHER("Other");

    private String gender;
    GenderOption(String gender) {
        this.gender = gender;
    }
    public String getGender() {
        return gender;
    }
}