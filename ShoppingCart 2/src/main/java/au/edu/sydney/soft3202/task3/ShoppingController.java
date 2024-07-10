package au.edu.sydney.soft3202.task3;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HexFormat;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import java.sql.SQLException;

@Controller
public class ShoppingController {
    private final SecureRandom randomNumberGenerator = new SecureRandom();
    private final HexFormat hexFormatter = HexFormat.of();

    Map<String, String> sessions = new HashMap<>();
    Map<String, ShoppingBasket> userBaskets = new HashMap<>();

    List<String> users = null;
    DatabaseHelper dbHelper = null;
    Map<String, Integer> initialItems = new HashMap<>();


    public ShoppingController() {
        initialItems.put("apple", 0);
        initialItems.put("orange", 0);
        initialItems.put("pear", 0);
        initialItems.put("banana", 0);

    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam(value = "user", defaultValue = "") String user, Model model) {
        try {
            dbHelper = new DatabaseHelper();
            //Pre populates single user: "A"
//            if (dbHelper.getUser("B") == null) {
//                dbHelper.addUser("B");
//            }

            //checks users table if "Admin" exists through dbHelper, if it doesn't exist it is added to users table
            if (dbHelper.getUser("Admin") == null) {
                dbHelper.addUser("Admin");
            }

            //is this nesscacary?:
            if (!user.equals("Admin")) {
                user = dbHelper.getUser(user);
            }

        } catch (SQLException sqle) {
            System.out.println(sqle);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Unable to connect: " + sqle.getMessage()+ ".\n");
        }

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid user.\n");
        }

        byte[] sessionTokenBytes = new byte[16];
        randomNumberGenerator.nextBytes(sessionTokenBytes);
        String sessionToken = hexFormatter.formatHex(sessionTokenBytes);

        sessions.put(sessionToken, user);

        String setCookieHeaderValue = String.format("session=%s; Path=/; HttpOnly; SameSite=Strict;", sessionToken);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Set-Cookie", setCookieHeaderValue);

        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).location(URI.create("/cart")).build();
    }

    @GetMapping("/newuser")
    public String newUser(@CookieValue(value = "session", defaultValue = "") String sessionToken, Model model) {
        if (!sessions.containsKey(sessionToken)) {
            return "unauthorized";
        }
        String user = sessions.get(sessionToken);

        if (!user.equals("Admin")) {
            return "error";
        }

        return "newuser";
    }

    @PostMapping("/newuser")
    public String addNewUser(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                             @RequestParam(value = "userName", defaultValue = "") String userName, Model model) {
        if (!sessions.containsKey(sessionToken)) {
            return "unauthorized";
        }

        String usr = sessions.get(sessionToken);

        if (!usr.equals("Admin")) {
            return "error";
        }

        List<String> dbUsers;

        try {
            dbUsers = dbHelper.getUsers();
            for (String user : dbUsers) {
                if (userName.equals(user)) {
                    return "error";
                }
            }
            if (!userName.matches("[a-zA-Z0-9]+")) {
                return "error";
            }
            //MIGHT NEED TO ADD A CHECK TO MAKE SURE INPUTED NAME IS ALPHANUMERIC AS PER SPECS (NEED TO CONFIRM ON ED)
            dbHelper.addUser(userName);
            dbHelper.initaliseNewUserShoppingCart(userName);


            //think i need to add this to /cartpage
            ShoppingBasket newUserBasket = new ShoppingBasket();
            users.add(userName);
            userBaskets.put(userName, newUserBasket);

        } catch (SQLException se) {
            return "error";
        }

        return "redirect:/cart";
    }

    @PostMapping("/updateusers")
    public String updateUsers(@CookieValue(value = "session", defaultValue = "") String sessionToken, Model model,
                                              @RequestParam(value = "userToDelete", defaultValue = "") ArrayList<String> userToDelete) {

//        model.addAttribute("users", users);
//        System.out.println(model.getAttribute(users));

        String user = sessions.get(sessionToken);

        if (!user.equals("Admin")) {
            return "error";
        }

        try {
            for (String usrToDelete : userToDelete){
                dbHelper.deleteUser(usrToDelete);
                dbHelper.deleteUserCart(usrToDelete);

                sessions.remove(usrToDelete);
                users.remove(usrToDelete);
                userBaskets.remove(usrToDelete);
            }

        } catch (SQLException se) {
            return "error";
        }
//        return ResponseEntity.status(HttpStatus.OK).body("[" + userToDelete + "]");

        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String cart(@CookieValue(value = "session", defaultValue = "") String sessionToken, Model model) {
        if (!sessions.containsKey(sessionToken)) {
            return "unauthorized";
        }

        String user = sessions.get(sessionToken);

        if (user.equals("Admin")) {
            try {
                users = dbHelper.getUsers();
                users.remove("Admin");
            } catch (SQLException se) {
                return "error";
            }
            model.addAttribute("users", users);
            return "users";

        } else {

            try {
                //create new shoppingbasket for user if they dont have one associated and stores in userBaskets Map
                if (!userBaskets.containsKey(user)) {
                    ShoppingBasket newBasket = new ShoppingBasket();
                    userBaskets.put(user, newBasket);
                    newBasket.items.putAll(dbHelper.getUserShoppingCart(user));
                    newBasket.values.putAll(dbHelper.getUserShoppingCartCosts(user));
                }

                HashMap<String, Integer> cart = dbHelper.getUserShoppingCart(user);
                ShoppingBasket userBasket = userBaskets.get(user);

                model.addAttribute("cartItems", cart);
                model.addAttribute("total", userBasket.getValue());

                return "cart";

            } catch (SQLException se) {
                return "error";
            }
        }
    }

    @PostMapping("/updatecart")
    public String updateCart(@CookieValue(value = "session", defaultValue = "") String sessionToken, Model model,
                                              @RequestParam Map<String, String> allParams) {

        if (!sessions.containsKey(sessionToken)) {
            return "unauthorized";
        }

        String user = sessions.get(sessionToken);

        if (user.equals("Admin")) {
            return "error";
        }

        ShoppingBasket userCart = userBaskets.get(user);

        //check for negative values , catches error if input is not integer and returns error page
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            try {
                if (entry.getValue() == null) {
                    //checks if user has sumbited empty form
                    return "invalidInput";
                }
                Integer itemQuantity = Integer.parseInt(entry.getValue());
                if (itemQuantity < 0) {
                    //checks that input is not < 0 or MAXVALUE
                    return "invalidInput";
                }
            } catch (Exception e) {
                return "error";
            }
        }

        //update cart
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            //update Users ShoppingBasket items list
            String itemId = entry.getKey();
            Integer itemQuantity = Integer.parseInt(entry.getValue());
            userCart.items.replace(itemId, itemQuantity);
            //update shoppingcartDB
        }
        try {
            dbHelper.updateUserShoppingCartQty(user, allParams);
        } catch (SQLException se) {
            return "error";
        }

        return "redirect:/cart";

    }

    @GetMapping("/newname")
    public String newName(@CookieValue(value = "session", defaultValue = "") String sessionToken) {
        if (!sessions.containsKey(sessionToken)) {
            return "unauthorized";
        }

        String user = sessions.get(sessionToken);

        if (user.equals("Admin")) {
            return "error";
        }

        return "newname";
    }

    @PostMapping("/newname")
    public String addNewName(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                             @RequestParam(value = "itemName", defaultValue = "") String itemName,
                             @RequestParam(value = "itemCost", defaultValue = "") Double itemCost) {

        String user = sessions.get(sessionToken);
        ShoppingBasket userCart = userBaskets.get(user);

        if (user.equals("Admin")) {
            return "error";
        }

        //Prevents Empty form submission
        if (itemName == null || itemCost == null) {
            return "invalidInput";
        }
        //prevents user from adding dup name
        if (userCart.values.containsKey(itemName)) {
            return "invalidInput";
        }
        //prevents user adding cost < 0 or > MAXVALUE
        if (itemCost < 0 || itemCost > Double.MAX_VALUE) {
            return "invalidInput";
        }

        //adds new item to users cart (DB and ShoppingBasket), catches errors and returns error page if error occurs
        try {
            dbHelper.addNewShoppingCartItem(user, itemName, itemCost);
            userCart.items.put(itemName, 0);
            userCart.values.put(itemName, itemCost);
        } catch (SQLException se) {
            return "error";
        }


        return "redirect:/cart";
    }

    @GetMapping("/delname")
    public String delNamePage(Model model, @CookieValue(value = "session", defaultValue = "") String sessionToken) {
        if (!sessions.containsKey(sessionToken)) {
            return "unauthorized";
        }

        String user = sessions.get(sessionToken);
//        ShoppingBasket userBasket = userBaskets.get(user);

        if (user.equals("Admin")) {
            return "error";
        }

        Map<String, Double> dbValues;
        try {
            dbValues = dbHelper.getUserShoppingCartCosts(user);
        } catch (SQLException se) {
            return "error";
        }

        model.addAttribute("items", dbValues);

        return "delname";
    }


    @PostMapping("/delname")
    public String delName(Model model, @CookieValue(value = "session", defaultValue = "") String sessionToken,
                                          @RequestParam(value = "itemToDelete", defaultValue = "") ArrayList<String> itemToDelete) {


        String user = sessions.get(sessionToken);
        ShoppingBasket userBasket = userBaskets.get(user);

        if (user.equals("Admin")) {
            return "error";
        }

        ArrayList<String> toDelete = new ArrayList<>();
        Map<String, Integer> currentDbItems;

        try {
            currentDbItems = dbHelper.getUserShoppingCart(user);
            for (String item : currentDbItems.keySet()) {
                if (!itemToDelete.contains(item)) {
                    //adds items that are to be removed to toDelete list for later use
                    toDelete.add(item);
                }
            }
            dbHelper.deleteShoppingcartItem(toDelete, user);
            for (String item : toDelete) {
                if (userBasket.values.containsKey(item)) {
                    userBasket.values.remove(item);
                }
                if (userBasket.items.containsKey(item)) {
                    userBasket.items.remove(item);
                }
            }
        } catch (SQLException se) {
            return "error";
        }

        return "redirect:/cart";
    }

    @GetMapping("/updatename")
    public String showUpdateName(@CookieValue(value = "session", defaultValue = "") String sessionToken, Model model) {
        if (!sessions.containsKey(sessionToken)) {
            return "unauthorized";
        }

        String user = sessions.get(sessionToken);
        ShoppingBasket userBasket = userBaskets.get(user);

        if (user.equals("Admin")) {
            return "error";
        }

        Map<String, Double> dbItems;
        try {
            dbItems = dbHelper.getUserShoppingCartCosts(user);
        } catch (SQLException se) {
            return "error";
        }

        model.addAttribute("items", dbItems);
        return "updatename";
    }

    @PostMapping("/updatename")
    public String updateName(@CookieValue(value = "session", defaultValue = "") String sessionToken, Model model,
                                             @RequestParam Map<String, String> formParams) {

        if (!sessions.containsKey(sessionToken)) {
            return "unauthorized";
        }

        String user = sessions.get(sessionToken);

        if (user.equals("Admin")) {
            return "error";
        }

        ShoppingBasket userBasket = userBaskets.get(user);
        HashMap<String, Integer> newItems = new HashMap<>();

        HashMap<String, Double> newValues = new HashMap<>();

        for (String paramName : formParams.keySet()) {

            if (paramName.startsWith("itemName-")) {
                String valueParamName = "itemCost-" + paramName.substring("itemName-".length());
                String itemName = formParams.get(paramName);
                Double itemCost = Double.parseDouble(formParams.get(valueParamName));

                //check for dup item names
                if (newItems.containsKey(itemName)) {
                    return "invalidInput";
                }
                //checks that new item cost is not negative or greater than Double.MAXVALUE
                if (itemCost < 0 || itemCost > Double.MAX_VALUE) {
                    return "invalidInput";
                }
                String oldName = paramName.substring("itemName-".length());
                newItems.put(itemName, userBasket.items.get(oldName));
                newValues.put(itemName, itemCost);
            }
        }

        try {
            dbHelper.deleteUserCart(user);
            dbHelper.setNewUserShoppingcart(newItems, newValues, user);
            userBasket.values = newValues;
            userBasket.items = newItems;
        } catch (SQLException se) {
            return "error";
        }

        return "redirect:/cart";
    }

    @GetMapping("/logout")
    public String logout(@CookieValue(value = "session", defaultValue = "") String sessionToken) {
        if (!sessions.containsKey(sessionToken)) {
            return "unauthorized";
        }

        sessions.remove(sessionToken);
        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        String stackTrace = Arrays.stream(e.getStackTrace())
                .limit(1)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));

        String errorMessage = String.format("%s\n%s", e.getMessage(), stackTrace);
        model.addAttribute("errorMessage", errorMessage);
        return "error";
    }

}

