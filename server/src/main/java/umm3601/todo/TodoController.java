package umm3601.todo;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.mongojack.JacksonMongoCollection;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import umm3601.Controller;

public class TodoController implements Controller {
  private static final String API_TODOS = "/api/todos";
  private static final String API_TODO_BY_ID = "/api/todos/{id}";
  static final String OWNER_KEY = "owner";
  static final String CATEGORY_KEY = "category";
  static final String BODY_KEY = "body";
  static final String STATUS_KEY = "status";
  static final String SORT_ORDER_KEY = "sortorder";


  private final JacksonMongoCollection<Todo> todoCollection;

/**
   * Construct a controller for todos.
   *
   * @param database the database containing user data
   */
  public TodoController(MongoDatabase database) {
    todoCollection = JacksonMongoCollection.builder().build(
        database,
        "todos",
        Todo.class,
        UuidRepresentation.STANDARD);
  }

/**
   * Set the JSON body of the response to be the single todo
   * specified by the `id` parameter in the request
   *
   * @param ctx a Javalin HTTP context
   */
  public void getTodo(Context ctx) {
    String id = ctx.pathParam("id");
    Todo todo;

    try {
      todo = todoCollection.find(eq("_id", new ObjectId(id))).first();
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("The requested todo id wasn't a legal Mongo Object ID.");
    }
    if (todo == null) {
      throw new NotFoundResponse("The requested todo was not found");
    } else {
      ctx.json(todo);
      ctx.status(HttpStatus.OK);
    }
  }


/**
   * Set the JSON body of the response to be a list of all the users returned from
   * the database
   * that match any requested filters and ordering
   *
   * @param ctx a Javalin HTTP context
   */

 public void getTodos(Context ctx) {
   Bson combinedFilter = constructFilter(ctx);
   Bson sortingOrder = constructSortingOrder(ctx);


   ArrayList<Todo> matchingTodos = todoCollection
       .find(combinedFilter)
       .sort(sortingOrder)
       .into(new ArrayList<>());

       ctx.json(matchingTodos);

       ctx.status(HttpStatus.OK);
     }


     private Bson constructFilter(Context ctx) {
      List<Bson> filters = new ArrayList<>(); // start with an empty list of filters

      if (ctx.queryParamMap().containsKey(OWNER_KEY)) {
        Pattern pattern = Pattern.compile(Pattern.quote(ctx.queryParam(OWNER_KEY)), Pattern.CASE_INSENSITIVE);
        filters.add(regex(OWNER_KEY, pattern));
      }
      if (ctx.queryParamMap().containsKey(CATEGORY_KEY)) {
          Pattern pattern = Pattern.compile(Pattern.quote(ctx.queryParam(CATEGORY_KEY)), Pattern.CASE_INSENSITIVE);
          filters.add(regex(CATEGORY_KEY, pattern));
      }
      if (ctx.queryParamMap().containsKey(BODY_KEY)) {
            Pattern pattern = Pattern.compile(Pattern.quote(ctx.queryParam(BODY_KEY)), Pattern.CASE_INSENSITIVE);
            filters.add(regex(BODY_KEY, pattern));
      }
      if (ctx.queryParamMap().containsKey(STATUS_KEY)) {
              Pattern pattern = Pattern.compile(Pattern.quote(ctx.queryParam(STATUS_KEY)), Pattern.CASE_INSENSITIVE);
              filters.add(regex(STATUS_KEY, pattern));
      }

      // Combine the list of filters into a single filtering document.
      Bson combinedFilter = filters.isEmpty() ? new Document() : and(filters);

      return combinedFilter;
    }

    private Bson constructSortingOrder(Context ctx) {
      // Sort the results. Use the `sortby` query param (default "name")
      // as the field to sort by, and the query param `sortorder` (default
      // "asc") to specify the sort order.
      String sortBy = Objects.requireNonNullElse(ctx.queryParam("sortby"), "owner");
      String sortOrder = Objects.requireNonNullElse(ctx.queryParam("sortorder"), "asc");
      Bson sortingOrder = sortOrder.equals("desc") ? Sorts.descending(sortBy) : Sorts.ascending(sortBy);
      return sortingOrder;
    }

    public void addNewTodo(Context ctx) {

      String body = ctx.body();
      Todo newTodo = ctx.bodyValidator(Todo.class)
          .check(todo -> todo.owner != null && todo.owner.length() > 0,
              "Todo must have a non-empty todo owner; body was " + body)
          .check(todo -> todo.body != null && todo.category.length() > 0,
              "Todo must have a non-empty body; body was " + body)
          .check(todo -> todo.category != null && todo.category.length() > 0,
              "Todo must have a non-empty category name; body was " + body)
          .get();

      todoCollection.insertOne(newTodo);

      ctx.json(Map.of("id", newTodo._id));

      ctx.status(HttpStatus.CREATED);
    }


     /**
   * Delete the todo specified by the `id` parameter in the request.
   *
   * @param ctx a Javalin HTTP context
   */
    public void deleteTodo(Context ctx) {
      String id = ctx.pathParam("id");
      DeleteResult deleteResult = todoCollection.deleteOne(eq("_id", new ObjectId(id)));
      // We should have deleted 1 or 0 todos, depending on whether `id` is a valid
      // todo ID.
      if (deleteResult.getDeletedCount() != 1) {
        ctx.status(HttpStatus.NOT_FOUND);
        throw new NotFoundResponse(
            "Was unable to delete ID "
                + id
                + "; perhaps illegal ID or an ID for an item not in the system?");
      }
      ctx.status(HttpStatus.OK);
    }

 public void addRoutes(Javalin server) {
   // Get the specified user
   server.get(API_TODO_BY_ID, this::getTodo);

   // List users, filtered using query parameters
   server.get(API_TODOS, this::getTodos);

   // Delete the specified user
   server.delete(API_TODO_BY_ID, this::deleteTodo);

   // Add new user with the user info being in the JSON body
   // of the HTTP request
   server.post(API_TODOS, this::addNewTodo);
 }

}
