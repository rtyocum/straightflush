package com.estore.api.estoreapi.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.Valid;

import com.estore.api.estoreapi.model.*;
import com.estore.api.estoreapi.persistence.CartDAO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private static final Logger LOG = Logger.getLogger(CartController.class.getName());

    private CartDAO cartDAO;

    @Autowired
    public CartController(CartDAO cartDAO) {
        this.cartDAO = cartDAO;
    }

    @GetMapping("")
    public ResponseEntity<Cart> getCart(@RequestHeader("token") String token) {
        LOG.info("GET /cart");
        try {
            Cart cart = cartDAO.getCart(token);
            return new ResponseEntity<Cart>(cart, HttpStatus.OK);
        } catch (AccountNotFoundException anfe) {
            LOG.log(Level.WARNING, anfe.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (InvalidTokenException ite) {
            LOG.log(Level.WARNING, ite.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, ioe.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("")
    public ResponseEntity<Cart> updateCart(@Valid @RequestHeader("token") String token,
            @Valid @RequestBody CartProduct[] products) {
        LOG.info("PUT /cart " + token + " " + Arrays.deepToString(products));
        try {
            Cart newCart = cartDAO.updateCart(token, new Cart(products));
            return new ResponseEntity<Cart>(newCart, HttpStatus.OK);
        } catch (AccountNotFoundException anfe) {
            LOG.log(Level.WARNING, anfe.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (InvalidTokenException ite) {
            LOG.log(Level.WARNING, ite.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, ioe.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("")
    public ResponseEntity<Cart> clearCart(@Valid @RequestHeader("token") String token) {
        LOG.info("Delete /cart " + token);
        try {
            Cart newCart = cartDAO.clearCart(token);
            return new ResponseEntity<Cart>(newCart, HttpStatus.OK);
        } catch (AccountNotFoundException anfe) {
            LOG.log(Level.WARNING, anfe.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (InvalidTokenException ite) {
            LOG.log(Level.WARNING, ite.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, ioe.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
