CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  id_card VARCHAR(18) NULL,
  phone VARCHAR(20) NULL,
  email VARCHAR(80) NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'PASSENGER',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT ck_user_role CHECK (role IN ('ADMIN', 'PASSENGER'))
);

CREATE TABLE IF NOT EXISTS flight (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  flight_no VARCHAR(20) NOT NULL,
  airline VARCHAR(50) NOT NULL,
  departure_city VARCHAR(40) NOT NULL,
  arrival_city VARCHAR(40) NOT NULL,
  departure_airport VARCHAR(80) NOT NULL,
  arrival_airport VARCHAR(80) NOT NULL,
  departure_time DATETIME NOT NULL,
  arrival_time DATETIME NOT NULL,
  aircraft_type VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT ck_flight_status CHECK (status IN ('SCHEDULED', 'CANCELLED', 'DEPARTED', 'ARRIVED')),
  UNIQUE KEY uk_flight_no_time (flight_no, departure_time),
  INDEX idx_flight_route_time (departure_city, arrival_city, departure_time)
);

CREATE TABLE IF NOT EXISTS flight_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  flight_id BIGINT NOT NULL,
  cabin_type VARCHAR(20) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  total_seats INT NOT NULL,
  available_seats INT NOT NULL,
  version INT NOT NULL DEFAULT 0,
  CONSTRAINT ck_cabin_type CHECK (cabin_type IN ('ECONOMY', 'BUSINESS')),
  CONSTRAINT ck_inventory_seats CHECK (total_seats >= 0 AND available_seats >= 0 AND available_seats <= total_seats),
  CONSTRAINT ck_inventory_price CHECK (price >= 0),
  CONSTRAINT fk_inventory_flight FOREIGN KEY (flight_id) REFERENCES flight(id),
  UNIQUE KEY uk_flight_cabin (flight_id, cabin_type)
);

CREATE TABLE IF NOT EXISTS ticket_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
  pay_deadline DATETIME NULL,
  paid_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT ck_order_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'CANCELLED', 'REFUNDED')),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  INDEX idx_order_user_created (user_id, created_at)
);

CREATE TABLE IF NOT EXISTS ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_no VARCHAR(40) NOT NULL UNIQUE,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  flight_id BIGINT NOT NULL,
  original_flight_id BIGINT NULL,
  passenger_name VARCHAR(50) NOT NULL,
  passenger_id_no VARCHAR(18) NOT NULL,
  passenger_phone VARCHAR(20) NULL,
  cabin_type VARCHAR(20) NOT NULL,
  seat_no VARCHAR(10) NULL,
  price DECIMAL(10,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT ck_ticket_status CHECK (status IN ('PENDING', 'ISSUED', 'RESCHEDULED', 'REFUNDED', 'CANCELLED')),
  CONSTRAINT ck_ticket_cabin CHECK (cabin_type IN ('ECONOMY', 'BUSINESS')),
  CONSTRAINT fk_ticket_order FOREIGN KEY (order_id) REFERENCES ticket_order(id),
  CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_ticket_flight FOREIGN KEY (flight_id) REFERENCES flight(id),
  INDEX idx_ticket_user (user_id),
  INDEX idx_ticket_flight (flight_id)
);

CREATE TABLE IF NOT EXISTS reschedule_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  from_flight_id BIGINT NOT NULL,
  to_flight_id BIGINT NOT NULL,
  from_cabin VARCHAR(20) NOT NULL,
  to_cabin VARCHAR(20) NOT NULL,
  price_diff DECIMAL(10,2) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reschedule_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id)
);

CREATE TABLE IF NOT EXISTS refund_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  refund_amount DECIMAL(10,2) NOT NULL,
  reason VARCHAR(200) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_refund_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id),
  CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES ticket_order(id)
);

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  action VARCHAR(50) NOT NULL,
  detail VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
