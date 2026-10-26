package com.changarrito.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.repository.MermaRepository;
import com.changarrito.repository.ProductoRepository;
import com.changarrito.repository.VentaRepository;

import java.util.ArrayList;
import java.util.List;

public class VentaViewModel extends AndroidViewModel {

    private final ProductoRepository productoRepository;
    private final VentaRepository ventaRepository;

    private final LiveData<List<ProductoEntity>> productosDisponibles;
    private final MutableLiveData<List<ItemCarrito>> carrito = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> mensaje = new MutableLiveData<>();
    private final MutableLiveData<Boolean> ventaRegistrada = new MutableLiveData<>();
    private volatile boolean registrando;

    public VentaViewModel(@NonNull Application application) {
        super(application);
        productoRepository = new ProductoRepository(application);
        ventaRepository = new VentaRepository(application);
        productosDisponibles = productoRepository.getAllProductos();
    }

    public LiveData<List<ProductoEntity>> getProductosDisponibles() {
        return productosDisponibles;
    }

    public LiveData<List<ItemCarrito>> getCarrito() {
        return carrito;
    }

    /** Mensajes de error/aviso para mostrar en un Toast. Se limpia sola tras leerse una vez. */
    public LiveData<String> getMensaje() {
        return mensaje;
    }

    public LiveData<Boolean> getVentaRegistrada() {
        return ventaRegistrada;
    }

    public double calcularCantidadPorImporte(ProductoEntity producto, double importeDeseado) {
        return ventaRepository.calcularCantidadPorImporte(producto, importeDeseado);
    }

    /** Para el escaneo directo desde la pantalla de venta: busca el producto por su código. */
    public LiveData<ProductoEntity> buscarProductoPorBarcode(String barcode) {
        return productoRepository.getProductoByBarcode(barcode);
    }

    /**
     * Agrega un producto al carrito. Si ya estaba, suma la cantidad a la línea
     * existente en vez de duplicarla. Valida contra el stock ya restando lo que
     * otras líneas del carrito ya "apartaron" en esta misma venta, para no dejar
     * que el carrito prometa más de lo que hay disponible.
     */
    public void agregarAlCarrito(ProductoEntity producto, double cantidad) {
        if (producto == null) {
            mensaje.setValue("Selecciona un producto.");
            return;
        }
        if (cantidad <= 0) {
            mensaje.setValue("La cantidad debe ser mayor a 0.");
            return;
        }
        if (producto.unidadMedida == UnidadMedida.PZA && cantidad != Math.floor(cantidad)) {
            mensaje.setValue(producto.nombre + " se vende por pieza, no acepta decimales.");
            return;
        }

        List<ItemCarrito> actual = new ArrayList<>(
                carrito.getValue() != null ? carrito.getValue() : new ArrayList<>());

        for (ItemCarrito item : actual) {
            if (item.producto.id == producto.id) {
                double nuevaCantidad = item.cantidad + cantidad;
                if (nuevaCantidad > producto.cantidadDisponible) {
                    mensaje.setValue(mensajeStockInsuficiente(producto));
                    return;
                }
                item.cantidad = nuevaCantidad;
                carrito.setValue(actual);
                return;
            }
        }

        if (cantidad > producto.cantidadDisponible) {
            mensaje.setValue(mensajeStockInsuficiente(producto));
            return;
        }

        actual.add(new ItemCarrito(producto, cantidad));
        carrito.setValue(actual);
    }

    private String mensajeStockInsuficiente(ProductoEntity producto) {
        return "Solo hay " + MermaRepository.cantidadTexto(producto.cantidadDisponible, producto.unidadMedida)
                + " de " + producto.nombre + ".";
    }

    /**
     * Botones − / + del carrito y "cambiar cantidad" de los productos a granel: deja la
     * linea con la cantidad nueva (0 o menos la quita), con las mismas reglas que al
     * agregar (por pieza sin decimales y nunca mas de lo que hay).
     */
    public void cambiarCantidad(int idProducto, double nueva) {
        List<ItemCarrito> actual = new ArrayList<>(
                carrito.getValue() != null ? carrito.getValue() : new ArrayList<>());
        for (int i = 0; i < actual.size(); i++) {
            ItemCarrito item = actual.get(i);
            if (item.producto.id != idProducto) continue;
            ProductoEntity p = item.producto;
            if (nueva <= 0.0005) {
                actual.remove(i);
            } else if (p.unidadMedida == UnidadMedida.PZA && nueva != Math.floor(nueva)) {
                mensaje.setValue(p.nombre + " se vende por pieza, no acepta decimales.");
                return;
            } else if (nueva > p.cantidadDisponible + 0.0005) {
                mensaje.setValue(mensajeStockInsuficiente(p));
                return;
            } else {
                item.cantidad = nueva;
            }
            carrito.setValue(actual);
            return;
        }
    }

    public void quitarDelCarrito(ItemCarrito item) {
        List<ItemCarrito> actual = new ArrayList<>(
                carrito.getValue() != null ? carrito.getValue() : new ArrayList<>());
        actual.remove(item);
        carrito.setValue(actual);
    }

    public void limpiarCarrito() {
        carrito.setValue(new ArrayList<>());
    }

    /**
     * Registra las líneas del carrito una por una contra VentaRepository (que valida
     * el stock real en BD dentro de su propia transacción — la validación de arriba
     * es solo para dar feedback rápido en la UI, esta es la que manda). Si una línea
     * falla, se detiene ahí: las líneas anteriores ya se registraron y quedan fuera
     * del carrito (venta parcial), y se avisa cuál línea falló.
     */
    public void registrarVenta() {
        List<ItemCarrito> items = carrito.getValue();
        if (items == null || items.isEmpty()) {
            mensaje.setValue("El carrito está vacío.");
            return;
        }
        // Dos toques rapidos a "Cobrar" no deben registrar la venta dos veces.
        if (registrando) return;
        registrando = true;
        // Un solo numero de ticket para todo el carrito (sirve para el ticket promedio).
        registrarSiguiente(new ArrayList<>(items), 0, System.currentTimeMillis());
    }

    private void registrarSiguiente(List<ItemCarrito> items, int indice, long idTicket) {
        if (indice >= items.size()) {
            carrito.postValue(new ArrayList<>());
            registrando = false;
            ventaRegistrada.postValue(true);
            return;
        }

        ItemCarrito item = items.get(indice);
        ventaRepository.registrarVenta(item.producto.id, item.cantidad, idTicket, resultado -> {
            if (resultado.exito) {
                if (indice + 1 >= items.size()) {
                    // Este callback llega en un hilo de fondo: postValue, no setValue.
                    carrito.postValue(new ArrayList<>());
                    registrando = false;
                    ventaRegistrada.postValue(true);
                } else {
                    // Quitar del carrito lo ya vendido antes de seguir con la siguiente línea
                    List<ItemCarrito> restantes = new ArrayList<>(items.subList(indice + 1, items.size()));
                    carrito.postValue(restantes);
                    registrarSiguiente(items, indice + 1, idTicket);
                }
            } else {
                registrando = false;
                mensaje.postValue("Venta parcial registrada. Falló " + item.producto.nombre
                        + ": " + resultado.error);
            }
        });
    }

    public static class ItemCarrito {
        public final ProductoEntity producto;
        public double cantidad;

        public ItemCarrito(ProductoEntity producto, double cantidad) {
            this.producto = producto;
            this.cantidad = cantidad;
        }

        public double getSubtotal() {
            return producto.precio * cantidad;
        }
    }
}
