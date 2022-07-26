package com.estore.api.estoreapi.persistence;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;

import com.estore.api.estoreapi.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CartFileDAO implements CartDAO {
    private static final Logger LOG = Logger.getLogger(CartFileDAO.class.getName());
    private File cartsDirectory = new File("data/carts");
    private static CartFileDAO instance = null;

    private Map<Integer, Cart> carts;

    /**
     * The object mapper.
     */
    private ObjectMapper objectMapper;

    private UserDAO userDAO = null;
    private InventoryDAO inventoryDAO = null;

    @Autowired
    public CartFileDAO(ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
        readAllCarts();
        if (instance == null)
            instance = this;
    }

    public CartFileDAO(ObjectMapper objectMapper, String cartsDirectory) throws IOException {
        this.objectMapper = objectMapper;
        this.cartsDirectory = new File(cartsDirectory);
        readAllCarts();
        if (instance == null)
            instance = this;
    }

    public static CartFileDAO getInstance() {
        return instance;
    }

    private void getSingletonDependencies() {
        if (this.userDAO == null)
            this.userDAO = UserFileDAO.getInstance();
        if (this.inventoryDAO == null)
            this.inventoryDAO = InventoryFileDAO.getInstance();
    }

    @Override
    public Cart createCart(UserAccount user, Cart cart) throws IOException {
        getSingletonDependencies();
        File newCart = new File(cartsDirectory, user.getId() + ".json");
        if (newCart.createNewFile()) {
            Cart verified = verifyCart(cart);
            this.carts.put(user.getId(), verified);
            this.writeCart(user.getId(), verified);
            return cart;
        } else {
            throw new FileAlreadyExistsException(newCart.getName());
        }
    }

    @Override
    public Cart getCart(String token)
            throws AccountNotFoundException, InvalidTokenException, IOException {
        getSingletonDependencies();
        UserAccount user = userDAO.verifyToken(token);
        return verifyCart(carts.get(user.getId()));
    }

    @Override
    public Cart updateCart(String token, Cart cart)
            throws AccountNotFoundException, InvalidTokenException, IOException {
        getSingletonDependencies();
        int id = userDAO.verifyToken(token).getId();
        cart = verifyCart(cart);
        carts.put(id, cart);
        writeCart(id, cart);
        return cart;
    }

    @Override
    public Cart clearCart(String token)
            throws AccountNotFoundException, InvalidTokenException, IOException {
        getSingletonDependencies();
        int id = userDAO.verifyToken(token).getId();
        Cart old = new Cart(carts.get(id).getProducts());
        carts.put(id, Cart.EMPTY);
        writeCart(id, Cart.EMPTY);
        return old;
    }

    public Cart deleteCart(String token)
            throws AccountNotFoundException, InvalidTokenException, IOException {
        getSingletonDependencies();
        int id = userDAO.verifyToken(token).getId();
        Cart old = new Cart(carts.get(id).getProducts());
        carts.remove(id);
        deleteCartFile(getCartFile(id));
        return old;
    }

    private Cart verifyCart(Cart cart) throws IOException {
        if(cart == null)
        {
            return null;
        }
        getSingletonDependencies();
        ArrayList<CartProduct> products = new ArrayList<>();
        Double totalPrice = 0.0;
        boolean changed = false;
        for (CartProduct cartProduct : cart.getProducts()) {
            Product product = inventoryDAO.getProduct(cartProduct.getId());
            if (product == null) {
                LOG.log(Level.INFO, String.format(
                        "An item of ID %d was removed from a cart because it does not exist in the inventory.",
                        cartProduct.getId()));
                changed = true;
            } else if (product.getQuantity() != null
                    && product.getQuantity() < cartProduct.getQuantity()) {
                LOG.log(Level.INFO, String.format(
                        "An item of ID %d has exceeded the max in the inventory, trimmed down.",
                        cartProduct.getId()));
                changed = true;
                products.add(new CartProduct(product.getId(), product.getQuantity()));
                totalPrice += product.getQuantity() * product.getPrice();
            } else if (cartProduct.getQuantity() != null && product.getPrice() != null) {
                products.add(cartProduct);
                totalPrice += product.getPrice() * cartProduct.getQuantity();
            } else if (cartProduct.getQuantity() != null) {
                products.add(cartProduct);
            } else {
                changed = true;
            }
        }
        Cart verified = !changed ? cart : new Cart(products.toArray(new CartProduct[0]));
        verified.setTotalPrice(totalPrice);
        return verified;
    }

    private void readAllCarts() throws IOException {
        this.carts = new TreeMap<Integer, Cart>();
        for (File cartFile : cartsDirectory.listFiles()) {
            if (cartFile.getName().substring(cartFile.getName().length() - 4).equals("json")) {
                Integer id = Integer.parseInt(cartFile.getName().replaceAll(".json", ""));
                System.out.printf("Reading cart %s%n", cartFile.getPath());
                this.carts.put(id, readCart(cartFile));
            }
        }
    }

    private Cart readCart(File cartFile) throws IOException {
        CartProduct[] products = objectMapper.readValue(cartFile, CartProduct[].class);
        System.out.printf("Got cart with %d products\n", products.length);
        return new Cart(products);
    }

    private void writeCart(int id, Cart cart) throws IOException {
        File newCart = new File(cartsDirectory, id + ".json");
        objectMapper.writeValue(newCart, cart.getProducts());
    }

    private File getCartFile(int id) {
        return (new File(cartsDirectory, id + ".json"));
    }

    private void deleteCartFile(File cartFile) {
        cartFile.delete();
    }
}
