package au.edu.sydney.soft3202.task3;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class DatabaseHelper {
  private static final String DB_NAME = "fruitbasket.db";
  private Connection connection;

  private void connect() throws SQLException {
    connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
  }

  private void ensureUsersTable() throws SQLException {
    String sql =
      "CREATE TABLE IF NOT EXISTS users (user TEXT PRIMARY KEY NOT NULL)";
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private void deleteUsersTable() throws SQLException {
    String sql = "DROP TABLE IF EXISTS users";
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  public void addUser(String name) throws SQLException {
    String sql = "INSERT INTO users (user) VALUES (?)";
    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setString(1, name);
      preparedStatement.executeUpdate();
    }
  }

  public void deleteUser(String name) throws SQLException {
    String sql = "DELETE FROM users WHERE user = ?";
    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setString(1, name);
      preparedStatement.executeUpdate();
    }
  }


  public List<String> getUsers() throws SQLException {
    String sql = "SELECT user FROM users";

    try(PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      ResultSet resultSet = preparedStatement.executeQuery();
      List<String> users = new ArrayList<String>();

      while (resultSet.next()) {
        String user = resultSet.getString("user");
        users.add(user);
      }
      return users;
    }

  }

  public String getUser(String name) throws SQLException {
    String sql = "SELECT user FROM users WHERE user = ?";

    try(PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setString(1, name);
      ResultSet resultSet = preparedStatement.executeQuery();

      while (resultSet.next()) {
        String user = resultSet.getString("user");
        return user;
      }
    }
    return null;
  }

  private void ensureShoppingCartTable() throws SQLException {
    String sql =
            "CREATE TABLE IF NOT EXISTS shoppingcart (user TEXT NOT NULL, item TEXT NOT NULL," +
                    " count INTEGER NOT NULL, cost REAL NOT NULL)";
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  public void initaliseNewUserShoppingCart(String newUser) throws SQLException {

    String sql =
            """
              INSERT INTO shoppingcart(user, item, count, cost) VALUES
              (?, 'apple', 0, 2.5),
              (?, 'orange', 0, 1.25),
              (?, 'pear', 0, 3.00),
              (?, 'banana', 0, 4.95)
            """;

    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setString(1, newUser);
      preparedStatement.setString(2, newUser);
      preparedStatement.setString(3, newUser);
      preparedStatement.setString(4, newUser);
      preparedStatement.executeUpdate();
    }

  }

  public void deleteUserCart(String name) throws SQLException {
    String sql = "DELETE FROM shoppingcart WHERE user = ?";
    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setString(1, name);
      preparedStatement.executeUpdate();
    }
  }

  public void deleteShoppingcartItem(ArrayList<String> itemsToDelete, String name) throws SQLException {
    String sql = "DELETE FROM shoppingcart WHERE user = ? AND item = ?";

    for (String item : itemsToDelete) {
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setString(1, name);
        preparedStatement.setString(2, item);
        preparedStatement.executeUpdate();
      }
    }

  }

  public void setNewUserShoppingcart(Map<String, Integer> items, Map<String, Double> values, String name) throws SQLException {
    String sql =
            """
              INSERT INTO shoppingcart(user, item, count, cost) VALUES
              (?, ?, ?, ?)
            """;

    for (String item : items.keySet()) {
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setString(1, name);
        preparedStatement.setString(2, item);
        preparedStatement.setInt(3, items.get(item));
        preparedStatement.setDouble(4, values.get(item));
        preparedStatement.executeUpdate();
      }
    }


  }


  //sets new item qty in shoppingcart DB (newItemQty = map of input from form submission /cart page)
  //(name = user currently logged into /cart page)
  public void updateUserShoppingCartQty(String name, Map<String, String> newItemQty) throws SQLException {
    String sql = "UPDATE shoppingcart SET count = ? WHERE user = ? AND item = ?";

    for (Map.Entry<String, String> entry : newItemQty.entrySet()) {
      //update Users ShoppingBasket items list
      String itemId = entry.getKey();
      Integer itemQuantity = Integer.parseInt(entry.getValue());

      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)){
        preparedStatement.setInt(1, itemQuantity);
        preparedStatement.setString(2, name);
        preparedStatement.setString(3, itemId);
        preparedStatement.executeUpdate();
      }
    }
  }

  public void addNewShoppingCartItem(String name, String item, Double cost) throws SQLException {
    String sql =
            """
              INSERT INTO shoppingcart(user, item, count, cost) VALUES
              (?, ?, 0, ?)
            """;

    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setString(1, name);
      preparedStatement.setString(2, item);
      preparedStatement.setDouble(3, cost);
      preparedStatement.executeUpdate();
    }

    //NOT SURE WHY ONLY THIS NEEDS CATCH BLOCK ^^^ AND OTHERS DONT, COULD BE SOMTING WRONG WITH IT??
  }



  //returns a list of specified user's (name) cart from the db (shoppingcart table)
  public HashMap<String, Integer> getUserShoppingCart(String name) throws SQLException {
    String sql = "SELECT user, item, count, cost FROM shoppingcart WHERE user = ?";

    try(PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setString(1, name);
      ResultSet resultSet = preparedStatement.executeQuery();

      HashMap<String, Integer> userShoppingCart = new HashMap<>();

      while (resultSet.next()) {
        String item = resultSet.getString("item");
        Integer count = resultSet.getInt("count");
        userShoppingCart.put(item, count);
      }
      return userShoppingCart;
    }

  }

  public Map<String, Double> getUserShoppingCartCosts(String name) throws SQLException {
    String sql = "SELECT user, item, count, cost FROM shoppingcart WHERE user = ?";

    try(PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setString(1, name);
      ResultSet resultSet = preparedStatement.executeQuery();

      HashMap<String, Double> userShoppingCartCosts = new HashMap<>();

      while (resultSet.next()) {
        String item = resultSet.getString("item");
        Double count = resultSet.getDouble("cost");
        userShoppingCartCosts.put(item, count);
      }
      return userShoppingCartCosts;
    }
  }

  public DatabaseHelper() throws SQLException {
    connect();
    ensureUsersTable();
    ensureShoppingCartTable();
  }

}

