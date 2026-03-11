package com.expmatik.backend.invoice;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.expmatik.backend.batch.Batch;
import com.expmatik.backend.batch.BatchService;
import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.exceptions.UnauthorizedActionException;
import com.expmatik.backend.invoice.DTOs.InvoiceRequest;
import com.expmatik.backend.invoice.DTOs.InvoiceRequestUpdate;
import com.expmatik.backend.user.User;


@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SupplierService supplierService;
    private final BatchService batchService;
    private final InvoiceCSVLector invoiceCSVLector;

    @Autowired
    public InvoiceService(
            InvoiceRepository invoiceRepository,
            SupplierService supplierService,
            BatchService batchService,
            InvoiceCSVLector invoiceCSVLector) {
        this.invoiceRepository = invoiceRepository;
        this.supplierService = supplierService;
        this.batchService = batchService;
        this.invoiceCSVLector = invoiceCSVLector;
    }

    @Transactional
    public Invoice save(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice createInvoice(User user, InvoiceRequest invoice) {
        Supplier supplier = supplierService.findOrRegister(invoice.supplierName());
        
        if(invoiceRepository.findByInvoiceNumber(invoice.invoiceNumber()).isPresent()) {
            throw new ConflictException("Invoice number already exists");
        }
        
        Invoice newInvoice = new Invoice();
        newInvoice.setInvoiceNumber(invoice.invoiceNumber());
        newInvoice.setStatus(InvoiceStatus.PENDING);
        newInvoice.setSupplier(supplier);
        newInvoice.setUser(user);
        newInvoice.setInvoiceDate(invoice.invoiceDate());
        newInvoice.setBatch(new ArrayList<>());
        newInvoice = save(newInvoice);

        if(invoice.batches().isEmpty()) {
            throw new BadRequestException("Invoice must have at least one batch");
        }
        List<Batch> batches = newInvoice.getBatch();
        for(var batch : invoice.batches()) {
            Batch createdBatch = batchService.createBatch(user.getId(), batch, newInvoice.getId());
            batches.add(createdBatch);
        }
        if(invoice.status() == InvoiceStatus.RECEIVED) {
            newInvoice = markInvoiceAsReceived(newInvoice);
        }else{
            newInvoice.setStatus(invoice.status());
        }
        return save(newInvoice);
    }

    @Transactional(readOnly = true)
    public Invoice findInvoiceById(UUID id,UUID userId) {
        Invoice invoice = invoiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (!invoice.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("Unauthorized access to invoice");
        }
        return invoice;
    }

    @Transactional(readOnly = true)
    public Invoice findInvoiceByInvoiceNumber(String invoiceNumber, UUID userId) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (!invoice.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("Unauthorized access to invoice");
        }
        return invoice;
    }

    @Transactional
    public Invoice updateInvoiceStatus(UUID id, InvoiceStatus status, UUID userId) {
        Invoice invoice = invoiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (!invoice.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("Unauthorized access to invoice");
        }
        if(invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException("Only pending invoices can be updated");
        }
        if(status == InvoiceStatus.RECEIVED){
            invoice=markInvoiceAsReceived(invoice);
            return invoice;
        }
        invoice.setStatus(status);
        return save(invoice);
    }

    @Transactional
    public Invoice markInvoiceAsReceived(Invoice invoice) {
        for(Batch batch : invoice.getBatch()) {
            batchService.addStockQuantity(batch, batch.getQuantity(), invoice.getUser());
        }
        invoice.setStatus(InvoiceStatus.RECEIVED);
        return save(invoice);
    }

    @Transactional(readOnly = true)
    public List<Invoice> searchInvoices(UUID userId, InvoiceStatus status, LocalDate startDate, LocalDate endDate, 
                                        String invoiceNumber, String supplierName, BigDecimal minPrice, BigDecimal maxPrice) {
        String statusStr = status != null ? status.name() : null;
        return invoiceRepository.searchInvoices(userId, statusStr, startDate, endDate, invoiceNumber, supplierName, minPrice, maxPrice);
    }

    @Transactional
    public void deleteInvoice(UUID invoiceId, UUID userId) {
        Invoice invoice = findInvoiceById(invoiceId, userId);
        if(invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException("Only pending invoices can be deleted");
        }
        invoiceRepository.delete(invoice);
    }

    @Transactional
    public Invoice updateInvoice(UUID id, InvoiceRequestUpdate invoiceRequest, UUID userId) {
        Invoice existingInvoice = invoiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (!existingInvoice.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("Unauthorized access to invoice");
        }
        if(existingInvoice.getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException("Only pending invoices can be updated");
        }
        if(!existingInvoice.getInvoiceNumber().equals(invoiceRequest.invoiceNumber()) 
            && invoiceRepository.findByInvoiceNumber(invoiceRequest.invoiceNumber()).isPresent()) {
            throw new ConflictException("Invoice number already exists");
        }
        
        existingInvoice.setInvoiceNumber(invoiceRequest.invoiceNumber());

        existingInvoice.setInvoiceDate(invoiceRequest.invoiceDate());
        Supplier supplier = supplierService.findOrRegister(invoiceRequest.supplierName());
        if(invoiceRequest.status() == InvoiceStatus.RECEIVED) {
            existingInvoice = markInvoiceAsReceived(existingInvoice);
        }else{
            existingInvoice.setStatus(invoiceRequest.status());
        }
        existingInvoice.setSupplier(supplier);
        existingInvoice.setInvoiceDate(invoiceRequest.invoiceDate());
        return save(existingInvoice);
    }
    @Transactional
    public List<Invoice> createInvoicesFromCSV(User user,MultipartFile csvContent) {
        if (csvContent == null || csvContent.isEmpty()) {
            throw new BadRequestException("No file uploaded or file is empty.");
        }
        String originalFilename = csvContent.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new BadRequestException("The file must have a .csv extension.");
        }

        File tempCsv;
        try {
            tempCsv = File.createTempFile("invoice-import-", ".csv");
            csvContent.transferTo(tempCsv);
        } catch (IOException ex) {
            throw new BadRequestException("Could not process the CSV file: " + ex.getMessage());
        }

        List<Invoice> createdInvoices = new ArrayList<>();
        try {
            List<InvoiceRequest> requests = invoiceCSVLector.readCSV(tempCsv);
            for (InvoiceRequest request : requests) {
                createdInvoices.add(createInvoice(user, request));
            }
        } finally {
            tempCsv.delete();
        }
        return createdInvoices;

    }
}
