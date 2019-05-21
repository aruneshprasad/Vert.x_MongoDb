package io.vertx.models;

import io.vertx.core.json.JsonObject;

public class LoginDetails {

    String studentId;
    String password;

    public LoginDetails() {
    }

    public LoginDetails(String studentId, String password) {
        this.studentId = studentId;
        this.password = password;
    }

    public LoginDetails(JsonObject jsonObject) {
        this.studentId = jsonObject.getString("studentId");
        this.password = jsonObject.getString("password");
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
