package sv.edu.utec.servicio;

import sv.edu.utec.api.ProveedorAPI;
import sv.edu.utec.datos.ProductoDAO;
import sv.edu.utec.modelo.Producto;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class SincronizacionService {

    private final ProveedorAPI proveedor;
    private final ProductoDAO dao;

    public SincronizacionService(ProveedorAPI proveedor, ProductoDAO dao) {
        this.proveedor = proveedor;
        this.dao = dao;
    }

    public int[] sincronizar(int limite) throws IOException, InterruptedException, SQLException {
        List<Producto> productos = proveedor.obtenerProductos(limite);

        int insertados = 0;
        int actualizados = 0;

        for (Producto p : productos) {
            if (dao.existe(p.getId())) {
                dao.actualizar(p);
                actualizados++;
            } else {
                dao.insertar(p);
                insertados++;
            }
        }

        return new int[]{insertados, actualizados};
    }
}