-- admin123          repo123
INSERT INTO app_user (id, email, password, first_name, last_name, role) VALUES ('00000000-0000-0000-0000-000000000001', 'admin@expmatik.com', '$2a$10$XUck0ZZl9lU8iRQpWexSy.izAOIXNW7XowjPL2Q4DUlNeljX/2tGu', 'Admin', 'Admin', 'ADMINISTRATOR');
INSERT INTO app_user (id, email, password, first_name, last_name, role) VALUES ('00000000-0000-0000-0000-000000000002', 'repo@expmatik.com', '$2a$10$HtXpXb2XGvjL8MeO4Zcn.ee0SSKMi5hHcyM5hAg3BH6Da3j1hqEb6', 'Reponedor', 'Reponedor', 'MAINTAINER');

INSERT INTO product (id, name, brand, description, is_perishable, barcode, is_custom, created_by,image_url) VALUES 
('00000000-0000-0000-0000-000000000001', 'Leche Entera', 'Puleva', 'Leche entera de vaca', false, '1234567890123', false, null, 'https://example.com/images/leche_entera.jpg'),
('00000000-0000-0000-0000-000000000002', 'Pan de Molde', 'Bimbo', 'Pan de molde integral', false, '2345678901234', false, null, 'https://example.com/images/pan_molde.jpg'),
('00000000-0000-0000-0000-000000000003', 'Yogur Natural', 'Danone', 'Yogur natural sin azúcar', false, '3456789012345', false, null, 'https://example.com/images/yogur_natural.jpg');  
