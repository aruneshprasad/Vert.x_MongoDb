package io.vertx.models;

import io.vertx.core.json.JsonObject;

public class Student {

  String _id;
  String studentId;
  String studentName;
  String email;
  String password;

  public Student() {
  }

  public Student(String _id, String studentId, String studentName, String email, String password) {
    this._id = _id;
    this.studentId = studentId;
    this.studentName = studentName;
    this.email = email;
    this.password = password;
  }

  public Student(JsonObject jsonObject) {
    if (jsonObject.containsKey("_id")) this._id = jsonObject.getString("_id");
    this.studentId = jsonObject.getString("studentId");
    this.studentName = jsonObject.getString("studentName");
    this.email = jsonObject.getString("email");
    this.password = jsonObject.getString("password");
  }

  public JsonObject toMongoJson() {
    JsonObject retVal = new JsonObject();
    if (this._id != null) {
      retVal.put("_id", this._id);
    }
    retVal.put("studentId", studentId)
            .put("studentName", studentName)
            .put("email", email)
            .put("password", password);
    return retVal;
  }

  public String get_id() {
    return _id;
  }

  public void set_id(String _id) {
    this._id = _id;
  }

  public String getStudentId() {
    return studentId;
  }

  public void setStudentId(String studentId) {
    this.studentId = studentId;
  }

  public String getStudentName() {
    return studentName;
  }

  public void setStudentName(String studentName) {
    this.studentName = studentName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
