package com.changarrito.ui.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.ui.adapters.ProductoAdapter;
import com.changarrito.ui.fragments.ProductoDialogFragment;
import com.changarrito.viewmodel.ProductoViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class ProductosActivity extends AppCompatActivity implements ProductoDialogFragment.ProductoDialogListener {

    private RecyclerView rvProductos;
    private FloatingActionButton fabAgregarProducto;
    private ProductoAdapter adapter;
    private ProductoViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productos);

        rvProductos = findViewById(R.id.rvProductos);
        fabAgregarProducto = findViewById(R.id.fabAgregarProducto);

        // ViewModel
        viewModel = new ViewModelProvider(this).get(ProductoViewModel.class);

        // RecyclerView setup
        rvProductos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductoAdapter(new ArrayList<>());
        rvProductos.setAdapter(adapter);

        // Observar cambios en BD
        viewModel.getAllProductos().observe(this, productos -> {
            adapter = new ProductoAdapter(productos);
            rvProductos.setAdapter(adapter);
        });

        // FAB click listener - Abrir dialog agregar
        fabAgregarProducto.setOnClickListener(v -> {
            ProductoDialogFragment dialog = ProductoDialogFragment.newInstance(null, this);
            dialog.show(getSupportFragmentManager(), "ProductoDialog");
        });
    }

    @Override
    public void onProductoGuardado(ProductoEntity producto) {
        if (producto.id == 0) {
            // Nuevo producto
            viewModel.insertarProducto(producto);
        } else {
            // Editar producto existente
            viewModel.actualizarProducto(producto);
        }
    }
}