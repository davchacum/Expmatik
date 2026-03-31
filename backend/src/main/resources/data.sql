DELETE FROM notification;
DELETE FROM expiration_batch;
DELETE FROM sale;
DELETE FROM vending_slot;
DELETE FROM vending_machine;
DELETE FROM batch;
DELETE FROM invoice;
DELETE FROM supplier;
DELETE FROM product_info;
DELETE FROM product;
DELETE FROM refresh_tokens;
DELETE FROM app_user;

-- admin123          repo123
INSERT INTO app_user (id, email, password, first_name, last_name, role) VALUES ('00000000-0000-0000-0000-000000000001', 'admin@expmatik.com', '$2a$10$XUck0ZZl9lU8iRQpWexSy.izAOIXNW7XowjPL2Q4DUlNeljX/2tGu', 'Admin', 'Admin', 'ADMINISTRATOR');
INSERT INTO app_user (id, email, password, first_name, last_name, role) VALUES ('00000000-0000-0000-0000-000000000003', 'admin2@expmatik.com', '$2a$10$XUck0ZZl9lU8iRQpWexSy.izAOIXNW7XowjPL2Q4DUlNeljX/2tGu', 'Admin', 'Admin', 'ADMINISTRATOR');
INSERT INTO app_user (id, email, password, first_name, last_name, role) VALUES ('00000000-0000-0000-0000-000000000002', 'repo@expmatik.com', '$2a$10$HtXpXb2XGvjL8MeO4Zcn.ee0SSKMi5hHcyM5hAg3BH6Da3j1hqEb6', 'Reponedor', 'Reponedor', 'MAINTAINER');

INSERT INTO product (id, name, brand, description, is_perishable, barcode, is_custom, created_by,image_url) VALUES 
('00000000-0000-0000-0000-000000000001', 'Leche Entera', 'Puleva', 'Leche entera de vaca', true, '20000001', false, null, 'https://example.com/images/leche_entera.jpg'),
('00000000-0000-0000-0000-000000000002', 'Pan de Molde', 'Bimbo', 'Pan de molde integral', false, '20000002', false, null, 'https://example.com/images/pan_molde.jpg'),
('00000000-0000-0000-0000-000000000003', 'Yogur Natural', 'Danone', 'Yogur natural sin azúcar', false, '20000003', false, null, 'https://example.com/images/yogur_natural.jpg'),
('00000000-0000-0000-0000-000000000004', 'ProductoPersonalizado', 'ProductoPersonalizado', 'Producto personalizado de prueba', false, '20000000', true, '00000000-0000-0000-0000-000000000001', '/uploads/images/fotoPrueba.jpg');

INSERT INTO product_info (id,stock_quantity, vat_rate,sale_unit_price,last_purchase_unit_price,need_update,product_id,user_id) VALUES 
('00000000-0000-0000-0000-000000000001', 100, 0.21, 2.50, 1.5,false, '00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000002', 200, 0.21, 3.00, 2.2,false, '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001');

INSERT INTO supplier (id, name) VALUES 
('00000000-0000-0000-0000-000000000001', 'Proveedor 1');

INSERT INTO invoice (id,invoice_date,invoice_number,status,supplier_id,user_id) VALUES 
('00000000-0000-0000-0000-000000000001', '2024-01-01', 'INV-001', 'PENDING', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000002', '2024-02-02', 'INV-002', 'RECEIVED', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001');

INSERT INTO batch (id, expiration_date, unit_price, quantity, product_id, invoice_id) VALUES 
('00000000-0000-0000-0000-000000000001', '2024-12-31', 3, 50, '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000002', null, 2, 100, '00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000003', '2024-11-30', 1.59, 50, '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002');

INSERT INTO vending_machine (id, location, name,column_count,row_count,user_id) VALUES 
('00000000-0000-0000-0000-000000000001', 'Edificio A, Planta Baja', 'Máquina 1', 1, 1, '00000000-0000-0000-0000-000000000001');

INSERT INTO vending_slot (id,max_capacity,current_stock,is_blocked,row_number,column_number,vending_machine_id, product_id) VALUES 
('00000000-0000-0000-0000-000000000001', 2, 0, false, 1, 1, '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001');