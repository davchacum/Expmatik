package com.expmatik.backend.invoice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional Supplier findOrRegister(String name) {
        Supplier supplier = supplierRepository.findByName(name).orElseGet(() -> {
            Supplier newSupplier = new Supplier(name);
            return supplierRepository.save(newSupplier);
        });
        return supplier;
    }

}
