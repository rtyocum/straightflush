package com.estore.api.estoreapi.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.validation.Valid;

import com.estore.api.estoreapi.model.Product;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InventoryFileDAO implements InventoryDAO {

    /**
     * The current inventory.
     */
    private Map<String, Product> inventory;

    /**
     * The file name of the inventory file.
     */
    private String filename;

    /**
     * The object mapper.
     */

    private ObjectMapper objectMapper;

    public InventoryFileDAO(@Value("${inventory.filename}") String filename, ObjectMapper objectMapper)
            throws IOException {
        this.filename = filename;
        this.objectMapper = objectMapper;
        loadInventory();
    }

    private ArrayList<Product> getInventoryArray() {
        return new ArrayList<>(inventory.values());
    }

    @Override
    public Product createProduct(@Valid Product product) throws IOException,
            IllegalArgumentException {
        synchronized (inventory) {
            Product newProduct = new Product(product.getName(),
                    product.getDescription(), product.getPrice(),
                    product.getQuantity());

            if (inventory.containsKey(newProduct.getName())) {
                throw new IllegalArgumentException("Product with name " +
                        newProduct.getName() + " already exists");
            }
            inventory.put(newProduct.getName(), newProduct);
            saveInventory();
            return newProduct;
        }
    }

    @Override
    public Product[] searchProducts(String searchTerms) {
        if (searchTerms.length() == 0)
            return new Product[0];

        ArrayList<Product> products = new ArrayList<>();
        for (Product product : inventory.values()) {
            if (product.getName().toLowerCase().contains(searchTerms.toLowerCase())) {
                products.add(product);
            }
        }

        return products.toArray(new Product[0]);
    }

    public Product[] getInventory() {
        ArrayList<Product> products = new ArrayList<>();

        for (Product product : inventory.values())
            products.add(product);

        Product[] pArray = new Product[products.size()];
        pArray = products.toArray(pArray);

        return pArray;
    }

    @Override
    public Product getProduct(String name) {
        synchronized (inventory) {
            Product tempProduct = inventory.get(name);
            return tempProduct;
        }
    }

    @Override
    public Product updateProduct(Product product) throws IOException, IllegalArgumentException {
        synchronized (inventory) {
            if (!inventory.containsKey(product.getName())) {
                throw new IllegalArgumentException("Product with name " +
                        product.getName() + " does not exist");
            }
            Product updatedProduct = new Product(product.getName(), product.getDescription(), product.getPrice(),
                    product.getQuantity());

            inventory.put(updatedProduct.getName(), updatedProduct);
            saveInventory();
            return updatedProduct;
        }
    }

    private void saveInventory() throws IOException {
        objectMapper.writeValue(new File(filename), getInventoryArray());
    }

    private void loadInventory() throws IOException {
        inventory = new TreeMap<>();
        Product[] inventoryArray = objectMapper.readValue(new File(filename), Product[].class);
        for (Product product : inventoryArray) {
            inventory.put(product.getName(), product);
        }
    }

    @Override
    public boolean deleteProduct(String name) throws IOException {
        boolean success = true;

        synchronized (inventory) {
            try {
                inventory.remove(name);
            } catch (Exception e) {
                success = false;
                return success;
            }
        }

        return success;
    }
}
