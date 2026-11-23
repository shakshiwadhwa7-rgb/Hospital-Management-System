-- =========================================================
--  Hospital Management System - Database Script
--  Run this ONCE in MySQL Workbench (as root).
--  It creates the database, all tables, a common app user
--  and some dummy data for testing.
-- =========================================================

DROP DATABASE IF EXISTS hospital_db;
CREATE DATABASE hospital_db;
USE hospital_db;

-- ---------------------------------------------------------
-- 1. USERS  (login for every role)
-- ---------------------------------------------------------
CREATE TABLE users (
    user_id     INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(50)  NOT NULL,
    role        ENUM('ADMIN','DOCTOR','NURSE','RECEPTIONIST','PHARMACIST') NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    phone       VARCHAR(15),
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------
-- 2. DOCTORS  (extra details for users with role DOCTOR)
-- ---------------------------------------------------------
CREATE TABLE doctors (
    doctor_id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT NOT NULL UNIQUE,
    specialization    VARCHAR(100) NOT NULL,
    consultation_fee  DECIMAL(10,2) NOT NULL DEFAULT 500.00,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ---------------------------------------------------------
-- 3. PATIENTS  (registered by Receptionist)
-- ---------------------------------------------------------
CREATE TABLE patients (
    patient_id     INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    age            INT NOT NULL,
    gender         ENUM('MALE','FEMALE','OTHER') NOT NULL,
    phone          VARCHAR(15),
    address        VARCHAR(255),
    blood_group    VARCHAR(5),
    registered_on  DATE NOT NULL DEFAULT (CURRENT_DATE)
);

-- ---------------------------------------------------------
-- 4. APPOINTMENTS  (booked by Receptionist, seen by Doctor)
-- ---------------------------------------------------------
CREATE TABLE appointments (
    appointment_id    INT AUTO_INCREMENT PRIMARY KEY,
    patient_id        INT NOT NULL,
    doctor_id         INT NOT NULL,
    appointment_date  DATE NOT NULL,
    appointment_time  TIME NOT NULL,
    reason            VARCHAR(255),
    status            ENUM('SCHEDULED','COMPLETED','CANCELLED') DEFAULT 'SCHEDULED',
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (doctor_id)  REFERENCES doctors(doctor_id)
);

-- ---------------------------------------------------------
-- 5. MEDICINES  (managed by Pharmacist)
-- ---------------------------------------------------------
CREATE TABLE medicines (
    medicine_id   INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    manufacturer  VARCHAR(100),
    price         DECIMAL(10,2) NOT NULL,
    stock_qty     INT NOT NULL DEFAULT 0,
    expiry_date   DATE NOT NULL
);

-- ---------------------------------------------------------
-- 6. PRESCRIPTIONS  (written by Doctor, dispensed by Pharmacist)
-- ---------------------------------------------------------
CREATE TABLE prescriptions (
    prescription_id  INT AUTO_INCREMENT PRIMARY KEY,
    appointment_id   INT NOT NULL,
    patient_id       INT NOT NULL,
    doctor_id        INT NOT NULL,
    diagnosis        VARCHAR(255) NOT NULL,
    notes            VARCHAR(255),
    created_on       DATE NOT NULL DEFAULT (CURRENT_DATE),
    status           ENUM('PENDING','DISPENSED') DEFAULT 'PENDING',
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id),
    FOREIGN KEY (patient_id)     REFERENCES patients(patient_id),
    FOREIGN KEY (doctor_id)      REFERENCES doctors(doctor_id)
);

-- Medicines written inside one prescription
CREATE TABLE prescription_items (
    item_id          INT AUTO_INCREMENT PRIMARY KEY,
    prescription_id  INT NOT NULL,
    medicine_id      INT NOT NULL,
    quantity         INT NOT NULL,
    dosage           VARCHAR(100),          -- e.g. "1-0-1 after food"
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(prescription_id) ON DELETE CASCADE,
    FOREIGN KEY (medicine_id)     REFERENCES medicines(medicine_id)
);

-- ---------------------------------------------------------
-- 7. BEDS  (allotted by Nurse)
-- ---------------------------------------------------------
CREATE TABLE beds (
    bed_id       INT AUTO_INCREMENT PRIMARY KEY,
    ward_name    VARCHAR(50) NOT NULL,      -- General, ICU, Private
    bed_number   VARCHAR(10) NOT NULL,
    charge_per_day DECIMAL(10,2) NOT NULL,
    is_occupied  BOOLEAN DEFAULT FALSE,
    patient_id   INT NULL,
    admitted_on  DATE NULL,
    UNIQUE (ward_name, bed_number),
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

-- ---------------------------------------------------------
-- 8. VITALS  (recorded by Nurse)
-- ---------------------------------------------------------
CREATE TABLE vitals (
    vital_id     INT AUTO_INCREMENT PRIMARY KEY,
    patient_id   INT NOT NULL,
    nurse_id     INT NOT NULL,              -- user_id of the nurse
    bp           VARCHAR(10),               -- e.g. "120/80"
    temperature  DECIMAL(4,1),              -- in Fahrenheit
    pulse        INT,
    recorded_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (nurse_id)   REFERENCES users(user_id)
);

-- ---------------------------------------------------------
-- 9. BILLS  (generated by Receptionist)
-- ---------------------------------------------------------
CREATE TABLE bills (
    bill_id              INT AUTO_INCREMENT PRIMARY KEY,
    patient_id           INT NOT NULL,
    consultation_charge  DECIMAL(10,2) DEFAULT 0,
    medicine_charge      DECIMAL(10,2) DEFAULT 0,
    room_charge          DECIMAL(10,2) DEFAULT 0,
    total_amount         DECIMAL(10,2) NOT NULL,
    bill_date            DATE NOT NULL DEFAULT (CURRENT_DATE),
    payment_status       ENUM('PAID','UNPAID') DEFAULT 'UNPAID',
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

-- =========================================================
--  DUMMY DATA (for testing)
-- =========================================================

-- Login accounts  (username / password)
INSERT INTO users (username, password, role, full_name, phone) VALUES
('admin',     'admin123', 'ADMIN',        'Shakshi Wadhwa', '9876500001'),
('drsharma',  'doc123',   'DOCTOR',       'Dr. Rajesh Sharma', '9876500002'),
('drmehta',   'doc123',   'DOCTOR',       'Dr. Priya Mehta',   '9876500003'),
('nurse1',    'nurse123', 'NURSE',        'Anita Verma',       '9876500004'),
('reception', 'rec123',   'RECEPTIONIST', 'Rohit Kumar',       '9876500005'),
('pharma',    'pharma123','PHARMACIST',   'Sunil Gupta',       '9876500006');

INSERT INTO doctors (user_id, specialization, consultation_fee) VALUES
(2, 'Cardiologist',       800.00),
(3, 'General Physician',  500.00);

INSERT INTO patients (name, age, gender, phone, address, blood_group) VALUES
('Amit Singh',   45, 'MALE',   '9811100001', 'Delhi',     'B+'),
('Neha Kapoor',  29, 'FEMALE', '9811100002', 'Noida',     'O+'),
('Ravi Yadav',   62, 'MALE',   '9811100003', 'Gurugram',  'A-');

INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, reason, status) VALUES
(1, 1, CURRENT_DATE, '10:30:00', 'Chest pain',  'SCHEDULED'),
(2, 2, CURRENT_DATE, '11:00:00', 'Fever',       'SCHEDULED'),
(3, 1, CURRENT_DATE - INTERVAL 1 DAY, '12:00:00', 'BP check-up', 'COMPLETED');

INSERT INTO medicines (name, manufacturer, price, stock_qty, expiry_date) VALUES
('Paracetamol 500mg', 'Cipla',      2.00, 500, '2027-12-31'),
('Amoxicillin 250mg', 'Sun Pharma', 8.50, 200, '2027-06-30'),
('Aspirin 75mg',      'Bayer',      3.00, 300, '2028-01-31'),
('Cetirizine 10mg',   'Dr. Reddys', 1.50, 400, '2027-09-30');

INSERT INTO prescriptions (appointment_id, patient_id, doctor_id, diagnosis, notes, status) VALUES
(3, 3, 1, 'High blood pressure', 'Reduce salt, review after 2 weeks', 'PENDING');

INSERT INTO prescription_items (prescription_id, medicine_id, quantity, dosage) VALUES
(1, 3, 14, '1-0-0 after breakfast');

INSERT INTO beds (ward_name, bed_number, charge_per_day) VALUES
('General', 'G1', 1000.00),
('General', 'G2', 1000.00),
('General', 'G3', 1000.00),
('ICU',     'I1', 5000.00),
('ICU',     'I2', 5000.00),
('Private', 'P1', 3000.00);

-- =========================================================
--  COMMON APP USER
--  The Java code logs in with this user, so the same code
--  works on every teammate's laptop. Nobody has to share or
--  change their own MySQL root password.
-- =========================================================
DROP USER IF EXISTS 'hms_user'@'localhost';
CREATE USER 'hms_user'@'localhost' IDENTIFIED BY 'hms123';
GRANT ALL PRIVILEGES ON hospital_db.* TO 'hms_user'@'localhost';
FLUSH PRIVILEGES;

SELECT 'Database ready!' AS message;
