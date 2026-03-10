-- admin123          repo123
INSERT INTO app_user (id, email, password, first_name, last_name, role) VALUES ('00000000-0000-0000-0000-000000000001', 'admin@expmatik.com', '$2a$10$XUck0ZZl9lU8iRQpWexSy.izAOIXNW7XowjPL2Q4DUlNeljX/2tGu', 'Admin', 'Admin', 'ADMINISTRATOR');
INSERT INTO app_user (id, email, password, first_name, last_name, role) VALUES ('00000000-0000-0000-0000-000000000003', 'admin2@expmatik.com', '$2a$10$XUck0ZZl9lU8iRQpWexSy.izAOIXNW7XowjPL2Q4DUlNeljX/2tGu', 'Admin', 'Admin', 'ADMINISTRATOR');
INSERT INTO app_user (id, email, password, first_name, last_name, role) VALUES ('00000000-0000-0000-0000-000000000002', 'repo@expmatik.com', '$2a$10$HtXpXb2XGvjL8MeO4Zcn.ee0SSKMi5hHcyM5hAg3BH6Da3j1hqEb6', 'Reponedor', 'Reponedor', 'MAINTAINER');

INSERT INTO product (id, name, brand, description, is_perishable, barcode, is_custom, created_by,image_url) VALUES 
('00000000-0000-0000-0000-000000000001', 'Leche Entera', 'Puleva', 'Leche entera de vaca', false, '20000001', false, null, 'https://example.com/images/leche_entera.jpg'),
('00000000-0000-0000-0000-000000000002', 'Pan de Molde', 'Bimbo', 'Pan de molde integral', false, '20000002', false, null, 'https://example.com/images/pan_molde.jpg'),
('00000000-0000-0000-0000-000000000003', 'Yogur Natural', 'Danone', 'Yogur natural sin azúcar', false, '20000003', false, null, 'https://example.com/images/yogur_natural.jpg'),
('00000000-0000-0000-0000-000000000004', 'ProductoPersonalizado', 'ProductoPersonalizado', 'Producto personalizado de prueba', false, '20000000', true, '00000000-0000-0000-0000-000000000001', '/uploads/images/fotoPrueba.jpg');

INSERT INTO product_info (id,stock_quantity, vat_rate,sale_unit_price,last_purchase_unit_price,product_id,user_id) VALUES 
('00000000-0000-0000-0000-000000000001', 100, 0.21, 2.50, 1.5, '00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000003', 200, 0.21, 3.00, 2.2, '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001');
