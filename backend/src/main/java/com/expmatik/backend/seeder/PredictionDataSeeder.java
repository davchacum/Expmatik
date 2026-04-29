package com.expmatik.backend.seeder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.expmatik.backend.product.ProductService;
import com.expmatik.backend.sale.PaymentMethod;
import com.expmatik.backend.sale.SaleService;
import com.expmatik.backend.sale.TransactionStatus;
import com.expmatik.backend.sale.DTOs.SaleCreate;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserRepository;
import com.expmatik.backend.vendingMachine.VendingMachineService;

@Component
public class PredictionDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PredictionDataSeeder.class);
    private static final String CSV_PATH = "prediction-sales.csv";
    private static final String PREDICTION_USER_EMAIL = "prediction@expmatik.com";
    private static final int DEFAULT_MAX_CAPACITY_PER_SLOT = 10;

    private final VendingMachineService vendingMachineService;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final SaleService saleService;

    public PredictionDataSeeder(VendingMachineService vendingMachineService,
                                ProductService productService,
                                UserRepository userRepository,
                                SaleService saleService) {
        this.vendingMachineService = vendingMachineService;
        this.productService = productService;
        this.userRepository = userRepository;
        this.saleService = saleService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        User predictionUser = userRepository.findByEmail(PREDICTION_USER_EMAIL).orElse(null);
        if (predictionUser == null) {
            log.warn("Prediction seeder skipped: user {} not found", PREDICTION_USER_EMAIL);
            return;
        }

        Map<String, int[]> machineMaxDimensions = new LinkedHashMap<>();
        Set<String> uniqueBarcodes = new LinkedHashSet<>();
        List<SaleCreate> salesRows = new ArrayList<>();

        parseCsv(machineMaxDimensions, uniqueBarcodes, salesRows);

        for (Map.Entry<String, int[]> entry : machineMaxDimensions.entrySet()) {
            String machineName = entry.getKey();
            int[] dims = entry.getValue();
            vendingMachineService.createVendingMachineForSeeder(
                    machineName, machineName, dims[0], dims[1], DEFAULT_MAX_CAPACITY_PER_SLOT, predictionUser);
        }

        for (String barcode : uniqueBarcodes) {
            productService.createProductForSeeder(
                    barcode, "Product " + barcode, "Prediction Data", null, false);
        }

        boolean salesAlreadyExist = saleService
                .searchSales(predictionUser.getId(), null, null, null, null, null, null, null, null, PageRequest.of(0, 1))
                .getTotalElements() > 0;

        int salesCreated = 0;
        if (!salesAlreadyExist) {
            for (SaleCreate saleCreate : salesRows) {
                saleService.createSale(saleCreate, predictionUser);
                salesCreated++;
            }
        }

        log.info("Prediction seeder completed: {} machines, {} products and {} sales seeded for {}",
                machineMaxDimensions.size(), uniqueBarcodes.size(), salesCreated, PREDICTION_USER_EMAIL);
    }

    private void parseCsv(Map<String, int[]> machineMaxDimensions, Set<String> uniqueBarcodes, List<SaleCreate> salesRows) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(CSV_PATH);
        if (is == null) {
            log.warn("Prediction seeder: {} not found in classpath, skipping", CSV_PATH);
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                if (parts.length < 8) continue;

                LocalDateTime saleDate = LocalDateTime.parse(parts[0].trim());
                BigDecimal totalAmount = new BigDecimal(parts[1].trim());
                PaymentMethod paymentMethod = PaymentMethod.valueOf(parts[2].trim());
                TransactionStatus status = TransactionStatus.valueOf(parts[3].trim());
                String barcode = parts[4].trim();
                String machineName = parts[5].trim();
                int rowNumber = Integer.parseInt(parts[6].trim());
                int colNumber = Integer.parseInt(parts[7].trim());

                uniqueBarcodes.add(barcode);
                machineMaxDimensions.merge(
                        machineName,
                        new int[]{rowNumber, colNumber},
                        (existing, newVal) -> new int[]{
                                Math.max(existing[0], newVal[0]),
                                Math.max(existing[1], newVal[1])
                        });

                salesRows.add(new SaleCreate(saleDate, totalAmount, paymentMethod, status, barcode, machineName, rowNumber, colNumber));
            }
        }
    }
}
