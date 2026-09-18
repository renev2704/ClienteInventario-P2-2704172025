package sv.edu.utec.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import sv.edu.utec.modelo.Producto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class ProveedorAPI {

    private final HttpClient cliente = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Producto> obtenerProductos(int limite) throws IOException, InterruptedException {
        String url = "https://dummyjson.com/products?limit=" + limite + "&select=title,stock";

        HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> respuesta = cliente.send(solicitud, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() != 200) {
            throw new IOException("Error HTTP del proveedor: codigo " + respuesta.statusCode());
        }

        RespuestaProductos respuestaProductos =
                mapper.readValue(respuesta.body(), RespuestaProductos.class);

        List<Producto> productos = new ArrayList<>();
        if (respuestaProductos.getProducts() != null) {
            for (ProductoApi p : respuestaProductos.getProducts()) {
                productos.add(p.aProducto());
            }
        }
        return productos;
    }
}