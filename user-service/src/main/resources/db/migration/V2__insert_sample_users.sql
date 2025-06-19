INSERT INTO users (id, first_name, last_name, email, password_hash, role, phone, is_active, created_at, updated_at)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'John', 'Doe', 'john.doe@example.com',
        '$2a$10$v4v3ELIwtlDYt6KaZEL4SezS.KcdPf93VYYdhoylI/gGbAeqf2aSS', 'BUYER', '123-456-7890', TRUE, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP),
       ('b1cdef01-1d2e-3f4a-5b6c-7d8e9f0a1b2c', 'Jane', 'Smith', 'jane.smith@example.com',
        '$2a$10$0EMd0Rjei5BB28ael4WKF.5sl2gG/X7TGt4X1ipDoBBdAAhAsnBAq', 'ADMIN', '098-765-4321', TRUE, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP),
       ('c2def012-2e3f-4a5b-6c7d-8e9f0a1b2c3d', 'Guest', 'User', 'guest@example.com',
        '$2a$10$tESEg9hvQUz3kL71JKQ/I.R1NZHAQnIGR0y35Nyk8asavu1Pag4MW', 'GUEST', NULL, FALSE, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);


-- Note: The password_hash values are hashed versions of 'password123', 'admin123', and 'guest123' respectively.