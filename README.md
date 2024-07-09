# ShoppingCart-2 Web Application with Database Backend

## Overview

The ShoppingBasket Web Application has been iterated from version 1 to include a database backend for user and cart data persistence. Additionally, a new system user called "Admin" has been introduced for user management tasks.

## Features

1. **User Authentication**
    - Login screen accessible at the root URL.
    - Users redirected to the /cart page upon successful login.
    - Unauthorized users receive a 401 Unauthorized response.
    - Admin user can manage other users.

2. **Admin User Management**
    - Admin can view a list of users (excluding Admin).
    - Admin can delete selected users.
    - Admin can add new users with alphanumeric usernames.

3. **Shopping Cart Management**
    - User carts loaded from the SQLite database.
    - Initial items used if no cart exists in the database.
    - Users can view and update item counts in their cart.
    - Users can add new items with names and costs.
    - Users can remove items from their cart.
    - Users can update item names and costs.


## Running the Application

1. **Start the application:**
    ```bash
    gradle build
    gradle bootRun
    ```

2. **Access the application:**
    - Open your browser and navigate to `http://localhost:8080`.


## Endpoints

1. **Login Screen**
    - URL: `/`
    - Description: Enter a valid username to log in.
    - "Admin" needs to be used first to add accounts

2. **Cart Page**
    - URL: `/cart`
    - Description: Displays the current items and their counts for the logged-in user.

3. **Add New Item**
    - URL: `/newname`
    - Description: Add a new item name and cost.

4. **Remove Item**
    - URL: `/delname`
    - Description: Remove items from the cart.

5. **Update Item**
    - URL: `/updatename`
    - Description: Update item names and costs.

6. **Update Cart**
    - URL: `/updatecart`
    - Description: Update the item counts in the cart.

7. **Admin User Management**
    - URL: `/admin`
    - Description: Admin can view, add, and delete users.
