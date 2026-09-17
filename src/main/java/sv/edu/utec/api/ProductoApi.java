package sv.edu.utec.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import sv.edu.utec.modelo.Producto;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductoApi {

    private int id;
    private String title;
    private int stock;

    public ProductoApi() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public Producto aProducto() {
        String nombre = title;
        if (nombre != null && nombre.length() > 50) {
            nombre = nombre.substring(0, 50);
        }
        return new Producto(id, nombre, stock);
    }
}