package com.expmatik.backend.invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.batch.Batch;
import com.expmatik.backend.batch.BatchService;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.exceptions.UnauthorizedActionException;
import com.expmatik.backend.invoice.DTOs.InvoiceRequest;
import com.expmatik.backend.invoice.DTOs.InvoiceRequestUpdate;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;


@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SupplierService supplierService;
    private final UserService userService;
    private final BatchService batchService;

    @Autowired
    public InvoiceService(InvoiceRepository invoiceRepository, SupplierService supplierService, UserService userService, BatchService batchService) {
        this.invoiceRepository = invoiceRepository;
        this.supplierService = supplierService;
        this.userService = userService;
        this.batchService = batchService;
    }

    @Transactional
    public Invoice createInvoice(InvoiceRequest invoice) {
        User user = userService.getUserProfile();
        Supplier supplier = supplierService.findOrRegister(invoice.supplierName());
        
        if(invoiceRepository.findByInvoiceNumber(invoice.invoiceNumber()).isPresent()) {
            throw new ConflictException("Invoice number already exists");
        }
        
        Invoice newInvoice = new Invoice();
        newInvoice.setInvoiceNumber(invoice.invoiceNumber());
        newInvoice.setStatus(invoice.status());
        newInvoice.setSupplier(supplier);
        newInvoice.setUser(user);
        newInvoice.setInvoiceDate(invoice.invoiceDate());
        newInvoice = invoiceRepository.save(newInvoice);

        List<Batch> batches = new ArrayList<>();
        for(var batch : invoice.batches()) {
            Batch createdBatch = batchService.createBatch(batch, newInvoice.getId());
            batches.add(createdBatch);
        }
        newInvoice.setBatch(batches);
        return invoiceRepository.save(newInvoice);
    }

    @Transactional(readOnly = true)
    public Invoice findInvoiceById(UUID id) {
        Invoice invoice = invoiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        User user = userService.getUserProfile();
        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("Unauthorized access to invoice");
        }
        return invoice;
    }

    @Transactional(readOnly = true)
    public Invoice findInvoiceByInvoiceNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        User user = userService.getUserProfile();
        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("Unauthorized access to invoice");
        }
        return invoice;
    }

    @Transactional
    public Invoice updateInvoiceStatus(UUID id, InvoiceStatus status) {
        Invoice invoice = invoiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        User user = userService.getUserProfile();
        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("Unauthorized access to invoice");
        }
        if(invoice.getStatus() == InvoiceStatus.RECEIVED) {
            throw new ConflictException("Cannot change status of an invoice that has already been received");
        }
        if(invoice.getStatus() == InvoiceStatus.CANCELED) {
            throw new ConflictException("Cannot change status of a canceled invoice");
        }
        if(status == InvoiceStatus.RECEIVED){
            invoice=markInvoiceAsReceived(invoice);
            return invoice;
        }
        invoice.setStatus(status);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice markInvoiceAsReceived(Invoice invoice) {
        //Lógica verificar lotes, asignar productos del catalogo global o privado, en caso de no encontrar pedir crear producto personalizado y actualizar stock
        invoice.setStatus(InvoiceStatus.RECEIVED);
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        User user = userService.getUserProfile();
        return invoiceRepository.findByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public List<Invoice> searchInvoices(InvoiceStatus status, LocalDate startDate, LocalDate endDate, 
                                        String invoiceNumber, String supplierName, BigDecimal minPrice, BigDecimal maxPrice) {
        User user = userService.getUserProfile();
        String statusStr = status != null ? status.name() : null;
        return invoiceRepository.searchInvoices(user.getId(), statusStr, startDate, endDate, invoiceNumber, supplierName, minPrice, maxPrice);
    }

    @Transactional
    public void deleteInvoice(String invoiceNumber) {
        Invoice invoice = findInvoiceByInvoiceNumber(invoiceNumber);
        User user = userService.getUserProfile();
        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("Unauthorized access to invoice");
        }
        if(invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException("Only pending invoices can be deleted");
        }
        invoiceRepository.delete(invoice);
    }

    @Transactional
    public Invoice updateInvoice(UUID id, InvoiceRequestUpdate invoiceRequest ) {
        Invoice existingInvoice = invoiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        User user = userService.getUserProfile();
        if (!existingInvoice.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("Unauthorized access to invoice");
        }
        if(existingInvoice.getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException("Only pending invoices can be updated");
        }
        existingInvoice.setInvoiceNumber(invoiceRequest.invoiceNumber());

        if(!existingInvoice.getInvoiceNumber().equals(invoiceRequest.invoiceNumber())) {
            if(invoiceRepository.findByInvoiceNumber(invoiceRequest.invoiceNumber()).isPresent()) {
                throw new ConflictException("Invoice number already exists");
            }
        }

        existingInvoice.setInvoiceDate(invoiceRequest.invoiceDate());
        Supplier supplier = supplierService.findOrRegister(invoiceRequest.supplierName());
        existingInvoice.setStatus(invoiceRequest.status());
        existingInvoice.setSupplier(supplier);
        existingInvoice.setInvoiceDate(LocalDate.now());
        return invoiceRepository.save(existingInvoice);
    }
}
