package io.vertx;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.models.PersistenceVerticle;
import io.vertx.models.Student;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class HttpVerticle extends AbstractVerticle{

    // Authentication provider for the api
  // Use JWT authentication for admin(1 in no.) so that he can access student database using exposed endpoints.
  //That makes more sense than students accessing other students.
    private JWTAuth jwtAuth;

  @Override
  public void start(Future<Void> startFuture) {

      // Configure authentication with JWT
      jwtAuth = JWTAuth.create(vertx, new JsonObject().put("keyStore", new JsonObject()
              .put("type", "jceks")
              .put("path", "keystore.jceks")
              .put("password", "secret")));

    Router baseRouter = Router.router(vertx);
    Router apiRouter = Router.router(vertx);

    baseRouter.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/plain").end("Hello Vert.x!");
    });

    apiRouter.route("/*").handler(BodyHandler.create());
    apiRouter.post("/registerStudent").handler(this::registerStudent);
    apiRouter.post("/loginStudent").handler(this::loginStudent);
    apiRouter.get("/getStudentById/:studentId").handler(JWTAuthHandler.create(jwtAuth)).handler(this::getStudentById);
    apiRouter.delete("/deleteStudentById/:studentId").handler(this::deleteStudentById);
    apiRouter.put("/updateStudent/:studentId").handler(this::updateStudent);

    baseRouter.mountSubRouter("/api", apiRouter);

    vertx.createHttpServer()
      .requestHandler(baseRouter::accept)
      .listen(8080, result -> {
        if (result.succeeded()) {
          startFuture.complete();
        } else {
          startFuture.fail(result.cause());
        }
      });
  }

  private void registerStudent(RoutingContext routingContext) {

    JsonObject message = new JsonObject()
      .put("action", "register.student")
      .put("student", routingContext.getBodyAsJson());

    vertx.eventBus().send(PersistenceVerticle.MESSAGE_ADDRESS, message, ar -> {

      if (ar.succeeded()) {
        JsonObject studentJson = ((JsonObject) ar.result().body()).getJsonObject("registeredStudent");
        final Student registeredStudent = new Student(studentJson);
        routingContext.response()
          .setStatusCode(201)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(registeredStudent));
      }else{
        routingContext.response()
          .setStatusCode(500)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(ar.cause().getMessage()));
      }
    });
  }

  private void loginStudent(RoutingContext routingContext) {

    JsonObject message = new JsonObject()
            .put("action", "login.student")
            .put("login", routingContext.getBodyAsJson());

    vertx.eventBus().send(PersistenceVerticle.MESSAGE_ADDRESS, message, ar -> {

      if (ar.succeeded()) {
          JsonObject studentJson = ((JsonObject) ar.result().body()).getJsonObject("loggedStudent");
          final Student loggedStudent = new Student(studentJson);
          String token = jwtAuth.generateToken(new JsonObject().put("studentId", loggedStudent.getStudentId()).put("password", loggedStudent.getPassword()), new JWTOptions().setIgnoreExpiration(true));
          System.out.println(token);
        routingContext.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(token));
      }else{
        routingContext.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(ar.cause().getMessage()));
      }
    });
  }

  private void getStudentById(RoutingContext routingContext) {

    String headerAuth = routingContext.request().getHeader("Authorization");
    System.out.println("headerAuth: " + headerAuth);

    String[] values = headerAuth.split(" ");
    System.out.println("values[1]: " + values[1]);

    final String studentId = routingContext.request().getParam("studentId");

    if (studentId == null || studentId.isEmpty()) {
      routingContext.response().setStatusCode(400).end();
    } else {

      jwtAuth.authenticate(new JsonObject().put("jwt", values[1]), res -> {
        if (res.succeeded()){

          io.vertx.ext.auth.User theUser = res.result();
          JsonObject principal = theUser.principal();
          System.out.println("theUser: " + theUser.principal().encodePrettily());

          JsonObject message = new JsonObject()
                  .put("action", "lookUpById.student")
                  .put("studentId", routingContext.request().getParam("studentId"));

          vertx.eventBus().send(PersistenceVerticle.MESSAGE_ADDRESS, message, ar -> {

            if (ar.succeeded()) {
              JsonObject studentJson = ((JsonObject) ar.result().body()).getJsonObject("studentById");
              final Student returnedStudent = new Student(studentJson);
              routingContext.response()
                      .setStatusCode(200)
                      .putHeader("Content-Type", "application/json; charset=utf-8")
                      //.putHeader("Content-Length", String.valueOf(userResult.toString().length()))
                      .end(Json.encodePrettily(returnedStudent));
            } else {
              routingContext.response().setStatusCode(422)
                      .putHeader("content-type", "application/json; charset=utf-8")
                      //.putHeader("Content-Length", String.valueOf(loginError.toString().length()))
                      .end(Json.encodePrettily(ar.cause().getMessage()));
            }
          });
        }else{
          routingContext.response().setStatusCode(422)
                  .putHeader("content-type", "application/json; charset=utf-8")
                  //.putHeader("Content-Length", String.valueOf(loginError.toString().length()))
                  .end(Json.encodePrettily("Authentication failed"));
        }

      });

    }
  }

  private void deleteStudentById(RoutingContext routingContext){
    final String studentId = routingContext.request().getParam("studentId");

    if (studentId == null || studentId.isEmpty()) {
      routingContext.response().setStatusCode(400).end();
    } else {
      JsonObject message = new JsonObject()
              .put("action", "deleteById.student")
              .put("field", "studentId")
              .put("studentId", routingContext.request().getParam("studentId"));

      vertx.eventBus().send(PersistenceVerticle.MESSAGE_ADDRESS, message, ar -> {

        if (ar.succeeded()) {
          String successMessage = ((JsonObject) ar.result().body()).getString("success.message");
          routingContext.response()
                  .setStatusCode(200)
                  .putHeader("Content-Type", "application/json; charset=utf-8")
                  //.putHeader("Content-Length", String.valueOf(userResult.toString().length()))
                  .end(Json.encodePrettily(successMessage));
        } else {
          routingContext.response().setStatusCode(422)
                  .putHeader("content-type", "application/json; charset=utf-8")
                  //.putHeader("Content-Length", String.valueOf(loginError.toString().length()))
                  .end(Json.encodePrettily(ar.cause().getMessage()));
        }
      });
    }
  }

  private void updateStudent(RoutingContext routingContext) {

    final String studentId = routingContext.request().getParam("studentId");

    JsonObject message = new JsonObject()
            .put("action", "update.student")
            .put("studentId", studentId)
            .put("student", routingContext.getBodyAsJson());

    vertx.eventBus().send(PersistenceVerticle.MESSAGE_ADDRESS, message, ar -> {

      if (ar.succeeded()) {
        JsonObject studentJson = ((JsonObject) ar.result().body()).getJsonObject("updatedStudent");
        final Student updatedStudent = new Student(studentJson);
        routingContext.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(updatedStudent));
      }else{
        routingContext.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(ar.cause().getMessage()));
      }
    });
  }

}

