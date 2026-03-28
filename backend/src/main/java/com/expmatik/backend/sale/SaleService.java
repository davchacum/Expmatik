package com.expmatik.backend.sale;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.exceptions.UnauthorizedActionException;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.product.ProductService;
import com.expmatik.backend.sale.DTOs.SaleCreate;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingSlot.VendingSlot;
import com.expmatik.backend.vendingSlot.VendingSlotService;


@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductService productService;
    private final VendingSlotService vendingSlotService;

    public SaleService(SaleRepository saleRepository, ProductService productService, VendingSlotService vendingSlotService) {
        this.saleRepository = saleRepository;
        this.productService = productService;
        this.vendingSlotService = vendingSlotService;
    }

    @Transactional
    public Sale save(Sale sale) {
        return saleRepository.save(sale);
    }

    private void checkUserAuthorization(Sale sale,User user) {
        if(sale.getVendingSlot().getVendingMachine().getUser().getId() != user.getId()) {
            throw new UnauthorizedActionException("You are not authorized to perform this action.");
        }
    }

    @Transactional(readOnly = true)
    public Sale getSaleById(UUID id, User user) {
        Sale sale = saleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));
        checkUserAuthorization(sale, user);
        return sale;
    }

    @Transactional
    public Sale createSale(SaleCreate saleCreate, User user) {
        //Logica muy basica, en donde no tiene en cuenta la asignacion de stock ni nada, 
        //simplemente crea la venta con los datos que le llegan
        Sale sale = new Sale();
        sale.setSaleDate(saleCreate.saleDate());
        sale.setTotalAmount(saleCreate.totalAmount());
        sale.setPaymentMethod(saleCreate.paymentMethod());
        sale.setStatus(saleCreate.status());
        Product product = productService.findInternalProductByBarcode(saleCreate.barcode(), user.getId());
        VendingSlot vendingSlot = vendingSlotService.getVendingSlotById(saleCreate.vendingSlotId(), user);
        sale.setProduct(product);
        sale.setVendingSlot(vendingSlot);
        return save(sale);
    }

    @Transactional
    public void delete(UUID id) {
        saleRepository.deleteById(id);
    }

}
