package sv.edu.utec.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RespuestaProductos {

    private List<ProductoApi> products;

    public RespuestaProductos() {
    }

    public List<ProductoApi> getProducts() { return products; }
    public void setProducts(List<ProductoApi> products) { this.products = products; }
}