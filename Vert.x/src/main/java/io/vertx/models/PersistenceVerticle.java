package io.vertx.models;

import io.vertx.MessagingErrorCodes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class PersistenceVerticle extends AbstractVerticle{

  public static final String MESSAGE_ADDRESS = "students.persistence";

  // for DB access
  private MongoClient mongoClient;

  @Override
  public void start(Future<Void> startFuture) {
    // Configure the MongoClient inline.  This should be externalized into a config file
    mongoClient = MongoClient.createShared(vertx, new JsonObject().put("db_name", config().getString("db_name", "StudentDb")).put("connection_string", config().getString("connection_string", "mongodb://localhost:27017")));

    EventBus eventBus = vertx.eventBus();
    MessageConsumer<JsonObject> consumer = eventBus.consumer(MESSAGE_ADDRESS);

    consumer.handler(message -> {

      String action = message.body().getString("action");

      switch (action) {
        case "register.student": registerStudent(message);
          break;
        case "lookUpById.student": getStudentById(message);
          break;
        case "deleteById.student": deleteStudentById(message);
          break;
        case "update.student": updateStudent(message);
          break;
        default:
          message.fail(MessagingErrorCodes.UNKNOWN_ACTION.ordinal(), "Unkown action: " + message.body());
      }
    });
    startFuture.complete();
  }

  private void registerStudent(Message<JsonObject> message) {

    final Student studentToRegister = new Student(message.body().getJsonObject("student"));
    studentToRegister.set_id(studentToRegister.getStudentId());

    mongoClient.insert("student", studentToRegister.toMongoJson(),ar -> {
      if(ar.succeeded()){
        message.reply(new JsonObject().put("registeredStudent", studentToRegister.toMongoJson()));
      }else{
        message.fail(MessagingErrorCodes.INSERT_FAILURE.ordinal(), MessagingErrorCodes.INSERT_FAILURE.message + ar.cause().getMessage());
      }
    });
  }

  private void getStudentById(Message<JsonObject> message) {
    JsonObject query = new JsonObject().put("studentId", message.body().getString("studentId"));

    mongoClient.find("student", query, res -> {
      if (res.succeeded()) {
        if(res.result().size() == 0){
          message.fail(MessagingErrorCodes.NOT_FOUND.ordinal(), MessagingErrorCodes.NOT_FOUND.message + "Student not found");
        }else{
          message.reply(new JsonObject().put("studentById", res.result().get(0)));
        }
      } else {
        message.fail(MessagingErrorCodes.NOT_FOUND.ordinal(), MessagingErrorCodes.NOT_FOUND.message + res.cause());
      }
    });
  }

  private Future<Student> getStudentById(String studentId) {
    Future<Student> retVal = Future.future();
    JsonObject query = new JsonObject().put("studentId", studentId);
    mongoClient.find("student", query, ar -> {
      if (ar.succeeded()) {
        if(ar.result().size() == 0){
          retVal.fail("Student with id " + studentId + " does not exist");
        }else{
          retVal.complete();
        }
      } else {
        retVal.fail("Student with id " + studentId + " does not exist");
      }
    });
    return retVal;
  }

  private void deleteStudentById(Message<JsonObject> message) {
    JsonObject query = new JsonObject().put(message.body().getString("field"), message.body().getString("studentId"));

    getStudentById(message.body().getString("studentId")).setHandler(ar -> {
      if(ar.succeeded()){
        mongoClient.removeDocument("student", query, res -> {
          if (res.succeeded()) {
            message.reply(new JsonObject().put("success.message", "Student is removed"));
          } else {
            message.fail(MessagingErrorCodes.DELETE_FAILURE.ordinal(), MessagingErrorCodes.DELETE_FAILURE.message + res.cause());
          }
        });
      }else {
        message.fail(MessagingErrorCodes.NOT_FOUND.ordinal(), MessagingErrorCodes.NOT_FOUND.message + ar.cause());
      }
    });
  }

  private void updateStudent(Message<JsonObject> message) {

    final Student studentToUpdate = new Student(message.body().getJsonObject("student"));
    studentToUpdate.set_id(studentToUpdate.getStudentId());

    JsonObject query = new JsonObject().put("studentId", message.body().getString("studentId"));
    JsonObject update = new JsonObject().put("$set", studentToUpdate.toMongoJson());

    getStudentById(message.body().getString("studentId")).setHandler(ar -> {
      if(ar.succeeded()){
        mongoClient.updateCollection("student", query, update, res -> {
          if (res.succeeded()) {
            message.reply(new JsonObject().put("updatedStudent", studentToUpdate.toMongoJson()));
          } else {
            message.fail(MessagingErrorCodes.UPDATE_FAILURE.ordinal(), MessagingErrorCodes.UPDATE_FAILURE.message + res.cause());
          }
        });
      }else {
        message.fail(MessagingErrorCodes.NOT_FOUND.ordinal(), MessagingErrorCodes.NOT_FOUND.message + ar.cause());
      }
    });
  }
}
